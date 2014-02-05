/*
 *  This module is a collection of classes designed to make working with
 *  the Appigee App Services API as easy as possible.
 *  Learn more at http://apigee.com/docs/usergrid
 *
 *   Copyright 2012 Apigee Corporation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  @author rod simpson (rod@apigee.com)
 *  @author matt dobson (matt@apigee.com)
 *  @author ryan bridges (rbridges@apigee.com)
 */


//Hack around IE console.log
window.console = window.console || {};
window.console.log = window.console.log || function() {};

//Usergrid namespace encapsulates this SDK
window.Usergrid = window.Usergrid || {};
Usergrid = Usergrid || {};
Usergrid.USERGRID_SDK_VERSION = '0.10.07';


/*
 * Tests if the string is a uuid
 *
 * @public
 * @method isUUID
 * @param {string} uuid The string to test
 * @returns {Boolean} true if string is uuid
 */
function isUUID (uuid) {
  var uuidValueRegex = /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/;
  if (!uuid) {
    return false;
  }
  return uuidValueRegex.test(uuid);
}

/*
 *  method to encode the query string parameters
 *
 *  @method encodeParams
 *  @public
 *  @params {object} params - an object of name value pairs that will be urlencoded
 *  @return {string} Returns the encoded string
 */
function encodeParams (params) {
  var tail = [];
  var item = [];
  var i;
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
