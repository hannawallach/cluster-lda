#!/bin/bash

SCRIPT=`basename $0 | cut -d'.' -f1`.R

RUNS=5

S=1000
SAMPLE=11

DATA=evening_news

for T in `echo 10 20 30 40 50`; do
  cat scripts/${SCRIPT} | R --slave --vanilla --args results/lda/${DATA}/T${T}-S${S}-SAMPLE${SAMPLE}-ID ${RUNS}
done

DATA=house_press_1000

for T in `echo 50 100 150 200 250`; do
  cat scripts/${SCRIPT} | R --slave --vanilla --args results/lda/${DATA}/T${T}-S${S}-SAMPLE${SAMPLE}-ID ${RUNS}
done
