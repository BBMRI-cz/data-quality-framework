FROM node:20-alpine AS frontend-builder
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm install --silent
COPY frontend/ .
RUN npm run build --silent

FROM maven:3.9.9-eclipse-temurin-21 AS backend-builder
ARG ARTIFACT_VERSION=unknown
WORKDIR /app/backend
COPY backend/pom.xml .
RUN mvn dependency:go-offline --quiet
COPY backend/src ./src
COPY --from=frontend-builder /app/frontend/dist ./src/main/resources/static
RUN mvn --quiet -B versions:set -DnewVersion=$ARTIFACT_VERSION
RUN mvn package --quiet -DskipTests

FROM eclipse-temurin:21-jre-jammy@sha256:daebe9ae03913ec4b2dadd8df60f3ea3df1aa6108fecd5d324d000bdd5c4c816
WORKDIR /app
ENV SPRING_PROFILES_ACTIVE=prod
HEALTHCHECK --interval=10s --timeout=10s CMD curl -f http://localhost:8082/api/health || exit 1
COPY --from=backend-builder /app/backend/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]