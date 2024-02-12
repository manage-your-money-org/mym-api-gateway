FROM eclipse-temurin:17-jdk-alpine
COPY build/libs/*.jar mym-api-gateway.jar
ENTRYPOINT ["java", "-jar", "/mym-api-gateway.jar"]