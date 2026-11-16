# ---------- stage 1: build the jar ----------
FROM maven:3.9-eclipse-temurin-17 AS build
ARG SERVICE
WORKDIR /build
COPY . .
RUN mvn -B -q -pl ${SERVICE} package -DskipTests
# ---------- stage 2: small runtime image ----------
FROM eclipse-temurin:17-jre-alpine
ARG SERVICE
ARG PORT=8080
RUN addgroup -S appuser && adduser -S appuser -G appuser
WORKDIR /app
COPY --from=build /build/${SERVICE}/target/*.jar app.jar
RUN mkdir -p /app/uploads && chown -R appuser /app
USER appuser
EXPOSE ${PORT}
ENTRYPOINT ["java", "-jar", "app.jar"]
