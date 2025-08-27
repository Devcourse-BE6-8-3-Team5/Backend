FROM openjdk:21-jdk-slim
WORKDIR /app

COPY . .
RUN chmod +x gradlew
RUN ./gradlew bootJar -x test --no-daemon

EXPOSE 8080
ENTRYPOINT ["/usr/local/openjdk-21/bin/java", "-Dspring.profiles.active=prod", "-jar", "build/libs/Backend-0.0.1-SNAPSHOT.jar"]