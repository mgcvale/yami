FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar
COPY .env .env
ENTRYPOINT ["sh", "-c", "java -jar app.jar && source .env && java -Dspring.profiles.active=prod -jar app.jar"]