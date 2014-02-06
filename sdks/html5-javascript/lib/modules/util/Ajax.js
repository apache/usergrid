//Ajax
(function() {
    var name = 'Ajax', global = this, overwrittenName = global[name], exports;

    Function.prototype.partial = function() {
        var fn = this,b = [].slice.call(arguments);
        return fn.bind(undefined, b);
    }

    exports = (function() {
        function Ajax() {
            this.logger=new global.Logger(name);
            var self=this;
            function encode(data) {
                var result = "";
                if (typeof data === "string") {
                    result = data;
                } else {
                    var e = encodeURIComponent;
                    for (var i in data) {
                        if (data.hasOwnProperty(i)) {
                            result += '&' + e(i) + '=' + e(data[i]);
                        }
                    }
                }
                return result;
            }
            var request = function(m, u, d) {
                var p = new Promise(), timeout;
                self.logger.time(m + ' ' + u);
                self.logger.timeEnd(m + ' ' + u);
                (function(xhr) {
                    xhr.onreadystatechange = function() {
                        this.readyState ^ 4 || (self.logger.timeEnd(m + ' ' + u), clearTimeout(timeout), p.done(null, this));
                    };
                    xhr.onerror=function(response){
                        clearTimeout(timeout);
                        p.done(response, null);
                    }
                    xhr.oncomplete=function(response){
                        clearTimeout(timeout);
                        self.info("%s request to %s returned %s", m, u, this.status );
                    }
                    xhr.open(m, u);
                    if (d) {
                        xhr.setRequestHeader("Content-Type", "application/json");
                        xhr.setRequestHeader("Accept", "application/json");
                    }
                    timeout = setTimeout(function() {
                        xhr.abort();
                        p.done("API Call timed out.", null)
                    }, 30000);
                    //TODO stick that timeout in a config variable
                    xhr.send(encode(d));
                }(new XMLHttpRequest()));
                return p;
            };
            this.request=request;
            this.get = request.partial('GET');
            this.post = request.partial('POST');
            this.put = request.partial('PUT');
            this.delete = request.partial('DELETE');
        }
        return new Ajax();
    }());
    global[name] =  exports;
    global[name].noConflict = function() {
        if(overwrittenName){
            global[name] = overwrittenName;
        }
        return exports;
    };
    return global[name];
}());
