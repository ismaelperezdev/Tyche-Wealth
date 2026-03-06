# Tyche-Wealth - User Service

Este es el servicio de usuarios de **Tyche-Wealth**, basado en **Spring Boot** y **Java 21**.

## Requisitos

- Java 21 instalado (`java -version`)
- Maven Wrapper incluido en el repo (`mvnw` / `mvnw.cmd`)

## Como ejecutar

```bash
cd user-service
./mvnw spring-boot:run
```

En Windows (PowerShell o CMD):

```powershell
cd user-service
.\mvnw.cmd spring-boot:run
```

La aplicacion arrancara por defecto en `http://localhost:8080`.

Prueba el endpoint de ejemplo:

- `GET http://localhost:8080/hello`

Deberias ver:

```text
Hola desde Spring Boot con Java 21!
```
