JFLAGS = -g -cp "lib/*:."
JAVAC = javac
SRC = adb/

.SUFFIXES: .java .class
.java.class:
	$(JAVAC) $(JFLAGS) $*.java

JAVA_FILES = $(wildcard $(SRC)*.java)
CLASS = $(JAVA_FILES:.java=.class)

default: $(CLASS)

run:
	java -cp "lib/*:." adb.QueryCLI

clean:
	$(RM) $(CLASS)