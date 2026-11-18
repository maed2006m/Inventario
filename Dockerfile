# Imagen del backend (Spring Boot).
#
# Igual que en la guia del laboratorio, el JAR se compila FUERA de Docker
# (./mvnw clean package, etapa Build de Jenkins) y aqui solo se copia.
#
# Diferencias con la guia:
# - eclipse-temurin:21-jre en lugar de openjdk:11-jre-slim: la imagen
#   openjdk esta descontinuada y el proyecto usa Java 21.
# - La aplicacion no corre como root.
FROM eclipse-temurin:21-jre

WORKDIR /app

RUN groupadd --system app && useradd --system --gid app app

COPY target/inventario-0.0.1-SNAPSHOT.jar app.jar

USER app

# Solo se expone dentro de la red de Docker: Nginx (contenedor frontend)
# es quien recibe el trafico de afuera y reenvia /api a este puerto.
EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", "-jar", "app.jar"]
