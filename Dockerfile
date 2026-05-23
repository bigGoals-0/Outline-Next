FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY . .
RUN mvn -pl server -am -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN mkdir -p /var/outline/uploads logs
COPY --from=build /app/server/target/outline-server-0.1.0.jar /app/outline-server.jar
ENV SPRING_PROFILES_ACTIVE=prod
ENV SQLITE_DB_PATH=/var/outline/outline-prod.sqlite
ENV OUTLINE_UPLOAD_DIR=/var/outline/uploads
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/outline-server.jar"]
