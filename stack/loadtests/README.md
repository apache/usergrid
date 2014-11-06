#Running
Gatling will run through mvn

execute mvn clean install

execute mvn gatling:execute with these options

*Required
> -Dthrottle={maxReqsSec} -Dduration={durationInSecs} -Dorg={org}  -Dapp={appName} -Dbaseurl={uriNoProceedingSlash} -DnumEntities={numberOfEntitiesYouWantToCreateInteger} -DnumUsers={totalUsersInteger} -DrampTime={rampTimeIntegerSeconds} -DadminUser={username} -DadminPassword={pwd}

*Addional optional settings 
	
>-DpushNotifier={notifierName} -DpushProvider=noop

So running will look something like this
>mvn gatling:execute -Dthrottle={maxReqsSec} -Dduration={durationInSecs} -Dorg={org}  -Dapp={appName} -Dbaseurl={uriNoProceedingSlash} -DnumEntities={numberOfEntitiesYouWantToCreateInteger} -DnumUsers={totalUsersInteger} -DrampTime={rampTimeIntegerSeconds} -DadminUser={username} -DadminPassword={pwd}

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

