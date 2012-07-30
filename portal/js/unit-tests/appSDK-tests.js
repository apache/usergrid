/**
 * @author tPeregrina
 */

function initCore(){
    prepareLocalStorage();
    parseParams();    
           
};

function cleanFixture(){
	var $fixture = $("#qunit-fixture");
	$fixture.children("*").remove("*");
};
//TODO: utility method, remove soon
function isInScope(scope, functionName){
	for (name in scope) {
    	if(name==functionName){
    		return true;
    	}
	};
	return false;

};

function printScope(scope){
	for (name in scope){
		console.log("this["+name+"]");
	};
};

/*
*Default Callbacks
*/

function defaultSuccess(data, status, xhr) {
  start();
  if (data) {
	console.log(data);
  } else {
	console.log('no data');
  }
  
  ok(true, "yahoo!!!");
};

function loginSuccess(data, status, xhr){
	start()
		if(data){
			console.log(data);
			credentials.token = data.access_token;
		}else{
			console.log("no Data");
		}
		
		ok(true, "loginSuccess")
};

function defaultError(xhr, status, error) {
  start();
  console.log(xhr);
  console.log('boo!');
  throw new Error("error!");
}

/*
* Fixtures
*/
var	credentials = {		
		login :"tperegrina@nearbpo.com",
		password:"123456789",
		token:"",
		UUID: "",
		userName: "",
		
	};
	
var apiClient = APIClient;

var org = {
	UUID : "ee6f0b10-d747-11e1-afad-12313b01d5c1",
	app1UUID: "ab252e3b-da8d-11e1-afad-12313b01d5c1",
	app2UUID: "568af5e2-d748-11e1-afad-12313b01d5c1",
	orgName: "MusicOrg",
};

var mockUser = {
	username: "",
	name:"",
	email :"",
	password:"",
	UUID: "",	
};

/*
 * Clean templates to Copy/Paste
 */
//TODO: remove templates after placing initial infrastructure
module("", {
		setup:function(){
			//Before test
	},
	teardown:function(){
		//after test
	}
});//END MODULE DEFINITION


module("login", {
	setup:function(){		
		initCore();
		apigee.userSession.clearAll();		
	},//FIN SETUP
	teardown:function(){
		 cleanFixture();
	}//END TEARDOWN
});//END MODULE DEFINITION

asyncTest("login with credentials", function(){
	expect(1);
	
	var formdata = {
      	grant_type: "password",
     	username: credentials.login,
     	password: credentials.password
   	 	};   		
		apiClient.runManagementQuery(new apigee.QueryObj('GET', 'token', null, formdata, loginSuccess, defaultError));
});

asyncTest("login with Token", function(){
	expect(1);
	var token = credentials.token;
	apiClient.setToken(token);
	apiClient.runManagementQuery(new apigee.QueryObj("GET","users/" + credentials.login, null, null, defaultSuccess, defaultError));
});

module("GET Methods", {
	setup:function(){		
		initCore();	
	},//FIN SETUP
	teardown:function(){
		 cleanFixture();
	}//END TEARDOWN
});//END MODULE DEFINITION

asyncTest("Fetching Apps from Org: " + org.orgName + " GET", function(){
	expect(1);
	apiClient.runManagementQuery(new apigee.QueryObj("GET", "organizations/" + org.UUID + "/applications", null, null, defaultSuccess, defaultError));
});


module("POST Methods", {
	setup:function(){		
		initCore();	
	},//FIN SETUP
	teardown:function(){
		 cleanFixture();
	}//END TEARDOWN
});//END MODULE DEFINITION

asyncTest("Add new User : " + mockUser.username + " POST", function(){
	expect(1);
	var data = {
		username: mockUser.username,
		name: mockUser.name,
		email: mockUser.email,
		password: mockUser.password	
	};
	apiClient.runAppQuery(new apigee.QueryObj("POST", 'users', data, null,defaultSuccess, defaultError));
});

module("PUT Methods", {
	setup:function(){
		initCore();
	},
	teardown:function(){
		cleanFixture();
	}
});

asyncTest("Update AccountUser : " + mockUser.username + " PUT", function(){
	expect(1);
	var userData = {
		username: credentials.login,
		password: credentials.password,
		email:	credentials.userName,
	};
	apiClient.runManagementQuery(new apigee.QueryObj("PUT",'users/' + credentialsUUID, userData, null, defaultSuccess, defaultError));
});

module("DELETE Methods", {
	setup:function(){
		initCore();
	},
	teardown:function(){
		cleanFixture();
	}
});

asyncTest("Delete User : " + mockUser.username + " DELETE", function(){
	expect(1);
	//TODO: save user UUID en el fixtures? u obtenerlo dinamico? a lo mejor, crear y borrar user en el mismo test, pero deja de ser unitario
	var userId = "";
	apiClient.runAppQuery(new apigee.QueryObj("DELETE", 'users/' + userId, null, null, defaultSuccess, defaultError));
});



