![Judo Chop](http://stash.safehaus.org/projects/CHOP/repos/main/browse/judo-chop.jpeg?at=321c1def8f09eb0f0d488a3ea2d874d1b2ef7c94&raw)

# TODO

* Very old and needs to be updated for 2.0 

# What is it?

Judo Chop is a simple distributed performance testing framework designed to
pound the heck out of anything you want to give a REALLY BAD DAY! Performance
testing has never been easier. Just annotate your JUnit tests with TimeChop or 
IterationChop annotations telling Judo Chop how to chop it up. Judo Chop 
uses your own project's JUnit Test Cases as drivers to bombard your application, 
service, or server.

# How does it work?  

Judo Chop has two kinds of annotations you apply to JUnit Test classes. Each 
annotation value and the annotations applying to tests are listed below:
 
* time (TimeChop) - time in milliseconds to run the test
* iterations (IterationChop) - iterations of the test to run per thread
* threads (Both) - the number of threads to use per controller
* delay (Both) - the number of milliseconds to delay between test runs
* saturate (Both) - first automatically run some preliminary tests to find the 
saturation point where increases in the number of runners or the number of threads 
no longer results in any throughput gains, then run the test

It's probably already pretty clear how this Chop thingy works. The Chop annotations tell 
Judo Chop how to run your JUnit or Jukito tests. Of course it's up to you to
make sure your chop tests actually pound on something else rather than running
locally. 

At this point you might be thinking, "But dood my tests start up their own local instance
of Cassandra, and run against that, how those tests run against a real cluster?" For this
specific reason, Judo Chop uses a sweet dynamically reconfigurable Guice based library
called GuicyFig. Besides giving you a dope type safe and interface driven access method
to configuration properties, it allows your application to be environment aware. GuicyFig
contains an EnvironResource (short for Environment) which allows you to easily port 
your JUnit ExternalResources (a la JUnit Rules) to an EnvironResource. So basically on
your local machine (UNIT environment) your tests will fire up a local instance of 
Cassandra, however in the CHOP environment, your tests will switch over to using a 
Cassandra cluster. You can apply this to Mongo or to any other kind of external 
resource.     

Judo Chop's Maven Plugin takes your annotated tests and builds a runner war
out of it. The runner.war is deployed by the plugin to several machine instances: the
runner cluster. The plugin contains goals to use a trivial REST API on the runner
application to trigger the synchronous bombardment of your application via your own
Chopped tests. You could even use simple curl statements to issue start, stop and
reset commands against the REST API of the runners from within your own tests. A simple
Java client API let's you dynamically grow your cluster to change load characteristics 
during in flight tests, and collect statistics while doing so. Reports are generated and 
placed in a store where they can be later analyzed.

Judo Chop is designed to work well with Jenkins and Sonar. Each time you change your 
source code and commit to your VCS of choice, Judo Chop with a little help from Jenkins
can deploy your new source, associating it back to the Maven project and VCS commitId
uniquely identifying the code under test. That way you have a history of performance 
metrics collected for the life of your code base directly associated with the versioned
sources in your code repository.

## Future Enhancements

* Inject yammer metrics and send the results to Graphite
* Build the final version of the results visualization console - a quick and dirty 
  console already exists
* Support more VCS', more stores, and more virtual environments 
* Dynamic reconfiguration to tweak configuration parameters while using the same sources
  for different runs and recording these differences to correlate them with performance
  metrics
* Complete the in flight test load modification feature to enable the saturate capability
  which fires up tests in a chop and increases parameters of the chop to find out the 
  point at which the throughput and performance of your target ceases to improve.

# How do I use it?

The best way is to look at an 
[example](http://stash.safehaus.org/projects/CHOP/repos/main/browse/example/pom.xml) 
project where it has been used. More info on how to use the plugin is also available 
[here](http://stash.safehaus.org/projects/CHOP/repos/main/browse/plugin).

-------

## Yer Maven Configuration

First add the Judo Chop maven plugin to your project like so:

~~~~~~

      <plugin>
        <groupId>org.apache.usergrid.chop</groupId>
        <artifactId>chop-maven-plugin</artifactId>
        <version>1.0-SNAPSHOT</version>
        <configuration>
          <accessKey>${aws.s3.key}</accessKey>
          <secretKey>${aws.s3.secret}</secretKey>
          <bucketName>${aws.s3.bucket}</bucketName>
          <managerAppUsername>admin</managerAppUsername>
          <managerAppPassword>${manager.app.password}</managerAppPassword>
          <testPackageBase>org.apache.usergrid.chop.example</testPackageBase>
          <runnerSSHKeyFile>${controller.ssh.key.file}</runnerSSHKeyFile>
          <failIfCommitNecessary>false</failIfCommitNecessary>
          <amiID>${ami.id}</amiID>
          <awsSecurityGroup>${security.group}</awsSecurityGroup>
          <runnerKeyPairName>${controller.keypair.name}</runnerKeyPairName>
          <runnerCount>12</runnerCount>
          <securityGroupExceptions>
            <param>21.14.31.218/32</param>
          </securityGroupExceptions>
        </configuration>
      </plugin>

~~~~~~

Give yourself a chop on the back if you guessed that Judo Chop works in the Amazon EC2 
environment. Eventually we would like to make sure it works independent of any specific
environment however we had to start somewhere. Everything here is pretty self explanatory 
and if it is not then ping us about it on judo-chop AT safehaus.org. 

Please note that it's much better for you to use properties and variable substitution 
for these values. In your settings.xml you can create profiles that are active by default.
Then in your pom.xml file use variable substitution rather than using static values. For
example if you're working with a big team, or working on an OS project you don't want
values to conflict or for the public to see your AWS account credentials. For these 
reasons, and many many more use your settings.xml for personal and machine specific
parameters with substitution in the pom.xml, while allowing for static project specific 
parameters in the pom.xml. Just think, person or machine specific private stuff goes in
your settings.xml, public shared project information goes into the POM. 

By the way, you'll also need to make Maven generate an <artifact>-tests.jar of your 
test classes. Just add the following Maven Jar plugin configuration into the project 
module that you'd like to chop up like a 
[Honey Badger](http://www.youtube.com/watch?v=4r7wHMg5Yjg):

~~~~~~

      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-jar-plugin</artifactId>
        <version>2.4</version>
        <executions>
          <execution>
            <goals>
              <goal>test-jar</goal>
            </goals>
          </execution>
        </executions>
      </plugin>

~~~~~~


## What does the plugin do with my software?

When running the *chop:deploy* Maven Goal, Judo Chop's Maven Plugin will take your tests 
and bundle them into a single runner war file and push it up to the S3 bucket specified 
in the plugin's configuration. You can specify any bucket name, and the plugin will 
create it for you with a standard layout to your project, it's tests and runs on those 
tests.

~~~~~~
**NOTE**: _See the section below on security._ You **MUST** run a *chop:cert* goal before 
other goals to initialize the certificate trust store locally for your project. Any other
goals run before this one time execution will fail. Because of the way Maven and the
SSL Socket implementation in Java works you cannot chain this goal either, it must be 
run alone by itself.
~~~~~~

Judo Chop concepts are simple and borrowed by things already in existence. We're not 
creating new concepts like confusing hipsters, although the name is pretty damn cool.
You have projects tracked by their VCS repository URL plus commitId, and the Maven 
Project settings: i.e. version, groupId, and artifactId. These parameters uniquely 
identify the project and the code under test.

Each project may have several tests corresponding one-to-one with a commitId, a runner.war
and a project.properties file. These are loaded into S3 by the *chop:deploy* goal. Each 
test may have N number of runs where the project's properties or environment have been 
changed but the commitId still holds. A new commitId requires the upload of a new 
runner.war and new a new project properties file. The idea here is that you deploy your 
code, and use dynamic property reconfiguration to tweak the environment and properties 
across different runs to see how those properties impact overall performance.

There are actually two goals that handle this process of generating, verifying and 
deploying the produced runner.war files. MD5 checksums are used to make sure the right
runner is associated with the right commit version of the code. When you execute the
*chop:deploy* goal, it actually checks to make sure the runner.war is up to date if 
present and it's MD5 has not changed for the commitId, the war is not rebuilt or deployed.
If not present or not up to date the plugin generates a new runner.war automatically by 
chaining the *chop:war* goal. You can run the *chop:war* goal then follow up with the 
*chop:deploy* goal separately as you like. All Judo Chop Maven Plugin goals are 
idempotent and chained to make sure the right sequence is used except for the *chop:cert*
goal. There's more information about this goal in the next section.


## How are runners (load injector instances) created and used?

The Maven Plugin uses the Amazon EC2 API to create instances using a public AMI. The 
public AMI called _Judo Chop 1.0 Runner_ (ami-id ami-c56152ac). This AMI goes hand in
hand with the version of Judo Chop being used. Don't worry as Judo Chop versions are 
released new AMI's will be produced for them. The AMI has a stock version of the runner
setup to be loaded by the plugin with your project's runner. The plugin allows you to
override this AMI in your configuration settings so if you want to make changes to the
runners and produce an AMI to use instead of the stock AMI that comes with Judo Chop,
then you are free to do so.

The Judo Chop 1.0 Runner AMI uses the latest Ubuntu 13.04 release with minor tweaks to 
ulimits and kernel socket parameters. The standard Open JDK 1.7.0_x is used with the 
stock Tomcat 7 that can be installed using apt. Because there are restrictions on the
distribution of Oracle JDKs we did not use one for the stock AMI. If you like swap out
the JDK, save your AMI and override the default AMI in your project's pom.xml in the
Judo Chop Maven Plugin configuration section.

Tomcat is used as the Servlet Container. Changing this is not so easy because the 
Admin Manager Application is used to deploy new versions of your code. Even though we
restart Tomcat we started this way and may remove the dependency later to enable the
use of just about any container in a pluggable fashion.

Tomcat is also setup with a self signed certificate to use HTTPS enabled by default. The
certificate's public key is packaged into Judo Chop to create a certificate trust store
so HTTPS and SSL can be used securely with the runners it creates. Because tests and 
runners are temporal, you create them using *chop:setup* and destroy them using 
*chop:destroy*, there's very little threat for little gain. Within a few hours the 
machines that were created are destroyed. 

However if you're paranoid that:

1. A hacker will waste the time to extract the private key from the AMI, 
2. Then use the private key to snoop on the 1-2 minutes of setup traffic transmitting 
   passwords, in the hope to
3. Gain access and capture your runner instances for the 2-4 hours your tests run,

Then we have a solution for you. Replace the key on the AMI and produce a new AMI and 
drop the new key into your project and configure it. Then maybe you can sleep at night :D.

So because thanks to the paranoid security freaks out there we added this extra security
to Judo Chop from the start. However thanks to these guys :P, 99.23% of you who don't 
give a shit now have to execute an additional *chop:cert* goal the first time you use
Judo Chop on a new project artifact. This command creates the X.509 certificate trust 
store needed by the JDK SSL socket implementation to validate your runner identities. Ya,
all this for securing down runners you'll destroy in a few hours. 

So after running a *chop:cert* command you can run any of the other commands. If you 
change the cert and use your own AMI, you'll have to run this goal again to add the new
cert to your trust store: jssecacert file in the top level of your Maven Module under 
test. You might want to add that to .gitignores .. hint .. hint! 

Once the trust store is setup you can then run *chop:setup* which uses the Amazon EC2
API to setup the number of instances of runners using your or the stock AMI to bring
them up. A security group is created using the name you specified along with exceptions
to allow for example your IP address to be able to SSH into the runners and to actually
use the REST API of the runners. So technically we're also adding to security here as 
well so changing the cert is 99.9999% unnecessary IOHO. Note all the goodies for 
configuration are available like the region, the zone to deploy to, and the instance 
type to use. 

The *chop:setup* goal will create the security group, create the number of needed
instances, adding more if necessary and removing some if there are too many. It will then
log into the runners and change the Tomcat Admin Manager password and restart the running 
Tomcat instances. This all happens in 2-4 minutes from clusters as small as 3 to as large
as 36. Almost all the commands execute in parallel to speed up the process so you're 
not waiting 45 minutes for the test cluster to come up.

Once the runners are up you can issue a *chop:load* goal which issues a REST /load 
command in parallel to all runners with outdated MD5 signatures on their current setup. 
The runners use this signal to pull down the right runner.war and project.properties 
from S3 so you don't have to upload the runner to each machine. If your war locally is
not present it will be generated automatically and all the needed MD5 checks will be
performed to determine if the new runner.war needs to be uploaded to S3 again. If that
is needed then the *chop:load* goal will automatically invoke the *chop:war* and 
*chop:deploy* goals. If the runner instances are too many or too few, for the runnerName 
used in the plugin configuration, then some will be destroyed or some will be created
respectively using the specified AMI.

Once you've loaded your project materials you can actually start a test run using the 
*chop:start* goal. Note you could have just issued the *chop:start* after a *chop:cert*
and the *chop:start* goal will automatically chain and execute the other goals 
(*chop:war*, *chop:deploy*, *chop:setup* and *chop:load*) as needed on demand.

The other remaining operations are:

* *chop:destroy* destroys the cluster of runners (S3 is not touched) 
* *chop:stop* stops an test run putting runners into STOPPED (needs reset) state
* *chop:reset* puts runners into the READY state
* *chop:verify* verifies that all runners are READY to run and loaded w/ same MD5
* *chop:results* pulls down test results and summary info from S3

The following REST endpoints are used to control runners:

 * POST /start
 * POST /stop 
 * POST /reset
 * GET  /stats
 * GET  /status
 * POST /load 

The following ascii text shows the state transition diagram of a runner which one can 
go through while issuing REST operations to the end points above:

~~~~~~~

            start           stop
    +-----+       +-------+      +-------+
--->+ready+------>+running+----->+stopped|
    +--+--+       +-------+      +---+---+
       ^                             |
       |_____________________________v
                    reset

~~~~~~~

# Project Resources

* Mailing List: [ALL Traffic](mailto:judo-chop@safehaus.org)
* Version Control Repository: [Git via Stash](http://stash.safehaus.org/projects/CHOP/repos/main/browse)
* Wiki and Home Page: [Confluence Wiki](http://confluence.safehaus.org/display/CHOP/Home)
* Issue Tracker: [JIRA Issues](http://jira.safehaus.org/browse/CHOP)
* Jenkins CI: [Jenkins Job](http://jenkins.safehaus.org/job/JudoChop/)
* Sonar Quality: [Sonar Project](http://sonar.safehaus.org/dashboard/index/1082)

# Special Thanks

* Apache Usergrid Peeps - Todd, Dave, Rod
* Safehaus Peeps - Jimmy Rybacki (plugin), Askhat Asanaliev (web-ui), Yigit Sapli (infrastructure)
* Atlassian
* Jetbrains

Happy Chopping!
The Judo Chop Team

