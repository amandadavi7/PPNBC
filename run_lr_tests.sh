#!/bin/bash

#TODO this is for lr-test now Do it for damf too 

totaltests=$1

if [[ $# -eq 0 ]] ; then
    echo 'Usage: ./run_tests.sh <testCases>'
    exit 1
fi

clientPath="test/client_data/"

#STEP 1: distribute the client data, and get the model from the parties
. "scripts/lr_client.sh" $totaltests "lr-pax-test"
#. "scripts/lr_client.sh" $totaltests "rr-test"

#declare -a testSuite=('lr-pax-test' 'damf-test' 'rr-test');
declare -a testSuite=('lr-pax-test');

#STEP 2: For each test case run the parties to generate the output
for testCase in "${testSuite[@]}"
do
   baseoutputpath="test/"$testCase"/pax-test-"
   echo $testCase
   for (( i=2 ; i <= $totaltests; i++ ))
   do  
    echo "========================================================================"
    outputpath=$baseoutputpath$i"/"
    partyCount=$i

    #start ti
    java -cp build/classes/ TrustedInitializer.TI 3000 $partyCount 5 5 50000 > /dev/null &
    pids[0]=$!
    echo "Started ti; process id:"${pids[0]}

    #start ba
    java -cp build/classes/ BroadcastAgent.BA port=4000 partyCount=$partyCount > /dev/null &
    pids[1]=$!
    echo "Started ba; process id:"${pids[1]}
    
    #Let the TI and BA initialize the server socket
    sleep 10

    # run n parties
    startPort=5000
    for (( j=0; j < $partyCount; j++ ))
	do
            xFileName="thetaPower_"
            yFileName="BETA_"
            xCsvFile="$outputpath$xFileName$j.csv"
            yCsvFile="$outputpath$yFileName$j.csv"
            java -cp build/classes/ Party.Party party_port=$(($startPort+100)) ti=127.0.0.1:3000 ba=127.0.0.1:4000 party_id=$j yCsv="$yCsvFile" xCsv="$xCsvFile" oneShares=0 output=$outputpath model=2 partyCount=$partyCount &
	    pids[$((2+$j))]=$!
            echo "Started party $j; process id:"${pids[$((2+$j))]}
            startPort=$(($startPort+100))
	done

    echo "All processes:"
    echo ${pids[*]}
    for pid in ${pids[*]}; do
        wait $pid
    done

  done
done

#STEP 4: TODO merge outputfiles and calculate rmse
for testCase in "${testSuite[@]}"
do
    baseoutputpath="test/"$testCase"/pax-test-"
    for (( i=2 ; i <= $totaltests; i++ ))
    do  
        outputpath=$baseoutputpath$i"/"
        python3 calculate_rmse.py $outputpath $clientPath
  done
done




