function usergrid_console_app(Pages) {
  //This code block *WILL NOT* load before the document is complete
  window.usergrid = window.usergrid || {};
  usergrid.console = usergrid.console || {};
  client = usergrid.client || {};

  var OFFLINE = false;
  var OFFLINE_PAGE = "#query-page";

  var self = this;

  var emailRegex = new RegExp("^(([0-9a-zA-Z]+[_\+.-]?)+@[0-9a-zA-Z]+[0-9,a-z,A-Z,.,-]*(.){1}[a-zA-Z]{2,4})+$");
  var emailAllowedCharsMessage = 'eg. example@apigee.com';

  var passwordRegex = new RegExp("^([0-9a-zA-Z@#$%^&!?<>;:.,'\"~*-=+_\[\\](){}/\\ |])+$");
  var passwordAllowedCharsMessage = 'Password field only allows: A-Z, a-z, 0-9, ~ @ # % ^ & * ( ) - _ = + [ ] { } \\ | ; : \' " , . < > / ? !';

  var usernameRegex = new RegExp("^([0-9a-zA-Z\.\-])+$");
  var usernameAllowedCharsMessage = 'Username field only allows : A-Z, a-z, 0-9, dot, and dash';

  var organizatioNameRegex = new RegExp ("^([0-9a-zA-Z.-])+$");
  var organizationNameAllowedCharsMessage = 'Organization name field only allows : A-Z, a-z, 0-9, dot, and dash';

  var nameRegex = new RegExp("^([0-9a-zA-Z@#$%^&!?;:.,'\"~*-=+_\[\\](){}/\\ |])+$");
  var nameAllowedCharsMessage = 'Name field only allows: A-Z, a-z, 0-9, ~ @ # % ^ & * ( ) - _ = + [ ] { } \\ | ; : \' " , . / ? !';

  var titleRegex = new RegExp("^([0-9a-zA-Z.!-?/])+$");
  var titleAllowedCharsMessage = 'Title field only allows : space, A-Z, a-z, 0-9, dot, dash, /, !, and ?';

  var alphaNumRegex = new RegExp("^([0-9a-zA-Z])+$");
  var alphaNumAllowedCharsMessage = 'Collection name only allows : a-z 0-9';

  var pathRegex = new RegExp("^([0-9a-z./-])+$");
  var pathAllowedCharsMessage = 'Path only allows : /, a-z, 0-9, dot, and dash';

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
  var client = usergrid.client;
  Pages.resetPasswordUrl = client.resetPasswordUrl;

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
    var bearerToken = session.getAccessToken();
    var app_name = session.getApplicationId();
    if (typeof current_application_name != 'string') {
      app_name = '';
    }
    var org_name = session.getOrganizationObj().name;
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
    var url = 'https://apigee.com/console/usergrid?v=2&embedded=true&auth=' + bearerTokenString;
    return url;
  }
  usergrid.console.getAccessTokenURL = getAccessTokenURL;

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

  function selectFirstTabButton(bar){
    selectTabButton($(bar).find("li:first-child a"));
  }

  function setNavApplicationText() {
    if(!current_application_name) {
      current_application_name = "Select an Application";
    }
    $('#current-app-name').html(current_application_name+ '  <span style="float: right">&#9660;</span>');
    $('.thingy span.title span.app_title').text(" - " + current_application_name);
  }

  /*******************************************************************
   *
   * Query Explorer
   *
   ******************************************************************/

  $("#query-source").val("{ }");

  function pageOpenQueryExplorer(collection) {
    collection = collection || "";
    showPanel("#query-panel");
    hideMoreQueryOptions();
    //reset the form fields
    $("#query-path").val(collection);
    $("#query-source").val("{ }");
    $("#query-ql").val("");
    query_history = [];
    //bind events for previous and next buttons
    bindPagingEvents('query-response');
    //clear out the table before we start
    var output = $('#query-response-table');
    output.empty();
    //if a collection was provided, go ahead and get the default data
    if (collection) {
      getCollection('GET');
    }
  }
  window.usergrid.console.pageOpenQueryExplorer = pageOpenQueryExplorer;

  function getCollection(method){
    //get the data to run the query
    var path = $("#query-path").val();
    var data = $("#query-source").val();
    var ql = $("#query-ql").val();

    //merge the data and the query
    var params = {"ql":ql};
    data = JSON.parse(data);

    //make a new query object
    queryObj = new client.queryObj(method, path, data, params, getCollectionCallback, function() { alertModal("Error", "Unable to retrieve collection data.") });
    //store the query object on the stack
    pushQuery(queryObj);
    //then run the query
    runAppQuery(queryObj);
  }


  function getCollectionCallback(response) {
    hidePagination('query-response');

    query_entities = null;
    query_entities_by_id = null;
    var t = "";
    if (response.entities && (response.entities.length > 0)) {
      query_entities = response.entities;
      query_entities_by_id = {};

      var path = response.path || "";
      path = "" + path.match(/[^?]*/);

      if (response.entities.length > 1) {
        for (i in query_entities) {
          var entity = query_entities[i];
          query_entities_by_id[entity.uuid] = entity;

          var entity_path = (entity.metadata || {}).path;
          if ($.isEmptyObject(entity_path)) {
            entity_path = path + "/" + entity.uuid;
          }

          t += "<div class=\"query-result-row entity_list_item\" id=\"query-result-row-"
            + i
            + "\" data-entity-type=\""
            + entity.type
            + "\" data-entity-id=\"" + entity.uuid + "\" data-collection-path=\""
            + entity_path
            + "\"></div>";
        }

        $("#query-response-table").html(t);
        $(".entity_list_item").loadEntityCollectionsListWidget();
      } else {
        var entity = response.entities[0];
        query_entities_by_id[entity.uuid] = entity;

        var entity_path = (entity.metadata || {}).path;
        if ($.isEmptyObject(entity_path)) {
          entity_path = path + "/" + entity.uuid;
        }

        t = '<div class="query-result-row entity_detail" id="query-result-detail" data-entity-type="'
          + entity.type
          + '" data-entity-id="' + entity.uuid + '" data-collection-path="'
          + entity_path
          + '"></div>';

        $("#query-response-table").html(t);
        $('#query-result-detail').loadEntityCollectionsDetailWidget();
      }
      showBackButton();
      showPagination('query-response');
    }

  }

  function pushQuery(queryObj) {
    //store the query object on the stack
    query_history.push(queryObj);
  }

  function popQuery() {
    if (query_history.length < 1) {
      $('#button-query-back').hide();
      return;
    }
    //get the last query off the stack
    query_history.pop(); //first pull off the current query
    lastQueryObj = query_history.pop(); //then get the previous one
    //set the path in the query explorer
    $("#query-path").val(lastQueryObj.getPath());
    //get the ql and set it in the query explorer
    var params = lastQueryObj.getParams();
    $("#query-ql").val(params.ql);
    //delete the ql from the json obj -it will be restored later when the query is run in getCollection()
    var jsonObj = lastQueryObj.getJsonObj();
    delete jsonObj.ql;
    //set the data obj in the query explorer
    $("#query-source").val(JSON.stringify(jsonObj));

    showBackButton();

    getCollection(lastQueryObj.getMethod());
  }

  //helper function for query explorer back button
  function showBackButton() {
    if (query_history.length > 1){
      $('#button-query-back').show();
    } else {
      $('#button-query-back').hide();
    }
  }

  $.fn.loadEntityCollectionsListWidget = function() {
    this.each(function() {
      var entityType = $(this).dataset('entity-type');
      var entityUIPlugin = "usergrid_collections_" + entityType + "_list_item";
      if (!$(this)[entityUIPlugin]) {
        entityUIPlugin = "usergrid_collections_entity_list_item";
      }
      $(this)[entityUIPlugin]();
    });
  };

  $.fn.loadEntityCollectionsDetailWidget = function() {
    this.each(function() {
      var entityType = $(this).dataset('entity-type');
      var entityUIPlugin = "usergrid_collections_" + entityType + "_detail";
      if (!$(this)[entityUIPlugin]) {
        entityUIPlugin = "usergrid_collections_entity_detail";
      }
      $(this)[entityUIPlugin]();
    });
  };

  function getQueryResultEntity(id) {
    if (query_entities_by_id) {
      return query_entities_by_id[id];
    }
    return null;
  }
  window.usergrid.console.getQueryResultEntity = getQueryResultEntity;

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
    $('#query-source').focus(expandQueryInput);
    $('#button-query-shrink').click(shrinkQueryInput);
    $('#button-query-expand').click(expandQueryInput);

    $('#button-query-back').click(function() {popQuery();return false;} );

    $('#button-query-get').click(function() {getCollection('GET');return false;} );
    $('#button-query-post').click(function() {getCollection('POST');return false;} );
    $('#button-query-put').click(function() {getCollection('PUT');return false;} );
    $('#button-query-delete').click(function() {getCollection('DELETE');return false;} );
    $('#button-query-validate').click(function() {validateJson();return false;});
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
    $('#query-source').val("{ }");
  }

  function toggleMoreQueryOptions() {
    $('.query-more-options').toggle();
    $('.query-less-options').toggle();
    $('#query-ql').val("");
    $('#query-source').val("{ }");
  }

  $('#button-query-more-options').click(function() {
    toggleMoreQueryOptions();
    return false;
  });

  $('#button-query-less-options').click(function() {
    toggleMoreQueryOptions();
    return false;
  });

  $('#query-source').keypress(function(event) {
    if (event.keyCode == 13) {
      validateJson();
    }
  });

  function validateJson() {
    try {
      var result = jsonlint.parse($('#query-source').val());
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

  $('#button-clear-query-source').click(function(event) {
    shrinkQueryInput();
    $('#query-source').val("{ }");
    return false;
  });

  $('#button-clear-path').click(function(event) {
    $('#query-path').val("");
    return false;
  });

  $('#button-clear-ql').click(function(event) {
    $('#query-ql').val("");
    return false;
  });

  window.usergrid.console.doChildClick = function(event) {
    var path = new String($('#query-path').val());
    if (!path.endsWith("/")) {
      path += "/";
    }
    path += event.target.innerText;
    $('#query-path').val(path);
  };

  var queryQl = $('#query-ql');
  queryQl.typeahead();

  function doBuildIndexMenu() {
    queryQl.data('typeahead').source = indexes;
  }

  $('#delete-entity-link').click(deleteEntity);
  function deleteEntity(e) {
    e.preventDefault();

    var items = $('#query-response-table input[id^=queryResultItem]:checked');
    if(!items.length){
      alertModal("Please, first select the entities you want to delete.");
      return;
    }

    confirmDelete(function(){
      items.each(function() {
        var entityId = $(this).attr('value');
        var path = $(this).attr('name');
        runAppQuery(new client.queryObj("DELETE", path + "/" + entityId, null, null,
          getCollection('GET'),
          function() { alertModal("Unable to delete entity"); }
        ));
      });
    });
  }

  /*******************************************************************
   *
   * Organization Home
   *
   ******************************************************************/

  function pageSelectHome() {
    setupMenu();
    Pages.SelectPanel('organization');
    requestApplications();
    requestAdmins();
    requestOrganizationCredentials();
    requestAdminFeed();
  }
  window.usergrid.console.pageSelectHome = pageSelectHome;

  $(document).on('click','#go-home', pageSelectHome);

  function displayApplications(response) {
    var t = "";
    var m2 = "";
    applications = {};
    applications_by_id = {};
    var appMenu = $('#applications-menu');
    var appList = $('table#organization-applications-table');
    appMenu.empty();
    appList.empty();

    if (response.data) {
      applications = response.data;
      var count = 0;
      var applicationNames = keys(applications).sort();
      var data = [];
      var appMenuTmpl = $('<li><a href="#">${name}</a></li>');

      for (var i in applicationNames) {
        var name = applicationNames[i];
        var uuid = applications[name];
        data.push({uuid:uuid, name:name.split("/")[1]});
        count++;
        applications_by_id[uuid] = name.split("/")[1];
      }

      if (count) {
        $.tmpl('usergrid.ui.applications.table_rows.html', data).appendTo(appList);
        appMenuTmpl.tmpl(data).appendTo(appMenu);
        appMenu.find("a").click(function selectApp(e) {
          var link = $(this);
          pageSelect(link.tmplItem().data.uuid);
          Pages.SelectPanel('application');
        });
        
        appList.find("a").click(function selectApp(e) {
          e.preventDefault();
          var link = $(this);
          pageSelect(link.tmplItem().data.uuid);
          Pages.SelectPanel('application');
        });
        enableApplicationPanelButtons();
        selectFirstApp();
      }
      appMenu.append('<li class="divider"></li>');
      appMenu.append('<li><a class="" data-toggle="modal" href="#dialog-form-new-application"> <strong>+</strong> New Application</a></li>');
    }

    if(appList.is(":empty")){
      appList.html('<div class="alert user-panel-section">No applications created.</div>');
      appMenu.html('<li>--No Apps--</li>');
      forceNewApp();
    }
  }

  function requestApplications() {
    var sectionApps = $('#organization-applications-table');
    sectionApps.empty().html('<div class="alert alert-info user-panel-section">Loading...</div>');
    runManagementQuery(new client.queryObj("GET","organizations/" + session.getOrganizationUUID() + "/applications", null, null,
      displayApplications,
      function() { sectionApps.html('<div class="alert user-panel-section">Unable to retrieve application list.</div>'); }
    ));
  }

  function selectFirstApp() {
    applicationId = session.getApplicationId();
    if(applicationId && applications_by_id[applicationId])
      pageSelect(applicationId);
    else {
      var organization = session.getOrganizationObj();
      var firstApp = null;
      for (firstApp in organization.applications) {break};
      if (firstApp) {
        pageSelect(organization.applications[firstApp]);
        session.setApplicationId(organization.applications[firstApp]);
      }
    }
  }

  function displayAdmins(response) {
    var sectionAdmins = $('#organization-admins-table');
    sectionAdmins.empty();
    if (response.data) {
      var admins = {};
      admins = response.data;
      admins = admins.sort();
      for (var i in admins) {
        var admin = admins[i];
        admin.gravatar = get_gravatar(admin.email, 20);
        $.tmpl('usergrid.ui.admins.table_rows.html', admin).appendTo(sectionAdmins);
      }
    }
    if(sectionAdmins.is(':empty')){
      sectionAdmins.html('<div class="alert user-panel-section">No organization administrators.</div>');
    }
  }

  function requestAdmins() {
    var sectionAdmins =$('#organization-admins-table');
    sectionAdmins.empty().html('<div class="alert alert-info user-panel-section">Loading...</div>');
    runManagementQuery(new client.queryObj("GET","organizations/" + session.getOrganizationUUID() + "/users", null, null,
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
    return 'https://secure.gravatar.com/avatar/' + MD5(email) + '?d=identicon&s=' + size;
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
        $.tmpl('usergrid.ui.feed.table_rows.html', activity).appendTo(sectionActivities);
      }
    }

    if (sectionActivities.is(":empty")) {
      sectionActivities.html('<div class="alert user-panel-section">No activities.</div>');
    }
  }

  function requestAdminFeed() {
    var section =$('#organization-activities');
    section.empty().html('<div class="alert alert-info">Loading...</div>');
    runManagementQuery(new client.queryObj("GET","orgs/" + session.getOrganizationUUID() + "/feed", null, null, displayAdminFeed,
      function() { section.html('<div class="alert">Unable to retrieve feed.</div>'); }));
  }
  window.usergrid.console.requestAdminFeed = requestAdminFeed;

  var organization_keys = { };

  function requestOrganizationCredentials() {
    $('#organization-panel-key').html('<div class="alert alert-info marginless">Loading...</div>');
    $('#organization-panel-secret').html('<div class="alert alert-info marginless">Loading...</div>');
    runManagementQuery(new client.queryObj("GET",'organizations/'+ session.getOrganizationUUID() + "/credentials", null, null,
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
    runManagementQuery(new client.queryObj("POST",'organizations/' + session.getOrganizationUUID()  + "/credentials",null, null,
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
  window.usergrid.console.newOrganizationCredentials = newOrganizationCredentials;

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
  $('#dialog-form-add-role-to-user').submit(submitAddRoleToUser);
  $('#dialog-form-add-group-to-role').submit(submitAddGroupToRole);

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
      runManagementQuery(new client.queryObj("POST","organizations/" + session.getOrganizationUUID() + "/applications", form.serializeObject(), null,
        function(response) {
          for (var elem in response.data) { break; }
          pageSelect(response.data[elem]);
          requestApplications(response);
        },
        function() {
          closeErrorMessage = function() {
            $('#home-messages').hide();
          };
          var closebutton = '<a href="#" onclick="closeErrorMessage();" class="close">&times;</a>'
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
      runManagementQuery(new client.queryObj("POST","organizations/" + session.getOrganizationUUID() + "/users", data, null,
        requestAdmins,
        function () { alertModal("Unable to create admin"); }
      ));
      $(this).modal('hide');
    }
  }

  function submitNewOrg() {
    var form = $(this);
    formClearErrors(form);

    var new_organization_name = $('#new-organization-name');

    var bValid = checkLength2(new_organization_name, 4, 80)
      && checkRegexp2(new_organization_name, organizatioNameRegex, organizationNameAllowedCharsMessage);

    if (bValid) {
      var data = form.serializeObject();
      runManagementQuery(new client.queryObj("POST","users/" + session.getLoggedInUserUUID() + "/organizations", data, null,
        requestOrganizations,
        function() { alertModal("Unable to create organization"); }
      ));
      $(this).modal('hide');
    }
  }

  function submitNewUser() {
    var form = $(this);
    formClearErrors(form);

    var new_user_email = $('#new-user-email');
    var new_user_username = $('#new-user-username');
    var new_user_fullname = $('#new-user-fullname');
    var new_user_password = $('#new-user-password');

    var bValid =
      checkLength2(new_user_fullname, 1, 80)
      && checkRegexp2(new_user_fullname, nameRegex, nameAllowedCharsMessage)
      && checkRegexp2(new_user_username, usernameRegex, usernameAllowedCharsMessage)
      && checkLength2(new_user_email, 6, 80)
      && checkRegexp2(new_user_email,emailRegex, emailAllowedCharsMessage)
      && checkLength2(new_user_password, 5, 16)
      && checkRegexp2(new_user_password,passwordRegex, passwordAllowedCharsMessage);

    if (bValid) {
      var data = form.serializeObject();
      runAppQuery(new client.queryObj("POST", 'users', data, null,
        function() {
          getUsers();
          closeErrorMessage = function() {
            $('#users-messages').hide();
          };
          var closebutton = '<a href="#" onclick="closeErrorMessage();" class="close">&times;</a>'
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
          var closebutton = '<a href="#" onclick="closeErrorMessage();" class="close">&times;</a>'
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
      && checkRegexp2(new_role_name, usernameRegex, usernameAllowedCharsMessage)
      && checkLength2(new_role_title, 1, 80)
      && checkRegexp2(new_role_title,titleRegex, titleAllowedCharsMessage);

    if (bValid) {
      var data = form.serializeObject();
      runAppQuery(new client.queryObj("POST", "rolenames", data, null,
        function() {
          getRoles();
          closeErrorMessage = function() {
            $('#roles-messages').hide();
          };
          var closebutton = '<a href="#" onclick="closeErrorMessage();" class="close">&times;</a>'
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
          var closebutton = '<a href="#" onclick="closeErrorMessage();" class="close">&times;</a>'
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
      runAppQuery(new client.queryObj("PUT", "", metadata, null,
        function() {
          getCollections();
          closeErrorMessage = function() {
            $('#collections-messages').hide();
          };
          var closebutton = '<a href="#" onclick="closeErrorMessage();" class="close">&times;</a>'
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
          var closebutton = '<a href="#" onclick="closeErrorMessage();" class="close">&times;</a>'
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
      runAppQuery(new client.queryObj("POST", "groups", data, null,
        function() {
          getGroups();
          closeErrorMessage = function() {
          $('#groups-messages').hide();
        };
        var closebutton = '<a href="#" onclick="closeErrorMessage();" class="close">&times;</a>'
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
          var closebutton = '<a href="#" onclick="closeErrorMessage();" class="close">&times;</a>'
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

      runAppQuery(new client.queryObj("POST", "/groups/" + groupId + "/users/" + userId, null, null,
        function() { requestUser(userId); },
        function() { alertModal("Unable to add group to user"); }
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
      runAppQuery(new client.queryObj("POST", "/groups/" + groupId + "/users/" + userId, null, null,
        function() { requestGroup(groupId); },
        function() { alertModal("Unable to add user to group"); }
      ));
      $(this).modal('hide');
    }
  }

  function submitAddRoleToUser() {
    var form = $(this);
    formClearErrors(form);
    var roleIdField = $('#search-roles-user-name-input');
    var bValid = checkLength2(roleIdField, 1, 80) && checkRegexp2(roleIdField, usernameRegex, usernameAllowedCharsMessage)
    var username = $('#search-roles-user-name-input').val();
    if (bValid) {
      runAppQuery(new client.queryObj("POST", "/roles/" + current_role_id + "/users/" + username, null, null,
        function() { pageSelectRoleUsers(current_role_id, current_role_name); },
        function() { alertModal("Unable to add user to role"); }
      ));
      $(this).modal('hide');
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
      runAppQuery(new client.queryObj("POST", "/roles/" + roleId + "/users/" + username, null, null,
        function() { pageSelectUserPermissions(username); },
        function() { alertModal("Unable to add user to role"); }
      ));

      $(this).modal('hide');
    }
  }

  function deleteUsersFromRoles(username) {
    var items = $('input[class^=userRoleItem]:checked');
    if(!items.length){
      alertModal("Error", "Please, first select the roles you want to delete for this user.");
      return;
    }

    confirmDelete(function(){
        items.each(function() {
          var roleId = $(this).attr("value");
          runAppQuery(new client.queryObj("DELETE", "/users/" + username + "/rolename/" + roleId, null, null,
            function() { pageSelectUserPermissions (username); },
            function() { alertModal("Unable to remove user from role"); }
          ));
      });
    });
  }
  window.usergrid.console.deleteUsersFromRoles = deleteUsersFromRoles;

  function deleteRoleFromUser(roleId, rolename) {
    var items = $('#role-users input[class^=userRoleItem]:checked');
    if(!items.length){
      alertModal("Error", "Please, first select the users you want to delete from this role.")
        return;
    }

    confirmDelete(function(){
        items.each(function() {
          var username = $(this).attr("value");
          runAppQuery(new client.queryObj("DELETE", "/users/" + username + "/roles/" + roleId, null, null,
            function() { pageSelectRoleUsers (roleId, rolename); },
            function() { alertModal("Unable to remove user from role"); }
          ));
      });
    });
  }
  window.usergrid.console.deleteRoleFromUser = deleteRoleFromUser;

  function removeUserFromGroup(userId) {
    var items = $('#user-panel-memberships input[class^=userGroupItem]:checked');
    if(!items.length){
      alertModal("Error", "Please, first select the groups you want to delete for this user.")
        return;
    }
    confirmDelete(function(){
      items.each(function() {
        var groupId = $(this).attr("value");
        runAppQuery(new client.queryObj("DELETE", "/groups/" + groupId + "/users/" + userId, null, null,
          function() { pageSelectUserGroups (userId); },
          function() { alertModal("Unable to remove user from group"); }
        ));
      });
    });
  }
  window.usergrid.console.removeUserFromGroup = removeUserFromGroup;

  function removeGroupFromUser(groupId) {
    var items = $('#group-panel-memberships input[id^=userGroupItem]:checked');
    if (!items.length) {
      alertModal("Error", "Please, first select the users you want to from this group.");
      return;
    }

    confirmDelete(function(){
      items.each(function() {
        var userId = $(this).attr("value");
        runAppQuery(new client.queryObj("DELETE", "/groups/" + groupId + "/users/" + userId, null, null,
          function() { pageSelectGroupMemberships (groupId); },
          function() { alertModal("Unable to remove user from group"); }
        ));
      });
    });
  }
  window.usergrid.console.removeGroupFromUser = removeGroupFromUser;

  /*******************************************************************
   *
   * Generic page select
   *
   ******************************************************************/
  function pageSelect(uuid) {
    if (uuid) {
      current_application_id = uuid;
      current_application_name = applications_by_id[uuid];
      session.setApplicationId(uuid);
    }
    setNavApplicationText();
    getCollections();
    query_history = [];
  }
  window.usergrid.console.pageSelect = pageSelect;


  /*******************************************************************
   *
   * Application
   *
   ******************************************************************/

  function pageSelectApplication() {
    pageSelect();
    requestApplicationCredentials();
    requestApplicationUsage();
  }
  window.usergrid.console.pageSelectApplication = pageSelectApplication;

  function updateApplicationDashboard() {
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
        is3D: true,
        backgroundColor: backgroundGraphColor
      }
    );

    $('#application-panel #application-panel-text').html(t);
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

    runAppQuery(new client.queryObj("GET", "counters", null, params,
      function(response) {
        var usage_counters = response.counters;

        if (!usage_counters) {
          $('#application-entities-timeline').html("");
          $('#application-cpu-time').html("");
          $('#application-data-uploaded').html("");
          $('#application-data-downloaded').html("");
          return;
        }

        var graph_width = 400;
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
  window.usergrid.console.requestApplicationUsage = requestApplicationUsage;

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

  function showCurlCommand(section, curl) {
    $('#'+section+'-curl').val(curl);
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
    client.runAppQuery(obj);
    return false;
  }

  function runManagementQuery(_queryObj) {
    var obj = _queryObj || queryObj;
    client.runManagementQuery(obj);
    return false;
  }

  /*******************************************************************
   *
   * Users
   *
   ******************************************************************/
  var userLetter = "*";
  var userSortBy = "username";

  function pageSelectUsers(uuid) {
    //make a new query object
    queryObj = new client.queryObj(null);
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
  window.usergrid.console.pageSelectUsers = pageSelectUsers;

  function getUsers(search, searchType) {
    //clear out the table before we start
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

    queryObj = new client.queryObj("GET", "users", null, query, getUsersCallback, function() { alertModal("Error", "Unable to retrieve users."); });
    runAppQuery(queryObj);
  }

  function getUsersCallback(response) {
    hidePagination('users');
    var output = $('#users-table');
    if (response.entities.length < 1) {
      output.replaceWith('<div id="users-table" class="user-panel-section-message">No users found.</div>');
    } else {
      output.replaceWith('<table id="users-table" class="table"><tbody></tbody></table>');
      for (i = 0; i < response.entities.length; i++) {
        var this_data = response.entities[i];
        if (!this_data.picture) {
          this_data.picture = window.location.protocol+ "//" + window.location.host + window.location.pathname + "images/user_profile.png"
        } else {
          this_data.picture = this_data.picture.replace(/^http:\/\/www.gravatar/i, 'https://secure.gravatar');
          this_data.picture = this_data.picture + "?d="+window.location.protocol+"//" + window.location.host + window.location.pathname + "images/user_profile.png"
        }
        $.tmpl('usergrid.ui.users.table_rows.html', this_data).appendTo('#users-table');
      }
    }
    showPagination('users');
    //showCurlCommand('users', queryObj.getCurl());
  }

  function showUsersForSearch(search){
    selectFirstTabButton('#users-panel-tab-bar');
    $('#users-panel-search').hide();
    selectTabButton('#button-users-list');
    $('#users-panel-list').show();
    userLetter = search;
    getUsers();
  }
  usergrid.console.showUsersForSearch = showUsersForSearch;

  function searchUsers(){
    var search = $('#search-user-username').val();
    var searchType = ($('#search-user-type').val())?$('#search-user-type').val():userSortBy;
    //make sure the input is valid:
    if (searchType == 'name') {
      searchType = 'name';
    } else if (searchType == 'username') {searchType = 'username';}
    getUsers(search, searchType);
  }
  usergrid.console.searchUsers = searchUsers;

  function selectAllUsers(){
    $('[class=userListItem]').attr('checked', true);
    $('#deselectAllUsers').show();
    $('#selectAllUsers').hide();
  }
  window.usergrid.console.selectAllUsers = selectAllUsers;

  function deselectAllUsers(){
    $('[class=userListItem]').attr('checked', false);
    $('#selectAllUsers').show();
    $('#deselectAllUsers').hide();
  }
  window.usergrid.console.deselectAllUsers = deselectAllUsers;



  $('#delete-users-link').click(deleteUsers);
  function deleteUsers(e) {
    e.preventDefault();

    var items = $('#users-table input[class^=userListItem]:checked');
    if(!items.length){
      alertModal("Error", "Please, first select the users you want to delete.");
      return;
    }

    confirmDelete(function(){
      items.each(function() {
        var userId = $(this).attr("value");
        runAppQuery(new client.queryObj("DELETE", 'users/' + userId, null, null,
          getUsers,
          function() { alertModal("Unable to delete user - " + userId) }
        ));
      });
    });
  }

  /*******************************************************************
   *
   * User
   *
   ******************************************************************/

  function pageOpenUserProfile(userId) {
    Pages.SelectPanel('user');
    requestUser(userId);
    selectTabButton('#button-user-profile');
    showPanelContent('#user-panel', '#user-panel-profile');
  }
  window.usergrid.console.pageOpenUserProfile = pageOpenUserProfile;

  function pageOpenUserActivities(userId) {
    Pages.SelectPanel('user');
    requestUser(userId);
    selectTabButton('#button-user-activities');
    showPanelContent('#user-panel', '#user-panel-activities');
  }
  window.usergrid.console.pageOpenUserActivities = pageOpenUserActivities;

  function pageSelectUserPermissions(userId) {
    Pages.SelectPanel('user');
    requestUser(userId);
    selectTabButton('#button-user-permissions');
    showPanelContent('#user-panel', '#user-panel-permissions');
  }
  window.usergrid.console.pageSelectUserPermissions = pageSelectUserPermissions;

  function pageSelectUserGroups(userId) {
    Pages.SelectPanel('user');
    requestUser(userId);
    selectTabButton('#button-user-memberships');
    showPanelContent('#user-panel', '#user-panel-memberships');
  }
  window.usergrid.console.pageSelectUserGroups = pageSelectUserGroups;

  function saveUserProfile(uuid){
    var payload = usergrid.console.ui.jsonSchemaToPayload(usergrid.console.ui.collections.vcard_schema);
    runAppQuery(new client.queryObj("PUT", "users/"+uuid, payload, null,
      completeSave,
      function() { alertModal("Unable to update User"); }
    ));
  }
  window.usergrid.console.saveUserProfile = saveUserProfile;

  function completeSave(){
    closeMessage = function() {
      $('.messages').hide();
    };
    var closebutton = '<a href="#" onclick="closeMessage();" class="close">&times;</a>'
    $('.messages').text("Information Saved.").prepend(closebutton).show();
  }

  function redrawUserPanel() {
    $('#user-panel-profile').html("");
    $('#user-panel-memberships').html("");
    $('#user-panel-activities').html("");
    $('#user-panel-graph').html("");
    $('#user-panel-permissions').html("");

    if (user_data) {
      var details = $.tmpl('usergrid.ui.panels.user.profile.html', user_data);
      var formDiv = details.find('.query-result-form');
      $(formDiv).buildForm(usergrid.console.ui.jsonSchemaToDForm(usergrid.console.ui.collections.vcard_schema, user_data.entity));

      details.appendTo('#user-panel-profile');

      $.tmpl('usergrid.ui.panels.user.memberships.html', user_data).appendTo('#user-panel-memberships');
      updateGroupsAutocomplete();

      $.tmpl('usergrid.ui.panels.user.activities.html', user_data).appendTo('#user-panel-activities');
      $.tmpl('usergrid.ui.panels.user.graph.html', user_data).appendTo('#user-panel-graph');
      $.tmpl('usergrid.ui.panels.user.permissions.html', user_data).appendTo('#user-panel-permissions');
      updateRolesAutocomplete();
      updateQueryAutocompleteCollectionsUsers();
    }
  }

  function saveUserData(){
    usergrid.console.ui.jsonSchemaToPayload(schema, obj);
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
        picture = entity.picture + "?d="+window.location.protocol+"//" + window.location.host + window.location.pathname + "images/user_profile.png"
      }

      user_data = {
        entity : entity_contents,
        picture : picture,
        name : name,
        username : username,
        path : entity_path,
        collections : collections,
        metadata : metadata,
        uri : (entity.metadata || { }).uri
      };

      redrawUserPanel();

      runAppQuery(new client.queryObj("GET", 'users/' + entity.uuid + '/groups', null, null,
        function(response) {
          if (user_data && response.entities && (response.entities.length > 0)) {
            user_data.memberships = response.entities;
            redrawUserPanel();
          }
        },
        function() { alertModal("Error", "Unable to retrieve user's groups."); }
      ));

      runAppQuery(new client.queryObj("GET", 'users/' + entity.uuid + '/activities', null, null,
        function(response) {
          if (user_data && response.entities && (response.entities.length > 0)) {
            user_data.activities = response.entities;
            redrawUserPanel();
            $('span[id^=activities-date-field]').each( function() {
              var created = dateToString(parseInt($(this).html()))
              $(this).html(created);
            });
          }
        },
        function() { alertModal("Error", "Unable to retrieve user's activities."); }
      ));

      runAppQuery(new client.queryObj("GET", 'users/' + entity.uuid + '/roles', null, null,
        function(response) {
          if (user_data && response.entities && (response.entities.length > 0)) {
            user_data.roles = response.entities;
            redrawUserPanel();
          } else {
            user_data.roles = null;
          }
        },
        function() { alertModal("Error", "Unable to retrieve user's roles."); }
      ));

      runAppQuery(new client.queryObj("GET", 'users/' + entity.uuid + '/permissions', null, null,
        function(response) {
          var permissions = {};
          if (user_data && response.data && (response.data.length > 0)) {

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

              user_data.permissions = permissions;
              redrawUserPanel();
            }
          }
        },
        function() { alertModal("Error", "Unable to retrieve user's permissions."); }
      ));

      runAppQuery(new client.queryObj("GET", 'users/' + entity.uuid + '/following', null, null,
        function(response) {
          if (user_data && response.entities && (response.entities.length > 0)) {
            user_data.following = response.entities;
            redrawUserPanel();
          }
        },
        function() { alertModal("Error", "Unable to retrieve user's following."); }
      ));

      runAppQuery(new client.queryObj("GET", 'users/' + entity.uuid + '/followers', null, null,
        function(response) {
          if (user_data && response.entities && (response.entities.length > 0)) {
            user_data.followers = response.entities;
            redrawUserPanel();
          }
        },
        function() { alertModal("Error", "Unable to retrieve user's followers."); }
      ));
    }
  }

  function requestUser(userId) {
    $('#user-profile-area').html('<div class="alert alert-info">Loading...</div>');
    runAppQuery(new client.queryObj("GET", 'users/'+userId, null, null, handleUserResponse,
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
    //make a new query object
    queryObj = new client.queryObj(null);
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
  window.usergrid.console.pageSelectGroups = pageSelectGroups;

  function getGroups(search, searchType) {
    //clear out the table before we start
    var output = $('#groups-table');
    output.empty();

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

    queryObj = new client.queryObj("GET", "groups", null, query, getGroupsCallback, function() { alertModal("Error", "Unable to retrieve groups."); });
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
      output.replaceWith('<div id="groups-table" class="group-panel-section-message">No groups found.</div>');
    } else {
      output.replaceWith('<table id="groups-table" class="table"><tbody></tbody></table>');
      for (i = 0; i < response.entities.length; i++) {
        var this_data = response.entities[i];
        $.tmpl('usergrid.ui.groups.table_rows.html', this_data).appendTo('#groups-table');
      }
    }

    showPagination('groups');
  }

  function showGroupsForLetter(c) {
    groupLetter = c;
    getGroups();
  }
  usergrid.console.showGroupsForLetter = showGroupsForLetter;

  function showGroupsForSearch(search){
    selectFirstTabButton('#groups-panel-tab-bar');
    $('#groups-panel-search').hide();
    selectTabButton('#button-groups-list');
    $('#groups-panel-list').show();
    groupLetter = search;
    getGroups();
  }
  usergrid.console.showUsersForSearch = showUsersForSearch;

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
  usergrid.console.searchGroups = searchGroups;

  function selectAllGroups(){
    $('[class=groupListItem]').attr('checked', true);
    $('#deselectAllGroups').show();
    $('#selectAllGroups').hide();
  }
  window.usergrid.console.selectAllGroups = selectAllGroups;

  function deselectAllGroups(){
    $('[class=groupListItem]').attr('checked', false);
    $('#selectAllGroups').show();
    $('#deselectAllGroups').hide();
  }
  window.usergrid.console.deselectAllGroups = deselectAllGroups;

  $('#delete-groups-link').click(deleteGroups);

  function deleteGroups(e) {
    e.preventDefault();

    var items = $('#groups-table input[class^=groupListItem]:checked');
    if (!items.length) {
      alertModal("Error", "Please, first select the groups you want to delete.")
        return;
    }

    confirmDelete(function(){
      items.each(function() {
        var groupId = $(this).attr('value');
        runAppQuery(new client.queryObj("DELETE", "groups/" + groupId, null, null,
          getGroups,
          function() { alertModal("Unable to delete group"); }
        ));
      });
    });
  }

  /*******************************************************************
   *
   * Group
   *
   ******************************************************************/

  function pageOpenGroupProfile(groupId) {
    Pages.SelectPanel('group');
    requestGroup(groupId);
    selectTabButton('#button-group-details');
    showPanelContent('#group-panel', '#group-panel-details');
  }
  window.usergrid.console.pageOpenGroupProfile = pageOpenGroupProfile;

  function pageSelectGroupMemberships(groupId) {
    Pages.SelectPanel('group');
    requestGroup(groupId);
    selectTabButton('#button-group-memberships');
    showPanelContent('#group-panel', '#group-panel-memberships');
  }
  window.usergrid.console.pageSelectGroupMemberships = pageSelectGroupMemberships;

  function redrawGroupPanel() {
    $('#group-panel-details').html("");
    $('#group-panel-memberships').html("");
    $('#group-panel-activities').html("");
    $('#group-panel-permissions').html("");
    if (group_data) {
      var details = $.tmpl('usergrid.ui.panels.group.details.html', group_data);
      var formDiv = details.find('.query-result-form');
      $(formDiv).buildForm(usergrid.console.ui.jsonSchemaToDForm(usergrid.console.ui.collections.group_schema, group_data.entity));

      details.appendTo('#group-panel-details');
      details.find('.button').button();

      $.tmpl('usergrid.ui.panels.group.memberships.html', group_data).appendTo('#group-panel-memberships');
      updateUsersAutocomplete();

      $.tmpl('usergrid.ui.panels.group.activities.html', group_data).appendTo('#group-panel-activities');
      $.tmpl('usergrid.ui.panels.group.graph.html', group_data).appendTo('#group-panel-graph');

      if (group_data.roles && group_data.roles.length == 0) {
        delete group_data.roles
      }

      $.tmpl('usergrid.ui.panels.group.permissions.html', group_data).appendTo('#group-panel-permissions');
    }
  }

  function saveGroupProfile(uuid){
    var payload = usergrid.console.ui.jsonSchemaToPayload(usergrid.console.ui.collections.group_schema);
    runAppQuery(new client.queryObj("PUT", "groups/"+uuid, payload, null, completeSave,
      function() {
        closeErrorMessage = function() {
          $('#group-messages').hide();
        };
        var closebutton = '<a href="#" onclick="closeErrorMessage();" class="close">&times;</a>'
        $('#group-messages').text("Unable to update Group").prepend(closebutton).addClass('alert-error').show();
      }
    ));
  }

  window.usergrid.console.saveGroupProfile = saveGroupProfile;

  function selectAllGroupMemberships(){
    $('[id=userGroupItem]').attr('checked', true);
    $('#deselectAllGroupMemberships').show();
    $('#selectAllGroupMemberships').hide();
  }
  usergrid.console.selectAllGroupMemberships = selectAllGroupMemberships;

  function deselectAllGroupMemberships(){
    $('[id=userGroupItem]').attr('checked', false);
    $('#deselectAllGroupMemberships').hide();
    $('#selectAllGroupMemberships').show();
  }
  usergrid.console.deselectAllGroupMemberships = deselectAllGroupMemberships;

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

      group_data = {
        entity : entity_contents,
        picture : entity.picture,
        name : name,
        uuid : uuid,
        path : entity_path,
        collections : collections,
        metadata : metadata,
        uri : (entity.metadata || { }).uri
      };

      redrawGroupPanel();

      runAppQuery(new client.queryObj("GET",'groups/' + entity.uuid + '/users', null, null,
        function(response) {
          if (user_data && response.entities && (response.entities.length > 0)) {
            group_data.memberships = response.entities;
            redrawGroupPanel();
          }
        },
        function() { alertModal("Error", "Unable to retrieve group's users."); }
      ));

      runAppQuery(new client.queryObj("GET",'groups/' + entity.uuid + '/activities', null, null,
        function(response) {
          if (user_data && response.entities && (response.entities.length > 0)) {
            group_data.activities = response.entities;
            redrawGroupPanel();
          }
        },
        function() { alertModal("Error", "Unable to retrieve group's activities."); }
      ));

      runAppQuery(new client.queryObj("GET",'groups/' + entity.uuid + '/rolenames', null, null,
        function(response) {
          if (user_data && response.entities && (response.entities.length > 0)) {
            group_data.roles = response.data;
            redrawGroupPanel();
          }
        },
        function() { alertModal("Error", "Unable to retrieve group's activities."); }
      ));
     }
/*
      client.queryGroupMemberships(current_application_id, entity.uuid, function(response) {
        if (group_data && response.entities && (response.entities.length > 0)) {
          group_data.memberships = response.entities;
          redrawGroupPanel();
        }
      })

      client.queryGroupActivities(current_application_id, entity.uuid, function(response) {
        if (group_data && response.entities && (response.entities.length > 0)) {
          group_data.activities = response.entities;
          redrawGroupPanel();
        }
      })

      client.requestGroupRoles(current_application_id, entity.uuid, function(response) {
        if (group_data && response.entities) {
          group_data.roles = response.entities;
          redrawGroupPanel();
        }
      })
    }
*/
  }

  function requestGroup(groupId) {
    $('#group-details-area').html('<div class="alert alert-info">Loading...</div>');
    runAppQuery(new client.queryObj("GET",'groups/'+ groupId, null, null,
      handleGroupResponse,
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
    //make a new query object
    queryObj = new client.queryObj(null);
    //bind events for previous and next buttons
    bindPagingEvents('roles', getPreviousRoles, getNextRoles);
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
  window.usergrid.console.pageSelectRoles = pageSelectRoles;

  function getRoles(search, searchType) {
    //clear out the table before we start
    var output = $('#roles-table');
    output.empty();

    //put the sort by back in once the API is fixed
    //var query = {"ql" : "order by " + roleSortBy};
    var query = {};
    if (roleLetter != "*") query = {"ql" : roleSortBy + "='" + groupLetter + "*'"};

    queryObj = new client.queryObj("GET", "roles", null, query, getRolesCallback, function() { alertModal("Error", "Unable to retrieve roles."); });
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
    var output = $('#roles-table');
    if (response.entities.length < 1) {
      output.html('<div class="group-panel-section-message">No roles found.</div>');
    } else {
      for (i = 0; i < response.entities.length; i++) {
        var this_data = response.entities[i];
        $.tmpl('usergrid.ui.roles.table_rows.html', this_data).appendTo('#roles-table');
      }
    }
    showPagination('roles');
  }

  $('#delete-roles-link').click(deleteRoles);
  function deleteRoles(e) {
    e.preventDefault();

    var items = $('#roles-table input[class^=roleListItem]:checked');
    if(!items.length){
      alertModal("Error", "Please, first select the roles you want to delete.")
        return;
    }

    confirmDelete(function(){
      items.each(function() {
        var roleId = $(this).attr("value");
        runAppQuery(new client.queryObj("DELETE", "role/" + roleId, null, null, getRoles,
          function() { alertModal("Unable to delete role"); }
        ));
      });
    });
  }

  /*******************************************************************
   *
   * Role
   *
   ******************************************************************/

  var current_role_name = "";
  var current_role_id = "";

  function pageOpenRole(roleName, roleId) {
    current_role_name = roleName;
    current_role_id = roleId;
    requestRole();
    showPanel('#role-panel');
    $('#role-panel-list').hide();
    selectTabButton('#button-role-settings');
    $('#role-panel-settings').show();
  }
  window.usergrid.console.pageOpenRole = pageOpenRole;

  function pageSelectRoleUsers (roleName, roleId){
    current_role_name = roleName;
    current_role_id = roleId;
    requestRole();
    showPanel('#role-panel');
    $('#role-panel-list').hide();
    selectTabButton('#button-role-users');
    $('#role-panel-users').show();
  }
  window.usergrid.console.pageSelectRoleUsers = pageSelectRoleUsers;

  function pageSelectRoleGroups(roleName, roleId) {
    current_role_name = roleName;
    current_role_id = roleId;
    requestRole();
    showPanel('#role-panel');
    $('#role-panel-list').hide();
    selectTabButton('#button-role-groups');
    $('#role-panel-groups').show();
  }
  window.usergrid.console.pageSelectRoleGroups = pageSelectRoleGroups;

  var permissions = {};
  function displayPermissions(response) {
    var section = $('#role-permissions');
    section.empty();

    var t = "";
    var m = "";
    permissions = {};
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
      $.tmpl('usergrid.ui.panels.role.permissions.html', {"role" : current_role_name, "permissions" : permissions}, {}).appendTo('#role-permissions');
      updatePermissionAutocompleteCollections();
    } else {
      section.html('<div class="alert">No permission information retrieved.</div>');
    }
  }

  function displayRoleInactivity(response) {
    $('#role-inactivity-input').val(response.entities[0].inactivity);
    $('#role-inactivity-submit').on('click', function() {editRoleInactivity(); return false;});
  }

  var rolesUsersResults = ''
  function displayRolesUsers(response) {
    $('#role-users').html('');
    data = {};
    data.roleId = current_role_id;
    data.rolename = current_role_name;
    if (response.entities) {
      data.users = response.entities;
    }
    $.tmpl('usergrid.ui.panels.role.users.html', {"data" : data}, {}).appendTo('#role-users');
    updateUsersForRolesAutocomplete();
  }

  var rolesGroupsResults = ''
  function displayRoleGroups(response) {
    $('#role-groups').html('');
    data = {};
    data.roleId = current_role_id;
    data.rolename = current_role_name;
    $.tmpl('usergrid.ui.role.groups.table_rows.html', response.entities, {}).appendTo('#role-groups');
    updateGroupsForRolesAutocomplete();
  }

  function selectAllRolesUsers(){
    $('[class=userRoleItem]').attr('checked', true);
    $('#deselectAllRolesUsers').show();
    $('#selectAllRolesUsers').hide();
  }
  window.usergrid.console.selectAllRolesUsers = selectAllRolesUsers;

  function deselectAllRolesUsers(){
    $('[class=userRoleItem]').attr('checked', false);
    $('#selectAllRolesUsers').show();
    $('#deselectAllRolesUsers').hide();
  }
  window.usergrid.console.deselectAllRolesUsers = deselectAllRolesUsers;

  function requestRole() {
    $('#role-section-title').html("");
    $('#role-permissions').html("");
    $('#role-users').html("");
    //requestApplicationRoles
    runAppQuery(new client.queryObj("GET",'rolenames', null, null,
      function(response) {
        getRolesCallback(response);
        $('#role-section-title').html(current_role_name + " Role");
        $('#role-permissions').html('<div class="alert alert-info">Loading ' + current_role_name + ' permissions...</div>');
        //requestRole
        runAppQuery(new client.queryObj("GET", "roles/" + current_role_id, null, null,
          displayRoleInactivity,
          function() { $('#role-inactivity-form').html('<div class="alert">Unable to load role\'s inactivity value.</div>') }
        ));
        //requestApplicationRolePermissions
        runAppQuery(new client.queryObj("GET", "rolenames/" + current_role_name, null, null,
          function(response) { displayPermissions(response); },
          function() { $('#application-roles').html('<div class="alert">Unable to retrieve ' + current_role_name + ' role permissions.</div>'); }
        ));
        //requestApplicationRoleUsers
        runAppQuery(new client.queryObj("GET", "roles/" + current_role_id + "/users", null, null,
          function(response) { displayRolesUsers(response); },
          function() { $('#application-roles').html('<div class="alert">Unable to retrieve ' + current_role_name + ' role permissions.</div>'); }
        ));
        //requestGroupRoles
        runAppQuery(new client.queryObj("GET", "/roles/" + current_role_id + "/groups", null, null,
            function(response) { displayRoleGroups(response); },
            function() { $('#application-roles').html('<div class="alert">Unable to retrieve ' + current_role_name + ' role permissions.</div>'); }
          ));
      },
      function() {
        $('#application-roles').html('<div class="alert">Unable to retrieve roles list.</div>');
      }
    ));
  }

function deleteRolePermission(roleName, permission) {
    data = {"permission":permission};
    confirmDelete(function(){
      runAppQuery(new client.queryObj("DELETE", "rolenames/" + roleName, null, data, requestRole, requestRole));
    });
  }
  window.usergrid.console.deleteRolePermission = deleteRolePermission;

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
      runAppQuery(new client.queryObj("POST", "/rolenames/" + roleName, data, null, requestRole, requestRole));
    } else {
      alertModal('Please select a verb', ' ');
    }
  }
  window.usergrid.console.addRolePermission = addRolePermission;

  function editRoleInactivity() {
    var inactivity = $('#role-inactivity-input').val();
    var roleId = current_role_id;

    if (intRegex.test(inactivity)) {
      data = { inactivity: inactivity };
      runAppQuery(new client.queryObj("DELETE", "/roles/" + roleId, data, null, requestRole, requestRole));
    } else {
      $('#inactivity-integer-message').show()
    }
  }
  window.usergrid.console.editRoleInactivity = editRoleInactivity;

  function deleteUserPermission(userName, permission) {
    var data = {"permission": permission};
    confirmDelete(function(){
      runAppQuery(new client.queryObj("DELETE", "/users/" + userName + "/permissions", null, data,
        function() { pageSelectUserPermissions (userName); },
        function() { alertModal("Unable to delete permission"); }
      ));
    });
  }
  window.usergrid.console.deleteUserPermission = deleteUserPermission;

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
      runAppQuery(new client.queryObj("POST", "/users/" + userName + "/permissions/", data, null,
        function() { pageSelectUserPermissions (userName); },
        function() { alertModal("Unable to add permission"); }
      ));
    } else {
      alertModal('Please select a verb', '');
    }
  }
  window.usergrid.console.addUserPermission = addUserPermission;

  function submitAddGroupToRole() {
    var form = $(this);
    formClearErrors(form);

    var groupId = $('#search-roles-group-name-input');
    var bValid = checkLength2(groupId, 1, 80)
      && checkRegexp2(groupId, nameRegex, nameAllowedCharsMessage);

    if (bValid) {
      runAppQuery(new client.queryObj("POST", "/roles/" + current_role_id + "/groups/" + groupId.val(), null, null,
        function() { pageSelectRoleGroups(current_role_id, current_role_name); },
        function() { alertModal("Unable to add group to role", client.getLastErrorMessage("An internal error occured.")); }
      ));
      $(this).modal('hide');
    }
  }
  window.usergrid.console.submitAddGroupToRole = submitAddGroupToRole;

  function removeGroupFromRole() {
    var items = $('input[class=roleGroupItem]:checked');
    if (!items.length) {
      alertModal("Error", "Please, first select the groups you want to delete for this role.");
      return;
    }
    confirmDelete(function(){
      $.each(items, function() {
        var groupId = $(this).val();
        runAppQuery(new client.queryObj("DELETE", "/roles/" + current_role_id + "/groups/" + groupId, null, null,
	  function() {
	    pageSelectRoleGroups(current_role_id, current_role_name);
	  },
	  function() {
            alertModal("Error","Unable to remove group from role: " + client.getLastErrorMessage('An internal error occured'));
          }
	));
      });
    });

  }

  $('#remove-selected-role-groups').click(removeGroupFromRole);

  function selectAllRoleGroups(){
    $('[class=roleGroupItem]').attr('checked', true);
    $('#deselectAllRoleGroups').show();
    $('#selectAllRoleGroups').hide();
  }
  window.usergrid.console.selectAllRoleGroups = selectAllRoleGroups;

  function deselectAllRoleGroups(){
    $('[class=roleGroupItem]').attr('checked', false);
    $('#selectAllRoleGroups').show();
    $('#deselectAllRoleGroups').hide();
  }
  window.usergrid.console.deselectAllRoleGroups = deselectAllRoleGroups;

  /*******************************************************************
   *
   * Activities
   *
   ******************************************************************/
  var activitiesLetter = '*';
  var activitiesSortBy = 'created';

  function pageSelectActivities(uuid) {
    //make a new query object
    queryObj = new client.queryObj(null);
    //bind events for previous and next buttons
    bindPagingEvents('activities', getPreviousActivities, getNextActivities);
    //reset paging so we start at the first page
    queryObj.resetPaging();
    //the method to get the compile and call the query
    getActivities();
    //ui stuff
    showPanelList('activities');

  }
  window.usergrid.console.pageSelectActivities = pageSelectActivities;

  function getActivities(search, searchType) {
    //clear out the table before we start
    var output = $('#activities-table');
    output.empty();

    var query = {"ql" : "order by " + activitiesSortBy}; //default to built in search
    if (typeof search == 'string') {
      if (search.length > 0) {
        query = {"ql" : searchType + "='" + search + "*'"};
      }
    }

    queryObj = new client.queryObj("GET", "activities", {}, query, getActivitiesCallback, function() { alertModal("Error", "Unable to retrieve activities.")});
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
      output.replaceWith('<div id="activities-table" class="user-panel-section-message">No activities found.</div>');
    } else {
      output.replaceWith('<table id="activities-table" class="table"><tbody></tbody></table>');
      for (i = 0; i < response.entities.length; i++) {
        var this_data = response.entities[i];
        if (!this_data.actor.picture) {
          this_data.actor.picture = window.location.protocol+ "//" + window.location.host + window.location.pathname + "images/user_profile.png"
        } else {
          this_data.actor.picture = this_data.actor.picture.replace(/^http:\/\/www.gravatar/i, 'https://secure.gravatar');
        }

        $.tmpl('usergrid.ui.activities.table_rows.html', this_data).appendTo('#activities-table');
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
  usergrid.console.searchActivities = searchActivities;

  /*******************************************************************
   *
   * Analytics
   *
   ******************************************************************/

  function pageSelectAnalytics(uuid) {
    requestApplicationCounterNames();
  }
  window.usergrid.console.pageSelectAnalytics = pageSelectAnalytics;

  var application_counters_names = [];

  function requestApplicationCounterNames() {
    $('#analytics-counter-names').html('<div class="alert alert-info">Loading...</div>');
    runAppQuery(new client.queryObj("GET","counters", null, null,
      function(response) {
        application_counters_names = response.data;
        var html = usergrid.console.ui.makeTableFromList(application_counters_names, 1, {
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

    runAppQuery(new client.queryObj("GET","counters", null, params,
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
   * Settings
   *
   ******************************************************************/

  function pageSelectSettings(uuid) {
    requestApplicationCredentials();
    requestOrganizations();
  }
  window.usergrid.console.pageSelectSettings = pageSelectSettings;

  var application_keys = {};

  function requestApplicationCredentials() {
    $('#application-panel-key').html('<div class="alert alert-info">Loading...</div>');
    $('#application-panel-secret').html('<div class="alert alert-info">Loading...</div>');

    runAppQuery(new client.queryObj("GET", "credentials", null, null,
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

    runAppQuery(new client.queryObj("POST", "credentials", null, null,
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
  window.usergrid.console.newApplicationCredentials = newApplicationCredentials;

  /*******************************************************************
   *
   * Shell
   *
   ******************************************************************/

  function pageSelectShell(uuid) {
    requestApplicationCredentials();
    $('#shell-input').focus();
  }
  window.usergrid.console.pageSelectShell = pageSelectShell;

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
    var orgName = session.getOrganizationObj().uuid;

    if (s) {
      history.push(s);
      history_i = history.length - 1;
    }
    var path = '';
    var params = '';

    if (s.startsWith("/")) {
      path = encodePathString(s);
      printLnToShell(path);
      runAppQuery(new client.queryObj("GET",path, null, null, displayShellResponse,null));
    } else if (s.startsWith("get /")) {
      path = encodePathString(s.substring(4));
      printLnToShell(path);
      runAppQuery(new client.queryObj("GET",path, null, null, displayShellResponse,null));
    } else if (s.startsWith("put /")) {
      params = encodePathString(s.substring(4), true);
      printLnToShell(params.path);
      runAppQuery(new client.queryObj("PUT",params.path, params.payload, null, displayShellResponse,null));
  } else if (s.startsWith("post /")) {
      params = encodePathString(s.substring(5), true);
      printLnToShell(params.path);
      runAppQuery(new client.queryObj("POST",params.path, params.payload, null, displayShellResponse,null));
    } else if (s.startsWith("delete /")) {
      path = encodePathString(s.substring(7));
      printLnToShell(path);
      runAppQuery(new client.queryObj("DELETE",path, null, null, displayShellResponse,null));
    } else if ((s == "clear") || (s == "cls"))  {
      $('#shell-output').html(" ");
    } else if (s == "help") {
      printLnToShell("/&lt;path&gt; - API get request");
      printLnToShell("get /&lt;path&gt; - API get request");
      printLnToShell("put /&lt;path&gt; {&lt;json&gt;} - API put request");
      printLnToShell("post /&lt;path&gt; {&lt;json&gt;} - API post request");
      printLnToShell("delete /&lt;path&gt; - API delete request");
      printLnToShell("cls, clear - clear the screen");
      printLnToShell("help - show this help");
    } else if (s == "") {
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
   * Collections
   *
   ******************************************************************/

  function pageSelectCollections(uuid) {
    getCollections();
  }
  window.usergrid.console.pageSelectCollections = pageSelectCollections;

  function getCollections(search, searchType) {
    //clear out the table before we start
    var output = $('#collections-table');
    output.empty();

    var section =$('#application-collections');
    section.empty().html('<div class="alert alert-info">Loading...</div>');

    runAppQuery(new client.queryObj("GET",'', null, null, getCollectionsCallback,
      function() { alertModal("Error", "There was an error getting the collections"); }
    ));
    return false;
  }

  function getCollectionsCallback(response) {
    $('#collections-pagination').hide();
    $('#collections-next').hide();
    $('#collections-previous').hide();

    if (response.entities && response.entities[0] && response.entities[0].metadata && response.entities[0].metadata.collections) {
      applicationData.Collections = response.entities[0].metadata.collections;
      updateApplicationDashboard();
      updateQueryAutocompleteCollections();
    }

    var data = response.entities[0].metadata.collections;
    var output = $('#collections-table');

    if ($.isEmptyObject(data)) {
      output.replaceWith('<div id="collections-table" class="collection-panel-section-message">No collections found.</div>');
    } else {
      output.replaceWith('<table id="collections-table" class="table"><tbody></tbody></table>');
      for (var i in data) {
        var this_data = data[i];
        $.tmpl('usergrid.ui.collections.table_rows.html', this_data).appendTo('#collections-table');
      }
    }
  }

  /*******************************************************************
   *
   * Autocomplete
   *
   ******************************************************************/

  function updateUsersAutocomplete(){
    runAppQuery(new client.queryObj("GET", 'users/', null, null, updateUsersAutocompleteCallback,
      function() { alertModal("Error", "Unable to retrieve users."); }
    ));
    return false;
  }

  function updateUsersAutocompleteCallback(response) {
    users = {};
    if (response.entities) {
      users = response.entities;
    }
    var pathInput = $('#search-user-name-input');
    var list = [];
    for (var i in users) {
      list.push(users[i].username);
    }
    pathInput.typeahead({source:list});
    pathInput.data('typeahead').source = list;
  }
  window.usergrid.console.updateUsersAutocompleteCallback = updateUsersAutocompleteCallback;

  function updateUsersForRolesAutocomplete(){
    runAppQuery(new client.queryObj("GET",'users', null, null, updateUsersForRolesAutocompleteCallback,
      function() { alertModal("Error", "Unable to retrieve users."); }
    ));
    return false;
  }

  function updateUsersForRolesAutocompleteCallback(response) {
    users = {};
    if (response.entities) {
      users = response.entities;
    }
    var pathInput = $('#search-roles-user-name-input');
    var list = [];
    for (var i in users) {
      list.push(users[i].username);
    }
    pathInput.typeahead({source:list});
    pathInput.data('typeahead').source = list;
  }
  window.usergrid.console.updateUsersForRolesAutocompleteCallback = updateUsersForRolesAutocompleteCallback;

  function updateGroupsAutocomplete(){
    runAppQuery(new client.queryObj("GET",'groups', null, null, updateGroupsAutocompleteCallback,
      function() { alertModal("Error", "Unable to retrieve groups."); }
    ));
    return false;
  }

  function updateGroupsAutocompleteCallback(response) {
    groups = {};
    if (response.entities) {
      groups = response.entities;
    }
    var pathInput = $("#search-group-name-input");
    var list = [];
    for (var i in groups) {
      list.push(groups[i].path);
    }
    pathInput.typeahead({source:list});
    pathInput.data('typeahead').source = list;
  }
  window.usergrid.console.updateGroupsAutocompleteCallback = updateGroupsAutocompleteCallback;

  function updateGroupsForRolesAutocomplete(){
    runAppQuery(new client.queryObj("GET",'groups', null, null, updateGroupsForRolesAutocompleteCallback,
    function() { alertModal("Error", "Unable to retrieve groups."); }
    ));
    return false;
  }

  function updateGroupsForRolesAutocompleteCallback(response) {
    groups = {};
    if (response.entities) {
      groups = response.entities;
    }
    var pathInput = $('#search-roles-group-name-input');
    var list = [];
    for (var i in groups) {
      list.push(groups[i].path);
    }
    pathInput.typeahead({source:list});
    pathInput.data('typeahead').source = list;
  }
  window.usergrid.console.updateGroupsForRolesAutocompleteCallback = updateGroupsForRolesAutocompleteCallback;

  function updatePermissionAutocompleteCollections(){
    var pathInput = $("#role-permission-path-entry-input");
    var list = [];

    for (var i in applicationData.Collections) {
      list.push('/' + applicationData.Collections[i].name + '/*');
    }

    pathInput.typeahead({source:list});
   }

  function updateQueryAutocompleteCollectionsUsers(){
    var pathInput = $("#user-permission-path-entry-input");
    var list = [];

    for (var i in applicationData.Collections) {
      list.push('/' + applicationData.Collections[i].name + '/');
    }

    pathInput.typeahead({source:list});
    pathInput.data('typeahead').source = list;
  }

  function updateQueryAutocompleteCollections(){
    var pathInput = $('#query-path');
    var list = [];

    for (var i in applicationData.Collections) {
      list.push('/' + applicationData.Collections[i].name + '/');
    }

    pathInput.typeahead({source:list});
    pathInput.data('typeahead').source = list;
  }

  function updateRolesAutocomplete(){
    runAppQuery(new client.queryObj("GET", 'roles', null, null, updateRolesAutocompleteCallback,
      function() { alertModal("Error", "Unable to retrieve roles."); }
    ));
    return false;
  }

  function updateRolesAutocompleteCallback(response) {
    roles = {};
    if (response.entities) {
      roles = response.entities;
    }
    var pathInput = $('#search-role-name-input');
    var list = [];

    for (var i in roles) {
      list.push(roles[i].title);
    }

    pathInput.typeahead({source:list});
    pathInput.data('typeahead').source = list;
  }
  window.usergrid.console.updateRolesAutocompleteCallback = updateRolesAutocompleteCallback;

  /*******************************************************************
   *
   * Login
   *
   ******************************************************************/

  $('#login-organization').focus();

  function displayLoginError() {
    $('#login-area .box').effect('shake', {times: 2},100);
    $('#login-message').show();
  }

  function setupMenu() {
    var userNameBox = $('#userEmail');
    var userEmail = session.getLoggedInUserEmail();
    if (userEmail){
      userNameBox.html(userEmail);
      setupOrganizationsMenu();
    } else {
      userNameBox.html("No Logged In User");
    }
  }

  function setupOrganizationsMenu() {
    var organizations = session.getLoggedInUserOrgs();
    var orgName = session.getOrganizationName();
    if (!organizations) {
      return;
    }

    $('#organizations-menu > a span').text(orgName);
    $('#selectedOrg').text(orgName);

    var orgMenu = $('#organizations-menu ul');
    var orgTmpl = $('<li><a href="#">${name}</a></li>');
    var data = [];
    for (var name in organizations) {
      data.push({uuid:organizations[name].uuid, name:name});
    }
    orgMenu.empty();
    orgTmpl.tmpl(data).appendTo(orgMenu);
    orgMenu.find('a').click(selectOrganization);
    displayCurrentOrg();
  }

  function selectOrganization(e) {
    if (!client) {
      return;
    }

    var link = $(this);
    var orgName = link.text();
    var organizations = session.getLoggedInUserOrgs();
    for (var org in organizations) {
      if (organizations[org].name == orgName) {
         session.setOrganizationObj(organizations[org]);
      }
    }

    Pages.ShowPage('console');
  }

  function login() {
    var email = $('#login-email').val();
    var password = $('#login-password').val();
    client.loginAdmin(email, password, loginOk, displayLoginError);
  }

  function logout() {
    session.clearAll();
    if (client.useSSO()) {
      Pages.clearPage();
      client.sendToSSOLogoutPage();
    } else {
      Pages.ShowPage("login");
    }
    initOrganizationVars();
    return false;
  }
  usergrid.console.logout = logout;

  $("#login-form").submit(function () {
    login();
    return false;
  });

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
    if (!(organizatioNameRegex.test(organization_name))) {
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
    runManagementQuery(new client.queryObj("POST",'organizations', formdata, null,
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
          "<a onclick=\"usergrid.console.leaveOrganization('" + uuid + "')\"" + ' ' + "href=\"#" + uuid + "\" class=\"btn btn-danger\">Leave</a>" +
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
  usergrid.console.displayAccountSettings = displayAccountSettings;

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
        alertModal("Error - New passwords don't match");
        requestAccountSettings();
        return;
      }

      if (!(passwordRegex.test(new_pass))) {
        alertModal("Error " + passwordAllowedCharsMessage);
        requestAccountSettings();
        return;
      }
      userData.newpassword = new_pass;
      userData.oldpassword = old_pass;
    }
    runManagementQuery(new client.queryObj("PUT",'users/' + session.getLoggedInUserUUID(), userData, null,
      function(response) {
      $('#account-update-modal').modal('show');
        if ((old_pass && new_pass) && (old_pass != new_pass)) {
          logout();
          return;
        }
        requestAccountSettings();
      },
      function(response) {
        alertModal("Error - Unable to update account settings");
        requestAccountSettings();
      }
    ));
    return false;
  });

  function requestAccountSettings() {
    if (client.useSSO()) {
      client.sendToSSOProfilePage();
    } else {
      $('#update-account-id').text(session.getLoggedInUserUUID());
      $('#update-account-name').val("");
      $('#update-account-email').val("");
      $('#old-account-password').val("");
      $('#update-account-password').val("");
      $('#update-account-password-repeat').val("");
      runManagementQuery(new client.queryObj("GET",'users/' + session.getLoggedInUserUUID(), null, null, displayAccountSettings, null));
    }
  }
  usergrid.console.requestAccountSettings = requestAccountSettings;

  function displayOrganizations(response) {
    var t = "";
    var m = "";
    organizations = {};
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
          "<a onclick=\"usergrid.console.leaveOrganization('" + uuid + "')\" class=\"btn btn-danger\">Leave</a>" +
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

  function requestOrganizations() {
    $('#organizations').html('<div class="alert alert-info">Loading...</div>');
    runManagementQuery(new client.queryObj("GET","users/" + session.getLoggedInUserUUID() + "/organizations", null, null,
      displayOrganizations,
      function() {
        $('#organizations').html('<div class="alert">Unable to retrieve organizations list.</div>');
      }));
  }
  usergrid.console.requestOrganizations = requestOrganizations;

  function leaveOrganization(UUID) {
    confirmAction(
      "Are you sure you want to leave this Organization?",
      "You will lose all access to it.",
      function() {
        runManagementQuery(new client.queryObj("DELETE","users/" + session.getLoggedInUserUUID() + "/organizations/" + UUID, null, null,
          requestAccountSettings,
          function() { alertModal("Unable to leave organization"); }));
        }
    );

    return false;
  }
  usergrid.console.leaveOrganization = leaveOrganization;

  function displayCurrentOrg() {
    var orgname = session.getOrganizationName();
    var uuid = session.getOrganizationUUID();
    $('#organizations-table').html('<tr class="zebraRows"><td>' + orgname + '</td><td class="monospace">' + uuid + '</td></tr>');
  }
  usergrid.console.displayCurrentOrg = displayCurrentOrg;

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
    selectTabButton(this);
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
    selectTabButton(this);
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
      selectTabButton('#button-roles-search');
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
      selectTabButton('#button-role-settings');
      $('#roles-panel-list').hide();
      $('#role-panel-users').hide();
      $('#role-panel-groups').hide();
      $('#role-panel-settings').show();
    } else if ($(this).attr('id') == "button-role-users") {
      selectTabButton('#button-role-users');
      $('#roles-panel-list').hide();
      $('#role-panel-settings').hide();
      $('#role-panel-groups').hide();
      $('#role-panel-users').show();
    } else if ($(this).attr('id') == "button-role-groups") {
      selectTabButton('#button-role-groups');
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
    Pages.ShowPage(OFFLINE_PAGE)
    return;
  }

  function showLoginForNonSSO(){
    if (!session.loggedIn() && !client.useSSO()) {
      Pages.ShowPage('login');
    }
  }
  usergrid.console.showLoginForNonSSO = showLoginForNonSSO;

  function loginOk(){
    $('#login-message').hide();
    $('#login-email').val("");
    $('#login-password').val("");
    Pages.ShowPage('console');
  }
  usergrid.console.loginOk = loginOk;

  //load the templates only after the rest of the page is
  $(window).bind("load", function() {
    usergrid.console.ui.loadTemplate("usergrid.ui.users.table_rows.html");
    usergrid.console.ui.loadTemplate("usergrid.ui.groups.table_rows.html");
    usergrid.console.ui.loadTemplate("usergrid.ui.roles.table_rows.html");
    usergrid.console.ui.loadTemplate("usergrid.ui.role.groups.table_rows.html");
    usergrid.console.ui.loadTemplate("usergrid.ui.activities.table_rows.html");
    usergrid.console.ui.loadTemplate("usergrid.ui.collections.table_rows.html");
    usergrid.console.ui.loadTemplate("usergrid.ui.panels.role.users.html");
    usergrid.console.ui.loadTemplate("usergrid.ui.panels.role.permissions.html");
    usergrid.console.ui.loadTemplate("usergrid.ui.panels.user.profile.html");
    usergrid.console.ui.loadTemplate("usergrid.ui.panels.user.memberships.html");
    usergrid.console.ui.loadTemplate("usergrid.ui.panels.user.activities.html");
    usergrid.console.ui.loadTemplate("usergrid.ui.panels.user.graph.html");
    usergrid.console.ui.loadTemplate("usergrid.ui.panels.user.permissions.html");
    usergrid.console.ui.loadTemplate("usergrid.ui.collections.entity.header.html");
    usergrid.console.ui.loadTemplate("usergrid.ui.collections.entity.contents.html");
    usergrid.console.ui.loadTemplate("usergrid.ui.collections.entity.metadata.html");
    usergrid.console.ui.loadTemplate("usergrid.ui.collections.entity.collections.html");
    usergrid.console.ui.loadTemplate("usergrid.ui.collections.entity.json.html");
    usergrid.console.ui.loadTemplate("usergrid.ui.collections.entity.detail.html");
    usergrid.console.ui.loadTemplate("usergrid.ui.panels.group.details.html");
    usergrid.console.ui.loadTemplate("usergrid.ui.panels.group.memberships.html");
    usergrid.console.ui.loadTemplate("usergrid.ui.panels.group.activities.html");
    usergrid.console.ui.loadTemplate("usergrid.ui.panels.group.permissions.html");
  });

  //these templates are used on the front page and should be loaded up front
  usergrid.console.ui.loadTemplate("usergrid.ui.applications.table_rows.html");
  usergrid.console.ui.loadTemplate("usergrid.ui.admins.table_rows.html");
  usergrid.console.ui.loadTemplate("usergrid.ui.feed.table_rows.html");

}
