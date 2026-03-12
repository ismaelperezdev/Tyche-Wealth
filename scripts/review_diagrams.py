from __future__ import annotations

import argparse
import re
from datetime import datetime, timezone
from pathlib import Path

from generate_docs import (
    CATALOG,
    EXTRACTOR,
    REPO_ROOT,
    build_source_excerpt,
    get_branch_name,
    get_complex_model,
    get_project_facts,
    normalize_llm_output,
    post_chat_completion,
    rank_source_file_for_doc,
    read_text,
    repo_relative_posix,
    write_text,
)
from polish_docs import polish_markdown
from reporting import format_path_list, write_script_report


DEFAULT_TARGETS = [
    CATALOG.docs_root / "architecture" / "system.md",
    CATALOG.docs_root / "database" / "overview.md",
    *[
        CATALOG.docs_root / "services" / service.name / "api.md"
        for service in CATALOG.services
    ],
]


def build_prompt(doc_path: Path, current_doc: str, source_excerpt: str) -> str:
    return f"""
You are reviewing Mermaid diagrams for a technical Markdown page.

Task:
- inspect the current page
- inspect the source excerpt
- decide whether one additional Mermaid diagram would materially improve the page

Response rules:
- if the current diagrams are already sufficient, return exactly `NO_CHANGE`
- otherwise return only one Markdown snippet in this exact shape:

### <Short Diagram Title>
```mermaid
<diagram>
```

Hard constraints:
- do not rewrite the whole page
- do not include explanations before or after the snippet
- do not invent endpoints, services, tables, flows, or interactions not visible in the source context
- prefer a compact diagram over a wide one
- add at most one new diagram

Project facts:
{get_project_facts()}

Target page:
{doc_path.relative_to(REPO_ROOT)}

Current page:
{current_doc}

Relevant source excerpt:
{source_excerpt}
""".strip()


def sanitize_response(content: str) -> str:
    text = normalize_llm_output(content).strip()
    if text.upper() == "NO_CHANGE":
        return "NO_CHANGE"
    lines = text.splitlines()
    while lines and lines[0].strip().lower() in {
        "diagram suggestion:",
        "suggested diagram:",
        "here is the diagram:",
    }:
        lines.pop(0)
    return "\n".join(lines).strip()


def is_safe_snippet(snippet: str, current_doc: str, source_excerpt: str) -> bool:
    if snippet == "NO_CHANGE":
        return True
    allowed_tokens = set(re.findall(r"\b[A-Z][A-Za-z0-9_]+(?:Dto|Entity|Helper|Service|Controller|Repository|Config)?\b", current_doc + "\n" + source_excerpt))
    proposed_tokens = set(re.findall(r"\b[A-Z][A-Za-z0-9_]+(?:Dto|Entity|Helper|Service|Controller|Repository|Config)?\b", snippet))
    disallowed = proposed_tokens - allowed_tokens
    suspicious_literals = ["201 Created", "PATCH ", "GET ", "DELETE ", "/user/", "/portfolio"]
    if disallowed:
        return False
    if any(item in snippet for item in suspicious_literals):
        return False
    return True


def extract_existing_titles(content: str) -> set[str]:
    titles = set()
    for line in content.splitlines():
        stripped = line.strip()
        if stripped.startswith("### "):
            titles.add(stripped)
    return titles


def choose_insert_anchor(doc_path: Path, content: str) -> str | None:
    relative = repo_relative_posix(doc_path)
    if relative.endswith("/api.md"):
        return "## Flows and Sequence Diagrams"
    if relative.endswith("architecture/system.md"):
        return "## Interactions"
    if relative.endswith("database/overview.md"):
        return "### Relationships"
    return None


def insert_snippet(doc_path: Path, content: str, snippet: str) -> str:
    anchor = choose_insert_anchor(doc_path, content)
    if not anchor or anchor not in content:
        return content
    if snippet.splitlines()[0].strip() in extract_existing_titles(content):
        return content
    insertion = f"{anchor}\n\n{snippet}\n"
    return content.replace(anchor, insertion, 1)


def review_document(doc_path: Path) -> str:
    service = CATALOG.get_service_for_doc(doc_path)
    source_files = EXTRACTOR.list_source_files(service) if service else []
    ranked = sorted(source_files, key=lambda item: rank_source_file_for_doc(doc_path, item), reverse=True)
    excerpt = build_source_excerpt(ranked[:4])
    current_doc = read_text(doc_path)
    prompt = build_prompt(doc_path, current_doc, excerpt)
    response = post_chat_completion(get_complex_model(), prompt)
    snippet = sanitize_response(response)
    if snippet == "NO_CHANGE" or not is_safe_snippet(snippet, current_doc, excerpt):
        return current_doc
    updated = insert_snippet(doc_path, current_doc, snippet)
    return polish_markdown(updated, doc_path)


def resolve_paths(raw_paths: list[str]) -> list[Path]:
    if not raw_paths:
        return [path for path in DEFAULT_TARGETS if path.exists()]
    resolved = []
    for raw_path in raw_paths:
        path = Path(raw_path)
        if not path.is_absolute():
            path = (REPO_ROOT / path).resolve()
        if path.exists():
            resolved.append(path)
    return resolved


def main() -> None:
    started_at = datetime.now(timezone.utc)
    parser = argparse.ArgumentParser(description="Review docs and add at most one extra Mermaid diagram when useful.")
    parser.add_argument("paths", nargs="*", help="Optional markdown files to review.")
    args = parser.parse_args()
    targets = resolve_paths(args.paths)
    if not targets:
        print("No documentation files matched for diagram review.")
        return
    updated_paths: list[Path] = []
    skipped_items: list[str] = []
    for doc_path in targets:
        try:
            updated = review_document(doc_path)
            write_text(doc_path, updated)
            updated_paths.append(doc_path)
            print(f"Reviewed diagrams in {repo_relative_posix(doc_path)}")
        except Exception as exc:
            skipped_items.append(f"{repo_relative_posix(doc_path)}: {exc}")
            print(f"Skipped {repo_relative_posix(doc_path)}: {exc}")
    report_paths = write_script_report(
        reports_root=CATALOG.reports_root,
        branch_name=get_branch_name(),
        script_name="review_diagrams.py",
        started_at=started_at,
        ended_at=datetime.now(timezone.utc),
        intro="This run reviewed the current Mermaid coverage and only kept conservative changes that fit the existing documentation structure.",
        summary_lines=[
            f"Target pages reviewed: `{len(targets)}`",
            f"Pages written back: `{len(updated_paths)}`",
            f"Skipped items: `{len(skipped_items)}`",
            f"Model used: `{get_complex_model()}`",
        ],
        sections=[
            ("Target Pages", format_path_list(targets, REPO_ROOT)),
            ("Updated Pages", format_path_list(updated_paths, REPO_ROOT)),
            ("Skipped Items", "\n".join(f"- {item}" for item in skipped_items) or "- none"),
        ],
        changed_paths=updated_paths,
        repo_root=REPO_ROOT,
    )
    print(f"Report written to {report_paths.latest_path.relative_to(REPO_ROOT)}")


if __name__ == "__main__":
    main()
