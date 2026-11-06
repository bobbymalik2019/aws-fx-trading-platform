FROM eclipse-temurin:17-jre

WORKDIR /app

COPY target/fix-accepter-server-0.0.1-SNAPSHOT.jar app.jar

RUN mkdir -p /app/store /app/log

EXPOSE 9876

ENTRYPOINT ["java", "-Xmx384m", "-jar", "/app/app.jar"]