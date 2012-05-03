/**
 * User: David S
 * Date: 31/01/12
 * Time: 03:01 PM
 */

$(document).ready(function () {

	Init();

	function Init() {
		usergrid_console_app();
		InitMenu();
        StatusBar.Init('#statusbar-placeholder');
		    usergrid.console.loginOk();
        makePanelCollapsable();

	    if (getQueryParams().goto_signup) {
		    Pages.ShowPage("signup");
	    }

	}

    function makePanelCollapsable(){
        var triangle = $("<span class='openPanel'>&#9660;</span >");
        var triangleOpen =  $("<span class='closePanel'>&#9654;</span >");

        var titles = $("#console-panels h3 .title");
        triangle.appendTo(titles);
        triangleOpen.appendTo(titles).hide();
        titles.click(toggleSection);
    }

    function toggleSection(e){
        e.preventDefault();
        var h3 = $(this).parent();
        h3.parent().find(".hideable").toggle();
        h3.find(".openPanel").toggle();
        h3.find(".closePanel").toggle();
    }

    function makeResultCollapsable(){
        var triangle = $("<a class='openPanel'>&#9660;</a>").click( function(e){
            e.preventDefault();
            var link = $(this);
            link.parent().parent().find(".console-section-contents").slideUp();
            link.parent().find(".closePanel").show();
            link.hide();
        });
        var triangleOpen =  $("<a class='closePanel'>&#9654;</a>").click( function(e){
            e.preventDefault();
            var link = $(this);
            link.parent().parent().find(".console-section-contents").slideDown();
            link.parent().find(".openPanel").show();
            link.hide();
        });
        triangle.prependTo(".query-result-portlet-title");
        triangleOpen.prependTo(".query-result-portlet-title").hide();
    }


	function InitMenu() {
		$('.navbar .dropdown-toggle').dropdown();
		$('#sidebar-menu .dropdown-toggle').dropdown();
        $("#logout-link").click(usergrid.console.logout);

        var publicMenu = $("#publicMenu");
        var privateMenu =$("#privateMenu");

		Pages.AddPage({name:'login', menu:publicMenu});
		Pages.ShowPage('login');

        Pages.AddPage({name:'signup', menu:publicMenu});		
        Pages.AddPage({name:'forgot-password', menu:publicMenu});
        Pages.AddPage({name:'post-signup', menu:publicMenu});

        Pages.AddPage({name:'console', menu:privateMenu, initFunction:InitConsole, showFunction:usergrid.console.pageSelectHome});
   
	}

	function InitConsole() {
		//Pages.AddPanel(pageName,linkSelector,boxSelector,initfunc,showfunc);
		Pages.AddPanel('organization', null, null, null, null);
    Pages.AddPanel('console', null, null, initConsoleFrame,null );
		Pages.AddPanel('application', null, null, null, usergrid.console.pageSelectApplication);
		Pages.AddPanel('user', "#sidebar-menu a[href='#users']", null, null, null);
		Pages.AddPanel('users', null, null, null, usergrid.console.pageSelectUsers);
		Pages.AddPanel('group', "#sidebar-menu a[href='#groups']", null, null, null);
		Pages.AddPanel('groups', null, null, null, usergrid.console.pageSelectGroups);		
		Pages.AddPanel('roles', null, null, null, usergrid.console.pageSelectRoles);
		Pages.AddPanel('activities', null, null, null, usergrid.console.pageSelectActivities);
		Pages.AddPanel('collections', null, null, null, usergrid.console.pageSelectCollections);
		Pages.AddPanel('analytics', null, null, null, usergrid.console.pageSelectAnalytics);
		Pages.AddPanel('settings', null, null, null, usergrid.console.pageSelectSettings);
		Pages.AddPanel('shell', null, null, null, usergrid.console.pageSelectShell);
    Pages.AddPanel('account', "#account-link", null, null, usergrid.console.requestAccountSettings);
		//$("#sidebar-menu > ul > li > a").click(Pages.ShowPanel);
	}
	
    function getQueryParams() {
        var query_params = {};
        var e,
            a = /\+/g,
            r = /([^&=]+)=?([^&]*)/g,
            d = function (s) { return decodeURIComponent(s.replace(a, " ")); },
            q = window.location.search.substring(1);

        while (e = r.exec(q))
           query_params[d(e[1])] = d(e[2]);
        return query_params;
    }

    function initConsoleFrame(){
        /*
       // var bearerToken = usergrid.console.getAccessToken();
        var bearerToken = localStorage.getObject('usergrid_access_token');
        var bearerTokenJson = JSON.stringify(
            {
            "type":"custom_token",
            "name":"Authorization",
            "value":"Bearer " + bearerToken,
            "style":"header"
            },
            {
            "type":"custom_token",
            "name":"app_id",
            "value":"8a197065-58c5-11e1-bfb7-22000a1c5a67",
            "style":"template"
            });
        //var string='[{"type":"custom_token","name":"Authorization","value":"Bearer '+bearerToken+'","style":"header"},{"type":"custom_token","name":"app_id","value":"8a197065-58c5-11e1-bfb7-22000a1c5a67","style":"template"}]';
        var bearerTokenString = encodeURIComponent(bearerTokenJson);
        //var bearerTokenString = encodeURIComponent("[{'type':'custom_token','access_token':'"+bearerToken+"'}]");
        var url = 'https://apigee.com/console/usergrid?embedded=true&auth='+bearerTokenString;
        $("#console-panel iframe").attr("src", url);
        */
    }
    
});
