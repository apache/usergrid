/**
 *  App SDK is a collection of classes designed to make working with
 *  the Appigee App Services API as easy as possible.
 *  Learn more at http://apigee.com/docs
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
 */

window.Usergrid = window.Usergrid || {};
Usergrid = Usergrid || {};
/**
 *  Standardized methods for maintaining user and authentication state in the Application
 *  @class UserSession
 *  @author Rod Simpson (rod@apigee.com)
 */
(function() {
  Usergrid.ApiClient.getOrganizationName = function() {
     return localStorage.getItem('organizationName');
  }
  Usergrid.ApiClient.setOrganizationName = function(organizationName) {
     localStorage.setItem('organizationName', organizationName);
  }

  Usergrid.ApiClient.getApplicationName = function() {
     return localStorage.getItem('applicationName');
  }
  Usergrid.ApiClient.setApplicationName = function(applicationName) {
     localStorage.setItem('applicationName', applicationName);
  }

  Usergrid.ApiClient.getToken = function() {
     return localStorage.getItem('token');
  }
  Usergrid.ApiClient.setToken = function(token) {
     localStorage.setItem('token', token);
  }
  
  Usergrid.ApiClient.getLoggedInUser = function() {
     var data = JSON.parse(localStorage.getItem('user'));
     var user = new Usergrid.Entity('user');
     user.set(data);
     return user;
  }
  Usergrid.ApiClient.setLoggedInUser = function(user) {
    var data = null;
    if (user) { data = user.get(); }
    localStorage.setItem('user', JSON.stringify(data));
  }

  Usergrid.ApiClient.clearSession = function () {
    localStorage.removeItem('organizationName');
    localStorage.removeItem('applicationName');
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  }
  
})(Usergrid);
