import java.net.*;
import java.io.*;

class receiver{
	public static void main(String[] args) throws Exception{
		final int UDP_SIZE = 65535;
		final String arrivalLog = "arrival.log";
		
		int length = args.length;
		
		if(length != 4){
			throw new Exception("needs four arguments!");
		}

		// input parameters
		InetAddress hostname_emul = InetAddress.getByName(args[0]);
		int port_emul = Integer.parseInt(args[1]);
		int port_receiver = Integer.parseInt(args[2]);
		String outputFile = args[3].toString();

		/* recently sent ACK. if the first incoming packet is lost
		 * (i.e, if currentACK == -1),
		 *  ACK will not be sent. (special case)
		 */
		int currentACK = 0;
		int currentSeq = 0;
		
		// packet received from emulator
		packet receivedPacket;
		// ACK packet
		packet sendACK;
		
		// byte[] for received data and sending data
		byte[] receivedData = new byte[UDP_SIZE];
		byte[] sendData = new byte[UDP_SIZE];
		
		DatagramSocket receiveSocket = null;
		
		try{
		
			receiveSocket = new DatagramSocket(port_receiver);
		
			BufferedWriter out = new BufferedWriter(new FileWriter(outputFile));
			BufferedWriter outLog = new BufferedWriter(new FileWriter(arrivalLog));
		
			while(true){
				DatagramPacket receivedDataPacket = new DatagramPacket(receivedData, receivedData.length);
				DatagramPacket sendPacket;

				// packet received!
				receiveSocket.receive(receivedDataPacket);
				receivedData = receivedDataPacket.getData();
				receivedPacket = packet.parseUDPdata(receivedData);
			
				currentSeq = receivedPacket.getSeqNum();
			
				// if received packet is EOT
				if(receivedPacket.getType() == 2 && currentACK == currentSeq){
				
					// create EOT
					sendACK = packet.createEOT(currentACK);
				
					// convert packet to byte[]
					sendData = sendACK.getUDPdata();
					sendPacket = new DatagramPacket(sendData, sendData.length, hostname_emul, port_emul);
				
					// send
					receiveSocket.send(sendPacket);
				
					// terminate
					break;
				} // end of if
			
				// if received packet is not EOT and (ACK == SeqNum)
				else if(currentACK == currentSeq){
					System.out.println(currentSeq+" SeqNum received!");
					String transData = new String(receivedPacket.getData());
				
					// prints seqNum in arrival.log
					outLog.write(Integer.toString(currentSeq));
					outLog.newLine();
				
					// create ACK
					sendACK = packet.createACK(currentACK);
					// convert packet to byte[]
					sendData = sendACK.getUDPdata();
				
					sendPacket = new DatagramPacket(sendData, sendData.length, hostname_emul,port_emul);
				
					// send converted byte[] to emulator
					receiveSocket.send(sendPacket);
					out.write(transData);
					currentACK++;
				} // end of else if	
			
				// if received packet is not EOT and (ACK != SeqNum)
				else{
					// checks if the very first packet is lost.
					if(currentACK != -1){
						// create ACK
						sendACK = packet.createACK(currentACK-1);
						// convert packet to byte[]
						sendData = sendACK.getUDPdata();
						sendPacket = new DatagramPacket(sendData, sendData.length, hostname_emul,port_emul);
						// send
						receiveSocket.send(sendPacket);				
						System.out.println("ERROR! needed: "+currentACK+", received = "+currentSeq+". ACK: "+sendACK.getSeqNum()+" sent!");
					}
				} // end of else			
			
			} // end of while		
			
			// close everything before terminate
			outLog.close();
			out.close();
		}
		catch(Exception ex){
			System.err.println("ERROR: UDP data transfer failed!");
			System.exit(-1);
		}
		finally{
			receiveSocket.close();
		}
		
	} // end of main
	
} // end of class receiver