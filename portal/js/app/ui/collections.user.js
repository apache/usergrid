window.apigee = window.apigee || {};
apigee.console = apigee.console || {};
apigee.console.ui = apigee.console.ui || { };
apigee.console.ui.collections = apigee.console.ui.collections || { };

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
      "familyName":{
        "type":"string",
        "title" : "Last Name"
      },
      "givenName":{
        "type":"string",
        "title" : "First Name"
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
  apigee.console.ui.collections.vcard_schema = vcard_schema;

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
  apigee.console.ui.collections.group_schema = group_schema;

  apigee.console.ui.loadTemplate("apigee.ui.collections.user.header.html");
  apigee.console.ui.loadTemplate("apigee.ui.collections.user.detail.html");

  // User list view is a subclass of the Entity list view
  $.widget("ui.apigee_collections_user_list_item", $.extend( true, { }, apigee.console.ui.collections.entity_list_item, {

    getHeader : function() {
      var entity = this.options.entity;

      var name = entity.uuid + " : "
        + entity.type;
      if (entity.name) {
        name = name + " : " + entity.name;
      } else if (entity.username) {
        name = name + " : " + entity.username;
      }

      var path = this.options.path;

      var collections = !$.isEmptyObject((entity.metadata || { }).collections || (entity.metadata || { }).connections);

      var uri = (entity.metadata || { }).uri;

      if (!entity.picture) {
        entity.picture = "/images/user_profile.png"
      } else {
        entity.picture = entity.picture + "?d=http://" + window.location.host + window.location.pathname + "images/user_profile.png"
      }

      return $.tmpl("apigee.ui.collections.user.header.html", {
        entity : entity,
        picture : entity.picture,
        name : name,
        path : path,
        fblink : entity.fblink,
        collections : collections,
        uri : uri
      } );

    }

  }));

  $.widget("ui.apigee_collections_user_detail", $.extend( true, { }, apigee.console.ui.collections.entity_detail, {

    getDetails : function() {
      var entity = this.options.entity;

      var name = entity.uuid + " : "
        + entity.type;
      if (entity.name) {
        name = name + " : " + entity.name;
      } else if (entity.username) {
        name = name + " : " + entity.username;
      }

      var collections = $.extend({ }, (entity.metadata || { }).collections, (entity.metadata || { }).connections);
      if ($.isEmptyObject(collections)) collections = null;
      var entity_contents = $.extend( false, { }, this.options.entity);
      var path = entity_contents['metadata']['path'];
      entity_contents = $.extend( false, entity_contents, {'path': path});
      var sets = entity_contents['metadata']['sets'];
      entity_contents = $.extend( false, entity_contents, {'sets': sets});
      var collections = entity_contents['metadata']['collections'];
      entity_contents = $.extend( false, entity_contents, {'collections': collections});
      delete entity_contents['metadata'];

      var metadata = entity.metadata;
      if ($.isEmptyObject(metadata)) metadata = null;

      var details = $.tmpl("apigee.ui.collections.user.detail.html", {
        entity : entity_contents,
        picture : entity.picture,
        name : name,
        path : this.options.path,
        collections : collections,
        metadata : metadata,
        uri : (entity.metadata || { }).uri
      }, {
        makeObjectTable : apigee.console.ui.makeObjectTable,
        tableOpts : apigee.console.ui.standardTableOpts,
        metadataTableOpts : apigee.console.ui.metadataTableOpts
      });

      var formDiv = details.find(".query-result-form");
      $(formDiv).buildForm(apigee.console.ui.jsonSchemaToDForm(vcard_schema, entity));

      return details;
    }

  }));

})(jQuery);
