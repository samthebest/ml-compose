#!/bin/bash

set -ex

OPTIND=1

function show_help {
    echo "-b to skip building the jar (useful for debugging the Dockerfile)"
    echo "-B will build only and not start zeppelin. This is used by build.sh in the root of the project"
    echo "-p port"
    echo "-t will skip tests as part of jar build (we should just make the tests faster)"
    echo "-m copy local maven and npn repos to docker container during build"
}

port=8080

while getopts "h?cbmBdtp:" opt; do
    case "$opt" in
    h|\?)
        set +x
        show_help
        exit 0
        ;;
    b)  skip_build=true
        ;;
    B)  build_only=true
        ;;
    p)  port=$OPTARG
        ;;
    t)  skip_test=true
        ;;
    m)  local_m2=true
        ;;
    esac
done

shift $((OPTIND-1))

[ "$1" = "--" ] && shift

source ./bin/utils.sh

mkdir -p host-volume

if [ "${skip_build}" != "true" ]; then
    if [ "${skip_test}" = "true" ]; then
        sbt 'set test in assembly := {}' clean assembly
    else
        ./bin/it-tests.sh
        sbt assembly
    fi
    cp target/scala-2.11/${project_name}-assembly-*.jar ./docker/${project_name}-assembly.jar
fi

pwd=`pwd`

mkdir -p ${pwd}/host-volume

create_notebook_dir ${config_notebook} ${demo_notebooks} ${template_notebooks} ${test_notebooks}

rm -r ./docker/maven_repo || true
rm -r ./docker/npm_repo || true
if [ "${local_m2}" == "true" ]; then
    cp -r ~/.m2/repository docker/maven_repo
    cp -r ~/.npm docker/npm_repo
else
    pwd
    mkdir -p docker/maven_repo
    mkdir -p docker/npm_repo
    ls -l
fi

docker build -t ${project_name} docker

rm -r docker/maven_repo || true
rm -r docker/npm_repo || true

if [ "${build_only}" != "true" ]; then
    docker run -m 2500m -v ${pwd}/host-volume:/usr/zeppelin/host-volume -p ${port}:8080 \
      -p 62911:62911 -p 1898:1898 -p 4040:4040 -i ${project_name} bin/zeppelin.sh -g2
fi
