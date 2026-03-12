from __future__ import annotations

import re
from pathlib import Path

from doc_models import DtoContract, DtoFieldConstraint, EndpointInfo, EntityField, EntityInfo, EntityRelation, ServiceDefinition, ServiceFacts


class ServiceFactsExtractor:
    SUPPORTED_SOURCE_SUFFIXES = {".java", ".json", ".md", ".properties", ".sql", ".xml", ".yaml", ".yml"}

    def __init__(self, repo_root: Path) -> None:
        self.repo_root = repo_root

    def list_source_files(self, service: ServiceDefinition) -> list[Path]:
        files: list[Path] = []
        for path in service.root.rglob("*"):
            if not path.is_file():
                continue
            if "target" in path.parts or ".idea" in path.parts or path.name == "application-local.properties":
                continue
            if path.suffix.lower() not in self.SUPPORTED_SOURCE_SUFFIXES:
                continue
            files.append(path)
        for extra in (service.root / "pom.xml", service.readme_path):
            if extra.exists():
                files.append(extra)
        return sorted(set(files))

    def build_service_facts(self, service: ServiceDefinition) -> ServiceFacts:
        return ServiceFacts(
            service=service,
            application_properties=self._read_properties(service.root / "src" / "main" / "resources" / "application.properties"),
            java_constants=self._extract_java_constants(next(service.root.rglob("ApiConstants.java"), None)),
            endpoints=self._extract_endpoints(service),
            entities=self._extract_entities(service),
            dto_contracts=self._extract_dto_contracts(service),
        )

    def _read_text(self, path: Path | None) -> str:
        if not path or not path.exists():
            return ""
        return path.read_text(encoding="utf-8")

    def _read_properties(self, path: Path) -> dict[str, str]:
        props: dict[str, str] = {}
        for raw_line in self._read_text(path).splitlines():
            line = raw_line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, value = line.split("=", 1)
            props[key.strip()] = value.strip()
        return props

    def _extract_java_constants(self, path: Path | None) -> dict[str, str]:
        constants: dict[str, str] = {}
        for name, expr in re.findall(r'public static final String\s+(\w+)\s*=\s*(.+?);', self._read_text(path)):
            resolved = expr.strip()
            for const_name, const_value in sorted(constants.items(), key=lambda item: len(item[0]), reverse=True):
                resolved = resolved.replace(const_name, f'"{const_value}"')
            parts = [part.strip().strip('"') for part in resolved.split("+")]
            constants[name] = "".join(parts)
        return constants

    def _extract_endpoints(self, service: ServiceDefinition) -> list[EndpointInfo]:
        endpoints: list[EndpointInfo] = []
        constants = self._extract_java_constants(next(service.root.rglob("ApiConstants.java"), None))
        for api_path in sorted(service.root.rglob("*Api.java")):
            content = self._read_text(api_path)
            request_mapping_match = re.search(r'@RequestMapping\(value\s*=\s*(.+?)\)', content)
            base_expr = request_mapping_match.group(1).strip() if request_mapping_match else '""'
            for const_name, const_value in sorted(constants.items(), key=lambda item: len(item[0]), reverse=True):
                base_expr = base_expr.replace(const_name, f'"{const_value}"')
            base_path = "".join(part.strip().strip('"') for part in base_expr.split("+"))

            pattern = re.compile(
                r'@(?P<verb>Post|Get|Put|Delete)Mapping\(value\s*=\s*"(?P<path>[^"]+)"[^)]*\)\s+ResponseEntity<(?P<response>\w+)>\s+'
                r'(?P<method>\w+)\((?P<params>.*?)\);',
                re.MULTILINE | re.DOTALL,
            )
            for match in pattern.finditer(content):
                params = match.group("params")
                request_match = re.search(r'@RequestBody\s+(?P<request>\w+)', params)
                endpoints.append(
                    EndpointInfo(
                        http_method=match.group("verb").upper(),
                        path=base_path + match.group("path"),
                        request_type=request_match.group("request") if request_match else None,
                        response_type=match.group("response"),
                        source_api=api_path.name,
                    )
                )
        return endpoints

    def _extract_entities(self, service: ServiceDefinition) -> list[EntityInfo]:
        entities: list[EntityInfo] = []
        for path in sorted(service.root.rglob("*Entity.java")):
            content = self._read_text(path)
            class_match = re.search(r"class\s+(\w+)", content)
            table_match = re.search(r'@Table\(name\s*=\s*"([^"]+)"\)', content)
            if not class_match or not table_match:
                continue
            fields: list[EntityField] = []
            relations: list[EntityRelation] = []
            pending_relation: str | None = None
            pending_join_column: str | None = None
            pending_column_name: str | None = None
            pending_nullable = True

            for raw_line in content.splitlines():
                line = raw_line.strip()
                if line.startswith("@ManyToOne"):
                    pending_relation = "many-to-one"
                elif line.startswith("@OneToMany"):
                    pending_relation = "one-to-many"
                elif line.startswith("@JoinColumn"):
                    join_match = re.search(r'name\s*=\s*"([^"]+)"', line)
                    nullable_match = re.search(r'nullable\s*=\s*(true|false)', line)
                    pending_join_column = join_match.group(1) if join_match else None
                    pending_nullable = nullable_match.group(1) != "false" if nullable_match else True
                elif line.startswith("@Column"):
                    name_match = re.search(r'name\s*=\s*"([^"]+)"', line)
                    nullable_match = re.search(r'nullable\s*=\s*(true|false)', line)
                    pending_column_name = name_match.group(1) if name_match else None
                    pending_nullable = nullable_match.group(1) != "false" if nullable_match else True
                elif line.startswith("private "):
                    match = re.match(r'private\s+([\w<>]+)\s+(\w+)(?:\s*=\s*[^;]+)?;', line)
                    if not match:
                        continue
                    type_name = match.group(1)
                    generic_target = None
                    generic_match = re.search(r"<(\w+)>", type_name)
                    if generic_match:
                        generic_target = generic_match.group(1)
                    if pending_relation:
                        relations.append(
                            EntityRelation(
                                relation_type=pending_relation,
                                target_entity=generic_target or type_name.replace(">", "").split("<")[-1],
                                join_column=pending_join_column,
                            )
                        )
                    elif pending_column_name:
                        fields.append(
                            EntityField(
                                name=pending_column_name,
                                type_name=type_name,
                                nullable=pending_nullable,
                            )
                        )
                    pending_relation = None
                    pending_join_column = None
                    pending_column_name = None
                    pending_nullable = True

            entities.append(
                EntityInfo(
                    class_name=class_match.group(1),
                    table_name=table_match.group(1),
                    fields=fields,
                    relations=relations,
                )
            )
        return entities

    def _extract_dto_contracts(self, service: ServiceDefinition) -> dict[str, DtoContract]:
        contracts: dict[str, DtoContract] = {}
        for path in sorted(service.root.rglob("*RequestDto.java")):
            content = self._read_text(path)
            class_match = re.search(r"class\s+(\w+)", content)
            if not class_match:
                continue
            class_name = class_match.group(1)
            field_constraints: list[DtoFieldConstraint] = []
            field_pattern = re.compile(
                r"(?P<annotations>(?:\s*@[\s\S]*?\n)+)\s*private\s+[\w<>]+\s+(?P<field>\w+)(?:\s*=\s*[^;]+)?;",
                re.MULTILINE,
            )
            for match in field_pattern.finditer(content):
                field_name = match.group("field")
                annotation_block = match.group("annotations")
                rules = self._annotation_rules(annotation_block)
                normalization = self._normalization_rule(content, field_name)
                field_constraints.append(
                    DtoFieldConstraint(
                        field_name=field_name,
                        rules=rules,
                        normalization=normalization,
                    )
                )

            cross_field_rules = self._cross_field_rules(content)
            contracts[class_name] = DtoContract(
                name=class_name,
                field_constraints=field_constraints,
                cross_field_rules=cross_field_rules,
            )
        return contracts

    def _annotation_rules(self, annotation_block: str) -> list[str]:
        rules: list[str] = []
        for annotation in re.findall(r"@\w+(?:\([\s\S]*?\))?", annotation_block):
            message_match = re.search(r'message\s*=\s*"([^"]+)"', annotation)
            message = message_match.group(1) if message_match else None
            if annotation.startswith("@NotBlank"):
                rules.append(message or "Must not be blank.")
            elif annotation.startswith("@Email"):
                rules.append(message or "Must be a valid email address.")
            elif annotation.startswith("@Size"):
                min_match = re.search(r"min\s*=\s*(\d+)", annotation)
                max_match = re.search(r"max\s*=\s*(\d+)", annotation)
                if message:
                    rules.append(message)
                elif min_match and max_match:
                    rules.append(f"Length must be between {min_match.group(1)} and {max_match.group(1)} characters.")
                elif min_match:
                    rules.append(f"Length must be at least {min_match.group(1)} characters.")
                elif max_match:
                    rules.append(f"Length must be at most {max_match.group(1)} characters.")
            elif annotation.startswith("@Pattern"):
                rules.append(message or "Must match the configured format policy.")
        return rules

    def _normalization_rule(self, content: str, field_name: str) -> str | None:
        setter_pattern = re.compile(
            rf"set{field_name[:1].upper()}{field_name[1:]}\s*\(\s*String\s+{field_name}\s*\)\s*\{{(?P<body>.*?)\}}",
            re.DOTALL,
        )
        setter_match = setter_pattern.search(content)
        if not setter_match:
            return None
        if "Utils.normalizeIdentity" in setter_match.group("body"):
            return "Value is normalized before downstream validation and persistence checks."
        return None

    def _cross_field_rules(self, content: str) -> list[str]:
        rules: list[str] = []
        for message in re.findall(r'@AssertTrue\(message\s*=\s*"([^"]+)"\)', content):
            rules.append(message)
        return rules
