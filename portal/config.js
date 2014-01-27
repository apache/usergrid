var Usergrid = Usergrid || {};

Usergrid.showNotifcations = true;

// used only if hostname does not match a real server name
Usergrid.overrideUrl = 'https://api.usergrid.com/';

Usergrid.options = {
  client:{
   // apiKey:'123456'
  },
  cssRefs:[],
  "scriptReferences":{
    "dev":[],
    "main": []
  },
  menuItems:[
    {path:'#!/org-overview', active:true,pic:'&#128193',title:'Org Overview'},
    {path:'#!/getting-started/setup',pic:'&#128640;',title:'Getting Started'},
    {path:'#!/app-overview/summary',pic:'&#59214;',title:'App Overview',
      items:[
        {path:'#!/app-overview/summary',pic:'&#128241;',title:'Summary'}
      ]
    },
    {
      path:'#!/users',pic:'&#59214;',title:'Users',
      items:[
        {path:'#!/users',pic:'&#128100;',title:'Users'},
        {path:'#!/groups',pic:'&#128101;',title:'Groups'},
        {path:'#!/roles',pic:'&#59170;',title:'Roles'}
      ]
    },
    {
      path:'#!/data',pic:'&#128248;',title:'Data',
      items:[
        {path:'#!/data',pic:'&#128254;',title:'Collections'}
      ]
    },
    {
      path:'#!/activities',pic:'&#59194;',title:'Activities'
    },
    {
      path:'#!/configure/default-configs',pic:'&#9874;',title:'Configure',
      items:[
        {path:'#!/configure/default-configs',pic:'&#128214;',title:'Default Configs'},
        {path:'#!/configure/beta-configs',pic:'&#59190;',title:'Beta Testing Configs'},
        {path:'#!/configure/ab-configs',pic:'&#59146;',title:'A/B Configs'}
      ]
    },
    {path:'#!/shell',pic:'&#9000;',title:'Shell'}
  ]
};

Usergrid.regex = {
  appNameRegex: new RegExp("^[0-9a-zA-Z.-]{3,25}$"),
  usernameRegex: new RegExp("^[0-9a-zA-Z\.\_-]{4,25}$"),
  nameRegex: new RegExp("^([0-9a-zA-Z@#$%^&!?;:.,'\"~*-:+_\[\\](){}/\\ |]{3,60})+$"),
  roleNameRegex: new RegExp("^([0-9a-zA-Z./-]{3,25})+$"),
  emailRegex: new RegExp("^(([0-9a-zA-Z]+[_\+.-]?)+@[0-9a-zA-Z]+[0-9,a-z,A-Z,.,-]*(.){1}[a-zA-Z]{2,4})+$"),
  passwordRegex: new RegExp("^([0-9a-zA-Z@#$%^&!?<>;:.,'\"~*-:+_\[\\](){}/\\ |]{6,25})+$"),
  pathRegex: new RegExp("^/[a-zA-Z0-9\.\*_~-]+(\/[a-zA-Z0-9\.\*_~-]+)*$"),
  titleRegex: new RegExp("[a-zA-Z0-9.!-?]+[\/]?"),
  urlRegex: new RegExp("^(http?:\/\/)?([\da-z\.-]+)\.([a-z\.]{2,6})([\/\w \.-]*)*\/?$"),
  zipRegex: new RegExp("^[0-9]{5}(?:-[0-9]{4})?$"),
  countryRegex: new RegExp("^[A-Za-z ]{3,100}$"),
  stateRegex: new RegExp("^[A-Za-z ]{2,100}$"),
  collectionNameRegex: new RegExp("^[0-9a-zA-Z_.]{3,25}$"),
  appNameRegexDescription: "This field only allows : A-Z, a-z, 0-9, dot, and dash and must be between 3-25 characters.",
  usernameRegexDescription: "This field only allows : A-Z, a-z, 0-9, dot, underscore and dash. Must be between 4 and 15 characters.",
  nameRegexDescription: "Please enter a valid name. Must be betwee 3 and 60 characters.",
  roleNameRegexDescription: "Role only allows : /, a-z, 0-9, dot, and dash. Must be between 3 and 25 characters.",
  emailRegexDescription: "Please enter a valid email.",
  passwordRegexDescription: "Please enter a valid password between 6 and 25 characters.",
  pathRegexDescription: "Path must begin with a slash, path only allows: /, a-z, 0-9, dot, and dash, paths of the format:  /path/ or /path//path are not allowed",
  titleRegexDescription: "Please enter a valid title.",
  urlRegexDescription: "Please enter a valid url",
  zipRegexDescription: "Please enter a valid zip code.",
  countryRegexDescription: "Sorry only alphabetical characters or spaces are allowed. Must be between 3-100 characters.",
  stateRegexDescription: "Sorry only alphabetical characters or spaces are allowed. Must be between 2-100 characters.",
  collectionNameRegexDescription: "Collection name only allows : a-z A-Z 0-9. Must be between 3-25 characters."
};

try{
  if(module && module.exports){
    module.exports = Usergrid;
  }
}catch(e){}