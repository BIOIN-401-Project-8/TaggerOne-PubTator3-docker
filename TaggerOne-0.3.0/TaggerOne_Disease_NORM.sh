CP=libs/taggerOne.jar
CP=${CP}:libs/trove-3.0.3.jar
CP=${CP}:libs/dragontool.jar
CP=${CP}:libs/heptag.jar
CP=${CP}:libs/fastutil-7.0.6.jar
CP=${CP}:libs/jopt-simple-4.9.jar
CP=${CP}:libs/commons-math3-3.5.jar
CP=${CP}:libs/bioc.jar
CP=${CP}:libs/stax-utils.jar
CP=${CP}:libs/stax2-api-3.1.1.jar
CP=${CP}:libs/woodstox-core-asl-4.2.0.jar
CP=${CP}:libs/slf4j-api-1.7.20.jar
CP=${CP}:libs/slf4j-simple-1.7.20.jar
PR="-Dorg.slf4j.simpleLogger.defaultLogLevel=debug -Dorg.slf4j.simpleLogger.showThreadName=false -Dorg.slf4j.simpleLogger.showLogName=false -Dorg.slf4j.simpleLogger.logFile=System.out"
PR="${PR} -Dorg.slf4j.simpleLogger.log.ncbi.util.Profiler=debug"
MODEL=output/model_DISE2023.bin
INPUT=$1
OUTPUT=$2
ABBR=$3
OPT="--input ${INPUT}"
OPT="${OPT} --output ${OUTPUT}"
OPT="${OPT} --modelInputFilename ${MODEL}"
OPT="${OPT} --compileModel false"
OPT="${OPT} --abbreviationDir ${ABBR}"
echo ${OPT}
java ${PR} -Xmx24G -Xms24G -cp ${CP} ncbi.taggerOne.NormalizeMentions ${OPT}
