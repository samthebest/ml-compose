#!/bin/bash
source ./bin/utils.sh
docker stop `docker ps -a | grep "${project_name}" | head -1 | awk '{print $1}'`
