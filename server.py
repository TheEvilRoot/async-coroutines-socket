import socket
import threading
import time
import random
import datetime

serv = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
serv.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
serv.bind(('0.0.0.0', 9999))
serv.listen()

socks = []

def console_thread():
  global socks
  while True:
    input("awaiting for next command...")
    print('sending 16 bytes...')
    print('0 /', len(socks), end='\r')
    idx = 1
    for sock in socks:
      try:
        print(idx, '/', len(socks), end='\r')
        sock.sendall(b'\xfa' * 0x10)
      except:
        socks.remove(sock)
      idx += 1
    print('\n16 bytes sent')

threading.Thread(target=console_thread).start()
while True:
  print('awaiting client...')
  sock, addr = serv.accept()
  print('client arrived from', addr)
  socks.append(sock)
	

