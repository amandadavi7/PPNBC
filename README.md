
# Privacy-Preserving Machine Learning Classifier

## Privacy-Preserving Naive Bayes Classifier (PPNBC)

This is a java version of the PPNBC protocol presented in [1].

## Prerequisites

The framework uses Java (>=1.8). Ensure that JDK and JRE are installed.

## Installing

To install the framework,

1.  Clone a copy of the main git repository by running:

	```
	git clone git://github.com/amandadavi7/PPNBC
	```
		
2. Build it:
	a) Run make to build and generate the class files
	b) Run make jar to generate class files and create JAR files
3. Four Executable Jar files will be created in the build directory: Party.jar, BA.jar, TI.jar and Client.jar

### Executing the Code

The framework needs a terminal for the Trusted Initializer, a terminal for the Broadcast Agent, and a terminal for each party (minimum 2 parties)

In order to run the framework, run the following commands to trigger each process in the respective terminal:

 - Trusted Initializer: The trusted initializer generates the Beavers Triples randomly and distributes the shares to the parties to introduce randomness.

#### Template
```
java -Dconfig.properties=config.properties -jar TI.jar port=<TI-Port> partyCount=<number of parties> decimal=<No. of Integer Triples> binary=<No. of Binary Triples (For bits)> real=<No. of Big Integer Triples (For real numbers)> truncation=<No. of Big Integer Pairs (For real numbers)>
```

#### Example to run

``` 
java -Dconfig.properties=../src/resources/config.properties -jar TI.jar port=4000 partyCount=2 decimal=1000 binary=1000 real=0 truncation=0
```

 - Broadcast Agent: The broadcast agent broadcasts the messages received from each party to all the other parties.
#### Template

``` 
java -Dconfig.properties=config.properties -jar BA.jar port=<BA Port> partyCount=<number of parties>
```
#### Example to run

```
java -Dconfig.properties=config.properties -jar BA.jar port=6000 partyCount=2
```

 - Party: The parties that execute the main model:

#### Template

```
java -Dconfig.properties=config.properties -jar Party.jar party_port=<Party Port> ti=<TrustedInitializer-IP>:<TrustedInitializer-Port> ba=<BroadcastAgent-IP>:<BroadcastAgnet-Port> party_id=<Party-ID> partyCount=<number of parties> hashLength<number of bits of each word> numberClasses<number of classes> model=<model-name> assymetricBit=<0 or 1 - This value should be 1 for one party and 0 for all the other parties> probWord=<CSV file with the secret share of the probabilities of each word> probClass<CSV file with the secret share of the probabilities of each class (spam/ham)> featureShares <CSV file with the secret share of each word in the dictionary> privateDocumentShares=<CSV file with the secret share of each word in an sms> 
```

#### Example to run in two terminals

```
java -Dconfig.properties=../src/resources/config.properties -jar Party.jar party_port=3000 ti=127.0.0.1:4000 ba=127.0.0.1:6000 party_id=1 partyCount=2 hashLength=14 numberClasses=2 model=NaiveBayesScoring asymmetricBit=0 probWord=../data_example/l_probs_0.csv probClass=../data_example/class_priors_0.csv featureShares=../data_example/word_training0.csv privateDocumentShares=../data_example/word_example0.csv 
```

```
java -Dconfig.properties=../src/resources/config.properties -jar Party.jar party_port=500 ti=127.0.0.1:4000 ba=127.0.0.1:6000 party_id=1 partyCount=2 hashLength=14 numberClasses=2 model=NaiveBayesScoring asymmetricBit=1 probWord=../data_example/l_probs_1.csv probClass=../data_example/class_priors_1.csv featureShares=../data_example/word_training1.csv privateDocumentShares=../data_example/word_example1.csv 
```

#### Output

This should print the following output in the one terminal: 

```
	PredictedClassLabel: 1
```
This should print the following output in the one terminal: 
```
	PredictedClassLabel: 1
```
or

This should print the following output in the one terminal: 
```
	PredictedClassLabel: 0
```
This should print the following output in the one terminal: 
```
	PredictedClassLabel: 0
```

The result is (1+1)%2 = 0 or (0+0)%2 = 0. This is a example of a SMS classified as not spam (ham).

### References

[1] Amanda Resende, Davis Railsback, Rafael Dowsley, Anderson Nascimento and Diego Aranha. Fast Privacy-Preserving Text Classification based on Secure Multiparty Computation. In Cryptol. ePrint Arch., 2021.

