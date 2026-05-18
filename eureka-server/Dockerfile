FROM eclipse-temurin:17-jre

WORKDIR /app

COPY target/*SNAPSHOT.jar app.jar

EXPOSE 8761

ENTRYPOINT ["java", "-jar", "app.jar"]
