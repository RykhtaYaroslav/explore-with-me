FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -pl infra/config-server -am -DskipTests package

FROM amazoncorretto:21-alpine
COPY --from=build /workspace/infra/config-server/target/config-server-*.jar /app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
