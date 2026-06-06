# ---- build stage ----
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
# Resolve dependencies first for better layer caching.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -q -DskipTests dependency:go-offline
COPY src ./src
RUN ./mvnw -B -q -DskipTests clean package

# ---- runtime stage ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
