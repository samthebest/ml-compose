#!/bin/bash

set -ex

project_name="ml-compose"

config_notebook=docker/template-notebooks/Configuration.json
demo_notebooks=`ls docker/demo-notebooks/ | awk '{print "docker/demo-notebooks/" $1}'`
template_notebooks=`ls docker/template-notebooks/ | grep -v Configuration | awk '{print "docker/template-notebooks/" $1}'`
test_notebooks=`ls docker/test-notebooks/ | awk '{print "docker/test-notebooks/" $1}'`

function create_notebook_dir {
    rm -r docker/notebooks || true

    for notebook in $*
    do
        id=`cat ${notebook} | jq -r '.id'`
        mkdir -p docker/notebooks/${id}
        cp ${notebook} docker/notebooks/${id}/note.json
    done
}

ip=`echo ${DOCKER_HOST} | cut -c 7- | cut -c -14`

# Only works for mac
export IP=`echo ${DOCKER_HOST} | cut -c 7- | cut -c -14`

# For linux
if [ "${IP}" = "" ]; then
    IP=localhost
fi
