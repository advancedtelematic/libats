#!/bin/bash

docker rm --force $(docker ps -q --filter label=service=libats) || true


