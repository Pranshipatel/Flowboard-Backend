FROM eclipse-temurin:21-jre

WORKDIR /app

COPY target/*SNAPSHOT.jar app.jar

EXPOSE 8085

ENTRYPOINT ["java", "-jar", "app.jar"]
