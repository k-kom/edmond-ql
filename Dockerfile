FROM openjdk:13-alpine3.9

EXPOSE 8888

WORKDIR /app

COPY ./target/edmond-ql.jar /app/edmond-ql.jar
CMD java -jar /app/edmond-ql.jar

