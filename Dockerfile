# Stage 1: Build the application using Maven
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Run the application using a lightweight JDK image
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
COPY --from=builder /app/target/automation-0.0.1-SNAPSHOT.jar app.jar

# Set the default port Render uses
ENV PORT=8080
EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
