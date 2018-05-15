#!/bin/bash

totalruns=14

for (( i=2 ; i <= $totalruns; i++ ))
  do  
    outputpath="test/pax-test-"$i
    mkdir -p $outputpath
    java -cp build/classes/ Client.LinearRegressionClient $i "test/client_data/subject_14_thetaPower.csv" $outputpath
  done



