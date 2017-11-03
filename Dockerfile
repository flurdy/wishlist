FROM flurdy/activator-mini:1.3.12-alpine

MAINTAINER Ivar Abrahamsen <@flurdy>

WORKDIR /opt/app

RUN adduser -D appuser

USER appuser

ADD target/universal/stage/ /opt/app/

ENTRYPOINT ["/opt/app/bin/app"]

CMD ["-Dconfig.resource=heroku.conf","-Dhttp.port=${PORT}"]

EXPOSE 9000
