function getQueryParams() {
  var query_params = {};
  if (window.location.search) {
    // split up the query string and store in an associative array
    var params = window.location.search.slice(1).split("&");
    for (var i = 0; i < params.length; i++) {
      var tmp = params[i].split("=");
      query_params[tmp[0]] = unescape(tmp[1]);
    }
  }

  return query_params;
}

function prepareLocalStorage() {
  if (!Storage.prototype.setObject) {
    Storage.prototype.setObject = function(key, value) {
      this.setItem(key, JSON.stringify(value));
    };
  }

  if (!Storage.prototype.getObject) {
    Storage.prototype.getObject = function(key) {
      try {
        return this.getItem(key) && JSON.parse(this.getItem(key));
      } catch(err) {
      }
      return null;
    };
  }
}

//if all of our vars are in the query string, grab them and save them
function parseParams() {
  if (query_params.access_token && query_params.admin_email && query_params.uuid) {

    //then send the user to the parent
    var new_target = window.location.host + window.location.pathname;

    var separatorMark = '?';
    if (query_params.api_url) {
      new_target = new_target + separatorMark + 'api_url=' + query_params.api_url;
      separatorMark = '&';
    }

    if (query_params.use_sso) {
      new_target = new_target + separatorMark + 'use_sso=' + query_params.use_sso;
      separatorMark = '&';
    }

    window.location = window.location.protocol + '//' + new_target;
    throw "stop!";
  }
}

