# Apache Usergrid Documentation

Usergrid documentation is written in [Markdown](https://help.github.com/articles/markdown-basics/) (*.md) and RST (.rst) formats and we use the Python based [Sphinx-Docs](http://sphinx-doc.org/) documentation system to transform our Markdown and RST files to HTML format.

## Prerequisites

Sphinx requires Python and pip. Once you have Python, you can install sphinx and pip like so:

	$ sudo easy_install sphinx
	$ sudo easy_install pip

## How to build the Usergrid documentation using Sphinx

Sphinx runs via a Makefile in this, the docs directory. So, cd to this directory and make html:

	cd $usergrid/docs
	make clean html

## How to update the Usergrid docs 

Find the Markdown (.md) or reST (.rst) files that you need to change, edit them and then run ``make clean html`` to generate the HTML into the ``target/html`` directory, where you can preview your changes. 

### Note the difference between RST and Markdown files

Note that Sphinx's native format is [reStructuredText](http://docutils.sourceforge.net/rst.html) (RST) and not Markdown. This causes some issues. For example, using Sphinx directives in Markdown files sometimes works and sometimes does not. RST is a different format than Markdown and it has a different set of directives, some very useful for organizing a large set of documentation files.

### Updating the Table of Contents

The Table of Contents for the documentation is in index.rst, which ties everything together
with a series of RST __toctree__ directives. All other files should be written in Markdown,
unless they need some special RST directives that will not work in Markdown.

### Dealing with other tables

A note about tables. Markdown and reST do not have very good table support. Simple tables are easy to do and you can find examples (e.g. connecting-entities.md) but for tables with multiple lines in each cell, you'll probably want to use a raw HTML table, and there are plenty of examples of that around too.

## How to publish the Usergrid docs to Usergrid website

First you generate the HTML for the docs from the Markdown and reST sources like so:

	cd $usergrid/docs
	make clean html
	
To update the Usergrid website you must copy the udpated docs files from ``target/html`` directory to the website directory at the root of the Usergrid project, i.e. ``${usergrid-project-dir}/website/docs``.

You can do this by running the script ``update-website.sh``.

Once you've done that then you should follow the instructions in the website/README.md file, which explains how to update the website.

## Updating the REST API reference

The REST API documentation in ``rest-endpoints/api-docs.md`` is generated from a Swagger file in the 
directory ``src/main/resources``, so DO NOT edit that file directly.

If you need to update the REST API docs, you should edit the usergrid-swagger.yaml file and then
 re-generate the file. 

If you need to change the formatting of the REST API docs, then you will need to edit the Mustache templates in ``src/main/resource`` and you may need to edit the Groovy script that does the generation: ``src/main/groovy/usergrid.ApiDocGenerator``.
 
You will need:
* Groovy 2.x
* [Mustache.java](https://github.com/spullara/mustache.java) 

__NOTE__: Mustache.hava is not in Maven Central so unfortunately, you will have to Git Clone Mustache.java before you can run the generation script.

This is the command to run the generation:

	groovy src/main/groovy/usergrid/ApiDocGenerator.groovy
	
The script will update the file ``rest-endpoints/api-docs.md`` and when you are happy with your update you should commit and push that file with Git.
