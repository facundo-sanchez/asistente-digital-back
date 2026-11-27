# Imagen de producción del backend. Compila con Maven y corre en una JRE 21.
# Las imágenes base son multiarquitectura: sirve en x86 y en ARM (Oracle Ampere).

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
# Baja las dependencias en una capa propia: no se repite si solo cambia el código
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 185 app && mkdir -p /data/documentos && chown 185 /data/documentos
COPY --from=build --chown=185 /src/target/quarkus-app/lib/ ./lib/
COPY --from=build --chown=185 /src/target/quarkus-app/*.jar ./
COPY --from=build --chown=185 /src/target/quarkus-app/app/ ./app/
COPY --from=build --chown=185 /src/target/quarkus-app/quarkus/ ./quarkus/
USER 185
ENV DOCUMENTS_DIR=/data/documentos \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -Dquarkus.http.host=0.0.0.0"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar quarkus-run.jar"]
