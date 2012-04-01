#!/bin/bash

RUNS=5

S=1000
SAMPLE=11

DATA=fenno_lit

make data/${DATA}.dat

for T in `echo 2 5 10 25 50`; do
  mkdir -p results/sge; qsub -l long -t 1-${RUNS} -cwd -V -o results/sge/stdout_lda_${DATA}_${T}_${S}_${SAMPLE}.txt -e results/sge/stderr_lda_${DATA}_${T}_${S}_${SAMPLE}.txt ./scripts/run_lda.sh ${DATA} ${T} ${S} ${SAMPLE}
done

DATA=house_press_1000

make data/${DATA}.dat

for T in `echo 25 50 75 100 150`; do
  mkdir -p results/sge; qsub -t 1-${RUNS} -cwd -V -o results/sge/stdout_lda_${DATA}_${T}_${S}_${SAMPLE}.txt -e results/sge/stderr_lda_${DATA}_${T}_${S}_${SAMPLE}.txt ./scripts/run_lda.sh ${DATA} ${T} ${S} ${SAMPLE}
done
