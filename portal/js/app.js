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

		//Pages.AddPage(name,link,box,init,extra);
		Pages.AddPage('login',null,null,null,null);
		Pages.ShowPage('login');

		Pages.AddPage('signup',null,null,null,null);
		Pages.AddPage('forgot-password',null,null,null,null);

		//Pages.AddPage('navlist',null,null,null,null);
		Pages.AddPage('console',null,null,InitNavigation,null);
		Pages.AddPage('account',null,null,null,usergrid.console.requestAccountSettings);

		usergrid_console_app();

		$("#logout-link").click(usergrid.console.logout);
	}

	function InitNavigation (){
		$("#sidebar-menu > ul > li > a").click(Pages.ShowPanel);
	}
});