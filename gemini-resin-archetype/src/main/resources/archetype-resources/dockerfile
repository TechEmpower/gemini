FROM maven:3.5.3-jdk-8-slim as maven

WORKDIR /${artifactId}

COPY src src
COPY pom.xml pom.xml
RUN mkdir -p src/main/webapp/WEB-INF/mustache
RUN touch src/main/webapp/WEB-INF/mustache/required

RUN mvn -q compile
RUN mvn -q war:war

FROM openjdk:8-jdk
RUN apt update -qqy && apt install -yqq curl > /dev/null

WORKDIR /resin
RUN curl -sL http://caucho.com/download/resin-4.0.56.tar.gz | tar xz --strip-components=1
RUN rm -rf webapps/*
COPY --from=maven /${artifactId}/target/${artifactId}-*.war webapps/ROOT.war

ENV ConfigurationFile="Dev.conf"

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "lib/resin.jar", "console"]