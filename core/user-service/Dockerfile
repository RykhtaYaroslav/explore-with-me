FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -pl core/user-service -am -DskipTests package

FROM amazoncorretto:21-alpine
COPY --from=build /workspace/core/user-service/target/user-service-*.jar /app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
