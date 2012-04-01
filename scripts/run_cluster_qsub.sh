#!/bin/bash

RUNS=1

make build

DATA=house_press_1000

for T in `echo 25 50 75 100 150`; do

  STATE_FILE=results/lda/${DATA}/T${T}-S1000-SAMPLE11-ID1/state.txt.gz.1000

  for PRIOR in `echo DP`; do
    for THETA in `echo 0.01 0.1 0.2 0.5 1.0 2.0 5.0 10.0`; do
      for EPS in `echo 0.0`; do
        mkdir -p results/sge; qsub -l long -t 1-${RUNS} -cwd -V -o results/sge/stdout_${THETA}_${EPS}_${PRIOR}_${DATA}_${T}.txt -e results/sge/stderr_${THETA}_${EPS}_${PRIOR}_${DATA}_${T}.txt ./scripts/run_cluster.sh ${THETA} ${EPS} ${PRIOR} ${DATA} ${T} ${STATE_FILE}
      done
    done
  done
done
