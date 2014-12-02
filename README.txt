
instructions
1. type 'make' - it will compile both sender.java and receiver.java
2. type './nEmulator p1 p2 p3 p4 p5 p6 p7 p8 p9, where
	p1 = <emulator's receiving UDP port number in the forward (sender) direction>,
	p2 = <receiver¡¯s network address>,
	p3 = <receiver¡¯s receiving UDP port number>,
	p4 = <emulator's receiving UDP port number in the backward (receiver) direction>,
	p5 = <sender¡¯s network address>,
	p6 = <sender¡¯s receiving UDP port number>,
	p7 = <maximum delay of the link in units of millisecond>,
	p8 = <packet discard probability>,
	p9 = <verbose-mode>
3. type java receiver r1 r2 r3 r4, where
	r1 = <hostname for the network emulator>
	r2 = <UDP port number used by the link emulator to receive ACKs from the receiver>
	r3 = <UDP port number used by the receiver to receive data from the emulator>
	r4 = <name of the file into which the received data is written>
4. type java sender s1 s2 s3 s4, where
	s1 = <host address of the network emulator>
	s2 = <UDP port number used by the emulator to receive data from the sender>
	s3 = <UDP port number used by the sender to receive ACKs from the emulator>
	s4 = <name of the file to be transferred>
5. log files are 'arrival.log', 'seqnum.log', and 'ack.log'

Sample Execution
1. type 'make run' on linux024
2. type 'make receiver' on linux028
3. type 'make sender' on linux032 will automatically test with delay = 0, discard rate = 0

my program is built and tested on 'linux024.cs.uwaterloo.ca' as emulator
my program is built and tested on 'linux028.cs.uwaterloo.ca' as receiver
my program is built and tested on 'linux032.cs.uwaterloo.ca' as sender

version of make: GNU Make 3.81
version of java compiler: javac 1.6.0_20
