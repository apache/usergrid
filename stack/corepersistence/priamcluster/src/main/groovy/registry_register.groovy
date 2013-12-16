// 
// configure_usergrid.groovy 
// 
// Register this host machine as a Cassandra node in our stack. 
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

// creates domain or no-op if it already exists
sdbClient.createDomain(new CreateDomainRequest(domain))

def gar = new GetAttributesRequest(domain, hostName);
def response = sdbClient.getAttributes(gar);
if (response.getAttributes().size() == 1) {
    println "Already registered"
    def attrs = response.getAttributes()
    for (att in attrs) {
        println("${hostName} -> ${att.getName()} : ${att.getValue()}")
    }
} else {
    println "Registering..."
    def stackAtt = new ReplaceableAttribute("stackname", stackName, true)
    def attrs = new ArrayList()
    attrs.add(stackAtt)
    def par = new PutAttributesRequest(domain, hostName, attrs)
    sdbClient.putAttributes(par);
}
