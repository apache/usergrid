/*
  *Usergrid.Navigation
  *
  * Functions to control the navigation of the Console client
  *
  * Uses: jQuery.Address
  *
  * Requires:
  *
  */

(function ($) {
   Usergrid.Navigation = function() {
    var subscribers = {
      },
      pathMap = {
        organization: 0,
        panel: 1
      }

    $.address.change(function(event) {
      subscribers.each(function() {
        if(event.pathNames[pathMap.panel]==this.pathName){

        }
      })
    });

    return {
      initialize: function( ){

      },
      Subscribe: function( ){

      }
    }
  }
  Usergrid.Navigation.Subscriber = function() {

  }

})(jQuery);

Usergrid.Navigation = new Usergrid.Navigation();