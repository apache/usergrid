// 
// tag_instance.groovy 
// 
// Tag instance so we can easily identify it in the EC2 console 
//
import com.amazonaws.auth.*
import com.amazonaws.services.ec2.*
import com.amazonaws.services.ec2.model.*

String type       = (String)System.getenv().get("TYPE")
String accessKey  = (String)System.getenv().get("AWS_ACCESS_KEY")
String secretKey  = (String)System.getenv().get("AWS_SECRET_KEY")
String instanceId = (String)System.getenv().get("EC2_INSTANCE_ID")
String stackName  = (String)System.getenv().get("STACK_NAME")

def creds = new BasicAWSCredentials(accessKey, secretKey)
def ec2Client = new AmazonEC2Client(creds)

def resources = new ArrayList()
resources.add(instanceId)

def tags = new ArrayList()
tags.add(new Tag("Name", "${stackName}-${type}-${instanceId}"))

ec2Client.createTags(new CreateTagsRequest(resources, tags))
