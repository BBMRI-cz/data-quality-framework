FROM node:26-alpine AS frontend-builder
WORKDIR /app/frontend
COPY server/frontend/package*.json ./
RUN npm install --silent
COPY server/frontend/ .
RUN npm run build --silent

FROM maven:3.9.9-eclipse-temurin-21@sha256:3a4ab3276a087bf276f79cae96b1af04f53731bec53fb2e651aca79e4b10211e AS backend-builder
ARG ARTIFACT_VERSION=unknown
WORKDIR /app
COPY .git ./.git
WORKDIR /app/backend
COPY server/backend/pom.xml .
RUN mvn dependency:go-offline --quiet
COPY server/backend/src ./src
COPY --from=frontend-builder /app/frontend/dist ./src/main/resources/static
RUN mvn --quiet -B versions:set -DnewVersion=$ARTIFACT_VERSION
RUN mvn package --quiet -DskipTests

FROM eclipse-temurin:25-jre-jammy@sha256:c3e62cd0cece58d8de8d760ab95a5014f3b5a6ea32178f54270edb5b4aab9d1f
RUN groupadd -r appuser && useradd -r -g appuser -u 1001 appuser
WORKDIR /app
RUN mkdir -p /app/data && \
    chown -R appuser:appuser /app/data && \
    chmod 700 /app/data
ENV SPRING_PROFILES_ACTIVE=prod
HEALTHCHECK --interval=10s --timeout=10s CMD curl -f http://localhost:8082/api/health || exit 1
COPY --chown=appuser:appuser --from=backend-builder /app/backend/target/app.jar app.jar
USER appuser
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]