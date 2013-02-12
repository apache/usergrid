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
    if (!type) {
      type = 'info';
    }

    var closebutton = '<a onclick="closeErrorMessage();" class="close">&times;</a>'
    var item = $('<div class="alert span3 alert-' + type + ' ">' + msg + closebutton + '</div>');
    self.box.find(".alert").remove();
    self.box.show().prepend(item);
    item.show();

  };

  closeErrorMessage = function() {
    self.box.hide();
  };

  return self;
}();
