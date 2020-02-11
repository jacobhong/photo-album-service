FROM openjdk:11-jdk-slim
USER root
COPY docker/keystore/tls.crt /usr/local/openjdk-11/lib/security/
RUN \
    cd /usr/local/openjdk-11/lib/security/ \
    && keytool -keystore cacerts -storepass changeit -noprompt -trustcacerts -importcert -alias keycloak -file tls.crt
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar
EXPOSE 8080
ENTRYPOINT ["java","-Dspring.profiles.active=docker","-jar","app.jar"]
