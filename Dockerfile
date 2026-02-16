# STAGE 1: Build the application
# We use a full Maven image to build the project
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy the pom.xml and the maven wrapper
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Grant execution permission to the maven wrapper
RUN chmod +x mvnw

# Download dependencies (this layer is cached)
RUN ./mvnw dependency:go-offline -B

# Copy source code and build the JAR
COPY src ./src
RUN ./mvnw clean package -DskipTests

# STAGE 2: Run the application
# We use a slim JRE image for the final container to save space
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the JAR from the build stage
# We use a wildcard *.jar to avoid version mismatch errors
COPY --from=build /app/target/*.jar app.jar

# Expose the port (Render uses the PORT env var, but 8080 is standard)
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
