FROM java:8

MAINTAINER delivery-engineering@netflix.com
ENV REDIS_HOST redis
ENV FRONT50_HOST front50

COPY . workdir/

WORKDIR workdir

RUN GRADLE_USER_HOME=cache ./gradlew buildDeb -x test

RUN dpkg -i ./clouddriver-web/build/distributions/*.deb

CMD ["/opt/clouddriver/bin/clouddriver"]
