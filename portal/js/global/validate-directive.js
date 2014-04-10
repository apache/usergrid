/**
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */
'use strict';
AppServices.Directives.directive('ugValidate', ["$rootScope", function ($rootScope) {
  return{
    scope:true,
    restrict: 'A',
    require:'ng-model',
    replace: true,
    link: function linkFn(scope, element, attrs, ctrl) {
      var validate = function(){
        var id = element.attr('id');
        var validator = id+'-validator';
        var title = element.attr('title');
        title = title && title.length  ? title : 'Please enter data' ;
        $('#'+validator).remove();
        if(!ctrl.$valid){
          var validatorElem = '<div id="'+validator+'"><span  class="validator-error-message">'+title+'</span></div>';
          $( '#'+id ).after( validatorElem);
          element.addClass('has-error');
        }else{
          element.removeClass('has-error');
          $('#'+validator).remove();
        }
      };

      var firing = false;
      element.bind('blur', function (evt) {
        validate(scope,element,attrs,ctrl);
      }).bind('input', function (evt) {
            if(firing){
              return ;
            }
            firing = true;
            setTimeout(function(){
              validate(scope,element,attrs,ctrl);
              firing=false;
            },500)
          });
    }
  };
}]);