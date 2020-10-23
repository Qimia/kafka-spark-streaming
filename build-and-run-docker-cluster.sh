#!/bin/bash

docker-compose -f docker-compose-cluster.yml down
./build-spark-docker-images.sh
docker-compose -f docker-compose-cluster.yml up