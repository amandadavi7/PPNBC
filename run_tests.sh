#!/bin/bash

totalruns=14

for ( i=2 ; i <= $totalruns; i++ )
  do  
    outputpath="test/pax-test-"$i
    mkdir -p $outputpath
    java -cp build/classes/ Client.LinearRegressionClient $i "test/client_data/subject_14_thetaPower.csv" $outputpath
  done

# For each test case run the parties to generate the output
for ( i=2 ; i <= $totalruns; i++ )
  do  
    outputpath="test/pax-test-"$i
    java -cp build/classes/ TrustedInitializer.TI 3000 5 5 50000 &
    pids[$i]=$!
    # run n parties
    startPort=4000
    for (j=0; j <= $i; j++)
	do
	    java -cp build/classes/ Party.Party party_port=($startPort+100) ti=127.0.0.1:3000 peer_port=127.0.0.1:5000 party_id=$j yCsv=/home/anisha/Documents/PPML\ test\ files/BETA_0.csv xCsv="test/pax-test-$i/thetaPower_$j.csv" oneShares=0 &
	    pids[$i+$j]=$!
	done
    for pid in ${pids[*]}; do
        wait $pid
    done
  done



