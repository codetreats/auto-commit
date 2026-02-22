#!/bin/bash
###############################################
RUNSTEP=/pipeline/run_step.sh $STATUS
###############################################
set -e
STATUS=$1
PARAMS=$2

# Enter your pipeline steps below
# Syntax: $RUNSTEP $STATUS <DESCRIPTION> <COMMAND>
$RUNSTEP $STATUS "Prepare" "/job/run.sh $STATUS $PARAMS"