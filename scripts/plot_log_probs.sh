#!/bin/bash

SCRIPT=`basename $0 | cut -d'.' -f1`.R

RUNS=1

SG=10000
SAMPLE=false

DATA=evening_news

for T in `echo 10 20 30 40 50`; do
  for PRIOR in `echo DP`; do
    for THETA in `echo 0.1 0.2 0.5 1.0 2.0 5.0 10.0`; do
      for EPS in `echo 0.0`; do
        cat scripts/${SCRIPT} | R --slave --vanilla --args results/cluster_lda/${DATA}/T${T}-C1-SG${SG}-SC1-ST1-THETA${THETA}-EPS${EPS}-SAMPLE${SAMPLE}-PERCLUSTERtrue-DOCCOUNTStrue-${PRIOR}-ID ${RUNS}
      done
    done
  done
done

DATA=house_press_1000

for T in `echo 50 100 150 200 250`; do
  for PRIOR in `echo DP`; do
    for THETA in `echo 0.1 0.2 0.5 1.0 2.0 5.0 10.0`; do
      for EPS in `echo 0.0`; do
        cat scripts/${SCRIPT} | R --slave --vanilla --args results/cluster_lda/${DATA}/T${T}-C1-SG${SG}-SC1-ST1-THETA${THETA}-EPS${EPS}-SAMPLE${SAMPLE}-PERCLUSTERtrue-DOCCOUNTStrue-${PRIOR}-ID ${RUNS}
      done
    done
  done
done
