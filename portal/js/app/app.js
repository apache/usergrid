Usergrid.organizations = new Usergrid.Organization();

var Pages = new ApigeePages();



$(document).ready(function () {

  var query_params = Usergrid.Params.queryParams;
  if (Usergrid.apiUrl) {
    Usergrid.ApiClient.setApiUrl(Usergrid.apiUrl);
  }
  Pages.resetPasswordUrl = Usergrid.ApiClient.getResetPasswordUrl();

  initCore();
  initUI(query_params);
  startApp();

  function initCore() {
    prepareLocalStorage();
    parseParams();
  }

  function initUI(query_params) {
    apigee_console_app(Pages, query_params);
    initMenu();
    StatusBar.Init('#statusbar-placeholder');
    toggleableSections();
  }

  function startApp() {

    if (!Usergrid.userSession.loggedIn()) {
      // test to see if the Portal is running on apigee, if so, send to SSO, if not, fall through to login screen
      if (Usergrid.SSO.usingSSO()) {
        Pages.clearPage();
        Usergrid.SSO.sendToSSOLoginPage();
      } else if (query_params.goto_signup) {
        Pages.ShowPage("signup");
      } else {
        Usergrid.console.showLoginForNonSSO();
      }
    } else {
      Usergrid.console.autoLogin(
        function () {
          Usergrid.console.loginOk();
        },
        function () {
          Usergrid.console.logout();
        }
      );
    }
  }

  function initMenu() {
    $('.navbar .dropdown-toggle').dropdown();
    $('#sidebar-menu .dropdown-toggle').dropdown();
    $('#logout-link').click(Usergrid.console.logout);
    $('#hideBanner').click(Pages.hideBanner);

    var publicMenu = $('#publicMenu');
    var privateMenu = $('.privateMenu');

    Pages.AddPage({name:'login', menu:publicMenu});
    Pages.AddPage({name:'message', menu:publicMenu});
    Pages.AddPage({name:'signup', menu:publicMenu});
    Pages.AddPage({name:'forgot-password', menu:publicMenu});
    Pages.AddPage({name:'post-signup', menu:publicMenu});
    Pages.AddPage({name:'console', menu:privateMenu, initFunction:initConsole, showFunction: function() {
      if(!Backbone.History.started){
        Backbone.history.start();
      }
    }});
  }

  function initConsole() {
    //Pages.AddPanel(pageName,linkSelector,boxSelector,initfunc,showfunc,buttonHandlerFunction);
    Pages.AddPanel('organization', '.go-home', null,null, null, Usergrid.console.pageSelectHome,null);
    Pages.AddPanel('console', null, null, null, null, null, null);
    Pages.AddPanel('dashboard', null, null, null, null, Usergrid.console.pageSelectApplication,null);
    Pages.AddPanel('user', null, "#users-sublink", null, null, null, function() {});
    Pages.AddPanel('users', null, "#users-sublink", null, null, Usergrid.console.pageSelectUsers, null);
    Pages.AddPanel('group', null, "#groups-sublink", null, null, null, function() {});
    Pages.AddPanel('groups', null, null, null, null, Usergrid.console.pageSelectGroups, null);
    Pages.AddPanel('roles',  null, null, null, null, Usergrid.console.pageSelectRoles, null);
    Pages.AddPanel('activities', null, null, null, null, Usergrid.console.pageSelectActivities, null);
    Pages.AddPanel('notifications', null, null, null, null, Usergrid.console.pageSelectNotifcations, null);
    Pages.AddPanel('notifications-', null, "#notifications-sublink", null, null, null, function() {});
    Pages.AddPanel('notifications--', null, "#notifications--sublink", null, null, null, function() {});
    Pages.AddPanel('collections', "#collections-link", null, null, null, Usergrid.console.pageSelectCollections, null);
    Pages.AddPanel('analytics', null, null, null, null, Usergrid.console.pageSelectAnalytics, null);
    Pages.AddPanel('properties', null, null, null, null, Usergrid.console.pageSelectProperties, null);
    Pages.AddPanel('shell', null, null, null, null, Usergrid.console.pageSelectShell, null);
    Pages.AddPanel('account', "#account-link", null, null, null, null, accountRedirect);
    //$("#sidebar-menu > ul > li > a").click(Pages.ShowPanel);

  }

  function accountRedirect(e) {
    e.preventDefault();
    Usergrid.console.requestAccountSettings(Backbone.history.getHash(window));
  }

  function initCenterPanels(){
    $(window).resize(centerPanels);
    $(window).resize();
  }

  function centerPanels(){
    var panels = $("#console-page");
    var freeSpace = $(window).width() - panels.width();
    console.log("window: " + $(window).width() + " Panels:" + panels.width());
    console.log("free space: "+freeSpace);
    panels.css('margin-left',function(){return freeSpace / 2;});
  }
});
