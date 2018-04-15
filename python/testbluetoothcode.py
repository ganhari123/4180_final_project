from bluedot.btcomm import *
from bluetooth import *

a = BluetoothAdapter()
a.allow_pairing()
a.discoverable = True
print(a.paired_devices)
fin_mac = "";
for d in a.paired_devices:
	if (d[1] == 'Nexus 5X'):
		fin_mac = d[0]
		break
print(fin_mac)
service_matches = find_service(address=fin_mac)
fin_port = 0
for s in service_matches:
        print(s)
        for key in s.keys():
                if key == 'name':
                        print(key)
        if s['name'] == 'Raspi':
                fin_port = s['port']
                client_socket = BluetoothSocket(RFCOMM)
                client_socket.connect((fin_mac, fin_port))
                break
                

