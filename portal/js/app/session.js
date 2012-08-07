
window.apigee = window.apigee || {};
apigee = apigee || {};

(function() {
  /**
   *  Application is a class for holding application info
   *
   *  @class Application
   *  @param {string} name the name of the application
   *  @param {string} uuid the uuid of the application
   */
  apigee.Application = function(name, uuid) {
    this._name = name;
    this._uuid = uuid;
  };
  apigee.Application.prototype = {
    getName: function() { 
      return this._name;
    },
    setName: function(name) { 
      this._name = name;
    },
    getUUID: function() { 
      return this._uuid;
    },
    setUUID: function(uuid) {
      this._uuid = uuid;
    },
    setCurrentApplication: function(app) {
      this.setName(app.getName());
      this.setUUID(app.getUUID());
    }
  };


  /**
   *  Organization is a class for holding application info
   *
   *  @class Organization
   *  @param {string} name organization's name
   *  @param {string} organization's uuid
   *  @param {string} list organization's applications
   */
  apigee.Organization = function(name, uuid) {
    this._name = name;
    this._uuid = uuid;
    this._list = [];
  };

  apigee.Organization.prototype = {
    getName: function() {
      return this._name;
    },
    setName: function(name) {
      this._name = name;
    },
    getUUID: function() {
      return this._uuid;
    },
    setUUID: function(uuid) {
      this._uuid = uuid;
    },
    setCurrentOrganization: function(org) {
      this._name = org.getName();
      this._uuid = org.getUUID();
      this._list = org.getList();
    },
    addItem: function(item) {
      var count = this._list.length;
      this._list[count] = item;
    },
    getItemByName: function(name) {
      var count = this._list.length;
      var i=0;
      for (i=0; i<count; i++) {
        if (this._list[i].getName() == name) {
          return this._list[i];
        }
      }
      return null;
    },
    getItemByUUID: function(UUID) {
      var count = this._list.length;
      var i=0;
      for (i=0; i<count; i++) {
        if (this._list[i].getUUID() == UUID) {
          return this._list[i];
        }
      }
      return null;
    },
    getFirstItem: function() {
      var count = this._list.length;
      if (count > 0) {
        return this._list[0];
      }
      return null;
    },
    getList: function() {
      return this._list;
    },
    setList: function(list) {
      this._list = list;
    },
    clearList: function() {
      this._list = [];
    }
  };

  /**
    *  Standardized methods for mantianing user and authentication state in the Application
    *  @class UserSession
    */
  apigee.UserSession = function(){};
        
  apigee.UserSession.prototype = {  	
    //access token access and setter methods
    getAccessToken: function() {
      var accessToken = localStorage.getItem('accessToken');
      return accessToken;
    },
    setAccessToken: function setAccessToken(accessToken) {
      localStorage.setItem('accessToken', accessToken);
    },
    //logged in user access and setter methods
    getUserUUID: function () {
      return localStorage.getItem('userUUID');
    },
    setUserUUID: function (uuid) {
    localStorage.setItem('userUUID', uuid);
    },
    getUserEmail: function () {
      return localStorage.getItem('userEmail');
    },
    setUserEmail: function (email) {
      localStorage.setItem('userEmail', email);
    },

    //convenience method to verify if user is logged in
    loggedIn: function () {
      var token = this.getAccessToken();
      var email = this.getUserEmail();
      return (token && email);
    },

    //convenience method for saving all active user vars at once
    saveAll: function (uuid, email, accessToken) {
      this.setUserUUID(uuid);
      this.setUserEmail(email);
      this.setAccessToken(accessToken);
    },

    //convenience method for clearing all active user vars at once
    clearAll: function () {
      localStorage.removeItem('userUUID');
      localStorage.removeItem('userEmail');
      localStorage.removeItem('accessToken');
    }
  };
})(apigee);
