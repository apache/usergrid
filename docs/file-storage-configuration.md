# File storage configuration

Usergrid can store your assets either on your hard drive or in the Amazon S3 cloud.

Local storage configuration
---

By default assets are stored in the temporary folder /tmp/usergrid
This can be changed by editing this file /stack/rest/src/main/resources/usergrid-rest-context.xml and replacing {usergrid.temp.files} by the wanted destination
```xml
<bean id="binaryStore" class="org.apache.usergrid.services.assets.data.LocalFileBinaryStore">
  <property name="reposLocation" value="${usergrid.temp.files}"/>
</bean>
```

AwS S3 configuration
---

To use your AWS S3 storage you need to change the binaryStore classpath and add several constructor arguments in /stack/rest/src/main/resources/usergrid-rest-context.xml

Some examples :
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

The filesize is limited to 50GB but you need to keep in mind that the file has to be stored on the hard drive before being sended to Amazon.
