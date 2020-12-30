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
    echo ":: using $protocol no auth. port: $port"
    pproxy -l "$protocol://0.0.0.0:$port"
    exit 0
fi

if [ $# -eq 3 ]
  then
     port=1080
     echo ":: using $protocol user/pass auth. port: $port"
     pproxy -l "$protocol://0.0.0.0:$port#$2:$3"
     exit 0
fi


