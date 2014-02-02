
// 
// configure_cassandra.groovy 
// 
// Emits Cassandra config file based on environment and Cassandra node 
// registry in SimpleDB
//
import com.amazonaws.auth.*
import com.amazonaws.services.simpledb.*
import com.amazonaws.services.simpledb.model.*

String accessKey = (String)System.getenv().get("AWS_ACCESS_KEY")
String secretKey = (String)System.getenv().get("AWS_SECRET_KEY")
String stackName = (String)System.getenv().get("STACK_NAME")
String hostName  = (String)System.getenv().get("PUBLIC_HOSTNAME")
def clusterName  = (String)System.getenv().get("CASSANDRA_CLUSTER_NAME")
String domain    = stackName

def creds = new BasicAWSCredentials(accessKey, secretKey)
def sdbClient = new AmazonSimpleDBClient(creds)

// build seed list by listing all Cassandra nodes found in SimpleDB domain with our stackName
def selectResult = sdbClient.select(new SelectRequest((String)"select * from ${domain}"))
def seeds = ""
def sep = ""
for (item in selectResult.getItems()) {
    def att = item.getAttributes().get(0)
    if (att.getValue().equals(stackName)) {
        seeds = "${seeds}${sep}\"${item.getName()}\""
        sep = ","
    }
}

def elasticSearchConfig = """
cluster.name: usergrid2
discovery.zen.ping.multicast.enabled: false
discovery.zen.ping.unicast.hosts: [${seeds}]
node:
    name: ${hostName} 
network:
    host: ${hostName}\
path:
  logs: /mnt/log/elasticsearch
  data: /mnt/data/elasticsearch
"""

println elasticSearchConfig
