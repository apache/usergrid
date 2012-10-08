function ApigeePages() {
  var self = {
    pages: {},
    panels: {},
    resetPasswordUrl: ''
  };

  self.clearPage = function(){
    $("#pages > div").hide();
  };

  self.ShowPage = function(pageName){
    // console.log('showing ' + pageName);
    $("#pages > div").hide();
    var page = self.pages[pageName];
    page.box.show();
    $(".navbar li.active").removeClass('active');
    $(".navbar .navbar-inner").hide();

    if(page.link.parent().parent().hasClass("dropdown-menu")) {
      page.link.parent().parent().parent().addClass('active');
    } else {
      page.link.parent().addClass('active');
      page.menu.show();
    }

    if(page.showFunction) {
      page.showFunction();
    }

    if(Usergrid.userSession.getBannerState() == 'true'){
      this.showBanner();
    }
  };
  self.showBanner = function(){
    Usergrid.userSession.showBanner();
    $('#banner').show();
  };
  
  self.hideBanner = function(){
    Usergrid.userSession.hideBanner();
    $("#banner").hide();
  };

  self.AddPage = function(page) {
    if(!page.link)
      page.link = $("#" + page.name + '-link');

    if(!page.box)
      page.box = $("#" + page.name + '-page');

    page.link.click(function(e) {
      e.preventDefault();
      if(!page.link.hasClass("dropdown-toggle"))
        self.ShowPage(page.name);
    });

    LoadPage(page);
    self.pages[page.name] = page;
  };

  self.AddPanel = function(panelName, linkSelector,boxSelector,initFunction,showFunction) {
    if (!linkSelector) {
      linkSelector = "#sidebar-menu a[href='#" + panelName + "']";
    }

    if (!boxSelector) {
      boxSelector = "#" + panelName + '-panel';
    }

    var panel = {
      name: panelName,
      link: $(linkSelector),
      box: $(boxSelector),
      initFunction: initFunction,
      showFunction: showFunction
    };

    if (panel.initFunction) {
      panel.initFunction();
    }

    panel.link.click(function(e) {
      e.preventDefault();
      self.SelectPanel(panel.name);
    });

    self.panels[panel.name] = panel;
  };

  self.ActivatePanel = function(panelName){
    var panel = self.panels[panelName];
    $("#sidebar-menu li.active").removeClass('active');
    panel.link.parent().addClass('active');
  }

  self.SelectPanel = function (panelName){
    var panel = self.panels[panelName];

    $("#sidebar-menu li.active").removeClass('active');
    panel.link.parent().addClass('active');

    if (panel.showFunction) {
      panel.showFunction();
    }

    if (panelName == 'console') {
      url = Usergrid.console.getAccessTokenURL();
      $("#console-panel iframe").attr("src", url);
    }

    $("#console-panels > div").hide();
    panel.box.show();

  };

  function LoadPage(page){

    if (page.name=='forgot-password') {
      $("#forgot-password-page iframe").attr("src", self.resetPasswordUrl);
    } else if(page.name=='console-frame') {
      $("#console-frame-page iframe").attr("src", "consoleFrame.html");
    } else {
      if (window.location.pathname.indexOf('app') > 0) {
        $.ajaxSetup ({cache: false});
        page.box.load(page.name + '.html',page.initFunction);
        $.ajaxSetup ({cache: true});
      } else if(page.initFunction) {
        page.initFunction();
      }
    }
  }
  return self;
}
