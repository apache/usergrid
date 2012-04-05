var usergridData = function() {
  var self = {};
  self.organizations = [];
  self.currentOrganization = null;

	self.setCurrentOrganization = function (orgName) {
    self.currentOrganization = null;
    if (!self.loggedInUser || !self.organizations)
      return;

    if(orgName)
      self.currentOrganization = self.organizations[orgName];
    else
      self.currentOrganization = self.organizations[localStorage.currentOrganizationName];

    if (!self.currentOrganization) {
      var firstOrg = null;
      for (firstOrg in self.organizations) break;
      if (firstOrg) self.currentOrganization = self.organizations[firstOrg];
    }

    localStorage.currentOrganizationName = self.currentOrganization.name;
	}
  

  return self;
}();
