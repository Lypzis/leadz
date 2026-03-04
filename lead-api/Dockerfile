# Build stage: compile the Spring Boot application
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

# Build the application
COPY src src
RUN mvn -B -ntp package -DskipTests

# Runtime stage: run the fat jar
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the built jar from the builder image
ARG JAR_FILE=target/*.jar
COPY --from=build /app/${JAR_FILE} app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
