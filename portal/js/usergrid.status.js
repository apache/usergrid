/**
 * User: David S
 * Date: 17/02/12
 * Time: 12:43 PM
 */
var StatusBar = function () {
    var self = {
        box: null
    };

    self.Init = function(boxSelector){
        self.box = $(boxSelector);
    };

    self.showAlert = function (msg, type) {
      if(!type) {
          type = 'info';
          var item = $('<div class="alert alert-' + type + ' ">' + msg + '</div>');
          self.box.find(".alert").remove();
          self.box.show().prepend(item);
          item.show();
          var t = setTimeout(removeOldestAlert, 4000);	
      } else if (type = 'error') {
          var closebutton = '<div style="float: right;"><a href="#" onclick="closeErrorMessage();" style="color: #B94A48;">close</a></div>'
          var item = $('<div class="alert alert-' + type + ' ">' + msg + closebutton + '</div>');
          self.box.find(".alert").remove();
          self.box.show().prepend(item);
          item.show();
      } else {
          var item = $('<div class="alert alert-' + type + ' ">' + msg + '</div>');
          self.box.find(".alert").remove();
          self.box.show().prepend(item);
          item.show();
          var t = setTimeout(removeOldestAlert, 4000);
      };
    };

    closeErrorMessage = function() {
      self.box.hide();
    };

    var removeOldestAlert = function () {
      if (self.box.find(":first-child").hasClass("alert-info")) {
        var oldItem = self.box;
        oldItem.hide();
      }

      if (self.box.children().length == 0) {
          self.box.hide();
      }
    };

    return self;
}();
