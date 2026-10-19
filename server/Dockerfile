FROM node:26.12.0@sha256:1b84b89ada025e3b14543cbb837ffaa1c1b5273b841c5b94dd1a8f9c1d8cf7fe AS frontend-builder
WORKDIR /app/frontend
COPY server/frontend/package*.json ./
RUN npm ci --silent
COPY server/frontend/ .
RUN npm run build --silent

FROM maven:3.9-eclipse-temurin-26-noble@sha256:b2c1ad85954592f9928e84327c65201f308ad9b5d8ed7d5b823717c97bf23fbb AS backend-builder
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
