This directory contains scripts and code to run PubTator 3 normalization for Diseases and CellLines.

Diseases and CellLines are both based on trained TaggerOne models, modified to only perform normalization.

There are four scripts: one for each entity type, file format combination.

The parameters for each script are the same:
	1. Input directory. All files within this directory will be processed, regardless of contents or file extension.
	2. Abbreviation directory. All files within this directory will be loaded as abbreviation TSV files
	3. Output directory. The is where the file output will be placed. Files with the same name as a file in the input directory will always be overwritten. All other files will be ignored.

Specifically:
	./run_Disease_BioCXML.sh input abbr output
	./run_Disease_PubTator.sh input abbr output
	./run_CellLine_BioCXML.sh input abbr output
	./run_CellLine_PubTator.sh input abbr output

Internally the scripts use several variables:
	1. A BASE_DIR variable to reference the full path of the installation. This will need to be updated if moved or copied.
	2. A BASE_TEMP_DIR variable which specifies the full path of a directory where processes can create temporary folders. The user starting the scripts must have write access to this directory. In addition, if a process errors or is cancelled the temp folder for that process will need to be deleted manually.
