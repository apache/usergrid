Apache Usergrid Website
=======================

Check out the latest code with

    $ svn --username "APACHE_USERNAME" --password "APACHE_PASSWORD" co https://svn.apache.org/repos/asf/incubator/usergrid/site/

All pages are generated from the content (html,js,css) under `content/`
All doc sources are in markdown files under `content/docs/`

To generate the site locally, you need [pandoc](http://johnmacfarlane.net/pandoc/installing.html), ruby and python installed.

You will need pandoc

    http://johnmacfarlane.net/pandoc/installing.html

You will need pygments

    $ sudo easy_install Pygments

You will also need a few rubygems

    $ sudo gem install nanoc pygments.rb htmlentities pandoc-ruby nokogiri rack mime-types

To test locally, you can use the autocompiler (will build changes on every request) and check the website at [http://0.0.0.0:3000](http://0.0.0.0:3000)

    $ nanoc autocompile

To build for export use the following comments. The static website will be in `publish/`

    $ nanoc compile

To re-deploy the site, make sure you commit both your changes under `content/` AND have built the site with `nanoc compile`, which should have created some changes under `publish/`. If you do not commit the files changed under `publish/` the production website will not change. Commit all changes by doing

If you added any files:
	$ svn add <file or directory>

Then commit:
    $ svn ci -m "Some message"

To pull down the latest:
	$ svn update
