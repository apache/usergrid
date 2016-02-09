# How to Contribute Code & Docs

Contribute via GitHub
---

The Usergrid project uses GitHub as our code review system. If you want to contribute to Usergrid, then you should fork the apache/usergrid repo and submit PRs back to the project. 
Here are step-by-step guides for both both external contributors and committers:

* How to contribute code to Usergrid: [External Contributors Guide](https://cwiki.apache.org/confluence/display/usergrid/Usergrid+External+Contributors+Guide)

* How to accept code contributions to Usergrid: [Usergrid Committers Guide](https://cwiki.apache.org/confluence/display/usergrid/Usergrid+Committers+Guide)


Building Usergrid
---

Usergrid is made up of multiple components that are built separately:

* __Stack__: The Usergrid Stack is a Java web application, built using Maven. 
    * Build instructions are in the [README](https://github.com/apache/usergrid/blob/master/stack/README.md).

* __Portal__: The Usergrid Portal is an Angular.js application and builds with Grunt. 
    * Build instructions are in the [README](https://github.com/apache/usergrid/blob/master/portal/README.md). 

* SDKs: See the README files in the `/sdks` sub-directories for SDK build instructions.


Building the Website and Documentation
---

Usergrid documentation source is located in the `/docs` directory of our Git repo, written in [Markdown](https://daringfireball.net/projects/markdown/) format and managed by the [Sphinx](http://www.sphinx-doc.org) documentation system. For more information: 

* [Apache Usergrid Documentation build instructions](https://github.com/apache/usergrid/blob/master/docs/README.md)

The Usergrid website source is managed in the `/website` directory. We use the [Nanoc](http://nanoc.ws) static-site generator to generate website content from source. For more information:

* [Apache User Website build instructions](https://github.com/apache/usergrid/blob/master/website/README.md)
