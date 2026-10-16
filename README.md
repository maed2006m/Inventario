# Sistema de Inventario para Tienda de Barrio

API REST para administrar los productos, proveedores y movimientos de inventario de una tienda de barrio. Permite registrar entradas y salidas de productos, consultar existencias, generar reportes y controlar el acceso mediante autenticación y roles de usuario.

La regla central del sistema es que **la existencia nunca se digita, se calcula** a partir de los movimientos registrados.

**Integrantes:** Miguel Ángel Escobar Díaz · Juan Andrés Sanabria Prieto · Alejandro Méndez Ducuara

---

## Tabla de contenido

1. [Tecnologías](#1-tecnologías)
2. [Requisitos previos](#2-requisitos-previos)
3. [Descarga del proyecto](#3-descarga-del-proyecto)
4. [Configuración de la base de datos](#4-configuración-de-la-base-de-datos)
5. [Ejecución de la aplicación](#5-ejecución-de-la-aplicación)
6. [Usuarios de prueba y seguridad](#6-usuarios-de-prueba-y-seguridad)
7. [Verificación del funcionamiento](#7-verificación-del-funcionamiento)
8. [Pruebas automatizadas](#8-pruebas-automatizadas)
9. [Estructura del proyecto](#9-estructura-del-proyecto)
10. [Flujo de trabajo en Git](#10-flujo-de-trabajo-en-git)
11. [Consideraciones finales](#11-consideraciones-finales)

---

## 1. Tecnologías

| Componente | Tecnología |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 3.3.5 |
| Persistencia | Spring Data JPA (Hibernate) |
| Base de datos | PostgreSQL 16 |
| Migraciones | Flyway |
| Seguridad | Spring Security + JWT |
| Mapeo DTO | MapStruct |
| Pruebas | JUnit 5, Mockito, Testcontainers, JaCoCo |
| Documentación API | Swagger (OpenAPI) |
| Integración continua | GitHub Actions |

---

## 2. Requisitos previos

Antes de instalar el proyecto se deben tener las siguientes herramientas:

- **Java JDK 21:** necesario para compilar y ejecutar la aplicación.
- **PostgreSQL 16:** sistema gestor de base de datos.
- **Git:** para descargar y administrar el repositorio.
- **IDE:** se recomienda IntelliJ IDEA, Eclipse o Visual Studio Code con soporte para Java.
- **Postman:** para probar las peticiones HTTP de la API.
- **Docker** (opcional): solo es necesario para ejecutar las pruebas de integración.

No es necesario instalar Maven: el proyecto incluye `mvnw` y `mvnw.cmd`, que lo descargan automáticamente.

Para verificar que Java está instalado correctamente:

```bash
java -version
```

La respuesta debe indicar la versión 21.

---

## 3. Descarga del proyecto

Abra una terminal en la carpeta donde desea guardar el proyecto y ejecute:

```bash
git clone https://github.com/maed2006m/Inventario.git
cd Inventario
```

El proyecto se puede abrir desde el IDE importándolo como un **proyecto Maven**. Verifique que el IDE esté usando Java 21.

El archivo `pom.xml` contiene las dependencias necesarias, entre ellas Spring Web, Spring Data JPA, Spring Security, PostgreSQL, Flyway, JWT, MapStruct y las herramientas de pruebas.

---

## 4. Configuración de la base de datos

Abra la consola de PostgreSQL (`psql`) o pgAdmin con un usuario administrador y ejecute:

```sql
CREATE USER inventario WITH PASSWORD 'inventario';
CREATE DATABASE inventario_tienda OWNER inventario;
```

Si el usuario o la base de datos ya existen, no es necesario crearlos de nuevo.

### Conexión

La conexión se configura en `src/main/resources/application.yaml`. Los valores por defecto son:

| Variable de entorno | Valor por defecto |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/inventario_tienda` |
| `DB_USER` | `inventario` |
| `DB_PASSWORD` | `inventario` |
| `JWT_SECRET` | secreto de desarrollo (cambiar en producción) |

Si su instalación usa otros valores, puede definir estas variables de entorno sin modificar el código.

### Migraciones

No es necesario crear las tablas a mano. Al iniciar la aplicación, **Flyway** ejecuta automáticamente los scripts de `src/main/resources/db/migration`:

| Migración | Contenido |
|---|---|
| `V1__esquema_inicial.sql` | Tablas de usuarios, proveedores, productos y movimientos; restricciones, índices, la vista de existencias `v_stock_producto` y los triggers que impiden editar movimientos o dejar el stock negativo |
| `V2__usuarios_iniciales.sql` | Usuarios `admin` y `tendero` con contraseñas cifradas con BCrypt |

En el perfil de desarrollo (`dev`, activo por defecto) también se cargan los datos de demostración de `src/main/resources/db/demo`:

| Migración | Contenido |
|---|---|
| `V100__datos_demo.sql` | 5 proveedores, 12 productos y 90 días de movimientos de ejemplo |
| `V101__datos_casos_borde.sql` | Casos especiales: productos en bajo stock, agotados y deshabilitados, y un proveedor inactivo |

---

## 5. Ejecución de la aplicación

Desde la carpeta raíz del proyecto, ejecute:

**Windows (CMD o PowerShell):**
```bash
mvnw.cmd spring-boot:run
```

**Git Bash, Linux o macOS:**
```bash
./mvnw spring-boot:run
```

Maven descargará las dependencias, compilará el código e iniciará la aplicación. Si todo está correcto, el servidor queda disponible en:

```
http://localhost:8080
```

Durante el inicio, verifique en la consola que la conexión con PostgreSQL sea exitosa y que las migraciones de Flyway terminen sin errores.

---

## 6. Usuarios de prueba y seguridad

El sistema usa **Spring Security con JWT**. El inicio de sesión se hace en `POST /api/auth/login` y devuelve un token que debe enviarse en las demás peticiones con la cabecera:

```
Authorization: Bearer <token>
```

Usuarios creados para el entorno de desarrollo:

| Rol | Usuario | Contraseña | Permisos |
|---|---|---|---|
| ADMIN | `admin` | `Admin123*` | Todo, incluida la edición e inactivación de proveedores, los reportes y la exportación a CSV |
| TENDERO | `tendero` | `Tendero123*` | Operación diaria: productos, movimientos y consultas |

> ⚠️ Estas credenciales son solo para desarrollo y pruebas. Antes de desplegar en un entorno real se deben cambiar las contraseñas y definir un `JWT_SECRET` seguro mediante variables de entorno.

---

## 7. Verificación del funcionamiento

### Swagger

Con la aplicación en ejecución, la documentación interactiva de la API está disponible en:

```
http://localhost:8080/swagger-ui/index.html
```

### Postman

La carpeta `postman/` contiene una colección y un entorno local listos para importar:

1. Abra Postman y seleccione **Import**.
2. Importe `postman/inventario.collection.json` y `postman/local.environment.json`.
3. Seleccione el entorno local en la esquina superior derecha.
4. Ejecute primero las peticiones de **00 Autenticacion** para obtener los tokens.
5. Ejecute el resto de carpetas en orden: proveedores, productos, movimientos, verificación de existencia y seguridad.

La colección usa datos fijos (NIT y SKU de prueba). Si se ejecuta una segunda vez sin reiniciar la base de datos, las creaciones devolverán 409 por duplicado.

### Principales endpoints

| Módulo | Endpoint |
|---|---|
| Autenticación | `POST /api/auth/login`, `GET /api/auth/me` |
| Proveedores | CRUD en `/api/proveedores` (eliminar = inactivar) |
| Productos | CRUD, habilitar y alertas en `/api/productos` |
| Movimientos | Registrar y consultar en `/api/movimientos` |
| Reportes | `/api/reportes/rotacion`, `/api/reportes/participacion` |
| Dashboard | `/api/dashboard/resumen` |
| Exportación | `/api/movimientos/export` (CSV) |

---

## 8. Pruebas automatizadas

Para ejecutar todas las pruebas unitarias y de integración:

**Windows:**
```bash
mvnw.cmd verify
```

**Git Bash, Linux o macOS:**
```bash
./mvnw verify
```

- Las **pruebas unitarias** usan Mockito y verifican las reglas de negocio de los servicios.
- Las **pruebas de integración** usan **Testcontainers**, que levanta un PostgreSQL temporal. Por eso **Docker debe estar instalado y en ejecución**.
- Al terminar, el reporte de cobertura de **JaCoCo** queda en `target/site/jacoco/index.html`.

Las pruebas también se ejecutan automáticamente con **GitHub Actions** en cada pull request.

---

## 9. Estructura del proyecto

El código fuente está en `src/main/java/co/edu/javeriana/inventario`, organizado por capas:

| Paquete | Responsabilidad |
|---|---|
| `model` | Entidades (Producto, Proveedor, Movimiento, Usuario) y enumerados de tipo, motivo, rol y estado |
| `repository` | Interfaces de Spring Data JPA para consultar y guardar datos |
| `service` | Lógica y reglas de negocio |
| `controller` | Controladores REST que reciben las peticiones HTTP |
| `dto` | Objetos de entrada y salida de la API |
| `mapper` | Conversión entre entidades y DTO con MapStruct |
| `exception` | Excepciones personalizadas y manejador global de errores |
| `config` | Configuración de seguridad y de la documentación de la API |
| `security` | Generación y validación de tokens JWT |

`InventarioApplication.java` es la clase principal desde la que se inicia Spring Boot. Las pruebas están en `src/test/java` y la configuración y migraciones en `src/main/resources`.

---

## 10. Flujo de trabajo en Git

El equipo trabaja con **Gitflow**:

| Rama | Uso |
|---|---|
| `main` | Versión estable entregada |
| `develop` | Integración del trabajo del equipo |
| `feature/...` | Desarrollo de cada funcionalidad |
| `fix/...` | Corrección de errores |
| `release/...` | Preparación de una entrega |

Todo cambio entra a `develop` mediante un **pull request** revisado y con las pruebas de GitHub Actions aprobadas. Al cerrar una entrega, `develop` se integra a `main`.

---

## 11. Consideraciones finales

- La instalación requiere configurar correctamente Java, PostgreSQL y las variables de conexión antes de iniciar la aplicación.
- Antes de usar el sistema, verifique los registros de ejecución, las migraciones y las pruebas de los endpoints.
- Para un despliegue en producción se deben reemplazar las credenciales de desarrollo, proteger los secretos y revisar los permisos de acceso a la base de datos.
