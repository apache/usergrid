/**
 * Created by ryan bridges on 2014-02-05.
 */

    //noinspection ThisExpressionReferencesGlobalObjectJS
(function (global) {
    var name = 'Module',
        short = '_m',
        _name = global[name],
        _short = (short !== undefined) ? global[short] : undefined;

    function Module() {
        /* put code in here */

    }

    global[name] = Module;
    if (short !== undefined) {
        global[short] = Module;
    }
    global[name].global[name].noConflict = function () {
        if (_name) {
            global[name] = _name;
        }
        if (short !== undefined) {
            global[short] = _short;
        }
        return Module;
    };
    if( typeof module !== "undefined" && ('exports' in module)){
        module.exports	= global[name]
    }
    return global[name];
}(this));
