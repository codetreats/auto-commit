#!/bin/bash
cd /src
chmod +x gradlew

export CERT_PASS=$(cat /apache/pass.txt)
export KEY_ALIAS=$(cat /apache/keyalias.txt)

if [[ "$1" == "WEBSERVER" ]] ; then  
  ./gradlew run --args="$@" | tee /var/www/html/pipeline/logs/WEBSERVER.log
else
  FILENAME=$(basename "$1")
  LOGFILE="/var/www/html/pipeline/logs/$FILENAME.log"
  echo "Log file: $LOGFILE"
  ./gradlew run -Pextendedlog=$LOGFILE --args="$@" 
fi