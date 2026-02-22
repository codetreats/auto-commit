#!/bin/bash
source $CODETREATS_BASHUTILS_DIR/docker-utils.sh

source /media/nas/docker/data/auto-commit/config.cfg

export LLM_URL=$LLM_URL
export LLM_MODEL=$LLM_MODEL
export LLM_KEY=$LLM_KEY
export LLM_THROTTLING_TIME=$LLM_THROTTLING_TIME
export GIT_USER=$GIT_USER
export GIT_MAIL=$GIT_MAIL
export GITHUB_TOKEN=${GITHUB_TOKEN}

remove_container auto-commit
prune_images
build_and_up auto-commit:master