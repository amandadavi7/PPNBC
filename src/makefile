JFLAGS = -g -d
JC = javac
CLASSPATH = ../build/classes/
MKDIR_P = mkdir -p
.SUFFIXES: .java .class

CLASSES = \
        ./BroadcastAgent/*.java \
        ./Communication/*.java \
        ./Model/*.java \
        ./Party/*.java \
        ./Protocol/*.java \
        ./Protocol/Utility/*.java \
        ./TrustedInitializer/*.java \
        ./Utility/*.java

.java.class:
	$(MKDIR_P) ../build/
	$(MKDIR_P) $(CLASSPATH)
	$(JC) $(JFLAGS) $(CLASSPATH) $(CLASSES)

default: .java.class

jar: $(.java.class)
	jar cvfe ../build/Party.jar Party.Party -C $(CLASSPATH) Model -C $(CLASSPATH) Party -C $(CLASSPATH) Protocol -C $(CLASSPATH) Utility -C $(CLASSPATH) Communication -C $(CLASSPATH) TrustedInitializer resources
	jar cvfe ../build/TI.jar TrustedInitializer.TI -C $(CLASSPATH) TrustedInitializer -C $(CLASSPATH) Utility -C $(CLASSPATH) Communication resources
	jar cvfe ../build/BA.jar BroadcastAgent.BA -C $(CLASSPATH) BroadcastAgent -C $(CLASSPATH) Utility -C $(CLASSPATH) Communication resources

clean:
	rm -rf $(CLASSPATH)
	rm -f ../build/*.jar
