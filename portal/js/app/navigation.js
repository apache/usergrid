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
      ":organization/:application/analytics": "analytics",
      ":organization/:application/properties": "properties",
      ":organization/:application/shell": "shell",
      ":organization/:application/console": "console",
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
    },
    dashboard: function(organization,application) {
      this.checkOrganization(organization);
      this.checkApplication(application);
      Usergrid.console.pageSelect(application);
      Pages.SelectPanel('dashboard');
    },
    users: function(organization, application) {
      this.checkOrganization(organization);
      this.checkApplication(application);
      Pages.SelectPanel('users');
    },
    groups: function(organization, application) {
      this.checkOrganization(organization);
      this.checkApplication(application);
      Pages.SelectPanel('groups');
    },
    roles: function(organization, application) {
      this.checkOrganization(organization);
      this.checkApplication(application);
      Pages.SelectPanel('roles')
    },
    activities: function(organization, application) {
      this.checkOrganization(organization);
      this.checkApplication(application);
      Pages.SelectPanel('activities');
    },
    collections: function(organization, application) {
      this.checkOrganization(organization);
      this.checkApplication(application);
      Pages.SelectPanel('collections');
    },
    analytics: function(organization, application) {
      this.checkOrganization(organization);
      this.checkApplication(application);
      Pages.SelectPanel('analytics');
    },
    properties: function(organization, application) {
      this.checkOrganization(organization);
      this.checkApplication(application);
      Pages.SelectPanel('properties');
    },
    shell: function(organization, application) {
      this.checkOrganization(organization);
      this.checkApplication(application);
      Pages.SelectPanel('shell');
    },
    console: function(organization, application) {
      this.checkOrganization(organization);
      this.checkApplication(application);
      Pages.SelectPanel('console');
    },
    account: function(organization, application) {
      Pages.SelectPanel('account');
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

    navigateTo: function(address) {
      var url;
      url = Usergrid.ApiClient.getOrganizationName();
      url += "/" + Usergrid.ApiClient.getApplicationName();
      url += "/" + address;
      this.navigate(url, true);
    }
  });

Usergrid.Navigation.router = new Usergrid.Navigation();
_.bindAll(Usergrid.Navigation.router);