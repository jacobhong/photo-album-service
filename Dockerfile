FROM openjdk:11-jdk-slim
USER root
COPY docker/keystore/keycloak/tls.crt /usr/local/openjdk-11/lib/security/
RUN \
    cd /usr/local/openjdk-11/lib/security/ \
    && keytool -keystore cacerts -storepass changeit -noprompt -trustcacerts -importcert -alias keycloak.kooriim.com -file tls.crt
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar
RUN mkdir /opt/images
EXPOSE 8080
ENTRYPOINT ["java","-Dspring.profiles.active=docker","-jar","app.jar"]
