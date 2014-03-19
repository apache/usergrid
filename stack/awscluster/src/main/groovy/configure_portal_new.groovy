//
// Emits dash/app/config.js file 
//
def baseUrl = "http://${System.getenv().get("DNS_NAME")}.${System.getenv().get("DNS_DOMAIN")}"
config = """
var VERSION = 'R-2013-07-02-02';
var Usergrid = Usergrid || {};
Usergrid.showNotifcations = true;

// used only if hostname does not match a real server name
Usergrid.overrideUrl = '${baseUrl}';

Usergrid.settings = {
  hasMonitoring:true //change to false to remove monitoring
};
"""
println config
