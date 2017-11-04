FROM flurdy/activator-mini:1.3.12-alpine

MAINTAINER Ivar Abrahamsen <@flurdy>

WORKDIR /opt/app

ENV PORT 9000

ADD target/universal/stage/ /opt/app/

RUN adduser -D appuser && chown -R appuser /opt/app

USER appuser

ENTRYPOINT /opt/app/bin/app -Dhttp.port=${PORT}

# CMD ["-Dconfig.resource=heroku.conf"]

CMD ["-Dconfig.resource=application.conf"]

EXPOSE 9000
