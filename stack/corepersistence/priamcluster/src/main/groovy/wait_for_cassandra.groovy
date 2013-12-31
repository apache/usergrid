// 
// wait_for_cassandra.groovy 
// 
// Wait for enough Cassandra servers are up before proceding,
// Enough means count greater than or equal to replication factor. 
//
import com.amazonaws.auth.*
import com.amazonaws.services.simpledb.*
import com.amazonaws.services.simpledb.model.*

String domain    = "UGCloudFormation"
String accessKey = (String)System.getenv().get("AWS_ACCESS_KEY")
String secretKey = (String)System.getenv().get("AWS_SECRET_KEY")
String stackName = (String)System.getenv().get("STACK_NAME")
def replicationFactor = System.getenv().get("CASSANDRA_REPLICATION_FACTOR")

def creds = new BasicAWSCredentials(accessKey, secretKey)
def sdbClient = new AmazonSimpleDBClient(creds)

println "Waiting..."
    
def count = 0
while (true) {
    try {
        def selectResult = sdbClient.select(new SelectRequest((String)"select * from ${domain}"))
        for (item in selectResult.getItems()) {
            def att = item.getAttributes().get(0)
            if (att.getValue().equals(stackName)) {
                count++
            }
        }
        if (count >= replicationFactor || count > 300) {
            break
        }
    } catch (Exception e) {
        println "ERROR waiting for Casasndra ${e.getMessage()}, will continue waiting"
        return
    }
    Thread.sleep(1000)
}

println "Waiting done."
