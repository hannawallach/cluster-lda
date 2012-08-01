BUILD_DIR = build
LIBS_DIR = libs
SRC_DIR = src
DATA_DIR = data
RESULTS_DIR = results
SCRIPTS_DIR = scripts

MAX_HEAP = 1500m

JAVA_FLAGS = -server -Xmx$(MAX_HEAP) -XX:MaxPermSize=500m
#JAVA_FLAGS = -server -enableassertions -Xmx$(MAX_HEAP) -XX:MaxPermSize=500m

CP = $(BUILD_DIR):$(LIBS_DIR)/mallet.jar:$(LIBS_DIR)/mallet-deps.jar

# by default simply compile source code

all: $(BUILD_DIR)

.PHONY: $(BUILD_DIR)

# compilation is handled by ant

$(BUILD_DIR): #clean
	ant build

# experiments...

.PRECIOUS: $(DATA_DIR)/patents/%

$(DATA_DIR)/patents/%: $(DATA_DIR)/patents/%.tar.gz
	tar zxvf $< -C $(@D)

$(DATA_DIR)/patents_%.dat: $(DATA_DIR)/patents/%
	java $(JAVA_FLAGS) \
	-classpath $(CP) \
	cc.mallet.classify.tui.Text2Vectors \
	--keep-sequence \
	--remove-stopwords \
	--extra-stopwords $(DATA_DIR)/stopwordlist.txt \
	--output $@ \
	--input $<

$(DATA_DIR)/%.dat: $(DATA_DIR)/%.csv
	java $(JAVA_FLAGS) \
	-classpath $(CP) \
	cc.mallet.classify.tui.Csv2Vectors \
	--keep-sequence \
	--output $@ \
	--input $<

$(RESULTS_DIR)/lda/%/T$(T)-S$(S)-SAMPLE$(SAMPLE)-ID$(ID):
	mkdir -p $@; \
	I=`expr $(S) / 10`; \
	java $(JAVA_FLAGS) \
	-classpath $(CP) \
        edu.umass.cs.wallach.cluster.LDAExperiment \
	$(DATA_DIR)/$*.dat \
	$(T) \
	$(S) \
	20 \
	$$I \
	$(SAMPLE) \
	$@ \
	> $@/stdout.txt

$(RESULTS_DIR)/cluster_word/%/C$(C)-SG$(SG)-SC$(SC)-THETA$(THETA)-EPS$(EPS)-SAMPLE$(SAMPLE)-PERCLUSTER$(PERCLUSTER)-DOCCOUNTS$(DOCCOUNTS)-$(PRIOR)-ID$(ID):
	mkdir -p $@; \
	I=`expr $(SG) / 100`; \
	java $(JAVA_FLAGS) \
	-classpath $(CP) \
        edu.umass.cs.wallach.cluster.ClusterWordExperiment \
	$(DATA_DIR)/$*.dat \
	$(C) \
	$(SG) \
	$(SC) \
	$$I \
	$(THETA) \
	$(EPS) \
	$(SAMPLE) \
	$(PERCLUSTER) \
	$(DOCCOUNTS) \
	$(PRIOR) \
	$@ \
	> $@/stdout.txt

# STATE_FILE should be a gzipped state file in MALLET format

$(RESULTS_DIR)/cluster_topic/%/T$(T)-C$(C)-SG$(SG)-SC$(SC)-THETA$(THETA)-EPS$(EPS)-SAMPLE$(SAMPLE)-PERCLUSTER$(PERCLUSTER)-DOCCOUNTS$(DOCCOUNTS)-$(PRIOR)-ID$(ID):
	mkdir -p $@; \
	I=`expr $(SG) / 100`; \
	java $(JAVA_FLAGS) \
	-classpath $(CP) \
        edu.umass.cs.wallach.cluster.ClusterFeatureExperiment \
	$(DATA_DIR)/$*.dat \
	$(STATE_FILE) \
	$(T) \
	$(C) \
	$(SG) \
	$(SC) \
	$$I \
	$(THETA) \
	$(EPS) \
	$(SAMPLE) \
	$(PERCLUSTER) \
	$(DOCCOUNTS) \
	$(PRIOR) \
	$@ \
	> $@/stdout.txt

# STATE_FILE should be a gzipped state file in MALLET format

$(RESULTS_DIR)/cluster_lda/%/T$(T)-C$(C)-SG$(SG)-SC$(SC)-ST$(ST)-THETA$(THETA)-EPS$(EPS)-SAMPLE$(SAMPLE)-PERCLUSTER$(PERCLUSTER)-DOCCOUNTS$(DOCCOUNTS)-$(PRIOR)-ID$(ID):
	mkdir -p $@; \
	I=`expr $(SG) / 100`; \
	java $(JAVA_FLAGS) \
	-classpath $(CP) \
        edu.umass.cs.wallach.cluster.ClusterLDAExperiment \
	$(DATA_DIR)/$*.dat \
	$(STATE_FILE) \
	$(T) \
	$(C) \
	$(SG) \
	$(SC) \
	$(ST) \
	$$I \
	$(THETA) \
	$(EPS) \
	$(SAMPLE) \
	$(PERCLUSTER) \
	$(DOCCOUNTS) \
	$(PRIOR) \
	$@ \
	> $@/stdout.txt

$(RESULTS_DIR)/cluster/%/num_clusters.txt:
	./$(SCRIPTS_DIR)/aggregate_num_clusters.sh > $@

# FEATURES_FILE should be a file containing one line per topic, each
# line starting with "Feature <num>: " and then followed by ten words

# INPUT_FILE should be the cluster_features.txt file

.PHONY: extract-features

extract-features:
	java $(JAVA_FLAGS) \
	-classpath $(CP) \
	edu.umass.cs.wallach.cluster.ExtractTopFeatures \
	$(INPUT_FILE) \
	$(FEATURES_FILE) \
	$(NUM) \
	$(DUPS)

clean:
	ant clean
