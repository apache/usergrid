
/**
 *  Item is a base container class for apps and orgs
 *
 *  @class Item
 *  @param {string} name the name of the application
 *  @param {string} uuid the uuid of the application
 */
function Item(name, uuid){
  this._name = name;
  this._uuid = uuid;
}
Item.prototype.getName = function getName() { return this._name; }
Item.prototype.setName = function setName(name) { this._name = name; }
Item.prototype.getUUID = function getUUID() { return this._uuid; }
Item.prototype.setUUID = function setUUID(uuid) { this._uuid = uuid; }

/**
 *  User is a class for holding application info
 *
 *  @class Application
 *  @param {string} name the name of the application
 *  @param {string} uuid the uuid of the application
 */
function User(name, uuid, token, email){
  this._name = name;
  this._uuid = uuid;
  this._email = email;
}
User.prototype = new Item();
User.prototype.getEmail = function getEmail() { return this._email; }
User.prototype.setEmail = function setEmail(email) { this._email = email; }
User.prototype.clearAll = function clearAll() {
  this._name = null;
  this._uuid = null;
  this._token = null;
  this._email = null;
}
User.prototype.loggedIn = function loggedIn() {
  return (this._token && this._email);
}

/**
 *  Application is a class for holding application info
 *
 *  @class Application
 *  @param {string} name the name of the application
 *  @param {string} uuid the uuid of the application
 */
function Application(name, uuid){
  this._name = name;
  this._uuid = uuid;
}
Application.prototype = new Item();
Application.prototype.setCurrentApplication = function setCurrentApplication(app) {
  this.setName(app.getName());
  this.setUUID(app.getUUID());
}

/**
 *  Organization is a class for holding application info
 *
 *  @class Organization
 *  @param {string} name organization's name
 *  @param {string} organization's uuid
 *  @param {string} list organization's applications
 */
function Organization(name, uuid){
  this._name = name;
  this._uuid = uuid;
  this._list = [];
}
Organization.prototype = new Item();
Organization.prototype.constructor=Organization;
Item.prototype.setCurrentOrganization = function setCurrentOrganization(org) {
  this._name = org.getName();
  this._uuid = org.getUUID();
  this._list = org.getList();
}
Organization.prototype.addListItem = function addListItem(item) {
  var count = this._list.length;
  this._list[count] = item;
}
Organization.prototype.getItemByName = function getItemByName(name) {
  var count = this._list.length;
  var i=0;
  for (i=0; i<count; i++) {
    if (this._list[i].getName() == name) {
      return this._list[i];
    }
  }
}
Organization.prototype.getItemByUUID = function getItemByUUID(UUID) {
  var count = this._list.length;
  var i=0;
  for (i=0; i<count; i++) {
    if (this._list[i].getUUID() == UUID) {
      return this._list[i];
    }
  }
}
Organization.prototype.getFirstItem = function getFirstItem() {
  var count = this._list.length;
  if (count > 0) {
    return this._list[0];
  }
  return null;
}
Organization.prototype.getList = function getList() {
  return this._list;
}
Organization.prototype.setList = function setList(list) {
  this._list = list;
}
Organization.prototype.clearList = function clearList() {
  this._list = [];
}

/**
 *  Organizations is a class for holding all organizations
 *
 *  @class Organizations
 *  @param {string} name organization's name
 *  @param {string} organization's uuid
 *  @param {string} list organization's applications
 */
function Organizations(){
  this._list = [];
}
Organizations.prototype = new Organization();
Organizations.prototype.constructor=Organizations;




/**
 *  Standardized methods for mantianing authentication state in the Application
 *  @class session
 */
function Session() {

}

//access token access and setter methods
Session.prototype.getAccessToken = function getAccessToken() {
  var accessToken = localStorage.getItem('accessToken');
  return accessToken;
}
Session.prototype.setAccessToken = function setAccessToken(accessToken) {
  localStorage.setItem('accessToken', accessToken);
}
//logged in user access and setter methods
Session.prototype.getLoggedInUserUUID = function getLoggedInUserUUID() {
  return localStorage.getItem('usergridUserUUID');
}
Session.prototype.setLoggedInUserUUID = function setLoggedInUserUUID(uuid) {
 localStorage.setItem('usergridUserUUID', uuid);
}
Session.prototype.getLoggedInUserEmail = function getLoggedInUserEmail() {
  return localStorage.getItem('usergridUserEmail');
}
Session.prototype.setLoggedInUserEmail = function setLoggedInUserEmail(email) {
  localStorage.setItem('usergridUserEmail', email);
}

//convenience method to verify if user is logged in
Session.prototype.loggedIn = function loggedIn() {
  var token = this.getAccessToken();
  var email = this.getLoggedInUserEmail();
  return (token && email);
}

//convenience method for saving all active user vars at once
Session.prototype.saveAll = function saveAll(uuid, email, accessToken) {
  this.setLoggedInUserUUID(uuid);
  this.setLoggedInUserEmail(email);
  this.setAccessToken(accessToken);
}

//convenience method for clearing all active user vars at once
Session.prototype.clearAll = function clearAll() {
  localStorage.removeItem('usergridUserUUID');
  localStorage.removeItem('usergridUserEmail');
  localStorage.removeItem('accessToken');
}