#!/usr/bin/env sh

if [ $# -eq 0 ]
  then
    echo "usage: test_proxy [username password]"
    exit 1
fi

if [ $1 = "4" ]
  then
    protocol=socks4
    port=1082
  elif [ $1 = "5" ]
   then
     protocol=socks5
     port=1081
  else
    echo "protocol $1 is invalid. use 5 or 4 instead"
    exit 1
fi
      

if [ $# -eq 1 ]
  then
    echo ':: using no auth. port: 1081'
    pproxy -l "$protocol://0.0.0.0:$port"
    exit 0
fi

if [ $# -eq 3 ]
  then
     echo ':: using user/pass auth. port: 1080'
     port=1080
     pproxy -l "$protocol://0.0.0.0:$port#$1:$2"
     exit 0
fi


