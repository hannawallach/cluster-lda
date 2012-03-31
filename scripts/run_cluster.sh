#!/bin/bash

# Usage: e.g., mkdir -p results/sge; qsub -t 1-5 -cwd -V -o results/sge/stdout_0.5_UP.txt -e results/sge/stderr_0.5_UP.txt ./scripts/run_cluster.sh 0.5 UP

ID=`expr $SGE_TASK_ID`
make results/cluster_lda/core/C25-SG2000-SC1-THETA$1-SAMPLEfalse-DOCCOUNTStrue-$2-ID${ID} C=25 SG=2000 SC=1 THETA=$1 SAMPLE=false DOCCOUNTS=true PRIOR=$2 ID=${ID}
