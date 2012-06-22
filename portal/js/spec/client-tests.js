$(document).ready(function () {

  initCore();
 
 });

function defaultSuccess(data, status, xhr) {
  start();
  console.log('yay');
  console.log(data);
  console.log(status);
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

asyncTest ("logging in normal...", function() {
  expect(1);
  usergrid.client.loginAdmin("fjendle@apigee.com",
  			     "mafalda1",
  			     defaultSuccess,
  			     defaultError
  			    );
});

asyncTest ("logging in with TOKEN...", function() {
  expect(1);
  usergrid.client.autoLogin(defaultSuccess, defaultError);
});

