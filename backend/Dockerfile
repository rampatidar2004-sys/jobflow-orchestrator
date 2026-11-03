FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:25-jre-jammy
WORKDIR /app
RUN useradd --system --create-home jobflow
COPY --from=build /app/target/*.jar app.jar
USER jobflow
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
