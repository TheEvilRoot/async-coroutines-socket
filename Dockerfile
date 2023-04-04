FROM ubuntu:latest

RUN apt-get update && apt-get install -y openjdk-8-jdk python3 python3-pip
RUN python3 -m pip install pproxy

WORKDIR /tests
COPY gradle/ /tests/gradle
COPY gradlew /tests/

RUN touch settings.gradle.kts && ./gradlew :wrapper

COPY settings.gradle.kts build.gradle.kts gradle.properties /tests/

RUN ./gradlew --refresh-dependencies

COPY test_proxy.sh run_tests.sh server.py /tests/
COPY src /tests/src
