from __future__ import annotations

import argparse
import re
from datetime import datetime, timezone
from pathlib import Path

from generate_docs import (
    CATALOG,
    EXTRACTOR,
    get_branch_name,
    get_simple_model,
    REPO_ROOT,
    get_complex_model,
    normalize_llm_output,
    post_chat_completion,
    rank_source_file_for_doc,
    read_text,
    repo_relative_posix,
    write_text,
)
from polish_docs import polish_markdown
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


def build_source_excerpt_for_doc(doc_path: Path) -> str:
    service = CATALOG.get_service_for_doc(doc_path)
    source_files = EXTRACTOR.list_source_files(service) if service else []
    ranked = sorted(source_files, key=lambda item: rank_source_file_for_doc(doc_path, item), reverse=True)
    blocks = []
    remaining = 5000
    for path in ranked[:4]:
        text = read_text(path)
        if not text:
            continue
        excerpt = text[: min(remaining, 1400)]
        blocks.append(f"Source: {path.relative_to(REPO_ROOT)}\n```text\n{excerpt}\n```")
        remaining -= len(excerpt)
        if remaining <= 0:
            break
    return "\n\n".join(blocks)


def split_sections(content: str) -> list[str]:
    lines = content.replace("\r\n", "\n").replace("\r", "\n").split("\n")
    sections: list[list[str]] = []
    current: list[str] = []
    for line in lines:
        if line.startswith("## ") and current:
            sections.append(current)
            current = [line]
            continue
        current.append(line)
    if current:
        sections.append(current)
    return ["\n".join(section).strip() for section in sections if "\n".join(section).strip()]


def build_section_prompt(doc_path: Path, section: str, source_excerpt: str) -> str:
    heading = section.splitlines()[0]
    return f"""
You are refining one section of a Markdown technical document.

Goal:
- improve legibility
- fix punctuation, commas, and obvious spelling or wording issues
- make the prose more natural
- make short or robotic bullets read more like maintained documentation
- keep the same structure

Hard rules:
- keep the first heading line exactly as it is
- preserve all subheadings exactly as they are
- preserve all Mermaid blocks, tables, and fenced code blocks exactly
- do not invent facts
- do not add or remove list items unless strictly necessary for clarity
- return only the refined section markdown

Target page:
{doc_path.relative_to(REPO_ROOT)}

Target section heading:
{heading}

Current section:
{section}

Relevant source excerpt:
{source_excerpt}

Editing guidance:
- rewrite within the existing bullets and paragraphs, not around them
- prefer direct, concrete wording over template-like phrases
- improve rhythm and readability even if the current text is technically correct
- do not introduce any new technical capability, compliance standard, security mechanism, infrastructure component, or architectural claim
""".strip()


def build_section_classification_prompt(doc_path: Path, section: str) -> str:
    return f"""
You are triaging one Markdown documentation section for editorial refinement.

Choose exactly one label:
- KEEP
- LIGHT_FIX
- HEAVY_FIX

Rules:
- KEEP: already readable, no meaningful editorial gain
- LIGHT_FIX: wording is serviceable but can be made more natural, clearer, or less robotic
- HEAVY_FIX: text is noticeably robotic, vague, or awkward and needs stronger rewriting
- return only the label

Target page:
{doc_path.relative_to(REPO_ROOT)}

Section:
{section}
""".strip()


def sanitize_output(content: str) -> str:
    text = normalize_llm_output(content)
    lines = text.splitlines()
    while lines and lines[0].strip().lower() in {
        "here is the refined section:",
        "here's the refined section:",
        "refined section:",
    }:
        lines.pop(0)
    return "\n".join(lines).strip()


def classify_section(doc_path: Path, section: str) -> str:
    if "```" in section or "|" in section:
        return "KEEP"
    if len(section.splitlines()) <= 3:
        return "KEEP"
    prompt = build_section_classification_prompt(doc_path, section)
    try:
        response = normalize_llm_output(post_chat_completion(get_simple_model(), prompt)).strip().upper()
    except Exception:
        return "KEEP"
    if response in {"KEEP", "LIGHT_FIX", "HEAVY_FIX"}:
        return response
    return "KEEP"


def apply_light_fix(doc_path: Path, section: str) -> str:
    heading = section.splitlines()[0]
    prompt = f"""
Refine this Markdown section very lightly.

Goals:
- improve readability in a visible but safe way
- fix commas, punctuation, and awkward wording
- make bullets and short paragraphs sound more natural and maintained
- keep the same information density or slightly improve it

Hard rules:
- keep the first heading exactly unchanged
- keep all bullets, tables, Mermaid blocks, and subheadings unchanged in structure
- do not invent facts
- return only the final section markdown

Target page:
{doc_path.relative_to(REPO_ROOT)}

Section:
{section}

Editing guidance:
- rewrite sentence wording inside the existing bullets if it improves clarity
- avoid robotic phrases like "is present", "is implemented", or "should be read from code" when a clearer wording is possible
- do not convert bullets into paragraphs or paragraphs into bullets
""".strip()
    try:
        response = sanitize_output(post_chat_completion(get_simple_model(), prompt))
        if is_safe_section(section, response) and response.splitlines()[0].strip() == heading.strip():
            return response
    except Exception:
        pass
    return section


def is_safe_section(original: str, refined: str) -> bool:
    original_lines = original.splitlines()
    refined_lines = refined.splitlines()
    if not original_lines or not refined_lines:
        return False
    if original_lines[0].strip() != refined_lines[0].strip():
        return False
    original_subheadings = [line.strip() for line in original_lines if line.strip().startswith("### ") or line.strip().startswith("#### ")]
    refined_subheadings = [line.strip() for line in refined_lines if line.strip().startswith("### ") or line.strip().startswith("#### ")]
    if original_subheadings != refined_subheadings:
        return False
    if original.count("```") != refined.count("```"):
        return False
    if "|" in original and "|" not in refined:
        return False
    if not has_safe_vocabulary(original, refined):
        return False
    return True


def extract_meaningful_terms(text: str) -> set[str]:
    blacklist = {
        "the",
        "and",
        "for",
        "with",
        "from",
        "that",
        "this",
        "into",
        "through",
        "about",
        "service",
        "repository",
        "repositories",
        "controller",
        "controllers",
        "configuration",
        "config",
        "class",
        "classes",
        "system",
        "current",
        "state",
        "implemented",
        "operations",
        "support",
        "notes",
        "related",
        "documentation",
        "page",
        "pages",
    }
    terms = {
        term.lower()
        for term in re.findall(r"\b[A-Za-z][A-Za-z0-9\-]{3,}\b", text)
        if term.lower() not in blacklist
    }
    return terms


def has_safe_vocabulary(original: str, refined: str) -> bool:
    original_terms = extract_meaningful_terms(original)
    refined_terms = extract_meaningful_terms(refined)
    disallowed_terms = {
        "gdpr",
        "rbac",
        "aes",
        "aes-256",
        "mfa",
        "backup",
        "backups",
        "firewall",
        "penetration",
        "quarterly",
        "compliance",
        "administrator",
        "administrative",
        "encryption",
        "brute-force",
        "role-based",
        "session",
        "sessions",
    }
    if refined_terms & disallowed_terms:
        return False
    new_terms = refined_terms - original_terms
    suspicious_new_terms = {
        term
        for term in new_terms
        if term[0].isalpha() and term not in {"orchestration", "interception", "lifecycle", "codebase", "configured"}
    }
    return len(suspicious_new_terms) <= 4


def refine_section(doc_path: Path, section: str, source_excerpt: str) -> str:
    classification = classify_section(doc_path, section)
    if classification == "KEEP":
        return section
    if classification == "LIGHT_FIX":
        return apply_light_fix(doc_path, section)
    prompt = build_section_prompt(doc_path, section, source_excerpt)
    last_error: Exception | None = None
    for _ in range(2):
        try:
            response = post_chat_completion(get_complex_model(), prompt)
            cleaned = sanitize_output(response)
            if not is_safe_section(section, cleaned):
                raise RuntimeError("Refined section changed headings, tables, or fenced blocks.")
            return cleaned
        except Exception as exc:
            last_error = exc
            prompt = f"{prompt}\n\nPrevious attempt failed: {exc}\nReturn only corrected section Markdown."
    return section if last_error else section


def refine_document(doc_path: Path) -> str:
    content = read_text(doc_path)
    source_excerpt = build_source_excerpt_for_doc(doc_path)
    sections = split_sections(content)
    if not sections:
        return content
    refined_sections = [sections[0]]
    for section in sections[1:]:
        refined_sections.append(refine_section(doc_path, section, source_excerpt))
    return polish_markdown("\n\n".join(refined_sections).strip() + "\n", doc_path)


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
    parser = argparse.ArgumentParser(description="Refine documentation text section by section with the complex model.")
    parser.add_argument("paths", nargs="*", help="Optional markdown files to refine.")
    args = parser.parse_args()
    targets = resolve_paths(args.paths)
    if not targets:
        print("No documentation files matched for refinement.")
        return
    updated_paths: list[Path] = []
    skipped_items: list[str] = []
    for doc_path in targets:
        try:
            write_text(doc_path, refine_document(doc_path))
            updated_paths.append(doc_path)
            print(f"Refined {repo_relative_posix(doc_path)}")
        except Exception as exc:
            skipped_items.append(f"{repo_relative_posix(doc_path)}: {exc}")
            print(f"Skipped {repo_relative_posix(doc_path)}: {exc}")
    report_paths = write_script_report(
        reports_root=CATALOG.reports_root,
        branch_name=get_branch_name(),
        script_name="refine_texts.py",
        started_at=started_at,
        ended_at=datetime.now(timezone.utc),
        intro="This run focused on section-by-section editorial cleanup, trying to improve readability without changing the page structure or technical claims.",
        summary_lines=[
            f"Target pages considered: `{len(targets)}`",
            f"Pages rewritten: `{len(updated_paths)}`",
            f"Skipped items: `{len(skipped_items)}`",
            f"Simple model: `{get_simple_model()}`",
            f"Complex model: `{get_complex_model()}`",
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
