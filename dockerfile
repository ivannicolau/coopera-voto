# Imagem base com Java 21 (Eclipse Temurin)
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copia o jar da aplicação (construído via "mvn clean package")
COPY target/coopera-voto-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
