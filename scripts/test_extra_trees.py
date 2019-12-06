import argparse
import os
import pandas as pd
import numpy as np


round_off = 1
party_shares_dir = ""
feature_mappings_dir = ""
decision_trees_file_dir = ""
testing_dataset_dir = ""
party_count = 2
tree_count = 0
attr_count = 0
feature_count = 0


class TreeNode:
    def __init__(self, _attr=None, _val=None, _parent=None):
        self.attr = _attr
        self.val = _val
        self.parent_node = _parent
        self.children = None
        if _attr is not None:
            self.children = []


class Tree:
    def __init__(self, _root=None):
        self.root = _root

    def evaluate(self, X, thresholds):
        global round_off

        current_node = self.root
        while current_node.children is not None:
            attr_index = current_node.attr
            if int(float(X[int(attr_index)]) * round_off) < thresholds[int(attr_index)]:
                attr_val = 0
            else:
                attr_val = 1
            current_node = current_node.children[int(float(attr_val))]
        y_pred = current_node.val
        return y_pred


def build_tree(tree_str, attr_value_count):
    tree_segs = tree_str.split(",")
    current_root = None
    tree = Tree()
    for item in tree_segs:
        key_value = item.split("=")
        key = key_value[0]
        value = key_value[1]
        current_node = TreeNode()
        if key == "attr":
            current_node.attr = value
            current_node.children = []
        else:
            current_node.val = value
        if current_root is None:
            current_root = current_node
            tree.root = current_node
        else:
            while len(current_root.children) == attr_value_count:
                current_root = current_root.parent_node
            current_node.parent_node = current_root
            current_root.children.append(current_node)
            if key == "attr":
                current_root = current_node
    return tree


def calculateThreshold(party_count, tree_count, attr_count, feature_count):
    global party_shares_dir, feature_mappings_dir

    party_shares_file_pattern = os.path.join(party_shares_dir, "Party_{}_log_tree_{}.txt")
    selected_thresholds = [[0] * feature_count for t in range(tree_count)]
    for p in range(party_count):
        for t in range(tree_count):
            party_file_name = party_shares_file_pattern.format(p, t)
            with open(party_file_name, "r") as party_file:
                for line in party_file:
                    values = [int(v[::-1], 2) for v in line.strip().split()]
                    for i in range(feature_count):
                        selected_thresholds[t][i] ^= values[i]

    feature_mappings_file_pattern = os.path.join(feature_mappings_dir, "Selected_Feature_Mapping_tree_{}.txt")
    thresholds = [[0] * attr_count for t in range(tree_count)]
    for t in range(tree_count):
        selected_feature_mapping_file_name = feature_mappings_file_pattern.format(t)
        with open(selected_feature_mapping_file_name, "r") as selected_feature_mapping_file:
            for line in selected_feature_mapping_file:
                feature_mapping = [int(m) for m in line.strip().split()]
                for i in range(feature_count):
                    thresholds[t][feature_mapping[i]] = selected_thresholds[t][i]

    return thresholds


def test():
    global decision_trees_file_dir, testing_dataset_dir
    global party_count, tree_count, attr_count, feature_count

    forest = []
    with open(os.path.join(decision_trees_file_dir, "eT_result.txt"), "r") as file:
        for cnt, line in enumerate(file):
            tree = build_tree(line, 2)
            forest.append(tree)
    X = []
    y = []
    with open(os.path.join(testing_dataset_dir, "testing_X.csv"), "r") as file:
        for cnt, line in enumerate(file):
            if cnt != 0:
                X.append(line.split(","))
    with open(os.path.join(testing_dataset_dir, "testing_y.csv"), "r") as file:
        for cnt, line in enumerate(file):
            if cnt != 0:
                y.append(int(line))

    accurate_cnt = 0

    thresholds = calculateThreshold(party_count, tree_count, attr_count, feature_count)
    for i in range(0, len(X)):
        result_cnt = {"0": 0, "1": 0}
        result = 0
        tree_index = 0
        for tree in forest:
            tep_result = tree.evaluate(X[i], thresholds[tree_index])
            tep_result = tep_result.strip()
            if tep_result == "0":
                result_cnt["0"] += 1
            else:
                result_cnt["1"] += 1
            tree_index += 1;
        if result_cnt["0"] < result_cnt["1"]:
            result = 1
        if int(y[i]) == result:
            accurate_cnt += 1
    accuracy = accurate_cnt / len(X)
    print("accuracy: {}".format(accuracy))

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--round_off", help = "round off factor", required = True)
    parser.add_argument("--party_shares_dir", help = "directory containing files for party shares", required = True)
    parser.add_argument("--feature_mappings_dir", help = "directory containing files for feature mappings", required = True)
    parser.add_argument("--decision_trees_file_dir", help = "directory containing file with decision trees", required = True)
    parser.add_argument("--testing_dataset_dir", help = "directory containing files for test dataset", required = True)
    parser.add_argument("--party_count", help = "number of parties", required = True)
    parser.add_argument("--tree_count", help = "number of decision trees in ensemble", required = True)
    parser.add_argument("--attr_count", help = "number of columns in dataset", required = True)
    parser.add_argument("--feature_count", help = "number of selected features", required = True)
    args = parser.parse_args()

    round_off = int(args.round_off)
    party_shares_dir = args.party_shares_dir
    feature_mappings_dir = args.feature_mappings_dir
    decision_trees_file_dir = args.decision_trees_file_dir
    testing_dataset_dir = args.testing_dataset_dir
    party_count = int(args.party_count)
    tree_count = int(args.tree_count)
    attr_count = int(args.attr_count)
    feature_count = int(args.feature_count)

    test()