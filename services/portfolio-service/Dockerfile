FROM eclipse-temurin:21-jdk AS build

WORKDIR /app/services/portfolio-service

COPY pom.xml /app/pom.xml
COPY services/portfolio-service/.mvn/ .mvn/
COPY services/portfolio-service/mvnw services/portfolio-service/pom.xml ./

RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline

COPY services/portfolio-service/src/ src/

RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/services/portfolio-service/target/*.jar app.jar
RUN useradd --system --create-home --uid 10001 appuser
USER 10001:10001

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
