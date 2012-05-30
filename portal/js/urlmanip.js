var query_params = {};
(function () {
    var e,
        a = /\+/g,
        r = /([^&=]+)=?([^&]*)/g,
        d = function (s) {return decodeURIComponent(s.replace(a, " "));},
        q = window.location.search.substring(1);

    while (e = r.exec(q))
        query_params[d(e[1])] = d(e[2]);
})();

if (!Storage.prototype.setObject) {
    Storage.prototype.setObject = function(key, value) {
        this.setItem(key, JSON.stringify(value));
    };
}

//if all of our vars are in the query string, grab them and save them
if (query_params.access_token && query_params.admin_email && query_params.uuid) {
    localStorage.setObject('usergrid_access_token', query_params.access_token);
    var user = {uuid:query_params.uuid, email:query_params.admin_email}
    localStorage.setObject('usergrid_user', user);
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
    if (query_params.callback) {
        new_target = new_target + separatorMark + 'callback=' + query_params.callback;
        separatorMark = '&';
    }
    window.location = window.location.protocol+'//'+new_target;
    throw "stop!";
}