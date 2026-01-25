FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy Gradle wrapper and build files first
COPY gradle gradle
COPY gradlew build.gradle.kts settings.gradle.kts ./

# Download dependencies (cached unless build.gradle.kts changes)
RUN ./gradlew dependencies --no-daemon

# Now copy source and build
COPY src src
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]