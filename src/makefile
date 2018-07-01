JFLAGS = -g -d
JC = javac
CLASSPATH = ../build/classes/
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $(CLASSPATH) $*.java

CLASSES = \
        ./BroadcastAgent/*.java \
        ./Client/*.java \
        ./Communication/*.java \
        ./Model/*.java \
        ./Party/*.java \
        ./Protocol/*.java \
        ./Protocol/Utility/*.java \
        ./TrustedInitializer/*.java \
        ./Utility/*.java

default: classes

classes: $(CLASSES:.java=.class)

jar: $(CLASSES:.java=.class)
	jar cvf ../build/Client.jar $(CLASSPATH)Client/* $(CLASSPATH)Utility/* 
	jar cvf ../build/Party.jar $(CLASSPATH)Model/* $(CLASSPATH)Party/* $(CLASSPATH)Protocol/* $(CLASSPATH)Utility/* $(CLASSPATH)Communication/*
	jar cvf ../build/TI.jar $(CLASSPATH)TrustedInitializer/* $(CLASSPATH)Utility/* $(CLASSPATH)/Communication/*
	jar cvf ../build/BA.jar $(CLASSPATH)BroadcastAgent/* $(CLASSPATH)Utility/* $(CLASSPATH)Communication/*

clean:
	rm -rf $(CLASSPATH)*
