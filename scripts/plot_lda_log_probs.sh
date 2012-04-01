#!/bin/bash

SCRIPT=`basename $0 | cut -d'.' -f1`.R

RUNS=5

S=1000
SAMPLE=11

DATA=fenno_lit

for T in `echo 2 5 10 25 50`; do
  cat scripts/${SCRIPT} | R --slave --vanilla --args results/lda/${DATA}/T${T}-S${S}-SAMPLE${SAMPLE}-ID ${RUNS}
done

DATA=house_press_1000

for T in `echo 25 50 75 100 150`; do
  cat scripts/${SCRIPT} | R --slave --vanilla --args results/lda/${DATA}/T${T}-S${S}-SAMPLE${SAMPLE}-ID ${RUNS}
done
