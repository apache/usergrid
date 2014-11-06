#Running
Gatling will run through mvn

1. navigate to loadtests under stack

1. execute mvn gatling:execute with these options

	*Required
	> -Dthrottle={maxReqsSec} -Dduration={durationInSecs} -Dorg={org}  -Dapp={appName} -Dbaseurl={uriNoProceedingSlash} -DnumEntities={numberOfEntitiesYouWantToCreateInteger} -DnumUsers={totalUsersInteger} -DrampTime={rampTimeIntegerSeconds} -DadminUser={username} -DadminPassword={pwd} 

	*Addional optional settings 
		
	>-DpushNotifier={notifierName} -DpushProvider={noop_apns_or_gcm} -Dsc={simulationFQDN}

	So running will look something like this
	>mvn gatling:execute -Dthrottle=100 -Dduration=300 -Dorg=usergrid  -Dapp=load -Dbaseurl=http://load.usergrid.com -DnumEntities=300 -DnumUsers=600 -DrampTime=30 -DadminUser=usergrid -DadminPassword=test -Dsc=org.apache.usergrid.simulations.AppSimulation
	
	Setting the rampTime => Injects a given number of users with a linear ramp over a given duration.

	Values for simulation are 'all','connections'
	
	Also see http://gatling.io/docs/2.0.2/general/simulation_setup.html

##Additional

The simulation to run is configured in the pom.xml

	<plugin>
		<groupId>io.gatling</groupId>
		<artifactId>gatling-maven-plugin</artifactId>
       <configuration>
          <simulationsFolder>src/main/scala</simulationsFolder>
          <simulationClass>org.apache.usergrid.simulations.AppSimulation</simulationClass>
          <propagateSystemProperties>true</propagateSystemProperties>
       </configuration>
	</plugin>


Additional docs can be found here http://gatling.io/docs/2.0.2/