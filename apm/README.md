Developers Getting Started for Apigee App Performance Monitoring (APM) previously known as mAX.

**Pre-requisites:**
- Oracle JDK 7 preferred. Works with JDK 6 as well.
- Download Tomcat 7. Works with Tomcat 6 as well.
- AWS account with access to SQS and S3
- MySql or Amazon RDS

**Database Setup:**
- Install MySQL 5.5/5.6 locally or use Amazon RDS.
- Create a new user with all DB permissions and note password. 
- Create two databases named instaops_appmanagement and instaops_analytics.
- Into instaops_appmanagement, import schema from db-schemas/apm-config-schema.sql. Verify that tables got created.
- Into instaops_analytics, import schema from db-schemas/apm-metrics-schema.sql Verify that tables got created.
- Make sure that user you created has full access to these two schemas.

**Building the application**
- To do a quick build:
   mvn clean install -DskipTests
- To do a build with tests
	mvn clean install	
Build produces two wars. One for injestor apigee-apm-injestor-$version.war  and one for REST apigee-apm-rest-$version.war. 
apigee-apm-rest.war is superset of apigee-apm-injestor.war so for local testing, you only need to deploy REST .war file and it will
work as injestor too as long as properties file (see below) is set correctly. In production and different planets, we have separate machines
for injestor and REST servers.
	
**Properties file**
All together 3 props file need to be in classpath. To ease with .war deployments across different planets and avoid confusion with Ops, 
we recommended that Ops copies these props file to lib folder of tomcat. It's recommended to do the same while testing locally or you can
put these files in portal-service/src/main/resources. You can see example of these files under portal-service/src/test/resources
hibernate-analytics.cfg.xml (make sure to update DB connection, username and password properties)
hibernate-app-management.cfg.xml (make sure to update DB connection, username and password properties)
deployment-config.properties ( make sure to update AWS keys, SQS account number, S3 bucket, enable/disable injestor)	

**Deploy**	
- Verify that 3 properties files are present in tomcat's lib folder.
-Rename the .war from portal-rest/target/apigee-apm.war to ROOT.war and deploy it to Tomcat 6 or Tomcat 7 with Injestor enabled in
deployment-config.properties file. You should not see any error in catalina.out 

Verify the application is running at :
-http://localhost:8080/apm/status (app context will change if you use different maven profile and your version is different)
-If you have Demo Org and Demo App created using DemoAppDataPopulator (you can run it in your IDE or from command line),
 then you should be able to get metrics for example at: http://localhost:8080/Demo/AcmeBank/apm/networkRawData/1
 
 This takes care of App Monitoring deployment
 
 ** Complete end to end deployment**
 - System diagram at https://docs.google.com/a/apigee.com/drawings/d/12xnFhXGPYqbXeo6ths3GFESY7GGTg7zKzBnTgtBqMJA/edit
 -Have a running Usergrid configured with App Montoring. When a new app gets created, Usergrid makes a REST call to APM
 to register a new app. During crash log parsing, APM makes a REST call to Usergrid to find who to send email notification too. [More details to be added]
 -Gateway bundle that wires Usergrid, APM, SQS and S3 together. Details at https://docs.google.com/a/apigee.com/document/d/1IzYDIJtvSKwNLUl_XNYE1Lo-mWU3EeZ4QuUfKc-MYXo/edit
 -Bundle code can be seen at revision.eng.wacapps.net:pjha/max-gateway.git
 
 This README doc needs to have more details and will be updated as part of transition. 
 
 Pls contact prabhat@apigee.com if you need help.
 






