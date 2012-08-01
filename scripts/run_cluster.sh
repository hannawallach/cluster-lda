#!/bin/bash

# Usage: e.g., mkdir -p results/sge; qsub -t 1-5 -cwd -V -o results/sge/stdout_patents_core_100_10000_0.5_0.0_false_DP.txt -e results/sge/stderr_patents_core_100_10000_0.5_0.0_false_DP.txt ./scripts/run_cluster.sh patents_core 100 10000 0.5 0.0 false DP results/lda/patents_core/T100-S1000-SAMPLE11-ID1/state.txt.gz.1000

ID=`expr $SGE_TASK_ID`
make results/cluster_lda/$1/T$2-C1-SG$3-SC1-ST1-THETA$4-EPS$5-SAMPLE$6-PERCLUSTERtrue-DOCCOUNTStrue-$7-ID${ID} T=$2 C=1 SG=$3 SC=1 ST=1 THETA=$4 EPS=$5 SAMPLE=$6 PERCLUSTER=true DOCCOUNTS=true PRIOR=$7 ID=${ID} STATE_FILE=$8
