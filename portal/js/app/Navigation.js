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
      ":organization/:application/home": "home",
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
      ":organization/home": "home",
      "": "home"
    },
    //Router Methods
    initAddress: function() {
      this.navigateToHome();
    },
    home: function(organization, application) {
      //TODO: for debug only
      console.log("Home");
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
      Usergrid.console.pageSelectApplication();
    },
    users: function(organization, application) {
      this.checkOrganization(organization);
      this.checkApplication(application);
      Usergrid.console.pageSelectUsers();
    },
    groups: function(organization, application) {
      this.checkOrganization(organization);
      this.checkApplication(application);
      Usergrid.console.pageSelectGroups();
    },
    roles: function(organization, application) {
      this.checkOrganization(organization);
      this.checkApplication(application);
      Usergrid.console.pageSelectRoles();
    },
    activities: function(organization, application) {
      this.checkOrganization(organization);
      this.checkApplication(application);
      Usergrid.console.pageSelectActivities();
    },
    collections: function(organization, application) {
      this.checkOrganization(organization);
      this.checkApplication(application);
      Usergrid.console.pageSelectCollections();
    },
    analytics: function(organization, application) {
      this.checkOrganization(organization);
      this.checkApplication(application);
      Usergrid.console.pageSelectAnalytics();
    },
    properties: function(organization, application) {
      this.checkOrganization(organization);
      this.checkApplication(application);
      Usergrid.console.pageSelectProperties();
    },
    shell: function(organization, application) {
      this.checkOrganization(organization);
      this.checkApplication(application);
      Usergrid.console.pageSelectShell();
    },
    console: function(organization, application) {
      this.checkOrganization(organization);
      this.checkApplication(application);
    },
    //Utils
    checkOrganization: function(org) {
      if(!this.isActiveOrganization(org)) {
        Usergrid.console.selectOrganization(org);
      }
    },
    isActiveOrganization: function(org) {
      console.log("ORG: " + org); //TODO: REMOVE AFTER DEBUG
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
      console.log("App " + app) // TODO: REMOVE AFTER DEBUG
      if(app) {
        if(Usergrid.ApiClient.getApplicationName() === app) {
          return true
        }
      }
      return false
    },
    navigateToHome: function() {
      this.navigateTo('home');
    },
    navigateToDashboard: function() {
      this.navigateTo('dashboard');
    },
    navigateToUsers: function() {
      this.navigateTo('users');
    },
    navigateToGroups: function() {
      this.navigateTo('groups');
    },
    navigateToRoles: function() {
      this.navigateTo('roles');
    },
    navigateToActivities: function() {
      this.navigateTo('activities');
    },
    navigateToCollections: function() {
      this.navigateTo('collections');
    },
    navigateToAnalytics: function() {
      this.navigateTo('analytics');
    },
    navigateToProperties: function() {
      this.navigateTo('properties');
    },
    navigateToShell: function() {
      this.navigateTo('shell');
    },
    navigateToConsole: function() {
      this.navigateTo('console');
    },
    navigateTo: function(address) {
      var url;
      url = "/" + Usergrid.ApiClient.getOrganizationName();
      url += "/" + Usergrid.ApiClient.getApplicationName();
      url += "/" + address;
      this.navigate(url, {trigger: true});
    }
  });

Usergrid.Navigation.router = new Usergrid.Navigation();
_.bindAll(Usergrid.Navigation.router);