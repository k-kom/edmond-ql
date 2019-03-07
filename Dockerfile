FROM openjdk:13-alpine3.9

EXPOSE 8888

WORKDIR /app

COPY ./target/edmond-ql-0.1.0-SNAPSHOT-standalone.jar /app
CMD java -jar /app/edmond-ql-0.1.0-SNAPSHOT-standalone.jar

