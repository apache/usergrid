'use strict';
AppServices.MAX = AppServices.MAX || angular.module('appservices.max', []);

AppServices.MAX.filter('rawLogFilter', ['data', function (data) {
  return function (items, searchObj, fullTextAttrs, greaterThanAttrs) {

    if(items){

      //build a temporary array with populated search strings
      var tempSearchArray = [];
      for(var key in searchObj){
        var obj = searchObj[key];
        var tempObj = {};

        if(obj){
          tempObj[key] = obj
          tempSearchArray.push(tempObj)
        }

      }

      if(tempSearchArray.length < 1){
        return items;
      }

      return items.filter(function(element,index,array){

        var tempItems = [];
        var compositeCheck = [];

        //loop over populated search values
        for(var i = 0; i < tempSearchArray.length; i++){

          for(var elementKey in element){
            //does this array object have a matching key?
            if(tempSearchArray[i][elementKey]){

              var searchString = tempSearchArray[i][elementKey],
                elementString = element[elementKey];

                var attrsCheck = false;

                if(greaterThanAttrs && greaterThanAttrs.length > 0){
                  //doing server side check which returns all values greater that input
                  //..we know the server is returning what user needs to see, so push true value
                  compositeCheck.push(true);
                  attrsCheck = true;

                }else if(fullTextAttrs && fullTextAttrs.length > 0){
                  for(var j = 0; j < fullTextAttrs.length; j++){
                    if(elementKey == fullTextAttrs[j]){
                      compositeCheck.push((elementString + '').search(new RegExp(searchString, "i")) != -1);
                      attrsCheck = true;
                    }
                  }
                }

                if(!attrsCheck){
                //case insensitive "index of"
                compositeCheck.push((elementString + '').search(new RegExp(searchString, "i")) == 0);
                }
            }

          }

        }

        //for each item, check the recorded true/false history
        function checkHistory(){
          for(var i = 0; i < compositeCheck.length; i++){
            if(!compositeCheck[i]){
              return false;
            }
          }
          //otherwise no negatives found

          return true;
        }

        return checkHistory();

      })

    }

  }

}]);