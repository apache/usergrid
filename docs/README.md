# Apache Usergrid Documentation

Uses the Sphinx documentation system with Markdown content.

## Prerequisites

You will need Python, Sphinx and PIP.

	$ sudo easy_install sphinx
	$ sudo easy_install pip

## How to gererate docs

	cd $usergrid/docs
	make clean html

## How to update documentations in Usergrid site

For most changes you simply edit the Markdown (.md) and reST (.rst) files where and commit your changes to Git.

Note that Sphinx's native format is reST and not Markdown. This causes some issues. For example, using Sphinx directives in Markdown files sometimes works and sometimes does not. 

To update the default-entities and rest-endpoint tables, you should edit the tab-delimited text files (.text) in the source directory, and the run the Groovy scripts there to generate the markdown file representations of those tables.