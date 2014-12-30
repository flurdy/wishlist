FROM flurdy/activator:latest

MAINTAINER flurdy

ENV DEBIAN_FRONTEND noninteractive

ENV APPDIR /var/local/application

ADD repositories /root/.sbt/

COPY . /var/local/application

WORKDIR /var/local/application

RUN /usr/local/bin/activator stage

WORKDIR /var/local/application/target/universal/stage/bin/

RUN rm -f playapp ;\ 
   for i in `ls | grep -v .bat | grep -v playapp` ;\
   do mv $i playapp; done

ENTRYPOINT ["/var/local/application/target/universal/stage/bin/playapp"]

EXPOSE 9000
EXPOSE 9999
