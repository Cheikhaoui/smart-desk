# ── Stage 1: build ───────────────────────────────────────────────────────────
# Use the official Maven image — Maven is pre-installed, no wrapper JVM download.
# --platform=$BUILDPLATFORM ensures compilation runs natively on the host CPU
# (ARM64 on Apple Silicon, amd64 on CI) since Java bytecode is platform-independent.
FROM --platform=$BUILDPLATFORM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

ENV MAVEN_OPTS="-Xmx512m"

# Resolve dependencies first — this layer is cached until pom.xml changes
COPY pom.xml ./
RUN mvn dependency:go-offline -B

COPY src/ src/
RUN mvn package -DskipTests -B

# Extract Spring Boot layered JAR for optimized runtime image layers
RUN java -Djarmode=layertools -jar target/*.jar extract --destination target/extracted

# ── Stage 2: runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre
WORKDIR /app

# Never run a container as root
RUN addgroup --system app && adduser --system --ingroup app app
USER app

# Copy layers in order of change frequency (least → most frequent)
COPY --from=build /app/target/extracted/dependencies/ ./
COPY --from=build /app/target/extracted/spring-boot-loader/ ./
COPY --from=build /app/target/extracted/snapshot-dependencies/ ./
COPY --from=build /app/target/extracted/application/ ./

EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
