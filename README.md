!!WORK IN PROGRESS!!

Build with debug mode

```alias bootRunDebug="./gradlew clean build; ./gradlew bootRun --debug-jvm"```

Build without debug mode

```alias bootRun="./gradlew clean build; ./gradlew bootRun "```
useful alias for running project

requires keycloak + mysql db running, use ```docker-compose up``` to start up stack,
update import.mysql to modify database schema when creating. Only run once, delete ./docker/mysql to have it run again

in ```application.yml``` change directory for images to be saved, if want to use default /opt than must grant permission to write to directory

build and push docker image to ec2

```
docker build -t photo-album-service .
docker tag photo-album-service:latest 500536527570.dkr.ecr.us-west-1.amazonaws.com/photo-album-service:latest
docker push 500536527570.dkr.ecr.us-west-1.amazonaws.com/photo-album-service:latest
ec2-54-183-228-18.us-west-1.compute.amazonaws.com
```

create private cert for testing
```$xslt
keytool -genkeypair -alias keycloak -keyalg RSA -keystore keycloak.jks -validity 10950
keytool -importkeystore -srckeystore keycloak.jks -destkeystore keycloak.p12 -deststoretype PKCS12
openssl pkcs12 -in keycloak.p12 -nokeys -out tls.crt
openssl pkcs12 -in keycloak.p12 -nocerts -nodes -out tls.key
```

import existing crt into existing keystore

```$xslt
openssl pkcs12 -export -name keycloak -in tls.crt -inkey tls.key -out keystore.p12
```

configure keycloak with realm + role + client + google identity provider

```$xslt
authenticate @ localhost:8443/auth keycloak::password
```

```$xslt
create new realm : kooriim-fe
create new client : kooriim-fe
create new identity provider: google
-add new mapper of type Hardcoded Role: select koorim-fe
create new role: kooriim-fe
-in roles - default roles - add koorim-fe
```


