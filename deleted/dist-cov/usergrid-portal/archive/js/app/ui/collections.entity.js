window.Usergrid = window.Usergrid || {};
Usergrid.console = Usergrid.console || {};
Usergrid.console.ui = Usergrid.console.ui || { };
Usergrid.console.ui.collections = Usergrid.console.ui.collections || { };

(function() {
  // This code block *WILL* load before the document is complete

  var entity_list_item = {
    options: {
    },

    _create: function() {

      var self = this;
      var o = self.options;
      var el = self.element;

      var entity_id = el.dataset('entity-id');
      var entity_type = el.dataset('entity-type');
      var entity = Usergrid.console.getQueryResultEntity(entity_id);
      if (!entity) return;
      o.entity = entity;
      o.path = el.dataset('collection-path');

      o.header = self.getHeader();
      o.collections = self.getCollections();
      o.contents = self.getContents();
      o.metadata = self.getMetadata();
      o.json = self.getJson();

      o.header.appendTo(el);
      o.collections.appendTo(el);
      o.contents.appendTo(el);
      o.metadata.appendTo(el);
      o.json.appendTo(el);

      o.header.find(".button").button();

      var contents_button = o.header.find(".query-result-header-toggle-contents");
      contents_button.click(function() {
        if (o.contents.css('display') == 'none') {
          o.contents.show();
          o.json.hide();
          o.collections.hide();
        } else {
          o.contents.hide();
        }
        return false;
      });

      var json_button = o.header.find(".query-result-header-toggle-json");
      json_button.click(function() {
        if (o.json.css('display') == 'none') {
          o.json.show();
          o.contents.hide();
          o.collections.hide();
        } else {
          o.json.hide();
        }
        return false;
      });

      var collections_button = o.header.find(".query-result-header-toggle-collections");
      collections_button.click(function() {
        if (o.collections.css('display') == 'none') {
          o.collections.show();
          o.contents.hide();
          o.json.hide();
        } else {
          o.collections.hide();
        }
        return false;
      });

      var metadata_button = o.header.find(".query-result-header-toggle-metadata");
      metadata_button.click(function() {
        o.metadata.toggle();
        return false;
      });

      var link_button = o.header.find(".query-result-header-link");
    },

    getHeader : function() {
      var entity = this.options.entity,
         name = entity.uuid + " : " + entity.type;

      if (entity.name) {
        name = name + " : " + entity.name;
      } else if (entity.username) {
        name = name + " : " + entity.username;
      } else if (entity.title) {
        name = name + " : " + entity.title;
      }

      var collections = {
        entity : entity,
        name : name,
        path : this.options.path,
        collections : !$.isEmptyObject((entity.metadata || { }).collections || (entity.metadata || { }).connections),
        uri : (entity.metadata || { }).uri
      }

      if(entity.type === 'user' || entity.picture){
        if (!entity.picture) {
          entity.picture = "/images/user-photo.png"
        } else {
          entity.picture = entity.picture + "?d=http://" + window.location.host + window.location.pathname + "images/user-photo.png"
        }
        collections['picture'] = entity.picture;
      }

      return $.tmpl("apigee.ui.collections.entity.header.html",
        collections);

    },

    getCollections : function() {
      var entity = this.options.entity;

      var collections = $.extend({ }, (entity.metadata || { }).collections, (entity.metadata || { }).connections);

      if (!$.isEmptyObject(collections)) {
        return $.tmpl("apigee.ui.collections.entity.collections.html", {
          collections : collections
        }, {
          makeObjectTable : Usergrid.console.ui.makeObjectTable,
          tableOpts : Usergrid.console.ui.standardTableOpts
        });
      }
      return $("");
    },

    getContents : function() {
      var entity_contents = $.extend( false, { }, this.options.entity);
      try { // metadata may or may not be present
        var path = entity_contents['metadata']['path'];
        entity_contents = $.extend( false, entity_contents, {'path': path});
        var sets = entity_contents['metadata']['sets'];
        entity_contents = $.extend( false, entity_contents, {'sets': sets});
        var collections = entity_contents['metadata']['collections'];
        entity_contents = $.extend( false, entity_contents, {'collections': collections});
        delete entity_contents['metadata'];
      } catch(e) {}
      return $.tmpl("apigee.ui.collections.entity.contents.html", {
        entity : entity_contents
      }, {
        makeObjectTable : Usergrid.console.ui.makeObjectTable,
        tableOpts : Usergrid.console.ui.standardTableOpts
      });
    },

    getMetadata : function() {
      var entity = this.options.entity;
      if (!$.isEmptyObject(entity.metadata)) {
        return $.tmpl("apigee.ui.collections.entity.metadata.html", {
          metadata : entity.metadata
        }, {
          makeObjectTable : Usergrid.console.ui.makeObjectTable,
          tableOpts : Usergrid.console.ui.metadataTableOpts
        });
      }
      return $("");
    },

    getJson : function() {
      return $.tmpl("apigee.ui.collections.entity.json.html", {
        entity : this.options.entity
      }, {
        makeObjectTable : Usergrid.console.ui.makeObjectTable,
        tableOpts : Usergrid.console.ui.standardTableOpts
      });
    },

    destroy: function() {
      this.element.html("");
      this.options.entity = null;
      this.options.header = null;
      this.options.contents = null;
      this.options.metadata = null;
      this.options.collections = null;
      this.options.json = null;
    }

  }
  Usergrid.console.ui.collections.entity_list_item = entity_list_item;

  var entity_detail = {
    options: {
    },

    _create: function() {

      var self = this;
      var o = self.options;
      var el = self.element;

      var entity_id = el.dataset('entity-id');
      var entity_type = el.dataset('entity-type');
      var entity = Usergrid.console.getQueryResultEntity(entity_id);
      if (!entity) return;
      o.entity = entity;
      o.path = el.dataset('collection-path');

      o.details = self.getDetails();

      o.details.appendTo(el);

      o.collections = el.find(".query-result-collections");
      o.details.find(".button").button();
      o.contents = el.find(".query-result-contents");
      o.json = el.find(".query-result-json");
      o.formDiv = el.find(".query-result-form");

      var content_button = o.details.find(".query-result-header-toggle-contents");
      content_button.click(function() {
        if (o.contents.css('display') == 'none') {
          o.contents.show();
          o.json.hide();
          o.collections.hide();
        } else {
          o.contents.hide();
        }
        return false;
      });

      var collections_button = o.details.find(".query-result-header-toggle-collections");
      collections_button.click(function() {
        if (o.collections.css('display') == 'none') {
          o.collections.show();
          o.contents.hide();
          o.json.hide();
        } else {
          o.collections.hide();
        }
        return false;
      });

      var json_button = o.details.find(".query-result-header-toggle-json");
      json_button.click(function() {
        if (o.json.css('display') == 'none') {
          o.json.show();
          o.contents.hide();
          o.collections.hide();
        } else {
          o.json.hide();
        }
        return false;
      });

      var link_button = o.details.find(".query-result-header-link");
    },

    getDetails : function() {
      var entity = this.options.entity,
        name = entity.uuid + " : " + entity.type,
        collections,
        connections;

      if (entity.name) {
        name = name + " : " + entity.name;
      } else if (entity.username) {
        name = name + " : " + entity.username;
      } else if (entity.title) {
        name = name + " : " + entity.title;
      }

      if(entity.metadata.collections){
        collections = entity.metadata.collections;
      }

      if(entity.metadata.connections){
        connections = entity.metadata.connections;
      }

      var entity_contents = $.extend( false, { }, this.options.entity);
      var path = entity_contents['metadata']['path'];
      entity_contents = $.extend( false, entity_contents, {'path': path});
      delete entity_contents['metadata'];

      var metadata = entity.metadata;
      if ($.isEmptyObject(metadata)) metadata = null;

      return $.tmpl("apigee.ui.collections.entity.detail.html", {
        entity : entity_contents,
        name : name,
        path : this.options.path,
        collections : collections,
        connections : connections,
        metadata : metadata,
        uri : (entity.metadata || { }).uri
      }, {
        makeObjectTable : Usergrid.console.ui.makeObjectTable,
        tableOpts : Usergrid.console.ui.standardTableOpts,
        metadataTableOpts : Usergrid.console.ui.metadataTableOpts
      });

    },

    destroy: function() {
      this.element.html("");
      this.options.entity = null;
      this.options.details.header = null;
      this.options.json = null;
      this.options.formDiv = null;
    }

  };
  Usergrid.console.ui.collections.entity_detail = entity_detail;

  (function($) {
    // This code block *WILL NOT* load before the document is complete

    $.widget("ui.apigee_collections_entity_list_item", entity_list_item);
    $.widget("ui.apigee_collections_entity_detail", entity_detail);

  })(jQuery);

})();
