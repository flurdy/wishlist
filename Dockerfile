FROM flurdy/activator-mini:1.3.12-alpine

MAINTAINER Ivar Abrahamsen <@flurdy>

WORKDIR /opt/app

ENV PORT 9000

# Locally or your CI will have to have run `sbt stage` beforehand

ADD target/universal/stage/ /opt/app/

RUN rm -f /opt/app/bin/*.bat && \
    mv -f /opt/app/bin/* /opt/app/bin/app

ADD bin/entrypoint.sh /opt/app/bin/

RUN adduser -D appuser && chown -R appuser /opt/app

USER appuser

ENTRYPOINT ["/opt/app/bin/entrypoint.sh"]

CMD ["-Dconfig.resource=production.conf"]

EXPOSE 9000
