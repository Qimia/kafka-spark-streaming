#!/bin/bash

set -e
TAG=3.0.1-hadoop2.7

build() {
  NAME=$1
  IMAGE=io.qimia.kafka/spark-$NAME:$TAG
  cd ./docker/images/"$NAME"
  echo '--------------------------' building "$IMAGE" in "$(pwd)"
  docker build -t "$IMAGE" .
  cd -
}

build base
build master
build worker
build history-server
mkdir -p docker/spark_logs
chmod 777 docker/spark_logs
