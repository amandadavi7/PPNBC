#!/bin/bash
# loop through directories in tests folder
for dir in ./*/;do
    CURRDIR="$dir"         # dir is directory only because of the / after *
    #loop through n-party directories
    for testcase in ${CURRDIR}*/; do
	CURRTESTCASE="$testcase"
	#loop through clients
	for client in ${CURRTESTCASE}*/; do
		CLIENT="$client"
		RESULTS="${CLIENT}results"
		#clean up results folder
		rm -f $RESULTS/*		
    	done
    done
done
