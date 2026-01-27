# Multi-stage Docker build for Spring Boot (WebFlux) app

# 1) Build stage: compile the project and produce a runnable JAR
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy only pom first to leverage Docker layer caching
COPY pom.xml ./

# Copy sources
COPY src ./src

# Build the application (skip tests for faster image builds)
RUN mvn -B -DskipTests package


# 2) Runtime stage: run the built JAR on a lightweight JRE
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy JAR from the build stage (the artifact name is defined in pom.xml)
COPY --from=build /app/target/*-SNAPSHOT.jar /app/app.jar

# Expose the default Spring Boot port
EXPOSE 8080

# Optional JVM flags can be passed via JAVA_OPTS env var at runtime
ENV JAVA_OPTS=""

# Run the application
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
