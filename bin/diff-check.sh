#!/bin/bash

count=`git diff master --stat | grep -v "docker/.*notebooks" | awk '{print $3}' | egrep "^[0-9]+$" | awk '{s+=$1} END {print s}'`

if [ $count -ge 1000 ]; then
	echo "ERROR: Your branch is too big, break it down"
	exit 1
fi
