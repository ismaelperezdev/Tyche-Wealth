FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY libraries/redis-support/pom.xml libraries/redis-support/pom.xml
COPY services/user-service/pom.xml services/user-service/pom.xml
COPY services/portfolio-service/pom.xml services/portfolio-service/pom.xml
COPY services/market-data-service/pom.xml services/market-data-service/pom.xml

RUN chmod +x mvnw
RUN ./mvnw -pl services/portfolio-service -am dependency:go-offline

COPY libraries/redis-support/src/ libraries/redis-support/src/
COPY services/portfolio-service/src/ services/portfolio-service/src/

RUN ./mvnw -pl services/portfolio-service -am clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/services/portfolio-service/target/*.jar app.jar
RUN useradd --system --create-home --uid 10001 appuser
USER 10001:10001

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
