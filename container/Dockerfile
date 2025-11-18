FROM codetreats/simple-pipeline:latest
RUN apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -q -y nodejs npm ts-node
RUN git config --global --add safe.directory "*"
COPY job /job
COPY www/* /var/www/html/
RUN echo "30 13 * * * root /trigger.sh" >> /etc/cron.d/cronjob
CMD ["/start.sh"]
