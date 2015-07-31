# Apache Usergrid Documentation

Usergrid documentation is written in [Markdown](https://help.github.com/articles/markdown-basics/) 
(*.md) and RST (.rst) formats and we use the Python based [Sphinx-Docs](http://sphinx-doc.org/) 
documentation system to transform our Markdown and RST files to HTML format.

## Prerequisites

Sphinx requires Python and pip. Once you have Python, you can install sphinx and pip like so:

	$ sudo easy_install sphinx
	$ sudo easy_install pip

## How to the Usergrid documentation using Sphinx

Sphinx runs via a Makefile in this, the docs directory. So, cd to this directory and make html:

	cd $usergrid/docs
	make clean html

## How to update documentations in Usergrid site

For most changes you simply edit the Markdown (.md) and reST (.rst) files where and 
commit your changes to Git.

### Note the difference between RST and Markdown files

Note that Sphinx's native format is [reStructuredText](http://docutils.sourceforge.net/rst.html) 
(RST) and not Markdown. This causes some issues. For example, using Sphinx directives in 
Markdown files sometimes works and sometimes does not. RST is a different format than Markdown 
and it has a different set of directives, some very useful for organizing a large set of 
documentation files.

### The Table of Contents

The Table of Contents for the documentation is in index.rst, which ties everything together
with a series of RST __toctree__ directives. All other files should be written in Markdown,
unless they need some special RST directives that will not work in Markdown.

### Other Tables

A note about tables: simple tables are easy and you can find examples (e.g. connecting-entities.md) 
but for tables with multiple lines in each cell, you'll probably want to use a raw HTML table,
and there are plenty of examples of that around too.

### The REST documentation 

TBD
