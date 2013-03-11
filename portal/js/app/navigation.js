/*
  * Usergrid.Navigation
  *
  * Functions to control the navigation of the Console client
  *
  * Uses:  Backbone.router
  *
  * Requires: Backbonejs, Underscore.js
  *
  */
Usergrid.Navigation = Backbone.Router.extend({
    routes: {
      ":organization/:application/organization": "home",
      ":organization/:application/dashboard": "dashboard",
      ":organization/:application/users": "users",
      ":organization/:application/groups": "groups",
      ":organization/:application/roles": "roles",
      ":organization/:application/activities": "activities",
      ":organization/:application/collections": "collections",
      ":organization/:application/notifications": "notifications",
      ":organization/:application/analytics": "analytics",
      ":organization/:application/properties": "properties",
      ":organization/:application/shell": "shell",
      ":organization/:application/account": "account",
      ":organization/home": "home",
      ":organization": "home",
      "": "home"
    },
    //Router Methods
    home: function(organization, application) {
      if(organization) {
        this.checkOrganization(organization);
      }
      this.checkApplication(application);
      Pages.SelectPanel('organization');
      $('#left2').hide();
    },
    dashboard: function(organization,application) {
      this.checkOrganization(organization);
      this.checkApplication(application);
      Usergrid.console.pageSelect(application);
      Pages.SelectPanel('dashboard');
      $('#left2').hide();
    },
    users: function(organization, application) {
      this.checkOrganization(organization);
      this.checkApplication(application);
      Pages.SelectPanel('users');
      this.showAppUserContent();
    },
    groups: function(organization, application) {
      this.checkOrganization(organization);
      this.checkApplication(application);
      Pages.SelectPanel('groups');
      this.showAppUserContent();
    },
    roles: function(organization, application) {
      this.checkOrganization(organization);
      this.checkApplication(application);
      Pages.SelectPanel('roles');
      this.showAppUserContent();
    },
    activities: function(organization, application) {
      this.checkOrganization(organization);
      this.checkApplication(application);
      Pages.SelectPanel('activities');
      $('#left2').hide();
    },
    collections: function(organization, application) {
      this.checkOrganization(organization);
      this.checkApplication(application);
      Pages.ActivatePanel("collections");
      Pages.SelectPanel('collections');
      this.showAppDataContent();
    },
    notifications: function(organization, application) {
      this.checkOrganization(organization);
      this.checkApplication(application);
      Pages.ActivatePanel("notifications");
      Pages.SelectPanel('notifications');
      this.showAppNotificationsContent();
    },
    analytics: function(organization, application) {
      this.checkOrganization(organization);
      this.checkApplication(application);
      Pages.SelectPanel('analytics');
      $('#left2').hide();
    },
    properties: function(organization, application) {
      this.checkOrganization(organization);
      this.checkApplication(application);
      Pages.SelectPanel('properties');
      $('#left2').hide();
    },
    shell: function(organization, application) {
      this.checkOrganization(organization);
      this.checkApplication(application);
      Pages.SelectPanel('shell');
      $('#left2').hide();
    },
    account: function(organization, application) {
      Pages.SelectPanel('account');
      $('#left2').hide();
    },
    //Utils
    checkOrganization: function(org) {
      if(!this.isActiveOrganization(org)) {
        Usergrid.console.selectOrganization(org);
      }
    },
    isActiveOrganization: function(org) {
      if(org) {
        if(Usergrid.ApiClient.getOrganizationName() === org ) {
          return true
        }
      }
      return false
    },
    checkApplication: function(app) {
      if(app){
        if(!this.isActiveApplication(app)) {
          Usergrid.console.pageSelect(app);
        }
      }
    },
    isActiveApplication: function(app) {
      if(app) {
        if(Usergrid.ApiClient.getApplicationName() === app) {
          return true
        }
      }
      return false
    },
    showAppUserContent: function(){
      $('#left2').show();
      $('#sidebar-menu2').show();
      $('#left-collections-menu').hide();
      $('#left-notifications-menu').hide();
    },
    showAppDataContent: function(){
      $('#left2').show();
      $('#sidebar-menu2').hide();
      $('#left-collections-menu').show();
      $('#left-notifications-menu').hide();
    },
    showAppNotificationsContent: function(){
      $('#left2').show();
      $('#sidebar-menu2').hide();
      $('#left-collections-menu').hide();
      $('#left-notifications-menu').show();
    },

    navigateTo: function(address) {
      var url;
      url = Usergrid.ApiClient.getOrganizationName();
      url += "/" + Usergrid.ApiClient.getApplicationName();
      url += "/" + address;
      // Backbone navigate only triggers page loading if url changes
      // loading manually if the url hasn't changed is necessary.
      if(Backbone.history.fragment === url) {
        Backbone.history.loadUrl(url);
      } else {
        this.navigate(url, {trigger: true});
      }
    }
  });

Usergrid.Navigation.router = new Usergrid.Navigation();
_.bindAll(Usergrid.Navigation.router);