######################################################
# Script to extract IF-THEN rules as a txt file from a Pickled classifier, and turn into properties file
#
# Classifiers supported: Decision Tree, Random Forest, Ada Boost Classifier
# 
# Input: 
# --file = file name to be used as the names of txt and properties files
# --depth = depth of the tree (int)
# --roundoff = constant multiplier to map real numbers to integers (int)
# --classlabels = number of class labels (int)
# --bitlength = bit length (int)
# --features = csv file of attribute names (as one row)
# --classifier = name of the pickled file (ex: adaboost.pkl)
######################################################

import extract_if_then
import argparse
import os
from sklearn.externals import joblib
import sklearn
import pandas as pd
import numpy as np

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument('--file', help='tree file')
    parser.add_argument('--depth', help="depth of tree", type=int)
    parser.add_argument('--roundoff', help="constant multiplier", type=int)
    parser.add_argument('--classlabels', help="no. of class labels", type=int)
    parser.add_argument('--bitlength', help="attribute bit length", type=int)
    parser.add_argument('--features', help='tree features')
    parser.add_argument('--classifier', help='pickled classifier')

    args = parser.parse_args()
    filename = args.file
    depth = args.depth
    roundoff = args.roundoff
    classlabels = args.classlabels
    bitLength = args.bitlength
    features = args.features
    
    singleString = ':'.join(np.squeeze(pd.read_csv(args.features).columns)) #changed .values to .columns 
    attributes = singleString.split(':')
    
    clf = joblib.load(args.classifier)
    
    ##### Random Forest Classifier 
    if (type(clf) == sklearn.ensemble.forest.RandomForestClassifier):
        extract_if_then.printRF(clf, features=attributes, filename=filename)
        pf = 'propertyfiles='
        sf = 'propertyfiles='
        for i in range(len(clf.estimators_)):
            os.system('python3 generate_property_tree_ensemble.py --file ' + filename + str(i) + ' --depth ' + str(depth) + ' --roundoff ' + str(roundoff) + ' --classlabels ' + str(classlabels) + ' --bitlength ' + str(bitLength) + ' --features ' + features)
            pf = pf + filename + str(i) + '_data.properties,'
            sf = sf + filename + str(i) + '_score.properties,'
        pf = pf[0:len(pf) -1]
        sf = sf[0:len(sf) -1]
        treecount = 'treecount=' + str(clf.n_estimators)
        print(pf,file=open(filename + '.randomforeststored.properties', "a"))
        print(treecount,file=open(filename + '.randomforeststored.properties', "a"))
        print('bitLength=' + str(bitLength),file=open(filename + '.randomforeststored.properties', "a"))
        
        print(sf,file=open(filename + '.randomforestscore.properties', "a"))
        print(treecount,file=open(filename + '.randomforestscore.properties', "a"))
        print('bitLength=' + str(bitLength),file=open(filename + '.randomforestscore.properties', "a"))
    
    ##### Decision Tree Classifier        
    elif (type(clf) == sklearn.tree.tree.DecisionTreeClassifier):
        extract_if_then.printDT(clf, features=attributes, filename=filename)
        os.system('python3 generate_property_files_dec_tree.py --file ' + filename + ' --depth ' + str(depth) + ' --roundoff ' + str(roundoff) + ' --classlabels ' + str(classlabels) + ' --bitlength ' + str(bitLength) + ' --features ' + features)
    
    ##### Ada Boost Classifier
    elif (type(clf) == sklearn.ensemble.AdaBoostClassifier):
        extract_if_then.printADA(clf, features=attributes, filename=filename)
        pf = 'propertyfiles='
        sf = 'propertyfiles='
        for i in range(len(clf.estimators_)):
            os.system('python3 generate_property_tree_ensemble.py --file ' + filename + str(i) + ' --depth ' + str(depth) + ' --roundoff ' + str(roundoff) + ' --classlabels ' + str(classlabels) + ' --bitlength ' + str(bitLength) + ' --features ' + features)
            pf = pf + filename + str(i) + '_data.properties,'
            sf = sf + filename + str(i) + '_score.properties,'
        pf = pf[0:len(pf) -1]
        sf = sf[0:len(sf) -1]
        treecount = 'treecount=' + str(clf.n_estimators)
        print(pf,file=open(filename + '.adastored.properties', "a"))
        print(treecount,file=open(filename + '.adastored.properties', "a"))
        print('bitLength=' + str(bitLength),file=open(filename + '.adastored.properties', "a"))
        
        print(sf,file=open(filename + '.adascore.properties', "a"))
        print(treecount,file=open(filename + '.adascore.properties', "a"))
        print('bitLength=' + str(bitLength),file=open(filename + '.adascore.properties', "a"))
    