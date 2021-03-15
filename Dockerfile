FROM openjdk:8
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} d1gaming-user-backend.jar
ENTRYPOINT ["java", "-jar", "/d1gaming-user-backend.jar"]
EXPOSE 8080