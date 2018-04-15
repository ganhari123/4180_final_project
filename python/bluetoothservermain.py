from bluetooth import *
import os
import glob
import array

base_dir = '/sys/bus/w1/devices/'

server_sock = BluetoothSocket(RFCOMM)
server_sock.bind(("", PORT_ANY))
server_sock.listen(1)
uuid = "563d6cfc-07f0-4f37-9832-0bf824a69c17"

advertise_service( server_sock, "AquaPiServer",
                   service_id = uuid,
                   service_classes = [ uuid, SERIAL_PORT_CLASS ],
                   profiles = [ SERIAL_PORT_PROFILE ], 
#                   protocols = [ OBEX_UUID ] 
                    )

while True:
	print("Waiting for connection on RFCOMM channel %d" % server_sock.getsockname()[1])
	client_sock, client_info = server_sock.accept()
	print("Accepted connection from ", client_info)
	try:
		data = client_sock.recv(1024)
		if len(data) != 0:
			print "received [%s]" % data;
		s = "Hiiii how are you!"
		arr = array.array('B', s);
		client_sock.send(s);
	except IOError:
		pass
