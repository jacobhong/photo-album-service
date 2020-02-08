!!WORK IN PROGRESS!!


java 11 Starter spring boot project using Spring JPA/Hibernate/Rest, for serving photos to FE starter project.

```alias bootRunDebug="./gradlew clean build; ./gradlew bootRun --debug-jvm"```

```alias bootRun="./gradlew clean build; ./gradlew bootRun "```
useful alias for running project

requires mysql db running, use ```docker-compose up``` to start mariadb,
update import.mysql to modify database schema when creating. Only run once, delete ./docker/mysql to have it run again

in ```application.yml``` change directory for images to be saved, if want to use default /opt than must grant permission to write to directory


docker build -t photo-album-service .
docker tag photo-album-service:latest 500536527570.dkr.ecr.us-west-1.amazonaws.com/photo-album-service:latest
docker push 500536527570.dkr.ecr.us-west-1.amazonaws.com/photo-album-service:latest
ec2-13-57-205-189.us-west-1.compute.amazonaws.com