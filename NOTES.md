# coroutines socket

example client-server protocol
-------

```

             Client                  Server
 print...print |        connect       |
      ...      | <------- 16b ------- | readline
               | <------- 16b ------- | readline
      ...      |         .....        |
               | <------- 16b ------- | readline
      ...      | <------- 16b ------- | readline
      ...      |       disconnect     |
```

> read and print operations are performed in one thread but in two separate coroutines.
> this example shows that read() sockets' operation is not thread-blocking,
> but only coroutine-blocking.

socks5 client
---------

this project also implements simple socks5 proxy client on coroutines async socket.

current state of implementation allows connecting to socks5 server with user/pass auth method or with no authentication.

after you can establish remote connection to server using socks5 CONNECT command.
after connection establishing all data passed to the socket will be redirected to remote server through socks5 proxy.

the example with http request to the ipify service API shows that ip the request coming from is socks5 server's ip.

also, schema of server-client communication example is simplified 

#### warning

be aware of windows defender firewall. I just faced 'The semaphore timeout period has expired.' IOException on connect() stage.
(also nvidia drivers has died once after a connect(), lol)