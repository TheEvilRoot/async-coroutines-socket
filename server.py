import socket
import threading
import time
import random
import datetime

serv = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
serv.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
serv.bind(('0.0.0.0', 9999))
serv.listen()

def client_thread(sock):
        while True:
                input("awaiting for next command...")
                print('sending 16 bytes...')
                sock.sendall(b'\xfa' * 0x10)
                print('16 bytes sent')

while True:
	print('awaiting client...')
	sock, addr = serv.accept()
	print('client arrived from', addr)
	thr = threading.Thread(name='client', target=client_thread, args=(sock, ))
	thr.start()
	

