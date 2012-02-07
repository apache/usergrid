/**
 * User: David S
 * Date: 31/01/12
 * Time: 03:01 PM
 */

$(document).ready(function() {

	Init();

	function Init(){
		$('.navbar .dropdown-toggle').dropdown();
		$('#sidebar-menu .dropdown-toggle').dropdown();

		//Pages.AddPage(name,link,box,init,show);
		Pages.AddPage('login',null,null,null,null);
		Pages.ShowPage('login');

		Pages.AddPage('signup',null,null,null,null);
		Pages.AddPage('forgot-password',null,null,null,null);

		//Pages.AddPage('navlist',null,null,null,null);
		Pages.AddPage('console',null,null,InitConsole,null);
		Pages.AddPage('account',null,null,null,usergrid.console.requestAccountSettings);

		usergrid_console_app();

		$("#logout-link").click(usergrid.console.logout);
	}

	function InitConsole (){
		//Pages.AddPanel(pageName,linkSelector,boxSelector,initfunc,showfunc);
		Pages.AddPanel('organization',null,null,null,null);
		Pages.AddPanel('application',null,null,null,usergrid.console.pageSelectApplication);
		Pages.AddPanel('user',null,null,null,null);
		Pages.AddPanel('users',null,null,null,usergrid.console.pageSelectUsers);
		Pages.AddPanel('groups',null,null,null,usergrid.console.pageSelectGroups);
		Pages.AddPanel('roles',null,null,null,usergrid.console.pageSelectRoles);
		Pages.AddPanel('activities',null,null,null,usergrid.console.pageSelectActivities);
		Pages.AddPanel('collections',null,null,null,usergrid.console.pageSelectCollections);
		Pages.AddPanel('analytics',null,null,null,usergrid.console.pageSelectAnalytics);
		Pages.AddPanel('settings',null,null,null,usergrid.console.pageSelectSettings);
		Pages.AddPanel('shell',null,null,null,usergrid.console.pageSelectShell);

		//$("#sidebar-menu > ul > li > a").click(Pages.ShowPanel);
	}
});