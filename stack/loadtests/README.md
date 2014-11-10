#Running
Gatling will run through mvn

1. navigate to loadtests under stack

1. execute mvn gatling:execute with these options

	*Required
	> -Dthrottle={maxReqsSec} -Dduration={durationInSecs} -Dorg={org}  -Dapp={appName} -Dbaseurl={uriNoProceedingSlash} -DnumEntities={numberOfEntitiesYouWantToCreateInteger} -DmaxPossibleUsers={totalUsersInteger} -DrampTime={rampTimeIntegerSeconds} -DadminUser={username} -DadminPassword={pwd} -Dgatling.simulationClass={simulationFQDN}

	*Addional optional settings 
		
	>-DpushNotifier={notifierName} -DpushProvider={noop_apns_or_gcm} -DskipSetup={boolean}

	skipSetup will skip the setup steps

	So running will look something like this
	>mvn gatling:execute -Dthrottle=100 -Dduration=300 -Dorg=usergrid  -Dapp=load -Dbaseurl=http://load.usergrid.com -DnumEntities=300 -DmaxPossibleUsers=600 -DrampTime=30 -DadminUser=usergrid -DadminPassword=test -Dgatling.simulationClass=org.apache.usergrid.simulations.AppSimulation
	
	Setting the users and duration => Injects a given number of users with a linear ramp over a given duration. users must be greater than duration
	
	Setting the throttle and ramptime => will attempt to hit a set reqs/sec over a given time period.  If users and duration are not great enough then you will never hit your throttle
	
	Values for simulation are 'all','connections'
	
	Also see http://gatling.io/docs/2.0.2/general/simulation_setup.html
	
	Additional docs can be found here http://gatling.io/docs/2.0.2/
