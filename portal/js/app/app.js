Usergrid.organizations = new Usergrid.Organization();

var Pages = new ApigeePages();

$(document).ready(function () {
  var query_params = Usergrid.Params.queryParams;
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
    var privateMenu = $('#privateMenu');

    Pages.AddPage({name:'login', menu:publicMenu});
    //Pages.ShowPage('login');
    Pages.AddPage({name:'signup', menu:publicMenu});
    Pages.AddPage({name:'forgot-password', menu:publicMenu});
    Pages.AddPage({name:'post-signup', menu:publicMenu});
    Pages.AddPage({name:'console', menu:privateMenu, initFunction:initConsole, showFunction:Usergrid.console.pageSelectHome});
  }

  function initConsole() {
    //Pages.AddPanel(pageName,linkSelector,boxSelector,initfunc,showfunc);
    Pages.AddPanel('organization', null, null, null, null);
    Pages.AddPanel('console', null, null, null, null);
    Pages.AddPanel('application', null, null, null, Usergrid.console.pageSelectApplication);
    Pages.AddPanel('user', "#sidebar-menu a[href='#users']", null, null, null);
    Pages.AddPanel('users', null, null, null, Usergrid.console.pageSelectUsers);
    Pages.AddPanel('group', "#sidebar-menu a[href='#groups']", null, null, null);
    Pages.AddPanel('groups', null, null, null, Usergrid.console.pageSelectGroups);
    Pages.AddPanel('roles', null, null, null, Usergrid.console.pageSelectRoles);
    Pages.AddPanel('activities', null, null, null, Usergrid.console.pageSelectActivities);
    Pages.AddPanel('collections', null, null, null, Usergrid.console.pageSelectCollections);
    Pages.AddPanel('analytics', null, null, null, Usergrid.console.pageSelectAnalytics);
    Pages.AddPanel('properties', null, null, null, Usergrid.console.pageSelectProperties);
    Pages.AddPanel('shell', null, null, null, Usergrid.console.pageSelectShell);
    Pages.AddPanel('account', "#account-link", null, null, Usergrid.console.requestAccountSettings);
    //$("#sidebar-menu > ul > li > a").click(Pages.ShowPanel);
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
