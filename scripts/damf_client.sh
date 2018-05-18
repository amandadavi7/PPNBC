#!/bin/bash

totaltests=$1
testSuite=$2
testPath="test/"
clientPath=$testPath"client_data/"
baseoutputpath="test/pax-test-"

if [[ $# -lt 2 ]] ; then
    echo 'Usage: ./lr_client.sh <testCases> <testSuite>'
    exit 1
fi


START=$(date +'%s')
for (( i=0 ; i <= $totaltests; i++ ))
  do  
    outputpath="test/"$testSuite"/pax-test-"$i
    mkdir -p $outputpath
    java -cp build/classes/ Client.LinearRegressionClient 2 $clientPath"subject_14_thetaPower.csv" $outputpath
  done
STOP=$(date +'%s')

echo "Client job done: $((STOP - START))sec"

for (( i=0 ; i <= $totaltests; i++ ))
  do  
      for ((j=0; j < 2; j++))
	do
            cp "../SMPCEngine/SMPCEngine/tests/damf-test/$i-pax/$j/results/BETA.csv" "test/"$testSuite"/pax-test-"$i"/BETA_"$j".csv"
	done
    
  done
