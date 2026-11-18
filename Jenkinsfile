// Pipeline de despliegue (guia del laboratorio, partes 5 a 7).
//
// Si una etapa falla, las siguientes no se ejecutan: en particular, si
// alguna prueba falla NO se despliega.
//
// Requisitos en Jenkins (ver jenkins/docker-compose.yml):
// - acceso al Docker del equipo (socket y grupo docker);
// - una credencial tipo "Secret file" con id "inventario-env" que
//   contenga el .env de produccion (ver .env.example).
pipeline {
    agent any

    options {
        // Dos despliegues a la vez pelearian por los mismos contenedores.
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '15'))
    }

    environment {
        // Desde el contenedor de Jenkins, "localhost" es el propio Jenkins;
        // host.docker.internal es el equipo donde corre la aplicacion.
        APP_URL = 'http://host.docker.internal'
    }

    stages {
        stage('Build') {
            steps {
                // El repositorio guarda mvnw sin permiso de ejecucion.
                sh 'chmod +x mvnw'
                sh './mvnw -B clean package -DskipTests'
            }
        }

        stage('Pruebas') {
            steps {
                // verify = unitarias (Surefire) + integracion con PostgreSQL
                // real (Failsafe + Testcontainers) + cobertura minima JaCoCo.
                sh './mvnw -B verify'
            }
            post {
                always {
                    junit allowEmptyResults: true,
                          testResults: 'target/surefire-reports/*.xml, target/failsafe-reports/*.xml'
                }
            }
        }

        stage('Docker Build') {
            steps {
                // El .env con las contrasenas vive en Jenkins como credencial,
                // nunca en Git. Se copia solo durante el despliegue.
                withCredentials([file(credentialsId: 'inventario-env', variable: 'ENV_FILE')]) {
                    sh 'cp "$ENV_FILE" .env'
                }
                sh 'docker compose build'
            }
        }

        stage('Deploy') {
            steps {
                // Reemplaza solo los contenedores cuya imagen cambio. La base
                // conserva sus datos en el volumen datos-postgres.
                sh 'docker compose up -d'
            }
        }

        stage('Comprobacion') {
            steps {
                // / debe responder 200 (Nginx + Angular) y un login con datos
                // falsos debe responder 401: eso prueba que /api llega al
                // backend y que este consulta la base. Si el backend no
                // arranco, Nginx responde 502.
                sh '''
                    for i in $(seq 1 30); do
                        web=$(curl -s -o /dev/null -w "%{http_code}" "$APP_URL/")
                        api=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$APP_URL/api/auth/login" \
                              -H "Content-Type: application/json" \
                              -d '{"username":"comprobacion","password":"comprobacion"}')
                        echo "Intento $i: web=$web api=$api"
                        if [ "$web" = "200" ] && [ "$api" = "401" ]; then
                            exit 0
                        fi
                        sleep 5
                    done
                    echo "La aplicacion no respondio como se esperaba."
                    docker compose ps
                    docker compose logs --tail 50 backend
                    exit 1
                '''
            }
        }
    }

    post {
        always {
            sh 'rm -f .env'
            echo 'Pipeline terminado.'
        }
        success {
            echo '¡Despliegue exitoso! Aplicacion disponible en http://localhost'
        }
        failure {
            echo 'Error en el pipeline. Revisar los logs.'
        }
    }
}
