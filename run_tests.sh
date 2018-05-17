#!/bin/bash

#TODO this is for lr-test now Do it for damf too 

totaltests=14
baseoutputpath="test/pax-test-"
clientPath="test/client_data/"

#STEP 1: run the client job
for ( i=2 ; i <= $totaltests; i++ )
  do  
    outputpath="test/pax-test-"$i
    mkdir -p $outputpath
    java -cp build/classes/ Client.LinearRegressionClient $i $clientPath"subject_14_thetaPower.csv" $outputpath
  done

#STEP 2: Copy all beta files from SMPC testsuits

for ( i=2 ; i <= $totaltests; i++ )
  do  
      for (j=0; j <= $i; j++)
	do
	    cp "../SMPCEngine/SMPCEngine/tests/lr-pax-test/"$i"-pax/"$j"/results/BETA.csv" $baseoutputpath$i"/BETA_"$j".csv"
	done
    
  done

#STEP 3: For each test case run the parties to generate the output
for ( i=2 ; i <= $totaltests; i++ )
  do  
    outputpath=$baseoutputpath$i"/"

    #start ti
    java -cp build/classes/ TrustedInitializer.TI 3000 5 5 50000 &
    pids[0]=$!
    echo "Started ti; process id:"$pid[0]

    #start ba
    java -cp build/classes/ BroadcastAgent.BA port=1000 partyCount=2 &
    pids[1]=$!
    echo "Started ba; process id:"$pid[1]

    # run n parties
    startPort=4000
    for (j=0; j <= $i; j++)
	do
	    java -cp build/classes/ Party.Party party_port=($startPort+100) ti=127.0.0.1:3000 peer_port=127.0.0.1:5000 party_id=$j yCsv=$outputpathBETA_"$j".csv xCsv=$outputpath"thetaPower_$j.csv" oneShares=0 output=$outputpath &
	    pids[2+$j]=$!
            echo "Started party; process id:"$pids[2+$j]
	done

    for pid in ${pids[*]}; do
        wait $pid
    done

#STEP 4: TODO merge outputfiles and calculate rmse

for ( i=2 ; i <= $totaltests; i++ )
  do  
    outputpath=$baseoutputpath$i"/"
    python3 calculate_rmse.py $outputpath $clientPath
  done

  done



