
/**
 * validation is a Singleton that provides methods for validating common field types
 *
 * @class Usergrid.validation
 * @author Rod Simpson (rod@apigee.com)
**/
Usergrid.validation = (function () {

  var usernameRegex = new RegExp("^([0-9a-zA-Z\.\-])+$");
  var nameRegex     = new RegExp("^([0-9a-zA-Z@#$%^&!?;:.,'\"~*-=+_\[\\](){}/\\ |])+$");
  var emailRegex    = new RegExp("^(([0-9a-zA-Z]+[_\+.-]?)+@[0-9a-zA-Z]+[0-9,a-z,A-Z,.,-]*(.){1}[a-zA-Z]{2,4})+$");
  var passwordRegex = new RegExp("^([0-9a-zA-Z@#$%^&!?<>;:.,'\"~*-=+_\[\\](){}/\\ |])+$");
  var pathRegex     = new RegExp("^([0-9a-z./-])+$");
  var titleRegex    = new RegExp("^([0-9a-zA-Z.!-?/])+$");

  /**
    * Tests the string against the allowed chars regex
    *
    * @public
    * @method validateUsername
    * @param {string} username - The string to test
    * @param {function} failureCallback - (optional), the function to call on a failure
    * @return {boolean} Returns true if string passes regex, false if not
    */
  function validateUsername(username, failureCallback) {
    if (usernameRegex.test(username) && checkLength(username, 4, 80)) {
      return true;
    } else {
      if (failureCallback && typeof(failureCallback) === "function") {
        failureCallback(this.getUsernameAllowedChars());
      }
      return false;
    }
  }

  /**
    * Returns the regex of allowed chars
    *
    * @public
    * @method getUsernameAllowedChars
    * @return {string} Returns a string with the allowed chars
    */
  function getUsernameAllowedChars(){
    return 'Length: min 4, max 80. Allowed: A-Z, a-z, 0-9, dot, and dash';
  }

  /**
    * Tests the string against the allowed chars regex
    *
    * @public
    * @method validateName
    * @param {string} name - The string to test
    * @param {function} failureCallback - (optional), the function to call on a failure
    * @return {boolean} Returns true if string passes regex, false if not
    */
  function validateName(name, failureCallback) {
    if (nameRegex.test(name) && checkLength(name, 4, 80)) {
      return true;
    } else {
      if (failureCallback && typeof(failureCallback) === "function") {
        failureCallback(this.getNameAllowedChars());
      }
      return false;
    }
  }

  /**
    * Returns the regex of allowed chars
    *
    * @public
    * @method getNameAllowedChars
    * @return {string} Returns a string with the allowed chars
    */
  function getNameAllowedChars(){
    return 'Length: min 4, max 80. Allowed: A-Z, a-z, 0-9, ~ @ # % ^ & * ( ) - _ = + [ ] { } \\ | ; : \' " , . / ? !';
  }

  /**
    * Tests the string against the allowed chars regex
    *
    * @public
    * @method validatePassword
    * @param {string} password - The string to test
    * @param {function} failureCallback - (optional), the function to call on a failure
    * @return {boolean} Returns true if string passes regex, false if not
    */
  function validatePassword(password, failureCallback) {
    if (passwordRegex.test(password) && checkLength(password, 5, 16)) {
      return true;
    } else {
      if (failureCallback && typeof(failureCallback) === "function") {
        failureCallback(this.getPasswordAllowedChars());
      }
      return false;
    }
  }

  /**
    * Returns the regex of allowed chars
    *
    * @public
    * @method getPasswordAllowedChars
    * @return {string} Returns a string with the allowed chars
    */
  function getPasswordAllowedChars(){
    return 'Length: min 5, max 16. Allowed: A-Z, a-z, 0-9, ~ @ # % ^ & * ( ) - _ = + [ ] { } \\ | ; : \' " , . < > / ? !';
  }

  /**
    * Tests the string against the allowed chars regex
    *
    * @public
    * @method validateEmail
    * @param {string} email - The string to test
    * @param {function} failureCallback - (optional), the function to call on a failure
    * @return {boolean} Returns true if string passes regex, false if not
    */
  function validateEmail(email, failureCallback) {
    if (emailRegex.test(email) && checkLength(email, 4, 80)) {
      return true;
    } else {
      if (failureCallback && typeof(failureCallback) === "function") {
        failureCallback(this.getEmailAllowedChars());
      }
      return false;
    }
  }

  /**
    * Returns the regex of allowed chars
    *
    * @public
    * @method getEmailAllowedChars
    * @return {string} Returns a string with the allowed chars
    */
  function getEmailAllowedChars(){
    return 'Email must be in standard form: e.g. example@Usergrid.com';
  }

  /**
    * Tests the string against the allowed chars regex
    *
    * @public
    * @method validatePath
    * @param {string} path - The string to test
    * @param {function} failureCallback - (optional), the function to call on a failure
    * @return {boolean} Returns true if string passes regex, false if not
    */
  function validatePath(path, failureCallback) {
    if (pathRegex.test(path) && checkLength(path, 4, 80)) {
      return true;
    } else {
      if (failureCallback && typeof(failureCallback) === "function") {
        failureCallback(this.getPathAllowedChars());
      }
      return false;
    }
  }

  /**
    * Returns the regex of allowed chars
    *
    * @public
    * @method getPathAllowedChars
    * @return {string} Returns a string with the allowed chars
    */
  function getPathAllowedChars(){
    return 'Length: min 4, max 80. Allowed: /, a-z, 0-9, dot, and dash';
  }

  /**
    * Tests the string against the allowed chars regex
    *
    * @public
    * @method validateTitle
    * @param {string} title - The string to test
    * @param {function} failureCallback - (optional), the function to call on a failure
    * @return {boolean} Returns true if string passes regex, false if not
    */
  function validateTitle(title, failureCallback) {
    if (titleRegex.test(title) && checkLength(title, 4, 80)) {
      return true;
    } else {
      if (failureCallback && typeof(failureCallback) === "function") {
        failureCallback(this.getTitleAllowedChars());
      }
      return false;
    }
  }

  /**
    * Returns the regex of allowed chars
    *
    * @public
    * @method getTitleAllowedChars
    * @return {string} Returns a string with the allowed chars
    */
  function getTitleAllowedChars(){
    return 'Length: min 4, max 80. Allowed: space, A-Z, a-z, 0-9, dot, dash, /, !, and ?';
  }

  /**
    * Tests if the string is the correct length
    *
    * @public
    * @method checkLength
    * @param {string} string - The string to test
    * @param {integer} min - the lower bound
    * @param {integer} max - the upper bound
    * @return {boolean} Returns true if string is correct length, false if not
    */
  function checkLength(string, min, max) {
    if (string.length > max || string.length < min) {
      return false;
    }
    return true;
  }

  /**
    * Tests if the string is a uuid
    *
    * @public
    * @method isUUID
    * @param {string} uuid The string to test
    * @returns {Boolean} true if string is uuid
    */
  function isUUID (uuid) {
    var uuidValueRegex = /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/;
    if (!uuid) return false;
    return uuidValueRegex.test(uuid);
  }

  return {
    validateUsername:validateUsername,
    getUsernameAllowedChars:getUsernameAllowedChars,
    validateName:validateName,
    getNameAllowedChars:getNameAllowedChars,
    validatePassword:validatePassword,
    getPasswordAllowedChars:getPasswordAllowedChars,
    validateEmail:validateEmail,
    getEmailAllowedChars:getEmailAllowedChars,
    validatePath:validatePath,
    getPathAllowedChars:getPathAllowedChars,
    validateTitle:validateTitle,
    getTitleAllowedChars:getTitleAllowedChars,
    isUUID:isUUID
  }
})();
