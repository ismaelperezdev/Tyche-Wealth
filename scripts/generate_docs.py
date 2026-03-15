from __future__ import annotations

import json
import os
import subprocess
import urllib.error
import urllib.request
from datetime import datetime, timezone
from pathlib import Path

from doc_catalog import ServiceCatalog
from doc_extractors import ServiceFactsExtractor
from doc_models import ServiceDefinition, ServiceFacts, UpdatedDocument
from doc_renderers import DeterministicRenderer
from reporting import format_path_list, format_source_inventory, write_script_report


REPO_ROOT = Path(__file__).resolve().parents[1]
SHARED_LOCAL_PROPERTIES_PATH = REPO_ROOT / "application-local.properties"
DISALLOWED_RESPONSE_PREFIXES = (
    "sure,",
    "sure ",
    "here's",
    "here is",
    "i'm sorry",
    "i am sorry",
    "please note",
    "of course",
)
DISALLOWED_RESPONSE_SNIPPETS = (
    "happy coding",
    "feel free to ask",
    "github repository",
    "mysql instance",
    "https ensures secure communication",
    "ai assistant",
)
TOPIC_KEYWORDS: dict[str, set[str]] = {
    "auth": {"auth", "login", "refresh", "token", "jwt", "security", "register", "interceptor"},
    "api": {"api", "controller", "dto", "request", "response", "swagger", "openapi"},
    "data": {"entity", "repository", "database", "schema", "liquibase", "changelog", "table"},
    "service": {"service", "helper", "mapper", "config", "application", "pom", "readme"},
    "development": {"readme", "pom", "mvn", "build", "application", "config", "test", "ci", "pipeline"},
    "architecture": {"architecture", "service", "controller", "repository", "entity", "config", "security"},
}

CATALOG = ServiceCatalog(REPO_ROOT)
EXTRACTOR = ServiceFactsExtractor(REPO_ROOT)
RENDERER = DeterministicRenderer(CATALOG)


def load_properties_file(path: Path) -> None:
    if not path.exists():
        return
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        key = key.strip()
        value = value.strip()
        if key and key not in os.environ:
            os.environ[key] = value


def load_local_configuration() -> None:
    for service in CATALOG.services:
        load_properties_file(service.local_properties_path)
    load_properties_file(SHARED_LOCAL_PROPERTIES_PATH)


def read_text(path: Path, fallback: str = "") -> str:
    if not path.exists():
        return fallback
    return path.read_text(encoding="utf-8")


def write_text(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")


def get_base_url() -> str:
    return os.getenv("LLM_BASE_URL", os.getenv("OLLAMA_BASE_URL", "http://localhost:11434/v1"))


def get_api_key() -> str:
    return os.getenv("LLM_API_KEY", os.getenv("OLLAMA_API_KEY", "")).strip()


def get_timeout_seconds() -> float:
    return float(os.getenv("LLM_TIMEOUT_SECONDS", "60"))


def get_simple_model() -> str:
    return os.getenv("LLM_SIMPLE_MODEL", os.getenv("OLLAMA_SIMPLE_MODEL", "deepseek-coder:6.7b"))


def get_complex_model() -> str:
    return os.getenv("LLM_COMPLEX_MODEL", os.getenv("OLLAMA_COMPLEX_MODEL", "deepseek-r1:8b"))


def get_max_excerpt_chars() -> int:
    return int(os.getenv("DOC_MAX_EXCERPT_CHARS", "12000"))


def get_max_source_files_per_doc() -> int:
    return int(os.getenv("DOC_MAX_SOURCE_FILES_PER_DOC", "8"))


def is_ci() -> bool:
    return any(os.getenv(name, "").lower() in {"1", "true", "yes"} for name in {"CI", "GITHUB_ACTIONS", "TF_BUILD"})


def sanitize_name(value: str) -> str:
    return "".join(char if char.isalnum() or char in {"-", "_", "."} else "_" for char in value)


def get_branch_name() -> str:
    try:
        result = subprocess.run(
            ["git", "branch", "--show-current"],
            cwd=REPO_ROOT,
            check=True,
            capture_output=True,
            text=True,
        )
        return sanitize_name(result.stdout.strip() or "_unknown-branch")
    except Exception:
        return "_unknown-branch"


def run_git_command(args: list[str], check: bool = True) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        ["git", *args],
        cwd=REPO_ROOT,
        check=check,
        capture_output=True,
        text=True,
    )


def resolve_ci_base_ref() -> str | None:
    candidates = [
        os.getenv("SYSTEM_PULLREQUEST_TARGETBRANCH"),
        os.getenv("GITHUB_BASE_REF"),
        os.getenv("BUILD_SOURCEBRANCHNAME"),
        "main",
    ]
    for candidate in candidates:
        if not candidate:
            continue
        ref = candidate.removeprefix("refs/heads/")
        result = run_git_command(["rev-parse", "--verify", ref], check=False)
        if result.returncode == 0:
            return ref
    return None


def list_changed_files_for_ci() -> list[Path]:
    base_ref = resolve_ci_base_ref()
    if not base_ref:
        return []
    merge_base = run_git_command(["merge-base", "HEAD", base_ref], check=False)
    if merge_base.returncode != 0 or not merge_base.stdout.strip():
        return []
    diff_result = run_git_command(["diff", "--name-only", merge_base.stdout.strip(), "HEAD"], check=False)
    if diff_result.returncode != 0:
        return []
    changed_files: list[Path] = []
    for raw_line in diff_result.stdout.splitlines():
        candidate = REPO_ROOT / raw_line.strip()
        if candidate.exists():
            changed_files.append(candidate)
    return sorted(set(changed_files))


def list_ci_commits() -> list[dict[str, object]]:
    base_ref = resolve_ci_base_ref()
    if not base_ref:
        return []
    merge_base = run_git_command(["merge-base", "HEAD", base_ref], check=False)
    if merge_base.returncode != 0 or not merge_base.stdout.strip():
        return []
    range_spec = f"{merge_base.stdout.strip()}..HEAD"
    log_result = run_git_command(
        ["log", "--reverse", "--format=%H%x1f%h%x1f%s", range_spec],
        check=False,
    )
    if log_result.returncode != 0:
        return []
    commits: list[dict[str, object]] = []
    for raw_line in log_result.stdout.splitlines():
        if not raw_line.strip():
            continue
        parts = raw_line.split("\x1f")
        if len(parts) != 3:
            continue
        commit_hash, short_hash, subject = parts
        show_result = run_git_command(
            ["show", "--pretty=format:", "--name-only", commit_hash],
            check=False,
        )
        changed_paths: list[Path] = []
        if show_result.returncode == 0:
            for raw_path in show_result.stdout.splitlines():
                if not raw_path.strip():
                    continue
                candidate = REPO_ROOT / raw_path.strip()
                if candidate.exists():
                    changed_paths.append(candidate)
        commits.append(
            {
                "hash": commit_hash,
                "short_hash": short_hash,
                "subject": subject,
                "paths": sorted(set(changed_paths)),
            }
        )
    return commits


def list_service_source_files() -> list[Path]:
    files: list[Path] = []
    for service in CATALOG.services:
        files.extend(EXTRACTOR.list_source_files(service))
    return sorted(set(files))


def repo_relative_posix(path: Path) -> str:
    return path.relative_to(REPO_ROOT).as_posix().lower()


def determine_target_docs(changed_files: list[Path], all_docs: list[Path]) -> tuple[list[Path], str]:
    if not is_ci():
        return CATALOG.list_default_target_docs(), "full-repository"
    if not changed_files:
        return sorted(set(all_docs + CATALOG.list_default_target_docs())), "ci-without-diff"

    service_names = {
        path.relative_to(REPO_ROOT).parts[0]
        for path in changed_files
        if path.is_relative_to(REPO_ROOT)
        and len(path.relative_to(REPO_ROOT).parts) > 1
        and path.relative_to(REPO_ROOT).parts[0].endswith("-service")
    }
    targets = set(CATALOG.list_default_target_docs())
    for doc_path in all_docs:
        relative = repo_relative_posix(doc_path)
        if any(f"services/{service_name}/" in relative for service_name in service_names):
            targets.add(doc_path)
    return sorted(targets), "ci-changed-services"


def determine_service_sources(changed_files: list[Path], all_service_files: list[Path]) -> tuple[list[Path], str]:
    if not is_ci():
        return all_service_files, "full-repository"
    if not changed_files:
        return all_service_files, "ci-without-diff"

    changed_roots = {
        path.relative_to(REPO_ROOT).parts[0]
        for path in changed_files
        if path.is_relative_to(REPO_ROOT)
        and len(path.relative_to(REPO_ROOT).parts) > 1
        and path.relative_to(REPO_ROOT).parts[0].endswith("-service")
    }
    selected = [
        path
        for path in all_service_files
        if path.is_relative_to(REPO_ROOT) and path.relative_to(REPO_ROOT).parts[0] in changed_roots
    ]
    return (selected or all_service_files), "ci-changed-services"


def format_commit_inventory(commits: list[dict[str, object]]) -> str:
    if not commits:
        return "- none"
    summary_lines = [
        "| Commit | Summary | Files |",
        "| --- | --- | ---: |",
    ]
    detail_blocks: list[str] = []
    for commit in commits:
        short_hash = str(commit["short_hash"])
        subject = str(commit["subject"])
        paths = list(commit["paths"])
        summary_lines.append(f"| `{short_hash}` | {subject} | {len(paths)} |")
        details_body = format_path_list(paths, REPO_ROOT, limit=100)
        detail_blocks.append(
            "\n".join(
                [
                    f"<details><summary>{short_hash} - {subject}</summary>",
                    "",
                    details_body,
                    "",
                    "</details>",
                ]
            )
        )
    return "\n".join(summary_lines) + "\n\n" + "\n\n".join(detail_blocks)


def get_service_for_doc(doc_path: Path) -> ServiceDefinition | None:
    return CATALOG.get_service_for_doc(doc_path)


def get_readme_context_for_doc(doc_path: Path) -> str:
    service = get_service_for_doc(doc_path)
    if not service:
        return "No service README context available."
    return read_text(service.readme_path, f"# {service.name}\n\nREADME not found.\n")


def detect_topic(doc_path: Path) -> str:
    text = repo_relative_posix(doc_path)
    for topic, keywords in TOPIC_KEYWORDS.items():
        if any(keyword in text for keyword in keywords):
            return topic
    return "service"


def get_required_sections(doc_path: Path) -> list[str]:
    doc_type = CATALOG.infer_doc_type(doc_path)
    if doc_type == "service-readme":
        return ["Overview", "Requirements", "Run Locally", "Local Configuration", "Implemented Endpoints", "Documentation Links"]
    if doc_type == "service-overview":
        return ["Overview", "Responsibilities", "Implemented Scope", "Main Components", "Security and Operational Notes", "Related Documentation"]
    if doc_type == "service-api":
        return ["Overview", "Base Paths and API Surface", "Implemented Endpoints", "Validation, Errors, and Constraints", "Flows and Sequence Diagrams", "Notes"]
    if doc_type in {"service-data-model", "database-overview"}:
        return ["Overview", "Implemented Entities", "Relationships", "Persistence and Schema Notes", "Related Documentation"] if doc_type == "service-data-model" else ["Overview", "Implemented Service Schemas", "Relationships", "Constraints and Persistence Notes", "Source of Truth"]
    if doc_type == "service-runtime":
        return ["Overview", "Requirements", "Run Locally", "Local Configuration", "Security, Rate Limiting, and Observability", "Operational Notes"]
    if doc_type == "service-observability":
        return ["Overview", "Dashboard Summary", "Dashboard Representation", "What The Dashboard Checks", "Metric Notes", "Operational Notes"]
    if doc_type == "architecture-system":
        return ["Overview", "Implemented Components", "Layered Structure", "Interactions", "Evolution Notes"]
    if doc_type == "architecture-observability":
        return ["Overview", "Repository Snapshot", "Implemented Flow", "Interaction Diagram", "Configuration Layout", "Metrics Families", "Notes"]
    return ["Overview", "Implemented Scope", "Notes"]


def get_document_specific_rules(doc_path: Path) -> str:
    doc_type = CATALOG.infer_doc_type(doc_path)
    if doc_type == "service-readme":
        return """
- Document only the resolved service.
- Use repository files as the source of truth for commands and requirements.
- Do not include real secrets, copied local credentials, or concrete passwords.
""".strip()
    if doc_type == "service-api":
        return """
- Consolidate API description, sequence diagrams, and request-flow notes in one page.
- List only routes visible in code.
- Keep each endpoint description concrete and tied to source APIs or controllers.
""".strip()
    if doc_type == "service-overview":
        return """
- Keep this page at service level, not class-by-class.
- Summarize only the implemented scope visible in the provided sources.
- Do not invent domain responsibilities that are not backed by code.
""".strip()
    if doc_type == "service-runtime":
        return """
- Focus on concrete runtime behavior: configuration, startup, security, rate limiting, and observability.
- Avoid vague deployment advice not backed by repository files.
""".strip()
    if doc_type == "service-observability":
        return """
- Explain the current dashboard intent and the metric groups it checks.
- Prefer operational interpretation over raw metric inventories.
- Describe only the Grafana and Prometheus flow that is visible in the repository and local setup.
""".strip()
    if doc_type == "architecture-observability":
        return """
- Keep this page at repository and system level.
- Explain the shared observability flow and the repository configuration layout.
- Do not drift into service-specific dashboard panel detail beyond what is necessary to explain the overall design.
""".strip()
    if doc_type in {"architecture-system", "database-overview"}:
        return """
- Consolidate related details into this page instead of splitting into many smaller pages.
- Prefer specific descriptions over generic architectural statements.
""".strip()
    return """
- Prefer concise summaries over class-by-class inventories.
- Avoid speculative roadmap sections unless the page is explicitly conceptual.
""".strip()


def get_project_facts() -> str:
    service_names = ", ".join(f"`{service.name}`" for service in CATALOG.services) or "`none detected`"
    return f"""
- Implemented microservices currently detected: {service_names}
- Use repository code as the primary source of truth over older markdown.
- Treat `docs/knowledge/project-context.md` as stable read-only context and the real `docs/knowledge/` tree as the target structure to maintain.
- Prefer service-specific source files, controllers, DTOs, entities, repositories, configuration, and changelogs when deriving facts.
- Only describe endpoints, tables, integrations, or flows that are visible in code or explicitly marked as conceptual.
- Service-specific runtime details such as ports, base paths, Swagger URLs, and environment keys must come from the relevant service sources, not from generic assumptions.
""".strip()


def sanitize_context_document(doc_path: Path, content: str) -> str:
    lowered = content.lower()
    if any(snippet in lowered for snippet in DISALLOWED_RESPONSE_SNIPPETS):
        return f"# {doc_path.stem}\n\nTODO\n"
    if "```markdown" in lowered or "i'm an ai assistant" in lowered or "sure, here's" in lowered:
        return f"# {doc_path.stem}\n\nTODO\n"
    return content


def select_doc_model(doc_path: Path) -> str:
    doc_type = CATALOG.infer_doc_type(doc_path)
    if doc_type in {"service-overview", "architecture-system", "database-overview"}:
        return get_complex_model()
    return get_simple_model()


def rank_source_file_for_doc(doc_path: Path, source_path: Path) -> int:
    topic = detect_topic(doc_path)
    source_text = str(source_path.relative_to(REPO_ROOT)).lower()
    score = 0
    for keyword in TOPIC_KEYWORDS[topic]:
        if keyword in source_text:
            score += 3
    if "src/main/java" in source_text:
        score += 2
    if "src/main/resources" in source_text:
        score += 1
    if source_path.name == "pom.xml":
        score += 2
    if source_path.name == "README.md":
        score += 1
    service = get_service_for_doc(doc_path)
    if service and source_path.is_relative_to(service.root):
        score += 3
    if doc_path.name in {"index.md", "project-context.md"}:
        score += 1
    return score


def build_source_excerpt(source_files: list[Path]) -> str:
    sections: list[str] = []
    remaining_chars = get_max_excerpt_chars()
    for path in source_files[: get_max_source_files_per_doc()]:
        if path.name == "application-local.properties":
            continue
        content = read_text(path)
        if not content:
            continue
        excerpt_budget = max(800, remaining_chars // max(1, get_max_source_files_per_doc()))
        excerpt = content[:excerpt_budget]
        sections.append(f"Source: {path.relative_to(REPO_ROOT)}\n```text\n{excerpt}\n```")
        remaining_chars -= len(excerpt)
        if remaining_chars <= 0:
            break
    return "\n\n".join(sections) or "No source excerpts available."


def post_chat_completion(model: str, prompt: str) -> str:
    payload = json.dumps(
        {
            "model": model,
            "messages": [{"role": "user", "content": prompt}],
            "stream": False,
        }
    ).encode("utf-8")
    headers = {"Content-Type": "application/json"}
    api_key = get_api_key()
    if api_key:
        headers["Authorization"] = f"Bearer {api_key}"
    request = urllib.request.Request(
        url=get_base_url().rstrip("/") + "/chat/completions",
        data=payload,
        headers=headers,
        method="POST",
    )
    try:
        with urllib.request.urlopen(request, timeout=get_timeout_seconds()) as response:
            response_body = response.read().decode("utf-8")
    except urllib.error.HTTPError as exc:
        error_body = exc.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"HTTP {exc.code} from LLM server: {error_body}") from exc
    except urllib.error.URLError as exc:
        raise RuntimeError(f"Unable to reach LLM server at {get_base_url()}: {exc.reason}") from exc
    parsed = json.loads(response_body)
    choices = parsed.get("choices") or []
    if not choices:
        raise RuntimeError(f"LLM response did not include choices: {response_body[:500]}")
    content = choices[0].get("message", {}).get("content", "")
    return normalize_llm_output(str(content))


def normalize_llm_output(content: str) -> str:
    text = content.replace("\r\n", "\n").strip()
    text = (
        text.replace("\u2013", "-")
        .replace("\u2014", "-")
        .replace("\u2018", "'")
        .replace("\u2019", "'")
        .replace("\u201c", '"')
        .replace("\u201d", '"')
        .replace("Ã¢â‚¬â€œ", "-")
        .replace("Ã¢â‚¬â€", "-")
        .replace("Ã¢â‚¬Ëœ", "'")
        .replace("Ã¢â‚¬â„¢", "'")
        .replace("Ã¢â‚¬Å“", '"')
        .replace("Ã¢â‚¬\x9d", '"')
    )
    lower = text.lower()
    if lower.startswith("```markdown"):
        text = text[len("```markdown"):].strip()
    elif lower.startswith("```md"):
        text = text[len("```md"):].strip()
    if text.startswith("```") and text.endswith("```"):
        text = text[3:-3].strip()
    lines = text.splitlines()
    while lines and lines[0].strip().lower() in {
        "sure, here's an updated version of your markdown:",
        "sure, here is an updated version of your markdown:",
        "please note that the above documentation is based on the provided sources.",
    }:
        lines.pop(0)
    return "\n".join(lines).strip() + "\n"


def validate_markdown_output(doc_path: Path, content: str) -> None:
    stripped = content.strip()
    lowered = stripped.lower()
    if not stripped:
        raise RuntimeError("LLM returned empty content.")
    if any(lowered.startswith(prefix) for prefix in DISALLOWED_RESPONSE_PREFIXES):
        raise RuntimeError("LLM returned chatty wrapper text instead of a direct document.")
    if "```markdown" in lowered:
        raise RuntimeError("LLM returned fenced markdown instead of raw markdown.")
    if "i'm sorry" in lowered or "i am sorry" in lowered:
        raise RuntimeError("LLM returned an apology instead of documentation.")
    if "could you provide more information" in lowered or "query seems unclear" in lowered:
        raise RuntimeError("LLM reported unclear instructions instead of updating the page.")
    if not stripped.startswith("#"):
        raise RuntimeError("LLM output does not start with a markdown heading.")
    if any(snippet in lowered for snippet in DISALLOWED_RESPONSE_SNIPPETS):
        raise RuntimeError("LLM output contains disallowed generic or invented content.")


def build_retry_prompt(base_prompt: str, invalid_output: str, error_message: str) -> str:
    return f"""
{base_prompt}

Your previous answer was invalid.

Validation error:
{error_message}

Invalid answer:
{invalid_output[:4000]}

Retry instructions:
- Return only the corrected final markdown.
- Start with a single `#` heading.
- Do not include any preamble, apology, explanation, or code fence.
- Remove invented claims not supported by the provided sources.
""".strip()


def generate_validated_markdown(doc_path: Path, base_prompt: str, preferred_model: str) -> tuple[str, str]:
    attempts = [preferred_model]
    if preferred_model != get_complex_model():
        attempts.append(get_complex_model())
    prompt = base_prompt
    last_error: Exception | None = None
    for model in attempts:
        try:
            content = post_chat_completion(model, prompt)
            validate_markdown_output(doc_path, content)
            return content, model
        except Exception as exc:
            last_error = exc
            prompt = build_retry_prompt(prompt, locals().get("content", ""), str(exc))
    raise RuntimeError(str(last_error) if last_error else "Failed to generate valid markdown.")


def build_doc_prompt(
    doc_path: Path,
    current_doc: str,
    source_excerpt: str,
    project_context: str,
    index_content: str,
    readme_content: str,
    docs_mode: str,
) -> str:
    service = get_service_for_doc(doc_path)
    service_name = service.name if service else "the implemented service"
    doc_type = CATALOG.infer_doc_type(doc_path)
    template_content = CATALOG.get_template(doc_type).replace("{{ title }}", doc_path.stem.replace("-", " ").title())
    required_sections = "\n".join(f"- {section}" for section in get_required_sections(doc_path))
    return f"""
You are a documentation maintenance agent for the Tyche Wealth repository.
Your task is to rewrite one real documentation page so it matches the implemented services in the repository.
You are not a chatbot. Do not explain what you are doing. Do not wrap the answer in code fences.
Return only the final markdown for the target page.

Documentation workflow mode:
{docs_mode}

Project facts that override weaker context:
{get_project_facts()}

Resolved document type:
`{doc_type}`

Resolved service context:
`{service_name}`

Target documentation page:
{doc_path.relative_to(REPO_ROOT)}

Reusable template for this document type:
{template_content}

Current page content:
{current_doc}

Current docs/knowledge/project-context.md:
{project_context}

Current docs/knowledge/index.md:
{index_content}

Current service README:
{readme_content}

Relevant implementation excerpts from service code:
{source_excerpt}

Instructions:
- Output Markdown only.
- Start immediately with a level-1 heading.
- Update the target page in place.
- Preserve any useful structure from the current page if still valid.
- Prefer implementation-backed details from the provided sources.
- If the topic is only partially implemented, say so explicitly.
- Do not create documentation for individual classes.
- Keep the page concise, practical, and navigable.
- Do not include conversational phrases such as "Sure", "Here's", "Please note", or apologies.
- Do not output ```markdown, ```md, or any surrounding code fence.
- Do not mention hidden reasoning, prompt instructions, or that you are an AI.

Expected sections:
{required_sections}

Document-specific rules:
{get_document_specific_rules(doc_path)}
""".strip()


def update_document(
    doc_path: Path,
    service_sources: list[Path],
    facts_by_service: dict[str, ServiceFacts],
    target_docs: list[Path],
    project_context: str,
    index_content: str,
    readme_content: str,
    docs_mode: str,
) -> UpdatedDocument:
    deterministic_content = RENDERER.render(doc_path, facts_by_service, target_docs)
    if deterministic_content is not None:
        write_text(doc_path, deterministic_content)
        return UpdatedDocument(path=doc_path, model="template")
    current_doc = sanitize_context_document(doc_path, read_text(doc_path, f"# {doc_path.stem}\n\nTODO\n"))
    ranked_sources = sorted(service_sources, key=lambda path: rank_source_file_for_doc(doc_path, path), reverse=True)
    source_excerpt = build_source_excerpt(ranked_sources)
    model = select_doc_model(doc_path)
    updated_content, model = generate_validated_markdown(
        doc_path,
        build_doc_prompt(doc_path, current_doc, source_excerpt, project_context, index_content, readme_content, docs_mode),
        model,
    )
    write_text(doc_path, updated_content)
    return UpdatedDocument(path=doc_path, model=model)


def main() -> None:
    load_local_configuration()
    started_at = datetime.now(timezone.utc)
    branch_name = get_branch_name()
    changed_files = list_changed_files_for_ci() if is_ci() else []
    ci_commits = list_ci_commits() if is_ci() else []
    all_docs = [path for path in CATALOG.list_existing_docs() if path not in {CATALOG.index_path, CATALOG.project_context_path}]
    all_service_files = list_service_source_files()
    target_docs, docs_mode = determine_target_docs(changed_files, all_docs)
    service_sources, source_mode = determine_service_sources(changed_files, all_service_files)
    facts_by_service = {service.name: EXTRACTOR.build_service_facts(service) for service in CATALOG.services}
    updated_docs: list[UpdatedDocument] = []
    skipped_items: list[str] = []
    project_context = read_text(CATALOG.project_context_path, "Project context file not found.")
    index_content = read_text(CATALOG.index_path, "Index file not found.")
    service_readmes = {service.readme_path for service in CATALOG.services}

    for doc_path in target_docs:
        if doc_path in {CATALOG.index_path, CATALOG.project_context_path} or doc_path in service_readmes:
            continue
        try:
            updated_doc = update_document(
                doc_path=doc_path,
                service_sources=service_sources,
                facts_by_service=facts_by_service,
                target_docs=target_docs,
                project_context=project_context,
                index_content=index_content,
                readme_content=get_readme_context_for_doc(doc_path),
                docs_mode=docs_mode,
            )
            updated_docs.append(updated_doc)
            print(f"Updated {doc_path.relative_to(REPO_ROOT)} with {updated_doc.model}")
        except Exception as exc:
            skipped_items.append(f"{doc_path.relative_to(REPO_ROOT)}: {exc}")
            print(f"Skipped {doc_path.relative_to(REPO_ROOT)}: {exc}")

    for service in CATALOG.services:
        try:
            updated_doc = update_document(
                doc_path=service.readme_path,
                service_sources=service_sources,
                facts_by_service=facts_by_service,
                target_docs=target_docs,
                project_context=project_context,
                index_content=index_content,
                readme_content=get_readme_context_for_doc(service.readme_path),
                docs_mode=docs_mode,
            )
            updated_docs.append(updated_doc)
            print(f"Updated {service.readme_path.relative_to(REPO_ROOT)} with {updated_doc.model}")
        except Exception as exc:
            skipped_items.append(f"{service.readme_path.relative_to(REPO_ROOT)}: {exc}")
            print(f"Skipped {service.readme_path.relative_to(REPO_ROOT)}: {exc}")

    try:
        index_doc = update_document(
            doc_path=CATALOG.index_path,
            service_sources=service_sources,
            facts_by_service=facts_by_service,
            target_docs=target_docs,
            project_context=project_context,
            index_content=index_content,
            readme_content="No service README context available.",
            docs_mode=docs_mode,
        )
        updated_docs.append(index_doc)
        print(f"Updated {CATALOG.index_path.relative_to(REPO_ROOT)} with {index_doc.model}")
    except Exception as exc:
        skipped_items.append(f"{CATALOG.index_path.relative_to(REPO_ROOT)}: {exc}")
        print(f"Skipped {CATALOG.index_path.relative_to(REPO_ROOT)}: {exc}")

    ended_at = datetime.now(timezone.utc)
    report_paths = write_script_report(
        reports_root=CATALOG.reports_root,
        branch_name=branch_name,
        script_name="generate_docs.py",
        started_at=started_at,
        ended_at=ended_at,
        intro="This run refreshed the compact documentation set, using deterministic renderers where possible and falling back to model-backed updates only when needed.",
        summary_lines=[
            f"Docs mode: `{docs_mode}`",
            f"Source mode: `{source_mode}`",
            f"Updated pages: `{len(updated_docs)}`",
            f"Skipped items: `{len(skipped_items)}`",
            f"Simple model: `{get_simple_model()}`",
            f"Complex model: `{get_complex_model()}`",
        ],
        sections=[
            ("Changed Files", format_path_list(changed_files, REPO_ROOT, limit=100)),
            ("Pipeline Commits", format_commit_inventory(ci_commits) if is_ci() else "- not applicable in local mode"),
            ("Target Documentation Pages", format_path_list(target_docs, REPO_ROOT, limit=100)),
            ("Service Source Files Used", format_source_inventory(service_sources, REPO_ROOT)),
            (
                "Updated Pages",
                "\n".join(f"- updated `{item.path.relative_to(REPO_ROOT)}` using `{item.model}`" for item in updated_docs) or "- none",
            ),
            ("Skipped Items", "\n".join(f"- {item}" for item in skipped_items) or "- none"),
            (
                "Notes",
                "\n".join(
                    [
                        "- This run updates the real `docs/` structure instead of creating per-class documentation.",
                        "- `docs/knowledge/project-context.md` is treated as read-only context by this script.",
                        "- `docs/knowledge/index.md` and service `README.md` files are refreshed as part of the cycle.",
                        "- Services are discovered from repository folders matching `*-service`.",
                    ]
                ),
            ),
        ],
        changed_paths=[item.path for item in updated_docs],
        repo_root=REPO_ROOT,
    )
    print(f"Report written to {report_paths.latest_path.relative_to(REPO_ROOT)}")


if __name__ == "__main__":
    main()
