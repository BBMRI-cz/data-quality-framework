FROM node:26.7.0@sha256:bde0dae02f2b12d2bce5ee72b2432f0e511767b7b2dc4dd3b064df11ae422fee AS frontend-builder
WORKDIR /app/frontend
COPY server/frontend/package*.json ./
RUN npm ci --silent
COPY server/frontend/ .
RUN npm run build --silent

FROM maven:3-eclipse-temurin-24-noble@sha256:a137a467ec89b5713d0be817b55bdba6b4d6ef16e3d05565a79bc08d8e775a1c AS backend-builder
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

FROM eclipse-temurin:25-jre-resolute@sha256:17ddcc32b1f296155738c4b2f689208e46bf0cf4eb0b215c717717deb8b74b90
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl ca-certificates && \
    rm -rf /var/lib/apt/lists/*
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
