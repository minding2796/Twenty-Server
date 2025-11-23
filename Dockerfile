FROM openjdk:26-ea-21-oraclelinux8
LABEL authors="minding2796"
ARG JAR_FILE=build/libs/Twenty_Backend-0.0.1-SNAPSHOT.jar
ADD ${JAR_FILE} tbe.jar
EXPOSE 9487 9487

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/tbe.jar"]