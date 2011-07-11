#/bin/bash

SCRIPT=`basename $0 | cut -d'.' -f1`.R

RUNS=5

for PRIOR in `echo UP DP`; do
  for THETA in `echo 0.25 0.5 1.0 2.0 5.0 10.0 15.0 20.0`; do
    for ID in `seq 1 ${RUNS}`; do
      echo -n "${PRIOR} ${THETA} ${ID} "
      cat results/cluster/core/C25-SG2000-SC1-THETA${THETA}-SAMPLEfalse-DOCCOUNTStrue-${PRIOR}-ID${ID}/permutation_stats.txt
    done
  done
done