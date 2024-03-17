import codecs
import collections
import datetime
import os
import sys
from tqdm import tqdm
from bioc import biocxml

def read_normalized(input_filename):
	mention_key2identifier = dict()
	if input_filename.endswith(".gz"):
		file = gzip.open(input_filename, 'rt', encoding="utf-8") 
	else:
		file = codecs.open(input_filename, 'r', encoding="utf-8") 
	for line in file:
		line = line.strip()
		if len(line) > 0:
			fields = line.split("\t")
			mention_key = tuple(fields[:3])
			identifier = fields[3]
			mention_key2identifier[mention_key] = identifier
	file.close()
	return mention_key2identifier

def process_PubTator(input_filename, output_filename, mention_key2identifier):
	input_file = codecs.open(input_filename, 'r', encoding="utf-8") 
	output_file = codecs.open(output_filename, 'w', encoding="utf-8") 
	for line in input_file:
		line = line.strip()
		fields = line.split("\t")
		if len(fields) == 1:
			output_file.write(line + "\n")
			continue
		docid = fields[0]
		start = fields[1]
		end = fields[2]
		mention_text = fields[3]
		entity_type = fields[4]
		# identifier = fields[5]
		mention_key = (docid, mention_text, entity_type)
		identifier = mention_key2identifier.get(mention_key)
		if not identifier is None:
			if identifier.startswith("UNKNOWN_"):
				identifier = ""
			output_file.write("{}\t{}\t{}\t{}\t{}\t{}\n".format(docid, start, end, mention_text, entity_type, identifier))
	input_file.close()
	output_file.close()

def process_BioCXML(input_filename, output_filename, mention_key2identifier):
	with open(input_filename, "r") as fp:
		collection = biocxml.load(fp)
	for document in collection.documents:
		for passage in document.passages:
			annotations = passage.annotations.copy()
			passage.annotations.clear()
			for annotation in annotations:
				mention_key = (document.id, annotation.text, annotation.infons.get("type"))
				identifier = mention_key2identifier.get(mention_key)
				print("mention key = {}; identifier found = {}".format(mention_key, identifier))
				if not identifier is None:
					if identifier.startswith("UNKNOWN_"):
						# NOTE: BioC infons are weird: del works, pop doesn't
						# DO NOT USE annotation.pop("identifier", None)
						if "identifier" in annotation.infons:
							del annotation.infons["identifier"]
					else:
						annotation.infons["identifier"] = identifier
					passage.annotations.append(annotation)					
	with open(output_filename, "w") as fp:
		biocxml.dump(collection, fp)
	
if __name__ == "__main__":
	start = datetime.datetime.now()
	if len(sys.argv) != 5:
		print("Usage: <input path> <input format> <normalized> <output path>")
		exit()
	input_path = sys.argv[1]
	input_format = sys.argv[2].lower()
	mention_key2identifier = read_normalized(sys.argv[3])
	output_path = sys.argv[4]
	
	if input_format == "pubtator":
		process_file = process_PubTator
	elif input_format == "biocxml":
		process_file = process_BioCXML
	else:
		raise ValueError("Unknown format: {}".format(input_format))
	
	start = datetime.datetime.now()
	if os.path.isdir(input_path):
		if not os.path.isdir(output_path):
			raise RuntimeError("If input path is a directory then output path must be a directory: " + output_path)
		print("Processing directory " + input_path)
		# Process any xml files found
		dir = tqdm(os.listdir(input_path))
		for item in dir:
			input_filename = input_path + "/" + item
			output_filename = output_path + "/" + item
			if os.path.isfile(input_filename):
				# print("Processing file " + input_filename + " to " + output_filename)
				process_file(input_filename, output_filename, mention_key2identifier)
	elif os.path.isfile(input_path):
		if os.path.isdir(output_path):
			raise RuntimeError("If input path is a file then output path may not be a directory: " + output_path)
		print("Processing file " + input_path + " to " + output_path)
		# Process directly
		process_file(input_path, output_path, mention_key2identifier)
	else:  
		raise RuntimeError("Path is not a directory or normal file: " + input_path)
	print("Total processing time = " + str(datetime.datetime.now() - start))
	print("Done.")

