#!/bin/bash

mvn package
docker-compose -f docker-compose-apps.yml up