from __future__ import annotations

from pathlib import Path

from doc_models import ServiceDefinition


class ServiceCatalog:
    DOC_TYPE_TO_TEMPLATE = {
        "service-readme": "service-readme.md.tpl",
        "service-overview": "service-overview.md.tpl",
        "service-api": "service-api.md.tpl",
        "service-data-model": "service-data-model.md.tpl",
        "service-runtime": "service-runtime.md.tpl",
        "architecture-system": "architecture-system.md.tpl",
        "database-overview": "database-overview.md.tpl",
        "docs-index": "index.md.tpl",
        "project-context": "project-context.md.tpl",
        "generic-doc": "generic-doc.md.tpl",
    }

    STATIC_DOC_TARGETS = (
        Path("docs/knowledge/architecture/system.md"),
        Path("docs/knowledge/database/overview.md"),
    )

    def __init__(self, repo_root: Path) -> None:
        self.repo_root = repo_root
        self.docs_root = repo_root / "docs" / "knowledge"
        self.templates_root = repo_root / "scripts" / "templates"
        self.index_path = self.docs_root / "index.md"
        self.project_context_path = self.docs_root / "project-context.md"
        self.reports_root = self.docs_root / "reports"
        self.docs_excluded_prefixes = {
            self.docs_root / "generated",
            self.docs_root / "reports",
        }
        self.services = self._discover_services()

    def _discover_services(self) -> list[ServiceDefinition]:
        services: list[ServiceDefinition] = []
        for path in self.repo_root.iterdir():
            if not path.is_dir() or path.name.startswith(".") or not path.name.endswith("-service"):
                continue
            if not ((path / "src").exists() or (path / "pom.xml").exists()):
                continue
            services.append(
                ServiceDefinition(
                    name=path.name,
                    root=path,
                    readme_path=path / "README.md",
                    local_properties_path=path / "application-local.properties",
                )
            )
        return sorted(services, key=lambda item: item.name)

    def list_existing_docs(self) -> list[Path]:
        docs: list[Path] = []
        for path in self.docs_root.rglob("*.md"):
            if any(path.is_relative_to(prefix) for prefix in self.docs_excluded_prefixes):
                continue
            docs.append(path)
        return sorted(docs)

    def list_default_target_docs(self) -> list[Path]:
        targets = [self.repo_root / relative_path for relative_path in self.STATIC_DOC_TARGETS]
        for service in self.services:
            targets.extend(
                [
                    self.docs_root / "services" / service.name / "readme.md",
                    self.docs_root / "services" / service.name / "overview.md",
                    self.docs_root / "services" / service.name / "api.md",
                    self.docs_root / "services" / service.name / "data-model.md",
                    self.docs_root / "services" / service.name / "runtime.md",
                ]
            )
        return sorted(set(targets))

    def get_service_for_doc(self, doc_path: Path) -> ServiceDefinition | None:
        for service in self.services:
            if doc_path == service.readme_path:
                return service
        try:
            relative = doc_path.relative_to(self.docs_root)
        except ValueError:
            return None
        if len(relative.parts) >= 3 and relative.parts[0] == "services":
            for service in self.services:
                if service.name == relative.parts[1]:
                    return service
        return self.services[0] if self.services else None

    def infer_doc_type(self, doc_path: Path) -> str:
        service = self.get_service_for_doc(doc_path)
        if service and doc_path == service.readme_path:
            return "service-readme"
        if doc_path == self.index_path:
            return "docs-index"
        if doc_path == self.project_context_path:
            return "project-context"
        try:
            relative = doc_path.relative_to(self.docs_root).as_posix().lower()
        except ValueError:
            return "generic-doc"
        if relative.startswith("services/") and relative.endswith("/readme.md"):
            return "service-readme"
        if relative.startswith("services/") and relative.endswith("/overview.md"):
            return "service-overview"
        if relative.startswith("services/") and relative.endswith("/api.md"):
            return "service-api"
        if relative.startswith("services/") and relative.endswith("/data-model.md"):
            return "service-data-model"
        if relative.startswith("services/") and relative.endswith("/runtime.md"):
            return "service-runtime"
        if relative == "architecture/system.md":
            return "architecture-system"
        if relative == "database/overview.md":
            return "database-overview"
        return "generic-doc"

    def get_template(self, doc_type: str) -> str:
        template_name = self.DOC_TYPE_TO_TEMPLATE.get(doc_type, self.DOC_TYPE_TO_TEMPLATE["generic-doc"])
        path = self.templates_root / template_name
        if path.exists():
            return path.read_text(encoding="utf-8")
        return f"# {{{{ title }}}}\n\nMissing template for `{doc_type}`.\n"
