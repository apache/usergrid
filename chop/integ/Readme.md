# What's this for?

Testing in the EC2 environment gets to be tedious after a while. We should not
have to deploy and test basic runner coordinator interactions manually in the
EC2 environment which takes time and is error prone.

We want to make sure that some interaction tests are always tested beforehand
and guarranteed to function properly between runners and the coordinator in 
the webapp project.

To do this properly we need to use the runner jar produced from the example
project, and have it run along side the executable jar from the webapp project
that contains the coordinator. Doing this in the existing project modules 
causes a headache because the executable jars must be generated then used
across at least two separate projects. This also creates cyclic module 
dependencies which maven will complain about.

This is the reason we chose to do these tests in a separate module that can
start multiple runners produced from the example project with its chop tests,
and the webapp project which contains the coordinator.

Optionally if the right properties are enabled, this module will attempt to
actually create a stack on EC2 to run the application. The example project 
does not need this however this functionality can be tested if a valid 
Amazon account is active, if not then the test will not bomb out, but will
pass. If the Amazon account and credentials are available then an example 
stack will be created and tested for correct creation and destruction.
