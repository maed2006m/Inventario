# Imagen del frontend (Angular servido por Nginx), en dos etapas.

# --- Etapa 1: compilar Angular -------------------------------------------
FROM node:20-alpine AS build

WORKDIR /app

# Primero solo package*.json: si no cambian, Docker reutiliza la capa
# de "npm ci" y no vuelve a descargar las dependencias.
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund

COPY . .
# Configuracion production: usa environment.prod.ts (API en /api y
# sin los usuarios de demostracion en el login).
RUN npx ng build --configuration production

# --- Etapa 2: servir los archivos con Nginx ------------------------------
# La imagen final no lleva Node ni node_modules, solo HTML/JS/CSS.
FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/inventario-frontend/browser /usr/share/nginx/html

EXPOSE 80
