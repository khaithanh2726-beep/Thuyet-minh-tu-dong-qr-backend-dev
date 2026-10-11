FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /build
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw
COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
LABEL org.opencontainers.image.source="https://github.com/khaithanh2726-beep/Thuyet-minh-tu-dong-qr-backend-dev"
RUN apk add --no-cache curl && addgroup -S app && adduser -S app -G app \
    && mkdir -p /app/audio_cache && chown -R app:app /app
WORKDIR /app
COPY --from=build --chown=app:app /build/target/qr-backend-0.0.1-SNAPSHOT.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
