#/bin/bash

# Usage: ./scripts/make_log_probs.sh

RUNS=5

for PRIOR in `echo UP DP`; do
  for THETA in `echo 0.25 0.5 1.0 2.0 5.0 10.0 15.0 20.0`; do
    for ID in `seq 1 ${RUNS}`; do
      make results/cluster/core/C25-SG2000-SC1-THETA${THETA}-SAMPLEfalse-DOCCOUNTStrue-${PRIOR}-ID${ID}/log_prob.txt C=25 SG=2000 SC=1 THETA=${THETA} SAMPLE=false DOCCOUNTS=true PRIOR=${PRIOR} ID=${ID}
    done
  done
done