#!/bin/bash

RUNS=5

make build

DATA=patents_core
T=100

STATE_FILE=results/lda/${DATA}/T${T}-S1000-SAMPLE11-ID1/state.txt.gz.1000

for PRIOR in `echo UP PYP DP`; do
  for THETA in `echo 0.25 0.5 1.0 2.0 5.0 10.0 15.0 20.0`; do
    for EPS in `echo 0.0`; do
      mkdir -p results/sge; qsub -t 1-${RUNS} -cwd -V -o results/sge/stdout_${THETA}_${EPS}_${PRIOR}_${DATA}_${T}.txt -e results/sge/stderr_${THETA}_${EPS}_${PRIOR}_${DATA}_${T}.txt ./scripts/run_cluster.sh ${THETA} ${EPS} ${PRIOR} ${DATA} ${T} ${STATE_FILE}
    done
  done
done
