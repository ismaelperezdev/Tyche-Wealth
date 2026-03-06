# Tyche-Wealth - User Service

Este es el servicio de usuarios de **Tyche-Wealth**, basado en **Spring Boot** y **Java 21**.

## Requisitos

- Java 21 instalado (`java -version`)
- Maven instalado (`mvn -version`)

## Como ejecutar

```bash
cd user-service
mvn spring-boot:run
```

La aplicacion arrancara por defecto en `http://localhost:8080`.

Prueba el endpoint de ejemplo:

- `GET http://localhost:8080/hello`

Deberias ver:

```text
Hola desde Spring Boot con Java 21!
```
