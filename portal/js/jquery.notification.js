/**
 * Copyright 2010 (c) akquinet.
 *
 * jQuery notification plugin is based on code of jquery.toastmessage, 
 * modified by Paul Bagwell <pbagwl.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * http://www.apache.org/licenses/LICENSE-2.0
 */
(function($) {
  var Notification = function(message, type, options) {
    this.show($.extend({text: message, type: type}, options));
  };

  $.extend(Notification.prototype, {
    settings: {
        inEffect: {opacity: 'show'}, // in effect
        inEffectDuration: 600, // in effect duration in ms
        stayTime: 3000, // time in miliseconds before the item has to disappear
        text: '',    // content of the item
        sticky: false, // should the notification item be sticky or not?
        type: 'notice', // notice, warning, error, success
        position: 'top-right', // top-right, center, middle-bottom etc
        close: null // callback function when the message is closed
    },

    html: '\
<div class="notification-wrapper">\
    <div class="notification">\
        <div class="notification-image"></div>\
        <div class="notification-close"></div>\
        <p></p>\
    </div>\
</div>\
',

    show: function(options) {
        var self = this,
            settings = $.extend(this.settings, options),
            ntfs = $(this.html).find('.notification');
        // global container for all notifications
        if (!$('.notifications').length) {
            $('<div class="notifications ' + settings.position + '"/>')
                .appendTo('body');
        }
        ntfs.parent().addClass(settings.type).appendTo('.notifications');
        ntfs.find('p').html(settings.text);
        ntfs.hide()
            .animate(settings.inEffect, settings.inEffectDuration);

        ntfs.find('.notification-close').click(function() {
            self.remove(ntfs, settings);
        });

        if (!settings.sticky) {
            setTimeout(function() {
                self.remove(ntfs, settings);
            },
            settings.stayTime);
        }
    },

    remove: function(obj, options) {
        obj.animate({opacity: '0'}, 600, function() {
            obj.parent().animate({height: '0px'}, 300, function() {
                obj.parent().remove();
            });
        });
        // callback
        if (options && options.close !== null) {
            options.close();
        }
    }
  });
});
