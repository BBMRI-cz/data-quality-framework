FROM node:26.8@sha256:e961046fec20896e8904f2b4a8b4c7e5ca91826d84d8d33d83dbaa61f942069e AS frontend-builder
WORKDIR /app/frontend
COPY agent/frontend/package*.json ./
RUN npm ci --silent
COPY agent/frontend/ .
RUN npm run build --silent

FROM maven:3-eclipse-temurin-26-noble@sha256:2bc6924d954a6efefde0a9629228914acf72e4abcdb927b68f19edd1135a0cb1 AS backend-builder
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

FROM eclipse-temurin:25-jre-resolute@sha256:23adeb62d27f7fefcfe6b81f6f1e3809539159f70a435ba70ac14c192a8cb75e
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
