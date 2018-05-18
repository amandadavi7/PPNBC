import os
import sys
import numpy as np
import pandas as pd
from math import sqrt
from sklearn.metrics import mean_squared_error

# TODO replace RT from everywhere
cwd = os.getcwd()
f = 64
divisor=2**f
print(divisor)

def sumPredictedShares(dataPath, datafile):
    totalY = pd.DataFrame()
    for x in os.listdir(dataPath):
        # iterate through the files to combine data from from drivers
        if os.path.isfile(cwd+"/"+dataPath+x):
            if "y_" in x:
                print("Considered file:"+ x)
                dfRT = pd.DataFrame.from_csv(cwd+"/"+dataPath+x, header=None, index_col=None)
                dfRT = dfRT.astype(float)
                dfRT = dfRT.divide(divisor)
                totalY = totalY.add(dfRT)
                print(totalY.describe)
    return totalY


# iterate through the files and test for each driver, avg the rmse - evaluation
rms_lr = []
outputPath = sys.argv[1]
clientDataPath = sys.argv[2]

clientY = pd.DataFrame.from_csv(cwd+"/"+clientDataPath+"subject_14_RT.csv", header=None, index_col=None)
print(clientY.shape)
actualYArray = clientY[0].values

for i in range(0,14):
    predictedY = sumPredictedShares(outputPath,"RT")
    print(predictedY.shape)
    predictedYArray = predictedY[0].values
    rms_lr.append(sqrt(mean_squared_error(actualYArray, predictedYArray)))
    
avg_rms_lr = float(sum(rms_lr))/len(rms_lr)
print(avg_rms_lr)


