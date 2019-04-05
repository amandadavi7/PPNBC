######################################################
# Script to turn IF-THEN rules of a decision tree to a properties file that can be used
# by the Lynx Framework
#
# Input: 
# --file = "ifthen" txt file (representation of decision tree that is part of a tree ensemble)
# --depth = depth of the tree (int)
# --roundoff = constant multiplier to map real numbers to integers (int)
# --classlabels = number of class labels (int)
# --bitlength = bit length (int)
# --features = csv file of attribute names (as one row)
#
# Output: 
# properties file representation of decision tree, format of input to Lynx Framework
######################################################

import argparse
import collections
import numpy as np
import pandas as pd

# Takes the if statement and returns the attribute index and threshold value
def process_if(if_statement):
    index = attributes.index(if_statement.split('"')[1])
    threshold = int(float(if_statement.rsplit(' ', 1)[1].split(':')[0]) * roundoff) + 1 # add 1 because threshold is different than sklearn
    return [index, threshold]

# Takes the return statement and returns the probability vector (mapped to integers) of that leaf
def process_return(return_statement):
    split_list = return_statement.split('[[')[1].split(']]')[0].split() # grabs only the numbers
    counts = []
    denominator = 0
    for i in range(classlabels):
        num = float(split_list[i])
        counts.append(num)
        denominator += num

    for i in range(classlabels):
        counts[i] = int(roundoff * float(counts[i])/denominator) # convert to int
    return counts 

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument('--file', help='tree file')
    parser.add_argument('--depth', help="depth of tree", type=int)
    parser.add_argument('--roundoff', help="constant multiplier", type=int)
    parser.add_argument('--classlabels', help="no. of class labels", type=int)
    parser.add_argument('--bitlength', help="attribute bit length", type=int)
    parser.add_argument('--features', help='tree features')
    args = parser.parse_args()
    filename = args.file
    depth = args.depth
    roundoff = args.roundoff
    classlabels = args.classlabels
    bitLength = args.bitlength

    df = pd.read_csv(args.features)
    singleString = ':'.join(np.squeeze(pd.read_csv(args.features).columns))
    attributes = singleString.split(':')
    print("The attribute list is {}".format(attributes))
    print("\nTotal number of attributes is {}\n".format(len(attributes)))

    cur_node_index = 0
    node_to_attr_index_mapping = ""
    leaf_to_prob_vector_mapping = ""
    attribute_thresholds = ""
    tree_node_index_value_mapping = dict()

    # Map if/return lines to corresponding nodes in tree
    with open(filename) as f:
        lines = (f.readlines())
        for i in lines:
            line = i.split('\n')[0]
            print(line)
            if line.startswith("if"):
                tree_node_index_value_mapping[cur_node_index] = line
                cur_node_index = cur_node_index*2 + 2
            if line.startswith("return"):
                tree_node_index_value_mapping[cur_node_index] = line
                if cur_node_index % 2 == 0:
                    cur_node_index -= 1
                else:
                    while cur_node_index > 0 and cur_node_index % 2 == 1:
                        cur_node_index = (cur_node_index - 1) / 2
                    cur_node_index -= 1


    leaf_to_class_index = [None] * pow(2, depth)
    
	# Move return string to corresponding leaf nodes
    num_intl_nodes = pow(2,depth) - 1
    for i in range(num_intl_nodes):
        if tree_node_index_value_mapping.get(i):
            if tree_node_index_value_mapping.get(i).startswith("return"):
                left = 2*i + 1
                right = 2*i + 2
                return_string = tree_node_index_value_mapping.pop(i)
                tree_node_index_value_mapping[left] = return_string
                tree_node_index_value_mapping[right] = return_string
  
    # Map internal nodes to attributes and thresholds
    x = 0
    power = pow(2, depth)
    for x in range(power-1):
        if tree_node_index_value_mapping.get(x):
            result = process_if(tree_node_index_value_mapping.get(x))
            if node_to_attr_index_mapping == "":
                node_to_attr_index_mapping = "{}".format(result[0])
            else:
                node_to_attr_index_mapping = "{},{}".format(node_to_attr_index_mapping, result[0])
            if attribute_thresholds == "":
                attribute_thresholds = "{}".format(result[1])
            else:
                attribute_thresholds = "{},{}".format(attribute_thresholds, result[1])
        else:
            if node_to_attr_index_mapping == "":
                node_to_attr_index_mapping = "{}".format(attributes.index("dummy"))
            else:
                node_to_attr_index_mapping = "{},{}".format(node_to_attr_index_mapping, attributes.index("dummy"))
            if attribute_thresholds == "":
                attribute_thresholds = "{}".format(1)
            else:
                attribute_thresholds = "{},{}".format(attribute_thresholds, 1)
        x += 1
    
    # Map leaf nodes to probability vectors	
    power = pow(2, depth+1)
    while x < power-1:
        if tree_node_index_value_mapping.get(x):
            result = process_return(tree_node_index_value_mapping.get(x))
            value = 0
			# Format probability vectors for each leaf
            for i in range(len(result)):
                value = result[i]
                if leaf_to_prob_vector_mapping == "":
                    leaf_to_prob_vector_mapping = "{}".format(value)
                elif i == 0:
                    leaf_to_prob_vector_mapping = "{},{}".format(leaf_to_prob_vector_mapping, value)
                else:
                    leaf_to_prob_vector_mapping = "{}:{}".format(leaf_to_prob_vector_mapping, value)

        x += 1

    # Write the properties file for stored tree details (tree owner)
    fp = open(filename+"_data.properties", "w")
    fp.write("depth=" + str(depth) + "\n")
    fp.write("attribute.count=" + str(len(attributes)) + "\n")
    fp.write("attribute.bitlength=" + str(bitLength) + "\n")
    fp.write("classlabel.count=" + str(classlabels) + "\n")
    fp.write("node.to.attribute.index.mapping=" + node_to_attr_index_mapping + "\n")
    fp.write("attribute.thresholds=" + attribute_thresholds + "\n")
    fp.write("leaf.to.class.index.mapping=" + leaf_to_prob_vector_mapping)
    fp.close()
    
    # Write the properties file for stored tree properties (feature vector owner)
    fp = open(filename+"_score.properties", "w")
    fp.write("depth=" + str(depth) + "\n")
    fp.write("attribute.count=" + str(len(attributes)) + "\n")
    fp.write("attribute.bitlength=" + str(bitLength) + "\n")
    fp.write("classlabel.count=" + str(classlabels) + "\n")
    fp.close()