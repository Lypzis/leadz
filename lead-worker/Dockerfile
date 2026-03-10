# Build stage: compile the Spring Boot application
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache dependencies for the lead-worker reactor slice.
COPY pom.xml pom.xml
COPY lead-contracts/pom.xml lead-contracts/pom.xml
COPY lead-worker/pom.xml lead-worker/pom.xml
COPY lead-api/pom.xml lead-api/pom.xml
RUN mvn -B -ntp -f pom.xml -pl lead-worker -am dependency:go-offline

# Build lead-worker (and required shared module)
COPY lead-contracts/src lead-contracts/src
COPY lead-worker/src lead-worker/src
RUN mvn -B -ntp -f pom.xml -pl lead-worker -am clean package -DskipTests

# Runtime stage: run the fat jar
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the built jar from the builder image
COPY --from=build /app/lead-worker/target/*.jar app.jar

ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
