# Build stage: compile the Spring Boot application
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache dependencies for the lead-api reactor slice.
COPY pom.xml pom.xml
COPY lead-contracts/pom.xml lead-contracts/pom.xml
COPY lead-api/pom.xml lead-api/pom.xml
COPY lead-worker/pom.xml lead-worker/pom.xml
RUN mvn -B -ntp -f pom.xml -pl lead-api -am dependency:go-offline

# Build lead-api (and required shared module)
COPY lead-contracts/src lead-contracts/src
COPY lead-api/src lead-api/src
RUN mvn -B -ntp -f pom.xml -pl lead-api -am package -DskipTests

# Runtime stage: run the fat jar
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the built jar from the builder image
ARG JAR_FILE=lead-api/target/*.jar
COPY --from=build /app/${JAR_FILE} app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
