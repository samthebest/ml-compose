#!/bin/bash

set -ex

export CUSTOM_ZEPPELIN_ARGS=$1

trap ./bin/stop.sh INT TERM EXIT

source ./bin/utils.sh

project_name_package=`echo ${project_name} | tr '-' '_'`

sbt "it:test-only ${project_name_package}.NotebookE2ETests"
