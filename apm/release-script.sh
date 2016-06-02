#!/bin/sh

#needed versions
CURRENT_DEV_VERSION=3.0.0-SNAPSHOT
RELEASE_VERSION=3.0.11-max
FUTURE_DEV_VERSION=3.0.0-SNAPSHOT

#clean out current distribution directory
rm -rf 'dist'

echo "updating all pom versions with release versions - now it should be done by mvn release plugin"

for pom in `find . -name pom.xml`; do 
	 	cp -fp $pom $pom".old";
		sed  -e "s/<version>$CURRENT_DEV_VERSION<\/version>/<version>$RELEASE_VERSION<\/version>/g" $pom".old" > $pom 
		if [ $? -ne 0 ]; then
			exit 1;
		fi
		rm $pom".old";
		echo "updated version for " $pom
	done

#updating injestor status page version. Temporary hack.
cp portal-injestor/src/main/webapp/status.txt portal-injestor/src/main/webapp/status.bak
sed -e  "s/project.version/$RELEASE_VERSION/g" portal-injestor/src/main/webapp/status.bak > status.txt.temp
mv status.txt.temp portal-injestor/src/main/webapp/status.txt
	
echo "Now going build injestor and REST .wars"
mkdir dist
mvn clean install  -Dmaven.test.skip=true
cp portal-injestor/target/*.war dist
cp portal-rest/target/*.war dist

	
#re-update all pom versions with future release version
for pom in `find . -name pom.xml`; do 
	 	cp -fp $pom $pom".old";
		sed  -e "s/<version>$RELEASE_VERSION<\/version>/<version>$FUTURE_DEV_VERSION<\/version>/g" $pom".old" > $pom 
		if [ $? -ne 0 ]; then
			exit 1;
		fi
		rm $pom".old";			
	done
mv portal-injestor/src/main/webapp/status.bak portal-injestor/src/main/webapp/status.txt
	
echo 'Done. Get your .wars at ./dist/'
	

