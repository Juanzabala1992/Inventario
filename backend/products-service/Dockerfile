FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY backend/pom.xml backend/pom.xml
COPY backend/products-service/pom.xml backend/products-service/pom.xml
COPY backend/inventory-service/pom.xml backend/inventory-service/pom.xml
COPY backend/products-service/src backend/products-service/src

RUN mvn -f backend/pom.xml -pl products-service -am package -DskipTests

FROM eclipse-temurin:21-jre

RUN apt-get update \
  && apt-get install -y --no-install-recommends wget \
  && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=build /workspace/backend/products-service/target/products-service-1.0.0.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

