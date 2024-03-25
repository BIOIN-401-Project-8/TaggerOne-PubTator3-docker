set -e
INPUT=$(realpath ${1})
ABBR=$(realpath ${2})
OUTPUT=$(realpath ${3})

mkdir -p ${OUTPUT}

BASE_DIR=$(pwd)
TAGGERONE_DIR="${BASE_DIR}/TaggerOne-0.3.0"
BASE_TEMP_DIR="${BASE_DIR}/temp"

cd ${BASE_DIR}

# Create a reasonably unique name for TEMP_DIR
HOST=$(hostname)
DATE=$(date "+%Y%m%d%H%M%S")
RAND=${RANDOM}
TEMP_DIR="${BASE_TEMP_DIR}/${HOST}_${DATE}_${RAND}"
echo "TEMP_DIR=${TEMP_DIR}"
mkdir -p ${TEMP_DIR}

# Extract mentions
INPUT_MENTIONS="${TEMP_DIR}/mentions.tsv"
python -u src/extract_mentions.py ${INPUT} BioCXML ${INPUT_MENTIONS}

# Normalize mentions
OUTPUT_MENTIONS="${TEMP_DIR}/normalized.tsv"
cd ${TAGGERONE_DIR}
./TaggerOne_CellLine_NORM.sh ${INPUT_MENTIONS} ${OUTPUT_MENTIONS} ${ABBR}

# Insert normalized mentions
cd ${BASE_DIR}
python -u src/insert_normalized.py ${INPUT} BioCXML ${OUTPUT_MENTIONS} ${OUTPUT}

rm -rf ${TEMP_DIR}
