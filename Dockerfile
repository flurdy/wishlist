FROM flurdy/activator-mini:1.3.12-alpine

MAINTAINER Ivar Abrahamsen <@flurdy>

WORKDIR /opt/app

ENV PORT 9000

ADD target/universal/stage/ /opt/app/

RUN adduser -D appuser && chown -R appuser /opt/app

USER appuser

ENTRYPOINT /opt/app/bin/app

# CMD ["-Dconfig.resource=heroku.conf"]

CMD "-Dhttp.port=${PORT}"

EXPOSE 9000
