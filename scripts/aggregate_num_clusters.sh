#/bin/bash

RUNS=5

for PRIOR in `echo UP DP`; do
  for THETA in `echo 0.25 0.5 1.0 2.0 5.0 10.0 15.0 20.0`; do

    # form the file name

    FILE_NAME=results/cluster/core/C25-SG2000-SC1-THETA${THETA}-SAMPLEfalse-DOCCOUNTStrue-${PRIOR}-ID1-${RUNS}_av_num_clusters.txt

    while read LP; do
      echo ${PRIOR} ${THETA} ${LP}
    done < ${FILE_NAME}
  done
done
