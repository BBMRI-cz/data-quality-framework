FROM node:26.7.0@sha256:bde0dae02f2b12d2bce5ee72b2432f0e511767b7b2dc4dd3b064df11ae422fee AS frontend-builder
WORKDIR /app/frontend
COPY agent/frontend/package*.json ./
RUN npm ci --silent
COPY agent/frontend/ .
RUN npm run build --silent

FROM maven:3-eclipse-temurin-26-noble@sha256:6206ae5e460fbc803743b53addc31c5caca04582cf6a99f0f91df29c54954b52 AS backend-builder
ARG ARTIFACT_VERSION=unknown
WORKDIR /app
COPY .git ./.git
WORKDIR /app/backend
COPY agent/backend/pom.xml .
RUN mvn dependency:go-offline --quiet
COPY agent/backend/src ./src
COPY --from=frontend-builder /app/frontend/dist ./src/main/resources/static
RUN mvn --quiet -B versions:set -DnewVersion=$ARTIFACT_VERSION
RUN mvn package --quiet -DskipTests

FROM eclipse-temurin:25-jre-resolute@sha256:dfb5912013299daafe722565f6ce6b019e96d5b396455b7e47c811ea03e21668
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl ca-certificates && \
    rm -rf /var/lib/apt/lists/*
RUN groupadd -r appuser && useradd -r -g appuser -u 1001 appuser
WORKDIR /app
RUN mkdir -p /app/data && \
    chown -R appuser:appuser /app/data && \
    chmod 700 /app/data
ENV SPRING_PROFILES_ACTIVE=prod
HEALTHCHECK --interval=10s --timeout=10s CMD curl -f http://localhost:8081/api/health || exit 1
COPY --chown=appuser:appuser --from=backend-builder /app/backend/target/app.jar app.jar
USER appuser
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
