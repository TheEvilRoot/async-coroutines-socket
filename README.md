# coroutines socket

example client-server protocol
-------

```

Client                  Server
  |         connect        |
  | <------- 2048b ------- |
  | <------- 2048b ------- |
  | <------- 2048b ------- |
  |                        |
  | -------- 2048b ------> |
  |          ....          |
  |          ....          |
  | -------  ping  ------> |
  | <------  pong  ------- |
  |          ....          |
  | <------  ping  ------- |
  | -------  pong  ------> |
  |          ....          |
  |       disconnect       |
```

