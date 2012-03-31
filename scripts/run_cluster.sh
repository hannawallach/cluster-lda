#!/bin/bash

# Usage: e.g., mkdir -p results/sge; qsub -t 1-5 -cwd -V -o results/sge/stdout_0.5_0.0_DP_patents_core_100.txt -e results/sge/stderr_0.5_0.0_DP_patents_core_100.txt ./scripts/run_cluster.sh 0.5 0.0 DP patents_core 100 results/lda/patents_core/T100-S1000-SAMPLE11-ID1/state.txt.gz.1000

ID=`expr $SGE_TASK_ID`
make results/cluster_lda/$4/T$5-C25-SG2000-SC1-ST1-THETA$1-EPS$2-SAMPLEfalse-PERCLUSTERfalse-DOCCOUNTStrue-$3-ID${ID} T=$5 C=25 SG=2000 SC=1 ST=1 THETA=$1 EPS=$2 SAMPLE=false PERCLUSTER=false DOCCOUNTS=true PRIOR=$3 ID=${ID} STATE_FILE=$6
