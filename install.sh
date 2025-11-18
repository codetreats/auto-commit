#!/bin/bash
source $CODETREATS_BASHUTILS_DIR/docker-utils.sh

source /media/nas/docker/data/auto-commit/config.cfg

remove_container auto-commit
prune_images
build_and_up auto-commit:master