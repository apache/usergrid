// 
// registry_list.groovy 
// 
// List contents of registry as debugging aid. Not used in any other scripts. 
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

def selectResult = sdbClient.select(new SelectRequest((String)"select * from ${domain}"))

for (item in selectResult.getItems()) {
    def att = item.getAttributes().get(0)
    println "${item.getName()} -> ${att.getName()} : ${att.getValue()}"
}
