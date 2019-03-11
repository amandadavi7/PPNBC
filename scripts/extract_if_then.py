import csv
import pandas as pd
import numpy as np
from collections import defaultdict
from sklearn.model_selection import train_test_split
from sklearn.tree import DecisionTreeClassifier
from sklearn.ensemble import RandomForestClassifier
from sklearn.ensemble import AdaBoostClassifier
from sklearn.metrics import accuracy_score
from sklearn.tree import _tree
import math
import re  


##################################################################
# Code to extract IF-THEN rules and estimator weights

def tree_to_code(tree, feature_names, weight, filename='output.txt'):
    tree_ = tree.tree_
    feature_name = [
        feature_names[i] if i != _tree.TREE_UNDEFINED else "undefined!"
        for i in tree_.feature
        ]
    
    def recurse(node, depth):
        indent = "" * depth
        if tree_.feature[node] != _tree.TREE_UNDEFINED:
            name = '"' + feature_name[node] +'"'
            threshold = tree_.threshold[node]
            print("{}if {} <= {}:".format(indent, name, threshold),file=open(filename, "a"))
            recurse(tree_.children_left[node], depth + 1)
            print("{}else:  # if {} > {}".format(indent, name, threshold),file=open(filename, "a"))
            recurse(tree_.children_right[node], depth + 1)
        else:
            value = str(tree_.value[node] * weight)
            line = re.sub('\[\[ *', '[[ ', value)
            line = line.replace('.  ', '. ')
            print("{}return {}".format(indent, line),file=open(filename, "a"))

    recurse(0, 1)

# Code to print a decision tree model
def printDT(clf, features,filename='output.txt'):
    tree_to_code(clf, features, 1, filename=filename)


# Code to print a Random Forest model
def printRF(clf, features, filename='output'):
    print(features)
    print("Number of trees:", clf.n_estimators)
    print([estimator.tree_.max_depth for estimator in clf.estimators_])
    for i in range(len(clf.estimators_)):
        tree_to_code(clf.estimators_[i], features, 1, filename=filename+str(i)) # weight is 1 for each tree in RF

# Code to print an AdaBoost model
def printADA(clf, features, filename='output'):
    print("Number of trees:", clf.n_estimators)
    print([estimator.tree_.max_depth for estimator in clf.estimators_])
    for i in range(len(clf.estimators_)):
        print("Estimator Weight: ", clf.estimator_weights_[i])
        print("Estimator: ")
        tree_to_code(clf.estimators_[i], features, clf.estimator_weights_[i], filename=filename+str(i))

