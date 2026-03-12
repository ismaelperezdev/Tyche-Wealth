from __future__ import annotations

import argparse
from datetime import datetime, timezone
from pathlib import Path

from generate_docs import (
    CATALOG,
    REPO_ROOT,
    get_branch_name,
    get_complex_model,
    normalize_llm_output,
    post_chat_completion,
    read_text,
    repo_relative_posix,
    validate_markdown_output,
    write_text,
)
from reporting import format_path_list, write_script_report


DEFAULT_DOCS = [
    CATALOG.docs_root / "architecture" / "system.md",
    CATALOG.docs_root / "database" / "overview.md",
    *[
        path
        for service in CATALOG.services
        for path in [
            CATALOG.docs_root / "services" / service.name / "overview.md",
            CATALOG.docs_root / "services" / service.name / "api.md",
            CATALOG.docs_root / "services" / service.name / "data-model.md",
            CATALOG.docs_root / "services" / service.name / "runtime.md",
        ]
    ],
]


def polish_markdown(content: str, path: Path) -> str:
    text = content.replace("\r\n", "\n").replace("\r", "\n").strip()
    text = text.replace("Fields:", "#### Fields").replace("Relations:", "#### Relations")
    text = text.replace("```mermaid\n\n", "```mermaid\n").replace("\n\n```", "\n```")
    lines = text.split("\n")
    result: list[str] = []
    blank_run = 0
    for line in lines:
        stripped = line.rstrip()
        if stripped == "":
            blank_run += 1
            if blank_run <= 1:
                result.append("")
            continue
        blank_run = 0
        if stripped.startswith("#") and result and result[-1] != "":
            result.append("")
        result.append(stripped)
    final = "\n".join(result).strip() + "\n"
    if path.name == "index.md":
        final = final.replace("## Service Pages\n###", "## Service Pages\n\n###")
    return final


def build_prompt(doc_path: Path, current_doc: str) -> str:
    return f"""
You are polishing a technical Markdown document for Obsidian reading.

Task:
- improve heading hierarchy
- improve visual structure
- add very light emphasis using `**bold**` and occasional `<u>...</u>` only on plain text, never inside code spans
- keep facts unchanged
- keep Mermaid and code fences intact
- keep the page markdown only

Rules:
- do not invent new facts
- do not remove useful technical detail
- do not add emojis
- do not add decorative clutter
- do not create new tables unless the current page already has a clear list that benefits from tabular layout
- do not bold text inside backticks
- do not add or remove fenced code blocks except to preserve existing ones
- preserve all Mermaid blocks
- keep the document concise but visually clearer
- return only the final Markdown

Target page:
{doc_path.relative_to(REPO_ROOT)}

Current page:
{current_doc}
""".strip()


def sanitize_output(content: str) -> str:
    text = normalize_llm_output(content)
    lines = text.splitlines()
    while lines and lines[0].strip().lower() in {
        "here is the polished version:",
        "here's the polished version:",
        "polished markdown:",
    }:
        lines.pop(0)
    return "\n".join(lines).strip() + "\n"


def is_safe_polish(original: str, polished: str) -> bool:
    if polished.count("```") != original.count("```"):
        return False
    if "`**" in polished or "**`" in polished:
        return False
    if polished.strip().endswith("```"):
        return False
    return True


def polish_document_with_model(doc_path: Path) -> str:
    prompt = build_prompt(doc_path, read_text(doc_path))
    last_error: Exception | None = None
    for _ in range(3):
        try:
            response = post_chat_completion(get_complex_model(), prompt)
            cleaned = sanitize_output(response)
            validate_markdown_output(doc_path, cleaned)
            if not is_safe_polish(read_text(doc_path), cleaned):
                raise RuntimeError("Polish output changed fences or applied invalid emphasis.")
            return polish_markdown(cleaned, doc_path)
        except Exception as exc:
            last_error = exc
            prompt = f"{prompt}\n\nPrevious attempt failed: {exc}\nReturn only corrected Markdown."
    raise RuntimeError(str(last_error) if last_error else "Failed to polish document.")


def resolve_paths(raw_paths: list[str]) -> list[Path]:
    if not raw_paths:
        return [path for path in DEFAULT_DOCS if path.exists()]
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
    parser = argparse.ArgumentParser(description="Polish documentation structure and presentation.")
    parser.add_argument("paths", nargs="*", help="Optional markdown files to polish.")
    parser.add_argument("--deterministic-only", action="store_true", help="Only apply deterministic markdown cleanup.")
    args = parser.parse_args()
    targets = resolve_paths(args.paths)
    if not targets:
        print("No documentation files matched for polishing.")
        return
    updated_paths: list[Path] = []
    skipped_items: list[str] = []
    for doc_path in targets:
        try:
            output = polish_markdown(read_text(doc_path), doc_path) if args.deterministic_only else polish_document_with_model(doc_path)
            write_text(doc_path, output)
            updated_paths.append(doc_path)
            print(f"Polished {repo_relative_posix(doc_path)}")
        except Exception as exc:
            skipped_items.append(f"{repo_relative_posix(doc_path)}: {exc}")
            print(f"Skipped {repo_relative_posix(doc_path)}: {exc}")
    report_paths = write_script_report(
        reports_root=CATALOG.reports_root,
        branch_name=get_branch_name(),
        script_name="polish_docs.py",
        started_at=started_at,
        ended_at=datetime.now(timezone.utc),
        intro="This run focused on visual polish and markdown presentation so the documentation reads better in Obsidian without changing the underlying facts.",
        summary_lines=[
            f"Target pages considered: `{len(targets)}`",
            f"Pages polished: `{len(updated_paths)}`",
            f"Skipped items: `{len(skipped_items)}`",
            f"Mode: `{'deterministic-only' if args.deterministic_only else 'model-assisted'}`",
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
