
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