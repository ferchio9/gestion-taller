# Etapa de build: compila el jar con Maven (cachea las dependencias en su propia capa).
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B dependency:go-offline
COPY src/ src/
RUN ./mvnw -B package -DskipTests

# Etapa final: solo el JRE y el jar ya construido.
FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd -r app && useradd -r -g app app
WORKDIR /app
COPY --from=build --chown=app:app /app/target/*.jar app.jar
USER app
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8080/login.html || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
