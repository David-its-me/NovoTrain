package sensorsAktuators;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import main.Main;
import modelling.Locomotive;
import modelling.Switch;

public class Z21 implements ControlUnit, Runnable {

	// ------------------------------------------------------------
	private final int timeout = 20;
	// ------------------------------------------------------------
	private final DatagramSocket broadcastSocket;
	private final InetAddress ip_addr;
	private boolean closed = true;

	public Z21() throws Exception {
		closed = false;
		broadcastSocket = new DatagramSocket();
		System.out.println("Socket for communication to Z21 opened at port " + broadcastSocket.getLocalPort());
		ip_addr = InetAddress.getByAddress(Main.S21_IP4);
		sendSetBroadcastFlag();
		new Thread(this).start();
	}

	private void sendSetBroadcastFlag() {
		byte message[] = new byte[8];
		message[0] = (0x08);
		message[1] = (0x00);
		message[2] = (0x50);
		message[3] = (0x00);
		message[4] = (0x00);
		message[5] = (0x00);
		message[6] = (0x01);
		message[7] = (0x00);
		send(message);
	}

	private void sendDeleteBroadcastFlag() {
		byte message[] = new byte[8];
		message[0] = (0x08);
		message[1] = (0x00);
		message[2] = (0x50);
		message[3] = (0x00);
		message[4] = (0x00);
		message[5] = (0x00);
		message[6] = (0x00);
		message[7] = (0x00);
		send(message);
	}

	@Override
	public void close() {
		closed = true;
		sendDeleteBroadcastFlag();
		broadcastSocket.close();
		System.out.println("Socket for communicatin to Z21 closed");
		
	}

	private static byte xor(byte data[]) {
		byte xor = 0;
		for (int i = 0; i < data.length; i++) {
			xor = (byte) (data[i] ^ xor);
		}
		return xor;
	}

	private void send(byte[] data) {
		DatagramPacket datagram = new DatagramPacket(data, data.length);
		datagram.setAddress(ip_addr);
		datagram.setPort(Main.S21_PORT);
		try {
			broadcastSocket.send(datagram);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private byte[] sendAndReceive(byte[] message) {
		DatagramPacket datagram = new DatagramPacket(message, message.length);
		try {
			DatagramSocket mySocket = new DatagramSocket();
			datagram.setAddress(ip_addr);
			datagram.setPort(Main.S21_PORT);

			mySocket.setSoTimeout(timeout);
			mySocket.send(datagram);
			mySocket.receive(datagram);
			mySocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return datagram.getData();
	}

	@Override
	public void setTrackPowerOff() {
		byte message[] = new byte[7];
		message[0] = (0x07);
		message[1] = (0x00);
		message[2] = (0x40);
		message[3] = (0x00);
		message[4] = (0x21);
		message[5] = (byte) (0x80);
		message[6] = xor(message);
		send(message);
	}

	@Override
	public void setTrackPowerOn() {
		byte message[] = new byte[7];
		message[0] = (0x07);
		message[1] = (0x00);
		message[2] = (0x40);
		message[3] = (0x00);
		message[4] = (0x21);
		message[5] = (byte) (0x81);
		message[6] = xor(message);
		send(message);
	}

	@Override
	public void stop() {
		byte message[] = new byte[7];
		message[0] = (0x06);
		message[1] = (0x00);
		message[2] = (0x40);
		message[3] = (0x00);
		message[4] = (byte) (0x80);
		message[5] = (byte) (0x80);
		sendAndReceive(message);
		assert (message[0] == (0x07));
		assert (message[1] == (0x00));
		assert (message[2] == (0x40));
		assert (message[3] == (0x00));
		assert (message[4] == (byte) (0x81));
		assert (message[5] == (0x00));
		assert (message[6] == (byte) (0x80));
	}

	private static byte getAddressMSB(Locomotive locomotive) {
		return (byte) ((0xc0) | (locomotive.getDccAddress() / 256));
	}

	private static byte getAddressLSB(Locomotive locomotive) {
		return (byte) (locomotive.getDccAddress() % 256);
	}

	private static int demarshallAdress(byte MSB, byte LSB) {
		int msb = (MSB & (0x3f));
		int lsb = LSB;
		if (lsb < 0) {
			lsb = lsb + 256;
		}
		return msb * 256 + lsb;
	}

	private static byte getSpeedFormat(Locomotive locomotive, byte dccSpeed) {
		if (locomotive.isDirectionDecoder()) {
			return (byte) ((0x80) | dccSpeed);
		}
		return dccSpeed;
	}

	@Override
	public void setDccSpeed(Locomotive locomotive, int dccSpeed) {
		assert (0 <= dccSpeed && dccSpeed < Main.MAX_DCC_VALUES);

		byte message[] = new byte[14];
		message[0] = (0x0A);
		message[1] = (0x00);
		message[2] = (0x40);
		message[3] = (0x00);
		message[4] = (byte) (0xe4);
		message[5] = (0x13);
		message[6] = getAddressMSB(locomotive);
		message[7] = getAddressLSB(locomotive);
		message[8] = getSpeedFormat(locomotive, (byte) dccSpeed);
		message[9] = xor(message);
		send(message);
	}

	@Override
	public void setLightsOn(Locomotive locomotive) {
		this.setLocoFunction(locomotive, (byte) 0, FunctionSwitchingType.ON);

	}

	@Override
	public void setLightsOff(Locomotive locomotive) {
		this.setLocoFunction(locomotive, (byte) 0, FunctionSwitchingType.OFF);
	}

	@Override
	public void setLocoFunction(Locomotive locomotive, byte functionIndex, FunctionSwitchingType switchType) {
		assert (functionIndex < 64);
		byte message[] = new byte[10];
		message[0] = (0x0A);
		message[1] = (0x00);
		message[2] = (0x40);
		message[3] = (0x00);
		message[4] = (byte) (0xe4);
		message[5] = (byte) (0xf8);
		message[6] = getAddressMSB(locomotive);
		message[7] = getAddressLSB(locomotive);
		switch (switchType) {
		case OFF:
			message[8] = functionIndex;
			break;
		case ON:
			message[8] = (byte) ((0x40) | functionIndex);
			break;
		case SWITCH:
			message[8] = (byte) (0x80 | functionIndex);
			break;
		}
		message[9] = xor(message);
		send(message);
	}

	private static byte[] generateLocoInfoMessage(Locomotive locomotive) {
		byte message[] = new byte[14];
		message[0] = (0x09);
		message[1] = (0x00);
		message[2] = (0x40);
		message[3] = (0x00);
		message[4] = (byte) (0xe3);
		message[5] = (byte) (0xf0);
		message[6] = getAddressMSB(locomotive);
		message[7] = getAddressLSB(locomotive);
		message[8] = xor(message);
		return message;
	}

	@Override
	public boolean isLightOn(Locomotive locomotive) {
		byte response[] = sendAndReceive(generateLocoInfoMessage(locomotive));
		if ((response[9] & (0x10)) == (0x10)) {
			return true;
		}
		return false;
	}

	@Override
	public int getDccSpeed(Locomotive locomotive) {
		byte[] response = sendAndReceive(generateLocoInfoMessage(locomotive));
		return response[8] & ((byte) (0x7f));
	}

	@Override
	public boolean getDirection(Locomotive locomotive) {
		byte[] response = sendAndReceive(generateLocoInfoMessage(locomotive));
		if (((byte) (0x80) & response[8]) == (byte) (0x80)) {
			return true;
		}
		return false;
	}

	@Override
	public boolean getSwitchState(Switch switch_) {
		// TODO Auto-generated method stub
		assert(false);
		return false;
	}

	@Override
	public void setSwitch(Switch switch_, boolean SwitchState) {
		// TODO Auto-generated method stub
		assert(false);

	}

	private void printBoadcastMessage(byte[] response) {
		System.out.println("Z21 BROADCAST:");
		if (response[4] == (byte) (0xef)) {
			printLocoInfo(response);
		}
		else if(response[4] == (0x61)) {
			if(response[5] == (0x00)) {
				System.out.println("      TrackPowerOff");
			}else {
			System.out.println("      TrackPowerOn");
			}
		}else {
			System.out.println("      Andere Broadcast Nachricht empfangen: " + response.toString());
		}
		System.out.println();
	}
	
	private void resolveBoadcastMessage(byte[] response) {
		
		//Loco Info
		if (response[4] == (byte) (0xef)) {
			resolveLocoInfo(response);
		}
		
		//Track Power
		else if(response[4] == (0x61)) {
			if(response[5] == (0x00)) {
				//TODO Track Power Off
			}else {
				//TODO Track Power On
			}
		}
	}

	private void printLocoInfo(byte[] response) {
		int dccAdress = demarshallAdress(response[5], response[6]);
		System.out.println("      Lok Name: " + Locomotive.getLocomotiveByDccAddress(dccAdress).getName());
		System.out.println("      DCC Adresse: " + dccAdress);
		// DB2
		if (((0x08) & response[7]) == (0x08)) {
			System.out.println("      ...wird von einem Handregler gesteuert");
		} else {
			System.out.println("      ...wird von Steuerung gesteuert");
		}

		if (((0x04) & response[7]) == (0x04)) {
			System.out.println("      Fahrstufen: 128");
		} else if (((0x02) & response[7]) == (0x02)) {
			System.out.println("      Fahrstufen: 28");
		} else {
			System.out.println("      Fahrstufen: 14");
		}

		// DB3
		if (((byte) (0x80) & response[8]) == (byte) (0x80)) {
			System.out.println("      Richtung: Vorwärts (true)");
		} else {
			System.out.println("      Richtung: Rückwärts (false)");
		}
		System.out.println("      DCC Fahrstufe: " + (response[8] & ((byte) (0x7f))));

		
		// DB4
		if ((0x40 & response[9]) == 0x40) {
			System.out.println("      Doppeltraktion: true");
		} else {
			System.out.println("      Doppeltraktion: false");
		}

		if ((0x20 & response[9]) == 0x20) {
			System.out.println("      Smartsearch: true");
		} else {
			System.out.println("      Smartsearch: false");
		}

		if ((0x10 & response[9]) == 0x10) {
			System.out.println("      Licht(F0): EIN (true)");
		} else {
			System.out.println("      Licht(F0): AUS (false)");
		}

		if ((0x01 & response[9]) == 0x01) {
			System.out.println("      F1: true");
		} else {
			System.out.println("      F1: false");
		}

		if ((0x02 & response[9]) == 0x02) {
			System.out.println("      F2: true");
		} else {
			System.out.println("      F2: false");
		}

		if ((0x04 & response[9]) == 0x04) {
			System.out.println("      F3: true");
		} else {
			System.out.println("      F3: false");
		}

		if ((0x08 & response[9]) == 0x08) {
			System.out.println("      F4: true");
		} else {
			System.out.println("      F4: false");
		}

		//DB5,6,7
		int currentPosition;
		for(int i = 10; i < 13; i++) {
			currentPosition = 1;
			for(int j = 0; j < 8 ; j++ ) {
				if(((byte) currentPosition & response[i]) == (byte) currentPosition) {
					System.out.println("      F" + (5 + (i - 10)*8 + j) + ": true");
				}else {
					System.out.println("      F" + (5 + (i - 10)*8 + j) + ": false");
				}
				currentPosition = currentPosition * 2;
			}
		}
	}
	
	private void resolveLocoInfo(byte[] response) {
		int dccAdress = demarshallAdress(response[5], response[6]);
		Locomotive locomotive = Locomotive.getLocomotiveByDccAddress(dccAdress);
		// DB2
		
		//Wird von Handregler gesteuert
		//Daraus folgt, dass eine manuelle Geschwindigkeitsgrenze gesetzt wurde.
		if (((0x08) & response[7]) == (0x08)) {
			locomotive.setDccSpeedLimit((response[8] & ((byte) (0x7f))));
			locomotive.updateDccSpeed((response[8] & ((byte) (0x7f))));
		//Wir nicht von Handregler gesteuert
		//In diesem Fall hat das Programm die Geschwindigkeit gesetzt.
		} else {
			locomotive.updateDccSpeed((response[8] & ((byte) (0x7f))));
		}

		//128 Fahrstufen
		if (((0x04) & response[7]) == (0x04)) {
			assert(true);
		}
		//28 Fahrstufen
		 else if (((0x02) & response[7]) == (0x02)) {
			assert(false);
		}
		//14 Fahrstufen
		 else {
			assert(false);
		}

		// DB3
		//Direction  Decoder
		if (((byte) (0x80) & response[8]) == (byte) (0x80)) {
			locomotive.updateDirectionDecoder(true);
		} else {
			locomotive.updateDirectionDecoder(false);
		}
	}

	@Override
	public void run() {

		System.out.println("Start Z21 Broadcast Thread");
		byte buffer[] = new byte[16];
		DatagramPacket currentDatagram = new DatagramPacket(buffer, buffer.length);

		while (!closed) {
			try {
				broadcastSocket.receive(currentDatagram);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
			}
			printBoadcastMessage(currentDatagram.getData());
			resolveBoadcastMessage(currentDatagram.getData());
		}
		
		System.out.println("Z21 Broadcast Thread terminated");

	}

}
