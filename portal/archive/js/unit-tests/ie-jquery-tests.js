/**
 * @author tPeregrina
 */

/*
 * Intialization
 */

var init = {
	core: function(){
		prepareLocalStorage();
    	parseParams();
    	init.modAjax();      
	},
	modAjax: function(){

	},
};

/*
 * Callbacks
 */

var cb = {
	simpleSuccess: function(data){
		start();		
	  	if (data) {
			console.log("DATOS: " + data);
	  	}else{
			console.log('no data');
	  	}  
	  	ok(true, "Succesful test");		
	},
	simpleFailure: function(data, status, xhr){
		start();
	  	console.log(xhr);
	  	console.log('FAIL!');
	  	throw new Error("error!");		
	},
};

/*
 * Utilities
 */

var util = {
	printScope: function(scope){
		for (name in scope){
			console.log("this["+name+"]");
		};
	},
	saveCredentials: function(token){
		credentials.token = token;
		console.log("SAVED TOKEN: "+credentials.token);
	},
	queryAPI: function(queryType, queryUrl, queryData, successCB, failureCB){
		if ('XDomainRequest' in window && window.XDomainRequest !== null) {
		  util.setAjaxToXDom(successCB, failureCB);
		  $.ajax({
		  	type: queryType,
		  	url: queryUrl,
			data: queryData,
			contentType: "application/json",
		  });
		}else{//REQUEST IS HttpHeadersRequest
			$.ajax({
				type: queryType,
				url: queryUrl,
				data: queryData,
				success: successCB,
				failure: failureCB,
			});
		};
	},
	setAjaxToXDom:function(success, failure){
		// override default jQuery transport
		  jQuery.ajaxSettings.xhr = function(){
		      try { 
		        xhr = new XDomainRequest();
		        xhr.contentType = "application/json";
				xhr.onload = success;
				xhr.onerror = failure;
		        return xhr;}
		         /*
		      return new XDomainRequest();}
		      */
		      catch(e) { }
		  };	 
		  // also, override the support check
		  jQuery.support.cors = true;	 
	},
	login: function(){
		var loginCredentials = {
			grant_type: "password",
			username: credentials.login,
			password: credentials.password,
		};
		
		var loginUrl = url.base + url.login;
		
		function exitoXDom(){
			response = xhr.responseText;
			parsedResponse = $.parseJSON(response);
			util.saveCredentials(parsedResponse.access_token);
			cb.simpleSuccess(response);
		};
		
		util.queryAPI('GET', loginUrl, loginCredentials, exitoXDom, cb.simpleFailure);		
		
	},
	autoLogin: function(){
		var tokenURL = url.base + url.autoLogin + credentials.login + url.token + credentials.token;
		util.queryAPI('GET',tokenURL,null,cb.simpleSuccess, cb.simpleFaillure);

	},
	createApp: function(){
		var appURL = url.base + url.managementOrgs + org.UUID + url.app + url.token + credentials.token;
		var appData = {
			name : "Nombre Generico 1",
		}
		appData = JSON.stringify(appData);
		console.log("DATOS: " + appData);
		util.queryAPI('POST', appURL, appData, cb.simpleSuccess, cb.simpleFailure);
	},
	createUser: function(){
		var userUrl= url.base + org.UUID + "/" + org.app.UUID + url.users;
		var userData = JSON.stringify(mockUser);
		util.queryAPI('POST', userUrl, userData, cb.simpleSuccess, cb.simpleFailure);		
	}
};

/*
 * Fixtures
 */

credentials = {
	login : "tperegrina@nearbpo.com",
	password : "123456789",
	UUID : "",
	token: "",	
}

mockUser = {
	username: "Usuario1",
	name: "Un Usuario",
	email: "Usuario@fakeEmailAddress.com",
	password: "123456789",
}

apiClient = APIClient;

org = {
	name: "tperegrina",
	UUID: "af32c228-d745-11e1-b36a-12313b01d5c1",
	app: {
		name: "SANDBOX",
		UUID: "af4fe725-d745-11e1-b36a-12313b01d5c1",
	}
};


url = {
	base: "http://api.usergrid.com/",
	login: "management/token",
	autoLogin: "management/users/",
	managementOrgs : "management/organizations/",
	app: "/applications",
	token:	"?access_token=",
	users: "/users",
}

/*
 * Tests 
 */

asyncTest("Test CRUD APP", function(){
	expect(4);
	start();
	init.core();
	util.login();
	util.autoLogin();
	util.createApp();
	util.updateOrg();
	util.readOrg();
	util.deleteOrg();	
});

asyncTest("TEST CRUD USERS", function(){
	expect(1);
	util.createUser();
});
