import argparse
import os

parser = argparse.ArgumentParser(argument_default=argparse.SUPPRESS, description="Generate input file for extra trees training.")
parser.add_argument("--input_file", type=str, help="Party's input file")
parser.add_argument("--total_rows", type=int, help="Total number of rows in combined dataset")
parser.add_argument("--party_id", type=int, help="Party ID")

args = parser.parse_args()

infile = open(args.input_file, "r")
input_file_name, input_file_extension = os.path.splitext(args.input_file)
outfile = open("{0}Input{1}".format(input_file_name, input_file_extension), "w")

num_columns = len(infile.readline().strip().split(","))
infile.seek(0)
all_rows = infile.readlines()
num_rows = len(all_rows)
zeros = ",".join(["0"] * num_columns)

if args.party_id == 0:
    outfile.writelines(all_rows)
    for i in range(args.total_rows - num_rows):
        outfile.write(zeros + "\n")
elif args.party_id == 1:
    for i in range(args.total_rows - num_rows):
        outfile.write(zeros + "\n")
    outfile.writelines(all_rows)

infile.close()
outfile.close()
