# Use an official OpenJDK runtime as the base image
FROM openjdk:21-jdk-slim

# Set the working directory in the container
WORKDIR /app

# Copy the JAR file into the container (adjust path as needed)
COPY target/wallet-service.jar wallet-service.jar

# Expose the port the app runs on
EXPOSE 8080

# Run the JAR file
ENTRYPOINT ["java", "-jar", "/app/wallet-service.jar"]