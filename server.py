import socket
import threading
import time
import random
import datetime

serv = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
serv.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
serv.bind(('0.0.0.0', 8888))
serv.listen()

last_ping = None

def ping_thread(sock, idx):
	global last_ping
	while True:
		time.sleep(int(random.random() * 10))
		sock.sendall(b'\xaa'*2048)
		last_ping = datetime.datetime.now()

def client_thread(sock, idx):
	global last_ping
	#handshake
	for i in range(3):
		sock.sendall(b'\x01'*2048)
	thr = threading.Thread(target=ping_thread, args=(sock, idx))
	thr.start()
	while True:
		d = sock.recv(2048)
		if not d:
			break
		if d[0] == 0xaa: 
			if last_ping is None:
				sock.sendall(b'\xaa'*2048)
			else:
				print('ping time:', datetime.datetime.now() - last_ping)
				last_ping = None
		else:
			sock.sendall(b'\xbb'*2048)
	print(idx, 'is done')
			
		

index = 0
while True:
	print('awaiting client...')
	sock, addr = serv.accept()
	print('client arrived from', addr)
	thr = threading.Thread(name='client' + str(index), target=client_thread, args=(sock, index))
	thr.start()
	index += 1
	

