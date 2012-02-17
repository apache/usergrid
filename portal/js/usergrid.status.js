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
        if(!type)
            type = 'info';
        var item = $('<div class="alert alert-' + type + ' ">' + msg + '</div>');
        self.box.show().prepend(item);
        item.show();
        var t = setTimeout(removeOldesAlert, 4000);
    };

    var removeOldesAlert = function () {
        var oldItem = self.box.find(":last-child");
        oldItem.remove();
        if (self.box.children().length == 0) {
            self.box.hide();
        }
    };

    return self;
}();


