#!/bin/bash
i=$1
baseoutputpath="test/pax-test-"
clientPath="test/client_data/"

echo "========================================================================"
    outputpath=$baseoutputpath$i"/"

    #start ti
    java -cp build/classes/ TrustedInitializer.TI 3000 $i 5 5 50000 > /dev/null &
    pids[0]=$!
    echo "Started ti; process id:"${pids[0]}

    #start ba
    java -cp build/classes/ BroadcastAgent.BA port=4000 partyCount=$i > /dev/null &
    pids[1]=$!
    echo "Started ba; process id:"${pids[1]}
    

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
            echo "Started party $j; process id:"${pids[$((2+$j))]}
            startPort=$(($startPort+100))
	done

    echo "All processes:"
    echo ${pids[*]}
    for pid in ${pids[*]}; do
        wait $pid
    echo "========================================================================"
    done
