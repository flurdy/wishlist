FROM eclipse-temurin:8-jre-alpine

MAINTAINER Ivar Abrahamsen <@flurdy>

WORKDIR /opt/app

ENV PORT 9000

COPY bin/distrust-emudhra-roots.sh /tmp/
RUN sh /tmp/distrust-emudhra-roots.sh && rm /tmp/distrust-emudhra-roots.sh

# Locally or your CI will have to have run `sbt stage` beforehand

ADD target/universal/stage/ /opt/app/

ADD bin/entrypoint.sh /opt/app/bin/

RUN apk add --no-cache bash && \
    rm -f /opt/app/bin/*.bat && \
    mv -f /opt/app/bin/wishlist /opt/app/bin/app && \
    adduser -D appuser && \
    chown -R appuser /opt/app

USER appuser

ENTRYPOINT ["/opt/app/bin/entrypoint.sh"]

CMD ["-Dconfig.resource=production.conf"]

EXPOSE 9000
