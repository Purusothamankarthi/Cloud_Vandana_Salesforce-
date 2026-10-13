# ==========================================
# Stage 1: Build Frontend (React + Vite)
# ==========================================
FROM node:20-alpine AS frontend-build
WORKDIR /app/frontend

COPY frontend/package*.json ./
RUN npm install

COPY frontend/ ./
RUN npm run build

# ==========================================
# Stage 2: Build Backend (Spring Boot 3)
# ==========================================
FROM eclipse-temurin:17-jdk-jammy AS backend-build
WORKDIR /app/backend

COPY backend/.mvn/ .mvn
COPY backend/mvnw backend/pom.xml ./
RUN chmod +x mvnw

# Cache dependencies
RUN ./mvnw dependency:go-offline -B || true

COPY backend/src ./src

# Copy built frontend assets to Spring Boot static resources
COPY --from=frontend-build /app/frontend/dist ./src/main/resources/static

RUN ./mvnw clean package -DskipTests

# ==========================================
# Stage 3: Runtime Image
# ==========================================
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=backend-build /app/backend/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
