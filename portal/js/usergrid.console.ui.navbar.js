(function($) {

  $.widget("ui.usergrid_console_navbar", {
    options : {},

    _create : function() {

      var self = this;
      var o = self.options;
      var el = self.element;

      var html = "<table><tr><td class=\"console-panel-nav-bar-breadcrumbs\"></td><td class=\"console-panel-nav-bar-tabs\"></td></tr></table>";
      el.html(html);
      el.addClass("console-panel-nav-bar");
      el.addClass("ui-widget-header");

      o.breadCrumbsTd = el.find(".console-panel-nav-bar-breadcrumbs");
      o.tabsTd = el.find(".console-panel-nav-bar-tabs");
    },

    _init : function() {
      var self = this;
      var o = self.options;
      var el = self.element;

      var html = "";
      if (o.crumbs) {
        for(var i = 0; i < o.crumbs.length; i++) {
          var crumb = o.crumbs[i];
          if (i != 0) html += "&nbsp;&nbsp;<span class=\"shrink-font\">&#9654;</span>&nbsp;&nbsp;";
          html += "<a href=\"#\">" + crumb.title + "</a>";
        }
      }
      o.breadCrumbsTd.html(html);
      var i = 0;
      o.breadCrumbsTd.children().each(
        function(){
          var crumb = o.crumbs[i];
          if (crumb.click) {
            $(this).click(crumb.click);
          }
        }
      );

      html = "";
      if (o.tabs) {
        for(var i = 0; i < o.tabs.length; i++) {
          var tab = o.tabs[i];
          html += "<button class=\"button\">" + tab.title + "</button>";
        }
      }
      o.tabsTd.html(html);
      i = 0;
      o.tabsTd.children().each(
        function(){
          if (i == 0) {
            $(this).addClass("tab-bar-selection");
          }
          $(this).button();
          var tab = o.tabs[i];
          if (tab.click) {
            $(this).click(tab.click);
          }
          i++;
        }
      );


    },

    destroy : function() {
      //this.element.next().remove();
    },

    _setOption : function(option, value) {
      $.Widget.prototype._setOption.apply(this, arguments);

      var el = this.element;

      switch (option) {
      case "location":
        break;
      }
    }
  });
})(jQuery);
