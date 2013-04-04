function apigee_console_app(Pages, query_params) {
  //This code block *WILL NOT* load before the document is complete
  window.Usergrid = window.Usergrid || {};
  Usergrid.console = Usergrid.console || {};

  // for running Apigee App Services as a local server
  var LOCAL_STANDALONE_API_URL = "http://localhost/usergrid";
  var LOCAL_TOMCAT_API_URL = "http://localhost:8080/ROOT";
  var LOCAL_API_URL = LOCAL_STANDALONE_API_URL;
  var PUBLIC_API_URL = "https://api.usergrid.com/";
  var FORCE_PUBLIC_API = true; // Always use public API
  if (!FORCE_PUBLIC_API && (document.domain.substring(0,9) == "localhost")) {
    Usergrid.ApiClient.setApiUrl(LOCAL_API_URL);
  }

  String.prototype.endsWith = function (s) {
    return (this.length >= s.length && this.substr(this.length - s.length) == s);
  };

  if (query_params.api_url) {
      if (!query_params.api_url.endsWith('/')) {
        query_params.api_url += '/';
      }
      Usergrid.ApiClient.setApiUrl(query_params.api_url);
  }
  //Display message page in case there is a a timeout to the API
  Usergrid.ApiClient.setCallTimeoutCallback(function(){
    showMessagePage();
  });

  var HIDE_CONSOLE = query_params.hide_console || "";

  if (HIDE_CONSOLE.indexOf("true") >= 0) {
    $('#sidebar-menu ul li a[href="#console"]').hide();
  }

  var OFFLINE = false;
  var OFFLINE_PAGE = "#query-page";

  var self = this;

  var emailRegex = new RegExp("^(([0-9a-zA-Z]+[_\+.-]?)+@[0-9a-zA-Z]+[0-9,a-z,A-Z,.,-]*(.){1}[a-zA-Z]{2,4})+$");
  var emailAllowedCharsMessage = 'eg. example@apigee.com';

  var passwordRegex = new RegExp("^([0-9a-zA-Z@#$%^&!?<>;:.,'\"~*=+_\[\\](){}/\\ |-])+$");
  var passwordAllowedCharsMessage = 'This field only allows: A-Z, a-z, 0-9, ~ @ # % ^ & * ( ) - _ = + [ ] { } \\ | ; : \' " , . < > / ? !';
  var passwordMismatchMessage = 'Password must match';

  var usernameRegex = new RegExp("^([0-9a-zA-Z\.\_-])+$");
  var usernameAllowedCharsMessage = 'This field only allows : A-Z, a-z, 0-9, dot, underscore and dash';

  var organizationNameRegex = new RegExp ("^([0-9a-zA-Z.-])+$");
  var organizationNameAllowedCharsMessage = 'This field only allows : A-Z, a-z, 0-9, dot, and dash';

  //Regex declared differently from al the others because of the use of ". Functions exacly as if it was called from new RegExp
  var nameRegex = /[0-9a-zA-ZáéíóúÁÉÍÓÚÑñ@#$%\^&!\?;:\.,'\"~\*-=\+_\(\)\[\]\{\}\|\/\\]+/;
  var nameAllowedCharsMessage = "This field only allows: A-Z, a-z, áéíóúÁÉÍÓÚÑñ, 0-9, ~ @ # % ^ & * ( ) - _ = + [ ] { } \\ | ; : \' \" , . / ? !";

  var titleRegex = new RegExp("[a-zA-Z0-9.!-?]+[\/]?");
  var titleAllowedCharsMessage = 'Title field only allows : space, A-Z, a-z, 0-9, dot, dash, /, !, and ?';

  var alphaNumRegex = new RegExp("[0-9a-zA-Z]+");
  var alphaNumAllowedCharsMessage = 'Collection name only allows : a-z A-Z 0-9';

  var pathRegex = new RegExp("^[^\/]*([a-zA-Z0-9\.-]+[\/]{0,1})+[^\/]$");
  var pathAllowedCharsMessage = 'Path only allows : /, a-z, 0-9, dot, and dash, paths of the format: /path, path/, or path//path are not allowed';

  var roleRegex = new RegExp("^([0-9a-zA-Z./-])+$");
  var roleAllowedCharsMessage = 'Role only allows : /, a-z, 0-9, dot, and dash';

  var intRegex = new RegExp("^([0-9])+$");

  var applications = {};
  var applications_by_id = {};

  var current_application_id = "";
  var current_application_name = {};
  var applicationData = {};

  var query_entities = null;
  var query_entities_by_id = null;

  var query_history = [];

  var indexes = [];
  var backgroundGraphColor = '#ffffff';

  String.prototype.startsWith = function(s) {
    return this.lastIndexOf(s, 0) === 0;
  };

  String.prototype.endsWith = function(s) {
    return this.length >= s.length
      && this.substr(this.length - s.length) == s;
  };

  function initOrganizationVars() {
    applications = {};
    applications_by_id = {};

    current_application_id = "";
    current_application_name = "";

    query_entities = null;
    query_entities_by_id = null;

    query_history = [];

    indexes = [];
  }

  function keys(o) {
    var a = [];
    for (var propertyName in o) {
      a.push(propertyName);
    }
    return a;
  }

  $('#api-activity').ajaxStart( function() {
    $(this).show();
  });

  $('#api-activity').ajaxComplete( function() {
    $(this).hide();
  });

  function showPanel(page) {
    var p = $(page);
    $("#console-panels").children().each(function() {
      if ($(this).attr("id") == p.attr("id")) {
        $(this).show();
      } else {
        $(this).hide();
      }
    });
  }

  function initConsoleFrame(){
    $("#console-panel iframe").attr("src", url);
  }

  function getAccessTokenURL(){
    var bearerToken = Usergrid.ApiClient.getToken();
    var app_name = Usergrid.ApiClient.getApplicationName();
    if (typeof app_name != 'string') {
      app_name = '';
    }
    var org_name = Usergrid.ApiClient.getOrganizationName();
    if (typeof org_name != 'string') {
      org_name = '';
    }
    var bearerTokenJson = JSON.stringify(
      [{
        "type":"custom_token",
        "name":"Authorization",
        "value":"Bearer " + bearerToken,
        "style":"header"
      }, {
        "type":"custom_token",
        "name":"app_name",
        "value":app_name,
        "style":"template"
      }, {
        "type":"custom_token",
        "name":"org_name",
        "value":org_name,
        "style":"template"
      }]
    );
    var bearerTokenString = encodeURIComponent(bearerTokenJson);
    var url = 'https://apigee.com/apigeedev/console/usergrid?v=2&embedded=true&auth=' + bearerTokenString;
    return url;
  }
  Usergrid.console.getAccessTokenURL = getAccessTokenURL;

  function showPanelContent(panelDiv, contentDiv) {
    var cdiv = $(contentDiv);
    $(panelDiv).children(".panel-content").each(function() {
      var el = $(this);
      if (el.attr("id") == cdiv.attr("id")) {
        el.show();
      } else {
        el.hide();
      }
    });
  }

  function selectTabButton(link) {
    var tab = $(link).parent();
    tab.parent().find("li.active").removeClass('active');
    tab.addClass('active');
  }

  function selectPillButton(link) {
    var tab = $(link);
    tab.parent().find("a.active").removeClass('active');
    tab.addClass('active');
  }
  function selectFirstTabButton(bar){
    selectTabButton($(bar).find("li:first-child a"));
  }

  function setNavApplicationText() {
    var name = Usergrid.ApiClient.getApplicationName();
    if(!name) {
      name = "Select an Application";
    }
    $('#current-app-name').html('<div class="app-menu">' + name + '</div>  <span class="caret"></span>');
  //  $('.thingy span.title span.app_title').text(" " + name);
    $('#nav-app-name').html(name);
  }

  /*******************************************************************
   *
   * Collections
   *
   ******************************************************************/

  function pageSelectCollections() {
    hideModal(' #collections-messages')
    getCollections();
  }
  window.Usergrid.console.pageSelectCollections = pageSelectCollections;

  function getCollections() {
    //clear out the table before we start
    var output = $('#collections-table');
    output.empty();
    hideCurlCommand('collections');
    var section =$('#application-collections');
    section.empty().html('<div class="alert alert-info">Loading...</div>');

    var queryObj = new Usergrid.Query("GET",'', null, null, getCollectionsCallback,
      function() { alertModal("Error", "There was an error getting the collections"); }
    );

    runAppQuery(queryObj);
    return false;
  }

function compare(a,b) {
  if (a.name < b.name)
     return -1;
  if (a.name > b.name)
    return 1;
  return 0;
}

  function getCollectionsCallback(response) {
    $('#collections-pagination').hide();
    $('#collections-next').hide();
    $('#collections-previous').hide();
    showEntitySelectButton();
    if (response.entities && response.entities[0] && response.entities[0].metadata && response.entities[0].metadata.collections) {
      applicationData.Collections = response.entities[0].metadata.collections;
      updateApplicationDashboard();
      updateQueryAutocompleteCollections();
    }

    var data = response.entities[0].metadata.collections;
    var output = $('#collections-table');

    var elements = [];
    for (var key in data) {
      if (data.hasOwnProperty(key)) {
        elements.push(data[key])
      }
    }
    elements.sort(compare)
    var r = {};
    if ($.isEmptyObject(data)) {
      output.replaceWith('<div id="collections-table" class="collection-panel-section-message">No collections found.</div>');
    } else {
      output.replaceWith('<table id="collections-table" class="table"><tbody></tbody></table>');
      var leftMenuContent = '<ul id="collections-link-buttons" class="nav nav-list" style="margin-bottom: 5px;">';
      for (var i=0;i<elements.length;i++) {
        r.name = elements[i];
        r.count = data[elements[i].count];
        $.tmpl('apigee.ui.collections.table_rows.html', r).appendTo('#collections-table');

        var link = "Usergrid.console.pageOpenQueryExplorer('"+elements[i].name+"'); return false;";
        leftMenuContent += '<li id="collections-link-button-'+elements[i].name+'"><a href="#" onclick="'+link+'" class="collection-nav-links"><span class="nav-menu-text">/'+elements[i].name+'</span></a></li>';


      }
      /*
      for (var i in data) {
        var this_data = data[i];
        $.tmpl('apigee.ui.collections.table_rows.html', this_data).appendTo('#collections-table');

        var link = "Usergrid.console.pageOpenQueryExplorer('"+data[i].name+"'); return false;";
        leftMenuContent += '<li id="collections-link-button-'+data[i].name+'"><a href="#" onclick="'+link+'" class="collection-nav-links"><span class="nav-menu-text">/'+data[i].name+'</span></a></li>';
      }*/
      leftMenuContent += '</ul>';
      $('#left-collections-content').html(leftMenuContent);
    }
    showCurlCommand('collections', this.getCurl(), this.getToken());
  }

  /*******************************************************************
   *
   * Query Explorer
   *
   ******************************************************************/

  function pageOpenQueryExplorer(collection) {

    collection = collection || "";
    showPanel("#collections-panel");
    hideMoreQueryOptions();
    //reset the form fields
    $("#query-path").val("");
    $("#query-source").val("");
    $("#query-ql").val("");
    showQueryCollectionView();
    query_history = [];
    //Prepare Collection Index Dropdown Menu
    requestIndexes(collection);
    //bind events for previous and next buttons
    bindPagingEvents('query-response');
    //clear out the table before we start
    var output = $('#query-response-table');
    output.empty();
    //if a collection was provided, go ahead and get the default data
    if (collection) {
      getCollection('GET', collection);
    }
  }
  window.Usergrid.console.pageOpenQueryExplorer = pageOpenQueryExplorer;

  //change contexts for REST operations
  $("#button-query-get").click(function(){
    $("#query-json-box").hide();
    $("#query-query-box").show();
    $("#query-limit-box").show();
  });

  $("#button-query-post").click(function(){
    $("#query-json-box").show();
    $("#query-query-box").hide();
    $("#query-limit-box").hide();
  });

  $("#button-query-put").click(function(){
    $("#query-json-box").show();
    $("#query-query-box").show();
    $("#query-limit-box").hide();
  });

  $("#button-query-delete").click(function(){
    $("#query-json-box").hide();
    $("#query-query-box").show();
    $("#query-limit-box").hide();
  });

  $("#data-explorer-link").click(function(){
    $('#data-explorer').show();
    $('#query-path').val('');
    $("#query-response-area").hide();
  });

  var queryPath = '';
  function runCollectionQuery(){
    var method;


    //Select method to use
    if($('#button-query-get').prop('checked') ){
      method = 'GET';
    } else if($('#button-query-post').prop('checked')){
      method = 'POST';
    } else if($('#button-query-put').prop('checked')){
      method = 'PUT';
    } else if($('#button-query-delete').prop('checked')){
      method = 'DELETE';
    } else {
      alertModal("Notice", "Please select a method.");
      return;
    }


    //If jsonBody is empty fill it with empty brackets
    if($('#query-source').val() === '') {
      $("#query-source").val('{"name":"value"}');
    }
    getCollection(method);
  }

  window.Usergrid.console.getCollection = getCollection;

  function getCollection(method, path){
    //get the data to run the query
    if(!path){
      var path = $("#query-path").val();
    }
    if(method.toUpperCase() !== 'GET' && method.toUpperCase() !== 'DELETE'){
      var data = $("#query-source").val();
      try{
        validateJson();
        data = JSON.parse(data);
      } catch (e) {
        alertModal("Error", "There is a problem with your JSON.");
        return false;
      }
    }

    var params = {};
    var ql = $("#query-ql").val();
    params.ql = ql;
    if(method.toUpperCase() === 'GET'){
      var limit = $("#query-limit").val();
      params.limit = limit;
    }

    queryPath = path;

    queryObj = new Usergrid.Query(method, path, data, params, getCollectionCallback, function(response) { alertModal("Error", response) });
    runAppQuery(queryObj);
  }


function getCollectionCallback(response) {
  hidePagination('query-response');
  $('#query-response-area').show();
  if (response.action == 'post') {
    pageSelectCollections();
  }

  var path = response.path || "";
  path = "" + path.match(/[^?]*/);
  var path_no_slashes = "";
  try {
    path_no_slashes = response.path.replace(/\//g,'');
  } catch(e) {}

  $("#collections-link-buttons li").removeClass('active');
  $("#collections-link-button-"+path_no_slashes).addClass('active');

  if(response.action === ("delete")){
    getCollection("GET", path);
    return;
  }

  var slashes = (queryPath.split("/").length -1)
  if (response.entities.length == 1 && slashes > 1){
    generateBackToCollectionButton(response.path);
  } else {
    $('#back-to-collection').hide();
  }
  if (!slashes && queryPath.length > 0) {
    queryPath = "/" + queryPath;
  }

  $('#query-path').val(queryPath);

  $("#collection-type-field").html(response.path);
  var output = $('#query-response-table');
  if (response.entities) {
    if (response.entities.length < 1) {
      output.replaceWith('<table id="query-response-table" class="table"><tbody><tr class="zebraRows users-row"><td>No entities found</td></tr></table>');
    } else {
      //Inform the user of a valid query
      showQueryStatus('Done');

      var entity_type = response.entities [0].type;

      //showQueryCollectionView(path)
      //updateQueryTypeahead(response, 'query-path');


      var table = '<table id="query-response-table" class="table"><tbody><tr class="zebraRows users-row">' +
                  '<td class="checkboxo"><input type="checkbox" onclick="Usergrid.console.selectAllEntities(this);" /></td>';
      if (entity_type === 'user') {
        table += '<td class="gravatar50-td">&nbsp;</td>'+
          '<td class="user-details bold-header">username</td>'+
          '<td class="user-details bold-header">Display Name</td>';
      } else if (entity_type === 'group') {
        table += '<td class="user-details bold-header">Path</td>'+
          '<td class="user-details bold-header">Title</td>';
      } else if (entity_type === 'role') {
        table += '<td class="user-details bold-header">Title</td>'+
          '<td class="user-details bold-header">Rolename</td>';
      } else {
        table += '<td class="user-details bold-header">Name</td>';
      }
      table += '<td class="view-details">&nbsp;</td>' +
               '</tr></tbody></table>';
      output.replaceWith(table);
      for (i = 0; i < response.entities.length; i++) {
        var this_data = {}
        this_data.r = response.entities [i];

        //next get a table view of the object
        this_data.content = buildContentArea(response.entities [i]);


        //get a json representation of the object
        this_data.json = JSON.stringify(response.entities [i], null, 2);

        if (this_data.type === 'user') {
          if (!this_data.r.picture) {
            this_data.r.picture = window.location.protocol+ "//" + window.location.host + window.location.pathname + "images/user-photo.png"
          } else {
            this_data.r.picture = get_replacementGravatar(this_data.r.picture);
          }
        } else {
          if (!this_data.r.name) {
            this_data.r.name = '[No value set]';
          }
        }
        $.tmpl('apigee.ui.collection.table_rows.html', this_data).appendTo('#query-response-table');
      }

    }
  } else {
    output.replaceWith('<table id="query-response-table" class="table"><tbody><tr class="zebraRows users-row"><td>No entities found</td></tr></table>');
  }

  showPagination('query-response');
}


function buildContentArea(obj2) {
  function getProperties(obj, depth){
    depth = 1;
    var output = '';
    for (var property in obj) {
      if (depth ==1) { output += '<tr>';}
      if (obj.hasOwnProperty(property)){
        if (obj[property].constructor == Object || obj[property] instanceof Array) {

          var prop = (obj[property] instanceof Array)?property:'';
          //console.log('**Object -> '+property+': ');
          output += '<td>'+prop+'</td><td style="padding: 0"><table><tr>';
          output += getProperties(obj[property], depth);
          output += '</td></tr></table>';
        }
        else {
          //console.log(property + " " + obj[property]);
          output += '<td>'+property+'</td><td>'+obj[property]+'</td>';
        }
      }
      if (depth ==1) { output += '</tr>';}
    }
    return output;
  }
  var output = getProperties(obj2, '', 0);
  return '<table>' + output + '</table>';
}

  function activateQueryRowJSONButton() {
     $("#button-query-show-row-JSON").removeClass('disabled').addClass('active');
     $("#button-query-show-row-content").removeClass('active').addClass('disabled');
  }
  window.Usergrid.console.activateQueryRowJSONButton = activateQueryRowJSONButton;

  function activateQueryRowContentButton() {
     $("#button-query-show-row-JSON").removeClass('active').addClass('disabled');
     $("#button-query-show-row-content").removeClass('disabled').addClass('active');
  }
  window.Usergrid.console.activateQueryRowContentButton = activateQueryRowContentButton;

  function showQueryCollectionView() {
    $('#query-collection-info').show();
    $('#query-detail-info').hide();
    $('#query-ql-box').show();
    $('#back-to-collection').hide();
  }



  function generateBackToCollectionButton(returnPath) {
    var backButton = $('#back-to-collection');
    if(backButton.attr('onclick')){
      backButton.removeAttr('onclick');
    }
    backButton.attr('onclick',"Usergrid.console.getCollection('GET','" + returnPath+ "')");
    $('#back-to-collection').show();
  }

  $.fn.loadEntityCollectionsListWidget = function() {
    this.each(function() {
      var entityType = $(this).dataset('entity-type');
      var entityUIPlugin = "apigee_collections_" + entityType + "_list_item";
      if (!$(this)[entityUIPlugin]) {
        entityUIPlugin = "apigee_collections_entity_list_item";
      }
      $(this)[entityUIPlugin]();
    });
  };

  $.fn.loadEntityCollectionsDetailWidget = function() {
    this.each(function() {
      var entityType = $(this).dataset('entity-type');
      var entityUIPlugin = "apigee_collections_" + entityType + "_detail";
      if (!$(this)[entityUIPlugin]) {
        entityUIPlugin = "apigee_collections_entity_detail";
      }
      $(this)[entityUIPlugin]();
    });
    if (this.length === 1 ){
      hideEntityCheckboxes();
      hideEntitySelectButton();
    }
  };

  function hideEntityCheckboxes(){
    $(".listItem").hide();
    $(".listItem").attr('checked', true);
  }

  function hideEntitySelectButton(){
    $("#selectAllCollections").hide();
  }
  function showEntitySelectButton(){
    $("#selectAllCollections").show();
  }

  function getQueryResultEntity(id) {
    if (query_entities_by_id) {
      return query_entities_by_id[id];
    }
    return null;
  }
  window.Usergrid.console.getQueryResultEntity = getQueryResultEntity;

  function showQueryStatus(s, _type) {
    StatusBar.showAlert(s, _type);
  }

  function expandQueryInput() {
    $('#query-source').height(150);
    $('#button-query-shrink').show();
    $('#button-query-expand').hide();
    return false;
  }

  function shrinkQueryInput() {
    $('#query-source').height(60);
    $('#button-query-shrink').hide();
    $('#button-query-expand').show();
    return false;
  }

  InitQueryPanel();
  function InitQueryPanel(){
    $('#query-source').focus(function(){
        expandQueryInput();
        prepareQueryInput(this);
    });
    $('#query-source').keyup(function(){activateJSONValidator('#button-query-validate', '#query-source');})
    $('#button-query-shrink').click(shrinkQueryInput);
    $('#button-query-expand').click(expandQueryInput);

    $('#button-query').click(function(){runCollectionQuery(); return false;});

  }

  function prepareQueryInput(selector) {
    var queryInput = $(selector);
    if( queryInput.val() === ""){
      queryInput.val("{\n\n}");
    }
  }

  function activateJSONValidator(valButton, jsonArea) {
    var validatorButton = $(valButton);
    var textArea = $(jsonArea)
    if(validatorButton.hasClass('disabled')){
      validatorButton.removeClass('disabled');
      validatorButton.click(function() {validateJson();return false;});
    } else if(textArea.val() === "") {
      validatorButton.addClass('disabled');
      validatorButton.unbind('click');
    }
  }

  function showMoreQueryOptions() {
    $('.query-more-options').show();
    $('.query-less-options').hide();
    $('#query-ql').val("");
    $('#query-source').val("{ }");
  }

  function hideMoreQueryOptions() {
    $('.query-more-options').hide();
    $('.query-less-options').show();
    $('#query-ql').val("");
    $('#query-source').val("");
  }

  function toggleMoreQueryOptions() {
    $('.query-more-options').toggle();
    $('.query-less-options').toggle();
    $('#query-ql').val("");
    $('#query-source').val("");
  }

  $('#button-query-more-options').click(function() {
    toggleMoreQueryOptions();
    return false;
  });

  $('#button-query-less-options').click(function() {
    toggleMoreQueryOptions();
    return false;
  });

  $('#query-source').keydown(function(e) {
    var key = e.keyCode || e.which;

    if ((key == 9 || key ===13)) {
      e.preventDefault();
      //Get cursor position
      var start = this.selectionStart;
      var end = this.selectionEnd;
      var field = $(this);
      var value = field.val();
      //insert Text and indentation
      field.val(value.substring(0, start) + '\r  ' + value.substring(end));
      //return cursor to its position
      this.selectionStart = this.selectionEnd = start + 1;
    }
  });

  function validateJson() {
    try {
      var result = JSON.parse($('#query-source').val());
      if (result) {
        showQueryStatus('JSON is valid!');
        $('#query-source').val(
            JSON.stringify(result, null, "  "));
        return result;
      }
    } catch(e) {
      showQueryStatus(e.toString(), "error");
    }
    return false;
  };

  window.Usergrid.console.doChildClick = function(event) {
    var path = new String($('#query-path').val());
    if (!path.endsWith("/")) {
      path += "/";
    }
    path += event.target.innerText;
    $('#query-path').val(path);
  };

  var queryQl = $('#query-ql');
  queryQl.typeahead({source:indexes});

  function doBuildIndexMenu() {
    queryQl.data('typeahead').source = indexes;
  }

  $('#delete-entity-link').click(deleteEntity);

  function deleteEntity(e) {
    e.preventDefault();
    var items = $('#query-response-table input[class=listItem]:checked');
    if(!items.length){
      alertModal("Please, first select the entities you want to delete.");
      return;
    }
    var itemsCount = items.size();
    confirmDelete(function(){
      items.each(function() {
        var path = $(this).attr('name');
        runAppQuery(new Usergrid.Query("DELETE", path, null, null,
          function() {
            itemsCount--;
            if(itemsCount==0){
              getCollection('GET');
            }},
          function() { alertModal("Unable to delete: " + path); }
        ));
      });
    });
  }

  function requestIndexes(path){
    var data = {};
    runAppQuery(new Usergrid.Query("GET", path + "/indexes", null, null,
      function(response) {
        if(response && response.data) {
          data = response;
        }
        buildIndexDropdown('query-collections-indexes-list', data);

      }));
  }

  function buildIndexDropdown(menuId, indexes) {
    var menu = $("#" + menuId);
    menu.empty();
    $.tmpl('apigee.ui.collections.query.indexes.html', indexes).appendTo(menu);
  }

  function appendToCollectionsQuery(message){
    var queryTextArea = $("#query-ql");
    queryTextArea.val(queryTextArea.val()+ " " + message );
  }
  window.Usergrid.console.appendToCollectionsQuery = appendToCollectionsQuery;

  /*******************************************************************
   *
   * Organization Home
   *
   ******************************************************************/

requestApplications();
  function pageSelectHome() {
    setupMenu();
   // requestApplications();
    requestAdmins();
    displayOrganizationName(Usergrid.ApiClient.getOrganizationName());
    requestOrganizationCredentials();
    requestAdminFeed();
  }
  window.Usergrid.console.pageSelectHome = pageSelectHome;


  function displayApplications(response) {
    applications = {};
    applications_by_id = {};
    var appMenu = $('.applications-menu');
    var appList = $('table#organization-applications-table');
    appMenu.empty();
    appList.empty();

    if (response.data) {
      applications = response.data;
      var count = 0;
      var applicationNames = keys(applications).sort();
      var data = [];
      var appMenuTmpl = $('<li><a >${name}</a></li>');

      for (var i in applicationNames) {
        var name = applicationNames[i];
        var uuid = applications[name];
        data.push({uuid:uuid, name:name.split("/")[1]});
        count++;
        applications_by_id[uuid] = name.split("/")[1];
      }

      if (count) {
        $.tmpl('apigee.ui.applications.table_rows.html', data).appendTo(appList);
        appMenuTmpl.tmpl(data).appendTo(appMenu);
        appMenuTmpl.tmpl(data)
        appMenu.find("a").click(function selectApp(e) {
          var link = $(this);
          pageSelect(link.tmplItem().data.name);
          Usergrid.Navigation.router.navigateTo('dashboard');
        });

        appList.find("a").click(function selectApp(e) {
          e.preventDefault();
          var link = $(this);
          pageSelect(link.tmplItem().data.name);
          Usergrid.Navigation.router.navigateTo('dashboard');
        });
        enableApplicationPanelButtons();
      }
      appMenu.append('<li class="divider"></li>');
      appMenu.append('<li><a class="" data-toggle="modal" href="#dialog-form-new-application"> <strong>+</strong> New Application</a></li>');
    }

    if(appList.is(":empty")){
      appList.html('<div class="alert user-panel-section">No applications created.</div>');
      appMenu.html('<li>--No Apps--</li>');
      forceNewApp();
    }

    var appName = Usergrid.ApiClient.getApplicationName();
    if (!appName) {
      selectFirstApp();
    } else {
      setNavApplicationText();
    }
  }

  function requestApplications() {
    var sectionApps = $('#organization-applications-table');
    sectionApps.empty().html('<div class="alert alert-info user-panel-section">Loading...</div>');
    runManagementQuery(new Usergrid.Query("GET","organizations/" + Usergrid.ApiClient.getOrganizationName() + "/applications", null, null,
      displayApplications,
      function() { sectionApps.html('<div class="alert user-panel-section">Unable to retrieve application list.</div>'); }
    ));
  }

  function selectFirstApp() {
    //get the currently specified app name
    var appName = Usergrid.ApiClient.getApplicationName();
    //and make sure we it is in one of the current orgs
    var app = Usergrid.organizations.getItemByName(appName);
    if(appName && app) {
      Usergrid.ApiClient.setApplicationName(appName);
      pageSelect(appName);
    } else {
      //we need to select an app, so get the current org name
      var orgName = Usergrid.ApiClient.getOrganizationName();
      //get a reference to the org object by using the name
      var org = Usergrid.organizations.getItemByName(orgName);
      //get a handle to the first app in the org
      app = org.getFirstItem();
      //store the new app in the client
      Usergrid.ApiClient.setApplicationName(app.getName());
      pageSelect(app.getName());
    }
    setNavApplicationText();
  }

  function displayAdmins(response) {
    var sectionAdmins = $('#organization-admins-table');
    sectionAdmins.empty();
    if (response.data) {
      var admins = response.data;
      admins = admins.sort();
      for (var i in admins) {
        var admin = admins[i];
        admin.gravatar = get_gravatar(admin.email, 20);
        $.tmpl('apigee.ui.admins.table_rows.html', admin).appendTo(sectionAdmins);
      }
    }
    if(sectionAdmins.is(':empty')){
      sectionAdmins.html('<div class="alert user-panel-section">No organization administrators.</div>');
    }
  }

  function requestAdmins() {
    var sectionAdmins =$('#organization-admins-table');
    sectionAdmins.empty().html('<div class="alert alert-info user-panel-section">Loading...</div>');
    runManagementQuery(new Usergrid.Query("GET","organizations/" + Usergrid.ApiClient.getOrganizationName()  + "/users", null, null,
      displayAdmins,
      function() {sectionAdmins.html('<div class="alert user-panel-section">Unable to retrieve admin list</div>');
    }));
  }

  $(document).on('click', '.toggleableSP', function() {
    $(this).parent().find('.toggleableSP').toggle();
    return false
  });

  function get_gravatar(email, size) {
    var size = size || 50;
    return 'https://secure.gravatar.com/avatar/' + MD5(email) + '?s=' + size + encodeURI("&d=http://apigee.com/usergrid/images/user_profile.png");
  }

  function get_replacementGravatar(picture) {
    picture = picture.replace(/^http:\/\/www.gravatar/i, 'https://secure.gravatar');
    //note: changing this to use the image on apigee.com - since the gravatar default won't work on any non-public domains such as localhost
    //this_data.picture = this_data.picture + encodeURI("?d="+window.location.protocol+"//" + window.location.host + window.location.pathname + "images/user_profile.png");
    picture = picture + encodeURI("?d=http://apigee.com/usergrid/images/user_profile.png");
    return picture;
  }

  function displayAdminFeed(response) {

    var sectionActivities = $('#organization-feed-table');
    sectionActivities.empty();

    if (response.entities && (response.entities.length > 0)) {
      var activities = response.entities;
      for (var i in activities) {
        var activity = activities[i];

        // Next part is a hack. The API should return the email and title cleanly.
        var title_tmp = $("<span/>", {html: activity.title});
        activity.actor.email  = title_tmp.find('a').attr('mailto');
        title_tmp.find('a').remove();
        activity.title = title_tmp.text();
        // hack ends here

        activity.actor.gravatar = get_gravatar(activity.actor.email, 20);
        $.tmpl('apigee.ui.feed.table_rows.html', activity).appendTo(sectionActivities);
      }
    }

    if (sectionActivities.is(":empty")) {
      sectionActivities.html('<div class="alert user-panel-section">No activities.</div>');
    }
  }

  function requestAdminFeed() {
    var section =$('#organization-activities');
    section.empty().html('<div class="alert alert-info">Loading...</div>');
    runManagementQuery(new Usergrid.Query("GET","orgs/" + Usergrid.ApiClient.getOrganizationName()  + "/feed", null, null, displayAdminFeed,
      function() { section.html('<div class="alert">Unable to retrieve feed.</div>'); }));
  }
  window.Usergrid.console.requestAdminFeed = requestAdminFeed;

  var organization_keys = { };

  function requestOrganizationCredentials() {
    $('#organization-panel-key').html('<div class="alert alert-info marginless">Loading...</div>');
    $('#organization-panel-secret').html('<div class="alert alert-info marginless">Loading...</div>');
    runManagementQuery(new Usergrid.Query("GET",'organizations/'+ Usergrid.ApiClient.getOrganizationName()  + "/credentials", null, null,
      function(response) {
        $('#organization-panel-key').html(response.credentials.client_id);
        $('#organization-panel-secret').html(response.credentials.client_secret);
        organization_keys = {client_id : response.credentials.client_id, client_secret : response.credentials.client_secret};
      },
      function() {
        $('#organization-panel-key').html('<div class="alert marginless">Unable to load...</div>');
        $('#organization-panel-secret').html('<div class="alert marginless">Unable to load...</div>');
      }));
  }

  function newOrganizationCredentials() {
    $('#organization-panel-key').html('<div class="alert alert-info marginless">Loading...</div>');
    $('#organization-panel-secret').html('<div class="alert alert-info marginless">Loading...</div>');
    runManagementQuery(new Usergrid.Query("POST",'organizations/' + Usergrid.ApiClient.getOrganizationName()   + "/credentials",null, null,
      function(response) {
        $('#organization-panel-key').html(response.credentials.client_id);
        $('#organization-panel-secret').html(response.credentials.client_secret);
        organization_keys = {client_id : response.credentials.client_id, client_secret : response.credentials.client_secret};
      },
      function() {
        $('#organization-panel-key').html('<div class="alert marginless">Unable to load...</div>');
        $('#organization-panel-secret').html('<div class="alert marginless">Unable to load...</div>');
      }
    ));
  }
  window.Usergrid.console.newOrganizationCredentials = newOrganizationCredentials;

  function updateTips(t) {
    tips.text(t).addClass('ui-state-highlight');
    setTimeout(function() {
      tips.removeClass('ui-state-highlight', 1500);
    },
    500);
  }

  function checkLength(o, n, min, max) {
    if (o.val().length > max || o.val().length < min) {
      o.addClass('ui-state-error');
      updateTips("Length of " + n + " must be between " + min
          + " and " + max + ".");
      return false;
    } else {
      return true;
    }
  }

  function checkRegexp(o, regexp, n) {
    if (! (regexp.test(o.val()))) {
      o.addClass('ui-state-error');
      updateTips(n);
      return false;
    } else {
      return true;
    }
  }

  function checkTrue(o, t, n) {
    if (!t) {
      o.addClass('ui-state-error');
      updateTips(n);
    }
    return t;
  }

  var tips = $('.validateTips');

  /*******************************************************************
   *
   * Modals
   *
   ******************************************************************/

  function alertModal(header,message) {
    $('#alertModal h4').text(header);
    $('#alertModal p').text(message);
    $('#alertModal').modal('show');
  }

  //use like: alertBanner("Oh no!", "Say it isn't so!!");
  //or like: alertBanner("Oh no!", "Say it isn't so!!", 5000); //will auto-close in 5 seconds
  function alertBanner(header, message, timeout) {
    $('#alert-error-header').html(header);
    $('#alert-error-message').html(message);
    $('#alert-error-message-container').show();
    if (timeout) {
      var alertTimer = setInterval(function(){
          $('#alert-error-message-container').hide();
          window.clearInterval(alertTimer);
        },timeout);
    }
  }


  function hideModal(id){
    $(id).hide();
  }

  function confirmAction(header, message, callback){
    var form = $('#confirmAction');

    form.find('h4').text(header);
    form.find('p').text(message);
    form.unbind('submit');

    form.submit(function(){
      form.modal("hide");
      callback();

      return false;
    });

    form.modal('show');
  }

  function resetModal(){
    this.reset();
    var form = $(this);
    formClearErrors(form);
  }

  function focusModal(){
    $(this).find('input:first').focus();
  }

  function submitModal(e){
    e.preventDefault();
  }

  $('form.modal').on('hidden',resetModal).on('shown',focusModal).submit(submitModal);
  $('#dialog-form-new-application').submit(submitApplication);
  $('#dialog-form-force-new-application').submit(submitApplication);
  $('#dialog-form-new-admin').submit(submitNewAdmin);
  $('#dialog-form-new-organization').submit(submitNewOrg);
  $('#dialog-form-new-user').submit(submitNewUser);
  $('#dialog-form-new-role').submit(submitNewRole);
  $('#dialog-form-new-collection').submit(submitNewCollection);
  $('#dialog-form-new-group').submit(submitNewGroup);
  $('#dialog-form-add-group-to-user').submit(submitAddGroupToUser);
  $('#dialog-form-add-user-to-group').submit(submitAddUserToGroup);
  $('#dialog-form-add-user-to-role').submit(submitAddUserToRole);
  $('#dialog-form-add-role-to-user').submit(function() {
    submitAddRoleToUser(current_roleName, current_roleTitle)});
  $('#dialog-form-add-group-to-role').submit(function() {
    submitAddGroupToRole(current_roleName, current_roleTitle)});
  $('#dialog-form-add-role-to-group').submit(submitAddRoleToGroup);
  $('#dialog-form-follow-user').submit(submitFollowUser);

  function checkLength2(input, min, max) {
    if (input.val().length > max || input.val().length < min) {
      var tip = "Length must be between " + min + " and " + max + ".";
      validationError(input,tip);
      return false;
    }

    return true;
  }

  function checkRegexp2(input, regexp, tip) {
    if (! (regexp.test(input.val()))) {
      validationError(input,tip);
      return false;
    }
    return true;
  }

  function checkTrue2(input, exp, tip) {
    if (!exp) {
      validationError(input,tip);
      return false;
    }

    return true;
  }

  function confirmDelete(callback){
    var form = $('#confirmDialog');
    if (form.submit) {
      form.unbind('submit');
    }

    form.submit(function(e){
      e.preventDefault();
      form.modal('hide');
    }).submit(callback);

    form.modal('show');
  }

  function validationError(input, tip){
    input.focus();
    input.parent().parent().addClass("error");
    input.parent().parent().find(".help-block").text(tip).addClass("alert-error").addClass("alert").show();
  }

  $.fn.serializeObject = function() {
    var o = {};
    var a = this.serializeArray();

    $.each(a, function() {
      if (o[this.name]) {
        if (!o[this.name].push) {
          o[this.name] = [o[this.name]];
        }
        o[this.name].push(this.value || '');
      } else {
        o[this.name] = this.value || '';
      }
    });

    return o;
  };

  function formClearErrors(form){
    form.find('.ui-state-error').removeClass('ui-state-error');
    form.find('.error').removeClass('error');
    form.find('.help-block').empty().hide();
  }

  function submitApplication() {
    var form = $(this);
    formClearErrors(form);

    var new_application_name = $(this).find('.new-application-name');

    var bValid = checkLength2(new_application_name, 4, 80)
      && checkRegexp2(new_application_name, usernameRegex, usernameAllowedCharsMessage);

    if (bValid) {
      runManagementQuery(new Usergrid.Query("POST","organizations/" + Usergrid.ApiClient.getOrganizationName()  + "/applications", form.serializeObject(), null,
        function(response) {
          for (var appName in response.data) { break; }
          var appTitle = appName.split("/")[1];
          var currentOrg = Usergrid.ApiClient.getOrganizationName();
          Usergrid.organizations.getItemByName(currentOrg).addItem(new Usergrid.Application(appTitle, response.data[appName]));
          pageSelect(appTitle);
          requestApplications();
        },
        function() {
          closeErrorMessage = function() {
            $('#home-messages').hide();
          };
          var closebutton = '<a  onclick="closeErrorMessage();" class="close">&times;</a>'
          $('#home-messages').text("Unable to create application: ").prepend(closebutton).addClass('alert-error').show();
      }));
      $(this).modal('hide');
    }
  }

  function submitNewAdmin() {
    var form = $(this);
    formClearErrors(form);

    var new_admin_email = $('#new-admin-email');
    var bValid = checkLength2(new_admin_email, 6, 80)
      && checkRegexp2(new_admin_email,emailRegex, emailAllowedCharsMessage);
    if (bValid) {
      var data = form.serializeObject();
      runManagementQuery(new Usergrid.Query("POST","organizations/" + Usergrid.ApiClient.getOrganizationName() + "/users", data, null,
        requestAdmins,
        function () { alertModal("Error", "Unable to create admin"); }
      ));
      $(this).modal('hide');
    }
  }

  function addOrganizationToList(orgName) {
    runManagementQuery(new Usergrid.Query("GET","orgs/" + orgName, null, null,
      function(response) {
        var orgName = response.organization.name;
        var orgUUID = response.organization.uuid;
        organization = new Usergrid.Organization(orgName, orgUUID);
        var apps = response.organization.applications;
        for(app in apps) {
          var appName = app.split("/")[1];
          //grab the id
          var appUUID = response.organization.applications[app];
          //store in the new Application object
          application = new Usergrid.Application(appName, appUUID);
          organization.addItem(application);
        }
        //add organization to organizations list
        Usergrid.organizations.addItem(organization);
        requestAccountSettings();
        setupOrganizationsMenu();
      },
      function() { alertModal("Error", "Unable to get organization" + orgName);
      }
    ));
  }

  function submitNewOrg() {
    var form = $(this);
    formClearErrors(form);

    var new_organization_name = $('#new-organization-name');
    var new_organization_name_val = $('#new-organization-name').val();
    var bValid = checkLength2(new_organization_name, 4, 80)
      && checkRegexp2(new_organization_name, organizationNameRegex, organizationNameAllowedCharsMessage);

    if (bValid) {
      var data = form.serializeObject();
      runManagementQuery(new Usergrid.Query("POST","users/" + Usergrid.userSession.getUserUUID() + "/organizations", data, null,
        function() {
          addOrganizationToList(new_organization_name_val);
        },
        function() { alertModal("Error", "Unable to create organization"); }
      ));
      $(this).modal('hide');
    }
  }
 //TODO: the organization, and required fields for this method, are hidden. There is no quick way to check variable names and order
 /*
  * Needed fields:
  * username:
  * name: FULL NAME
  * email:
  * password:
  */
  function submitNewUser() {
    var form = $(this);
    formClearErrors(form);

    var email = $('#new-user-email');
    var username = $('#new-user-username');
    var fullname = $('#new-user-fullname');
    var password = $('#new-user-password');
    var validate_password = $('#new-user-validate-password');

    var bValid =
      //Fullname can is not required.
      checkLength2(fullname , 0, 80)
      && ( fullname.val() === "" || checkRegexp2(fullname, nameRegex, nameAllowedCharsMessage) )
      //Username IS required
      && checkRegexp2(username, usernameRegex, usernameAllowedCharsMessage)
      //Email is NOT required
      && ( checkLength2(email, 6, 80) )
      && ( email.val() === "" || checkRegexp2(email,emailRegex, emailAllowedCharsMessage) )
      && ( checkLength2(password ,0 ,0) || checkLength2(password, 1, 64) )
      && ( password.val() === "" || checkRegexp2(password,passwordRegex, passwordAllowedCharsMessage) )
      && ( checkTrue2(password, (password.val() === validate_password.val()), passwordMismatchMessage));

    if (bValid) {
      var data = form.serializeObject();
      runAppQuery(new Usergrid.Query("POST", 'users', data, null,
        function() {
          getUsers();
          closeErrorMessage = function() {
            $('#users-messages').hide();
          };
          var closebutton = '<a  onclick="closeErrorMessage();" class="close">&times;</a>';
          $('#users-messages')
            .text("User created successfully.")
            .prepend(closebutton)
            .removeClass()
            .addClass('alert alert-warning')
            .show();
        },
        function() {
          closeErrorMessage = function() {
            $('#users-messages').hide();
          };
          var closebutton = '<a  onclick="closeErrorMessage();" class="close">&times;</a>'
          $('#users-messages')
          .text("Unable to create user")
          .prepend(closebutton)
          .removeClass()
          .addClass('alert alert-error')
          .show();
        }
      ));

      $(this).modal('hide');
    }
  }

  function submitNewRole() {
    var form = $(this);
    formClearErrors(form);

    var new_role_name = $('#new-role-name');
    var new_role_title = $('#new-role-title');

    var bValid = checkLength2(new_role_name, 1, 80)
      && checkRegexp2(new_role_name, roleRegex, roleAllowedCharsMessage)
      && checkLength2(new_role_title, 1, 80)
      && checkRegexp2(new_role_title,titleRegex, titleAllowedCharsMessage);

    if (bValid) {
      var data = form.serializeObject();
      runAppQuery(new Usergrid.Query("POST", "role", data, null,
        function() {
          getRoles();
          closeErrorMessage = function() {
            $('#roles-messages').hide();
          };
          var closebutton = '<a  onclick="closeErrorMessage();" class="close">&times;</a>'
          $('#roles-messages')
            .text("Role created successfully.")
            .prepend(closebutton)
            .removeClass()
            .addClass('alert alert-warning')
            .show();
          },
          function() {
            closeErrorMessage = function() {
            $('#roles-messages').hide();
          };
          var closebutton = '<a  onclick="closeErrorMessage();" class="close">&times;</a>'
          $('#roles-messages')
            .text("Unable to create user")
            .prepend(closebutton)
            .removeClass()
            .addClass('alert alert-error')
            .show();
        }
      ));

      $(this).modal('hide');
    }
  }

  function submitNewCollection() {
    var form = $(this);
    formClearErrors(form);

    var new_collection_name = $('#new-collection-name');

    var bValid = checkLength2(new_collection_name, 4, 80)
      && checkRegexp2(new_collection_name, alphaNumRegex, alphaNumAllowedCharsMessage);

    if (bValid) {
      var data = form.serializeObject();
      var collections = {};
      collections[data.name] = {};
      var metadata = {
        metadata: {
          collections: collections
        }
      }
      runAppQuery(new Usergrid.Query("PUT", "", metadata, null,
        function() {
          getCollections();
          closeErrorMessage = function() {
            $('#collections-messages').hide();
          };
          var closebutton = '<a  onclick="closeErrorMessage();" class="close">&times;</a>'
          $('#collections-messages')
            .text("Collection created successfully.")
            .prepend(closebutton)
            .removeClass()
            .addClass('alert alert-warning')
            .show();
        },
        function() {
          closeErrorMessage = function() {
            $('#collections-messages').hide();
          };
          var closebutton = '<a  onclick="closeErrorMessage();" class="close">&times;</a>'
          $('#collections-messages')
            .text("Unable to create user")
            .prepend(closebutton)
            .removeClass()
            .addClass('alert alert-error')
            .show();
        }
      ));

      $(this).modal('hide');
    }
  }

  function submitNewGroup() {
    var form = $(this);
    formClearErrors(form);

    var new_group_title = $('#new-group-title');
    var new_group_path = $('#new-group-path');

    var bValid = checkLength2(new_group_title, 1, 80)
      && checkRegexp2(new_group_title, nameRegex, nameAllowedCharsMessage)
      && checkLength2(new_group_path, 1, 80)
      && checkRegexp2(new_group_path, pathRegex, pathAllowedCharsMessage);

    if (bValid) {
      var data = form.serializeObject();
      runAppQuery(new Usergrid.Query("POST", "groups", data, null,
        function() {
          getGroups();
          closeErrorMessage = function() {
          $('#groups-messages').hide();
        };
        var closebutton = '<a  onclick="closeErrorMessage();" class="close">&times;</a>'
        $('#groups-messages')
          .text("Group created successfully.")
          .prepend(closebutton)
          .removeClass()
          .addClass('alert alert-warning')
          .show();
        },
        function() {
          closeErrorMessage = function() {
            $('#groups-messages').hide();
          };
          var closebutton = '<a  onclick="closeErrorMessage();" class="close">&times;</a>'
          $('#groups-messages').text("Unable to create group").prepend(closebutton).addClass('alert-error').show();
        }
      ));

      $(this).modal('hide');
    }
  }

  function submitAddGroupToUser() {
    var form = $(this);
    formClearErrors(form);
    var add_group_groupname = $('#search-group-name-input');
    var bValid = checkLength2(add_group_groupname, 1, 80)
      && checkRegexp2(add_group_groupname, usernameRegex, usernameAllowedCharsMessage);

    if (bValid) {
      userId = $('#search-group-userid').val();
      groupId = $('#search-group-name-input').val();

      runAppQuery(new Usergrid.Query("POST", "/groups/" + groupId + "/users/" + userId, null, null,
        function() { requestUser(userId); },
        function() { alertModal("Error", "Unable to add group to user"); }
      ));

      $(this).modal('hide');
    }
  }

  function submitAddUserToGroup() {
    var form = $(this);
    formClearErrors(form);
    var add_user_username = $('#search-user-name-input');
    var bValid = checkLength2(add_user_username, 1, 80)
      && checkRegexp2(add_user_username, usernameRegex, usernameAllowedCharsMessage);

    if (bValid) {
      userId = $('#search-user-name-input').val();
      groupId = $('#search-user-groupid').val();
      runAppQuery(new Usergrid.Query("POST", "/groups/" + groupId + "/users/" + userId, null, null,
        function() { requestGroup(groupId); },
        function() { alertModal("Error", "Unable to add user to group"); }
      ));
      $(this).modal('hide');
    }
  }

  function submitFollowUser(){
    var form = $(this);
    formClearErrors(form);
    var username = $('#search-follow-username-input');
    var bValid = checkLength2(username, 1, 80) && checkRegexp2(username, usernameRegex, usernameAllowedCharsMessage);
    if (bValid) {
      var followingUserId = $('#search-follow-username').val();
      var followedUserId = $('#search-follow-username-input').val();
      runAppQuery(new Usergrid.Query("POST", "/users/" + followingUserId + "/following/user/" + followedUserId, null, null,
        function() { pageSelectUserGraph(followingUserId)},
        function() {alertModal("Error", "Unable to follow User");}
      ));
      $(this).modal('hide');
    }
  }

  function submitAddRoleToUser(roleName, roleTitle) {
    var form = $(this);
    formClearErrors(form);
    var roleIdField = $('#search-roles-user-name-input');
    var bValid = checkLength2(roleIdField, 1, 80) && checkRegexp2(roleIdField, usernameRegex, usernameAllowedCharsMessage)
    var username = $('#search-roles-user-name-input').val();
    if (bValid) {
      runAppQuery(new Usergrid.Query("POST", "/roles/" + roleName + "/users/" + username, null, null,
        function() { pageSelectRoleUsers(roleName, roleTitle); },
        function() { alertModal("Error", "Unable to add user to role"); }
      ));
      $('#dialog-form-add-role-to-user').modal('hide');
    }
  }

  function submitAddUserToRole() {
    var form = $(this);
    formClearErrors(form);

    var roleIdField = $('#search-role-name-input');
    var bValid = checkLength2(roleIdField, 1, 80)
      && checkRegexp2(roleIdField, roleRegex, roleAllowedCharsMessage)

    var username = $('#role-form-username').val();
    var roleId = $('#search-role-name-input').val();
    // role may have a preceding or trailing slash, remove it
    roleId = roleId.replace('/','');
    if (bValid) {
      runAppQuery(new Usergrid.Query("POST", "/roles/" + roleId + "/users/" + username, null, null,
        function() { pageSelectUserPermissions(username); },
        function() { alertModal("Error", "Unable to add user to role"); }
      ));

      $(this).modal('hide');
    }
  }

  function deleteUsersFromRoles(username) {
    var items = $('#users-permissions-response-table input[class^=listItem]:checked');
    if(!items.length){
      alertModal("Error", "Please, first select the roles you want to delete for this user.");
      return;
    }

    confirmDelete(function(){
        items.each(function() {
          var roleName = $(this).attr("value");
          runAppQuery(new Usergrid.Query("DELETE", "/roles/" + roleName + "/users/" + username, null, null,
            function() { pageSelectUserPermissions (username); },
            function() { alertModal("Error", "Unable to remove user from role"); }
          ));
      });
    });
  }
  window.Usergrid.console.deleteUsersFromRoles = deleteUsersFromRoles;

  function deleteRoleFromUser(roleName, roleTitle) {
    var items = $('#role-users input[class^=listItem]:checked');
    if(!items.length){
      alertModal("Error", "Please, first select the users you want to delete from this role.");
        return;
    }

    confirmDelete(function(){
        items.each(function() {
          var username = $(this).attr("value");
          runAppQuery(new Usergrid.Query("DELETE", "/roles/" + roleName + "/users/" + username, null, null,
            function() { pageSelectRoleUsers (roleName, roleTitle); },
            function() { alertModal("Error", "Unable to remove user from role"); }
          ));
      });
    });
  }
  window.Usergrid.console.deleteRoleFromUser = deleteRoleFromUser;

  function removeUserFromGroup(userId) {
    var items = $('#user-panel-memberships input[class^=listItem]:checked');
    if(!items.length){
      alertModal("Error", "Please, first select the groups you want to delete for this user.")
        return;
    }
    confirmDelete(function(){
      items.each(function() {
        var groupId = $(this).attr("value");
        runAppQuery(new Usergrid.Query("DELETE", "/groups/" + groupId + "/users/" + userId, null, null,
          function() { pageSelectUserGroups (userId); },
          function() { alertModal("Error", "Unable to remove user from group"); }
        ));
      });
    });
  }
  window.Usergrid.console.removeUserFromGroup = removeUserFromGroup;

  function removeGroupFromUser(groupId) {
    var items = $('#group-panel-memberships input[class^=listItem]:checked');
    if (!items.length) {
      alertModal("Error", "Please, first select the users you want to from this group.");
      return;
    }

    confirmDelete(function(){
      items.each(function() {
        var userId = $(this).attr("value");
        runAppQuery(new Usergrid.Query("DELETE", "/groups/" + groupId + "/users/" + userId, null, null,
          function() { pageSelectGroupMemberships (groupId); },
          function() { alertModal("Error", "Unable to remove user from group"); }
        ));
      });
    });
  }
  window.Usergrid.console.removeGroupFromUser = removeGroupFromUser;

  function deleteRolesFromGroup(roleId, rolename) {
    var items = $('#group-panel-permissions input[class^=listItem]:checked');
    if(!items.length){
      alertModal("Error", "Please, first select the roles you want to delete from this group.")
        return;
    }

    confirmDelete(function(){
      items.each(function() {
        var roleId = $(this).attr("value");
        var groupname = $('#role-form-groupname').val();
        runAppQuery(new Usergrid.Query("DELETE", "/roles/" + roleId + "/groups/" + groupname, null, null,
          function() { pageSelectGroupPermissions(groupname); },
          function() { alertModal("Error", "Unable to remove role from group"); }
        ));
      });
    });
  }
  window.Usergrid.console.deleteRolesFromGroup = deleteRolesFromGroup;

  function submitAddRoleToGroup() {
    var form = $(this);
    formClearErrors(form);

    var roleIdField = $('#search-groups-role-name-input');
    var bValid = checkLength2(roleIdField, 1, 80)
      && checkRegexp2(roleIdField, roleRegex, roleAllowedCharsMessage)

    var groupname = $('#role-form-groupname').val();
    var roleId = $('#search-groups-role-name-input').val();
    // role may have a preceding or trailing slash, remove it
    roleId = roleId.replace('/','');

    if (bValid) {
      runAppQuery(new Usergrid.Query("POST", "/groups/" + groupname + "/roles/" + roleId, null, null,
        function() { pageSelectGroupPermissions(groupname); },
        function() { alertModal("Error", "Unable to add user to role"); }
      ));
    $(this).modal('hide');
    }
  }

  /*******************************************************************
   *
   * Generic page select
   *
   ******************************************************************/
  function pageSelect(name) {
    if (name) {
      //the following 3 lines are just a safety check, we could just store the name
      //get the current org name
      var currentOrg = Usergrid.ApiClient.getOrganizationName();
      //get a reference to the current org object by using the name
      var org = Usergrid.organizations.getItemByName(currentOrg);
      //get a reference to the specified app by name
      var app = org.getItemByName(name);
      //store the name
      Usergrid.ApiClient.setApplicationName(app.getName());
    }
    setNavApplicationText();
    getCollections();
    query_history = [];
  }
  window.Usergrid.console.pageSelect = pageSelect;


  /*******************************************************************
   *
   * Application
   *
   ******************************************************************/

  function pageSelectApplication() {
    pageSelect();
    requestApplicationUsage();
  }
  window.Usergrid.console.pageSelectApplication = pageSelectApplication;

  function updateApplicationDashboard(){
    var data = new google.visualization.DataTable();
    data.addColumn('string', 'Entity');
    data.addColumn('number', 'Count');
    var rows = [];
    var t = '<table class="table table-bordered" id="application-panel-entity-counts">';
    var collectionNames = keys(applicationData.Collections).sort();

    var entity_count = 0;
    for (var i in collectionNames) {
      var collectionName = collectionNames[i];
      var collection = applicationData.Collections[collectionName];
      var row = [collectionName, {v: collection.count}];
      rows.push(row);
      t += '<tr class="zebraRows"><td>' + collection.count + '</td><td>' + collectionName + '</td></tr>';
      entity_count += collection.count;
    }
    t += '<tr id="application-panel-entity-total"><th>' + entity_count + '</th><th>entities total</th></tr>';
    t += '</table>';
    data.addRows(rows);

    new google.visualization.PieChart(
      document.getElementById('application-panel-entity-graph')).
      draw(data, {
        height: 200,
        is3D: true,
        backgroundColor: backgroundGraphColor
      }
    );

    $('#dashboard-panel #application-panel-text').html(t);
  }

  function requestApplicationUsage() {
    $('#application-entities-timeline').html("");
    $('#application-cpu-time').html("");
    $('#application-data-uploaded').html("");
    $('#application-data-downloaded').html("");
    var params = {};
    params.start_time = Math.floor(new Date().getTime() / 1209600000) * 1209600000;
    params.end_time = start_timestamp + 1209600000;
    params.resolution = "day";
    params.counter = ["application.entities", "application.request.download", "application.request.time", "application.request.upload"];
    params.pad = true;

    runAppQuery(new Usergrid.Query("GET", "counters", null, params,
      function(response) {
        var usage_counters = response.counters;

        if (!usage_counters) {
          $('#application-entities-timeline').html("");
          $('#application-cpu-time').html("");
          $('#application-data-uploaded').html("");
          $('#application-data-downloaded').html("");
          return;
        }

        var graph_width = 350;
        var graph_height = 100;
        var data = new google.visualization.DataTable();
        data.addColumn('date', 'Time');
        data.addColumn('number', 'Entities');
        data.addRows(15);

        for (var i in usage_counters[0].values) {
        data.setCell(parseInt(i), 0, new Date(usage_counters[0].values[i].timestamp));
        data.setCell(parseInt(i), 1, usage_counters[0].values[i].value);
        }

        new google.visualization.LineChart(document.getElementById('application-entities-timeline')).draw(data, {
          title: "Entities",
          titlePosition: "in",
          titleTextStyle: {color: 'black', fontName: 'Arial', fontSize: 18},
          width: graph_width,
          height: graph_height,
          backgroundColor: backgroundGraphColor,
          legend: "none",
          hAxis: {textStyle: {color:"transparent", fontSize: 1}},
          vAxis: {textStyle: {color:"transparent", fontSize: 1}}
        });

        data = new google.visualization.DataTable();
        data.addColumn('date', 'Time');
        data.addColumn('number', 'CPU');
        data.addRows(15);

        for (var i in usage_counters[2].values) {
          data.setCell(parseInt(i), 0, new Date(usage_counters[2].values[i].timestamp));
          data.setCell(parseInt(i), 1, usage_counters[2].values[i].value);
        }

        new google.visualization.LineChart(document.getElementById('application-cpu-time')).draw(data, {
          title: "CPU Time Used",
          titlePosition: "in",
          titleTextStyle: {color: 'black', fontName: 'Arial', fontSize: 18},
          width: graph_width,
          height: graph_height,
          backgroundColor: backgroundGraphColor,
          legend: "none",
          hAxis: {textStyle: {color:"transparent", fontSize: 1}},
          vAxis: {textStyle: {color:"transparent", fontSize: 1}}
        });

        data = new google.visualization.DataTable();
        data.addColumn('date', 'Time');
        data.addColumn('number', 'Uploaded');
        data.addRows(15);

        for (var i in usage_counters[3].values) {
          data.setCell(parseInt(i), 0, new Date(usage_counters[3].values[i].timestamp));
          data.setCell(parseInt(i), 1, usage_counters[3].values[i].value);
        }

        new google.visualization.LineChart(document.getElementById('application-data-uploaded')).draw(data, {
          title: "Bytes Uploaded",
          titlePosition: "in",
          titleTextStyle: {color: 'black', fontName: 'Arial', fontSize: 18},
          width: graph_width,
          height: graph_height,
          backgroundColor: backgroundGraphColor,
          legend: "none",
          hAxis: {textStyle: {color:"transparent", fontSize: 1}},
          vAxis: {textStyle: {color:"transparent", fontSize: 1}}
        });

        data = new google.visualization.DataTable();
        data.addColumn('date', 'Time');
        data.addColumn('number', 'Downloaded');
        data.addRows(15);

        for (var i in usage_counters[1].values) {
          data.setCell(parseInt(i), 0, new Date(usage_counters[1].values[i].timestamp));
          data.setCell(parseInt(i), 1, usage_counters[1].values[i].value);
        }

        new google.visualization.LineChart(document.getElementById('application-data-downloaded')).draw(data, {
          title: "Bytes Downloaded",
          titlePosition: "in",
          titleTextStyle: {color: 'black', fontName: 'Arial', fontSize: 18},
          width: graph_width,
          height: graph_height,
          backgroundColor: backgroundGraphColor,
          legend: "none",
          hAxis: {textStyle: {color:"transparent", fontSize: 1}},
          vAxis: {textStyle: {color:"transparent", fontSize: 1}}
        });
      },
      function() {
        $('#application-entities-timeline').html("");
        $('#application-cpu-time').html("");
        $('#application-data-uploaded').html("");
        $('#application-data-downloaded').html("");
      }
    ));
  }
  window.Usergrid.console.requestApplicationUsage = requestApplicationUsage;

  /*******************************************************************
   *
   * Query Object Setup
   *
   ******************************************************************/
  var queryObj = {};

  function hidePagination(section) {
    $('#'+section+'-pagination').hide();
    $('#'+section+'-next').hide();
    $('#'+section+'-previous').hide();
  }

  function showPagination(section){
    if (queryObj.hasNext()) {
      $('#'+section+'-pagination').show();
      $('#'+section+'-next').show();
    }

    if (queryObj.hasPrevious()) {
      $('#'+section+'-pagination').show();
      $('#'+section+'-previous').show();
    }
  }

  function hideCurlCommand(section) {
    $('#'+section+'-curl-container').hide();
    $('#'+section+'-curl-token').hide();
  }

  function showCurlCommand(section, curl, token) {
    var data = {
      curlData: curl,
      sectionName: section
    };
    var sectionId = $('#'+section+'-curl-container');
    sectionId.html("");
    $.tmpl('apigee.ui.curl.detail.html', data).appendTo(sectionId);
     sectionId.show();
    if (!token) {
      $('#'+section+'-curl-token').hide();
    }
  }

  function copyCurlCommand() {
    $('#copypath', 'body')
    .find('a')
        .livequery('click', function() {
            $(this)
                .blur();
            var nodetext = $('#'+section+'-curl').html();
            $('#copypath input').focus();
            $('#copypath input').select();
            return false;
        });

  }

  function bindPagingEvents(section) {
    $(document).off('click', '#'+section+'-previous', getPrevious);
    $(document).off('click', '#'+section+'-next', getNext);
    //bind the click events
    $(document).on('click', '#'+section+'-previous', getPrevious);
    $(document).on('click', '#'+section+'-next', getNext);
  }

  function getPrevious() { //called by a click event - for paging
    queryObj.getPrevious();
    runAppQuery();
  }

  function getNext() { //called by a click event - for paging
    queryObj.getNext();
    runAppQuery();
  }

  function runAppQuery(_queryObj) {
    var obj = _queryObj || queryObj;
    Usergrid.ApiClient.runAppQuery(obj);
    return false;
  }

  function runManagementQuery(_queryObj) {
    var obj = _queryObj || queryObj;
    Usergrid.ApiClient.runManagementQuery(obj);
    return false;
  }

  /*******************************************************************
   *
   * Users
   *
   ******************************************************************/
  var userLetter = "*";
  var userSortBy = "username";

  function pageSelectUsers() {
    //Hide old Alert Messages
    hideModal('#users-messages');
    //make a new query object
    queryObj = new Usergrid.Query(null);
    //bind events for previous and next buttons
    bindPagingEvents('users');
    //reset paging so we start at the first page
    queryObj.resetPaging();
    //the method to get the compile and call the query
    getUsers();
    //ui stuff
    selectFirstTabButton('#users-panel-tab-bar');
    showPanelList('users');
    $('#search-user-username').val(''); //reset the search box
  }
  window.Usergrid.console.pageSelectUsers = pageSelectUsers;

  function getUsers(search, searchType) {
    //clear out the table before we start
    hideCurlCommand('users');
    var output = $('#users-table');
    output.empty();
    var query = {"ql" : "order by " + userSortBy}; //default to built in search
    if (typeof search == 'string') {
      if (search.length > 0) {
        if (searchType == 'name') {
          query = {"ql" : searchType + " contains '" + search + "*'"};
        } else {
          query = {"ql" : searchType + "='" + search + "*'"};
        }
      }
    } else if (userLetter != "*") {
      query = {"ql" : searchType + "='" + userLetter + "*'"};
    }

    queryObj = new Usergrid.Query("GET", "users", null, query, getUsersCallback, function() { alertModal("Error", "Unable to retrieve users."); });
    runAppQuery(queryObj);
  }

  function getUsersCallback(response) {
    hidePagination('users');
    var output = $('#users-table');
    if (response.entities.length < 1) {
      output.replaceWith('<div id="users-table" class="panel-section-message">No users found.</div>');
    } else {
      output.replaceWith('<table id="users-table" class="table"><tbody><tr class="zebraRows users-row"><td class="checkboxo"><input type="checkbox" onclick="Usergrid.console.selectAllEntities(this);" /></td><td class="gravatar50-td">&nbsp;</td><td class="user-details bold-header">username</td><td class="user-details bold-header">Display Name</td><td class="view-details">&nbsp;</td></tr></tbody></table>');
      for (i = 0; i < response.entities.length; i++) {
        var this_data = response.entities[i];
        if (!this_data.picture) {
          this_data.picture = window.location.protocol+ "//" + window.location.host + window.location.pathname + "images/user-photo.png"
        } else {
          this_data.picture = get_replacementGravatar(this_data.picture);
        }
        $.tmpl('apigee.ui.users.table_rows.html', this_data).appendTo('#users-table');
      }
    }
    showPagination('users');
    showCurlCommand('users', queryObj.getCurl(), queryObj.getToken());
  }

  function showUsersForSearch(search){
    selectFirstTabButton('#users-panel-tab-bar');
    $('#users-panel-search').hide();
    selectTabButton('#button-users-list');
    $('#users-panel-list').show();
    userLetter = search;
    getUsers();
  }
  Usergrid.console.showUsersForSearch = showUsersForSearch;

  function searchUsers(){
    var search = $('#search-user-username').val();
    var searchType = ($('#search-user-type').val())?$('#search-user-type').val():userSortBy;
    //make sure the input is valid:
    if (searchType == 'name') {
      searchType = 'name';
    } else if (searchType == 'username') {searchType = 'username';}
    getUsers(search, searchType);
  }
  Usergrid.console.searchUsers = searchUsers;

  function selectAllEntities(checkbox){
    if (checkbox.checked) {
      $('[class=listItem]').attr('checked', true);
    } else {
      $('[class=listItem]').attr('checked', false);
    }
  }
  window.Usergrid.console.selectAllEntities = selectAllEntities;

  $('#delete-users-link').click(deleteUsers);
  function deleteUsers(e) {
    e.preventDefault();

    var items = $('#users-table input[class^=listItem]:checked');
    if(!items.length){
      alertModal("Error", "Please, first select the users you want to delete.");
      return;
    }

    confirmDelete(function(){
      items.each(function() {
        var userId = $(this).attr("value");
        runAppQuery(new Usergrid.Query("DELETE", 'users/' + userId, null, null,
          getUsers,
          function() { alertModal("Error", "Unable to delete user - " + userId) }
        ));
      });
    });
  }

  /*******************************************************************
   *
   * User
   *
   ******************************************************************/

  function pageOpenUserProfile(userName) {
    hideModal('.messages');
    Pages.SelectPanel('user');
    requestUser(userName);
    selectPillButton('#button-user-profile');
    showPanelContent('#user-panel', '#user-panel-profile');
  }
  window.Usergrid.console.pageOpenUserProfile = pageOpenUserProfile;

  function pageOpenUserActivities(userId) {
    Pages.SelectPanel('user');
    requestUser(userId);
    selectPillButton('#button-user-activities');
    showPanelContent('#user-panel', '#user-panel-activities');
  }
  window.Usergrid.console.pageOpenUserActivities = pageOpenUserActivities;

  function pageSelectUserPermissions(userId) {
    Pages.SelectPanel('user');
    requestUser(userId);
    selectPillButton('#button-user-permissions');
    showPanelContent('#user-panel', '#user-panel-permissions');
  }
  window.Usergrid.console.pageSelectUserPermissions = pageSelectUserPermissions;

  function pageSelectUserGroups(userId) {
    Pages.SelectPanel('user');
    requestUser(userId);
    selectPillButton('#button-user-memberships');
    showPanelContent('#user-panel', '#user-panel-memberships');
  }

  function pageSelectUserGraph(userId) {
    Pages.SelectPanel('user');
    requestUser(userId);
    selectPillButton('#button-user-graph');
    showPanelContent('#user-panel', '#user-panel-graph');
  }

  window.Usergrid.console.pageSelectUserGroups = pageSelectUserGroups;

  function saveUserProfile(uuid){
    var payload = Usergrid.console.ui.jsonSchemaToPayload(Usergrid.console.ui.collections.vcard_schema);
    runAppQuery(new Usergrid.Query("PUT", "users/"+uuid, payload, null,
      completeSave,
      function() { alertModal("Error", "Unable to update User"); }
    ));
  }
  window.Usergrid.console.saveUserProfile = saveUserProfile;

  function completeSave(){
    closeMessage = function() {
      $('.messages').hide();
    };
    var closebutton = '<a  onclick="closeMessage();" class="close">&times;</a>'
    $('.messages').text("Information Saved.").prepend(closebutton).show();
  }

  function redrawUserProfile(data, curl){
    redrawFormPanel('user-panel-profile', 'apigee.ui.panels.user.profile.html', data);
    showCurlCommand('user-panel-profile', curl);
  };

  function redrawUserMemberships(data, curl){
    redrawPanel('user-panel-memberships', 'apigee.ui.panels.user.memberships.html', data);
    showCurlCommand('user-panel-memberships', curl);
    updateGroupsAutocomplete();
  };

  function redrawUserActivities(data, curl){
    redrawPanel('user-panel-activities', 'apigee.ui.panels.user.activities.html', data);
    showCurlCommand('user-panel-activities', curl);
  };

  function redrawUserGraph(data, curlFollowing, curlFollowers){
    redrawPanel('user-panel-graph', 'apigee.ui.panels.user.graph.html', data);
    showCurlCommand('user-panel-following', curlFollowing);
    showCurlCommand('user-panel-followers', curlFollowers);
    updateFollowUserAutocomplete();
  };

  function redrawUserPermissions(data, curlRoles, curlPermissions){
    redrawPanel('user-panel-permissions', 'apigee.ui.panels.user.permissions.html', data);
    showCurlCommand('user-panel-roles', curlRoles);
    showCurlCommand('user-panel-permissions', curlPermissions);
    updateRolesAutocomplete();
    updateQueryAutocompleteCollectionsUsers();
  };

  function redrawPanel(panelDiv, panelTemplate, data){
    $("#"+panelDiv).html("");
    $.tmpl(panelTemplate, data).appendTo($("#"+panelDiv));
  };

  function redrawGroupForm(panelDiv, panelTemplate, data){
    $("#"+panelDiv).html("");
    var details = $.tmpl(panelTemplate, data);
    var formDiv = details.find('.query-result-form');
    $(formDiv).buildForm(Usergrid.console.ui.jsonSchemaToDForm(Usergrid.console.ui.collections.group_schema, data.entity));
    details.appendTo($("#"+panelDiv));
    details.find('.button').button();
  }

  function redrawFormPanel(panelDiv, panelTemplate, data){
    $("#"+panelDiv).html("");
    var details = $.tmpl(panelTemplate, data);
    var formDiv = details.find('.query-result-form');
    $(formDiv).buildForm(Usergrid.console.ui.jsonSchemaToDForm(Usergrid.console.ui.collections.vcard_schema, data.entity));
    details.appendTo($("#"+panelDiv));
  };

  function saveUserData(){
    Usergrid.console.ui.jsonSchemaToPayload(schema, obj);
  }

  var user_data = null;

  function handleUserResponse(response) {
    if (response.entities && (response.entities.length > 0)) {
      var entity = response.entities[0];
      var path = response.path || "";
      path = "" + path.match(/[^?]*/);
      var username = entity.username;
      var name = entity.uuid + " : " + entity.type;

      if (entity.username) {
        name = entity.username;
      }

      if (entity.name) {
        name = name + " : " + entity.name;
      }

      var collections = $.extend({ }, (entity.metadata || { }).collections, (entity.metadata || { }).connections);
      if ($.isEmptyObject(collections)){
        collections = null;
      }

      var entity_contents = $.extend( false, { }, entity);
      delete entity_contents['metadata'];

      var metadata = entity.metadata;
      if ($.isEmptyObject(metadata)){
        metadata = null;
      }

      var entity_path = (entity.metadata || {}).path;
      if ($.isEmptyObject(entity_path)) {
        entity_path = path + "/" + entity.uuid;
      }

      var picture = window.location.protocol+ "//" + window.location.host + window.location.pathname + "images/user_profile.png";
      if (entity.picture) {
        entity.picture = entity.picture.replace(/^http:\/\/www.gravatar/i, 'https://secure.gravatar');
        //note: changing this to use the image on apigee.com - since the gravatar default won't work on any non-public domains such as localhost
        //this_data.picture = this_data.picture + encodeURI("?d="+window.location.protocol+"//" + window.location.host + window.location.pathname + "images/user_profile.png");
        picture = entity.picture + encodeURI("?d=http://apigee.com/usergrid/images/user_profile.png");
      }

    var data = {
      entity: entity_contents,
      picture: picture,
      name: name,
      username: username,
      path: entity_path,
      collections: collections,
      metadata: metadata,
      uri: (entity.metadata || { }).uri,
      followingCurl: "",
      followersCurl: "",
      rolesCurl: "",
      permissionsCurl: ""
    }

    redrawUserProfile(data, this.getCurl());

    //TODO: This block and the subsequent blocks could all be methods of their own
      runAppQuery(new Usergrid.Query("GET", 'users/' + entity.username + '/groups', null, null,
        function(response) {
          if (data && response.entities && (response.entities.length > 0)) {
            data.memberships = response.entities;
          }
          redrawUserMemberships(data, this.getCurl());
        },
        function() { alertModal("Error", "Unable to retrieve user's groups."); }
      ));

      runAppQuery(new Usergrid.Query("GET", 'users/' + entity.username + '/activities', null, null,
        function(response) {
          if (data && response.entities && (response.entities.length > 0)) {
            data.activities = response.entities;
            data.curl = this.getCurl();
            $('span[id^=activities-date-field]').each( function() {
              var created = dateToString(parseInt($(this).html()))
              $(this).html(created);
            });
          }
          redrawUserActivities(data, this.getCurl());
        },
        function() { alertModal("Error", "Unable to retrieve user's activities.");}
      ));

      runAppQuery(new Usergrid.Query("GET", 'users/' + entity.username + '/roles', null, null,
        function(response) {
          if (data && response.entities && (response.entities.length > 0)) {
            data.roles = response.entities;
          } else {
            data.roles = null;
          }
          data.rolesCurl = this.getCurl();
          //Run Permissions query after roles query has been handled
        runAppQuery(new Usergrid.Query("GET", 'users/' + entity.username + '/permissions', null, null,
          function(response) {
            var permissions = {};
            if (data && response.data && (response.data.length > 0)) {

              if (response.data) {
                var perms = response.data;
                var count = 0;

                for (var i in perms) {
                  count++;
                  var perm = perms[i];
                  var parts = perm.split(':');
                  var ops_part = "";
                  var path_part = parts[0];

                  if (parts.length > 1) {
                    ops_part = parts[0];
                    path_part = parts[1];
                  }

                  ops_part.replace("*", "get,post,put,delete")
                    var ops = ops_part.split(',');
                  permissions[perm] = {ops : {}, path : path_part, perm : perm};

                  for (var j in ops) {
                    permissions[perm].ops[ops[j]] = true;
                  }
                }

                if (count == 0) {
                  permissions = null;
                }
                data.permissions = permissions;
              }
            }
            data.permissionsCurl = this.getCurl();
            redrawUserPermissions(data, data.rolesCurl, data.permissionsCurl);
          },
          function() { alertModal("Error", "Unable to retrieve user's permissions.");}
        ));
        },
        function() { alertModal("Error", "Unable to retrieve user's roles.");}
      ));

        runAppQuery(new Usergrid.Query("GET", 'users/' + entity.username + '/following', null, null,
          function(response) {
            data.followingCurl = this.getCurl();
            if (data && response.entities && (response.entities.length > 0)) {
              data.following = response.entities;
            }
            //Requests /Followers after the /following response has been handled.
            runAppQuery(new Usergrid.Query("GET", 'users/' + entity.username + '/followers', null, null,
            function(response) {

              if (data && response.entities && (response.entities.length > 0)) {
                data.followers = response.entities;
              }
              data.followersCurl = this.getCurl();
              redrawUserGraph(data, data.followingCurl, data.followersCurl);
            },
            function() { alertModal("Error", "Unable to retrieve user's followers.");
            }
          ));
        },
        function() { alertModal("Error", "Unable to retrieve user's following.");}
        ));
    }

  };

  function requestUser(userId) {
    $('#user-profile-area').html('<div class="alert alert-info">Loading...</div>');
    runAppQuery(new Usergrid.Query("GET", 'users/'+userId, null, null, handleUserResponse,
      function() { alertModal("Error", "Unable to retrieve user's profile."); }
    ));
  }

  /*******************************************************************
   *
   * Groups
   *
   ******************************************************************/
  var groupLetter = "*";
  var groupSortBy = "path";
  function pageSelectGroups() {
    //Hide old messages
    hideModal('#groups-messages');
    //make a new query object
    queryObj = new Usergrid.Query(null);
    //bind events for previous and next buttons
    bindPagingEvents('groups', getPreviousGroups, getNextGroups);
    //reset paging so we start at the first page
    queryObj.resetPaging();
    //the method to get the compile and call the query
    getGroups();
    //ui stuff
    selectFirstTabButton('#groups-panel-tab-bar');
    showPanelList('groups');
    $('#search-user-groupname').val('');
  }
  window.Usergrid.console.pageSelectGroups = pageSelectGroups;

  function getGroups(search, searchType) {
    //clear out the table before we start
    var output = $('#groups-table');
    output.empty();
  hideCurlCommand('groups');
    var query = {"ql" : "order by " + groupSortBy};
    if (typeof search == 'string') {
      if (search.length > 0) {
        if (searchType == 'title') {
          query = {"ql" : searchType + " contains '" + search + "*'"};
        } else {
          query = {"ql" : searchType + "='" + search + "*'"};
        }
      }
    } else if (groupLetter != "*") {
      query = {"ql" : searchType + "='" + groupLetter + "*'"};
    }

    queryObj = new Usergrid.Query("GET", "groups", null, query, getGroupsCallback, function() { alertModal("Error", "Unable to retrieve groups."); });
    runAppQuery(queryObj);

    return false;
  }

  function getPreviousGroups() {
    queryObj.getPrevious();
    getGroups();
  }

  function getNextGroups() {
    queryObj.getNext();
    getGroups();
  }

  function getGroupsCallback(response) {
    hidePagination('groups');

    var output = $('#groups-table');
    if (response.entities.length < 1) {
      output.replaceWith('<div id="groups-table" class="panel-section-message">No groups found.</div>');
    } else {
      output.replaceWith('<table id="groups-table" class="table"><tbody><tr class="zebraRows users-row"><td class="checkboxo"><input type="checkbox" onclick="Usergrid.console.selectAllEntities(this);" /></td><td class="user-details bold-header">Path</td><td class="user-details bold-header">Title</td><td class="view-details">&nbsp;</td></tr></tbody></table>');
      for (i = 0; i < response.entities.length; i++) {
        var this_data = response.entities[i];
        $.tmpl('apigee.ui.groups.table_rows.html', this_data).appendTo('#groups-table');
      }
    }

    showPagination('groups');
    showCurlCommand('groups', queryObj.getCurl(), queryObj.getToken());
  }

  function showGroupsForSearch(search){
    selectFirstTabButton('#groups-panel-tab-bar');
    $('#groups-panel-search').hide();
    selectTabButton('#button-groups-list');
    $('#groups-panel-list').show();
    groupLetter = search;
    getGroups();
  }
  Usergrid.console.showUsersForSearch = showUsersForSearch;

  function searchGroups(){
    var search = $('#search-user-groupname').val();
    var searchType = ($('#search-group-type').val())?$('#search-group-type').val():groupSortBy;

    if (searchType == 'title') {
      searchType = 'title';
    } else if (searchType == 'path') {
      searchType = 'path';
    }

    getGroups(search, searchType);
  }
  Usergrid.console.searchGroups = searchGroups;

  $('#delete-groups-link').click(deleteGroups);

  function deleteGroups(e) {
    e.preventDefault();

    var items = $('#groups-table input[class^=listItem]:checked');
    if (!items.length) {
      alertModal("Error", "Please, first select the groups you want to delete.")
        return;
    }

    confirmDelete(function(){
      items.each(function() {
        var groupId = $(this).attr('value');
        runAppQuery(new Usergrid.Query("DELETE", "groups/" + groupId, null, null,
          getGroups,
          function() { alertModal("Error", "Unable to delete group"); }
        ));
      });
    });
  }

  /*******************************************************************
   *
   * Group
   *
   ******************************************************************/

  function pageOpenGroupProfile(groupPath) {
    Pages.SelectPanel('group');
    requestGroup(groupPath);
    selectPillButton('#button-group-details');
    showPanelContent('#group-panel', '#group-panel-details');
  }
  window.Usergrid.console.pageOpenGroupProfile = pageOpenGroupProfile;

  function pageSelectGroupMemberships(groupId) {
    Pages.SelectPanel('group');
    requestGroup(groupId);
    selectPillButton('#button-group-memberships');
    showPanelContent('#group-panel', '#group-panel-memberships');
  }
  window.Usergrid.console.pageSelectGroupMemberships = pageSelectGroupMemberships;

  function redrawGroupDetails(data,curl){
    redrawGroupForm('group-panel-details', 'apigee.ui.panels.group.details.html', data);
    showCurlCommand('group-panel-details', curl);
  }

  function redrawGroupMemberships(data, curl){
    redrawPanel('group-panel-memberships', 'apigee.ui.panels.group.memberships.html', data);
    showCurlCommand('group-panel-memberships', curl);
    updateUsersAutocomplete();
  }

  function redrawGroupActivities(data, curl){
    redrawPanel('group-panel-activities', 'apigee.ui.panels.group.activities.html', data);
    showCurlCommand('group-panel-activities', curl);
  }

  function redrawGroupPermissions(data, curlRoles, curlPermissions){
    if (data.roles && data.roles.length == 0) {
      delete data.roles
    }
    redrawPanel('group-panel-permissions', 'apigee.ui.panels.group.permissions.html', data);
    showCurlCommand('group-panel-roles', curlRoles);
    showCurlCommand('group-panel-permissions', curlPermissions);
    updateRolesForGroupsAutocomplete();
  }

  function saveGroupProfile(uuid){
    var payload = Usergrid.console.ui.jsonSchemaToPayload(Usergrid.console.ui.collections.group_schema);
    runAppQuery(new Usergrid.Query("PUT", "groups/"+uuid, payload, null, completeSave,
      function() {
        closeErrorMessage = function() {
          $('#group-messages').hide();
        };
        var closebutton = '<a  onclick="closeErrorMessage();" class="close">&times;</a>'
        $('#group-messages').text("Unable to update Group").prepend(closebutton).addClass('alert-error').show();
      }
    ));
  }

  window.Usergrid.console.saveGroupProfile = saveGroupProfile;

  var group_data = null;

  function handleGroupResponse(response) {
    if (response.entities && (response.entities.length > 0)) {
      var entity = response.entities[0];
      var path = response.path || "";
      path = "" + path.match(/[^?]*/);
      var uuid = entity.uuid;
      var name = entity.uuid + " : " + entity.type;

      if (entity.path) {
        name = entity.path;
      }

      if (entity.name) {
        name = name + " : " + entity.name;
      }

      var collections = $.extend({ }, (entity.metadata || { }).collections, (entity.metadata || { }).connections);
      if ($.isEmptyObject(collections)){
        collections = null;
      }

      var entity_contents = $.extend( false, { }, entity);
      delete entity_contents['metadata'];

      var metadata = entity.metadata;
      if ($.isEmptyObject(metadata)){
        metadata = null;
      }

      var entity_path = (entity.metadata || {}).path;
      if ($.isEmptyObject(entity_path)) {
        entity_path = path + "/" + entity.uuid;
      }

  var data = {
    entity : entity_contents,
        picture : entity.picture,
        name : name,
        uuid : uuid,
        path : entity_path,
        collections : collections,
        metadata : metadata,
        uri : (entity.metadata || { }).uri
  }

  redrawGroupDetails(data, this.getCurl());

      runAppQuery(new Usergrid.Query("GET",'groups/' + entity.path + '/users', null, null,
        function(response) {
          if (data && response.entities && (response.entities.length > 0)) {
            data.memberships = response.entities;

          }
          redrawGroupMemberships(data, this.getCurl());
        },
        function() { alertModal("Error", "Unable to retrieve group's users."); }
      ));

      runAppQuery(new Usergrid.Query("GET",'groups/' + entity.path + '/activities', null, null,
        function(response) {
          if (data && response.entities && (response.entities.length > 0)) {
            data.activities = response.entities;
          }
          redrawGroupActivities(data, this.getCurl());
        },
        function() { alertModal("Error", "Unable to retrieve group's activities."); }
      ));

      runAppQuery(new Usergrid.Query("GET",'groups/' + entity.path + '/roles', null, null,
        function(response) {
          if (data && response.entities) {
            data.roles = response.entities;
          }
          data.groupRolesCurl = this.getCurl();
          //WHEN /Roles is properly handled, get permissions
          runAppQuery(new Usergrid.Query("GET", 'groups/' + entity.path + '/permissions', null, null,
          function(response) {
            var permissions = {};
            if (data && response.data && (response.data.length > 0)) {

              if (response.data) {
                var perms = response.data;
                var count = 0;

                for (var i in perms) {
                  count++;
                  var perm = perms[i];
                  var parts = perm.split(':');
                  var ops_part = "";
                  var path_part = parts[0];

                  if (parts.length > 1) {
                    ops_part = parts[0];
                    path_part = parts[1];
                  }

                  ops_part.replace("*", "get,post,put,delete")
                    var ops = ops_part.split(',');
                  permissions[perm] = {ops : {}, path : path_part, perm : perm};

                  for (var j in ops) {
                    permissions[perm].ops[ops[j]] = true;
                  }
                }
                if (count == 0) {
                  permissions = null;
                }
                data.permissions = permissions;
              }
            }
            data.groupPermissionsCurl = this.getCurl();
            redrawGroupPermissions(data,data.groupRolesCurl, data.groupPermissionsCurl);
          },
          function() { alertModal("Error", "Unable to retrieve group's permissions."); }
        ));
        },
        function() { alertModal("Error", "Unable to retrieve group's roles."); }
      ));
    }
  }


  function requestGroup(groupId) {
    $('#group-details-area').html('<div class="alert alert-info">Loading...</div>');
    runAppQuery(new Usergrid.Query("GET",'groups/'+ groupId, null, null,handleGroupResponse,
      function() { alertModal("Error", "Unable to retrieve group details."); }
    ));
  }

  /*******************************************************************
   *
   * Roles
   *
   ******************************************************************/
  var roleLetter = '*';
  var roleSortBy = 'title';

  function pageSelectRoles(uuid) {
    //Hide old information modal
    hideModal('#roles-messages');
    //make a new query object
    queryObj = new Usergrid.Query(null);
    //bind events for previous and next buttons
    bindPagingEvents('roles');
    //reset paging so we start at the first page
    queryObj.resetPaging();
    //the method to get the compile and call the query
    getRoles();
    //ui stuff
    selectFirstTabButton('#roles-panel-tab-bar');
    showPanelList('roles');
    $('#role-panel-users').hide();
    $('#role-panel-groups').hide();
  }
  window.Usergrid.console.pageSelectRoles = pageSelectRoles;

  function getRoles(search, searchType) {
    var output = $('#roles-table');
    output.empty();
    hideCurlCommand('roles');
    //put the sort by back in once the API is fixed
    //var query = {"ql" : "order by " + roleSortBy};
    var query = {};
    if (roleLetter != "*") query = {"ql" : roleSortBy + "='" + groupLetter + "*'"};

    queryObj = new Usergrid.Query("GET", "roles", null, query, getRolesCallback, function() { alertModal("Error", "Unable to retrieve roles."); });
    runAppQuery(queryObj);
    return false;
  }

  function getPreviousRoles() {
    queryObj.getPrevious();
    getRoles();
  }

  function getNextRoles() {
    queryObj.getNext();
    getRoles();
  }

  function getRolesCallback(response) {
    hidePagination('roles');
    var output = $('#roles-table')
    output.empty();
    if (response.entities < 1) {
      output.html('<div class="panel-section-message">No roles found.</div>');
    } else {
      output.replaceWith('<table id="roles-table" class="table"><tbody><tr class="zebraRows users-row"><td class="checkboxo"><input type="checkbox" onclick="Usergrid.console.selectAllEntities(this);" /></td><td class="user-details bold-header">Title</td><td class="user-details bold-header">Role Name</td><td class="view-details">&nbsp;</td></tr></tbody></table>');

      $.each (response.entities, function(index, value) {
        var data = [
          {name: value.name,
           title: value.title}]
        $.tmpl('apigee.ui.roles.table_rows.html', data).appendTo('#roles-table');
      });
    }
    showPagination('roles');
    showCurlCommand('roles', this.getCurl(), this.getToken());
  }

  $('#delete-roles-link').click(deleteRoles);
  function deleteRoles(e) {
    e.preventDefault();

    var items = $('#roles-table input[class^=listItem]:checked');
    if(!items.length){
      alertModal("Error", "Please, first select the roles you want to delete.")
        return;
    }
    confirmDelete(function(){
      items.each(function() {
        var roleName = $(this).attr("value");
        runAppQuery(new Usergrid.Query("DELETE", "roles/" + roleName, null, null, getRoles,
          function() { alertModal("Error", "Unable to delete role"); }
        ));
      });
    });
  }

  /*******************************************************************
   *
   * Role
   *
   ******************************************************************/

  var current_roleName = "";
  var current_roleTitle = "";

  function updateCurrentRole(roleName, roleTitle) {
    current_roleName = roleName;
    current_roleTitle = roleTitle;
  }

  function pageOpenRole(roleName, roleTitle) {
    updateCurrentRole(roleName, roleTitle);
    requestRole(roleName, roleTitle);
    showPanel('#role-panel');
    Pages.ActivatePanel('roles');
    $('#role-panel-list').hide();
    selectPillButton('#button-role-settings');
    $('#role-panel-settings').show();
  }
  window.Usergrid.console.pageOpenRole = pageOpenRole;

  function pageSelectRoleUsers (roleName, roleTitle){
    updateCurrentRole(roleName, roleTitle);
    requestRole(roleName, roleTitle);
    showPanel('#role-panel');
    Pages.ActivatePanel('roles');
    $('#role-panel-list').hide();
    selectPillButton('#button-role-users');
    $('#role-panel-users').show();
  }
  window.Usergrid.console.pageSelectRoleUsers = pageSelectRoleUsers;

  function pageSelectRoleGroups(roleName, roleTitle) {
    updateCurrentRole(roleName, roleTitle);
    requestRole(roleName, roleTitle);
    showPanel('#role-panel');
    Pages.ActivatePanel('roles');
    $('#role-panel-list').hide();
    selectPillButton('#button-role-groups');
    $('#role-panel-groups').show();
  }
  window.Usergrid.console.pageSelectRoleGroups = pageSelectRoleGroups;

  var permissions = {};
  function displayPermissions(roleName, response, curl) {
    var section = $('#role-permissions');
    section.empty();

    var t = "";
    var m = "";
    permissions = {};
    var localPermission = {};
    if (response.data) {
      var perms = response.data;
      var count = 0;
      for (var i in perms) {
        count++;
        var perm = perms[i];
        var parts = perm.split(':');
        var ops_part = "";
        var path_part = parts[0];
        if (parts.length > 1) {
          ops_part = parts[0];
          path_part = parts[1];
        }
        ops_part.replace("*", "get,post,put,delete")
          var ops = ops_part.split(',');
        permissions[perm] = {ops : {}, path : path_part, perm : perm};
        for (var j in ops) {
          permissions[perm].ops[ops[j]] = true;
        }
      }
      if (count == 0) {
        permissions = null;
      }
      $.tmpl('apigee.ui.panels.role.permissions.html', {"role" : roleName, "permissions" : permissions}, {}).appendTo('#role-permissions');
      updatePermissionAutocompleteCollections();
    } else {
      section.html('<div class="alert">No permission information retrieved.</div>');
    }
  showCurlCommand('role-permissions', curl);
    displayRoleInactivity(roleName);
  }

  function displayRoleInactivity(roleName) {
    //requestRole & displayInactivity
    runAppQuery(new Usergrid.Query("GET", "roles/" + roleName, null, null,
      function(response) {
        if ( response && response.entities && response.entities[0].inactivity ){
          var inactivity = response.entities[0].inactivity.toString();
        } else {
         inactivity = 0;
        }
        $('#role-inactivity-input').val(inactivity);
      },
        function() { $('#role-inactivity-form').html('<div class="alert">Unable to load role\'s inactivity value.</div>') }
    ));
  }

  var rolesUsersResults = ''

  function displayRolesUsers(roleName, roleTitle, response, curl) {
    $('#role-users').html('');
    data = {};
    data.roleTitle = roleTitle;
    data.roleName = roleName;
    if (response.entities) {
      data.users = response.entities;
    }
    $.tmpl('apigee.ui.panels.role.users.html', {"data" : data}, {}).appendTo('#role-users');
    updateUsersForRolesAutocomplete();
    showCurlCommand('role-users', curl);
  }

  var rolesGroupsResults = ''

  function displayRoleGroups(response, curl) {
    response.roleName = current_roleName;
    response.roleTitle = current_roleTitle;
    $('#role-panel-groups').html('');
    $.tmpl('apigee.ui.role.groups.table_rows.html', response).appendTo('#role-panel-groups');
    updateGroupsForRolesAutocomplete();
    showCurlCommand('role-groups', curl);
  }

  function requestRole(roleName, roleTitle) {
    clearRoleSection();
    setRoleSectionTitles(roleTitle);
    getRolePermissions(roleName);
    getRoleUsers(roleName, roleTitle);
    getRoleGroups(roleName);
  }

  function clearRoleSection(){
    $('#role-section-title').empty();
    $('#role-permissions').empty();
    $('#role-users').empty();
  }

  function setRoleSectionTitles(roleTitle) {
    $('#role-section-title').html(roleTitle + " Role");
    $('#role-permissions').html('<div class="alert alert-info">Loading ' + roleTitle + ' permissions...</div>');
  }

  function getRolePermissions(roleName){
    runAppQuery(new Usergrid.Query("GET", "roles/" + roleName + "/permissions", null, null,
      function(response) { displayPermissions(roleName, response, this.getCurl()); },
      function() { $('#application-roles').html('<div class="alert">Unable to retrieve ' + roleName + ' role permissions.</div>'); }
    ));
  }

  function getRoleGroups(roleName) {
    runAppQuery(new Usergrid.Query("GET", "roles/" + roleName + "/groups", null, null,
        function(response) { displayRoleGroups(response, this.getCurl()); },
        function() { $('#application-roles').html('<div class="alert">Unable to retrieve ' + roleName + ' role permissions.</div>'); }
      ));
  }

  function getRoleUsers(roleName, roleTitle){
    runAppQuery(new Usergrid.Query("GET", "roles/" + roleName + "/users", null, null,
      function(response) { displayRolesUsers(roleName, roleTitle, response, this.getCurl()); },
      function() { $('#application-roles').html('<div class="alert">Unable to retrieve ' + roleName + ' role permissions.</div>'); }
    ));
  }

  function deleteRolePermission(roleName, permission) {
    data = {"permission":permission};
    confirmDelete(function(){
      runAppQuery(new Usergrid.Query("DELETE", "roles/" + roleName + "/permissions", null, data,
      function(){getRolePermissions(roleName)},
      function(){getRolePermissions(roleName)}
      ));
    });
  }
  window.Usergrid.console.deleteRolePermission = deleteRolePermission;

  function addRolePermission(roleName) {
    var path = $('#role-permission-path-entry-input').val();
    var ops = "";
    var s = "";
    if ($('#role-permission-op-get-checkbox').prop('checked')) {
      ops = "get";
      s = ",";
    }
    if ($('#role-permission-op-post-checkbox').prop('checked')) {
      ops = ops + s + "post";
      s = ",";
    }
    if ($('#role-permission-op-put-checkbox').prop('checked')) {
      ops =  ops + s + "put";
      s = ",";
    }
    if ($('#role-permission-op-delete-checkbox').prop('checked')) {
      ops =  ops + s + "delete";
      s = ",";
    }
    var permission = ops + ":" + path;
    var data = {"permission": ops + ":" + path};
    if (ops) {
      runAppQuery(new Usergrid.Query("POST", "/roles/" + roleName + "/permissions", data, null,
      function(){ getRolePermissions(roleName)},
      function(){ getRolePermissions(roleName)}));
    } else {
      alertModal("Error", "Please select a verb");
    }
  }
  window.Usergrid.console.addRolePermission = addRolePermission;

  function editRoleInactivity() {
    var inactivity = $('#role-inactivity-input').val();
    var roleName = current_roleName;

    if (intRegex.test(inactivity)) {
      data = { inactivity: inactivity };
      runAppQuery(new Usergrid.Query("PUT", "/role/" + roleName, data, null,
      function(){
        closeErrorMessage = function () {
          $('#role-permissions-messages').hide();
        }
        var closebutton = '<a onclick="closeErrorMessage();" class="close">&times;</a>'
        $('#role-permissions-messages').text('Inactivity time has been set').prepend(closebutton).show();
        displayRoleInactivity(current_roleName)},
      function(){ displayRoleInactivity(current_roleName)}
      ));
    } else {
      $('#inactivity-integer-message').show()
    }
  }
  window.Usergrid.console.editRoleInactivity = editRoleInactivity;

  function deleteUserPermission(userName, permission) {
    var data = {"permission": permission};
    confirmDelete(function(){
      runAppQuery(new Usergrid.Query("DELETE", "/users/" + userName + "/permissions", null, data,
        function(){ pageSelectUserPermissions (userName); },
        function(){ alertModal("Error", "Unable to delete permission"); }
      ));
    });
  }
  window.Usergrid.console.deleteUserPermission = deleteUserPermission;

  function addUserPermission(userName) {
    var path = $('#user-permission-path-entry-input').val();
    var ops = "";
    var s = "";
    if ($('#user-permission-op-get-checkbox').prop("checked")) {
      ops = "get";
      s = ",";
    }
    if ($('#user-permission-op-post-checkbox').prop("checked")) {
      ops = ops + s + "post";
      s = ",";
    }
    if ($('#user-permission-op-put-checkbox').prop("checked")) {
      ops =  ops + s + "put";
      s = ",";
    }
    if ($('#user-permission-op-delete-checkbox').prop("checked")) {
      ops =  ops + s + "delete";
      s = ",";
    }
    var data = {"permission": ops + ":" + path};
    if (ops) {
      runAppQuery(new Usergrid.Query("POST", "/users/" + userName + "/permissions/", data, null,
        function() { pageSelectUserPermissions (userName); },
        function() { alertModal("Error", "Unable to add permission"); }
      ));
    } else {
      alertModal("Error", "Please select a verb");
    }
  }
  window.Usergrid.console.addUserPermission = addUserPermission;

  function addGroupPermission(groupName) {
    var path = $('#group-permission-path-entry-input').val();
    var ops = "";
    var s = "";
    if ($('#group-permission-op-get-checkbox').prop("checked")) {
      ops = "get";
      s = ",";
    }
    if ($('#group-permission-op-post-checkbox').prop("checked")) {
      ops = ops + s + "post";
      s = ",";
    }
    if ($('#group-permission-op-put-checkbox').prop("checked")) {
      ops =  ops + s + "put";
      s = ",";
    }
    if ($('#group-permission-op-delete-checkbox').prop("checked")) {
      ops =  ops + s + "delete";
      s = ",";
    }
    var data = {"permission": ops + ":" + path};
    if (ops) {
      runAppQuery(new Usergrid.Query("POST", "/groups/" + groupName + "/permissions/", data, null,
        function() { pageSelectGroupPermissions(groupName); },
        function() { alertModal("Error", "Unable to add permission"); }
      ));
    } else {
      alertModal("Error", "Please select a verb");
    }
  }
  window.Usergrid.console.addGroupPermission = addGroupPermission;

  function deleteGroupPermission(groupName, permissions){
    var data = {"permission": permissions};
    runAppQuery(new Usergrid.Query("DELETE", "/groups/" + groupName + "/permissions/", null, data,
      function() { pageSelectGroupPermissions(groupName); },
      function() { alertModal("Error", "Unable to remove Permission"); }
    ));
  }

  window.Usergrid.console.deleteGroupPermission = deleteGroupPermission;

  function pageSelectGroupPermissions(groupId) {
    Pages.SelectPanel('group');
    requestGroup(groupId);
    selectPillButton('#button-group-permissions');
    showPanelContent('#group-panel', '#group-panel-permissions');
  }
  window.Usergrid.console.pageSelectGroupPermissions = pageSelectGroupPermissions;

  function submitAddGroupToRole(roleName, roleTitle) {
    var form = $(this);
    formClearErrors(form);

    var groupId = $('#search-roles-group-name-input');
    var bValid = checkLength2(groupId, 1, 80)
      && checkRegexp2(groupId, nameRegex, nameAllowedCharsMessage);

    if (bValid) {
      runAppQuery(new Usergrid.Query("POST", "/roles/" + roleName + "/groups/" + groupId.val(), null, null,
        function() { pageSelectRoleGroups(roleName, roleTitle); },
        function() { alertModal("Error", "Unable to add group to role"); }
      ));
      $('#dialog-form-add-group-to-role').modal('hide');
    }
  }
  window.Usergrid.console.submitAddGroupToRole = submitAddGroupToRole;

  function removeGroupFromRole(roleName,roleTitle) {
    var items = $('#role-panel-groups input[class=listItem]:checked');
    if (!items.length) {
      alertModal("Error", "Please, first select the groups you want to delete for this role.");
      return;
    }
    confirmDelete(function(){
      $.each(items, function() {
        var groupId = $(this).val();
        runAppQuery(new Usergrid.Query("DELETE", "/roles/" + roleName + "/groups/" + groupId, null, null,
          function() {
            pageSelectRoleGroups(roleName, roleTitle);
          },
          function() {
            alertModal("Error","Unable to remove group from role: ");
          }
        ));
      });
    });
  }
  window.Usergrid.console.removeGroupFromRole = removeGroupFromRole;

  /*******************************************************************
   *
   * Activities
   *
   ******************************************************************/
  var activitiesLetter = '*';
  var activitiesSortBy = 'created';

  function pageSelectActivities(uuid) {
    //make a new query object
    queryObj = new Usergrid.Query(null);
    //bind events for previous and next buttons
    bindPagingEvents('activities', getPreviousActivities, getNextActivities);
    //reset paging so we start at the first page
    queryObj.resetPaging();
    //the method to get the compile and call the query
    getActivities();
    //ui stuff
    showPanelList('activities');

  }
  window.Usergrid.console.pageSelectActivities = pageSelectActivities;

  function getActivities(search, searchType) {
    //clear out the table before we start
    var output = $('#activities-table');
    output.empty();

    queryObj = new Usergrid.Query("GET", "activities", null, null, getActivitiesCallback, function() { alertModal("Error", "Unable to retrieve activities.")});
    runAppQuery(queryObj);
    return false;
  }

  function getPreviousActivities() {
    queryObj.getPrevious();
    getActivities();
  }

  function getNextActivities() {
    queryObj.getNext();
    getActivities();
  }

  function getActivitiesCallback(response) {
    hidePagination('activities');
    var output = $('#activities-table');
    if (response.entities.length < 1) {
      output.replaceWith('<div id="activities-table" class="panel-section-message">No activities found.</div>');
    } else {
      output.replaceWith('<table id="activities-table" class="table"><tbody></tbody></table>');
      for (i = 0; i < response.entities.length; i++) {
        var this_data = response.entities[i];
        if (!this_data.actor.picture) {
          this_data.actor.picture = window.location.protocol+ "//" + window.location.host + window.location.pathname + "images/user_profile.png"
        } else {
          this_data.actor.picture = this_data.actor.picture.replace(/^http:\/\/www.gravatar/i, 'https://secure.gravatar');
          //note: changing this to use the image on apigee.com - since the gravatar default won't work on any non-public domains such as localhost
          //this_data.picture = this_data.picture + encodeURI("?d="+window.location.protocol+"//" + window.location.host + window.location.pathname + "images/user_profile.png");
          if (~this_data.actor.picture.indexOf('http')) {
            this_data.actor.picture = this_data.actor.picture + encodeURI("?d=http://apigee.com/usergrid/images/user_profile.png");
          } else {
            this_data.actor.picture = 'http://apigee.com/usergrid/images/user_profile.png';
          }
        }

        $.tmpl('apigee.ui.activities.table_rows.html', this_data).appendTo('#activities-table');
      }
    }
    showPagination('activities');
  }

  function searchActivities(){
    var search = $('#search-activities').val();
    var searchType = ($('#search-activities-type').val())?$('#search-activities-type').val():activitiesSortBy;

    if (searchType == 'actor') {
      searchType = 'actor.displayName';
    } else if (searchType == 'content') {
      searchType = 'content';
    }

    getActivities(search, searchType);
  }
  Usergrid.console.searchActivities = searchActivities;

  /*******************************************************************
   *
   * Analytics
   *
   ******************************************************************/

  function pageSelectAnalytics(uuid) {
    requestApplicationCounterNames();
  }
  window.Usergrid.console.pageSelectAnalytics = pageSelectAnalytics;

  var application_counters_names = [];

  function requestApplicationCounterNames() {
    $('#analytics-counter-names').html('<div class="alert alert-info">Loading...</div>');
    runAppQuery(new Usergrid.Query("GET","counters", null, null,
      function(response) {
        application_counters_names = response.data;
        var html = Usergrid.console.ui.makeTableFromList(application_counters_names, 1, {
          tableId : "analytics-counter-names-table",
          getListItem : function(i, col, row) {
            var counter_name = application_counters_names[i];
            var checked = !counter_name.startsWith("application.");
            return '<div class="analytics-counter"><input class="analytics-counter-checkbox" type="checkbox" name="counter" ' +
            'value="' + counter_name + '"' + (checked ? ' checked="true"' : '') + ' />' + counter_name + '</div>';
          }
        });
        $('#analytics-counter-names').html(html);
        requestApplicationCounters();
      },
      function() {
        $('#analytics-counter-names').html('<div class="alert">Unable to load...</div>');
      }
    ));
  }

  var application_counters = [];
  var start_timestamp = 1;
  var end_timestamp = 1;
  var resolution = "all";
  var show_application_counters = true;

  function requestApplicationCounters() {
    var counters_checked = $('.analytics-counter-checkbox:checked').serializeArray();
    var counter_names = new Array();
    for (var i in counters_checked) {
      counter_names.push(counters_checked[i].value);
    }

    $('#analytics-graph').html('<div class="alert alert-info">Loading...</div>');
    start_timestamp = $('#start-date').datepicker('getDate').at($('#start-time').timepicker('getTime')).getTime();
    end_timestamp = $('#end-date').datepicker('getDate').at($('#end-time').timepicker('getTime')).getTime();
    resolution = $('select#resolutionSelect').val();
    var params = {};
    params.start_time = start_timestamp;
    params.end_time = end_timestamp;
    params.resolution = resolution;
    params.counter = counter_names;
    params.pad = true;

    runAppQuery(new Usergrid.Query("GET","counters", null, params,
      function(response) {
        application_counters = response.counters;
        if (!application_counters) {
          $('#analytics-graph').html('<div class="alert marginless">No counter data.</div>');
          return;
        }

        var data = new google.visualization.DataTable();
        if (resolution == "all") {
          data.addColumn('string', 'Time');
        } else if ((resolution == "day") || (resolution == "week") || (resolution == "month")) {
          data.addColumn('date', 'Time');
        } else {
          data.addColumn('datetime', 'Time');
        }

        var column_count = 0;
        for (var i in application_counters) {
          var count = application_counters[i];
          if (show_application_counters || !count.name.startsWith("application.")) {
            data.addColumn('number', count.name);
            column_count++;
          }
        }

        if (!column_count) {
          $('#analytics-graph').html("No counter data");
          return;
        }

        var html = '<table id="analytics-graph-table"><thead><tr><td></td>';
        if (application_counters.length > 0) {
          data.addRows(application_counters[0].values.length);
          for (var j in application_counters[0].values) {
            var timestamp = application_counters[0].values[j].timestamp;
            if ((timestamp <= 1) || (resolution == "all")) {
              timestamp = "All";
            } else if ((resolution == "day") || (resolution == "week") || (resolution == "month")) {
              timestamp = (new Date(timestamp)).toString("M/d");
            } else {
              timestamp = (new Date(timestamp)).toString("M/d h:mmtt");
            }
            html += "<th>" + timestamp + "</th>";
            if (resolution == "all") {
              data.setValue(parseInt(j), 0, "All");
            } else {
              data.setValue(parseInt(j), 0, new Date(application_counters[0].values[j].timestamp));
            }
          }
        }
        html += "</tr></thead><tbody>";

        for (var i in application_counters) {
          var count = application_counters[i];
          if (show_application_counters || !count.name.startsWith("application.")) {
            html += "<tr><th scope=\"row\">" + count.name + "</th>";
            for (var j in count.values) {
              html += "<td>" + count.values[j].value + "</td>";
              data.setValue(parseInt(j), parseInt(i) + 1, count.values[j].value);
            }
            html += "</tr>";
          }
        }

        html += "</tbody></table>";
        $('#analytics-graph').html(html);

        if (resolution == "all") {
          new google.visualization.ColumnChart(document.getElementById('analytics-graph-area'))
            .draw(data, {width: 950, height: 500, backgroundColor: backgroundGraphColor});
        } else {
          new google.visualization.LineChart(document.getElementById('analytics-graph-area'))
            .draw(data, {width: 950, height: 500, backgroundColor: backgroundGraphColor});
        }
      },
      function() {
        $('#analytics-graph').html('<div class="alert">Unable to load...</div>');
      }));
  }

  /*******************************************************************
   *
   *Properties
   *******************************************************************/
  function pageSelectProperties(){
    requestApplicationCredentials();
  }

  Usergrid.console.pageSelectProperties = pageSelectProperties;

  /*******************************************************************
   *
   * Settings
   *
   ******************************************************************/


  var application_keys = {};

  function requestApplicationCredentials() {
    $('#application-panel-key').html('<div class="alert alert-info">Loading...</div>');
    $('#application-panel-secret').html('<div class="alert alert-info">Loading...</div>');

    runAppQuery(new Usergrid.Query("GET", "credentials", null, null,
      function(response) {
        $('#application-panel-key').html(response.credentials.client_id);
        $('#application-panel-secret').html(response.credentials.client_secret);
        application_keys[current_application_id] = {client_id : response.credentials.client_id, client_secret : response.credentials.client_secret};
      },
      function() {
        $('#application-panel-key').html('<div class="alert">Unable to load...</div>');
        $('#application-panel-secret').html('<div class="alert">Unable to load...</div>');
      }
    ));
  }

  function newApplicationCredentials() {
    $('#application-panel-key').html('<div class="alert alert-info">Loading...</div>');
    $('#application-panel-secret').html('<div class="alert alert-info">Loading...</div>');

    runAppQuery(new Usergrid.Query("POST", "credentials", null, null,
      function(response) {
        $('#application-panel-key').html(response.credentials.client_id);
        $('#application-panel-secret').html(response.credentials.client_secret);
        application_keys[current_application_id] = {client_id : response.credentials.client_id, client_secret : response.credentials.client_secret};
      },
      function() {
        $('#application-panel-key').html('<div class="alert">Unable to load...</div>');
        $('#application-panel-secret').html('<div class="alert">Unable to load...</div>');
      }
    ));
  }
  window.Usergrid.console.newApplicationCredentials = newApplicationCredentials;

  /*******************************************************************
   *
   * Shell
   *
   ******************************************************************/

  function pageSelectShell(uuid) {
    requestApplicationCredentials();
    $('#shell-input').focus();
  }
  window.Usergrid.console.pageSelectShell = pageSelectShell;

  var history_i = 0;
  var history = new Array();

  function scrollToInput() {
    // Do it manually because JQuery seems to not get it right
    var textArea = document.getElementById('shell-output');
    textArea.scrollTop = textArea.scrollHeight;
  }

  function echoInputToShell(s) {
    if (!s) s = "&nbsp;";
    var html = '<div class="shell-output-line"><span class="shell-prompt">&gt; </span><span class="shell-output-line-content">' + s + '</span></div>';
    scrollToInput();
  }

  function printLnToShell(s) {
    if (!s) s = "&nbsp;";
    var html = '<div class="shell-output-line"><div class="shell-output-line-content">' + s + '</div></div>';
    $('#shell-output').append(html);
    $('#shell-output').append(' ');
    scrollToInput();
  }

  function displayShellResponse(response) {
    printLnToShell(JSON.stringify(response, null, "  "));
    $('#shell-output').append('<hr />');
    prettyPrint();
    scrollToInput();
  }

  function handleShellCommand(s) {
    var orgName = Usergrid.ApiClient.getOrganizationName();

    if (s) {
      history.push(s);
      history_i = history.length - 1;
    }
    var path = '';
    var params = '';
    //Matches any sting that begins with "/", ignoring whitespaces.
    if (s.match(/^\s*\//)) {
      path = encodePathString(s);
      printLnToShell(path);
      runAppQuery(new Usergrid.Query("GET",path, null, null, displayShellResponse,null));
    //matches get or GET ignoring white spaces
    } else if (s.match(/^\s*get\s*\//i)) {
      path = encodePathString(s.substring(4));
      printLnToShell(path);
      runAppQuery(new Usergrid.Query("GET",path, null, null, displayShellResponse,null));
    } else if (s.match(/^\s*put\s*\//i)) {
      params = encodePathString(s.substring(4), true);
      printLnToShell(params.path);
      runAppQuery(new Usergrid.Query("PUT",params.path, params.payload, null, displayShellResponse,null));
  } else if (s.match(/^\s*post\s*\//i)) {
      params = encodePathString(s.substring(5), true);
      printLnToShell(params.path);
      runAppQuery(new Usergrid.Query("POST",params.path, params.payload, null, displayShellResponse,null));
    } else if (s.match(/^\s*delete\s*\//i)) {
      path = encodePathString(s.substring(7));
      printLnToShell(path);
      runAppQuery(new Usergrid.Query("DELETE",path, null, null, displayShellResponse,null));
    } else if (s.match(/^\s*clear|cls\s*/i))  {
      $('#shell-output').html(" ");
    } else if (s.match(/(^\s*help\s*|\?{1,2})/i)) {
      printLnToShell("/&lt;path&gt; - API get request");
      printLnToShell("get /&lt;path&gt; - API get request");
      printLnToShell("put /&lt;path&gt; {&lt;json&gt;} - API put request");
      printLnToShell("post /&lt;path&gt; {&lt;json&gt;} - API post request");
      printLnToShell("delete /&lt;path&gt; - API delete request");
      printLnToShell("cls, clear - clear the screen");
      printLnToShell("help - show this help");
    } else if (s === "") {
      printLnToShell("ok");
    } else {
      printLnToShell('<strong>syntax error!</strong><hr />');
    }
    prettyPrint();
  }

  $('#shell-input').keydown(function(event) {
    var shell_input = $('#shell-input');

    if (!event.shiftKey && (event.keyCode == '13')) {

      event.preventDefault();
      var s = $('#shell-input').val().trim();
      echoInputToShell(s);
      shell_input.val("");
      handleShellCommand(s);

    } else if (event.keyCode == '38') {

      event.preventDefault();
      history_i--;

      if (history_i < 0) {
        history_i = Math.max(history.length - 1, 0);
      }

      if (history.length > 0) {
        shell_input.val(history[history_i]);
      } else {
        shell_input.val("");
      }

    } else if (event.keyCode == '40') {

      event.preventDefault();
      history_i++;

      if (history_i >= history.length) {
        history_i = 0;
      }

      if (history.length > 0) {
        shell_input.val(history[history_i]);
      } else {
        shell_input.val("");
      }
    }
  });

  $('#shell-input').keyup(function() {
    var shell_input = $('#shell-input');
    shell_input.css('height', shell_input.attr('scrollHeight'));
  });


  /*******************************************************************
   *
   * Autocomplete
   *
   ******************************************************************/
  function updateAutocomplete(path, successCallback, failureMessage){
    runAppQuery(new Usergrid.Query("GET", path, null, null, successCallback,
    function(){ alertModal("Error", failureMessage)}
    ));
    return false
  }

  function updateUsersTypeahead(response, inputId){
    users = {};
    if (response.entities) {
      users = response.entities;
    }
    var pathInput = $('#'+inputId);
    var list = [];
    for (var i in users) {
      list.push(users[i].username);
    }
    pathInput.typeahead({source:list});
    pathInput.data('typeahead').source = list;
  }

  function updateRolesTypeahead(response, inputId){
    roles = {};
    if (response.entities) {
      roles = response.entities;
    }
    var pathInput = $('#'+inputId);
    var list = [];
    $.each(roles, function(key, value){
      list.push(value.roleName);
    })
    pathInput.typeahead({source:list});
    pathInput.data('typeahead').source = list;
  }

  function updateGroupsTypeahead(response, inputId){
    groups = {};
    if (response.entities) {
      groups = response.entities;
    }
    var pathInput = $('#'+inputId);
    var list = [];
    for (var i in groups) {
      list.push(groups[i].path);
    }
    pathInput.typeahead({source:list});
    pathInput.data('typeahead').source = list;
  }

  function updateCollectionTypeahead(inputId){
    var pathInput = $("#"+ inputId);
    var list = [];

    for (var i in applicationData.Collections) {
      list.push('/' + applicationData.Collections[i].name);
    }

    pathInput.typeahead({source:list});
    pathInput.data('typeahead').source = list;
  }

  function updateQueryTypeahead(response, inputId) {
    var pathInput = $("#" + inputId),
      list = [],
      name,
      path = response.path ;
    list.push(path);
    $.each(response.entities, function(entityKey, entityValue) {
      name = getEntityName(entityValue);
      list.push(path + '/' + name);
      //Fill collection names
      if (entityValue.metadata.collections) {
        $.each(entityValue.metadata.collections, function(key){
          list.push(path + '/' + name + '/' + key);
        });
      }
      //Fill Sets
      if (entityValue.metadata.sets) {
        $.each(entityValue.metadata.sets, function(key, value) {
          list.push(response.path + '/' + name + '/'+ key);
        })
      }
    });
    //TODO: Possible cleanup here, could not set the options via pathInput.typeahead.options, so overriding variables directly
    pathInput.data('typeahead').source = list;
    pathInput.data('typeahead').options.items = 10;
    pathInput.data('typeahead').matcher = function (item) {
      var checker = new RegExp('^' + this.query + '[a-z0-9.-]*\/?$', "i");
      return checker.test(item);
    }
  }

  function getEntityName(entity) {
    var name;
    switch(entity.type) {
      case 'user':
        name = entity.username;
        break;
      case 'group':
        name = entity.path;
        break;
      case 'role':
        name = entity.name;
        break;
      default:
        name = entity.uuid;
    }
    return name;
  }

  function updateUsersAutocomplete(){
    updateAutocomplete('users/', updateUsersAutocompleteCallback, "Unable to retrieve Users.");
  }

  function updateUsersAutocompleteCallback(response) {
    updateUsersTypeahead(response, 'search-user-name-input');
  }
  window.Usergrid.console.updateUsersAutocompleteCallback = updateUsersAutocompleteCallback;

  function updateFollowUserAutocomplete(){
    updateAutocomplete('users/',updateFollowUserAutocompleteCallback,"Unable to retrieve Users.");
  }

  function updateFollowUserAutocompleteCallback(response){
    updateUsersTypeahead(response, 'search-follow-username-input');
  }

  function updateUsersForRolesAutocomplete(){
    updateAutocomplete('users', updateUsersForRolesAutocompleteCallback,"Unable to retrieve Users.");
  }

  function updateUsersForRolesAutocompleteCallback(response) {
    updateUsersTypeahead(response, 'search-roles-user-name-input');
  }

  window.Usergrid.console.updateUsersForRolesAutocompleteCallback = updateUsersForRolesAutocompleteCallback;

  function updateGroupsAutocomplete(){
    updateAutocomplete('groups', updateGroupsAutocompleteCallback, "Unable to retrieve Groups.");
  }

  function updateGroupsAutocompleteCallback(response) {
    updateGroupsTypeahead(response, 'search-group-name-input');
  }
  window.Usergrid.console.updateGroupsAutocompleteCallback = updateGroupsAutocompleteCallback;

  function updateGroupsForRolesAutocomplete(){
    updateAutocomplete('groups', updateGroupsForRolesAutocompleteCallback, "Unable to retrieve Groups.");
  }

  function updateGroupsForRolesAutocompleteCallback(response) {
    updateGroupsTypeahead(response, 'search-roles-group-name-input');
  }
  window.Usergrid.console.updateGroupsForRolesAutocompleteCallback = updateGroupsForRolesAutocompleteCallback;

  function updatePermissionAutocompleteCollections(){
    updateCollectionTypeahead('role-permission-path-entry-input');
  }

  function updateQueryAutocompleteCollectionsUsers(){
    updateCollectionTypeahead('user-permission-path-entry-input');
  }

  function updateQueryAutocompleteCollections(){
    updateCollectionTypeahead('query-path');
  }

  function updateRolesAutocomplete(){
    updateAutocomplete('roles', updateRolesAutocompleteCallback, "Unable to retrieve Roles.");
  }

  function updateRolesAutocompleteCallback(response) {
    updateRolesTypeahead(response, 'search-role-name-input');
  }

  window.Usergrid.console.updateRolesAutocompleteCallback = updateRolesAutocompleteCallback;

  function updateRolesForGroupsAutocomplete(){
    updateAutocomplete('roles', updateRolesForGroupsAutocompleteCallback, "Unable to retrieve Roles.");
  }

  function updateRolesForGroupsAutocompleteCallback(response) {
    updateRolesTypeahead(response, 'search-groups-role-name-input');
  }
  window.Usergrid.console.updateRolesAutocompleteCallback = updateRolesAutocompleteCallback;


  /*******************************************************************
   *
   * Login
   *
   ******************************************************************/

  $('#login-organization').focus();

  function displayLoginError(message) {
    var message = message || '<strong>ERROR</strong>: Your details were incorrect.<br/>';
    $('#login-area .box').effect('shake', {times: 2},100);
    $('#login-message').html(message);
    $('#login-message').show();
  }

  function setupMenu() {
    var userNameBox = $('#userEmail');
    var userEmail = Usergrid.userSession.getUserEmail();
    if (userEmail){
      userNameBox.html(userEmail);
      setupOrganizationsMenu();
    } else {
      userNameBox.html("No Logged In User");
    }
  }

  function displayOrganizationName(orgName){
    $('#organization-name').text(orgName);
  }

  function setupOrganizationsMenu() {
    var organizations = Usergrid.organizations.getList();
      var orgName = Usergrid.ApiClient.getOrganizationName();
    if (!organizations) {
      return;
    }

    $('#organizations-menu > a span').text(orgName);
    $('#selectedOrg').text(orgName);

    var orgMenu = $('#organizations-menu ul');
    var orgTmpl = $('<li><a>${name}</a></li>');
    var data = [];
    var count = organizations.length;
    var i=0;
    for (i=0;i<count;i++) {
      var name = organizations[i].getName();
      data.push({"uuid":name, "name":name}); //only using name now
    }
    orgMenu.empty();
    orgTmpl.tmpl(data).appendTo(orgMenu);
    orgMenu.find('a').click(selectOrganizationDropdownHandler);
    displayCurrentOrg();
  }

  function selectOrganization(orgName) {
    if(orgName){
      var currentOrg = Usergrid.organizations.getItemByName(orgName);
      if (currentOrg) {
        Usergrid.ApiClient.setOrganizationName(currentOrg.getName());
        //sets the organization name in the console page.
        displayOrganizationName(Usergrid.ApiClient.getOrganizationName());
        // make sure there is an application for this org
        var app = currentOrg.getFirstItem();
        if (app) {
          Usergrid.ApiClient.setApplicationName(app.getName());
          setNavApplicationText();
        } else {
          forceNewApp();
        }
      } else {

        var newOrg = Usergrid.organizations.getFirstItem();
        Usergrid.ApiClient.setOrganizationName(newOrg.getName());
        var app = newOrg.getFirstItem();
        if (app) {
          Usergrid.ApiClient.setApplicationName(app.getName());
          setNavApplicationText();
        } else {
          forceNewApp();
        }
      }
    }
  }

  Usergrid.console.selectOrganization = selectOrganization;

  function selectOrganizationDropdownHandler(e) {
    var link = $(this);
    var orgName = link.text();
    selectOrganization(orgName);
    Usergrid.Navigation.router.navigateTo('organization');
  }

  function logout() {
    Usergrid.userSession.clearAll();
    if (Usergrid.SSO.usingSSO()) {
      Pages.clearPage();
      Usergrid.SSO.sendToSSOLogoutPage(Backbone.history.getHash(window));
    } else {
      Pages.ShowPage("login");
    }
    initOrganizationVars();
    return false;
  }
  Usergrid.console.logout = logout;

  function handleInvalidToken() {
    Usergrid.userSession.clearAll();
    if (Usergrid.SSO.usingSSO()) {
      Pages.clearPage();
      Usergrid.SSO.sendToSSOLoginPage(Backbone.history.getHash(window));
    } else {
      Pages.ShowPage("login");
    }
    initOrganizationVars();
    return false;
  }
  Usergrid.console.handleInvalidToken = handleInvalidToken;

  Usergrid.ApiClient.setLogoutCallback(Usergrid.console.handleInvalidToken);

  $("#login-form").submit(function () {
    login();
    return false;
  });

  /** //TODO Update documentation for .login() usage
  *  Authenticate an admin user and store the token and org list
  *  @method login
  *  @params {string} email - the admin's email (or username)
  *  @params {string} password - the admin's password
  *  @params {function} successCallback - callback function for success
  *  @params {function} errorCallback - callback function for error
  */
  function login(successCallback, errorCallback) {
    var email = $('#login-email').val();
    var password = $('#login-password').val();

    //empty local storage
    Usergrid.userSession.clearAll();

    var formdata = {
      grant_type: "password",
      username: email,
      password: password
    };
    runManagementQuery(new Usergrid.Query('POST', 'token', formdata, null,
      function(response) {
        if (!response || response.error){
          displayLoginError();
          return
        }
        //Fist clear the Organizations list
        Usergrid.organizations.clearList();
        //store all the organizations and their applications
        for (org in response.user.organizations) {
          //grab the name
          var orgName = response.user.organizations[org].name;
          //grab the uuid
          var orgUUID = response.user.organizations[org].uuid;
          organization = new Usergrid.Organization(orgName, orgUUID);
          for (app in response.user.organizations[org].applications) {
            //grab the name
            var appName = app.split("/")[1];
            //grab the id
            var appUUID = response.user.organizations[org].applications[app];
            //store in the new Application object
            application = new Usergrid.Application(appName, appUUID);
            organization.addItem(application);
          }
          //add organization to organizations list
          Usergrid.organizations.addItem(organization);
        }
        //select the first org by default
        var firstOrg = Usergrid.organizations.getFirstItem();
        //save the first org in the client
        Usergrid.ApiClient.setOrganizationName(firstOrg.getName());

        //store user data in local storage
        Usergrid.userSession.saveAll(response.user.uuid, response.user.email, response.access_token);

        //store the token in the client
        Usergrid.ApiClient.setToken(response.access_token);

        //call the success callback funciton
        loginOk(response);
      },
      function(response) {
        //call the error function
        displayLoginError();
      }
    ));
  }
  window.Usergrid.console.login = login;

  /**
  *  Reauthenticate an admin who already has a token
  *  @method autoLogin
  *  @params {function} successCallback - callback function for success
  *  @params {function} errorCallback - callback function for error
  */
  function autoLogin(successCallback, errorCallback) {
    //repopulate the user and the client with the info from the userSession
    var token = Usergrid.userSession.getAccessToken();
    Usergrid.ApiClient.setToken(token);

    runManagementQuery(new Usergrid.Query("GET","users/" + Usergrid.userSession.getUserEmail(), null, null,
      function(response) {
        if (!response) {
          errorCallback();
          return
        }

        //get the orgname and app name from the url
        var requestedOrgName = Usergrid.Navigation.router.getOrgNameFromURL();
        var requestedAppName = Usergrid.Navigation.router.getAppNameFromURL();

        var orgNameSelected = false;
        var appNameSelected = false;

        //store all the organizations and their applications
        for (org in response.data.organizations) {
          //grab the name
          var orgName = response.data.organizations[org].name;
          //grab the uuid
          var orgUUID = response.data.organizations[org].uuid;
          organization = new Usergrid.Organization(orgName, orgUUID);

          if (orgName === requestedOrgName) {
            Usergrid.ApiClient.setOrganizationName(orgName);
            orgNameSelected = true;
          }

          for (app in response.data.organizations[org].applications) {
            //grab the name
            var appName = app.split("/")[1];
            if (!appName) { appName = app; }
            if (appName === requestedAppName && orgNameSelected && orgName === requestedOrgName) {
               Usergrid.ApiClient.setApplicationName(appName);
               appNameSelected = true;
            }

            //grab the id
            var appUUID = response.data.organizations[org].applications[app];
            //store in the new Application object
            application = new Usergrid.Application(appName, appUUID);
            organization.addItem(application);
          }
          //add organization to organizations list
          Usergrid.organizations.addItem(organization);
        }

        if (!orgNameSelected) {
          //select the first org by default
          var firstOrg = Usergrid.organizations.getFirstItem();
          //save the first org in the client
          Usergrid.ApiClient.setOrganizationName(firstOrg.getName());
        }

        //store user data in local storage
        Usergrid.userSession.saveAll(response.data.uuid, response.data.email, response.data.token);

        //store the token in the client
        Usergrid.ApiClient.setToken(response.data.token);

        if (successCallback) {
          successCallback(response);
        }
      },
      function(response) {
        if (response == 'API CALL TIMEOUT') {
          showMessagePage();
        } else {
          displayLoginError();
        }
      }
    ));
    return;
  }
  window.Usergrid.console.autoLogin = autoLogin;

  function showMessagePage(message) {
    Pages.ShowPage('message');
    if (message) {
      $('#message-area').html(message);
    }
  }
  window.Usergrid.console.showMessagePage = showMessagePage;

  /*******************************************************************
   *
   * Signup
   *
   ******************************************************************/

  $('#signup-cancel').click(function() {
    Pages.ShowPage('login');
    clearSignupError();
    clearSignupForm();
    return false;
  });

  function displaySignupError(msg) {
    $('#signup-area .box').effect('shake', {times: 2},100);
    $('#signup-message').html('<strong>ERROR</strong>: ' + msg + '<br />').show();
  }

  function clearSignupForm() {
    $('#signup-organization-name').val("");
    $('#signup-username').val("");
    $('#signup-name').val("");
    $('#signup-email').val("");
    $('#signup-password').val("");
    $('#signup-password-confirm').val("");
  }

  function clearSignupError() {
    $('#signup-message').hide();
  }

  function signup() {
    var organization_name = $('#signup-organization-name').val();
    if (!(organizationNameRegex.test(organization_name))) {
      displaySignupError("Invalid organization name: " + organizationNameAllowedCharsMessage);
      return;
    }
    var username = $('#signup-username').val();
    if (!(usernameRegex.test(username))) {
      displaySignupError("Invalid username: " + usernameAllowedCharsMessage);
      return;
    }
    var name = $('#signup-name').val();
    if (!$.trim(name).length) {
      displaySignupError("Name cannot be blank.");
      return;
    }
    var email = $('#signup-email').val();
    if (!(emailRegex.test(email))) {
      displaySignupError("Invalid email: " + emailAllowedCharsMessage);
      return;
    }
    var password = $('#signup-password').val();
    if (!(passwordRegex.test(password))) {
      displaySignupError(passwordAllowedCharsMessage);
      return;
    }
    if (password != $('#signup-password-confirm').val()) {
      displaySignupError("Passwords must match.");
      return;
    }
    var formdata = {
      "organization": organization_name,
      "username": username,
      "name": name,
      "email": email,
      "password": password
    };
    runManagementQuery(new Usergrid.Query("POST",'organizations', formdata, null,
      function(response) {
        clearSignupError();
        clearSignupForm();
        Pages.ShowPage('post-signup');
      },
      function(response) {
        displaySignupError("Unable to create new organization at this time");
      }
    ));
  }

  $('#button-signup').click(function() {
    signup();
    return false;
  });

  /*******************************************************************
   *
   * Account Settings
   *
   ******************************************************************/

  function displayAccountSettings(response) {
    if (response.data) {
      response.data.gravatar = get_gravatar(response.data.email, 50);

      $('#update-account-username').val(response.data.username);
      $('#update-account-name').val(response.data.name);
      $('#update-account-email').val(response.data.email);
      $('#update-account-picture-img').attr('src', response.data.gravatar);
      $('#update-account-bday').attr('src', response.data.gravatar);

      var t = "";
      var organizations = response.data.organizations;
      var organizationNames = keys(organizations).sort();
      for (var i in organizationNames) {
        var organizationName = organizationNames[i];
        var organization = organizations[organizationName];
        var uuid = organization.uuid;
        t +=
          '<tr class="zebraRows leave-org-row">' +
          '<td>' +
          '<a href="#' + uuid + '">' +
          '<span>' + organizationName + '</span>' +
          '</a>' +
          '</td>' +
          '<td>' +
          '<span class="monospace">' + uuid + '</span>' +
          '</td>' +
          '<td>' +
          "<a onclick=\"Usergrid.console.leaveOrganization('" + uuid + "')\"" + ' ' + "href=\"#" + uuid + "\" class=\"btn btn-danger\">Leave</a>" +
          '</td>' +
          '</tr>';
      }

      $('#organizations').html(t);
      $('#organizations a').click( function(e){
        e.preventDefault();
        var uuid = $(this).attr('href').substring(1);
      });
    } else {
    }
  }
  Usergrid.console.displayAccountSettings = displayAccountSettings;

  $('#button-update-account').click(function() {
    var userData = {
      username : $('#update-account-username').val(),
      name : $('#update-account-name').val(),
      email : $('#update-account-email').val()
    };
    var old_pass = $('#old-account-password').val();
    var new_pass = $('#update-account-password').val();
    var new_pass2 = $('#update-account-password-repeat').val();
    if (old_pass && new_pass) {
      if (new_pass != new_pass2) {
        alertModal("Error", "New passwords don't match");
        requestAccountSettings();
        return;
      }

      if (!(passwordRegex.test(new_pass))) {
        alertModal("Error ", passwordAllowedCharsMessage);
        requestAccountSettings();
        return;
      }
      userData.newpassword = new_pass;
      userData.oldpassword = old_pass;
    }
    runManagementQuery(new Usergrid.Query("PUT",'users/' + Usergrid.userSession.getUserUUID(), userData, null,
      function(response) {
      $('#account-update-modal').modal('show');
        if ((old_pass && new_pass) && (old_pass != new_pass)) {
          logout();
          return;
        }
        requestAccountSettings();
      },
      function(response) {
        alertModal("Error", "Unable to update account settings");
        requestAccountSettings();
      }
    ));
    return false;
  });

  function requestAccountSettings(urlCallback) {
    if (Usergrid.SSO.usingSSO()) {
      Usergrid.SSO.sendToSSOProfilePage(urlCallback);
    } else {
     Pages.SelectPanel('account');
      $('#update-account-id').text(Usergrid.userSession.getUserUUID());
      $('#update-account-name').val("");
      $('#update-account-email').val("");
      $('#old-account-password').val("");
      $('#update-account-password').val("");
      $('#update-account-password-repeat').val("");
      runManagementQuery(new Usergrid.Query("GET",'users/' + Usergrid.userSession.getUserUUID(), null, null, displayAccountSettings, null));
    }
  }
  Usergrid.console.requestAccountSettings = requestAccountSettings;

  function displayOrganizations(response) {
    var t = "";
    var m = "";
    organizations = Usergrid.organizations.getList();
    orgainzations_by_id = {};
    if (response.data) {
      organizations = response.data;
      var count = 0;
      var organizationNames = keys(organizations).sort();
      for (var i in organizationNames) {
        var organization = organizationNames[i];
        var uuid = organizations[organization];
        t +=
          '<tr class="zebraRows leave-org-row">' +
          '<td>' +
          '<a href="#' + uuid + '">' +
          '<span>' + organization + '</span>' +
          '</a>' +
          '</td>' +
          '<td>' +
          '<span>' + uuid + '</span>' +
          '</td>' +
          '<td>' +
          "<a onclick=\"Usergrid.console.leaveOrganization('" + uuid + "')\" class=\"btn btn-danger\">Leave</a>" +
          '</td>' +
          '</tr>';

        count++;
        orgainzations_by_id[uuid] = organization;
      }
      if (count) {
        $('#organizations').html(t);
      } else {
        $('#organizations').html('<div class="alert">No organizations created.</div>');
      }
    } else {
      $('#organizations').html('<div class="alert">No organizations created.</div>');
    }
  }

  function leaveOrganization(UUID) {
    confirmAction(
      "Are you sure you want to leave this Organization?",
      "You will lose all access to it.",
      function() {
        runManagementQuery(new Usergrid.Query("DELETE","users/" + Usergrid.userSession.getUserUUID() + "/organizations/" + UUID, null, null,
          requestAccountSettings,
          function() { alertModal("Error", "Unable to leave organization"); }));
        }
    );

    return false;
  }
  Usergrid.console.leaveOrganization = leaveOrganization;

  function displayCurrentOrg() {
    var name = Usergrid.ApiClient.getOrganizationName() ;
    var org = Usergrid.organizations.getItemByName(name);
    var uuid = org.getUUID() ;
    $('#organizations-table').html('<tr class="zebraRows"><td>' + name + '</td><td class="monospace">' + uuid + '</td></tr>');
  }
  Usergrid.console.displayCurrentOrg = displayCurrentOrg;

  /*******************************************************************
   *
   * Startup
   *
   ******************************************************************/

  function showPanelList(type){
    $('#' + type + '-panel-search').hide();
    selectTabButton('#button-'+type+'-list');
    $('#' + type + '-panel-list').show();
  }

  $('#button-users-list').click(function() {
    showPanelList('users');
    return false;
  });

  $('#user-panel-tab-bar a').click(function() {
    //selectTabButton(this);
    selectPillButton(this);
    if ($(this).attr("id") == "button-user-list") {
      userLetter = '*';
      Pages.SelectPanel('users');
      showPanelList('users');
    } else {
      showPanelContent('#user-panel', '#user-panel-' + $(this).attr('id').substring(12));
    }
    return false;
  });

  $('#button-groups-list').click(function() {
    showPanelList('groups');
    return false;
  });

  $('#group-panel-tab-bar a').click(function() {
    selectPillButton(this);
    if ($(this).attr('id') == "button-group-list") {
      groupLetter = '*';
      Pages.SelectPanel('groups');
      showPanelList('groups');
    } else {
      showPanelContent('#group-panel', '#group-panel-' + $(this).attr('id').substring(13));
    }
    return false;
  });

  $('#roles-panel-tab-bar a').click(function() {
    if ($(this).attr('id') == "button-roles-list") {
      Pages.SelectPanel('roles');
      showPanelList('roles');
    }
    else if ($(this).attr('id') == "button-roles-search") {
      selectPillButton('#button-roles-search');
      $('#roles-panel-list').hide();
      $('#role-panel-users').hide();
      $('#roles-panel-search').show();
    } else {
      Pages.SelectPanel('roles');
    }
    return false;
  });

  $('#role-panel-tab-bar a').click(function() {
    if ($(this).attr('id') == "button-role-list") {
      Pages.SelectPanel('roles');
      showPanelList('roles');
    } else if ($(this).attr('id') == "button-role-settings") {
      selectPillButton('#button-role-settings');
      $('#roles-panel-list').hide();
      $('#role-panel-users').hide();
      $('#role-panel-groups').hide();
      $('#role-panel-settings').show();
    } else if ($(this).attr('id') == "button-role-users") {
      selectPillButton('#button-role-users');
      $('#roles-panel-list').hide();
      $('#role-panel-settings').hide();
      $('#role-panel-groups').hide();
      $('#role-panel-users').show();
    } else if ($(this).attr('id') == "button-role-groups") {
      selectPillButton('#button-role-groups');
      $('#roles-panel-list').hide();
      $('#role-panel-settings').hide();
      $('#role-panel-users').hide();
      $('#role-panel-groups').show();
    } else {
      Pages.SelectPanel('roles');
    }
    return false;
  });

  $('button, input:submit, input:button').button();

  $('select#indexSelect').change( function(e){
    $('#query-ql').val($(this).val() || "");
  });

  doBuildIndexMenu();

  function enableApplicationPanelButtons() {
    $('#application-panel-buttons').show();
  }

  function disableApplicationPanelButtons() {
    $('#application-panel-buttons').hide();
  }

  function forceNewApp() {
    $('#dialog-form-force-new-application').modal();
  }

  $('#system-panel-button-home').addClass('ui-selected');
  $('#application-panel-buttons .ui-selected').removeClass('ui-selected');

  $('#start-date').datepicker();
  $('#start-date').datepicker('setDate', Date.last().sunday());
  $('#start-time').val("12:00 AM");
  $('#start-time').timepicker({
    showPeriod: true,
    showLeadingZero: false
  });

  $('#end-date').datepicker();
  $('#end-date').datepicker('setDate', Date.next().sunday());
  $('#end-time').val("12:00 AM");
  $('#end-time').timepicker({
    showPeriod: true,
    showLeadingZero: false
  });

  $('#button-analytics-generate').click(function() {
    requestApplicationCounters();
    return false;
  });

  $('#link-signup-login').click(function() {
    Pages.ShowPage('login');
    return false;
  });

  if (OFFLINE) {
    Pages.ShowPage(OFFLINE_PAGE);
  }

  function showLoginForNonSSO(){
    if (!Usergrid.userSession.loggedIn() && !Usergrid.SSO.usingSSO()) {
      Pages.ShowPage('login');
    }
  }
  Usergrid.console.showLoginForNonSSO = showLoginForNonSSO;

  function loginOk(){
    $('#login-message').hide();
    $('#login-email').val("");
    $('#login-password').val("");
    Pages.ShowPage('console');
  }

  Usergrid.console.loginOk = loginOk;

  //load the templates only after the rest of the page is
  $(window).bind("load", function() {
    Usergrid.console.ui.loadTemplate("apigee.ui.users.table_rows.html");
    Usergrid.console.ui.loadTemplate("apigee.ui.groups.table_rows.html");
    Usergrid.console.ui.loadTemplate("apigee.ui.roles.table_rows.html");
    Usergrid.console.ui.loadTemplate("apigee.ui.role.groups.table_rows.html");
    Usergrid.console.ui.loadTemplate("apigee.ui.activities.table_rows.html");
    Usergrid.console.ui.loadTemplate("apigee.ui.collections.table_rows.html");
    Usergrid.console.ui.loadTemplate("apigee.ui.panels.role.users.html");
    Usergrid.console.ui.loadTemplate("apigee.ui.panels.role.permissions.html");
    Usergrid.console.ui.loadTemplate("apigee.ui.panels.user.profile.html");
    Usergrid.console.ui.loadTemplate("apigee.ui.panels.user.memberships.html");
    Usergrid.console.ui.loadTemplate("apigee.ui.panels.user.activities.html");
    Usergrid.console.ui.loadTemplate("apigee.ui.panels.user.graph.html");
    Usergrid.console.ui.loadTemplate("apigee.ui.panels.user.permissions.html");
    Usergrid.console.ui.loadTemplate("apigee.ui.collection.table_rows.html");
    Usergrid.console.ui.loadTemplate("apigee.ui.collections.query.indexes.html");
    Usergrid.console.ui.loadTemplate("apigee.ui.panels.group.details.html");
    Usergrid.console.ui.loadTemplate("apigee.ui.panels.group.memberships.html");
    Usergrid.console.ui.loadTemplate("apigee.ui.panels.group.activities.html");
    Usergrid.console.ui.loadTemplate("apigee.ui.panels.group.permissions.html");
    Usergrid.console.ui.loadTemplate("apigee.ui.curl.detail.html");
    $(window).resize();
  });

  //these templates are used on the front page and should be loaded up front
  Usergrid.console.ui.loadTemplate("apigee.ui.applications.table_rows.html");
  Usergrid.console.ui.loadTemplate("apigee.ui.admins.table_rows.html");
  Usergrid.console.ui.loadTemplate("apigee.ui.feed.table_rows.html");

  if (Usergrid.showNotifcations) {
    $("#notifications-link").show();
  }
}
