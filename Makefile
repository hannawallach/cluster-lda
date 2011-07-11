BUILD_DIR = build
LIBS_DIR = libs
SRC_DIR = src
DATA_DIR = data
RESULTS_DIR = results
SCRIPTS_DIR = scripts

MAX_HEAP = 1500m

JAVA_FLAGS = -server -enableassertions -Xmx$(MAX_HEAP) -XX:MaxPermSize=500m

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

$(DATA_DIR)/patents/%.dat: $(DATA_DIR)/patents/%
	java $(JAVA_FLAGS) \
	-classpath $(CP) \
	cc.mallet.classify.tui.Text2Vectors \
	--keep-sequence \
	--remove-stopwords \
	--extra-stopwords $(DATA_DIR)/stopwordlist.txt \
	--output $@ \
	--input $<

$(RESULTS_DIR)/cluster/%/C$(C)-SG$(SG)-SC$(SC)-THETA$(THETA)-SAMPLE$(SAMPLE)-DOCCOUNTS$(DOCCOUNTS)-$(PRIOR)-ID$(ID):
	mkdir -p $@; \
	java $(JAVA_FLAGS) \
	-classpath $(CP) \
        edu.umass.cs.wallach.cluster.ClusterWordExperiment \
	$(DATA_DIR)/patents/$*.dat \
	$(C) \
	$(SG) \
	$(SC) \
	$(THETA) \
	$(SAMPLE) \
	$(DOCCOUNTS) \
	$(PRIOR) \
	$@ \
	> $@/stdout.txt

$(RESULTS_DIR)/cluster/%/C$(C)-SG$(SG)-SC$(SC)-THETA$(THETA)-SAMPLE$(SAMPLE)-DOCCOUNTS$(DOCCOUNTS)-$(PRIOR)-ID$(ID)/log_prob.txt:
	MAX=`expr $(SG) - 1`; \
	echo -n "" > $@; \
	for x in `seq 0 $$MAX`; do \
	  cat $(@D)/topic_log_prob.txt.$$x >> $(@); \
	done

$(RESULTS_DIR)/cluster/%/num_clusters.txt:
	./$(SCRIPTS_DIR)/aggregate_num_clusters.sh > $@

# FEATURES_FILE should be a file containing one line per topic, each
# line starting with "Feature <num>: " and then followed by ten words

# INPUT_FILE should be the cluster_features.txt file

.PHONY: extract-features

extract-features: $(BUILD_DIR)
	java $(JAVA_FLAGS) \
	-classpath $(CP):$(BUILD_DIR) \
	edu.umass.cs.wallach.cluster.ExtractTopFeatures \
	$(INPUT_FILE) \
	$(FEATURES_FILE) \
	$(C) \
	$(F) \
	$(NUM)

clean:
	ant clean
