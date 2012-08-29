(function (){
  Usergrid.Params = function(){};

  Usergrid.Params.default = {
    queryParams : {}
  };

  Usergrid.Params.prototype = {
    parseParams : function(){
      if (window.location.search) {
        // split up the query string and store in an associative array
        var params = window.location.search.slice(1).split("&");
        for (var i = 0; i < params.length; i++) {
          var tmp = params[i].split("=");
          this.default.queryParams[tmp[0]] = unescape(tmp[1]);
        }
      }
    }
  };
})(Usergrid);