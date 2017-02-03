#!/bin/bash
eval "$(docker-machine env default)"

source ./bin/utils.sh

docker exec -i -t `docker ps -a | grep "${project_name}" | head -1 | awk '{print $1}'` bash
