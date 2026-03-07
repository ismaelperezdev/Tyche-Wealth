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

## URLs locales utiles

- App: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Actuator health: `http://localhost:8080/actuator/health`

## Configuracion local (sin exponer credenciales)

El proyecto carga `application-local.properties` de forma opcional.
Ese archivo queda fuera de git y sirve para tus credenciales locales.

Ruta:

- `userRespondeDto-service/application-local.properties`

Ejemplo:

```properties
DB_URL=jdbc:postgresql://localhost:5432/userdb
DB_USER=tu_usuario
DB_PASSWORD=tu_password
```
