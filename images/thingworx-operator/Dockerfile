FROM ubuntu:24.04
ARG OPERATOR_VERSION=dev
ENV OPERATOR_VERSION=${OPERATOR_VERSION}
RUN apt-get update && apt-get install -y --no-install-recommends openjdk-21-jre curl ca-certificates && rm -rf /var/lib/apt/lists/*
RUN groupadd -r twadmin && useradd -r -g twadmin -u 1337 twadmin

COPY apps/thingworx-operator/build/libs/thingworx-operator.jar /app/thingworx-operator.jar
RUN chown -R twadmin:twadmin /app
USER twadmin

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/thingworx-operator.jar"]
