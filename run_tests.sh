#!/usr/bin/env sh

./test_proxy.sh &
./test_proxy.sh username password &

./gradlew test --info
