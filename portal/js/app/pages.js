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

  self.AddPanel = function(panelName, linkSelector, sublinkSelector,boxSelector,initFunction,showFunction, buttonHandler) {
    if (!linkSelector) {
      //linkSelector = "#sidebar-menu a[href='#" + panelName + "']";
      linkSelector = "#" + panelName  + '-link';
    }
    if (!sublinkSelector) {
      sublinkSelector = "#" + panelName  + '-sublink';
    }

    if (!boxSelector) {
      boxSelector = "#" + panelName + '-panel';
    }

    var panel = {
      name: panelName,
      link: $(linkSelector),
      sublink: $(sublinkSelector),
      box: $(boxSelector),
      initFunction: initFunction,
      showFunction: showFunction
    };

    if(!buttonHandler) {
      buttonHandler = function(e) {
        e.preventDefault();
        redrawBox(panel.box);
        if(panel.name == "query") {
          Usergrid.Navigation.router.navigateTo("collections");
        } else {
          Usergrid.Navigation.router.navigateTo(panel.name);
        }
      }
    }
    panel.link.click(buttonHandler);
    panel.sublink.click(buttonHandler);

    self.panels[panel.name] = panel;

    if (panel.initFunction) {
      panel.initFunction();
    }
  };

  self.ActivatePanel = function(panelName){
    var panel = self.panels[panelName];
    $("#sidebar-menu li.active").removeClass('active');
    $("#"+panelName+"-link").parent().addClass('active');

    $("#left-notifications-menu li.active").removeClass('active');

  }

  self.SelectPanel = function (panelName){
    var panel = self.panels[panelName];

    $("#sidebar-menu li.active").removeClass('active');
    $("#sidebar-menu2 li.active").removeClass('active');
    panel.link.parent().addClass('active');
    panel.sublink.parent().addClass('active');

    Usergrid.console.setupMenu();
    Usergrid.console.requestApplications();
    if (panel.showFunction) {
      panel.showFunction();
    }

    redrawBox(panel.box);

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

  function redrawBox(box) {
    $("#console-panels > div").hide();
    box.show();

  }
  return self;
}
