/**
 * @author tPeregrina
 */


//Initializadon of the basic UserGrid infrastructure to be able to run the tests
$(document).ready(function() {
	console.log("inicializando general");
	initCore();
});

//TODO: Update initalization of infrastructure to reflect the changes in APP.SDK

function initCore(){
    prepareLocalStorage();
    parseParams();    
           
};

function initFixtures(){

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
}

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
		loginId: "login-email",
		passwordId: "login-password",
	};

/*
 * Clean templates to Copy
 */

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
		/* TODO: Check if commented part of the code is needed
		var $fixture = $("#qunit-fixture");
		var $inputEmail = $("<input type='text' />").attr("id", credentials.loginId).attr("value", credentials.login)
		var $inputPassword = $("<input type='password'/>").attr("id", credentials.passwordId).attr("value", credentials.password);
		$fixture.append($inputEmail, $inputPassword);
		*/
		
	},//FIN SETUP
	teardown:function(){
		 var $fixture = $("#qunit-fixture");
		 $fixture.children("*").remove("*");
	}//END TEARDOWN
});//END MODULE DEFINITION


asyncTest("login with credentials", function(){
	expect(1);
	var formdata = {
      	grant_type: "password",
     	username: credentials.login,
     	password: credentials.password
   	 	};			
	usergrid.client.runManagementQuery(new QueryObj('GET', 'token', null, formdata, defaultSuccess, defaultError));
});

asyncTest("login with Token", function(){
	expect(1);
	var token = usergrid.session.getAccessToken();
	usergrid.client.setToken(token);
	usergrid.client.runManagementQuery(new QueryObj("GET","users/" + usergrid.session.getUserEmail(), null, null, defaultSuccess, defaultError));
});

module("APIClient");


/*
* User tests
* CRUD tests for the user model
*/
module("User group");

//TODO USER TESTS
asyncTest("Create User", function() {

});

asyncTest("Update User", function() {

});

