// 
// wait_for_cassandra.groovy 
// 
// Wait for enough Cassandra servers are up before proceding,
// Enough means count greater than or equal to replication factor. 
//
import com.amazonaws.auth.*
import com.amazonaws.services.simpledb.*
import com.amazonaws.services.simpledb.model.*

String accessKey = (String)System.getenv().get("AWS_ACCESS_KEY")
String secretKey = (String)System.getenv().get("AWS_SECRET_KEY")
String stackName = (String)System.getenv().get("STACK_NAME")
String domain    = stackName

//def replicationFactor = System.getenv().get("CASSANDRA_REPLICATION_FACTOR")
int cassNumServers = System.getenv().get("CASSANDRA_NUM_SERVERS").toInteger()

def creds = new BasicAWSCredentials(accessKey, secretKey)
def sdbClient = new AmazonSimpleDBClient(creds)

println "Waiting for Cassandra nodes to register..."
    
def count = 0
while (true) {
    try {
        def selectResult = sdbClient.select(new SelectRequest((String)"select * from `${domain}` where itemName() is not null  order by itemName()"))
        for (item in selectResult.getItems()) {
            def att = item.getAttributes().get(0)
            if (att.getValue().equals(stackName)) {
                println("Found node with ip ${item.getName()}.  Incrementing count")
                count++
            }
        }
        if (count >= cassNumServers) {
            println("count = ${count}, total number of servers is ${cassNumServers}.  Breaking")
            break
        }
    } catch (Exception e) {
        println "ERROR waiting for Casasndra ${e.getMessage()}, will continue waiting"
        return
    }
    Thread.sleep(1000)
}

println "Waiting done."
