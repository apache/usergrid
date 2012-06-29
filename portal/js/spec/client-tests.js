$(document).ready(function () {

  initCore();

 });

function defaultSuccess(data, status, xhr) {
  start();
  if (data) {
    console.log(data);
  } else {
    console.log('no data');
  }
  // console.log(xhr);
  ok(true, "yahoo!!!");
}

function defaultError(xhr, status, error) {
  start();
  console.log('boo!');
  throw new Error("error!");
}

function initCore() {
  window.query_params = getQueryParams();
  parseParams();
  prepareLocalStorage();
  usergrid.client.Init();
}

function selectFirstApplication() {
  for (var i in usergrid.session.currentOrganization.applications) {
    usergrid.session.currentApplicationId = usergrid.session.currentOrganization.applications[i];
    localStorage.setItem('currentApplicationId', usergrid.session.currentApplicationId);
    console.log("current application: " + usergrid.session.currentApplicationId);
    break;
  };
}

QUnit.config.reorder = false;

asyncTest("logging-in with loginAdmin(credentials)", function() {
  expect(1);
  usergrid.client.loginAdmin(
    "fjendle@apigee.com",
    "mafalda1",
    defaultSuccess,
    defaultError
  );
});

asyncTest("logging-in autoLogin", function() {
  expect(1);
  usergrid.client.autoLogin(
    defaultSuccess,
    defaultError
  );
});

asyncTest("getting applications", function() {
  expect(1);
  usergrid.client.requestApplications(
    function() {
      selectFirstApplication();
      defaultSuccess();
    },
    defaultError
  );
});

asyncTest("getting users with requestUsers", function() {
  expect(1);
  usergrid.client.requestUsers(
    usergrid.session.currentApplicationId,
    defaultSuccess,
    defaultError
  );
});

asyncTest("getting users with queryUsers", function() {
  expect(1);
  usergrid.client.queryUsers(
    defaultSuccess,
    defaultError
  );
});

d = new Date;
d = MD5(d.toString()).substring(0,7);

asyncTest("creating user", function() {
  expect(1);
  usergrid.client.createUser(
    usergrid.session.currentApplicationId,
    {
      email: d + "@oarsy8.xom",
      name: d,
      password: "osautl4b",
      username: d
    },
    defaultSuccess,
    defaultError
  )
});

// asyncTest("deleting a user", function() {
//   expect(1);
//   usergrid.client.deleteUser(
//     usergrid.session.currentApplicationId,
//     null, /* select one */
//     defaultSuccess,
//     defaultError
//   )
// });

