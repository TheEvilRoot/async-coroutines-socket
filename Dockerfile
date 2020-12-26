FROM ubuntu:18.04

RUN apt-get update && apt-get install -y openjdk-8-jdk python3 python3-pip 
RUN python3 -m pip install pproxy

COPY . /tmp
WORKDIR /tmp
