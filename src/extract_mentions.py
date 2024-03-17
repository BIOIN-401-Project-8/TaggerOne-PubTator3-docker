import codecs
import collections
import datetime
import os
import sys
import re
import lxml.etree
from tqdm import tqdm
from bioc import biocxml

combine_whitespace = re.compile(r"\s")

def process_PubTator(input_filename):
	mentions = set()
	file = codecs.open(input_filename, 'r', encoding="utf-8") 
	for line in file:
		line = line.strip()
		fields = line.split("\t")
		if len(fields) == 1:
			continue
		mentions.add((fields[0], fields[3], fields[4]))
	file.close()
	return mentions

def sanitize(text):
	return combine_whitespace.sub(" ", text).strip()

def process_BioCXML(input_filename):
	mentions = set()
	with open(input_filename, "r") as fp:
		try:
			collection = biocxml.load(fp)
		except lxml.etree.XMLSyntaxError:
			return mentions
	for document in collection.documents:
		for passage in document.passages:
			for annotation in passage.annotations:
				mentions.add((sanitize(document.id), sanitize(annotation.text), sanitize(annotation.infons.get("type"))))
	return mentions
		
if __name__ == "__main__":
	start = datetime.datetime.now()
	if len(sys.argv) != 3 and len(sys.argv) != 4:
		print("Usage: <input path> <input format> <output filename>")
		exit()
	input_path = sys.argv[1]
	input_format = sys.argv[2].lower()
	output_filename = sys.argv[3]
	
	if input_format == "pubtator":
		process_file = process_PubTator
	elif input_format == "biocxml":
		process_file = process_BioCXML
	else:
		raise ValueError("Unknown format: {}".format(input_format))
	
	mentions = set()
	if os.path.isdir(input_path):
		print("Processing directory " + input_path)
		# Process any xml files found
		dir = tqdm(os.listdir(input_path))
		for item in dir:
			input_filename = input_path + "/" + item
			if os.path.isfile(input_filename):
				# print("Processing file " + input_filename)
				mentions.update(process_file(input_filename))
	elif os.path.isfile(input_path):
		print("Processing file " + input_path)
		# Process directly
		mentions.update(process_file(input_path))
	else:  
		raise RuntimeError("Path is not a directory or normal file: " + input_path)

	with codecs.open(output_filename, 'w', encoding="utf-8") as output_file:
		for docid, mention_text, type in mentions:
			output_file.write("{}\t{}\t{}\n".format(docid, mention_text, type))
	print("Done.")