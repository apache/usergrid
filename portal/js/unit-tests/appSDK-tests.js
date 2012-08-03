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

function printScope(scope){
	for (name in scope){
		console.log("this["+name+"]");
	};
};


function loginWithCredentials(calledFunction){	
	
	var formdata = {		
      	grant_type: "password",
     	username: credentials.login,
     	password: credentials.password
   	 	};   		
		apiClient.runManagementQuery(new apigee.QueryObj('GET', 'token', null, formdata, 
		//Success callback
		function(data){		
			if(data){
				calledFunction(data.access_token);
			};				
		}, defaultError));
};
//CLEANUP: used to remove users created programatically
function deleteUser(uuid){
	loginWithCredentials(function(token){
		apiClient.setToken(token)
		apiClient.setOrganizationName(mockOrg.UUID);
		apiClient.setApplicationName(mockOrg.mockApp.UUID);
		apiClient.runAppQuery(new apigee.QueryObj("DELETE", 'users/' + uuid, null, null));
		console.log("CLEANUP: DELETED USER UUID: " + uuid);		
	});
};

//SETUP: used to create users to be deleted or modified
function createUser(){
	var uuid;
	var data = getMockUserData();
	loginWithCredentials(function(){
		APIClient.setToken();		
		apiClient.setOrganizationName(mockOrg.UUID);
		apiClient.setApplicationName(mockOrg.mockApp.UUID);	
		apiClient.runAppQuery(new apigee.QueryObj("POST", 'users', data, null,
		function(data){
			console.log(data.entities[0].uuid);
			credentials.UUID = data.entities[0].uuid;
		}));		
	});
	
};

function getMockUserData(){
	var userMod = "1";
	var data = {
		username: mockCreateUser.baseUserName + userMod,
		name: mockCreateUser.baseName,
		email: mockCreateUser.emailHead + userMod + mockCreateUser.emailTail,
		password: mockCreateUser.password,
	}	
	return data;
};

/*
* Fixtures
*/
var	credentials = {		
		login :"tperegrina@nearbpo.com",
		password:"123456789",
		UUID: "",
		userName: "",		
};
	
var apiClient = APIClient;

var mockOrg = {
	UUID : "af32c228-d745-11e1-b36a-12313b01d5c1",
	mockApp: {
		name: "SANDBOX",
		UUID: "af4fe725-d745-11e1-b36a-12313b01d5c1",
	},
	name: "MusicOrg",
};
var mockUser = {
	username: "User1",
	name:"Ann User",
	email :"annUser@AnnUserEnterprises.com",
	password:"123456789",
	UUID: "08bc0ec6-dbf8-11e1-93e3-12313b0c5c38",	
};
var mockCreateUser = {
	baseUserName: "User-",
	baseName : "Mock User",
	emailHead: "MockUser-",
	emailTail: "@mockuser.com",
	password: "12345679",
	UUID: "",
}
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
function userCreateSuccess(data, status, xhr){
	start();
	var uuid = data.entities[0].uuid;
	deleteUser(uuid);
	ok(true, "UserCreated Success ID:" + uuid);
}

function defaultError(xhr, status, error) {
  start();
  console.log(xhr);
  console.log('boo!');
  throw new Error("error!");
}

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
	loginWithCredentials(function(token){
		start();		
		ok(true, "Succesful login TOKEN: " + token);		
	});
});

asyncTest("login with Token", function(){	
	expect(1);	
	loginWithCredentials(function(token){				
		apiClient.setToken(token);
		apiClient.runManagementQuery(new apigee.QueryObj("GET","users/" + credentials.login, null, null, defaultSuccess, defaultError));		
	});
});

module("GET Methods", {
	setup:function(){		
		initCore();
	},//FIN SETUP
	teardown:function(){
		 cleanFixture();
	}//END TEARDOWN
});//END MODULE DEFINITION

asyncTest("Fetching Apps from Org: " + mockOrg.name + " GET", function(){
	expect(1);
	loginWithCredentials(function(token){
		apiClient.setToken(token);	
		apiClient.runManagementQuery(new apigee.QueryObj("GET", "organizations/" + mockOrg.UUID + "/applications", null, null, defaultSuccess, defaultError));
	});
});

asyncTest("Requesting User ID : " + mockUser.UUID + " GET", function(){
	expect(1);
	loginWithCredentials(function(token){			
		apiClient.setToken(token);		
		apiClient.setOrganizationName(mockOrg.UUID);
		apiClient.setApplicationName(mockOrg.mockApp.UUID);	
		apiClient.runAppQuery(new apigee.QueryObj("GET", 'users/'+ mockUser.UUID, null, null, defaultSuccess, defaultError));
	});
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
		var data = getMockUserData();
		apiClient.setOrganizationName(mockOrg.UUID);
		apiClient.setApplicationName(mockOrg.mockApp.UUID);	
		apiClient.runAppQuery(new apigee.QueryObj("POST", 'users', data, null, userCreateSuccess, defaultError));
});

module("DELETE Methods", {
	setup:function(){
		initCore();
	},
	teardown:function(){
		cleanFixture();
	}
});
//TODO: Refractor the user creation out of this method
asyncTest("Delete User : " + mockUser.username + " DELETE", function(){
	expect(1);
	loginWithCredentials(function(token){		
		apiClient.setToken(token);
		var data = getMockUserData();
		apiClient.setOrganizationName(mockOrg.UUID);
		apiClient.setApplicationName(mockOrg.mockApp.UUID);
		apiClient.runAppQuery(new apigee.QueryObj("POST", 'users', data, null,
		function(data){
			console.log(data.entities[0].uuid);
			var uuid= data.entities[0].uuid;
			apiClient.runAppQuery(new apigee.QueryObj("DELETE", 'users/' + uuid, null, null, defaultSuccess, defaultError));
		}));			
		
	});
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
	apiClient.runManagementQuery(new apigee.QueryObj("PUT",'users/' + credentialsUUID, userData, null, userCreateSuccess, defaultError));
});



