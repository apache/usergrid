
AppServices.Directives.directive('insecureBanner', ['$rootScope','ug', function ($rootScope,ug) {
  return{
    restrict: 'E',
    transclude: true,
    templateUrl:'global/insecure-banner.html',
    link: function linkFn(scope, lElement, attrs) {
      scope.securityWarning = false;

      scope.$on('roles-received',function(evt,roles){
        scope.securityWarning = false;
        if(!roles || !roles._list)
          return;
        roles._list.forEach(function(roleHolder){
          var role = roleHolder._data;
          if(role.name.toUpperCase()==='GUEST'){
            roleHolder.getPermissions(function(err, data){
              if (!err) {
                if(roleHolder.permissions){
                  roleHolder.permissions.forEach(function(permission){
                    if(permission.path.indexOf('/**')>=0){
                      scope.securityWarning = true;
                      scope.applyScope();
                    }
                  });
                }
              }
            });

          }
        });
      });

      var initialized = false;
      scope.$on('app-initialized',function(){
        !initialized && ug.getRoles();
        initialized=true;
      });

      scope.$on('app-changed',function(){
        scope.securityWarning = false;
        ug.getRoles();
      });
    }
  }
}]);