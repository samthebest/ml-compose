#!/bin/bash

set -ex

OPTIND=1

function show_help {
    echo "-v version number to load"
    echo "-r will only restart, will not reload"
    echo "-p to set the host port the docker container will bind to"
    echo "-g to set the memory in Giza Bytes the container will use"
    echo "-u to set the user"
    echo "-l to set the label (either prod or test)"
    echo "-o to override the path where the artefacts are (we needed this for a workaround once)"
}

project_name=ml-compose

port=80
version_number="not set"
restart_only=false
memory=24
user=${project_name}-user
label=prod



while getopts "h?v:rp:g:u:l:" opt; do
    case "$opt" in
    h|\?)
        set +x
        show_help
        exit 0
        ;;
    r)  restart_only=true
        ;;
    v)  version_number=$OPTARG
        ;;
    p)  port=$OPTARG
        ;;
    g)  memory=$OPTARG
        ;;
    u)  user=$OPTARG
        ;;
    l)  label=$OPTARG
        ;;
    o)  artefact_dir_path=$OPTARG
        ;;
    esac
done

shift $((OPTIND-1))

[ "$1" = "--" ] && shift

if [ "$artefact_dir_path" = "" ]; then
    artefact_dir_path=/home/${user}/tmp-artefact-store
fi


screen_name=${project_name}-${label}
docker_label=${project_name}-${label}

image_id=`sudo docker images | grep ${docker_label} | awk '{print $3}'`
container_id=`sudo docker ps -a | grep ${image_id} | head -1 | awk '{print $1}'`

if [ "${image_id}" != "" ]; then

    if [ "${container_id}" != "" ]; then
        echo "INFO: Stopping and removing container"
        sudo docker stop ${container_id}
        sudo docker rm ${container_id}
    else
        echo "INFO: No container found: " ${container_id}
    fi

    if [ "${restart_only}" = false ]; then

        if [ "${version_number}" = "not set" ]; then
            echo "ERROR: Version number not set"
            exit 1
        fi

        echo "INFO: Zapping old ${project_name}"
        sudo docker rmi ${image_id} || true
    else
        echo "INFO: Not zapping old ${project_name}"
    fi
else
    echo "INFO: No image found: " ${image_id}
fi

screen -S ${screen_name} -X quit || true

if [ "${restart_only}" = false ]; then
    if [ "${version_number}" = "not set" ]; then
        echo "ERROR: Version number not set"
        exit 1
    fi

    echo "INFO: Loading new ${project_name}"
    path_to_tar=${artefact_dir_path}/${project_name}-${version_number}.tar

    # List docker images
    sudo docker images | grep -v REPOSITORY | sort > /tmp/docker-images-list

    sudo docker load -i ${path_to_tar}

    sudo docker images | grep -v REPOSITORY | sort > /tmp/docker-images-list-after-load

    image_id=`diff /tmp/docker-images-list /tmp/docker-images-list-after-load | awk '{print $4}' | tail -1`

    # Label it
    sudo docker tag ${image_id} ${docker_label}
fi

mkdir -p host-volume
pwd=`pwd`

screen -S ${screen_name} -dm bash -c \
  "sudo docker run -m ${memory}g -v ${pwd}/host-volume:/usr/zeppelin/host-volume -p ${port}:8080 -p 8088:8088 -i ${image_id} bin/zeppelin.sh 2>&1 | tee ${project_name}.log"
