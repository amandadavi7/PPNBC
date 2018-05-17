#!/bin/bash

#TODO this is for lr-test now Do it for damf too 

totaltests=$1
baseoutputpath="test/pax-test-"
clientPath="test/client_data/"

echo $baseoutputpath
#STEP 1: run the client job
START=$(date +'%s')
#. "scripts/lr_client.sh"

STOP=$(date +'%s')
echo "Client job done: $((STOP - START))sec"

#STEP 2: Copy all beta files from SMPC testsuits
START=$(date +'%s')
for (( i=2 ; i <= $totaltests; i++ ))
  do  
      for ((j=0; j < $i; j++))
	do
            cp "../SMPCEngine/SMPCEngine/tests/lr-pax-test/$i-pax/$j/results/BETA.csv" "$baseoutputpath$i/BETA_$j.csv"
	done
    
  done
STOP=$(date +'%s')
echo "Copying beta done: $((STOP - START))sec"

#STEP 3: For each test case run the parties to generate the output
for (( i=2 ; i <= $totaltests; i++ ))
  do  
    echo "========================================================================"
    outputpath=$baseoutputpath$i"/"

    #start ti
    java -cp build/classes/ TrustedInitializer.TI 3000 5 5 50000 > /dev/null &
    pids[0]=$!
    echo "Started ti; process id:"$pids[0]

    #start ba
    java -cp build/classes/ BroadcastAgent.BA port=4000 partyCount=2 > /dev/null &
    pids[1]=$!
    echo "Started ba; process id:"$pids[1]
    

    # run n parties
    startPort=5000
    for (( j=0; j < $i; j++ ))
	do
            xFileName="thetaPower_"
            yFileName="BETA_"
            xCsvFile="$outputpath$xFileName$j.csv"
            yCsvFile="$outputpath$yFileName$j.csv"
            java -cp build/classes/ Party.Party party_port=$(($startPort+100)) ti=127.0.0.1:3000 ba=127.0.0.1:4000 party_id=$j yCsv="$yCsvFile" xCsv="$xCsvFile" oneShares=0 output=$outputpath model=2 &
	    pids[$((2+$j))]=$!
            echo "Started party $j; process id:"$pids[$((2+$j))]
            startPort=$(($startPort+100))
	done

    echo "All processes:"
    echo ${pids[*]}
    for pid in ${pids[*]}; do
        wait $pid
    echo "========================================================================"
    done

echo "Party computation done"
  done

#STEP 4: TODO merge outputfiles and calculate rmse
:'
for (( i=2 ; i <= $totaltests; i++ ))
  do  
    outputpath=$baseoutputpath$i"/"
    python3 calculate_rmse.py $outputpath $clientPath
  done
'


