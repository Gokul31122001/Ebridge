# Use official Maven image to build the app
FROM maven:3.9.5-eclipse-temurin-17 AS build

WORKDIR /app

# Copy the pom.xml and download dependencies (better cache)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy everything else and build
COPY . .
RUN mvn clean package -DskipTests

# Use a lightweight Java runtime for the final image
FROM eclipse-temurin:17-jre

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Expose port 8080 (typical Spring Boot port)
EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
