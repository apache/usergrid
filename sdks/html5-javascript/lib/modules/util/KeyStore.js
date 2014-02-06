//KeyStore
var KeyStore = (function(global) {
    global.indexedDB = global.indexedDB || global.mozIndexedDB || global.webkitIndexedDB || global.msIndexedDB;
    // (Mozilla has never prefixed these objects, so we don't need global.mozIDB*)
    global.IDBTransaction = global.IDBTransaction || global.webkitIDBTransaction || global.msIDBTransaction;
    global.IDBKeyRange = global.IDBKeyRange || global.webkitIDBKeyRange || global.msIDBKeyRange;

    var store="data",
        keyPath="key",
        database="test_keystore",
        version= 8,
        logger=new Logger('KeyStore');
    var db, objectStore;
    function createObjectStore(db, name, keyPath){
        var p = new Promise(), objectStore;
        try {
            objectStore = event.currentTarget.transaction.objectStore(store);
            //console.log(objectStore);
            if (!objectStore) throw "not found";
        } catch (e) {
            logger.warn("creating new objectStore",e);
            objectStore = db.createObjectStore(name, {
                keyPath: keyPath
            });
        } finally {
            objectStore.transaction.oncomplete = function(event) {
                // console.info("created ObjectStore: '%s'", store);
                p.done(null, objectStore);
            };
        }
        return p;
    }
    function createObjectStoreIndex(objectStore, keyPath){
        var p = new Promise(), index;
        try {
            index = objectStore.index(keyPath);
            if (!index) throw "not found";
        } catch (e) {
            objectStore.createIndex(keyPath, keyPath, {
                unique: true
            });
        } finally {
            // objectStore.transaction.oncomplete = function(event) {
            // console.info("created ObjectStore index: '%s'", keyPath);
            p.done(null, db);
            // };
        }
        return p;
    }
    function openDatabase(){
        var p = new Promise();
        var request = indexedDB.open(database, version);
        request.onerror = function(event) {
            console.error("internal keystore error: " + event.target.errorCode);
            p.done(event.target, null);
        };
        request.onupgradeneeded = function(event) {
            console.warn("upgrading internal keystore");
            Promise.chain([
                function(err, db){return createObjectStore(event.target.result, store, keyPath)},
                function(err, objectStore){return createObjectStoreIndex(objectStore, keyPath)}
            ])
        };
        request.onsuccess = function(event) {
            //console.info("successfully opened database %s", event.target.result);
            p.done(null, event.target.result);
        };
        return p;
    }

    function get(key) {
        var p=new Promise();
        openDatabase().then(function(err, db){
            var item = db.transaction(store, "readwrite").objectStore(store).index(keyPath).get(key);
            item.onsuccess = function(event) {
                // console.log(event.target.result);
                p.done(null, event.target.result);//request.result
            };
            item.onerror = function(event) {
                p.done(event.target.error, null);
            };
        })
        return p;
    }
    function save(key, value) {
        var p=new Promise();
        openDatabase().then(function(err, db){
            var _store=db.transaction(store, "readwrite").objectStore(store);
            var item = _store.index(keyPath).get(key);
            item.onsuccess = function(event) {
                //update
                _store.put({key:key,value:value}).onsuccess=function(){
                    console.log("put");
                    p.done(null, event.target.result);//request.result
                }
            };
            item.onerror = function(event) {
                _store.post({key:key,value:value}).onsuccess=function(){
                    console.log("post");
                    p.done(event.target.error, null);//request.result
                }
                //p.done(event.target.error, event.target.result);
            };
        })
        return p;
    }
    function remove(key) {
        var p=new Promise();
        openDatabase().then(function(err, db){
            var item = db.transaction(store, "readwrite").objectStore(store).delete(key);
            item.onsuccess = function(event) {
                p.done(null, event.target.result);//request.result
            };
            item.onerror = function(event) {
                p.done(event.target.error, event.target.result);
            };
        });
        return p;
    }
    function post(key, value) {
        var p=new Promise();
        openDatabase().then(function(err, db){
            var item = db.transaction(store, "readwrite").objectStore(store).add({key:key, value:value});
            item.onsuccess = function(event) {
                p.done(null, event.target.result);//request.result
            };
            item.onerror = function(event) {
                p.done(event.target.error, event.target.result);
            };
        })
        return p;
    }
    function put(key, value) {
        var p=new Promise();
        var item = db.transaction(store, "readwrite").objectStore(store).put(key, value);
        item.onsuccess = function(event) {
            p.done(null, event.target.result);//request.result
        };
        item.onerror = function(event) {
            p.done(event.target.error, event.target.result);
        };
        return p;
    }
    /*
     *
     * {databases:[{
     *      name:'name'
     *     version:1,
     *      'stores':[
     *          {
     *             name:'name',
     *             options:{}
     *          }
     *      ]
     *
     * }]
     *
     */
    function KeyStoreFactory(options){
        this.data=options;
        var self=this;
        self.data.databases.forEach(function(database){
            console.log(database);
            //make DB request;
        });
    }
    KeyStoreFactory.prototype=new UsergridEvent();
    KeyStoreFactory.prototype.openDatabase=function(database){
        var self=this, p=new Promise();
        var request = indexedDB.open(database, version);
        request.onerror = function(event) {
            console.error("internal keystore error: " + event.target.errorCode);
            p.done(event.target, null);
        };
        request.onupgradeneeded = function(event) {
            console.warn("upgrading internal keystore");
            throw new UsergridKeystoreDatabaseUpgradeNeededError("internal keystore requires upgrade");
            /*Promise.chain([
                function(err, db){return createObjectStore(event.target.result, store, keyPath)},
                function(err, objectStore){return createObjectStoreIndex(objectStore, keyPath)}
            ])*/
        };
        request.onsuccess = function(event) {
            //console.info("successfully opened database %s", event.target.result);
            p.done(null, event.target.result);
        };
        return p;
    }
    global['KeyStoreFactory']=KeyStoreFactory;
    global['KeyStore']={
        openDatabase:openDatabase,
        get:get,
        put:put,
        post:post,
        save:save,
        remove:remove
    };
    return global['KeyStore'];
}(this));
