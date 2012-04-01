#!/bin/bash

# Usage: e.g., mkdir -p results/sge; qsub -t 1-5 -cwd -V -o results/sge/stdout_lda_patents_core_100_1000_11.txt -e results/sge/stderr_lda_patents_core_100_1000_11.txt ./scripts/run_lda.sh patents_core 100 1000 11

ID=`expr $SGE_TASK_ID`
make results/lda/$1/T$2-S$3-SAMPLE$4-ID${ID} T=$2 S=$3 SAMPLE=$4 ID=${ID}
