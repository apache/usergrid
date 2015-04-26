# File storage configuration

Usergrid can store your assets either on your hard drive or in the Amazon S3 cloud.

Local storage configuration
---

By default assets are stored in the temporary folder /tmp/usergrid
to change this


This is an alternative to the S3BinaryStore class in Usergrid.
It has several advantages :
 - upload files up to 50GB
 - support of V4 signing process
 - lower network latency when a regionName is defined

To use it add following dependency in stack/pom.xml

```xml
      <dependency>
        <groupId>com.amazonaws</groupId>
        <artifactId>aws-java-sdk-s3</artifactId>
        <version>1.9.31</version>
      </dependency>
```
and stack/services/pom.xml
```xml
    <dependency>
      <groupId>com.amazonaws</groupId>
      <artifactId>aws-java-sdk-s3</artifactId>
    </dependency>
```
then add the AwsSdkS3BinaryStore.java file in the /stack/services/src/main/java/org/apache/usergrid/services/assets/data/ folder.

finaly define the new classpath in the /stack/rest/src/main/resources/usergrid-rest-context.xml file
```xml
    <bean id="binaryStore" class="org.apache.usergrid.services.assets.data.AwsSdkS3BinaryStore">
        <constructor-arg name="accessId" value="x" />
        <constructor-arg name="secretKey" value="xx" />
        <constructor-arg name="bucketName" value="x" />
        <constructor-arg name="regionName" value="eu-central-1" />
    </bean>
```
the regionName field is not mandatory, this code is also valid
```xml
    <bean id="binaryStore" class="org.apache.usergrid.services.assets.data.AwsSdkS3BinaryStore">
        <constructor-arg name="accessId" value="x" />
        <constructor-arg name="secretKey" value="xx" />
        <constructor-arg name="bucketName" value="x" />
    </bean>
```
