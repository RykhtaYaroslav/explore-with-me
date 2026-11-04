FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -pl stats/aggregator -am -DskipTests package

FROM amazoncorretto:21-alpine
COPY --from=build /workspace/stats/aggregator/target/aggregator-*.jar /app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
