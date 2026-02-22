FROM codetreats/simple-pipeline:latest
RUN apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -q -y nodejs npm ts-node
RUN git config --global --add safe.directory "*"
COPY job /job

COPY src /release/src
COPY src /debug/src
RUN ln -s /release/src /src

RUN echo "30 13 * * * root /trigger.sh" >> /etc/cron.d/cronjob
CMD ["/start.sh"]
