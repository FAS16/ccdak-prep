FROM openjdk:17-oracle
COPY ./target/kafka-streams-api-1.0-SNAPSHOT-jar-with-dependencies.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
