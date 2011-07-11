#/bin/bash

RUNS=5

make build

for PRIOR in `echo UP DP`; do
  for THETA in `echo 0.25 0.5 1.0 2.0 5.0 10.0 15.0 20.0`; do
    mkdir -p results/sge; qsub -t 1-${RUNS} -cwd -V -o results/sge/stdout_${THETA}_${PRIOR}.txt -e results/sge/stderr_${THETA}_${PRIOR}.txt ./scripts/run_cluster.sh ${THETA} ${PRIOR}
  done
done
