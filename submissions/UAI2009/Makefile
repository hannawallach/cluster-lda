# Makefile for LaTeX files
# (c) 2002 Hanna Wallach

FILENAME = paper

.PHONY: all pdf figures word-count clean

all: pdf

pdf: $(FILENAME).pdf

$(FILENAME).pdf: *.tex *.bib figures
	echo "Running latex...."
	pdflatex -halt-on-error $(FILENAME).tex
	echo "Running bibtex...."
	bibtex $(FILENAME)
	echo "Rerunning latex...."
	pdflatex -halt-on-error $(FILENAME).tex
	pdflatex -halt-on-error $(FILENAME).tex

figures:
	$(MAKE) -C figures

word-count:
	untex -a -e -m *.tex | wc -w

clean:
	rm -f *.aux *.dvi *.log *.toc *.blg *.bbl $(FILENAME).pdf *~
	$(MAKE) clean -C figures
