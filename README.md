Apache Usergrid
===============

**Apache Usergrid is a multi-tenant Backend-as-a-Service stack for web & mobile applications, based on RESTful APIs. It is [currently incubating at the Apache Software Foundation](http://usergrid.incubator.apache.org/).**

This repository contains all the code for Apache Usergrid, including the server stack, portal, client and SDKs. Each of them have their own, much more detailed README in the corresponding subdirectories.

* The server-side stack, a Java 7 + Cassandra codebase that powers all of the features, is located under [`/stack`](stack). You can install dependencies and compile it with maven. See [stack/README.md](stack#requirements) for instructions.
* a command-line client “ugc” allowing you to complete most maintenance tasks, as well as queries in a manner similar to the mysql or the mongo shell, located under [`/ugc`](ugc). You can install it on your machine with a simple `sudo gem install ugc`
* the admin portal and the many SDKs. A pure HTML5+JavaScript app allowing you to register developers and let them manage their apps in a multi-tenant cluster. Located under [`/portal`](portal)
* SDKs for [iOS](sdks/ios), [Android](sdks/android), [HTML5/JavaScript](sdks/html5-javascript), [node.js](sdks/nodejs), [Ruby on Rails](ruby-on-rails), [pure Ruby](sdks/ruby), [PHP](sdks/php), (server-side) [Java](sdks/java) and [.Net / Windows](sdks/dotnet), located in their respective subdirectories under [`/sdks`](sdks).


Backwards compatibility
-----------------------

If you previously developed with Apache Usergrid, you know our code used to be structured into _separate_ repositories: usergrid-stack, usergrid-portal, etc. We are now using a _merged_ repository.

### How to update your code setup & pull changes from your old forks

You just need to clone this repository, and use [git subtree (usually requires git 1.8+)](http://engineeredweb.com/blog/how-to-install-git-subtree/) to merge your changes under the new structure. Here’s an example for a portal fork. Adjust the prefix, repository address and branch you want to pull from as necessary.

    git clone git@github.com:usergrid/usergrid.git
    cd usergrid
    git subtree pull --prefix=portal git@github.com:my-github-account/my-usergrid-portal-fork.git master

This will pull (i.e. merge) the changes you made from the master branch of github.com/my-github-account/my-usergrid-portal-fork into the portal/ subfolder. It should ask you to provide a commit message for the merge. There’s lot of flexibility on how to fetch and merge, [please see the git subtree manual](https://github.com/git/git/blob/master/contrib/subtree/git-subtree.txt) for details. Then please do consider sending us a pull request with these changes ;)


### How to pull commits made on this repo into your old forks

You should really update your old repositories to the new structure with the instructions above, but the following may work for you, although we make no guarantee they will work in the future.

You can produce a branch compatible with the old repos by using [git subtree (usually requires git 1.8+)](http://engineeredweb.com/blog/how-to-install-git-subtree/). Then from this repository you can

    git clone git@github.com:usergrid/usergrid.git
    cd usergrid
    git checkout master
    git subtree split --prefix=portal -b portal
    git checkout portal

This will create a “portal” branch that is compatible with the old usergrid-portal repository, from the code under the portal/ directory. You can pull from that branch (or push changes to it, although we will not accept pull requests sent thusly).

The `git subtree split` above should function for portal, ugc, and any of the SDKs, but will not work for the stack, due to some anonymous comments left in the tree that prevent a split, and cannot be corrected lest we break the history and force a rebase on all forks.

**Please update your code setup as soon as possible and [ask the dev list](https://mail-archives.apache.org/mod_mbox/incubator-usergrid-dev/) if you have any questions!**
