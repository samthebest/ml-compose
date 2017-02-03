#!/bin/bash

set -ex

OPTIND=1

function show_help {
    echo "-b to skip building the jar (useful for debugging the Dockerfile)"
    echo "-B will build only and not run it-tests. This is used by build.sh in the root of the project"
}

it_test_args=""

while getopts "h?cbBp:" opt; do
    case "$opt" in
    h|\?)
        show_help
        exit 0
        ;;
    b)  skip_build=true
        it_test_args="${it_test_args} -b"
        ;;
    B)  build_only=true
        ;;
    p)  it_test_args="${it_test_args} -p $OPTARG"
        ;;
    esac
done

shift $((OPTIND-1))

[ "$1" = "--" ] && shift

echo "INFO: Bootstrapping"
./bin/bootstrap.sh

source ./bin/utils.sh

if [ "${skip_build}" != "true" ]; then
    echo "INFO: Running unit tests and building jar"
    ./bin/data-it-tests.sh
    sbt assembly
    cp target/scala-2.10/spark-${project_name}-assembly-*.jar ./docker/${project_name}-assembly.jar
fi


echo "INFO: Running notebook integrations tests and building image"

if [ "${build_only}" = "true" ]; then
    ./bin/custom-zeppelin.sh ${it_test_args}
    exit 0
else
    ./bin/notebooks-e2e-tests.sh "-b ${it_test_args}"
fi

create_notebook_dir ${config_notebook} ${demo_notebooks} ${template_notebooks}

echo "INFO: Rebuilding notebooks into image, so that we don't include test notebooks"
mkdir docker/maven_repo
mkdir docker/npm_repo
docker build -t ${project_name} docker

image_id=`docker images | grep "${project_name}" | awk '{print $3}'`

docker tag ${image_id} ${project_name}

docker save -o ${project_name}.tar ${image_id}
