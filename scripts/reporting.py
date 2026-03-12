from __future__ import annotations

import shutil
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path


@dataclass(frozen=True)
class ReportPaths:
    category: str
    branch_slug: str
    report_dir: Path
    history_path: Path
    latest_path: Path
    report_name: str
    latest_name: str


def sanitize_report_fragment(value: str) -> str:
    cleaned = "".join(char if char.isalnum() or char in {"-", "_", "."} else "-" for char in value.strip())
    cleaned = cleaned.strip("-_.")
    return cleaned or "unknown"


def classify_branch(branch_name: str) -> tuple[str, str]:
    lowered = branch_name.lower()
    candidates = [
        ("feature_", "feature"),
        ("feature-", "feature"),
        ("feature/", "feature"),
        ("fix_", "fix"),
        ("fix-", "fix"),
        ("fix/", "fix"),
        ("bugfix_", "fix"),
        ("bugfix-", "fix"),
        ("bugfix/", "fix"),
        ("hotfix_", "fix"),
        ("hotfix-", "fix"),
        ("hotfix/", "fix"),
    ]
    for prefix, category in candidates:
        if lowered.startswith(prefix):
            return category, sanitize_report_fragment(branch_name[len(prefix):])
    return "feature", sanitize_report_fragment(branch_name)


def migrate_legacy_report_dir(reports_root: Path, branch_name: str, target_dir: Path) -> None:
    legacy_dir = reports_root / sanitize_report_fragment(branch_name)
    if not legacy_dir.exists() or legacy_dir == target_dir or not legacy_dir.is_dir():
        return
    target_dir.mkdir(parents=True, exist_ok=True)
    for path in sorted(legacy_dir.iterdir()):
        destination = target_dir / path.name
        if destination.exists():
            continue
        shutil.move(str(path), str(destination))
    if not any(legacy_dir.iterdir()):
        legacy_dir.rmdir()


def prepare_report_paths(reports_root: Path, branch_name: str, script_name: str, started_at: datetime) -> ReportPaths:
    category, branch_slug = classify_branch(branch_name)
    report_dir = reports_root / category / branch_slug
    migrate_legacy_report_dir(reports_root, branch_name, report_dir)
    report_dir.mkdir(parents=True, exist_ok=True)
    script_slug = sanitize_report_fragment(script_name.removesuffix(".py"))
    timestamp = started_at.strftime("%Y-%m-%d-%H%M%S")
    report_name = f"{timestamp}-{script_slug}.md"
    latest_name = f"latest-{script_slug}.md"
    return ReportPaths(
        category=category,
        branch_slug=branch_slug,
        report_dir=report_dir,
        history_path=report_dir / report_name,
        latest_path=report_dir / latest_name,
        report_name=report_name,
        latest_name=latest_name,
    )


def cleanup_old_reports(report_dir: Path, keep_last: int = 5) -> None:
    history_files = sorted(
        [path for path in report_dir.glob("*.md") if not path.name.startswith("latest-")],
        key=lambda path: path.name,
        reverse=True,
    )
    for stale_path in history_files[keep_last:]:
        stale_path.unlink(missing_ok=True)


def _report_title_from_slug(value: str) -> str:
    return value.replace("-", " ").replace("_", " ").strip() or "unknown"


def _format_report_timestamp(path: Path) -> str:
    stem = path.stem
    timestamp_fragment = stem[:19]
    try:
        dt = datetime.strptime(timestamp_fragment, "%Y-%m-%d-%H%M%S")
        return dt.strftime("%Y-%m-%d %H:%M:%S UTC")
    except ValueError:
        return stem


def _write_report_index(path: Path, title: str, body_lines: list[str]) -> None:
    content = "\n".join([f"# {title}", "", *body_lines]).strip() + "\n"
    path.write_text(content, encoding="utf-8")


def rebuild_report_indexes(reports_root: Path) -> None:
    reports_root.mkdir(parents=True, exist_ok=True)
    expected_categories = ("feature", "fix")
    for category_name in expected_categories:
        (reports_root / category_name).mkdir(parents=True, exist_ok=True)
    category_dirs = sorted(path for path in reports_root.iterdir() if path.is_dir())
    root_lines = [
        "## Overview",
        "",
        "This section indexes documentation automation reports stored under `docs/reports/`.",
        "",
        "## Available Branch Kinds",
        "",
        "| Branch Kind | Purpose |",
        "| --- | --- |",
    ]
    if category_dirs:
        for category_dir in category_dirs:
            root_lines.append(
                f"| [{category_dir.name}]({category_dir.name}/index.md) | Reports generated from `{category_dir.name}` branches. |"
            )
    else:
        root_lines.append("| none | No generated reports are currently present. |")
    root_lines.extend(
        [
            "",
            "## Notes",
            "",
            "- Reports are grouped by branch name so repeated runs stay traceable.",
            "- `latest-*.md` files point to the latest known report for a given script within a branch folder.",
            "- Timestamped files preserve run history for comparison and rollback decisions.",
        ]
    )
    _write_report_index(reports_root / "index.md", "Reports", root_lines)

    for category_dir in category_dirs:
        branch_dirs = sorted(path for path in category_dir.iterdir() if path.is_dir())
        category_lines = [
            "## Overview",
            "",
            f"{_report_title_from_slug(category_dir.name).capitalize()} branch reports are grouped by branch folder.",
            "",
            "## Branches",
            "",
            "| Branch | Latest Report | History |",
            "| --- | --- | --- |",
        ]
        if branch_dirs:
            for branch_dir in branch_dirs:
                latest_reports = sorted(branch_dir.glob("latest-*.md"))
                latest_link = (
                    f"[{_report_title_from_slug(latest_reports[0].stem.replace('latest-', ''))}]({branch_dir.name}/{latest_reports[0].name})"
                    if latest_reports
                    else "none"
                )
                category_lines.append(
                    f"| [{branch_dir.name}]({branch_dir.name}/index.md) | {latest_link} | [full branch index]({branch_dir.name}/index.md) |"
                )
        else:
            category_lines.append("| none | none | none |")
        _write_report_index(category_dir / "index.md", f"{_report_title_from_slug(category_dir.name).title()} Reports", category_lines)

        for branch_dir in branch_dirs:
            history_reports = sorted(
                [path for path in branch_dir.glob("*.md") if path.name != "index.md" and not path.name.startswith("latest-")],
                key=lambda item: item.name,
                reverse=True,
            )
            latest_reports = sorted(branch_dir.glob("latest-*.md"))
            branch_lines = [
                "## Overview",
                "",
                f"This branch folder contains generated reports for `{branch_dir.name}`.",
                "",
                "## Latest",
                "",
            ]
            if latest_reports:
                for latest_report in latest_reports:
                    branch_lines.append(
                        f"- [{_report_title_from_slug(latest_report.stem.replace('latest-', ''))}]({latest_report.name})"
                    )
            else:
                branch_lines.append("- No latest report alias is present.")
            branch_lines.extend(["", "## History", ""])
            if history_reports:
                for history_report in history_reports:
                    branch_lines.append(f"- [{_format_report_timestamp(history_report)}]({history_report.name})")
            else:
                branch_lines.append("- No timestamped report history is present.")
            _write_report_index(branch_dir / "index.md", _report_title_from_slug(branch_dir.name), branch_lines)


def format_path_list(paths: list[Path], repo_root: Path, limit: int = 25) -> str:
    if not paths:
        return "- none"
    lines = [f"- `{path.relative_to(repo_root).as_posix()}`" for path in paths[:limit]]
    if len(paths) > limit:
        lines.append(f"- ... and {len(paths) - limit} more")
    return "\n".join(lines)


def categorize_source_path(path: Path, repo_root: Path) -> str:
    relative = path.relative_to(repo_root).as_posix()
    if "/src/main/java/" in relative:
        return "Main Java"
    if "/src/main/resources/" in relative:
        return "Main Resources"
    if "/src/test/java/" in relative:
        return "Test Java"
    if "/src/test/resources/" in relative:
        return "Test Resources"
    if relative.endswith("/pom.xml"):
        return "Build Files"
    if relative.endswith("/README.md"):
        return "Readme"
    if "/.mvn/" in relative or relative.endswith("/mvnw") or relative.endswith("/mvnw.cmd"):
        return "Wrapper and Tooling"
    return "Other"


def format_source_inventory(paths: list[Path], repo_root: Path) -> str:
    if not paths:
        return "- none"

    groups: dict[str, list[Path]] = {}
    for path in sorted(paths, key=lambda item: item.relative_to(repo_root).as_posix()):
        groups.setdefault(categorize_source_path(path, repo_root), []).append(path)

    ordered_names = [
        "Build Files",
        "Wrapper and Tooling",
        "Readme",
        "Main Java",
        "Main Resources",
        "Test Java",
        "Test Resources",
        "Other",
    ]

    summary_lines = [
        "| Area | Files |",
        "| --- | ---: |",
    ]
    for group_name in ordered_names:
        items = groups.get(group_name)
        if items:
            summary_lines.append(f"| {group_name} | {len(items)} |")

    detail_blocks: list[str] = []
    for group_name in ordered_names:
        items = groups.get(group_name)
        if not items:
            continue
        detail_lines = "\n".join(f"- `{path.relative_to(repo_root).as_posix()}`" for path in items)
        detail_blocks.append(
            "\n".join(
                [
                    f"<details><summary>{group_name} ({len(items)})</summary>",
                    "",
                    detail_lines,
                    "",
                    "</details>",
                ]
            )
        )

    return "\n".join(summary_lines) + "\n\n" + "\n\n".join(detail_blocks)


def build_rollback_prompt(script_name: str, report_name: str, changed_paths: list[Path], repo_root: Path) -> str:
    targets = format_path_list(changed_paths, repo_root, limit=50)
    return "\n".join(
        [
            f"Usa el workspace actual. Quiero hacer rollback solo de los cambios introducidos por el reporte `{report_name}` del script `{script_name}`.",
            "Revierte únicamente estos archivos si siguen coincidiendo con esa ejecución:",
            targets,
            "No toques cambios ajenos del usuario.",
            "Si alguno de esos archivos ha cambiado después de este reporte, pregúntame antes de sobrescribirlo.",
        ]
    )


def build_report_document(
    *,
    script_name: str,
    report_paths: ReportPaths,
    branch_name: str,
    started_at: datetime,
    ended_at: datetime,
    intro: str,
    summary_lines: list[str],
    sections: list[tuple[str, str]],
    changed_paths: list[Path],
    repo_root: Path,
) -> str:
    summary_block = "\n".join(f"- {line}" for line in summary_lines) or "- No summary captured."
    rendered_sections = "\n\n".join(
        f"## {title}\n\n{body.strip() or '- none'}" for title, body in sections
    )
    rollback_prompt = build_rollback_prompt(script_name, report_paths.report_name, changed_paths, repo_root)
    title = script_name.removesuffix(".py").replace("_", " ").title()
    return f"""# {title} Report

_{intro}_

## Run Metadata

- Report file: `{report_paths.report_name}`
- Latest alias: `{report_paths.latest_name}`
- Script: `{script_name}`
- Branch kind: `{report_paths.category}`
- Branch: `{report_paths.branch_slug}`
- Raw git branch: `{branch_name}`
- Started at: `{started_at.isoformat()}`
- Finished at: `{ended_at.isoformat()}`

## Quick Read

{summary_block}

{rendered_sections}

## Rollback Prompt

```text
{rollback_prompt}
```
"""


def write_script_report(
    *,
    reports_root: Path,
    branch_name: str,
    script_name: str,
    started_at: datetime,
    ended_at: datetime,
    intro: str,
    summary_lines: list[str],
    sections: list[tuple[str, str]],
    changed_paths: list[Path],
    repo_root: Path,
) -> ReportPaths:
    report_paths = prepare_report_paths(reports_root, branch_name, script_name, started_at)
    report_content = build_report_document(
        script_name=script_name,
        report_paths=report_paths,
        branch_name=branch_name,
        started_at=started_at,
        ended_at=ended_at,
        intro=intro,
        summary_lines=summary_lines,
        sections=sections,
        changed_paths=changed_paths,
        repo_root=repo_root,
    )
    report_paths.history_path.write_text(report_content, encoding="utf-8")
    report_paths.latest_path.write_text(report_content, encoding="utf-8")
    cleanup_old_reports(report_paths.report_dir)
    rebuild_report_indexes(reports_root)
    return report_paths
