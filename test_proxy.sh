#!/usr/bin/env sh

if [ $# -eq 0 ]
  then
    echo ':: using no auth. port: 1081'
    pproxy -l socks5://0.0.0.0:1081
    exit 0
fi

if [ $# -eq 2 ]
  then
     echo ':: using user/pass auth. port: 1080'
     pproxy -l "socks5://0.0.0.0:1080#$1:$2"
     exit 0
  else
    echo "usage: test_proxy [username password]"
    exit 1
fi
