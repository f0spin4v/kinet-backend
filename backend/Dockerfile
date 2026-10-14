# ---------- Compilación ----------
FROM maven:3.9-eclipse-temurin-21 AS compilacion
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

# ---------- Ejecución ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
ENV TZ=America/Bogota
COPY --from=compilacion /app/target/kinet-backend-*.jar kinet.jar
EXPOSE 8080
# Variables que se definen al desplegar: DB_URL, DB_USUARIO, DB_CONTRASENA, KINET_JWT_SECRET,
# KINET_CORS_ORIGENES, KINET_FRONTEND_URL y, opcionalmente, MAIL_HOST, MAIL_USUARIO, MAIL_CONTRASENA.
ENTRYPOINT ["java", "-jar", "kinet.jar"]
