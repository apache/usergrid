/**
* jQuery UI Statusbar 15-05-2010
*
* Copyright (c) 2010 Tjerk Valentijn
* Licensed under the MIT license
*
* url: http://code.google.com/p/jquery-ui-widgets-tjercus/
*
* Depends:
*   jquery 1.4.2
*   jquery ui 1.8.1
*   jquery.ui.widget.js
*/
$.widget("ui.statusbar", {
    options: {
        hideAfter: 10,
        hidden: true
    },
    _create: function() {
        // method is called once at creation (attachment to DOM node)
        this.element.html("<ul></ul>");
        this.element.addClass("ui-statusbar");        
        if (this.options.hidden) {
            this.element.hide();
        } else {
            this.element.show();
        }
        this._init();
    },
    _init: function() {
        // called each time .statusbar(etc.) is called
    },
    add: function(text, _hideAfter, type) {
        var status_bar_class = "ui-state-default";
        var icon_class = "ui-icon-info";
        if (type == "alert") {
            status_bar_class = "ui-state-highlight";
            icon_class = "ui-icon-notice";
        }
        else if (type == "error") {
            status_bar_class = "ui-state-error";
            icon_class = "ui-icon-alert";
        }
        // public via $("#myel").statusbar("add", options);
        var imgHtml = "<span class='ui-icon " + icon_class + "' style='float:left;margin-right:0.3em;'></span> ";
        var li = $("<li class='" + status_bar_class + "'>" + imgHtml + text + "</li>");
        this.element.find("ul").prepend(li);
        this._trigger("messageadd", {}, text);
        this.element.show();
        
        var self = this; // scope correction for inner function
        var seconds = (_hideAfter || self.options.hideAfter) * 1000;
        setTimeout(function() {
            li.fadeOut("slow", function() {
                li.remove();
                self._trigger("messageremove", {}, text);
            });
        }, seconds);
    },
    destroy: function() {
        $.Widget.prototype.destroy.apply(this, arguments); // default destroy
        this.element.empty();
    }
});
