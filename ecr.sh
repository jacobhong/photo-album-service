#!/bin/bash
./gradlew clean build
docker build -t photo-album-service .
docker tag photo-album-service:latest 500536527570.dkr.ecr.us-west-1.amazonaws.com/photo-album-service:latest
docker push 500536527570.dkr.ecr.us-west-1.amazonaws.com/photo-album-service:latest