#/bin/bash

SCRIPT=`basename $0 | cut -d'.' -f1`.R

RUNS=5

for PRIOR in `echo UP DP`; do
  for THETA in `echo 0.25 0.5 1.0 2.0 5.0 10.0 15.0 20.0`; do
    cat scripts/${SCRIPT} | R --slave --vanilla --args results/cluster_lda/core/C25-SG2000-SC1-THETA${THETA}-SAMPLEfalse-DOCCOUNTStrue-${PRIOR}-ID ${RUNS}
  done
done
