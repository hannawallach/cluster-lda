# Makefile for LaTeX files
# (c) 2002 Hanna Wallach

.PHONY: all pdf clean

all: pdf

pdf: 65_paper.pdf 65_supplementary.pdf

%.pdf: %.tex
	pdflatex $<; bibtex $(*F); pdflatex $<

clean:
	rm -f *.aux *.dvi *.log *.toc *.blg *.bbl *~
