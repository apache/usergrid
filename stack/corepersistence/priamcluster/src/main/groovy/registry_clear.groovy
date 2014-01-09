// 
// registry_clear.groovy 
// 
// Deletes the Cassandra node registry 
//
import com.amazonaws.auth.*
import com.amazonaws.services.simpledb.*
import com.amazonaws.services.simpledb.model.*

String domain = "UGCloudFormation"
String accessKey = (String)System.getenv().get("AWS_ACCESS_KEY")
String secretKey = (String)System.getenv().get("AWS_SECRET_KEY")
String stackName = (String)System.getenv().get("STACK_NAME")
String hostName  = (String)System.getenv().get("PUBLIC_HOSTNAME")

def creds = new BasicAWSCredentials(accessKey, secretKey)
def sdbClient = new AmazonSimpleDBClient(creds)

// TODO: this should only clear out items owned by our stack, i.e. those with the same stack name 
sdbClient.deleteDomain(new DeleteDomainRequest(domain))
