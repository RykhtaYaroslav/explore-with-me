FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -pl core/event-service -am -DskipTests package

FROM amazoncorretto:21-alpine
COPY --from=build /workspace/core/event-service/target/event-service-*.jar /app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
