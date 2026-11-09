# ==============================================================================
# Stage 1: Kotlin / JVM Backend Builder (Compiles MedShiftKotlinServer.kt)
# ==============================================================================
FROM eclipse-temurin:21-jdk-alpine AS kotlin-builder

ARG KOTLIN_VERSION=2.1.0
WORKDIR /build

RUN apk add --no-cache curl unzip bash && \
    curl -fsSL "https://github.com/JetBrains/kotlin/releases/download/v${KOTLIN_VERSION}/kotlin-compiler-${KOTLIN_VERSION}.zip" -o /tmp/kotlinc.zip && \
    unzip -q /tmp/kotlinc.zip -d /opt && \
    rm /tmp/kotlinc.zip

ENV PATH="/opt/kotlinc/bin:${PATH}"

COPY MedShiftKotlinServer.kt /build/MedShiftKotlinServer.kt

RUN kotlinc /build/MedShiftKotlinServer.kt \
    -include-runtime \
    -jvm-target 21 \
    -d /build/medshift-kotlin-backend.jar

# ==============================================================================
# Stage 2: Production Google Cloud Run Runtime (Eclipse Temurin 21 JRE Alpine)
# ==============================================================================
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

RUN addgroup -S medshift && adduser -S medshift -G medshift

COPY --from=kotlin-builder /build/medshift-kotlin-backend.jar /app/medshift-kotlin-backend.jar

ENV PORT=8080
ENV K_REGION="southamerica-east1"
ENV FIREBASE_PROJECT_ID="gen-lang-client-0265308632"
ENV FIRESTORE_DATABASE_ID="ai-studio-android-medshift-eabb9a2d-89b1-44ad-943c-843ef86c682b"
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

EXPOSE 8080

USER medshift

ENTRYPOINT ["java", "-jar", "/app/medshift-kotlin-backend.jar"]
