
This directory contains scripts for generating the files and emails needed to 
create a Usegrid release and a release-candidate, and for publishing the 
release to the proper location for release and mirroring. The scripts are 
based on scripts from the Apache Aurora project.

These are the important files:

  /.usergridversion       - version number to be used for release
  /.gitattributes         - lists file patterns that are to be exlcuded
  /release/release-candidate.sh - create a release candidate
  /release/release.sh     - create a release
  /release/changelog.rb   - generate CHANGELOG based on JIRA fixVerison 

To create a release candidate:

1) Change to the master branch. Set .usergridversion to release version number.

2) Make sure that all JIRA issues that you want in the CHANGELOG have fixVersion
   that matches the release version number in .usergridversion

3) Ensure that everything is committed, branch must be clean

4) From the project root directory run release/release-candidate.sh


To create a release:

1) Change to the release branch. Set .usergridversion to release version number.

2) Make sure that all JIRA issues that you want in the CHANGELOG have fixVersion
   that matches the release version number in .usergridversion

3) Ensure that everything is committed, branch must be clean

4) From the project root directory run release/release.sh

