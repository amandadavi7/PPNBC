#!/bin/bash

totaltests=$1
baseoutputpath="test/pax-test-"
clientPath="test/client_data/"

for (( i=2 ; i <= $totaltests; i++ ))
  do  
    outputpath="test/pax-test-"$i
    mkdir -p $outputpath
    java -cp build/classes/ Client.LinearRegressionClient $i $clientPath"subject_14_thetaPower.csv" $outputpath
  done
