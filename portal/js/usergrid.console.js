function usergrid_console_app() {
    //This code block *WILL NOT* load before the document is complete
    window.usergrid = window.usergrid || {};
    usergrid.console = usergrid.console || {};

    var OFFLINE = false;
    var OFFLINE_PAGE = "#query-page";

    var self = this;

    var emailRegex = /^((([a-z]|\d|[!#\$%&'\*\+\-\/=\?\^_`{\|}~]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])+(\.([a-z]|\d|[!#\$%&'\*\+\-\/=\?\^_`{\|}~]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])+)*)|((\x22)((((\x20|\x09)*(\x0d\x0a))?(\x20|\x09)+)?(([\x01-\x08\x0b\x0c\x0e-\x1f\x7f]|\x21|[\x23-\x5b]|[\x5d-\x7e]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(\\([\x01-\x09\x0b\x0c\x0d-\x7f]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF]))))*(((\x20|\x09)*(\x0d\x0a))?(\x20|\x09)+)?(\x22)))@((([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.)+(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.?$/i;
    var passwordRegex = /^([0-9a-zA-Z@#$%^&])+$/;
    var nameRegex = /^([0-9a-zA-Z\.\-])+$/;
    var alphaNumRegex = /^([0-9a-zA-Z])+$/;

    var applications = {};
    var applications_by_id = {};

    var current_application_id = {};
    var current_application_name = {};
    var applicationData = {};

    var query_entities = null;
    var query_entities_by_id = null;

    var query_history = [];

    var indexes = [];
    var backgroundGraphColor = '#ffffff';
    var client = new usergrid.Client();
    client.onLogout = function() {
	    Pages.ShowPage("login");
    };

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

        current_application_id = {};
        current_application_name = {};

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

    client.onActiveRequest = function(activeRequests) {
        if (activeRequests <= 0) {
            $("#api-activity").delay(1000).hide(1);
        }
        else {
            $("#api-activity").show();
        }
    };

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
        //$('select#applicationSelect').selectmenu("value", current_application_id);
        if(!current_application_name)
            current_application_name = "Select an Application";
	    $('#selectedApp2').text(" - " + current_application_name);
	    $('#selectedApp').text(current_application_name);
    }

    function createAlphabetLinks(containerSelector, callback) {
        var li = $(containerSelector).html();
        var s = li.replace('{0}', '*');
        for (var i = 1; i <= 26; i++) {
            var char = String.fromCharCode(64 + i);
            s += li.replace('{0}', char);
        }
        $(containerSelector).html(s);
        $(containerSelector + " a").click(callback);
    }
    /*******************************************************************
     * 
     * Query Explorer
     * 
     ******************************************************************/

    $("#query-source").val("{ }");

    function pageSelectQueryExplorer() {
        showPanel("#query-panel");
        hideMoreQueryOptions();
        $("#query-path").val("");
        $("#query-source").val("{ }");
        $("#query-ql").val("");
        query_history = [];
        displayQueryResponse({});
    }
    window.usergrid.console.pageSelectQueryExplorer = pageSelectQueryExplorer;

    function pageOpenQueryExplorer(collection) {
        showPanel("#query-panel");
        hideMoreQueryOptions();
        $("#query-path").val(collection);
        $("#query-source").val("{ }");
        $("#query-ql").val("");
        //query_history = [];
        displayQueryResponse({});
        doQueryGet();
        var count = (collection.split('/').length - 1);
        if (count>1){
            $('#button-query-back').show();
        } else {
            $('#button-query-back').hide();
        }
    }
    window.usergrid.console.pageOpenQueryExplorer = pageOpenQueryExplorer;

    function pushQuery() {
        //don't let me get to deep...
        var newPath = $("#query-path").val();
        try {
            var oldPath = query_history[0].path;
            if (newPath == oldPath) {
                return;
            }
        } catch (e) {
            
        }
        query_history.unshift({
            path: $("#query-path").val(),
            query: $("#query-ql").val()
        });
        query_history.length = Math.min(5, query_history.length);
    }

    function popQuery() {
        if (query_history.length < 2) return;
        var item = null;
        query_history.shift();
        item = query_history[0];
        if (item) {
            $("#query-path").val(item.path);
            $("#query-ql").val(item.query);
            var count = (item.path.split('/').length - 1);
            if (count>1){
                $('#button-query-back').show();
            } else {
                $('#button-query-back').hide();
            }
        }
    }

    $.fn.loadEntityCollectionsListWidget = function() {
        this.each(function() {
            var entityType = $(this).dataset('entity-type');
            var entityUIPlugin = "usergrid_collections_" + entityType
            + "_list_item";
            if (!$(this)[entityUIPlugin]) {
                entityUIPlugin = "usergrid_collections_entity_list_item";
            }
            $(this)[entityUIPlugin]();
        });
    };

    $.fn.loadEntityCollectionsDetailWidget = function() {
        this.each(function() {
            var entityType = $(this).dataset('entity-type');
            var entityUIPlugin = "usergrid_collections_" + entityType
            + "_detail";
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

    var query = null;

    function displayQueryResponse(response) {
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
            }
            else {

                var entity = response.entities[0];
                query_entities_by_id[entity.uuid] = entity;

                var entity_path = (entity.metadata || {}).path;
                if ($.isEmptyObject(entity_path)) {
                    entity_path = path + "/" + entity.uuid;
                }

                t = "<div class=\"query-result-row entity_detail\" id=\"query-result-detail\" data-entity-type=\""
                + entity.type
                + "\" data-entity-id=\"" + entity.uuid + "\" data-collection-path=\""
                + entity_path
                + "\"></div>";

                $("#query-response-table").html(t);

                $("#query-result-detail").loadEntityCollectionsDetailWidget();
            }

            if (query.hasPrevious()) {
                $("#button-query-prev").show();
            }
            else {
                $("#button-query-prev").hide();
            }

            if (query.hasNext()) {
                $("#button-query-next").show();
            }
            else {
                $("#button-query-next").hide();
            }

            if (query.hasPrevious() || query.hasNext()) {
                $("#query-next-prev").show();
            }
            else {
                $("#query-next-prev").hide();
            }

        } else if (response.entities && (response.entities.length == 0) && !response.data) {
            $("#query-response-table").html(
            "<div id=\"query-response-message\">No entities in collection at path " + $("#query-path").val() + "</div>");
            $("#query-next-prev").hide();

            // This is a hack, will be fixed in API
        } else if (response.error && (
        (response.error.exception == "org.usergrid.services.exceptions.ServiceResourceNotFoundException: Service resource not found") ||
        (response.error.exception == "java.lang.IllegalArgumentException: Not a valid entity value type or null: null"))) {
            $("#query-response-table").html(
            "<div id=\"query-response-message\">No entities returned for query</div>");
            $("#query-next-prev").hide();

        } else {
            $("#query-response-table").html(
            "<pre class=\"query-response-json\">"
            + JSON.stringify(response, null, "  ") + "</pre>");
            $("#query-next-prev").hide();
        }
    }

    function doQueryPrevious() {
        query.getPrevious();
    }

    function doQueryNext() {
        query.getNext();
    }

    function showQueryStatus(s, _type) {
        //$("#statusbar-placeholder").statusbar("add", s, 7, _type);
        StatusBar.showAlert(s,_type);
    }

    function doQuerySend(method, data) {
        var qpath = $("#query-path").val();
        var ql = $("#query-ql").val();
        query = new client.Query(current_application_id, qpath, ql, {},
        function(data) {
            displayQueryResponse(data);
            var msg = "Web service completed successfully";
            if ((data || {}).duration) {
                msg += " in " + data.duration + "ms at " + new Date(data.timestamp);
            }
            showQueryStatus(msg);
        },
        function(data) {
            showQueryStatus("Web service failed: " + data.textStatus, "error");
        }
        );
        query.send(method, data);
    }

    function doQueryGet() {
        shrinkQueryInput();
        doQuerySend("GET", null);
        pushQuery();
        requestIndexes();
    }

    function doQueryBack() {
        popQuery();
        shrinkQueryInput();
        doQuerySend("GET", null);
    }

    function doQueryPost() {
        shrinkQueryInput();
        var obj = validateQuerySource();
        if (obj) {
            doQuerySend("POST", JSON.stringify(obj));
        }
        return false;
    }

    function doQueryPut() {
        shrinkQueryInput();
        var obj = validateQuerySource();
        if (obj) {
            doQuerySend("PUT", JSON.stringify(obj));
        }
        return false;
    }

    function doQueryDelete() {
        shrinkQueryInput();
        doQuerySend("DELETE", null);
        return false;
    }

    function expandQueryInput() {
        $("#query-source").height(150);
        $("#button-query-shrink").show();
        $("#button-query-expand").hide();
        return false;
    }

    function shrinkQueryInput() {
        $("#query-source").height(60);
        $("#button-query-shrink").hide();
        $("#button-query-expand").show();
        return false;
    }

    InitQueryPanel();
    function InitQueryPanel(){
        $("#query-source").focus(expandQueryInput);
        $("#button-query-shrink").click(shrinkQueryInput);
        $("#button-query-expand").click(expandQueryInput);

        $("#button-query-back").click(function() {doQueryBack();return false;});

        $("#button-query-get").click(function() {doQueryGet();return false;});
        $("#button-query-post").click(doQueryPost);
        $("#button-query-put").click(doQueryPut);
        $("#button-query-delete").click(doQueryDelete);
        $("#button-query-validate").click(function() {validateQuerySource();return false;});

        $("#button-query-prev").click(function() {doQueryPrevious();return false;});
        $("#button-query-next").click(function() {doQueryNext();return false;});
    }

    function showMoreQueryOptions() {
        $(".query-more-options").show();
        $(".query-less-options").hide();
        $("#query-ql").val("");
        $("#query-source").val("{ }");
    }

    function hideMoreQueryOptions() {
        $(".query-more-options").hide();
        $(".query-less-options").show();
        $("#query-ql").val("");
        $("#query-source").val("{ }");
    }

    function toggleMoreQueryOptions() {
        $(".query-more-options").toggle();
        $(".query-less-options").toggle();
        $("#query-ql").val("");
        $("#query-source").val("{ }");
    }

    $("#button-query-more-options").click(function() {
        toggleMoreQueryOptions();
        return false;
    });

    $("#button-query-less-options").click(function() {
        toggleMoreQueryOptions();
        return false;
    });

    $("#query-source").keypress(function(event) {
        if (event.keyCode == 13) {
            validateQuerySource();
        }
    });

    function validateQuerySource() {
        try {
            var result = jsonlint.parse($("#query-source").val());
            if (result) {
                showQueryStatus("JSON is valid!");
                $("#query-source").val(
                JSON.stringify(result, null, "  "));
                return result;
            }
        } catch(e) {
            showQueryStatus(e.toString(), "error");
        }
        return false;
    };

    $("#button-clear-query-source").click(function(event) {
        shrinkQueryInput();
        $("#query-source").val("{ }");
        return false;
    });

    $("#button-clear-path").click(function(event) {
        $("#query-path").val("");
        return false;
    });

    $("#button-clear-ql").click(function(event) {
        $("#query-ql").val("");
        return false;
    });

    window.usergrid.console.doChildClick = function(event) {
        var path = new String($("#query-path").val());
        if (!path.endsWith("/"))
        path += "/";
        path += event.target.innerText;
        $("#query-path").val(path);
    };

    var queryQl = $("#query-ql");
    queryQl.typeahead();
    function doBuildIndexMenu() {
        queryQl.data('typeahead').source = indexes;
    }

    function requestIndexes() {
        var qpath = $("#query-path").val();
        indexes = null;
        doBuildIndexMenu();
        client.requestCollectionIndexes(current_application_id, qpath,
        function(response) {
            if (response && response.data) {
                indexes = response.data;
            }
            doBuildIndexMenu();
        });
    }

    function deleteEntity() {
        var count = $("input[id^=queryResultItem]").length;
        $("input[id^=queryResultItem]").each( function() {
            if ($(this).prop("checked")) {  
                var entityId = $(this).attr("value");
                var path = $(this).attr("name");
                var refreshFunction = function() {pageOpenQueryExplorer(path);};
                if (count>1) {
                    refreshFunction = doQueryGet;
                }
                client.deleteEntity(current_application_id, entityId, path, doQueryGet,
                function() {
                    alert("Unable to delete entity: " + client.getLastErrorMessage(entityId));
                });
            }
        });
    }
    window.usergrid.console.deleteEntity = deleteEntity;
    
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

    function displayApplications(response) {
        var t = "";
        var m2 = "";
        applications = {};
        applications_by_id = {};
        var appMenu = $("#applications-menu ul");
        var appList = $("#organization-applications");
        appMenu.empty();
        appList.empty();

        if (response.data) {
            applications = response.data;
            var count = 0;
            var applicationNames = keys(applications).sort();
            var data = [];
            var appListTmpl = $('<div class="application-row"><a href="#"><span class="application-row-name">${name}</span> <span class="application-row-uuid">(${uuid})</span></a></br></div>');
            var appMenuTmpl = $('<li><a href="#">${name}</a></li>');

            for (var i in applicationNames) {
                var name = applicationNames[i];
                var uuid = applications[name];
                data.push({uuid:uuid, name:name});
                count++;
                applications_by_id[uuid] = name;
                if ($.isEmptyObject(current_application_id)) {
                    current_application_id = uuid;
                    current_application_name = name;
                }
            }
            if (count) {
                appListTmpl.tmpl(data).appendTo(appList);
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
            else {
                appList.html("<h2>No applications created.</h2>");
                appMenu.html('<li>--No Apps--</li>');
                disableApplicationPanelButtons();
            }
        } else {
            appList.html("<h2>No applications created.</h2>");
            appMenu.html('<li>--No Apps--</li>');
            disableApplicationPanelButtons();
        }
    }

    function requestApplications() {
        $("#organization-applications").html(
        "<h2>Loading...</h2>");
        client.requestApplications(displayApplications, function() {
            $("#organization-applications").html("<h2>Unable to retrieve application list.</h2>");
            disableApplicationPanelButtons();
        });
    }

    function selectFirstApp() {

        var firstApp = null;
        for (firstApp in client.currentOrganization.applications) break;
        if(firstApp)
            pageSelect(client.currentOrganization.applications[firstApp]);
    }

    function displayAdmins(response) {
        var t = "";
        admins = {};
        if (response.data) {
            admins = response.data;
            var i = 0;
            admins = admins.sort();
            for (i in admins) {
                var admin = admins[i];
                t += "<div class=\"admin-row\" id=\"admin-row-"
                + i
                + "\"><a href=\"#\" onclick=\"usergrid.console.pageSelectAdmin('"
                + admin.uuid
                + "'); return false;\"><span class=\"application-admin-name\">"
                + admin.name + " &lt;" + admin.email + "&gt;</span></a></div>";
            }
            if (i)
            $("#organization-admins").html(t);
            else
            $("#organization-admins").html(
            "<h2>No organization administrators.</h2>");

        } else {
            $("#organization-admin").html(
            "<h2>No organization administrators.</h2>");
        }
    }

    function requestAdmins() {
        $("#organization-admin").html(
        "<h2>Loading...</h2>");
        client.requestAdmins(displayAdmins,
        function() {
            $("#organization-admins").html("<h2>Unable to retrieve admin list.</h2>");
        });
    }

    function displayAdminFeed(response) {
        var t = "";
        var activities = {};
        if (response.entities && (response.entities.length > 0)) {
            activities = response.entities;
            var i = 0;
            for (i in activities) {
                var activity = activities[i];
                t += "<div class=\"organization-activity-row\">"
                + activity.title + "</div>";
            }
            if (i)
            $("#organization-activities").html(t);
            else
            $("#organization-activities").html(
            "<h2>No activities.</h2>");

        } else {
            $("#organization-activities").html(
            "<h2>No activities.</h2>");
        }
    }

    function requestAdminFeed() {
        $("#organization-activities").html(
        "<h2>Loading...</h2>");
        client.requestAdminFeed(displayAdminFeed,
        function() {
            $("#organization-activities").html("<h2>Unable to retrieve feed.</h2>");
        });
    }

    var organization_keys = { };
    
    function requestOrganizationCredentials() {
        $("#organization-panel-key").html("Loading...");
        $("#organization-panel-secret").html("Loading...");
        client.requestOrganizationCredentials(function(response) {
            $("#organization-panel-key").html(response.credentials.client_id);
            $("#organization-panel-secret").html(response.credentials.client_secret);
            organization_keys = {client_id : response.credentials.client_id, client_secret : response.credentials.client_secret};
        },
        function() {
            $("#organization-panel-key").html("Unable to load...");
            $("#organization-panel-secret").html("Unable to load...");
        });
    }

    function newOrganizationCredentials() {
        $("#organization-panel-key").html("Loading...");
        $("#organization-panel-secret").html("Loading...");
        client.regenerateOrganizationCredentials(function(response) {
            $("#organization-panel-key").html(response.credentials.client_id);
            $("#organization-panel-secret").html(response.credentials.client_secret);
            organization_keys = {client_id : response.credentials.client_id, client_secret : response.credentials.client_secret};
        },
        function() {
            $("#organization-panel-key").html("Unable to load...");
            $("#organization-panel-secret").html("Unable to load...");
        });
    }
    window.usergrid.console.newOrganizationCredentials = newOrganizationCredentials;

    function updateTips(t) {
        tips.text(t).addClass("ui-state-highlight");
        setTimeout(function() {
            tips.removeClass("ui-state-highlight", 1500);
        },
        500);
    }

    function checkLength(o, n, min, max) {
        if (o.val().length > max || o.val().length < min) {
            o.addClass("ui-state-error");
            updateTips("Length of " + n + " must be between " + min
            + " and " + max + ".");
            return false;
        } else {
            return true;
        }
    }

    function checkRegexp(o, regexp, n) {
        if (! (regexp.test(o.val()))) {
            o.addClass("ui-state-error");
            updateTips(n);
            return false;
        } else {
            return true;
        }
    }

    function checkTrue(o, t, n) {
        if (!t) {
            o.addClass("ui-state-error");
            updateTips(n);
        }
        return t;
    }

    var tips = $(".validateTips");

    /*******************************************************************
     *
     * Modals
     *
     ******************************************************************/

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
    $("#dialog-form-new-admin").submit(submitNewAdmin);
    $("#dialog-form-new-organization").submit(submitNewOrg);
    $("#dialog-form-new-user").submit(submitNewUser);
    $("#dialog-form-new-role").submit(submitNewRole);
    $("#dialog-form-new-collection").submit(submitNewCollection);
    $("#dialog-form-new-group").submit(submitNewGroup);
    $("#dialog-form-add-group-to-user").submit(submitAddGroupToUser);
    $("#dialog-form-add-user-to-group").submit(submitAddUserToGroup);

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
        form.find(".ui-state-error").removeClass("ui-state-error");
        form.find(".error").removeClass("error");
        form.find(".validateTips").remove();
        form.find(".help-block").empty().hide();
    }
    function submitApplication() {
        var form = $(this);
        formClearErrors(form);

        var new_application_name = $("#new-application-name");

        var bValid = checkLength2(new_application_name, 4, 80)
            && checkRegexp2(new_application_name, nameRegex, "only allows : a-z, 0-9, dot, and dash");

        if (bValid) {
            client.createApplication(form.serializeObject(), requestApplications, function() {
                alert("Unable to create application: " + client.getLastErrorMessage(name));
            });
            $(this).modal('hide');
        }
    }
    function submitNewAdmin() {
        var form = $(this);
        formClearErrors(form);

        var new_admin_email = $("#new-admin-email");
        var bValid = checkLength2(new_admin_email, 6, 80)
            && checkRegexp2(new_admin_email,emailRegex, "eg. example@apigee.com");
        if (bValid) {
            var data = form.serializeObject();
            client.createAdmin(data, requestAdmins, function () {
                alert("Unable to create admin: " + client.getLastErrorMessage(data.email));
            });
            $(this).modal('hide');
        }
    }
    function submitNewOrg() {
        var form = $(this);
        formClearErrors(form);

        var new_organization_name = $("#new-organization-name");

        var bValid = checkLength2(new_organization_name, 4, 80)
            && checkRegexp2(new_organization_name, nameRegex, "Organization name only allow : a-z, 0-9, dot, and dash.");

        if (bValid) {
            var data = form.serializeObject();
            client.createOrganization(data,requestOrganizations, function() {
                alert("Unable to create orgnization: " + client.getLastErrorMessage(data.name));
            });
            $(this).modal('hide');
        }
    }
    function submitNewUser() {
        var form = $(this);
        formClearErrors(form);

        var new_user_username = $("#new-user-username");
        var new_user_fullname = $("#new-user-fullname");
        var new_user_email = $("#new-user-email");
        var new_user_password = $("#new-user-password");

        var bValid = checkRegexp2(new_user_username,nameRegex,"Username only allows : a-z, 0-9, dot, and dash")
            && checkLength2(new_user_email, 6, 80)
            && checkRegexp2(new_user_email,emailRegex,"eg. ui@jquery.com")
            && checkLength2(new_user_password, 5, 16)
            && checkRegexp2(new_user_password,passwordRegex,"Password field only allows : a-z, 0-9, @, #, $, %, ^, &");

        if (bValid) {
            var data = form.serializeObject();
            client.createUser(current_application_id, data, requestUsers, function() {
                alert("Unable to create user: " + client.getLastErrorMessage('An internal error occured.'));
            });

            $(this).modal('hide');
        }
    }
    function submitNewRole() {
        var form = $(this);
        formClearErrors(form);

        var new_role_name = $("#new-role-name");
        var new_role_title = $("#new-role-title");

        var bValid = checkLength2(new_role_name, 1, 80)
            && checkRegexp2(new_role_name, nameRegex, "Name only allows : a-z, 0-9, dot, and dash")
            && checkLength2(new_role_title, 1, 80)
            && checkRegexp2(new_role_title, nameRegex, "Title only allows : a-z, 0-9, dot, and dash");

        if (bValid) {
            var data = form.serializeObject();
            client.createRole(current_application_id, data, requestRoles, function() {
                alert("Unable to create role: " + client.getLastErrorMessage(data.name));
            });
            $(this).modal('hide');
        }
    }
    function submitNewCollection() {
        var form = $(this);
        formClearErrors(form);

        var new_collection_name = $("#new-collection-name");

        var bValid = checkLength2(new_collection_name, 4, 80)
            && checkRegexp2(new_collection_name, alphaNumRegex, "Collection name only allow : a-z 0-9");

        if (bValid) {
            var data = form.serializeObject();
            client.createCollection(current_application_id, data, requestCollections, function() {
                alert("Unable to create collection: " + client.getLastErrorMessage(data.name));
            });
            $(this).modal('hide');
        }
    }
    function submitNewGroup() {
        var form = $(this);
        formClearErrors(form);

        var new_group_title = $("#new-group-title");
        var new_group_path = $("#new-group-path");

        var bValid = checkLength2(new_group_title, 1, 80)
            && checkRegexp2(new_group_title, nameRegex, "Title only allows : a-z, 0-9, dot, and dash")
            && checkLength2(new_group_path, 1, 80)
            && checkRegexp2(new_group_path, nameRegex, "Title only allows : a-z, 0-9, dot, and dash");

        if (bValid) {
            var data = form.serializeObject();
            client.createGroup(current_application_id, data, requestGroups, function() {
                alert("Unable to create group: " + client.getLastErrorMessage(data.path));
            });
            $(this).modal('hide');
        }
    }
    function submitAddGroupToUser() {
        var form = $(this);
        formClearErrors(form);

        var add_group_groupname = $("#lookup-group-groupname");

        var bValid = checkLength2(add_group_groupname, 1, 80)
            && checkRegexp2(add_group_groupname, nameRegex, "Group name name only allows : a-z, 0-9, dot, and dash.");

        if (bValid) {
            var data = form.serializeObject();
            client.addUserToGroup(current_application_id, data.group, data.userId,
                function() {requestUser(data.userId);},
                function() {
                    alert("Unable to add group to user: " + client.getLastErrorMessage('An internal error occured.'));
                }
            );

            $(this).modal('hide');
        }
    }
    function submitAddUserToGroup() {
        var form = $(this);
        formClearErrors(form);

        var add_user_username = $("#lookup-user-username");

        var bValid = checkLength2(add_user_username, 1, 80)
            && checkRegexp2(add_user_username, nameRegex, "Username name only allows : a-z, 0-9, dot, and dash.");

        if (bValid) {
            var data = form.serializeObject();
            client.addUserToGroup(current_application_id, data.groupId, data.username,
                function() {requestGroup(data.groupId);},
                function() {
                    alert("Unable to add user to group: " + client.getLastErrorMessage('An internal error occured.'));
                }
            );
            $(this).modal('hide');
        }
    }

/*-------------------- ------------------------ ---------------------------*/

    function pageSelect(uuid) {
        if (uuid) {
            current_application_id = uuid;
            current_application_name = applications_by_id[uuid];
        }
        setNavApplicationText();
        requestCollections();
        query_history = [];
    }
    window.usergrid.console.pageSelect = pageSelect;


    /*******************************************************************
     * 
     * Application
     * 
     ******************************************************************/

    function pageSelectApplication() {
        pageSelect(usergrid.console.currentApp);
        requestApplicationCredentials();
        requestApplicationUsage();
        //showPanel("#application-panel");
        //Pages.SelectPanel('application');
    }
    window.usergrid.console.pageSelectApplication = pageSelectApplication;

    function updateApplicationDashboard() {
        var data = new google.visualization.DataTable();
        data.addColumn('string', 'Entity');
        data.addColumn('number', 'Count');
        var rows = [];
        var t = '<table id="application-panel-entity-counts">';
        var collectionNames = keys(applicationData.Collections).sort();

        var entity_count = 0;
        for (var i in collectionNames) {
            var collectionName = collectionNames[i];
            var collection = applicationData.Collections[collectionName];
            var row = [collectionName, {v: collection.count}];
            rows.push(row);
            t += "<tr><td>" + collection.count + "</td><td>" + collectionName + "</td></tr>";
            entity_count += collection.count;
        }
        t += "<tr id=\"application-panel-entity-total\"><th>" + entity_count + "</th><th>entities total</th></tr>";
        t += "</table>";
        data.addRows(rows);

        new google.visualization.PieChart(
        document.getElementById('application-panel-entity-graph')).
        draw(data, {
            is3D: true,
            backgroundColor: backgroundGraphColor
        });

        $('#application-panel #application-panel-text').html(t);
    }

    function requestApplicationUsage() {
        $("#application-entities-timeline").html("");
        $("#application-cpu-time").html("");
        $("#application-data-uploaded").html("");
        $("#application-data-downloaded").html("");
        start_timestamp = Math.floor(new Date().getTime() / 1209600000) * 1209600000;
        end_timestamp = start_timestamp + 1209600000;
        resolution = "day";
        var counter_names = ["application.entities", "application.request.download", "application.request.time", "application.request.upload"];
        client.requestApplicationCounters(current_application_id, start_timestamp, end_timestamp, resolution, counter_names, function(response) {
            var usage_counters = response.counters;
            if (!usage_counters) {
                $("#application-entities-timeline").html("");
                $("#application-cpu-time").html("");
                $("#application-data-uploaded").html("");
                $("#application-data-downloaded").html("");
                return;
            }
            var graph_width = 400;
            var graph_height = 100;
            var data = new google.visualization.DataTable();
            data.addColumn('date', 'Time');
            data.addColumn('number', "Entities");
            data.addRows(15);
            for (var i in usage_counters[0].values) {
                data.setCell(parseInt(i), 0, new Date(usage_counters[0].values[i].timestamp));
                data.setCell(parseInt(i), 1, usage_counters[0].values[i].value);
            }
            new google.visualization.LineChart(
                document.getElementById('application-entities-timeline')).
                draw(data, {
                    title: "Entities",
                    titlePosition: "in",
                    titleTextStyle: {color: 'black', fontName: 'Arial', fontSize: 18},
                    width: graph_width,
                    height: graph_height,
                    backgroundColor: backgroundGraphColor,
                    legend: "none",
                    hAxis: {textStyle: {color:"transparent", fontSize: 1}},
                    vAxis: {textStyle: {color:"transparent", fontSize: 1}}});

            data = new google.visualization.DataTable();
            data.addColumn('date', 'Time');
            data.addColumn('number', "CPU");
            data.addRows(15);
            for (var i in usage_counters[2].values) {
                data.setCell(parseInt(i), 0, new Date(usage_counters[2].values[i].timestamp));
                data.setCell(parseInt(i), 1, usage_counters[2].values[i].value);
            }
            new google.visualization.LineChart(
                document.getElementById('application-cpu-time')).
                draw(data, {
                    title: "CPU Time Used",
                    titlePosition: "in",
                    titleTextStyle: {color: 'black', fontName: 'Arial', fontSize: 18},
                    width: graph_width,
                    height: graph_height,
                    backgroundColor: backgroundGraphColor,
                    legend: "none",
                    hAxis: {textStyle: {color:"transparent", fontSize: 1}},
                    vAxis: {textStyle: {color:"transparent", fontSize: 1}}});

            data = new google.visualization.DataTable();
            data.addColumn('date', 'Time');
            data.addColumn('number', "Uploaded");
            data.addRows(15);
            for (var i in usage_counters[3].values) {
                data.setCell(parseInt(i), 0, new Date(usage_counters[3].values[i].timestamp));
                data.setCell(parseInt(i), 1, usage_counters[3].values[i].value);
            }
            new google.visualization.LineChart(
                document.getElementById('application-data-uploaded')).
                draw(data, {
                    title: "Bytes Uploaded",
                    titlePosition: "in",
                    titleTextStyle: {color: 'black', fontName: 'Arial', fontSize: 18},
                    width: graph_width,
                    height: graph_height,
                    backgroundColor: backgroundGraphColor,
                    legend: "none",
                    hAxis: {textStyle: {color:"transparent", fontSize: 1}},
                    vAxis: {textStyle: {color:"transparent", fontSize: 1}}});
 
            data = new google.visualization.DataTable();
            data.addColumn('date', 'Time');
            data.addColumn('number', "Downloaded");
            data.addRows(15);
            for (var i in usage_counters[1].values) {
                data.setCell(parseInt(i), 0, new Date(usage_counters[1].values[i].timestamp));
                data.setCell(parseInt(i), 1, usage_counters[1].values[i].value);
            }
            new google.visualization.LineChart(
                document.getElementById('application-data-downloaded')).
                draw(data, {
                    title: "Bytes Downloaded",
                    titlePosition: "in",
                    titleTextStyle: {color: 'black', fontName: 'Arial', fontSize: 18},
                    width: graph_width,
                    height: graph_height,
                    backgroundColor: backgroundGraphColor,
                    legend: "none",
                    hAxis: {textStyle: {color:"transparent", fontSize: 1}},
                    vAxis: {textStyle: {color:"transparent", fontSize: 1}}});
        },
        function() {
            $("#application-entities-timeline").html("");
            $("#application-cpu-time").html("");
            $("#application-data-uploaded").html("");
            $("#application-data-downloaded").html("");
        });
    }
   
    /*******************************************************************
     * 
     * Users
     * 
     ******************************************************************/

    var userLetter = "*";
    var userSortBy = "username";
    function pageSelectUsers(uuid) {
        requestUsers();
        selectFirstTabButton('#users-panel-tab-bar');
        showPanelList('users');
        $('#search-user-username').val(''); //reset the search box
    }
    window.usergrid.console.pageSelectUsers = pageSelectUsers;

    usergrid.console.ui.loadTemplate("usergrid.ui.panels.user.list.html");

    var usersResults = null;
    function displayUsers(response) {
        console.log(response);
        usersResults = usergrid.console.ui.displayEntityListResponse({query: users_query}, {
            "listItemTemplate" : "usergrid.ui.panels.user.list.html",
            "getListItemTemplateOptions" : function(entity, path) {
                var name = entity.uuid + " : " + entity.type;
                var username = entity.username;
                if (entity.username) {
                    name = entity.username;
                }
                if (entity.name) {
                    name = name + " : " + entity.name;
                }           
                var collections = !$.isEmptyObject((entity.metadata || { }).collections || (entity.metadata || { }).connections);
                var uri = (entity.metadata || { }).uri;
                var id = 'userListItem';                          
                return {
                    entity : entity,
                    picture : entity.picture,
                    name : name,
                    id: id,
                    path : path,
                    username : username,
                    fblink : entity.fblink,
                    collections : collections,
                    uri : uri
                };
            },
            "onRender" : function() {
                //$("#users-by-alphabetical").show();
            },
            "onNoEntities" : function() {
                if (userLetter != "*") return "No users with usernames starting with " +  userLetter;
                return null;
            },
            "output" : "#users-response-table",
            "nextPrevDiv" : "#users-next-prev",
            "prevButton" : "#button-users-prev",
            "nextButton" : "#button-users-next",
            "noEntitiesMsg" : "No users found"
        }, response);
    }

    function showUsersForLetter(c) {
        userLetter = $(this).text();
        requestUsers();
    }
    usergrid.console.showUsersForLetter = showUsersForLetter;

    function showUsersForSearch(search){
        selectFirstTabButton('#users-panel-tab-bar');
        $('#users-panel-search').hide();
        selectTabButton("#button-users-list");
        $('#users-panel-list').show();
        userLetter = search;
        requestUsers();
    }    
    usergrid.console.showUsersForSearch = showUsersForSearch;

    function searchUsers(){
        var search = $('#search-user-username').val();
        var searchType = ($('#search-user-type').val())?$('#search-user-type').val():userSortBy;
        //make sure the input is valid:
        if (searchType == 'name') { searchType = 'name'; }
        else if (searchType == 'username') { searchType = 'username'; }
        requestUsers(search, searchType);
    }
    usergrid.console.searchUsers = searchUsers;

    function selectAllUsers(){
        $('[id=userListItem]').attr('checked', 'true');
    }
    usergrid.console.selectAllUsers = selectAllUsers;

    var users_query = null;
    function requestUsers(search, searchType) {
        var query = {"ql" : "order by " + userSortBy}; //default to built in search
        if (typeof search == 'string') {
            if (search.length > 0) {
                query = {"ql" : searchType + "='" + search + "*'"};
            }
        } else if (userLetter != "*") {
            query = {"ql" : searchType + "='" + userLetter + "*'"};
        }
        client.applicationId = current_application_id;
        users_query = client.queryUsers(displayUsers, query);
        return false;
    }
    usergrid.console.requestUsers = requestUsers;

    function deleteUsers() {        
        $("input[id^=userListItem]").each( function() {
            if ($(this).prop("checked")) {  
                var userId = $(this).attr("value");
                client.deleteUser(current_application_id, userId, requestUsers,
                function() {
                    alert("Unable to delete user: " + client.getLastErrorMessage(userId));
                });
            }
        });
    }
    window.usergrid.console.deleteUsers = deleteUsers;
    
    /*******************************************************************
     * 
     * User
     * 
     ******************************************************************/

    function pageOpenUserProfile(userId) {
        Pages.SelectPanel('user');
        requestUser(userId);
        selectTabButton("#button-user-profile");
        showPanelContent("#user-panel", "#user-panel-profile");
    }
    window.usergrid.console.pageOpenUserProfile = pageOpenUserProfile;

    usergrid.console.ui.loadTemplate("usergrid.ui.panels.user.profile.html");
    usergrid.console.ui.loadTemplate("usergrid.ui.panels.user.memberships.html");
    usergrid.console.ui.loadTemplate("usergrid.ui.panels.user.activities.html");
    usergrid.console.ui.loadTemplate("usergrid.ui.panels.user.graph.html");
    usergrid.console.ui.loadTemplate("usergrid.ui.panels.user.permissions.html");

    function saveUserProfile(uuid){
        var payload = usergrid.console.ui.jsonSchemaToPayload(usergrid.console.ui.collections.vcard_schema);
        client.saveUserProfile(current_application_id, uuid, payload, completeSaveProfile,
        function() {
            alert("Unable to update User: " + client.getLastErrorMessage('An internal error occured.'));
        });
    }
    window.usergrid.console.saveUserProfile = saveUserProfile;

    function saveGroupProfile(uuid){
        var payload = usergrid.console.ui.jsonSchemaToPayload(usergrid.console.ui.collections.group_schema);
        client.saveUserProfile(current_application_id, uuid, payload, completeSaveProfile,
        function() {
            alert("Unable to update Group: " + client.getLastErrorMessage('An internal error occured.'));
        });
    }
    window.usergrid.console.saveGroupProfile = saveGroupProfile;

    function completeSaveProfile(){
        alert('Information Saved.');
    }

    function redrawUserPanel() {
        $("#user-panel-profile").html("");
        $("#user-panel-memberships").html("");
        $("#user-panel-activities").html("");
        $("#user-panel-graph").html("");
        $("#user-panel-permissions").html("");
        if (user_data) {
            var options = {
                makeObjectTable : usergrid.console.ui.makeObjectTable,
                tableOpts : usergrid.console.ui.standardTableOpts,
                metadataTableOpts : usergrid.console.ui.metadataTableOpts
            };

            var details = $.tmpl("usergrid.ui.panels.user.profile.html", user_data, options);

            var formDiv = details.find(".query-result-form");
            $(formDiv).buildForm(usergrid.console.ui.jsonSchemaToDForm(usergrid.console.ui.collections.vcard_schema, user_data.entity));

            details.appendTo("#user-panel-profile");

            details.find(".button").button();

            $.tmpl("usergrid.ui.panels.user.memberships.html", user_data, options).appendTo("#user-panel-memberships");
            
            $.tmpl("usergrid.ui.panels.user.activities.html", user_data, options).appendTo("#user-panel-activities");
            
            $.tmpl("usergrid.ui.panels.user.graph.html", user_data, options).appendTo("#user-panel-graph");
            
            $.tmpl("usergrid.ui.panels.user.permissions.html", user_data, options).appendTo("#user-panel-permissions");
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
            var name = entity.uuid + " : "
                    + entity.type;
            if (entity.username) {
                name = entity.username;
            }
            if (entity.name) {
                name = name + " : " + entity.name;
            }
            
            var collections = $.extend({ }, (entity.metadata || { }).collections, (entity.metadata || { }).connections);
            if ($.isEmptyObject(collections)) collections = null;
            
            var entity_contents = $.extend( false, { }, entity);
            delete entity_contents['metadata'];
            
            var metadata = entity.metadata;
            if ($.isEmptyObject(metadata)) metadata = null;
            
            var entity_path = (entity.metadata || {}).path;
            if ($.isEmptyObject(entity_path)) {
                entity_path = path + "/" + entity.uuid;
            }

            user_data = {
                entity : entity_contents,
                picture : entity.picture,
                name : name,
                username : username,
                path : entity_path,
                collections : collections,
                metadata : metadata,
                uri : (entity.metadata || { }).uri
            };

            redrawUserPanel();
            
            client.queryUserMemberships(current_application_id, entity.uuid, function(response) {
                if (user_data && response.entities && (response.entities.length > 0)) {
                    user_data.memberships = response.entities;
                    redrawUserPanel();
                }
            })

            client.queryUserActivities(current_application_id, entity.uuid, function(response) {
                if (user_data && response.entities && (response.entities.length > 0)) {
                    user_data.activities = response.entities;
                    redrawUserPanel();
                }
            })

            client.queryUserRoles(current_application_id, entity.uuid, function(response) {
                if (user_data && response.entities && (response.entities.length > 0)) {
                    user_data.roles = response.entities;
                    redrawUserPanel();
                }
            })

            client.queryUserPermissions(current_application_id, entity.uuid, function(response) {
                if (user_data && response.entities && (response.entities.length > 0)) {
                    user_data.permissions = response.entities;
                    redrawUserPanel();
                }
            })

            client.queryUserFollowing(current_application_id, entity.uuid, function(response) {
                if (user_data && response.entities && (response.entities.length > 0)) {
                    user_data.following = response.entities;
                    redrawUserPanel();
                }
            })
        }
    }
    
    function requestUser(userId) {
        $("#user-profile-area").html(
        "<h2>Loading...</h2>");
        client.getUser(current_application_id, userId, handleUserResponse,
        function() {
            $("#user-profile-area").html("<h2>Unable to retrieve user profile.</h2>");
        });
    }

    /*******************************************************************
     * 
     * Groups
     * 
     ******************************************************************/
    var groupLetter = "*";
    var groupSortBy = "path";
    function pageSelectGroups(uuid) {
        requestGroups();
        selectFirstTabButton('#groups-panel-tab-bar');
        showPanelList('groups');
        $('#search-user-groupname').val(''); //reset the search box
    }
    window.usergrid.console.pageSelectGroups = pageSelectGroups;

    usergrid.console.ui.loadTemplate("usergrid.ui.panels.group.list.html");

    var groupsResults = null;
    function displayGroups(response) {
        groupsResults = usergrid.console.ui.displayEntityListResponse({query: groups_query}, {
            "listItemTemplate" : "usergrid.ui.panels.group.list.html",
            "getListItemTemplateOptions" : function(entity, path) {
                var name = entity.uuid + " : " + entity.type;
                if (entity.path) {
                    name = entity.path;
                }
                if (entity.name) {
                    name = name + " : " + entity.name;
                }
                var collections = !$.isEmptyObject((entity.metadata || { }).collections || (entity.metadata || { }).connections);
                var uri = (entity.metadata || { }).uri;
                var id = 'groupListItem'; 
                var title = entity.title;
                return {
                    entity : entity,
                    picture : entity.picture,
                    name : name,
                    id: id,
                    title: title,
                    path : path,
                    fblink : entity.fblink,
                    collections : collections,
                    uri : uri
                };
            },
            "onRender" : function() {
                //$("#groups-by-alphabetical").show();
            },
            "onNoEntities" : function() {
                if (groupLetter != "*") return "No groups with paths starting with " +  groupLetter;
                return null;
            },
            "output" : "#groups-response-table",
            "nextPrevDiv" : "#groups-next-prev",
            "prevButton" : "#button-groups-prev",
            "nextButton" : "#button-groups-next",
            "noEntitiesMsg" : "No groups found"
        }, response);
    }

    function showGroupsForLetter(c) {
        groupLetter = c;
        requestGroups();
    }
    usergrid.console.showGroupsForLetter = showGroupsForLetter;

    function showGroupsForSearch(search){
        selectFirstTabButton('#groups-panel-tab-bar');
        $('#groups-panel-search').hide();
        selectTabButton("#button-groups-list");
        $('#groups-panel-list').show();
        groupLetter = search;
        requestGroups();
    }    
    usergrid.console.showUsersForSearch = showUsersForSearch;

    function searchGroups(){
        var search = $('#search-user-groupname').val();
        var searchType = ($('#search-group-type').val())?$('#search-group-type').val():groupSortBy;
        //make sure the input is valid:
        if (searchType == 'name') { searchType = 'name'; }
        else if (searchType == 'title') { searchType = 'title'; }
        else if (searchType == 'path') { searchType = 'path'; }
        requestGroups(search, searchType);
    }
    usergrid.console.searchGroups = searchGroups;

    function selectAllGroups(){
        $('[id=groupListItem]').attr('checked', 'true');
    }
    usergrid.console.selectAllGroups = selectAllGroups;
    
    var groups_query = null;
    function requestGroups(search, searchType) {
        var query = {"ql" : "order by " + groupSortBy};
        if (typeof search == 'string') {
            if (search.length > 0) {
                query = {"ql" : searchType + "='" + search + "*'"};
            }
        } else if (groupLetter != "*") {
            query = {"ql" : searchType + "='" + groupLetter + "*'"};
        }
        client.applicationId = current_application_id;
        groups_query = client.queryGroups(displayGroups, query);
        return false;
    }
    usergrid.console.requestGroups = requestGroups;

    function deleteGroups() {
        $('#search-user-groupname').val('');
        $("input[id^=groupListItem]").each( function() {
            if ($(this).prop("checked")) {  
                var groupId = $(this).attr("value");
                client.deleteGroup(current_application_id, groupId, requestGroups,
                function() {
                    alert("Unable to delete group: " + client.getLastErrorMessage(groupId));
                });
            }
        });        
    }
    window.usergrid.console.deleteGroups = deleteGroups;
    
    //*****************************************************
    // add user to group - user autosearch
    //*****************************************************

    function findUserDialog(groupId) {
        $("#dialog-form-add-user-to-group").find('[name=groupId]').val(groupId);
        $("#dialog-form-add-user-to-group").modal("show");
    }
    window.usergrid.console.findUserDialog = findUserDialog;

    var targetDivUserList = '';
    var targetDivUserBox = '';
    var targetInputUserBox = '';
    function handleUserListResponse(response) {
        if (response.entities && (response.entities.length > 0)) {
            var response_html = '';
            var i=0;
            var e=response.entities.length;
            for (i=0;i<e;i++)
            {
                var entity = response.entities[i];
                username = entity.username;
                name = entity.name;
                uuid = entity.uuid;
                response_html = response_html +                
                '<a href="#" onclick="usergrid.console.setUsername(\''+username+'\');">'+username+' ('+name+')</a><br/>';
            }            
            $(targetDivUserList).html(response_html);
        } else {
             $(targetDivUserList).html('no entries found...');
        }
    }

    function setUsername(thisValue) {
        $(targetInputUserBox).val(thisValue);
        $(targetDivUserList).html("");
        setTimeout("$("+targetDivUserBox+").hide();", 200);
    }
    window.usergrid.console.setUsername = setUsername;

    function lookupUsername(searchString, targetBox, targetList, destination) {
        targetDivUserBox = $('#'+targetBox);
        targetDivUserList = $('#'+targetList);
        targetInputUserBox = destination;
        if(searchString.length == 0) {
            // no input, so hide the suggestion box.
            $(targetDivUserBox).hide();
        } else {
            $(targetDivUserBox).show();
            var data = 'searching...';
            $(targetDivUserList).html(data);

            var query = {"ql" : "order by " + userSortBy};
            if (searchString != "*") query = {"ql" : userSortBy + "='" + searchString + "*'"};
            client.applicationId = current_application_id;
            users_query = client.queryUsers(handleUserListResponse, query);
        }
    }
    window.usergrid.console.lookupUsername = lookupUsername;

    //*****************************************************
    // add user to group - group autosearch
    //*****************************************************


    function findGroupDialog(userId) {
        $("#dialog-form-add-group-to-user").find('[name=userId]').val(userId);
        $("#dialog-form-add-group-to-user").modal('show');
    }
    window.usergrid.console.findGroupDialog = findGroupDialog;

    var targetDivGroupList = '';
    var targetDivGroupBox = '';
    var targetInputGroupBox = '';
    function handleGroupListResponse(response) {
        if (response.entities && (response.entities.length > 0)) {
            var response_html = '';
            var i=0;
            var e=response.entities.length;
            for (i=0;i<e;i++)
            {
                var entity = response.entities[i];
                path = entity.path;
                title = entity.title;
                uuid = entity.uuid;
                response_html = response_html +
                '<a href="#" onclick="usergrid.console.setGroupPath(\''+path+'\');">'+path+'</a><br/>';
            }
            $(targetDivGroupList).html(response_html);
        } else {
            var data ='no entries found...';
            $(targetDivGroupList).html(data);
        }
    }

    function setGroupPath(thisValue) {
        $(targetInputGroupBox).val(thisValue);
        $(targetDivGroupList).html("");
        setTimeout("$('#suggestionsGroups').hide();", 200);
    }
    window.usergrid.console.setGroupPath = setGroupPath;

    function lookupGroupName(searchString, targetBox, targetList, destination) {
        targetDivGroupBox = $('#'+targetBox);
        targetDivGroupList = $('#'+targetList);
        targetInputGroupBox = destination;
        if(searchString.length == 0) {
            // no input, so hide the suggestion box.
            $(targetDivGroupBox).hide();
        } else {
            $(targetDivGroupBox).show();
            var data = 'searching...';
            $(targetDivGroupList).html(data);

            client.applicationId = current_application_id;
            var query = {"ql" : "order by " + groupSortBy};
            if (searchString != "*") query = {"ql" : groupSortBy + "='" + searchString + "*'"};
            users_query = client.queryGroups(handleGroupListResponse, query);
        }
    }
    window.usergrid.console.lookupGroupName = lookupGroupName;


    /*******************************************************************
     * 
     * Group
     * 
     ******************************************************************/

    function pageOpenGroupProfile(groupId) {
        Pages.SelectPanel('group');
        requestGroup(groupId);
        selectTabButton("#button-group-details");
        showPanelContent("#group-panel", "#group-panel-details");
    }
    window.usergrid.console.pageOpenGroupProfile = pageOpenGroupProfile;

    usergrid.console.ui.loadTemplate("usergrid.ui.panels.group.details.html");
    usergrid.console.ui.loadTemplate("usergrid.ui.panels.group.memberships.html");
    usergrid.console.ui.loadTemplate("usergrid.ui.panels.group.activities.html");
    usergrid.console.ui.loadTemplate("usergrid.ui.panels.group.permissions.html");

    function redrawGroupPanel() {
        $("#group-panel-details").html("");
        $("#group-panel-memberships").html("");
        $("#group-panel-activities").html("");
        $("#group-panel-permissions").html("");
        if (group_data) {
            var options = {
                makeObjectTable : usergrid.console.ui.makeObjectTable,
                tableOpts : usergrid.console.ui.standardTableOpts,
                metadataTableOpts : usergrid.console.ui.metadataTableOpts
            };

            var details = $.tmpl("usergrid.ui.panels.group.details.html", group_data, options);

            var formDiv = details.find(".query-result-form");
            $(formDiv).buildForm(usergrid.console.ui.jsonSchemaToDForm(usergrid.console.ui.collections.group_schema, group_data.entity));

            details.appendTo("#group-panel-details");

            details.find(".button").button();

            $.tmpl("usergrid.ui.panels.group.memberships.html", group_data, options).appendTo("#group-panel-memberships");

            $.tmpl("usergrid.ui.panels.group.activities.html", group_data, options).appendTo("#group-panel-activities");

            $.tmpl("usergrid.ui.panels.group.graph.html", group_data, options).appendTo("#group-panel-graph");

            $.tmpl("usergrid.ui.panels.group.permissions.html", group_data, options).appendTo("#group-panel-permissions");
        }
    }

    var group_data = null;

    function handleGroupResponse(response) {
        if (response.entities && (response.entities.length > 0)) {
            var entity = response.entities[0];
            var path = response.path || "";
            path = "" + path.match(/[^?]*/);
            var uuid = entity.uuid;
            var name = entity.uuid + " : "
                    + entity.type;
            if (entity.path) {
                name = entity.path;
            }
            if (entity.name) {
                name = name + " : " + entity.name;
            }

            var collections = $.extend({ }, (entity.metadata || { }).collections, (entity.metadata || { }).connections);
            if ($.isEmptyObject(collections)) collections = null;

            var entity_contents = $.extend( false, { }, entity);
            delete entity_contents['metadata'];

            var metadata = entity.metadata;
            if ($.isEmptyObject(metadata)) metadata = null;

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
                if (group_data && response.data) {
                    group_data.roles = response.data;
                    redrawGroupPanel();
                }
            })
        }
    }

    function requestGroup(groupId) {
        $("#group-details-area").html(
        "<h2>Loading...</h2>");
        client.getGroup(current_application_id, groupId, handleGroupResponse,
        function() {
            $("#group-details-area").html("<h2>Unable to retrieve group details.</h2>");
        });
    }

    /*******************************************************************
     * 
     * Roles
     * 
     ******************************************************************/

    usergrid.console.ui.loadTemplate("usergrid.ui.panels.roles.list.html");

    function pageSelectRoles(uuid) {
        requestRoles();
        selectFirstTabButton('#roles-panel-tab-bar');
        showPanelList('roles');
    }
    window.usergrid.console.pageSelectRoles = pageSelectRoles;

    var rolesResults = null;
    var roles_query = null;
    var roleLetter = '*';
    var roleSortBy = 'title';
    function requestRoles() {
        client.applicationId = current_application_id;
        var query = {"ql" : "order by " + roleSortBy};
        if (roleLetter != "*") query = {"ql" : roleSortBy + "='" + groupLetter + "*'"};
        roles_query = client.queryRoles(displayRoles, null);
        return false;
    }


    function displayRoles(response) {
        roles = {};
        if (response.data) {
            roles = response.data;
        }
        rolesResults = usergrid.console.ui.displayEntityListResponse({query: roles_query}, {
            "listItemTemplate" : "usergrid.ui.panels.roles.list.html",
            "getListItemTemplateOptions" : function(entity, path) {
                var name = entity.name;
                var title = entity.title;
                var collections = !$.isEmptyObject((entity.metadata || { }).collections || (entity.metadata || { }).connections);
                var uri = (entity.metadata || { }).uri;
                var id = 'roleListItem';
                return {
                    entity : entity,                    
                    name : name,
                    title: title,
                    id: id,
                    collections : collections,
                    uri : uri
                };
            },
            "onRender" : function() {
                //$("#users-by-alphabetical").show();
            },
            "onNoEntities" : function() {
                if (userLetter != "*") return "No roles found";
                return null;
            },
            "output" : "#roles-response-table",
            "nextPrevDiv" : "#roles-next-prev",
            "prevButton" : "#button-roles-prev",
            "nextButton" : "#button-roles-next",
            "noEntitiesMsg" : "No roles found"
        }, response);
    }

    function deleteRoles() {
        $("input[id^=roleListItem]").each( function() {
            if ($(this).prop("checked")) {
                var roleId = $(this).attr("value");
                client.deleteEntity(current_application_id, roleId, 'role', requestRoles,
                function() {
                    alert("Unable to delete role: " + client.getLastErrorMessage(roleId));
                });
            }
        });
    }
    window.usergrid.console.deleteRoles = deleteRoles;

    /*******************************************************************
     * 
     * Role
     * 
     ******************************************************************/

    var current_role_name = "";

    function pageOpenRole(roleName) {
        current_role_name = roleName;
        requestRole();
        showPanel("#role-panel");
        $('#role-panel-list').hide();
        //show the search tab
        selectTabButton('#button-role-search');
        //populate the panel content
        $('#role-panel-search').show();
    }
    window.usergrid.console.pageOpenRole = pageOpenRole;

    usergrid.console.ui.loadTemplate("usergrid.ui.panels.role.permissions.html");

    var permissions = {};
    function displayPermissions(response) {
        var section = $("#role-permissions");
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
            $.tmpl("usergrid.ui.panels.role.permissions.html", {"role" : current_role_name, "permissions" : permissions}, {}).appendTo("#role-permissions");
            updateAutocompleteCollections();
        } else {
            section.html("<h2>No permission information retrieved.</h2>");
        }
    }

    function requestRole() {
        $("#role-section-title").html("");
        $("#role-permissions").html("");
        client.requestApplicationRoles(current_application_id, function(response) {
            displayRoles(response);
            $("#role-section-title").html(roles[current_role_name] + " Role");
            $("#role-permissions").html(
            "<h2>Loading " + roles[current_role_name] + " permissions...</h2>");
            client.requestApplicationRolePermissions(current_application_id, current_role_name, function(response) {
                displayPermissions(response);
            },
            function() {
                $("#application-roles").html("<h2>Unable to retrieve " + roles[current_role_name] + " role permissions.</h2>");
            });
        },
        function() {
            $("#application-roles").html("<h2>Unable to retrieve roles list.</h2>");
        });
    }

    function deleteRolePermission(roleName, permission) {
        console.log("delete " + roleName + " - " + permission);
        client.deleteApplicationRolePermission(current_application_id, roleName, permission, requestRole, requestRole);
    }
    window.usergrid.console.deleteRolePermission = deleteRolePermission;

    function addRolePermission(roleName) {
        var path = $('#role-permission-path-entry-input').val();
        var ops = "";
        var s = "";
        if ($('#role-permission-op-get-checkbox').prop("checked")) {
            ops = "get";
            s = ",";
        }
        if ($('#role-permission-op-post-checkbox').prop("checked")) {
            ops = ops + s + "post";
            s = ",";
        }
        if ($('#role-permission-op-put-checkbox').prop("checked")) {
            ops =  ops + s + "put";
            s = ",";
        }
        if ($('#role-permission-op-delete-checkbox').prop("checked")) {
            ops =  ops + s + "delete";
            s = ",";
        }
        var permission = ops + ":" + path;
        console.log("add " + roleName + " - " + permission);
        if (ops) {
            client.addApplicationRolePermission(current_application_id, roleName, permission, requestRole, requestRole);
        }
    }
    window.usergrid.console.addRolePermission = addRolePermission;

    /*******************************************************************
     * 
     * Activities
     * 
     ******************************************************************/

    function pageSelectActivities(uuid) {
        requestActivities();
    }
    window.usergrid.console.pageSelectActivities = pageSelectActivities;

    var activitiesQuery = null;
    var activitiesResults = null;
    function displayActivities(response) {
        activitiesResults = usergrid.console.ui.displayEntityListResponse(activitiesQuery, {
            "getListItem" : function(entity, entity_path) {
                var t = "<div class=\"query-result-row entity_list_item\" data-entity-type=\""
                 + entity.type
                 + "\" data-entity-id=\"" + entity.uuid + "\" data-collection-path=\""
                 + entity_path
                 + "\">"
                 + entity.actorName + " " + entity.verb + " " + entity.content
                 + "</div>";
                return t;
            },
            "onRender" : function() {
            },
            "output" : "#activities-response-table",
            "nextPrevDiv" : "#activities-next-prev",
            "prevButton" : "#button-activities-prev",
            "nextButton" : "#button-activities-next",
            "noEntitiesMsg" : "No activities in application"
        }, response);
    }

    function requestActivities() {
        client.applicationId = current_application_id;
        activitiesQuery = { };
        activitiesQuery.query = client.queryActivities(displayActivities);
        return false;
    }


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
        $("#analytics-counter-names").html("Loading...");
        client.requestApplicationCounterNames(current_application_id, function(response) {
            application_counters_names = response.data;
            var html = usergrid.console.ui.makeTableFromList(application_counters_names, 1, {
                tableId : "analytics-counter-names-table",
                getListItem : function(i, col, row) {
                    var counter_name = application_counters_names[i];
                    var checked = !counter_name.startsWith("application.");
                    return "<div class='analytics-counter'><input class='analytics-counter-checkbox' type='checkbox' name='counter' " +
                        "value='" + counter_name + "'" + (checked ? " checked='true'" : "") + " />" + counter_name + "</div>";
                }
            });
            $("#analytics-counter-names").html(html);
            requestApplicationCounters();
        },
        function() {
            $("#analytics-counter-names").html("Unable to load...");
        });
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
        $("#analytics-graph").html("Loading...");
        start_timestamp = $("#start-date").datepicker("getDate").at($('#start-time').timepicker("getTime")).getTime();
        end_timestamp = $("#end-date").datepicker("getDate").at($('#end-time').timepicker("getTime")).getTime();
        resolution = $('select#resolutionSelect').val();
        client.requestApplicationCounters(current_application_id, start_timestamp, end_timestamp, resolution, counter_names, function(response) {
            application_counters = response.counters;
            if (!application_counters) {
                $("#analytics-graph").html("No counter data");
                return;
            }
            var data = new google.visualization.DataTable();
            if (resolution == "all") {
                data.addColumn('string', 'Time');
            }
            else if ((resolution == "day") || (resolution == "week") || (resolution == "month")) {
                data.addColumn('date', 'Time');
            }
            else {
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
                $("#analytics-graph").html("No counter data");
                return;
            }
            var html = "<table id=\"analytics-graph-table\"><thead><tr><td></td>";
            if (application_counters.length > 0) {
                data.addRows(application_counters[0].values.length);
                for (var j in application_counters[0].values) {
                    var timestamp = application_counters[0].values[j].timestamp;
                    if ((timestamp <= 1) || (resolution == "all")) {
                        timestamp = "All";
                    }
                    else if ((resolution == "day") || (resolution == "week") || (resolution == "month")) {
                        timestamp = (new Date(timestamp)).toString("M/d");
                    }
                    else {
                        timestamp = (new Date(timestamp)).toString("M/d h:mmtt");
                    }
                    html += "<th>" + timestamp + "</th>";
                    if (resolution == "all") {
                        data.setValue(parseInt(j), 0, "All");
                    }
                    else {
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
            $("#analytics-graph").html(html);
            
            if (resolution == "all") {
                new google.visualization.ColumnChart(
                    document.getElementById('analytics-graph-area')).
                    draw(data, {width: 950, height: 500, backgroundColor: backgroundGraphColor});
            }
            else {
                new google.visualization.LineChart(
                    document.getElementById('analytics-graph-area')).
                    draw(data, {width: 950, height: 500, backgroundColor: backgroundGraphColor});
            }
            //$("#analytics-graph-table").visualize({type: 'line', width: '950px', height: '500px'});
        },
        function() {
            $("#analytics-graph").html("Unable to load...");
        });
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
        $("#application-panel-key").html("Loading...");
        $("#application-panel-secret").html("Loading...");
        client.requestApplicationCredentials(current_application_id, function(response) {
            $("#application-panel-key").html(response.credentials.client_id);
            $("#application-panel-secret").html(response.credentials.client_secret);
            application_keys[current_application_id] = {client_id : response.credentials.client_id, client_secret : response.credentials.client_secret};
        },
        function() {
            $("#application-panel-key").html("Unable to load...");
            $("#application-panel-secret").html("Unable to load...");
        });
    }

    function newApplicationCredentials() {
        $("#application-panel-key").html("Loading...");
        $("#application-panel-secret").html("Loading...");
        client.regenerateApplicationCredentials(current_application_id, function(response) {
            $("#application-panel-key").html(response.credentials.client_id);
            $("#application-panel-secret").html(response.credentials.client_secret);
            application_keys[current_application_id] = {client_id : response.credentials.client_id, client_secret : response.credentials.client_secret};
        },
        function() {
            $("#application-panel-key").html("Unable to load...");
            $("#application-panel-secret").html("Unable to load...");
        });
    }
    window.usergrid.console.newApplicationCredentials = newApplicationCredentials;

    /*******************************************************************
     * 
     * Shell
     * 
     ******************************************************************/

    function pageSelectShell(uuid) {
        requestApplicationCredentials();
        $("#shell-input").focus();
    }
    window.usergrid.console.pageSelectShell = pageSelectShell;

    var history_i = 0;
    var history = new Array();

    function scrollToInput() {
        // Do it manually because JQuery seems to not get it right
        var console_panels = document.getElementById('console-panels');
        var shell_content = document.getElementById('shell-content');
        var scrollOffset = Math.max(shell_content.clientHeight - console_panels.clientHeight, 0);
        console_panels.scrollTop = scrollOffset;
    }
    
    function echoInputToShell(s) {
        if (!s) s = "&nbsp;";
        var html = "<div class=\"shell-output-line\"><span class=\"shell-prompt\">&gt; </span><span class=\"shell-output-line-content\">" + s + "</span></div>";
        $('#shell-output').append(html);
        scrollToInput();
    }
    
    function printLnToShell(s) {
        if (!s) s = "&nbsp;";
        var html = "<div class=\"shell-output-line\"><div class=\"shell-output-line-content\">" + s + "</div></div>";
        $('#shell-output').append(html);
        scrollToInput();
    }

    function displayShellResponse(response) {
        printLnToShell(JSON.stringify(response, null, "  "));
    }

    function handleShellCommand(s) {
        if (s) {
            history.push(s);
            history_i - history.length - 1;
        }
        if (s.startsWith("/")) {
            var path = client.encodePathString(s);
            printLnToShell(path);
            client.apiGetRequest("/" + current_application_id + path, null, displayShellResponse, null);
        }
        else if (s.startsWith("get /")) {
            var path = client.encodePathString(s.substring(4));
            printLnToShell(path);
            client.apiGetRequest("/" + current_application_id + path, null, displayShellResponse, null);
        }
        else if (s.startsWith("put /")) {
            var params = client.encodePathString(s.substring(4), true);
            printLnToShell(params.path);
            client.apiRequest("PUT", "/" + current_application_id + params.path, null, JSON.stringify(params.payload), displayShellResponse, null);
        }
        else if (s.startsWith("post /")) {
            var params = client.encodePathString(s.substring(5), true);
            printLnToShell(params.path);
            client.apiRequest("POST", "/" + current_application_id + params.path, null, JSON.stringify(params.payload), displayShellResponse, null);
        }
        else if (s.startsWith("delete /")) {
            var path = client.encodePathString(s.substring(7));
            printLnToShell(path);
            client.apiRequest("DELETE", "/" + current_application_id + path, null, null, displayShellResponse, null);
        }
        else if ((s == "clear") || (s == "cls"))  {
            $('#shell-output').html("<div class=\"shell-output-line\">Usergrid Interactive Shell</div>");
            echoInputToShell(s);
        }
        else if (s == "help") {
            printLnToShell("/&lt;path&gt; - API get request");
            printLnToShell("get /&lt;path&gt; - API get request");
            printLnToShell("put /&lt;path&gt; {&lt;json&gt;} - API put request");
            printLnToShell("post /&lt;path&gt; {&lt;json&gt;} - API post request");
            printLnToShell("delete /&lt;path&gt; - API delete request");
            printLnToShell("cls - clear screen");
            printLnToShell("clear - clear screen");
            printLnToShell("help - show this help");
        }
        else if (s == "") {
            printLnToShell("ok");
        }
        else {
            printLnToShell("syntax error");
        }
    }
    
    $('#shell-input').keydown(function(event) {
        var shell_input = $("#shell-input");
        if (!event.shiftKey && (event.keyCode == '13')) {
            event.preventDefault();
            var s = $('#shell-input').val().trim();
            echoInputToShell(s);
            shell_input.val("");
            handleShellCommand(s);
        }
        else if (event.keyCode == '38') {
            event.preventDefault();
            history_i--;
            if (history_i < 0) {
                history_i = Math.max(history.length - 1, 0);
            }
            if (history.length > 0) {
                shell_input.val(history[history_i]);
            }
            else {
                shell_input.val("");
            }
        }
        else if (event.keyCode == '40') {
            event.preventDefault();
            history_i++;
            if (history_i >= history.length) {
                history_i = 0;
            }
            if (history.length > 0) {
                shell_input.val(history[history_i]);
            }
            else {
                shell_input.val("");
            }
        }
    });

    $('#shell-input').keyup(function() {
        var shell_input = $("#shell-input");
        shell_input.css("height", shell_input.attr("scrollHeight"));
    });

    /*******************************************************************
     * 
     * Collections
     * 
     ******************************************************************/

    usergrid.console.ui.loadTemplate("usergrid.ui.panels.collections.list.html");

    function pageSelectCollections(uuid) {
        requestCollections();
    }
    window.usergrid.console.pageSelectCollections = pageSelectCollections;

    var collectionsResults = null;
    var collections_query = null;
    var collectionsLetter = '*';
    var collectionsSortBy = 'title';

    function requestCollections() {
        var section =$("#application-collections");
        section.empty().html("<h2>Loading...</h2>");
        client.requestCollections(current_application_id, displayCollections, function() {
            section.html("<h2>Unable to retrieve collections list.</h2>");
        });
    }

    function updateAutocompleteCollections(){
        var pathInput = $("#role-permission-path-entry-input");
        pathInput.typeahead(applicationData.Collections);
        //pathInput.data('typeahead').source = applicationData.Collections;
    }
    function displayCollections(response) {
        roles = {};

        if (response.data) {
            roles = response.data;
        }

        if (response.entities && response.entities[0] && response.entities[0].metadata && response.entities[0].metadata.collections) {
            applicationData.Collections = response.entities[0].metadata.collections;
            updateApplicationDashboard();
        }

        collectionsResults = usergrid.console.ui.displayEntityListResponse({query: roles_query}, {
            "listItemTemplate" : "usergrid.ui.panels.collections.list.html",
            "getListItemTemplateOptions" : function(entity, path) {
                var name = entity.name;
                var title = entity.title;
                var collections = !$.isEmptyObject((entity.metadata || { }).collections || (entity.metadata || { }).connections);
                var uri = (entity.metadata || { }).uri;
                var id = 'collectionListItem';
                var type = entity.type;
                var count = entity.count;
                return {
                    entity : entity,
                    name : name,
                    title: title,
                    id: id,
                    type: type,
                    count: count,
                    collections : collections,
                    uri : uri
                };
            },
            "onRender" : function() {},
            "onNoEntities" : function() {
                if (userLetter != "*") return "No collections found";
                return null;
            },
            "output" : "#collections-response-table",
            "nextPrevDiv" : "#collections-next-prev",
            "prevButton" : "#button-collections-prev",
            "nextButton" : "#button-collections-next",
            "noEntitiesMsg" : "No collections found"
        }, response);
    }    

    /*******************************************************************
     * 
     * Login
     * 
     ******************************************************************/

    $("#login-organization").focus();

    function clearLoginForm() {
        $("#login-email").val("");
        $("#login-password").val("");
    }

    function displayLoginError() {
	      $("#login-area .box").effect("shake", {times: 2},100);
        $("#login-message").show();
    }

    function clearLoginError() {
        $("#login-message").hide();
    }

   function setupMenu() {
      var userNameBox = $("#userEmail");
      if (client && client.loggedInUser)
          userNameBox.html(client.loggedInUser.email);
      else
          userNameBox.html("No Logged In User");
      setupOrganizationsMenu();
   }

    function setupOrganizationsMenu() {
        if (!client || !client.loggedInUser || !client.loggedInUser.organizations) {
            return;
        }
        var orgName = client.currentOrganization.name;
        $("#organizations-menu > a span").text(orgName);
        $("#selectedOrg").text(orgName);

        var organizations = client.loggedInUser.organizations;
        var orgMenu = $('#organizations-menu ul');
        var orgTmpl = $('<li><a href="#">${name}</a></li>');
        var data = [];
        for (var name in organizations) {
            data.push({uuid:organizations[name].uuid, name:name});
        }
        orgMenu.empty();
        orgTmpl.tmpl(data).appendTo(orgMenu);
        orgMenu.find("a").click(selectOrganization);
    }

        function selectOrganization(e){
            if (client) {
                    var link = $(this);
                    var orgName = link.text();
                    client.currentOrganization = client.loggedInUser.organizations[orgName];
                    Pages.ShowPage('console');
            disableApplicationPanelButtons();
        }
    }

    function login() {
        var email = $("#login-email").val();
        var password = $("#login-password").val();
        client.loginAdmin(email, password,loginOk,
            function() {
                displayLoginError();
            }
        );
    }

    function logout() {
        client.logout();
        initOrganizationVars();
        Pages.ShowPage("login");
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

    $("#signup-cancel").click(function() {
        Pages.ShowPage("login");
        clearSignupError();
        clearSignupForm();
        return false;
    });

    function displaySignupError(msg) {
        $("#signup-area .box").effect("shake", {times: 2},100);
        $("#signup-message").html('<strong>ERROR</strong>: ' + msg + '<br />').show();
    }

    function clearSignupForm() {
        $("#signup-organization-name").val("");
        $("#signup-username").val("");
        $("#signup-name").val("");
        $("#signup-email").val("");
        $("#signup-password").val("");
        $("#signup-password-confirm").val("");
    }

    function clearSignupError() {
        $("#signup-message").hide();
    }

    function signup() {
        var organization_name = $("#signup-organization-name").val();
        if (!(nameRegex.test(organization_name))) {
            displaySignupError("Invalid organization name, only a-z, 0-9, dot, and dash.");
            return;
        }
        var username = $("#signup-username").val();
        if (!(nameRegex.test(username))) {
            displaySignupError("Invalid username, only a-z, 0-9, dot, and dash.");
            return;
        }
        var name = $("#signup-name").val();
        if (!$.trim(name).length) {
            displaySignupError("Name cannot be blank.");
            return;
        }
        var email = $("#signup-email").val();
        if (!(emailRegex.test(email))) {
            displaySignupError("Invalid email.");
            return;
        }
        var password = $("#signup-password").val();
        if (!(passwordRegex.test(password))) {
            displaySignupError("Invalid password, only a-z, 0-9, @, #, $, %, ^, &.");
            return;
        }
        if (password != $("#signup-password-confirm").val()) {
            displaySignupError("Passwords must match.");
            return;
        }
        client.signup(organization_name, username, name, email, password,
        function(response) {
            clearSignupError();
            clearSignupForm();
            Pages.ShowPage("login");
        },
        function(response) {
            displaySignupError(client.getLastErrorMessage("Unable to create new organization at this time"));
        }
        );
    }

    $("#button-signup").click(function() {
        signup();
        return false;
    });

    /*******************************************************************
     * 
     * Account Settings
     * 
     ******************************************************************/

    function showAccountSettings() {
        Pages.ShowPage("account");
        requestAccountSettings();
    }

    function displayAccountSettings(response) {
        if (response.data) {
            $("#update-account-username").val(response.data.username);
            $("#update-account-name").val(response.data.name);
            $("#update-account-email").val(response.data.email);

            var t = "";
            var organizations = response.data.organizations;
            var organizationNames = keys(organizations).sort();
            for (var i in organizationNames) {
                var organizationName = organizationNames[i];
                var organization = organizations[organizationName];
                var uuid = organization.uuid;
                t +=
                "<div class='row' id='organization-row-" + uuid + "'>" +
                    "<div class='organization-row-link span'>" +
                        "<a href='#" + uuid + "' >" +
                            "<span>" + organizationName + "</span>" +
                            "<span> (" + uuid + ")</span>" +
                        "</a>" +
                    "</div>" +
                    "<div class='organization-row-buttons span pull-right'>" +
                        "<a onclick=\"usergrid.console.leaveOrganization('" + organizationName + "')\" href='#" + uuid + "' class='btn btn-danger' >Leave</a>" +
                    "</div>" +
                "</div>";
            }
            $("#organizations").html(t);
            $("#organizations a").click( function(e){
               e.preventDefault();
               var uuid = $(this).attr("href").substring(1);
               //usergrid.console.pageSelectOrganization(uuid); //TODO:david this function dont exist
            });
        } else {
        }
    }

    $("#button-update-account").click(function() {
        var userData = {
            username : $("#update-account-username").val(),
            name : $("#update-account-name").val(),
            email : $("#update-account-email").val()
        };
        var old_pass = $("#old-account-password").val();
        var new_pass = $("#update-account-password").val();
        var new_pass2 = $("#update-account-password-repeat").val();
        if (old_pass && new_pass) {
            if (new_pass != new_pass2) {
                $.jAlert(client.getLastErrorMessage("New passwords don't match"), 'error', function(result) {
                     requestAccountSettings();
                 });
                return;
            }
            if (!(passwordRegex.test(new_pass))) {
                $.jAlert(client.getLastErrorMessage("Password field only allows : a-z, 0-9, @, #, $, %, ^, &"), 'error', function(result) {
                     requestAccountSettings();
                 });
                return;
            }
            userData.newpassword = new_pass;
            userData.oldpassword = old_pass;
        }
        client.updateAdminUser(userData,
            function(response) {
                $.jAlert("Account settings update", '');
                if ((old_pass && new_pass) && (old_pass != new_pass)) {
                	logout();
                	return;
                }
                requestAccountSettings();
            },
            function(response) {
                $.jAlert(client.getLastErrorMessage("Unable to update account settings"), 'error', function(result) {
                    requestAccountSettings();
                });
            }
        );
        return false;
    });

    function requestAccountSettings() {
        $("#update-account-id").val(client.loggedInUser.uuid);
        $("#update-account-name").val("");
        $("#update-account-email").val("");
        $("#old-account-password").val("");
        $("#update-account-password").val("");
        $("#update-account-password-repeat").val("");
        client.requestAdminUser(displayAccountSettings,
        function() {
        });
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
                t += "<div class=\"organization-row\" id=\"organization-row-"
                + uuid
                + "\"><a href=\"#\" onclick=\"usergrid.console.pageSelectOrganization('"
                + uuid
                + "'); return false;\"><span class=\"organization-row-name\">"
                + organization
                + "</span> <span class=\"organization-row-uuid\">("
                + uuid + ")</span>" + "</a></div>";
                count++;
                orgainzations_by_id[uuid] = organization;
                /*
                if ($.isEmptyObject(current_organization_id)) {
                    current_organization_id = uuid;
                    current_organization_name = organization;
                }
                m += "<option value=\"" + uuid + "\"" + ((uuid == current_organization_id) ? " selected=\"selected\"": "") + ">" + organization + "</option>";
                */
            }
            if (count) {
                $("#organizations").html(t);                
            }
            else {
                $("#organizations").html(
                "<h2>No organizations created.</h2>"); 
            }
        } else {
            $("#organizations").html(
            "<h2>No organizations created.</h2>");
            
        }
       
    }
       
    function requestOrganizations() {
        $("#organizations").html("<h2>Loading...</h2>");
        client.requestOrganizations(displayOrganizations, function() {
            $("#organizations").html("<h2>Unable to retrieve organizations list.</h2>");
        });
    }


   function leaveOrganization(name) {
        if (confirm('Are you sure you want to leave this Organization?')) {
            client.leaveOrganization(name,requestOrganizations,
            function() {
                alert("Unable to leave orgnization: " + client.getLastErrorMessage('There was an error processing your request'));
            });
        }
    }
    window.usergrid.console.leaveOrganization = leaveOrganization;
    
    /*******************************************************************
     * 
     * Startup
     * 
     ******************************************************************/

    $("#logged-in-user-name").click(function() {
        showAccountSettings();
        return false;
    });
    
    function showPanelList(type){
        //hide the other panels
        $('#' + type + '-panel-search').hide();
        //show the list tab
        selectTabButton('#button-'+type+'-list');
        //populate the panel content
        $('#' + type + '-panel-list').show();
    }

    $("#button-users-list").click(function() {
        showPanelList('users');
        return false;
    });

    $("#user-panel-tab-bar a").click(function() {
        selectTabButton(this);
        if ($(this).attr("id") == "button-user-list") {
            //at this point we might be on another panel, so switch back to users and reset the search
            userLetter = '*';
            Pages.SelectPanel('users');
            showPanelList('users');
        }
        else {
            showPanelContent("#user-panel", "#user-panel-" + $(this).attr("id").substring(12));
        }
        return false;
    });

    //createAlphabetLinks("#users-by-alphabetical",usergrid.console.showUsersForLetter);
    //createAlphabetLinks("#groups-by-alphabetical", usergrid.console.showGroupsForLetter);

    $("#button-groups-list").click(function() {
        showPanelList('groups');
        return false;
    });

    $("#group-panel-tab-bar a").click(function() {
        selectTabButton(this);
        if ($(this).attr("id") == "button-group-list") {
            //at this point we might be on another panel, so switch back to groups and reset the search
            groupLetter = '*';
            Pages.SelectPanel('groups');
            showPanelList('groups');
        } else {
            showPanelContent("#group-panel", "#group-panel-" + $(this).attr("id").substring(13));
        }
        return false;
    });

    $("#roles-panel-tab-bar a").click(function() {
        if ($(this).attr("id") == "button-roles-list") {
            Pages.SelectPanel('roles');
            showPanelList('roles');
        }
        else if ($(this).attr("id") == "button-roles-search") {
            //show the search tab
            selectTabButton('#button-roles-search');
            //populate the panel content
            $('#roles-panel-list').hide();
            $('#roles-panel-search').show();
        } else {
            Pages.SelectPanel('roles');
        }
        return false;
    });

    $("#role-panel-tab-bar a").click(function() {
        if ($(this).attr("id") == "button-role-list") {
            Pages.SelectPanel('roles');
            showPanelList('roles');
        }
        else if ($(this).attr("id") == "button-role-search") {
            //show the search tab
            selectTabButton('#button-roles-search');
            //populate the panel content
            $('#roles-panel-list').hide();
            $('#roles-panel-search').show();
        } else {
            Pages.SelectPanel('roles');
        }
        return false;
    });

    $("button, input:submit, input:button").button();

    $('select#indexSelect').change( function(e){
	    $("#query-ql").val($(this).val() || "");
    });

    doBuildIndexMenu();

    function enableApplicationPanelButtons() {
        $("#application-panel-buttons").show();
    }

    function disableApplicationPanelButtons() {
        $("#application-panel-buttons").hide();
    }

    $('#system-panel-button-home').addClass('ui-selected');
    $('#application-panel-buttons .ui-selected').removeClass('ui-selected');
    
    $("#start-date").datepicker();
    $("#start-date").datepicker("setDate", Date.last().sunday());
    $('#start-time').val("12:00 AM");
    $('#start-time').timepicker({
        showPeriod: true,
        showLeadingZero: false
    });

    $("#end-date").datepicker();
    $("#end-date").datepicker("setDate", Date.next().sunday());
    $('#end-time').val("12:00 AM");
    $('#end-time').timepicker({
        showPeriod: true,
        showLeadingZero: false
    });
    
    //$('select#resolutionSelect').selectmenu();
    
    $('#button-analytics-generate').click(function() {
        requestApplicationCounters();
        return false;
    });

    //$("#console-panel-nav-bar").usergrid_console_navbar({crumbs : [{title : "Hello"}, {title : "Goodbye"}], tabs : [{title : "Hello"}, {title : "Goodbye"}]});


    if (OFFLINE) {
        Pages.ShowPage(OFFLINE_PAGE)
      return;
    }

    function loginOk(){
        clearLoginError();
        clearLoginForm();
        if (client.loggedIn())
            Pages.ShowPage('console');
    }
    usergrid.console.loginOk = loginOk;
    client.onAutoLogin = loginOk;
}