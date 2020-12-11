JFLAGS = -g -cp out -d out
JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

SRC_PATH = src/

CLASS_PATH = out/

CLASSES = $(shell find $(SRC_PATH) -regex ".*\.\(java\)")

default:
	$(JC) $(JFLAGS) ${CLASSES}

clean:
	rm -rf ${CLASS_PATH}

run:
	cd src & java -cp out com.arpan.peerProcess $(peerId)
