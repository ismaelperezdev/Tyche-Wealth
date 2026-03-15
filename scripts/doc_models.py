from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class ServiceDefinition:
    name: str
    root: Path
    readme_path: Path
    local_properties_path: Path


@dataclass(frozen=True)
class UpdatedDocument:
    path: Path
    model: str


@dataclass(frozen=True)
class EntityField:
    name: str
    type_name: str
    nullable: bool


@dataclass(frozen=True)
class EntityRelation:
    relation_type: str
    target_entity: str
    join_column: str | None


@dataclass(frozen=True)
class EntityInfo:
    class_name: str
    table_name: str
    fields: list[EntityField]
    relations: list[EntityRelation]


@dataclass(frozen=True)
class EndpointInfo:
    http_method: str
    path: str
    request_type: str | None
    response_type: str | None
    source_api: str


@dataclass(frozen=True)
class DtoFieldConstraint:
    field_name: str
    rules: list[str]
    normalization: str | None = None


@dataclass(frozen=True)
class DtoContract:
    name: str
    field_constraints: list[DtoFieldConstraint]
    cross_field_rules: list[str]


@dataclass(frozen=True)
class MetricInfo:
    name: str
    description: str


@dataclass(frozen=True)
class ServiceFacts:
    service: ServiceDefinition
    application_properties: dict[str, str]
    java_constants: dict[str, str]
    endpoints: list[EndpointInfo]
    entities: list[EntityInfo]
    dto_contracts: dict[str, DtoContract]
    metrics: list[MetricInfo]
