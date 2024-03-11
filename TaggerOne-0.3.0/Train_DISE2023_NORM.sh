echo TrainModel.sh $*
CP=libs/taggerOne.jar
CP=${CP}:libs/trove-3.0.3.jar
CP=${CP}:libs/dragontool.jar
CP=${CP}:libs/heptag.jar
CP=${CP}:libs/fastutil-7.0.6.jar
CP=${CP}:libs/commons-math3-3.5.jar
CP=${CP}:libs/jopt-simple-4.9.jar
CP=${CP}:libs/ojalgo-39.0.jar
CP=${CP}:libs/slf4j-api-1.7.20.jar
CP=${CP}:libs/slf4j-simple-1.7.20.jar
CP=${CP}:libs/bioc.jar
CP=${CP}:libs/stax-utils.jar
CP=${CP}:libs/stax2-api-3.1.1.jar
CP=${CP}:libs/woodstox-core-asl-4.2.0.jar

PR="-Dorg.slf4j.simpleLogger.defaultLogLevel=debug -Dorg.slf4j.simpleLogger.showThreadName=false -Dorg.slf4j.simpleLogger.showLogName=false -Dorg.slf4j.simpleLogger.logFile=System.out"
REGULARIZATION=$1 # Try 10.0
MAX_STEP_SIZE=$2 # Try 1.0
MODEL=$3
OPT="--entityTypes Disease"
OPT="${OPT} --lexiconNamespaces MESH|OMIM"
OPT="${OPT} --trainingDatasetConfig ncbi.taggerOne.dataset.NLMChemBioCDataset|data/lexicons/DiseaseNormOnly/train.xml|Disease->Disease|Disease->Identify"
OPT="${OPT} --holdoutDatasetConfig ncbi.taggerOne.dataset.NLMChemBioCDataset|data/lexicons/DiseaseNormOnly/dev.xml|Disease->Disease|Disease->Identify"
OPT="${OPT} --lexiconConfig ncbi.taggerOne.lexicon.loader.TabDelimitedLoader|data/LitCovid/lexicon.tsv"
OPT="${OPT} --lexiconConfig ncbi.taggerOne.lexicon.loader.CTDDiseaseLexiconMappingsLoader|Disease|data/lexicons/CTD_diseases_20230227_processed.tsv"
OPT="${OPT} --abbreviationSource ncbi.taggerOne.abbreviation.FileAbbreviationSource|abbreviations.tsv"
OPT="${OPT} --entityTokenizerClass ncbi.taggerOne.util.tokenization.SimpleTokenizer"
OPT="${OPT} --textInstanceTokenizerClass ncbi.taggerOne.util.tokenization.SimpleTokenizer"
OPT="${OPT} --stemmerClass ncbi.taggerOne.processing.string.PorterStemmer"
OPT="${OPT} --regularization ${REGULARIZATION}"
OPT="${OPT} --maxStepSize ${MAX_STEP_SIZE}"
OPT="${OPT} --topNLabelings 1"
OPT="${OPT} --topNNormalization 1"
OPT="${OPT} --iterationsPastLastImprovement 5"
OPT="${OPT} --maxTrainingIterations 100"
OPT="${OPT} --deterministicOrdering false"
OPT="${OPT} --averageRecognitionModel true"
OPT="${OPT} --averageNormalizationModels true"
OPT="${OPT} --modelOutputFilename ${MODEL}"
echo ${OPT}
java ${PR} -Xmx40G -Xms40G -cp ${CP} ncbi.taggerOne.TrainModel ${OPT}
HOSTNAME="$(hostname)"
