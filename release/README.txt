
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

NOTE: if you are releasing from a branch other than 'release' then make sure
the two bash scripts in this directory define RELEASE_BRANCH correctly.

To create a release candidate:

1) Change to the release branch (usually this is master). 
   Set .usergridversion to release version number.

2) Make sure that all JIRA issues that you want in the CHANGELOG have fixVersion
   that matches the release version number in .usergridversion

3) Ensure that everything is committed, branch must be clean

4) From the project root directory run release/release-candidate.sh --help
   to see how to use the script. Then run it with the correct options.

   For example, to create release candidate 4 of a 1.0.1 release you would use:

   $ release/release-candidate.sh -r 4 -l p


To create a release:

1) Change to the release branch. Set .usergridversion to release version number.

2) Make sure that all JIRA issues that you want in the CHANGELOG have fixVersion
   that matches the release version number in .usergridversion

3) Ensure that everything is committed, branch must be clean

4) From the project root directory run release/release.sh --help
   to see how to use the script. Then run it with the correct options.

