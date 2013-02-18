package upc.edu.btstresstest.util;

public class BTMessage {

	public BTMessage() {
		// TODO Auto-generated constructor stub
	}
	
	public byte[] createMessage(byte messageId, byte[] payload){
		int length = payload.length;
		byte[] message = null;
		
		if(0 <= length && length <= 128){
			byte crc = crc8(payload);
			
			//byte[] message = [0x02, messageId, length] + 
		}
		
		return message;
	}
	
	private byte crc8(byte[] value){
		
		byte crc = 0;
		
		for (byte b : value) {
			crc ^= b;
			for(int i = 0; i<8; ++i){
				if((crc&1)==1)
					crc = (byte) ((crc >> 1) ^ 0x8C);
				else
					crc = (byte) (crc >> 1);						
			}
		}
		return crc;
	}
}
