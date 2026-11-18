# Jenkins para el despliegue del inventario (guia, partes 3 a 7).
#
# Imagen oficial con Java 21 (el mismo JDK del proyecto) mas:
# - cliente de Docker y docker compose: los usan las etapas Docker Build
#   y Deploy, y Testcontainers en las pruebas de integracion. Jenkins
#   habla con el Docker del equipo a traves de /var/run/docker.sock.
# - plugins que usa el Jenkinsfile.
FROM jenkins/jenkins:lts-jdk21

USER root
RUN apt-get update \
 && apt-get install -y --no-install-recommends ca-certificates curl gnupg \
 && install -m 0755 -d /etc/apt/keyrings \
 && curl -fsSL https://download.docker.com/linux/debian/gpg -o /etc/apt/keyrings/docker.asc \
 && echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/debian $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
      > /etc/apt/sources.list.d/docker.list \
 && apt-get update \
 && apt-get install -y --no-install-recommends docker-ce-cli docker-compose-plugin \
 && rm -rf /var/lib/apt/lists/*
USER jenkins

# workflow-aggregator: trabajos tipo Pipeline
# git:                 descargar el repositorio
# credentials-binding: usar el .env guardado como credencial (withCredentials)
# junit:               mostrar el resultado de las pruebas en Jenkins
# pipeline-stage-view: vista de etapas del pipeline
RUN jenkins-plugin-cli --plugins \
      workflow-aggregator git credentials-binding junit pipeline-stage-view
