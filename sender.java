import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;

import java.util.*;
import java.io.*;
import java.net.*;

class sender{
	
	private static final int UDP_SIZE = 65535;
	private static final int WINDOW_SIZE = 10;
	private static final int TIMEOUT = 5000;		
	private static final String seqNumLogFile = "seqnum.log";
	private static final String ackLogFile = "ack.log";
	
	private static InetAddress hostname_emul = null;
	private static int port_emul = 0;
	private static int port_sender = 0;
	private static String input = null;
	
	// timer 
	private static Timer socketTimer = null;
	// current sequence number
	private static int SeqNum = 0;		
	// recently received ACK
	private static int lastACK = 0;
	// open slots for data transfer (window size)
	private static int N = WINDOW_SIZE;
	// true if every data is sent (sender can still re-send)
	private static boolean sendingDone = false;
	
	private static List<String> inputList = null;
	
	private static BufferedReader inputReader = null;
	private static BufferedWriter seqNumLog = null;
	private static BufferedWriter ackLog = null;

	private static DatagramSocket senderSocket = null;
	
	private static DatagramPacket sendPacket;
	private static DatagramPacket receivePacket;

	private static packet dataPacket;
	private static packet receiveACK;
	private static byte[] sendData = new byte[UDP_SIZE];
	private static byte[] receiveData = new byte[UDP_SIZE];
	
	public static void main(String[] args) throws Exception{
		
		int length = args.length;
		
		if(length != 4){
			throw new Exception("needs four arguments!");
		}

		// input parameters
		hostname_emul = InetAddress.getByName(args[0]);
		port_emul = Integer.parseInt(args[1]);
		port_sender = Integer.parseInt(args[2]);
		input = args[3].toString();
		
		// storing data to re-transmit when timeout happens
		inputList = new LinkedList<String>();
		
		senderSocket = new DatagramSocket(port_sender);
		
		
		
		ActionListener actionListener = new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				try{
					System.out.println("hello  lastACK: "+lastACK+", seqNum: "+SeqNum);
					int i;
					// re-sending all outstanding packets
					for(i = lastACK; i < SeqNum; i++){
						if(i == lastACK){
							// start timer when start transmitting
							socketTimer.start();
						}
						System.out.println("SeqNum "+i+" resent!");
						dataPacket = packet.createPacket(i, inputList.get(i));				
						sendData = dataPacket.getUDPdata();
						sendPacket = new DatagramPacket(sendData, sendData.length, hostname_emul, port_emul);
						seqNumLog.write(Integer.toString(SeqNum)+'\n');
						senderSocket.send(sendPacket);
					}
				}
				catch(Exception e){
					System.err.println("ERROR during TIMEOUT: "+e.getMessage());
					System.exit(-1);
				}
			}
		};
		
		socketTimer = new Timer(TIMEOUT, actionListener);
	        
		try{
			// output writer
			seqNumLog = new BufferedWriter(new FileWriter(seqNumLogFile));
			ackLog = new BufferedWriter(new FileWriter(ackLogFile));
			// input reader
			inputReader = new BufferedReader(new FileReader(input));
			
			// put every string from input in the list
			while(true){
				// if inputLine is null, there is nothing to send
				String inputLine = inputReader.readLine();
				if(inputLine == null){
					break;
				}
				// if input exceeds packet's string length limit
				else if(inputLine.length() > 499){
					while(inputLine.length() < 500){
						inputList.add(inputLine.substring(0, 500));
						inputLine = inputLine.substring(500);
					}
				}
				inputList.add(inputLine+'\n');
			}
			
			inputReader.close();
			
			while(true){
				// send as much data as window size at once
				while(N != 0 && !sendingDone){
					// if there exists data to send
					if(SeqNum < inputList.size()){
						System.out.println("SeqNum "+SeqNum+" is sent!");
						seqNumLog.write(Integer.toString(SeqNum)+'\n');
						dataPacket = packet.createPacket(SeqNum, inputList.get(SeqNum));
						sendData = dataPacket.getUDPdata();
						sendPacket = new DatagramPacket(sendData, sendData.length, hostname_emul, port_emul);
						senderSocket.send(sendPacket);
						
						if(lastACK == SeqNum){
							// start the timer after sending packet
							socketTimer.start();
						}
						
						// once packet is sent we increment SeqNum and decrement available open spots
						SeqNum++;
						N--;
					}					
					// if every data is sent, send EOT packet
					else{
						dataPacket = packet.createEOT(SeqNum);		
				
						sendData = dataPacket.getUDPdata();
						sendPacket = new DatagramPacket(
								sendData, sendData.length, hostname_emul, port_emul);
						senderSocket.send(sendPacket);
						SeqNum++;
						sendingDone = true;
						break;
					}
				} // end of inner while
				
				// receive ACK packet from emulator
				receivePacket = new DatagramPacket(receiveData, receiveData.length);
				senderSocket.receive(receivePacket);
				receiveData = receivePacket.getData();
				receiveACK = packet.parseUDPdata(receiveData);

				// if EOT received
				if(receiveACK.getType() == 2){
					break;
				}
				else{
					// if exact ACK is received
					if(lastACK == receiveACK.getSeqNum()){
						ackLog.write(Integer.toString(receiveACK.getSeqNum())+'\n');
						
						// since we successfully received ACK packet, open an additional spot
						N++;
						// increment last received ACK
						lastACK++;
						
						// reset timer since we received ACK successfully
						socketTimer.stop();
						socketTimer.start();
					}
					
					/* when received ACK is bigger than the expected, overwrite the 
					 * previous ACK we had. 
					 */
					else if(lastACK < receiveACK.getSeqNum()){						
						// set lastACK to recently received ACK
						lastACK = receiveACK.getSeqNum();
					}
				}
			}
		}// end of try{}
			
			// timeout occurred!
			catch(Exception e){
				System.out.println("ERROR FOUND!");
				System.err.println("ERROR: "+e.getMessage());
				System.exit(-1);
			}
			/*
			catch(SocketTimeoutException e){
				System.out.println("TIMEOUT: sending all outstanding packets: ["+inputList.size()+"]");
				int i, SeqNumResend;
				// re-sending all outstanding packets
				for(i = 0, SeqNumResend = lastACK; i < inputList.size(); i++){
					System.out.println("SeqNum "+SeqNumResend+" resent!");
					dataPacket = packet.createPacket(SeqNumResend, inputList.get(i));				
					sendData = dataPacket.getUDPdata();
					sendPacket = new DatagramPacket(
							sendData, sendData.length, hostname_emul, port_emul);
					senderSocket.send(sendPacket);			
					SeqNumResend++;
				}
				if(inputList.size() < WINDOW_SIZE){
					System.out.println("EOT resent!");
					dataPacket = packet.createEOT(SeqNumResend);		
			
					sendData = dataPacket.getUDPdata();
					sendPacket = new DatagramPacket(
							sendData, sendData.length, hostname_emul, port_emul);
					senderSocket.send(sendPacket);					
				}
				
			} // end of catch
			*/		
		finally{
			// close everything before terminate
			seqNumLog.close();
			ackLog.close();
			senderSocket.close();
		}
			
	} // end of main
	
} // end of class sender