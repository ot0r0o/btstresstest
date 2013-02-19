#!/usr/bin/python
import bluetooth
import sys
import threading
import signal
import time

#Search for devices starting with this string
DEVICE_NAME = "BH"
#String to error handling
HOST_IS_DOWN_SIGNATURE = "Host is down"
UUID = "00000000-deca-fade-deca-deafdecacaff"

def signal_handler(signal, frame):
	# print 'You pressed Ctrl+C!'
	scan.stop()
	sys.exit()

class ScanThread(threading.Thread):
	def __init__(self):
		self.cond = True
		threading.Thread.__init__(self, name="Scan thread")
		self.activeZephyrs=[]
		self.activeThreads=[]
		
	# Search for bluetooth devices starting with $DEVICE_NAME
	def run(self):

		# print "Started scanning thread"
		while self.cond==True:
			print("Scanning bluetooth devices... ")
			# for addr, name in bluetooth.discover_devices(lookup_names=True):
			# 	print("Device found: %s %s" %(name,addr))
			# 	if name.startswith(DEVICE_NAME) and addr not in self.activeZephyrs and name=="BH-DesireHD":
			# 		self.activeZephyrs.append(addr)
			# 		recvingTh = ReceivingThread(addr,name)
			# 		recvingTh.start()
			# 		self.activeThreads.append(recvingTh)
			try:
				services = bluetooth.find_service(uuid=UUID)
			except Exception:
				print "Failed to Scan, stopping all threads"
				self.stop()
			except KeyboardInterrupt:
				print "CTRL+C!"
				sys.exit()
			print "Services found: %d"%len(services)
			for service in services :
				print "Service found on BT %s @ %s" % (service["host"],service["port"])
				if service["host"] not in self.activeZephyrs:
					self.activeZephyrs.append(service["host"])
					recvingTh = ReceivingThread(service,self.activeZephyrs)
					recvingTh.start()
					self.activeThreads.append(recvingTh)
 					
			time.sleep(1)

	def stop(self):
		self.cond=False
		for thread in self.activeThreads:
			thread.stop()
		self.activeZephyrs=[]


class ReceivingThread(threading.Thread):
	def __init__(self,service,activeZephyrs):
		self.cond = True
		self.address = service["host"]
		threading.Thread.__init__(self,name=self.address)
		self.name = self.address
		self.port = service["port"]
		self.activeZephyrs = activeZephyrs
		

	def run(self):
		socket = bluetooth.BluetoothSocket(bluetooth.RFCOMM)
		try:
			socket.connect((self.address, self.port))
		except bluetooth.BluetoothError, ex:
			socket.close()
			self.activeZephyrs.remove(self.address)
			self.stop()
			
			if HOST_IS_DOWN_SIGNATURE in str(ex):
				pass
				# Saved BT address doesn't work, will do a scan
			else:
				raise

		print "Receiving from %s" % self.name
		try:
			test = self.create_message_frame(0x14, [1])
			socket.send(test)
			data = socket.recv(1024)
			self.__decode(data,socket)
			test = self.create_message_frame(0x15, [1])
			socket.send(test)
			data = socket.recv(1024)
			self.__decode(data,socket)
			test = self.create_message_frame(0x16, [1])
			socket.send(test)
			data = socket.recv(1024)
			self.__decode(data,socket)
			test = self.create_message_frame(0x19, [1])
			socket.send(test)
			data = socket.recv(1024)
			self.__decode(data,socket)
			test = self.create_message_frame(0x1E, [1])
			socket.send(test)
			data = socket.recv(1024)
			self.__decode(data,socket)
			test = self.create_message_frame(0xBD, [1,0])
			socket.send(test)
			data = socket.recv(1024)
			self.__decode(data,socket)
			while self.cond==True:
				data = socket.recv(1024)
				self.__decode(data,socket)
				

		except Exception, err:
			print "Device %s disconnected" % self.address
			socket.close()
			self.activeZephyrs.remove(self.address)
			self.stop()
			raise


	def stop(self):
		self.cond=False

	def __get_type_of_packet(self,data):
		if ord(data[1]) == 35:
			print("Received LifeSign message of %d bytes" % len(data))
		elif ord(data[1]) == 44:
			print("Received Event message of %d bytes" % len(data))
		elif ord(data[1]) == 43:
			print("Received Summary Data Packet of %d bytes" % len(data))
			print "************************************************************System confidence: %d" %ord(data[40]) 
		elif ord(data[1]) == 40:
			print("Received Extended Data Packet of %d bytes" % len(data))
		elif ord(data[1]) == 37:
			print("Received Accelerometer Data Packet of %d bytes" % len(data))
		elif ord(data[1]) == 36:
			print("Received R to R Data Packet of %d bytes" % len(data))
		elif ord(data[1]) == 32:
			print("Received General Data of %d bytes" % len(data))
		elif ord(data[1]) == 33:
			print("Received Breathing Data Packet of %d bytes" % len(data))
		elif ord(data[1]) == 34:
			print("Received ECG Packet of %d bytes" % len(data))
		else:
			print("Packet type: %d", ord(data[1]))
			print("Received Not recognised message of %d bytes" % len(data))


    
	def __decode(self, data, socket):
		# if self.address != "E0:D7:BA:A7:F1:5D":
		# 	print "test bytes %d %d %d %d" % (ord(data[0]),ord(data[1]),ord(data[2]),ord(data[3]))
		# if self.address == "E0:D7:BA:A7:F1:5D":
		# # print("************************************")        
		# 	self.__get_type_of_packet(data)
		# # print("************************************")      
		print "%s %d bytes sent" % (self.address, len(data))
		if ord(data[1]) == 35:
			lifesign = self.create_message_frame(0x23, [0])
			socket.send(lifesign)



	def crc_8_digest(self,values):
		crc = 0
    
		for byte in values:
			crc ^= byte
			for i in range(8):  #@UnusedVariable
				if crc & 1:
					crc = (crc >> 1) ^ 0x8C
				else:
					crc = (crc >> 1)
    
		return crc

	def create_message_frame(self,message_id, payload):
		dlc = len(payload)
		assert 0 <= dlc <= 128
    
		crc_byte = self.crc_8_digest(payload)
    
		message_bytes = [0x02, message_id, dlc] + payload + [crc_byte, 0x03]
    
		message_frame = "".join(chr(byte) for byte in message_bytes)
		return message_frame    

# print "Soy el hilo principal, inicio el hilo de escaneo";
scan = ScanThread()
scan.start()
signal.signal(signal.SIGINT, signal_handler)
# print 'Press Ctrl+C to stop'
signal.pause()

