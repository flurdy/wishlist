FROM flurdy/play-framework:2.5.14-alpine

MAINTAINER Ivar Abrahamsen <@flurdy>

COPY conf /etc/app/

ADD . /opt/build/

WORKDIR /opt/build

RUN /opt/activator/bin/activator clean stage && \
  rm -f target/universal/stage/bin/*.bat && \
  mv target/universal/stage/bin/* target/universal/stage/bin/app && \
  mv target/universal /opt/app && \
  ln -s /opt/app/stage/logs /var/log/app && \
  rm -rf /opt/build && \
  rm -rf /root/.ivy2

WORKDIR /opt/app

ADD . /opt/build/

ENTRYPOINT ["/opt/app/stage/bin/app"]

EXPOSE 9000
