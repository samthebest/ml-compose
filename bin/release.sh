#!/bin/bash

set -ex

OPTIND=1

function show_help {
    echo "-b to skip building the jar (useful for debugging the Dockerfile)"
    echo "-B will build only and not run it-tests. This is used by build.sh in the root of the project"
    echo "-s will build and deploy a snapshot and will not tag and such and such, useful for developing"
    echo "-i will skip the integrations tests"
    echo "-p port used for it-tests"
    echo "-P port used by container upon release"
    echo "-l label used to label container (either prod or test, default: prod)"
    echo "-H to set the Host (default: ???)"
    echo "-r will only restart zeppelin but not deploy. Currently must be used in conjunction with -s"
}

source ./bin/utils.sh

build_args=""

user=${project_name}-user
release_port=80
label=prod
host=???

while getopts "h?cbBsu:p:P:l:H:r" opt; do
    case "$opt" in
    h|\?)
        set +x
        show_help
        exit 0
        ;;
    b)  build_args="${build_args} -b"
        ;;
    B)  build_args="${build_args} -B"
        ;;
    s)  build_snapshot=true
        ;;
    u)  user=$OPTARG
        ;;
    p)  build_args="${build_args} -p $OPTARG"
        ;;
    P)  release_port=$OPTARG
        ;;
    l)  label=$OPTARG
        ;;
    H)  host=$OPTARG
        ;;
    r)  restart_only=true
        ;;
    esac
done

shift $((OPTIND-1))

[ "$1" = "--" ] && shift

if [ "${build_snapshot}" != "true" ]; then
    echo "INFO: Checking this is indeed master branch"

    git branch | grep "*" | grep master
fi

next_version=SNAPSHOT

if [ "${restart_only}" = "true" ]; then
    echo "INFO: Only restarting"

    if [ "${build_snapshot}" != "true" ]; then
        echo "ERROR: Restart only supported for snapshot, please use -s"
        exit 1
    fi

    ssh -t -i ~/.ssh/shepherd -o StrictHostKeyChecking=no ${user}@${host} "/home/${user}/run-server-mode.sh -v ${next_version} -u ${user} -l ${label} -p ${release_port}"

    exit 0
fi

./bin/build.sh ${build_args}

echo "INFO: Build success"

if [ "${build_snapshot}" != "true" ]; then
    last_version=`git tag | egrep "^[0-9]+$" | sort -n | tail -1`

    if [ "${last_version}" = "" ]; then
        last_version=0
    fi

    next_version=`expr ${last_version} + 1`
fi

echo "INFO: This version will be: ${next_version}"

if [ "${build_snapshot}" != "true" ]; then
    git tag ${next_version} && git push --tags

    echo "INFO: now our tags are:"

    git show-ref --tags -d
fi

docker_image=${project_name}.tar

artefact_name=${project_name}-${next_version}.tar

# TODO Save artifact in a docker repo

function deploy {
    echo "INFO: Deploying to $host"

    tmp_artefact_store=/home/${user}/tmp-artefact-store

    echo "INFO: scp-ing"
    scp -o StrictHostKeyChecking=no ${docker_image} ${user}@${host}:${tmp_artefact_store}/${artefact_name}

    scp -o StrictHostKeyChecking=no bin/run-server-mode.sh ${user}@${host}:/home/${user}/

    ssh -o StrictHostKeyChecking=no ${user}@${host} "/home/${user}/run-server-mode.sh -v ${next_version} -u ${user} -l ${label} -p ${release_port}"
}

deploy
