FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -pl stats/collector -am -DskipTests package

FROM amazoncorretto:21-alpine
COPY --from=build /workspace/stats/collector/target/collector-*.jar /app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
