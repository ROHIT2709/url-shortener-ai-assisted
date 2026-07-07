# syntax=docker/dockerfile:1

# ---------- Stage 1: build ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Resolve dependencies against the POM alone so this expensive layer is
# reused on every build where pom.xml hasn't changed
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B package -DskipTests

# Explode the fat jar into Spring Boot layers (dependencies change far less
# often than application code, so they cache as separate image layers)
RUN java -Djarmode=layertools -jar target/*.jar extract --destination extracted

# ---------- Stage 2: runtime ----------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Never run as root inside the container
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy layers least-frequently-changed first to maximize cache hits
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

EXPOSE 8080

# Container-aware heap sizing; extend or override per-deployment via env
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
