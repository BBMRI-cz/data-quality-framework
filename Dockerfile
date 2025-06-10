# Stage 1: Build Vue.js frontend
FROM node:20-alpine AS frontend-builder

WORKDIR /app/frontend

# Copy Vue.js project files
COPY modules/frontend/package*.json ./
RUN npm install

COPY modules/frontend/ .
RUN npm run build

# Stage 2: Build Spring Boot backend
FROM maven:3.9.9-eclipse-temurin-21 AS backend-builder
ARG ARTIFACT_VERSION=unknown
WORKDIR /app/backend

# Copy Spring Boot project files
COPY modules/backend/pom.xml .
RUN mvn dependency:go-offline

COPY modules/backend/src ./src
# Copy built Vue.js assets to Spring Boot's static resources
COPY --from=frontend-builder /app/frontend/dist ./src/main/resources/static
RUN mvn --quiet -B versions:set -DnewVersion=$ARTIFACT_VERSION
RUN mvn package -DskipTests

# Stage 3: Final image
FROM eclipse-temurin:21-jre-jammy@sha256:daebe9ae03913ec4b2dadd8df60f3ea3df1aa6108fecd5d324d000bdd5c4c816

WORKDIR /app

# Copy the built JAR from the backend builder stage
COPY --from=backend-builder /app/backend/target/*.jar app.jar

# Expose the port the app runs on
EXPOSE 8081

# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]