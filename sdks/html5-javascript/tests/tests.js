

Usergrid.ApiClient.init('Apigee', 'Sandbox');


var user = {
  username: 'tester',
  password: 'password'
};

module( "User Testing" );

asyncTest("Create User", function () {
  var success = function (result) {
    ok( true, "User Created!" );
    start();
  },

  error = function (result) {
    ok( false, 'Create User: <pre>ERROR: '+result+'</pre>' );
    start();
  };

  expect(1);
  Usergrid.ApiClient.runAppQuery (new Usergrid.Query('POST', 'users', user, null, success, error));
 
});

asyncTest("Update User", function () {
  var success = function (result) {
    ok( true, "User Updated!" );
    start();
  },

  error = function (result) {
    ok( false, 'Update User: <pre>ERROR: '+result+'</pre>' );
    start();
  };

  expect(1);
  data = {'key':'value'};
  Usergrid.ApiClient.runAppQuery (new Usergrid.Query('PUT', 'users/'+user.username, data, null, success, error));
  
});

asyncTest("Get User", function () {
  var success = function (result) {
    ok( true, "User Updated!" );
    start();
  },

  error = function (result) {
    ok( false, 'Get User: <pre>ERROR: '+result+'</pre>' );
    start();
  };

  expect(1);
  Usergrid.ApiClient.runAppQuery (new Usergrid.Query('GET', 'users/'+user.username, null, null, success, error));
  
});

asyncTest("Delete User", function () {
  var success = function (result) {
    ok( true, "User Deleted!" );
    start();
  },

  error = function (result) {
    ok( false, 'Delete User: <pre>ERROR: '+result+'</pre>' );
    start();
  };

  expect(1);
  Usergrid.ApiClient.runAppQuery (new Usergrid.Query('DELETE', 'users/'+user.username, null, null, success, error));
});

