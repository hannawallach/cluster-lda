# Makefile for MetaPost files
# (c) 2007 Hanna Wallach

all: pdf

pdf:
	for file in `ls *.mp`; do \
	  $(MAKE) `basename $$file .mp`-1.pdf; \
	done

%-1.pdf: %.mp
	echo "Running MPtoPDF...."
	mptopdf $*.mp

clean:
	rm -f mptrace.tmp *.1 *.log *.mpo *.mpx tmpgraph.* *~ #*-1.pdf
