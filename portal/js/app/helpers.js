/*
 * This file contains helper functions: Stuff used all over the application but not necessarily part of a module/object/paradigm/something.
 *
 * If your can't find where a function is defined, it's probably here. :)
 *
 * They need to be cleaned up. SOON!
 *
 */

var onIE = navigator.userAgent.indexOf("MSIE") >= 0;

function indexOfFirstType(type, args) {
  for (var i = 0; i < args.length; i++) {
    if (!args[i]) return - 1;
    if (typeof args[i] == type) return i;
  }
  return - 1;
}

function getByType(type, i, args) {
  var j = indexOfFirstType(type, args);
  if (j < 0) return null;
  var k = 0;
  while ((j < args.length) && (k <= i)) {
    if (type == "object") {
      if (args[j].constructor != Object) return null;
    } else if (typeof args[j] != type) return null;
    if (k == i) return args[j];
    j++;
    k++;
  }
  return null;
}

function countByType(type, args) {
  var c = 0;
  var j = indexOfFirstType(type, args);
  if (j < 0) return c;
  while (j < args.length) {
    if (type == "object") {
      if (args[j].constructor != Object) return c;
    } else if (typeof args[j] != type) return c;
    j++;
    c++;
  }
  return null;
}

function encodeParams(params) {
  tail = [];
  if (params instanceof Array) {
    for (i in params) {
      var item = params[i];
      if ((item instanceof Array) && (item.length > 1)) {
        tail.push(item[0] + "=" + encodeURIComponent(item[1]));
      }
    }
  } else {
    for (var key in params) {
      if (params.hasOwnProperty(key)) {
        var value = params[key];
        if (value instanceof Array) {
          for (i in value) {
            var item = value[i];
            tail.push(key + "=" + encodeURIComponent(item));
          }
        } else {
          tail.push(key + "=" + encodeURIComponent(value));
        }
      }
    }
  }
  return tail.join("&");
}

function encodePathString(path, returnParams) {

  var i = 0;
  var segments = new Array();
  var payload = null;
  while (i < path.length) {
    var c = path.charAt(i);
    if (c == '{') {
      var bracket_start = i;
      i++;
      var bracket_count = 1;
      while ((i < path.length) && (bracket_count > 0)) {
        c = path.charAt(i);
        if (c == '{') {
          bracket_count++;
        } else if (c == '}') {
          bracket_count--;
        }
        i++;
      }
      if (i > bracket_start) {
        var segment = path.substring(bracket_start, i);
        segments.push(JSON.parse(segment));
      }
      continue;
    } else if (c == '/') {
      i++;
      var segment_start = i;
      while (i < path.length) {
        c = path.charAt(i);
        if ((c == ' ') || (c == '/') || (c == '{')) {
          break;
        }
        i++;
      }
      if (i > segment_start) {
        var segment = path.substring(segment_start, i);
        segments.push(segment);
      }
      continue;
    } else if (c == ' ') {
      i++;
      var payload_start = i;
      while (i < path.length) {
        c = path.charAt(i);
        i++;
      }
      if (i > payload_start) {
        var json = path.substring(payload_start, i).trim();
        payload = JSON.parse(json);
      }
      break;
    }
    i++;
  }

  var newPath = "";
  for (i = 0; i < segments.length; i++) {
    var segment = segments[i];
    if (typeof segment === "string") {
      newPath += "/" + segment;
    } else {
      if (i == (segments.length - 1)) {
        if (returnParams) {
          return {path : newPath, params: segment, payload: payload};
        }
        newPath += "?";
      } else {
        newPath += ";";
      }
      newPath += encodeParams(segment);
    }
  }
  if (returnParams) {
    return {path : newPath, params: null, payload: payload};
  }
  return newPath;
}

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

// if all of our vars are in the query string, grab them and save them
function parseParams() {
  var query_params = {};
  if (window.location.search) {
    // split up the query string and store in an associative array
    var params = window.location.search.slice(1).split("&");
    for (var i = 0; i < params.length; i++) {
      var tmp = params[i].split("=");
      query_params[tmp[0]] = unescape(tmp[1]);
    }
  }

  if (query_params.access_token && query_params.admin_email && query_params.uuid) {
    localStorage.setItem('accessToken', query_params.access_token);
    var user = {uuid:query_params.uuid, admin_email:query_params.admin_email}
    var user = {uuid:query_params.uuid, email:query_params.admin_email}
    localStorage.setObject('usergridUser', user);

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

function dateToString(numberDate){
  var date = new Date(numberDate);
  return date.toString('dd MMM yyyy - h:mm tt ');
}

/* move toggleablesections to console? */
function toggleableSections() {
  $(document).on('click', '.title', function() {
    $(this).parent().parent().find('.hideable').toggle();
  })
}

function selectFirstElement(object) {
  var first = null;
  for (first in object) {
    break
  }
  return first
}
