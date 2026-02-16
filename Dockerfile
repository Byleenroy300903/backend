# Use an official Java runtime as a parent image
FROM eclipse-temurin:17-jdk-alpine

# Set the working directory in the container
WORKDIR /app

# Copy the pom.xml and the maven wrapper to install dependencies
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline

# Copy the rest of the code and build the application
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Run the jar file
ENTRYPOINT ["java","-jar","target/backend-0.0.1-SNAPSHOT.jar"]
