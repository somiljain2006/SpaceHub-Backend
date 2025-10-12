FROM maven:3.8.5-openjdk-17 AS build
COPY . .
RUN mvn clean package -DskipTests

FROM openjdk:17.0.1-jdk-slim
EXPOSE 8080
COPY --from=build target/spaceHub.jar spaceHub.jar
ENTRYPOINT ["java","-jar","/spaceHub.jar"]