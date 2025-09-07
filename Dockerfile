FROM eclipse-temurin:17-jre-alpine
VOLUME /tmp
COPY target/*.jar ItroomTestTask-0.0.1.jar
ENTRYPOINT ["java", "-jar", "ItroomTestTask-0.0.1.jar"]