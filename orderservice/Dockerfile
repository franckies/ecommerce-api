# Dockerfile
# create a application.yaml file in the same dir
FROM openjdk:11-jdk-slim
ARG JAR_FILE
COPY ${JAR_FILE} app.jar
COPY application.yaml .
ENTRYPOINT ["java","-jar","/app.jar"]