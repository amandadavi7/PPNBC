import os
import sys
import numpy as np
import pandas as pd
from math import sqrt
from sklearn.metrics import mean_squared_error

# TODO replace RT from everywhere
cwd = os.getcwd()
f = 2*64
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
                if totalY.empty:
                    totalY = dfRT
                else:
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



#in the clear calculation

def calculateLR(totalTP, totalRT):
    transposeX = totalTP.T
    XTX = transposeX.dot(totalTP)
    XTX_inv = pd.DataFrame(np.linalg.pinv(XTX.values), XTX.columns, XTX.index)
    GAMMA = transposeX.dot(totalRT)
    BETA = XTX_inv.dot(GAMMA)
    #print(BETA)
    return BETA


BETA_LR = calculateLR()
BETALR_values = BETA_LR[0].values
#evaluation in the clear
testDataPath = clientDataPath+"subject_14_thetaPower.csv"
testLabelPath = clientDataPath+"subject_14_RT.csv"
testData = pd.DataFrame.from_csv(testDataPath, header=None, index_col=None)
testRT = pd.DataFrame.from_csv(testLabelPath, header=None, index_col=None)
#print(testRT[0].values)
predictedValues = []
for index, row in testData.iterrows():
    testRow = row.values
    #print(testRow)
    predictedValue = np.dot(testRow,BETALR_values)
    #print(predictedValue)
    predictedValues.append(predictedValue)

rms = sqrt(mean_squared_error(testRT[0].values, predictedValues))

print(rms)
print(rms-avg_rms_lr)



