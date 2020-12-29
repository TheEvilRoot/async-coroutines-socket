#!/usr/bin/env sh

./test_proxy.sh 5 &
./test_proxy.sh 5 username password &
./test_proxy.sh 4 &

./gradlew test --info
