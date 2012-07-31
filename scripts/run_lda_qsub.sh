#!/bin/bash

RUNS=5

S=1000
SAMPLE=11

DATA=evening_news

make data/${DATA}.dat

for T in `echo 10 20 30 40 50`; do
  mkdir -p results/sge; qsub -l long -t 1-${RUNS} -cwd -V -o results/sge/stdout_lda_${DATA}_${T}_${S}_${SAMPLE}.txt -e results/sge/stderr_lda_${DATA}_${T}_${S}_${SAMPLE}.txt ./scripts/run_lda.sh ${DATA} ${T} ${S} ${SAMPLE}
done

DATA=house_press_1000

make data/${DATA}.dat

for T in `echo 50 100 150 200 250`; do
  mkdir -p results/sge; qsub -t 1-${RUNS} -cwd -V -o results/sge/stdout_lda_${DATA}_${T}_${S}_${SAMPLE}.txt -e results/sge/stderr_lda_${DATA}_${T}_${S}_${SAMPLE}.txt ./scripts/run_lda.sh ${DATA} ${T} ${S} ${SAMPLE}
done
