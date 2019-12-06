JFLAGS = -g -d
JC = javac
CLASSPATH = ../build/classes/
MKDIR_P = mkdir -p
.SUFFIXES: .java .class
.java.class:
	$(MKDIR_P) ../build/
	$(MKDIR_P) $(CLASSPATH)
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
	jar cvfe ../build/LinearRegressionSplitData.jar Client.LinearRegressionSplitData -C $(CLASSPATH) Client -C $(CLASSPATH) Utility resources 
	jar cvfe ../build/LinearRegressionComputeRMSE.jar Client.LinearRegressionComputeRMSE -C $(CLASSPATH) Client -C $(CLASSPATH) Utility resources
	jar cvfe ../build/LinearRegressionComputeDamfRMSE.jar Client.LinearRegressionComputeDamfRMSE -C $(CLASSPATH) Client -C $(CLASSPATH) Utility resources
	jar cvfe ../build/Party.jar Party.Party -C $(CLASSPATH) Model -C $(CLASSPATH) Party -C $(CLASSPATH) Protocol -C $(CLASSPATH) Utility -C $(CLASSPATH) Communication -C $(CLASSPATH) TrustedInitializer resources
	jar cvfe ../build/TI.jar TrustedInitializer.TI -C $(CLASSPATH) TrustedInitializer -C $(CLASSPATH) Utility -C $(CLASSPATH) Communication resources
	jar cvfe ../build/BA.jar BroadcastAgent.BA -C $(CLASSPATH) BroadcastAgent -C $(CLASSPATH) Utility -C $(CLASSPATH) Communication resources

clean:
	rm -rf $(CLASSPATH)
	rm -f ../build/*.jar
