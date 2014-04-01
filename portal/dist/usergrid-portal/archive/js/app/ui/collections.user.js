window.Usergrid = window.Usergrid || {};
Usergrid.console = Usergrid.console || {};
Usergrid.console.ui = Usergrid.console.ui || { };
Usergrid.console.ui.collections = Usergrid.console.ui.collections || { };

(function($) {
  //This code block *WILL NOT* load before the document is complete

  // Simplified vcard in JSON Schema for demonstration purposes
  // Every collection will have a JSON Schema available and downloadable from the server
  // Empty collections will have a minimal schema that only contains the basic required entity properties
  // Developers will be able to upload schemas to collections as well
  var vcard_schema = {
    "description":"A representation of a person, company, organization, or place",
    "type":"object",
    "properties":{
      "username":{
        "type":"string",
        "optional": true,
        "title" : "Username"
      },
      "name":{
        "description":"Formatted Name",
        "type":"string",
        "optional":true,
        "title" : "Full Name"
      },
      "title":{
        "description":"User Title",
        "type":"string",
        "optional": true,
        "title":"Title"
      },
      "url":{
        "type":"string",
        "format":"url",
        "optional":true,
        "title" : "Home Page"
      },
      "email":{
        "type":"string",
        "format":"email",
        "optional":true,
        "title" : "Email"
      },
      "tel":{
        "type":"string",
        "format":"phone",
        "optional":true,
        "title" : "Telephone"
      },
      "picture":{
        "type":"string",
        "format":"image",
        "optional":true,
        "title" : "Picture URL"
      },
      "bday":{
        "type":"string",
        "format":"date",
        "optional":true,
        "title" : "Birthday"
      },
      "adr":{
        "type":"object",
        "properties":{
          "id":{
            "type":"integer"
          },
          "addr1":{
            "type":"string",
            "title" : "Street 1"
          },
          "addr2":{
            "type":"string",
            "title" : "Street 2"
          },
          "city":{
            "type":"string",
            "title" : "City"
          },
          "state":{
            "type":"string",
            "title" : "State"
          },
          "zip":{
            "type":"string",
            "title" : "Zip"
          },
          "country":{
            "type":"string",
            "title" : "Country"
          }
        },
        "optional":true,
        "title" : "Address"
      }
    }
  };
  Usergrid.console.ui.collections.vcard_schema = vcard_schema;

  var group_schema = {
    "description":"A representation of a group",
    "type":"object",
    "properties":{
      "path":{
        "type":"string",
        "optional": true,
        "title" : "Group Path"
      },
      "title":{
        "type":"string",
        "optional":true,
        "title" : "Display Name"
      }
    }
  };
  Usergrid.console.ui.collections.group_schema = group_schema;

})(jQuery);
