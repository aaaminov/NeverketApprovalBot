FROM openjdk:21-jdk-slim AS build

WORKDIR /app

COPY pom.xml .
COPY .mvn/ .mvn
COPY mvnw .
COPY src ./src

RUN ./mvnw package -DskipTests

FROM openjdk:21-jdk-slim

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

CMD ["java", "-jar", "app.jar"]