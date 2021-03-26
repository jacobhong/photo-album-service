#!/bin/bash
./gradlew clean build
docker build -t mediaItem-album-service .
docker tag mediaItem-album-service:latest 500536527570.dkr.ecr.us-west-1.amazonaws.com/mediaItem-album-service:latest
docker push 500536527570.dkr.ecr.us-west-1.amazonaws.com/mediaItem-album-service:latest