FROM node:22-alpine AS build
WORKDIR /app

# Lockfile first so npm ci is cached unless dependencies actually change.
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund

COPY . .
# Baked in at build time: Vite inlines VITE_* variables, they are not read at runtime.
ARG VITE_API_BASE_URL=http://localhost:8080/api
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=build /app/build /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
