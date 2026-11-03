# syntax=docker/dockerfile:1

# ---- Build: compile and package with the Maven wrapper on a full JDK ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Resolve dependencies in their own layer so source changes don't re-download them
COPY .mvn/ .mvn/
# mvnw is committed without the executable bit, which Linux builders keep
COPY --chmod=0755 mvnw ./
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:go-offline

COPY src/ src/
# Tests run in CI; the image build only packages
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q package -DskipTests \
    && cp target/ecommerce-api-*.jar application.jar

# Split the fat jar into Spring Boot layers (dependencies change less often than code)
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# ---- Runtime: JRE only, non-root ----
FROM eclipse-temurin:21-jre

RUN groupadd --system --gid 10001 spring \
    && useradd --system --uid 10001 --gid spring --no-create-home --shell /usr/sbin/nologin spring

WORKDIR /application
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

USER 10001:10001

# Size the heap from the container memory limit rather than the host's RAM
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "application.jar"]
