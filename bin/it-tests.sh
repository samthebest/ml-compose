#!/bin/bash

function list-all-it-tests {
    find src/it/* | grep ITTest\.scala | rev | cut -c 7- | rev | tr '/' '\t' | cut -f 4,5,6 | tr '\t' '.' | tr '\n' ' '
}

it_tests=`list-all-it-tests`

sbt "it:test-only ${it_tests}"
