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
		$("#logout-link").click(usergrid.console.logout);
		usergrid.console.loginOk();

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
        triangle.prependTo("#console-panels h3");
        triangleOpen.prependTo("#console-panels h3").hide();
	}

	function InitMenu() {
		$('.navbar .dropdown-toggle').dropdown();
		$('#sidebar-menu .dropdown-toggle').dropdown();

        var publicMenu = $("#publicMenu");
        var privateMenu =$("#privateMenu");

		Pages.AddPage({name:'login', menu:publicMenu});
		Pages.ShowPage('login');

		Pages.AddPage({name:'signup', menu:publicMenu});
		Pages.AddPage({name:'forgot-password', menu:publicMenu});

        Pages.AddPage({name:'console', menu:privateMenu, initFunction:InitConsole, showFunction:usergrid.console.pageSelectHome});
        Pages.AddPage({name:'account', menu:privateMenu, initFunction:InitConsole, showFunction:usergrid.console.requestAccountSettings});
	}

	function InitConsole() {
		//Pages.AddPanel(pageName,linkSelector,boxSelector,initfunc,showfunc);
		Pages.AddPanel('organization', null, null, null, null);
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

		//$("#sidebar-menu > ul > li > a").click(Pages.ShowPanel);
	}
    
});