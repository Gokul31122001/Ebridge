# Use OpenJDK 17 as base image
FROM eclipse-temurin:17-jdk

# Create a directory for the app
WORKDIR /app

# Copy the JAR file into the container
COPY target/automation-0.0.1-SNAPSHOT.jar app.jar

# Run the JAR
ENTRYPOINT ["java", "-jar", "app.jar"]
