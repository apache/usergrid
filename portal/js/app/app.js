

apigee.userSession = new apigee.UserSession();
apigee.organizations = new apigee.Organization();

var Pages = new ApigeePages();

$(document).ready(function () {
  var query_params = getQueryParams();
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
    if (!apigee.userSession.loggedIn()) {
      // test to see if the Portal is running on apigee, if so, send to SSO, if not, fall through to login screen
      if ( apigee.console.useSSO() ){
        Pages.clearPage();
        apigee.console.sendToSSOLoginPage();
      } else if (query_params.goto_signup) {
        Pages.ShowPage("signup");
      } else {
        apigee.console.showLoginForNonSSO();
      }
    } else {
      apigee.console.autoLogin(
        function() {
          apigee.console.loginOk();
        },
        function() {
          apigee.console.logout();
        }
      );
    }

  }

  function initMenu() {
    $('.navbar .dropdown-toggle').dropdown();
    $('#sidebar-menu .dropdown-toggle').dropdown();
    $('#logout-link').click(apigee.console.logout);
    $('#hideBanner').click(Pages.hideBanner);

    var publicMenu = $('#publicMenu');
    var privateMenu =$('#privateMenu');

    Pages.AddPage({name:'login', menu:publicMenu});
    //Pages.ShowPage('login');
    Pages.AddPage({name:'signup', menu:publicMenu});
    Pages.AddPage({name:'forgot-password', menu:publicMenu});
    Pages.AddPage({name:'post-signup', menu:publicMenu});
    Pages.AddPage({name:'console', menu:privateMenu, initFunction:initConsole, showFunction:apigee.console.pageSelectHome});

  }

  function initConsole() {
    //Pages.AddPanel(pageName,linkSelector,boxSelector,initfunc,showfunc);
    Pages.AddPanel('organization', null, null, null, null);
    Pages.AddPanel('console', null, null, null ,null );
    Pages.AddPanel('application', null, null, null, apigee.console.pageSelectApplication);
    Pages.AddPanel('user', "#sidebar-menu a[href='#users']", null, null, null);
    Pages.AddPanel('users', null, null, null, apigee.console.pageSelectUsers);
    Pages.AddPanel('group', "#sidebar-menu a[href='#groups']", null, null, null);
    Pages.AddPanel('groups', null, null, null, apigee.console.pageSelectGroups);
    Pages.AddPanel('roles', null, null, null, apigee.console.pageSelectRoles);
    Pages.AddPanel('activities', null, null, null, apigee.console.pageSelectActivities);
    Pages.AddPanel('collections', null, null, null, apigee.console.pageSelectCollections);
    Pages.AddPanel('analytics', null, null, null, apigee.console.pageSelectAnalytics);
    Pages.AddPanel('shell', null, null, null, apigee.console.pageSelectShell);
    Pages.AddPanel('account', "#account-link", null, null, apigee.console.requestAccountSettings);
    //$("#sidebar-menu > ul > li > a").click(Pages.ShowPanel);
  }

});
