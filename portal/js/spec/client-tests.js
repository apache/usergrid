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
  throw new Error("aorsientaroiesn");
}

function initCore() {
  window.query_params = getQueryParams();
  parseParams();
  prepareLocalStorage();
  usergrid.client.Init();
}

asyncTest("logging-in with credentials", function() {
  expect(1);
  usergrid.client.loginAdmin("fjendle@apigee.com",
  			     "mafalda1",
  			     defaultSuccess,
                             function() {console.log('boo1')}
  			    );
});

asyncTest("logging-in with token", function() {
  expect(1);
  usergrid.client.autoLogin(defaultSuccess, defaultError);
});

asyncTest("getting applications", function() {
  expect(1);
  usergrid.client.requestApplications(defaultSuccess, defaultError);
  // for (var i in usergrid.session.currentOrganization.applications) {
  //   usergrid.session.currentApplicationID = usergrid.session.currentOrganization.applications[i];
  //   break;
  // };
});

// asyncTest("getting users list", function() {
//   expect(1);
//   usergrid.client.requestUsers(usergrid.session.currentApplicationId, defaultSuccess, function(){console.log('eh!')});
// });
