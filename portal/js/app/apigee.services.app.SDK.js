/**
 *  App SDK is a collection of classes designed to make working with
 *  the Appigee App Services API as easy as possible.
 *  Learn more at http://apigee.com/docs
 *
 *  apigee.services.QueryObj is a class for holding all query information and paging state
 *
 *  The goal of the query object is to make it easy to run any
 *  kind of CRUD call against the API.  This is done as follows:
 *
 *  1. Create a query object:
 *     queryObj = new apigee.QueryObj("GET", "users", null, function() { alert("success"); }, function() { alert("failure"); });
 *
 *  2. Run the query by calling the appropriate endpoint call
 *     runAppQuery(queryObj);
 *     or
 *     runManagementQuery(queryObj);
 *
 *  3. Paging - The apigee.QueryObj holds the cursor information.  To
 *     use, simply bind click events to functions that call the
 *     getNext and getPrevious methods of the query object.  This
 *     will set the cursor correctly, and the runAppQuery method
 *     can be called again using the same apigee.QueryObj:
 *     runAppQuery(queryObj);
 *
 *  @class apigee.QueryObj
 *  @param method REQUIRED - GET, POST, PUT, DELETE
 *  @param path REQUIRED - API resource (e.g. "users" or "users/rod", should not include http URL or org_name/app_name)
 *  @param jsonObj NULLABLE - a json data object to be passed to the API
 *  @param params NULLABLE - query parameters to be encoded and added to the API URL
 *  @param successCallback REQUIRED - the success callback function
 *  @param failureCallback REQUIRED - the failure callback function
 */

window.apigee = window.apigee || {};
apigee = apigee || {};

(function () {

  apigee.QueryObj = function(method, path, jsonObj, params, successCallback, failureCallback) {
    //query vars
    this._method = method;
    this._path = path;
    this._jsonObj = jsonObj;
    this._params = params;
    this._successCallback = successCallback;
    this._failureCallback = failureCallback;

    //curl command - will be populated by runQuery function
    this._curl = '';
    this._token = false;

    //paging vars
    this._cursor = null;
    this._next = null
    this._previous = [];
  };

  apigee.QueryObj.prototype = {
    getMethod: function() { 
      return this._method;
    },
    setMethod: function(method) { 
      this._method = method;
    },
    getPath: function() { 
      return this._path;
    },
    setPath: function(path) { 
      this._path = path;
    },
    getJsonObj: function() { 
      return this._jsonObj;
    },
    setJsonObj: function(jsonObj) { 
      this._jsonObj = jsonObj;
    },
    getParams: function() { 
      return this._params;
    },
    setParams: function(params) { 
      this._params = params;
    },
    getSuccessCallback: function() { 
      return this._successCallback;
    },
    setSuccessCallback: function(successCallback) { 
      this._successCallback = successCallback;
    },
    callSuccessCallback: function(response) { 
      this._successCallback(response);
    },
    getFailureCallback: function() { 
      return this._failureCallback;
    },
    setFailureCallback: function(failureCallback) { 
      this._failureCallback = failureCallback;
    },
    callFailureCallback: function(response) { 
      this._failureCallback(response);
    },

    getCurl: function() { 
      return this._curl;
    },
    setCurl: function(curl) { 
      this._curl = curl;
    },

    getToken: function() { 
      return this._token;
    },
    setToken: function(token) { 
      this._token = token;
    },

    //methods for accessing paging functions
    resetPaging: function() {
      this._previous = [];
      this._next = null;
      this._cursor = null;
    },

    hasPrevious: function() {
      return (this._previous.length > 0);
    },

    getPrevious: function() {
      this._next=null; //clear out next so the comparison will find the next item
      this._cursor = this._previous.pop();
    },

    hasNext: function(){
      return (this._next);
    },

    getNext: function() {
      this._previous.push(this._cursor);
      this._cursor = this._next;
    },

    saveCursor: function(cursor) {
      this._cursor = this._next; //what was new is old again
      //if current cursor is different, grab it for next cursor
      if (this._next != cursor) {
        this._next = cursor;
      } else {
        this._next = null;
      }
    },

    getCursor: function() {
      return this._cursor;
    }
  };



  /**
  * APIClient class is charged with making calls to the API endpoint
  *
  * @class APIClient
  * @constructor
  */
  apigee.APIClient = function (orgName, appName) {
    //API endpoint
    this._apiUrl = "https://api.usergrid.com";
    this._orgName = orgName;
    this._orgUUID = null;
    this._appName = appName;
    this._token = null;
    var clientId = null; //to be implemented
    var clientSecret = null; //to be implemented
  };
  /*
  *  method to set the organization name to be used by the client
  *  @method getOrganizationName
  *  @method setOrganizationName
  */
  apigee.APIClient.prototype = {

    getOrganizationName: function() { return this._orgName; },
    setOrganizationName: function(orgName) { this._orgName = orgName; },
    getOrganizationUUID: function() { return this._orgUUID; },
    setOrganizationUUID: function(orgUUID) { this._orgUUID = orgUUID; },

    /*
    *  method to set the application name to be used by the client
    *  @method getApplicationName
    *  @method setApplicationName
    */
    getApplicationName: function() { return this._appName; },
    setApplicationName: function(appName) {
      this._appName = appName;
    },

    /*
    *  method to set the token to be used by the client
    *  @method getToken
    *  @method setToken
    */
    getToken: function() { return this._token; },
    setToken: function(token) { this._token = token; },

    /*
    *  allows API URL to be overridden
    *  @method setApiUrl
    */
    setApiUrl: function(apiUrl) { this._apiUrl = apiUrl; },
    /*
    *  returns API URL
    *  @method getApiUrl
    */
    getApiUrl: function() { return this._apiUrl },

    /*
    *  returns the api url of the reset pasword endpoint
    *  @method getResetPasswordUrl
    */
    getResetPasswordUrl: function() { this.getApiUrl() + "/management/users/resetpw" },

    /*
    *  public function to run calls against the app endpoint
    *  @method runAppQuery
    *  @params {object} apigee.QueryObj - {method, path, jsonObj, params, successCallback, failureCallback}
    *
    */
    runAppQuery: function(QueryObj) {
      var endpoint = "/" + this.getOrganizationName() + "/" + this.getApplicationName() + "/";
      this.processQuery(QueryObj, endpoint);
    },

    /*
    *  public function to run calls against the management endpoint
    *  @method runManagementQuery
    *  @params {object} apigee.QueryObj - {method, path, jsonObj, params, successCallback, failureCallback}
    *
    */
    runManagementQuery: function(QueryObj) {
      var endpoint = "/management/";
      this.processQuery(QueryObj, endpoint)
    },

    /*
    *  @method processQuery
    *  @purpose to validate and prepare a call to the API
    *  @params {object} apigee.QueryObj - {method, path, jsonObj, params, successCallback, failureCallback}
    *
    */
    processQuery: function(QueryObj, endpoint) {
      var curl = "curl";
      //validate parameters
      try {
        var method = QueryObj.getMethod().toUpperCase();
        var path = QueryObj.getPath();
        var jsonObj = QueryObj.getJsonObj() || {};
        var params = QueryObj.getParams() || {};

        //method - should be GET, POST, PUT, or DELETE only
        if (method != 'GET' && method != 'POST' && method != 'PUT' && method != 'DELETE') {
          throw(new Error('Invalid method - should be GET, POST, PUT, or DELETE.'));
        }
        //curl - add the method to the command (no need to add anything for GET)
        if (method == "POST") {curl += " -X POST"; }
        else if (method == "PUT") { curl += " -X PUT"; }
        else if (method == "DELETE") { curl += " -X DELETE"; }
        else { curl += " -X GET"; }

        //curl - append the bearer token if this is not the sandbox app
        var application_name = this.getApplicationName();
        if (application_name) {
          application_name = application_name.toUpperCase();
        }
        if (application_name != 'SANDBOX' && this.getToken()) {
          curl += ' -i -H "Authorization: Bearer ' + this.getToken() + '"';
          QueryObj.setToken(true);
        }

        //params - make sure we have a valid json object
        _params = JSON.stringify(params)
        if (!jsonlint.parse(_params)) {
          throw(new Error('Params object is not valid.'));
        }

        //add in the cursor if one is available
        if (QueryObj.getCursor()) {
          params.cursor = QueryObj.getCursor();
        } else {
          delete params.cursor;
        }

        //add the endpoint to the path
        path = endpoint + path;

        //make sure path never has more than one / together
        if (path) {
          //regex to strip multiple slashes
          while(path.indexOf('//') != -1){
            path = path.replace('//', '/');
          }
        }

        //curl - append the path
        curl += " " + this.getApiUrl() + path;

        //curl - append params to the path for curl prior to adding the timestamp
        var encoded_params = this.encodeParams(params);
        if (encoded_params) {
          curl += "?" + encoded_params;
        }

        //add in a timestamp for gets and deletes - to avoid caching by the browser
        if ((method == "GET") || (method == "DELETE")) {
          params['_'] = new Date().getTime();
        }

        //append params to the path
        var encoded_params = this.encodeParams(params);
        if (encoded_params) {
          path += "?" + encoded_params;
        }

        //jsonObj - make sure we have a valid json object
        jsonObj = JSON.stringify(jsonObj)
        if (!jsonlint.parse(jsonObj)) {
          throw(new Error('JSON object is not valid.'));
        }
        if (jsonObj == '{}') {
          jsonObj = null;
        } else {
          //curl - add in the json obj
          curl += " -d '" + jsonObj + "'";
        }

      } catch (e) {
        //parameter was invalid
        console.log('processQuery - error occured -' + e.message);
        return false;
      }
      //log the curl command to the console
      console.log(curl);
      //store the curl command back in the object
      QueryObj.setCurl(curl);

      var ajaxOptions = {
        type: method,
        url: this.getApiUrl() + path,
        success: function(response) {
          //query completed succesfully, so store cursor
          QueryObj.saveCursor(response.cursor);
          //then call the original callback
          if (QueryObj.callSuccessCallback) {
            QueryObj.callSuccessCallback(response);
          }
        },
        error: function(response) {
          console.log('API call failed - ' + response.responseText);
          if (QueryObj.callFailureCallback) {
            QueryObj.callFailureCallback(response);
          }
        },
        data: jsonObj || {},
        contentType: "application/json; charset=utf-8",
        dataType: "json"
      }

      // work with ie for cross domain scripting
      var accessToken = this.getToken();
      if (onIE) {
        ajaxOptions.dataType = "jsonp";
        if (application_name != 'SANDBOX' && accessToken) { ajaxOptions.data['access_token'] = accessToken }
      } else {
        ajaxOptions.beforeSend = function(xhr) {
          if (application_name != 'SANDBOX' && accessToken) { xhr.setRequestHeader("Authorization", "Bearer " + accessToken) }
        }
      }

      $.ajax(ajaxOptions);
    },

    /**
    *  @method encodeParams
    *  @purpose - to encode the query string parameters
    *  @params {object} params - an object of name value pairs that will be urlencoded
    *
    */
    encodeParams: function(params) {
      tail = [];
      var item = [];
      if (params instanceof Array) {
        for (i in params) {
          item = params[i];
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
                item = value[i];
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
  };
})(apigee);

var jsonlint = (function() {
	var require = true, module = false;
	var exports = {};
	var grammar = (function() {
		var parser = {
			trace : function trace() {
			},
			yy : {},
			symbols_ : {
				"JSONString" : 2,
				"STRING" : 3,
				"JSONNumber" : 4,
				"NUMBER" : 5,
				"JSONNullLiteral" : 6,
				"NULL" : 7,
				"JSONBooleanLiteral" : 8,
				"TRUE" : 9,
				"FALSE" : 10,
				"JSONText" : 11,
				"JSONObject" : 12,
				"JSONArray" : 13,
				"JSONValue" : 14,
				"{" : 15,
				"}" : 16,
				"JSONMemberList" : 17,
				"JSONMember" : 18,
				":" : 19,
				"," : 20,
				"[" : 21,
				"]" : 22,
				"JSONElementList" : 23,
				"$accept" : 0,
				"$end" : 1
			},
			terminals_ : {
				"3" : "STRING",
				"5" : "NUMBER",
				"7" : "NULL",
				"9" : "TRUE",
				"10" : "FALSE",
				"15" : "{",
				"16" : "}",
				"19" : ":",
				"20" : ",",
				"21" : "[",
				"22" : "]"
			},
			productions_ : [ 0, [ 2, 1 ], [ 4, 1 ], [ 6, 1 ], [ 8, 1 ],
					[ 8, 1 ], [ 11, 1 ], [ 11, 1 ], [ 14, 1 ], [ 14, 1 ],
					[ 14, 1 ], [ 14, 1 ], [ 14, 1 ], [ 14, 1 ], [ 12, 2 ],
					[ 12, 3 ], [ 18, 3 ], [ 17, 1 ], [ 17, 3 ], [ 13, 2 ],
					[ 13, 3 ], [ 23, 1 ], [ 23, 3 ] ],
			performAction : function anonymous(yytext, yyleng, yylineno, yy) {
				var $$ = arguments[5], $0 = arguments[5].length;
				switch (arguments[4]) {
				case 1:
					this.$ = yytext;
					break;
				case 2:
					this.$ = Number(yytext);
					break;
				case 3:
					this.$ = null;
					break;
				case 4:
					this.$ = true;
					break;
				case 5:
					this.$ = false;
					break;
				case 6:
					return this.$ = $$[$0 - 1 + 1 - 1];
					break;
				case 7:
					return this.$ = $$[$0 - 1 + 1 - 1];
					break;
				case 8:
					this.$ = $$[$0 - 1 + 1 - 1];
					break;
				case 9:
					this.$ = $$[$0 - 1 + 1 - 1];
					break;
				case 10:
					this.$ = $$[$0 - 1 + 1 - 1];
					break;
				case 11:
					this.$ = $$[$0 - 1 + 1 - 1];
					break;
				case 12:
					this.$ = $$[$0 - 1 + 1 - 1];
					break;
				case 13:
					this.$ = $$[$0 - 1 + 1 - 1];
					break;
				case 14:
					this.$ = {};
					break;
				case 15:
					this.$ = $$[$0 - 3 + 2 - 1];
					break;
				case 16:
					this.$ = [ $$[$0 - 3 + 1 - 1], $$[$0 - 3 + 3 - 1] ];
					break;
				case 17:
					this.$ = {};
					this.$[$$[$0 - 1 + 1 - 1][0]] = $$[$0 - 1 + 1 - 1][1];
					break;
				case 18:
					this.$ = $$[$0 - 3 + 1 - 1];
					$$[$0 - 3 + 1 - 1][$$[$0 - 3 + 3 - 1][0]] = $$[$0 - 3 + 3 - 1][1];
					break;
				case 19:
					this.$ = [];
					break;
				case 20:
					this.$ = $$[$0 - 3 + 2 - 1];
					break;
				case 21:
					this.$ = [ $$[$0 - 1 + 1 - 1] ];
					break;
				case 22:
					this.$ = $$[$0 - 3 + 1 - 1];
					$$[$0 - 3 + 1 - 1].push($$[$0 - 3 + 3 - 1]);
					break;
				default:
					;
				}
			},
			table : [ {
				"11" : 1,
				"12" : 2,
				"13" : 3,
				"15" : [ 1, 4 ],
				"21" : [ 1, 5 ]
			}, {
				"1" : [ 3 ]
			}, {
				"1" : [ 2, 6 ]
			}, {
				"1" : [ 2, 7 ]
			}, {
				"16" : [ 1, 6 ],
				"17" : 7,
				"18" : 8,
				"2" : 9,
				"3" : [ 1, 10 ]
			}, {
				"22" : [ 1, 11 ],
				"23" : 12,
				"14" : 13,
				"6" : 14,
				"8" : 15,
				"2" : 16,
				"4" : 17,
				"12" : 18,
				"13" : 19,
				"7" : [ 1, 20 ],
				"9" : [ 1, 21 ],
				"10" : [ 1, 22 ],
				"3" : [ 1, 10 ],
				"5" : [ 1, 23 ],
				"15" : [ 1, 4 ],
				"21" : [ 1, 5 ]
			}, {
				"1" : [ 2, 14 ],
				"22" : [ 2, 14 ],
				"20" : [ 2, 14 ],
				"16" : [ 2, 14 ]
			}, {
				"16" : [ 1, 24 ],
				"20" : [ 1, 25 ]
			}, {
				"16" : [ 2, 17 ],
				"20" : [ 2, 17 ]
			}, {
				"19" : [ 1, 26 ]
			}, {
				"19" : [ 2, 1 ],
				"22" : [ 2, 1 ],
				"20" : [ 2, 1 ],
				"16" : [ 2, 1 ]
			}, {
				"1" : [ 2, 19 ],
				"22" : [ 2, 19 ],
				"20" : [ 2, 19 ],
				"16" : [ 2, 19 ]
			}, {
				"22" : [ 1, 27 ],
				"20" : [ 1, 28 ]
			}, {
				"22" : [ 2, 21 ],
				"20" : [ 2, 21 ]
			}, {
				"22" : [ 2, 8 ],
				"20" : [ 2, 8 ],
				"16" : [ 2, 8 ]
			}, {
				"22" : [ 2, 9 ],
				"20" : [ 2, 9 ],
				"16" : [ 2, 9 ]
			}, {
				"22" : [ 2, 10 ],
				"20" : [ 2, 10 ],
				"16" : [ 2, 10 ]
			}, {
				"22" : [ 2, 11 ],
				"20" : [ 2, 11 ],
				"16" : [ 2, 11 ]
			}, {
				"22" : [ 2, 12 ],
				"20" : [ 2, 12 ],
				"16" : [ 2, 12 ]
			}, {
				"22" : [ 2, 13 ],
				"20" : [ 2, 13 ],
				"16" : [ 2, 13 ]
			}, {
				"22" : [ 2, 3 ],
				"20" : [ 2, 3 ],
				"16" : [ 2, 3 ]
			}, {
				"22" : [ 2, 4 ],
				"20" : [ 2, 4 ],
				"16" : [ 2, 4 ]
			}, {
				"22" : [ 2, 5 ],
				"20" : [ 2, 5 ],
				"16" : [ 2, 5 ]
			}, {
				"22" : [ 2, 2 ],
				"20" : [ 2, 2 ],
				"16" : [ 2, 2 ]
			}, {
				"1" : [ 2, 15 ],
				"22" : [ 2, 15 ],
				"20" : [ 2, 15 ],
				"16" : [ 2, 15 ]
			}, {
				"18" : 29,
				"2" : 9,
				"3" : [ 1, 10 ]
			}, {
				"14" : 30,
				"6" : 14,
				"8" : 15,
				"2" : 16,
				"4" : 17,
				"12" : 18,
				"13" : 19,
				"7" : [ 1, 20 ],
				"9" : [ 1, 21 ],
				"10" : [ 1, 22 ],
				"3" : [ 1, 10 ],
				"5" : [ 1, 23 ],
				"15" : [ 1, 4 ],
				"21" : [ 1, 5 ]
			}, {
				"1" : [ 2, 20 ],
				"22" : [ 2, 20 ],
				"20" : [ 2, 20 ],
				"16" : [ 2, 20 ]
			}, {
				"14" : 31,
				"6" : 14,
				"8" : 15,
				"2" : 16,
				"4" : 17,
				"12" : 18,
				"13" : 19,
				"7" : [ 1, 20 ],
				"9" : [ 1, 21 ],
				"10" : [ 1, 22 ],
				"3" : [ 1, 10 ],
				"5" : [ 1, 23 ],
				"15" : [ 1, 4 ],
				"21" : [ 1, 5 ]
			}, {
				"16" : [ 2, 18 ],
				"20" : [ 2, 18 ]
			}, {
				"16" : [ 2, 16 ],
				"20" : [ 2, 16 ]
			}, {
				"22" : [ 2, 22 ],
				"20" : [ 2, 22 ]
			} ],
			parseError : function parseError(str, hash) {
				throw new Error(str);
			},
			parse : function parse(input) {
				var self = this, stack = [ 0 ], vstack = [ null ], table = this.table, yytext = "", yylineno = 0, yyleng = 0, shifts = 0, reductions = 0;
				this.lexer.setInput(input);
				this.lexer.yy = this.yy;
				var parseError = this.yy.parseError = this.yy.parseError
						|| this.parseError;
				function lex() {
					var token;
					token = self.lexer.lex() || 1;
					if (typeof token !== "number") {
						token = self.symbols_[token];
					}
					return token;
				}
				var symbol, state, action, a, r, yyval = {}, p, len, ip = 0, newState, expected;
				symbol = lex();
				while (true) {
					state = stack[stack.length - 1];
					action = table[state] && table[state][symbol];
					if (typeof action === "undefined" || !action.length
							|| !action[0]) {
						expected = [];
						for (p in table[state]) {
							if (this.terminals_[p] && p != 1) {
								expected.push("'" + this.terminals_[p] + "'");
							}
						}
						if (this.lexer.showPosition) {
							parseError("Parse error on line " + (yylineno + 1)
									+ ":\n" + this.lexer.showPosition()
									+ "\nExpecting " + expected.join(", "), {
								text : this.lexer.match,
								token : this.terminals_[symbol],
								line : this.lexer.yylineno,
								expected : expected
							});
						} else {
							parseError("Parse error on line " + (yylineno + 1)
									+ ": Unexpected '"
									+ this.terminals_[symbol] + "'", {
								text : this.lexer.match,
								token : this.terminals_[symbol],
								line : this.lexer.yylineno,
								expected : expected
							});
						}
					}
					if (action[0] instanceof Array && action.length > 1) {
						throw new Error(
								"Parse Error: multiple actions possible at state: "
										+ state + ", token: " + symbol);
					}
					a = action;
					switch (a[0]) {
					case 1:
						shifts++;
						stack.push(symbol);
						++ip;
						yyleng = this.lexer.yyleng;
						yytext = this.lexer.yytext;
						yylineno = this.lexer.yylineno;
						symbol = lex();
						vstack.push(null);
						stack.push(a[1]);
						break;
					case 2:
						reductions++;
						len = this.productions_[a[1]][1];
						yyval.$ = vstack[vstack.length - len];
						r = this.performAction.call(yyval, yytext, yyleng,
								yylineno, this.yy, a[1], vstack);
						if (typeof r !== "undefined") {
							return r;
						}
						if (len) {
							stack = stack.slice(0, -1 * len * 2);
							vstack = vstack.slice(0, -1 * len);
						}
						stack.push(this.productions_[a[1]][0]);
						vstack.push(yyval.$);
						newState = table[stack[stack.length - 2]][stack[stack.length - 1]];
						stack.push(newState);
						break;
					case 3:
						this.reductionCount = reductions;
						this.shiftCount = shifts;
						return true;
					default:
						;
					}
				}
				return true;
			}
		};
		var lexer = (function() {
			var lexer = ({
				EOF : "",
				parseError : function parseError(str, hash) {
					if (this.yy.parseError) {
						this.yy.parseError(str, hash);
					} else {
						throw new Error(str);
					}
				},
				setInput : function(input) {
					this._input = input;
					this._more = this._less = this.done = false;
					this.yylineno = this.yyleng = 0;
					this.yytext = this.matched = this.match = "";
					return this;
				},
				input : function() {
					var ch = this._input[0];
					this.yytext += ch;
					this.yyleng++;
					this.match += ch;
					this.matched += ch;
					var lines = ch.match(/\n/);
					if (lines) {
						this.yylineno++;
					}
					this._input = this._input.slice(1);
					return ch;
				},
				unput : function(ch) {
					this._input = ch + this._input;
					return this;
				},
				more : function() {
					this._more = true;
					return this;
				},
				pastInput : function() {
					var past = this.matched.substr(0, this.matched.length
							- this.match.length);
					return (past.length > 20 ? "..." : "")
							+ past.substr(-20).replace(/\n/g, "");
				},
				upcomingInput : function() {
					var next = this.match;
					if (next.length < 20) {
						next += this._input.substr(0, 20 - next.length);
					}
					return (next.substr(0, 20) + (next.length > 20 ? "..." : ""))
							.replace(/\n/g, "");
				},
				showPosition : function() {
					var pre = this.pastInput();
					var c = (new Array(pre.length + 1)).join("-");
					return pre + this.upcomingInput() + "\n" + c + "^";
				},
				next : function() {
					if (this.done) {
						return this.EOF;
					}
					if (!this._input) {
						this.done = true;
					}
					var token, match, lines;
					if (!this._more) {
						this.yytext = "";
						this.match = "";
					}
					for ( var i = 0; i < this.rules.length; i++) {
						match = this._input.match(this.rules[i]);
						if (match) {
							lines = match[0].match(/\n/g);
							if (lines) {
								this.yylineno += lines.length;
							}
							this.yytext += match[0];
							this.match += match[0];
							this.matches = match;
							this.yyleng = this.yytext.length;
							this._more = false;
							this._input = this._input.slice(match[0].length);
							this.matched += match[0];
							token = this.performAction.call(this, this.yy,
									this, i);
							if (token) {
								return token;
							} else {
								return;
							}
						}
					}
					if (this._input == this.EOF) {
						return this.EOF;
					} else {
						this.parseError("Lexical error on line "
								+ (this.yylineno + 1)
								+ ". Unrecognized text.\n"
								+ this.showPosition(), {
							text : "",
							token : null,
							line : this.yylineno
						});
					}
				},
				lex : function() {
					var r = this.next();
					if (typeof r !== "undefined") {
						return r;
					} else {
						return this.lex();
					}
				}
			});
			lexer.performAction = function anonymous(yy, yy_) {
				switch (arguments[2]) {
				case 0:
					break;
				case 1:
					return 5;
					break;
				case 2:
					yy_.yytext = yy_.yytext.substr(1, yy_.yyleng - 2);
					return 3;
					break;
				case 3:
					return 15;
					break;
				case 4:
					return 16;
					break;
				case 5:
					return 21;
					break;
				case 6:
					return 22;
					break;
				case 7:
					return 20;
					break;
				case 8:
					return 19;
					break;
				case 9:
					return 9;
					break;
				case 10:
					return 10;
					break;
				case 11:
					return 7;
					break;
				case 12:
					return "INVALID";
					break;
				default:
					;
				}
			};
			lexer.rules = [
					/^\s+/,
					/^-?([0-9]|[1-9][0-9]+)(\.[0-9]+)?([eE][-+]?[0-9]+)?\b\b/,
					/^"(\\["bfnrt\/\\]|\\u[a-fA-F0-9]{4}|[^\0-\x08\x0a-\x1f"\\])*"/,
					/^\{/, /^\}/, /^\[/, /^\]/, /^,/, /^:/, /^true\b/,
					/^false\b/, /^null\b/, /^./ ];
			return lexer;
		})();
		parser.lexer = lexer;
		return parser;
	})();
	if (typeof require !== 'undefined') {
		exports.parser = grammar;
		exports.parse = function() {
			return grammar.parse.apply(grammar, arguments);
		};
		exports.main = function commonjsMain(args) {
			var cwd = require("file").path(require("file").cwd());
			if (!args[1]) {
				throw new Error("Usage: " + args[0] + " FILE");
			}
			var source = cwd.join(args[1]).read({
				charset : "utf-8"
			});
			this.parse(source);
		};
		if (require.main === module) {
			exports.main(require("system").args);
		}
	}
	return exports;
})();
