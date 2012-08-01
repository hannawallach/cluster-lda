#!/bin/bash

RUNS=1

SG=10000
SAMPLE=false

DATA=evening_news

for T in `echo 10 20 30 40 50`; do

  STATE_FILE=results/lda/${DATA}/T${T}-S1000-SAMPLE11-ID1/state.txt.gz.1000

  for PRIOR in `echo DP`; do
    for THETA in `echo 0.1 0.2 0.5 1.0 2.0 5.0 10.0`; do
      for EPS in `echo 0.0`; do
        mkdir -p results/sge; qsub -l long -t 1-${RUNS} -cwd -V -o results/sge/stdout_${DATA}_${T}_${SG}_${THETA}_${EPS}_${SAMPLE}_${PRIOR}.txt -e results/sge/stderr_${DATA}_${T}_${SG}_${THETA}_${EPS}_${SAMPLE}_${PRIOR}.txt ./scripts/run_cluster.sh ${DATA} ${T} ${SG} ${THETA} ${EPS} ${SAMPLE} ${PRIOR} ${STATE_FILE}
      done
    done
  done
done

DATA=house_press_1000

for T in `echo 50 100 150 200 250`; do

  STATE_FILE=results/lda/${DATA}/T${T}-S1000-SAMPLE11-ID1/state.txt.gz.1000

  for PRIOR in `echo DP`; do
    for THETA in `echo 0.1 0.2 0.5 1.0 2.0 5.0 10.0`; do
      for EPS in `echo 0.0`; do
        mkdir -p results/sge; qsub -l long -t 1-${RUNS} -cwd -V -o results/sge/stdout_${DATA}_${T}_${SG}_${THETA}_${EPS}_${SAMPLE}_${PRIOR}.txt -e results/sge/stderr_${DATA}_${T}_${SG}_${THETA}_${EPS}_${SAMPLE}_${PRIOR}.txt ./scripts/run_cluster.sh ${DATA} ${T} ${SG} ${THETA} ${EPS} ${SAMPLE} ${PRIOR} ${STATE_FILE}
      done
    done
  done
done
