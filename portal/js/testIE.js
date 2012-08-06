/**
 * @author tPeregrina
 */

bien = function(data){alert("yuhu");};
mal = function(a,b,c){alert("fallo Crossdomainrequest");console.log(a);console.log(b); console.log(c);}
url = "http://api.usergrid.com/management/token";
credencial = {
    grant_type:"client_credentials",
    client_id:"b3U6ARmhacw3EeG89xIxPRxEkQ",
    client_secret:"b3U6VC5_XmCF96ZJMEZwFA3iB-cLcQU"
};

if ('XDomainRequest' in window && window.XDomainRequest !== null) {
  // override default jQuery transport
  jQuery.ajaxSettings.xhr = function() {
      try { 
        xhr = new XDomainRequest();
       // xhr.onload = bien;

        return xhr; }
      catch(e) { }
  };
 
  // also, override the support check
  jQuery.support.cors = true; 
}


$.getJSON(url,credencial, bien);

$.ajax({url:url, data: credencial, type: 'GET',success: bien});