'use strict';
AppServices.MAX = AppServices.MAX || angular.module('appservices.max', []);

AppServices.MAX.controller('ConfigureCtrl',
  [ 'data',
    '$scope',
    '$rootScope',
    '$log',
    function (data, $scope, $rootScope, $log) {

      $scope.logLevels = [
        {value: 2, label: 'Verbose'},
        {value: 3, label: 'Debug'},
        {value: 4, label: 'Info'},
        {value: 5, label: 'Warn'},
        {value: 6, label: 'Error'},
        {value: 7, label: 'Assert'}
      ];
      var getAccessToken = function(){
        return sessionStorage.getItem('accessToken');
      };
      function configPageSetup(){

        $scope.deviceNumberFilters = '';
        $scope.deviceIdFilters = '';
        $scope.defaultAppConfigParameters = [{tag:'',paramKey:'',paramValue:''}];
        $scope.deviceLevelConfigParameters  = [{tag:'',paramKey:'',paramValue:''}];
        $scope.abConfigParameters  = [{tag:'',paramKey:'',paramValue:''}];
        $scope.$on('app-initialized', function () {
          var configData = data.resource(
            {
              appname:$rootScope.currentApp,
              orgname:$rootScope.currentOrg,
              username:'apm',
              endpoint:'apigeeMobileConfig'
            },false).get({callback:'JSON_CALLBACK',access_token:getAccessToken()},
            function (cdata) {

              $scope.configData = cdata;


              if(cdata.deviceNumberFilters && cdata.deviceNumberFilters.length > 0){
                angular.forEach(cdata.deviceNumberFilters, function(value, key){
                  $scope.deviceNumberFilters += value.filterValue + '\n';
                });
              }

              if(cdata.deviceIdFilters && cdata.deviceIdFilters.length > 0){
                angular.forEach(cdata.deviceIdFilters, function(value, key){
                  $scope.deviceIdFilters += value.filterValue + '\n';
                });
              }

              //default configs
              angular.forEach($scope.logLevels, function(value, key){
                if(cdata.defaultAppConfig.logLevelToMonitor === value.value){
                  $scope.appConfigLogLevel = $scope.logLevels[key]
                }
              });

              if(cdata.defaultAppConfig.customConfigParameters.length > 0){
                $scope.defaultAppConfigParameters = cdata.defaultAppConfig.customConfigParameters;
              }

              //beta configs
              angular.forEach($scope.logLevels, function(value, key){
                if(cdata.deviceLevelAppConfig.logLevelToMonitor === value.value){
                  $scope.betaLogLevel = $scope.logLevels[key]
                }
              });

              if(cdata.deviceLevelAppConfig.customConfigParameters.length > 0){
                $scope.deviceLevelConfigParameters = cdata.deviceLevelAppConfig.customConfigParameters;
              }


              //a/b configs
              angular.forEach($scope.logLevels, function(value, key){
                if(cdata.abtestingAppConfig.logLevelToMonitor === value.value){
                  $scope.abLogLevel = $scope.logLevels[key]
                }
              });

              if(cdata.abtestingAppConfig.customConfigParameters.length > 0){
                $scope.abConfigParameters = cdata.abtestingAppConfig.customConfigParameters;
              }

            },
            function (fail) {
              $log.error('problem getting config data: ', fail);
            });

        });

      }

      configPageSetup();


      $scope.saveData = function(configStore){

          //check the array for a blank entry at the end
          var configStoreLength = $scope[configStore].length
          if(configStoreLength > 0){
            if($scope[configStore][(configStoreLength - 1)].tag === '' ||
              $scope[configStore][(configStoreLength - 1)].paramKey === '' ||
              $scope[configStore][(configStoreLength - 1)].paramValue === ''){
              $scope[configStore].pop();
            }
          }

          //remove the index property for UI needs only
          angular.forEach($scope[configStore], function(value, key){
            if(value.index){
              delete value.index;
            }
          });

          var payload;

          //update last modified timestamp
          $scope.configData.lastModifiedDate = new Date().getTime();

          payload = {'apigeeMobileConfig':$scope.configData};

          data.resource(
            {
            orgname:$rootScope.currentOrg,
            appname:$rootScope.currentApp
          },false).save(
            {
              access_token:getAccessToken()
            },
            payload);
           $rootScope.$broadcast('alert','success','Your values have been saved.')
     };

      $scope.updateLogLevel = function(logLevel,dataAttr){
        $scope.configData[dataAttr].logLevelToMonitor = logLevel.value;
      }



      $scope.updateFilter = function(filter,filterAttr,filterType){
        if(!filter || filter.length===0){
          console.warn("No "+filterType+" specified.");
          return;
        }
        if(!Array.isArray($scope.configData[filterAttr])){
          $scope.configData[filterAttr] = [];
        }
        $scope.configData[filterAttr].push({filterValue:filter,filterType:filterType});
      }




      $scope.addRemoveConfig = function(config,configStore){

        var addNew = true;
        if(config.index === $scope[configStore].length){
          //validation
          if(config.paramValue === '' && config.tag === '' && config.paramKey === ''){
            //remove blank
            $scope[configStore].pop();
            addNew = false;
          }
          if(addNew || 0 === $scope[configStore].length){
            //add it
            $scope[configStore].push({tag:'',paramKey:'',paramValue:''})
          }
        }else{
          //remove existing
          angular.forEach($scope[configStore], function(value, key){
            if(value.tag === config.tag && value.paramKey === config.paramKey && value.paramValue === config.paramValue){
              $scope[configStore].splice([key],1);
            }
          });
        }

        if(configStore === 'defaultAppConfigParameters'){
          $scope.configData.defaultAppConfig.customConfigParameters = $scope[configStore];
        }else if(configStore === 'deviceLevelConfigParameters'){
          $scope.configData.deviceLevelAppConfig.customConfigParameters = $scope[configStore];
        }else if(configStore === 'abConfigParameters'){
          $scope.configData.abtestingAppConfig.customConfigParameters = $scope[configStore];
        }

      }

      }])
