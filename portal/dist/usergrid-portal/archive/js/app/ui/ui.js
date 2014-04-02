window.Usergrid = window.Usergrid || {};
Usergrid.console = Usergrid.console || {};
Usergrid.console.ui = Usergrid.console.ui || { };

(function() {
  //This code block *WILL* load before the document is complete

  function loadTemplate(name) {
    $.ajax({
      type: "GET",
      url: "templates/" + name,
      complete: function(jqXHR, textStatus){
        var html = jqXHR.responseText;
        if (html) {
          $.template(name, html);
        }
      }
    });
  }
  Usergrid.console.ui.loadTemplate = loadTemplate;

  var standardTableOpts = {
    "base_css" : "query-result-table",
    "links" : {
      "metadata.collections*" : "<a href=\"#\" onclick=\"Usergrid.console.pageOpenQueryExplorer('${value}'); return false;\">${value}</a>",
      "metadata.connections*" : "<a href=\"#\" onclick=\"Usergrid.console.pageOpenQueryExplorer('${value}'); return false;\">${value}</a>",
      "all_collections*" : "<a href=\"#\" onclick=\"Usergrid.console.pageOpenQueryExplorer('${value}'); return false;\">${value}</a>",
      "metadata.path" : "<a href=\"#\" onclick=\"Usergrid.console.pageOpenQueryExplorer('${value}'); return false;\">${value}</a>",
      "*uri" : "<a href=\"${value}\">${value}</a>",
      "*link" : "<a href=\"${value}\">${value}</a>"
    }
  };
  Usergrid.console.ui.standardTableOpts = standardTableOpts;

  var metadataTableOpts = {
    "base_css" : "query-result-table",
    "links" : {
      "collections*" : "<a href=\"#\" onclick=\"Usergrid.console.pageOpenQueryExplorer('${value}'); return false;\">${value}</a>",
      "connections*" : "<a href=\"#\" onclick=\"Usergrid.console.pageOpenQueryExplorer('${value}'); return false;\">${value}</a>",
      "path" : "<a href=\"#\" onclick=\"Usergrid.console.pageOpenQueryExplorer('${value}'); return false;\">${value}</a>",
      "uri" : "<a href=\"${value}\">${value}</a>"
    }
  };
  Usergrid.console.ui.metadataTableOpts = metadataTableOpts;

  var re1 = /[^a-zA-Z0-9-]/g;
  var re2 = /--*/g;
  var re3 = /\${value}/g;
  function makeObjectTable(obj, opts, parent_k) {
    var base_css = opts.base_css;
    var doArray = ($.type(obj) == "array");

    var t = "<table class=\"" + base_css + " " + base_css + "-obj\">";
    if (doArray) {
      t = "<table class=\"" + base_css + " " + base_css + "-list\">";
    }

    for (var k in obj) {
      var v = obj[k];
      var full_k = parent_k ? parent_k + "." + k : k;

      var cs = k.replace(re1, "-").replace(re2, "-");

      var vType = $.type(v);
      if ((vType  == "object") || (vType  == "array")) {
        v = makeObjectTable(v, opts, full_k);
      }

      if (vType  == "object") {
        t += "<tr class=\"" + base_css + "-row-obj\">";
      }
      else if (vType  == "array") {
        t += "<tr class=\"" + base_css + "-row-array\">";
      }
      else {
        t += "<tr class=\"" + base_css + "-row-line\">";

        var links = opts.links;
        var link_href = links[full_k];
        if (!link_href) {
          link_href = links[parent_k + "*"];
        }
        if (!link_href) {
          link_href = links["*" + k];
        }
        if (link_href) {
          v = link_href.replace(re3, v);
        }
        v = "<div>" + v + "</div>";
      }

      if (doArray) {
        t += "<td class=\"" + base_css + "-item\"><div>" + v + "</div></td>";
      }
      else {
        t += "<td class=\"" + base_css + "-name\"><div data-path=\"" + full_k + "\">" + k + "</div></td>";
        t += "<td class=\"" + base_css + "-value " + base_css + "-name-" + cs + "\">" + v + "</td>";
      }

      t += "</tr>";
    }

    t += "</table>";
    return t;
  }
  Usergrid.console.ui.makeObjectTable = makeObjectTable;

  function makeTableFromList(list, width, options) {
    var getListItem = null;
    var getListItemTemplateOptions = null;
    var listItemTemplate = null;

    var onRender = null;
    var output = null;

    var tableId = null;

    var tableClass = null;
    var rowClass = null;
    var cellClass = null;

    var tableStyle = null;
    var rowStyle = null;
    var cellStyle = null;

    if (options) {
      getListItem = options.getListItem;
      getListItemTemplateOptions = options.getListItemTemplateOptions;
      listItemTemplate = options.listItemTemplate;

      onRender = options.onRender;
      output = options.output;

      tableId = options.tableId;

      tableClass = options.tableClass;
      rowClass = options.rowClass;
      cellClass = options.cellClass;

      tableStyle = options.tableStyle;
      rowStyle = options.rowStyle;
      cellStyle = options.cellStyle;
    }

    tableId = tableId ? " id=\"" + tableId + "\" " : "";

    tableClass = tableClass ? " class=\"" + tableClass + "\" " : "";
    rowClass = rowClass ? " class=\"" + rowClass + "\" " : "";
    cellClass = cellClass ? " class=\"" + cellClass + "\" " : "";

    tableStyle = tableStyle ? " style=\"" + tableStyle + "\" " : "";
    rowStyle = rowStyle ? " style=\"" + rowStyle + "\" " : "";
    cellStyle = cellStyle ? " style=\"" + cellStyle + "\" " : "";

    var t = "<table" + tableId + tableClass + tableStyle + ">\n";
    var i = 0;
    if (!width || (width < 1)) width = 1;
    var count = (Math.floor((list.length - 1) / width) + 1) * width;
    while (i < count) {
      if ((i % width) == 0) {
        t += "<tr" + rowClass + rowStyle + ">\n";
      }
      t += "<td" + cellClass + cellStyle + ">";
      if (i < list.length) {
        if (getListItem) {
          t += getListItem(i, i % count, Math.floor(i / count));
        }
        else {
          t += list[i];
        }
      }
      t += "</td>\n";
      if (((i + 1) % width) == 0) {
        t += "</tr>\n";
      }
      i++;
    }
    t += "</table>\n";
    return t;
  }
  Usergrid.console.ui.makeTableFromList = makeTableFromList;

  function jsonSchemaToDForm(schema, obj) {
    var dform = { elements : [] };

    for (var propName in schema.properties) {
      var property = schema.properties[propName];
      var type = property.type;
      if (type == "string") {
        var value = '';
        try {
          var value = obj[propName];
        } catch (e) {}
        if (!value) value = "";
        var element = {
          "name" : "ui-form-" + propName,
          "id" : "ui-form-" + propName,
          "caption" : property.title,
          "type" : "text",
          "value" : value
        };
        dform.elements.push(element);
        dform.elements.push({
          "type" : "br"
        });
      } else if (type == "object") {
        var element = jsonSchemaToDForm(property, obj[propName]);
        element.type = "fieldset";
        element.caption = property.title;
        dform.elements.push(element);
      }
    }
    return dform;
  }
  Usergrid.console.ui.jsonSchemaToDForm = jsonSchemaToDForm;

  function jsonSchemaToPayload(schema){
    var payloadData = new Object();
    var payload = '';
    for (var propName in schema.properties) {
      var property = schema.properties[propName];
      var type = property.type;
      if (type == "string") {
        var value = $('#ui-form-'+propName).val();
        if (!value) value = "";
        payloadData[propName] = value;

      } else if (type == "object") {
        payloadData[propName] = {};
        for (var propName2 in schema.properties[propName].properties) {
          var property2 = schema.properties[propName].properties[propName2];
          var type2 = property2.type;
          if (type2 == "string") {
            var value2 = $('#ui-form-'+propName2).val();
            if (!value) value = "";
            payloadData[propName][propName2] = value2;

          }
        }
      }
    }
    return payloadData;
  }
  Usergrid.console.ui.jsonSchemaToPayload = jsonSchemaToPayload;

  function displayEntityListResponse(query_results, options, response) {

    query_results = query_results || {};

    var getListItem = null;
    var getListItemTemplateOptions = null;
    var listItemTemplate = null;

    var output = null;
    var prevButton = null;
    var nextButton = null;
    var nextPrevDiv = null;

    var noEntitiesMsg = "";

    var onRender = null;
    var onNoEntities = null;
    var onError = null;
    var onRawJson = null;

    if (options) {
      getListItem = options.getListItem;
      getListItemTemplateOptions = options.getListItemTemplateOptions;
      listItemTemplate = options.listItemTemplate;

      output = options.output;

      prevButton = options.prevButton;
      nextButton = options.nextButton;
      nextPrevDiv = options.nextPrevDiv;

      noEntitiesMsg = options.noEntitiesMsg;

      onRender = options.onRender;
      onNoEntities = options.onNoEntities;
      onError = options.onError;
      onRawJson = options.onRawJson;
    }

    query_results.entities = null;
    query_results.entities_by_id = null;
    query_results.query = query_results.query || { };

    var t = "";
    if (response.entities && (response.entities.length > 0)) {

      query_results.entities = response.entities;
      query_results.entities_by_id = {};

      // collections is the only one that uses this lower level item, and can't be trusted to be here
      // so hardcoding this check for that type and loading it in a try to make sure we don't error out
      // in other cases
      try {
        if (response.entities[0].metadata.collections &&
            options.output == "#collections-response-table") {
          query_results.entities = response.entities[0].metadata.collections;
        }
      } catch (e) {
        //do nothing, this is only to trap errors
      }

      if (listItemTemplate) {
        $(output).html("");
      }

      var path = response.path || "";
      path = "" + path.match(/[^?]*/);

      for (i in query_results.entities) {
        var entity = query_results.entities[i];
        query_results.entities_by_id[entity.uuid] = entity;

        var entity_path = (entity.metadata || {}).path;
        if ($.isEmptyObject(entity_path)) {
          entity_path = path + "/" + entity.uuid;
        }

        if (getListItem) {
          t += getListItem(entity, entity_path);
        }
        else if (listItemTemplate) {
          var options = null;
          if (getListItemTemplateOptions) {
            options = getListItemTemplateOptions(entity, entity_path);
          }
          if (!options) {
            options = {entity: entity, path: entity_path};
          }
          $.tmpl(listItemTemplate, options).appendTo(output);
        }
      }

      if (!listItemTemplate) {
        $(output).html(t);
      }

      if (onRender) {
        onRender();
      }

      if (prevButton) {
        if (query_results.query.hasPrevious && query_results.query.hasPrevious()) {
          $(prevButton).click(query_results.query.getPrevious);
          $(prevButton).show();
        }
        else {
          $(prevButton).hide();
        }
      }

      if (nextButton) {
        if (query_results.query.hasNext && query_results.query.hasNext()) {
          $(nextButton).click(query_results.query.getNext);
          $(nextButton).show();
        }
        else {
          $(nextButton).hide();
        }
      }

      if (nextPrevDiv) {
        if (query_results.query.hasPrevious && query_results.query.hasNext && (query_results.query.hasPrevious() || query_results.query.hasNext())) {
          $(nextPrevDiv).show();
        }
        else {
          $(nextPrevDiv).hide();
        }
      }

    } else if (response.entities && (response.entities.length == 0)) {
      if (nextPrevDiv) {
        $(nextPrevDiv).hide();
      }
      var s = null;
      if (onNoEntities) {
        s = onNoEntities();
      }
      $(output).html("<div class=\"query-response-empty\">" + (s ? s : noEntitiesMsg) + "</div>");
      // This is a hack, will be fixed in API
    } else if (response.error && (
      (response.error.exception == "org.usergrid.services.exceptions.ServiceResourceNotFoundException: Service resource not found") ||
        (response.error.exception == "java.lang.IllegalArgumentException: Not a valid entity value type or null: null"))) {
      if (nextPrevDiv) {
        $(nextPrevDiv).hide();
      }
      var s = null;
      if (onError) {
        s = onError();
      }
      $(output).html("<div class=\"query-response-empty\">" + (s ? s : noEntitiesMsg) + "</div>");

    } else {
      $(output).html(
        "<pre class=\"query-response-json\">"
          + JSON.stringify(response, null, "  ") + "</pre>");
      if (nextPrevDiv) {
        $(nextPrevDiv).hide();
      }
      if (onRawJson) {
        onRawJson();
      }
    }

    $(output).find(".button").button();

    return query_results;
  }
  Usergrid.console.ui.displayEntityListResponse = displayEntityListResponse;

})();
