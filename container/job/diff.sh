#!/bin/bash
set -e
STATUS_FILE=$1
echo "Starting auto-commit scan..."
cd /job
npm i
ts-node scan-repos.ts $STATUS_FILE
echo "Scan completed!"