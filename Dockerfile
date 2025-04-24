FROM openjdk:17-jdk-slim

WORKDIR /app

COPY pom.xml .
COPY .mvn/ .mvn
COPY mvnw .

RUN ./mvnw dependency:resolve

COPY src ./src

# собираем проект в jar-файл
RUN ./mvnw package -DskipTests

# запускаем
CMD ["java", "-jar", "target/telegram-approval-bot.jar"]
