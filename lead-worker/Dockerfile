# Build stage: compile the Spring Boot application
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache dependencies
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x mvnw
RUN ./mvnw -q -DskipTests dependency:go-offline

# Build the application
COPY src src
RUN ./mvnw -q -DskipTests clean package

# Runtime stage: run the fat jar
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the built jar from the builder image
COPY --from=build /app/target/*.jar app.jar

ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
