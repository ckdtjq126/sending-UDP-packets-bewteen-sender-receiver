#!/bin/bash
all:
	javac sender.java
	javac receiver.java
	clear

run:
	clear
	./nEmulator 1235 129.97.167.52 5676 5677 129.97.167.53 5671 1 0.2 0
sender:
	javac sender.java
	clear
	java sender 129.97.167.51 1235 5671 input
receiver:
	javac receiver.java
	clear
	java receiver 129.97.167.51 5677 5676 output
clean:
	rm -rf *.class