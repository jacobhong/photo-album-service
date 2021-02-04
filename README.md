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
create real cert with certbot
```
https://stackoverflow.com/questions/52674979/keycloak-ssl-setup-using-docker-image
```

DEPRECATED(use certbot)
create private cert for testing
```$xslt
keytool -genkeypair -alias keycloak.kooriim.com -keyalg RSA -keystore keycloak.jks -validity 10950
keytool -importkeystore -srckeystore keycloak.jks -destkeystore keystore.p12 -deststoretype PKCS12
openssl pkcs12 -in keystore.p12 -nokeys -out tls.crt
openssl pkcs12 -in keystore.p12 -nocerts -nodes -out tls.key
```

#certbot renew
import existing crt into existing keystore

```$xslt
openssl pkcs12 -export -name keycloak -in docker/keystore/keycloak/tls.crt -inkey docker/keystore/keycloak/tls.key -out keystore.p12
```

configure keycloak with realm + role + client + google identity provider

```$xslt
authenticate @ localhost:8443/auth keycloak::password
```

```$xslt
create new realm : kooriim-fe
create new client : kooriim-fe
-https://now.kooriim.com/* redirect uri
-web origins https://now.kooriim.com
-create mapper in client
-name: grants, token claim name: grants, claim json type: string
create new identity provider: google
-add new mapper of type Hardcoded Role: select koorim-fe
create new role: kooriim-fe
-in roles - default roles - add koorim-fe
```


docker exec -it {contaierID} bash
cd keycloak/bin
./kcadm.sh config credentials --server http://localhost:8080/auth --realm master --user admin
./kcadm.sh update realms/master -s sslRequired=NONE
(when update cert do it on aws then commit to git the new keys then recreate keystore in intellij)
