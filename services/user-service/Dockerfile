FROM eclipse-temurin:21-jdk AS build

WORKDIR /app/services/user-service

COPY pom.xml /app/pom.xml
COPY services/user-service/.mvn/ .mvn/
COPY services/user-service/mvnw services/user-service/pom.xml ./

RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline

COPY services/user-service/src/ src/

RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/services/user-service/target/*.jar app.jar
RUN useradd --system --create-home --uid 10001 appuser
USER 10001:10001

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
