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

AppServices.Controllers.controller('ShellCtrl', ['ug', '$scope', '$log','$sce',
  function (ug, $scope, $log,$sce) {

    $scope.shell = {input:'',output:''}
    $scope.submitCommand = function(){
      if(!$scope.shell.input || !$scope.shell.input.length){
        return ;
      }
      handleShellCommand($scope.shell.input);
     // $scope.shell.input ="";
    };

    var handleShellCommand = function (s) {
      var path = '';
      var params = '';
      var shouldScroll = false;
      var hasMatchLength = function( expression){
        var res = s.match(expression);
        return res && res.length > 0;
      };
      try{
      //Matches any sting that begins with "/", ignoring whitespaces.
      switch (true){
        case hasMatchLength(/^\s*\//) :
          path = encodePathString(s);
          printLnToShell(path);
          ug.runShellQuery("GET",path, null);
          break;
        case hasMatchLength(/^\s*get\s*\//i) :
          path = encodePathString(s.substring(4));
          printLnToShell(path);
          ug.runShellQuery("GET",path, null);
          break;
        case hasMatchLength(/^\s*put\s*\//i) :
          params = encodePathString(s.substring(4), true);
          printLnToShell(params.path);
          ug.runShellQuery("PUT",params.path, params.payload);
          break;
        case hasMatchLength(/^\s*post\s*\//i) :
          params = encodePathString(s.substring(5), true);
          printLnToShell(params.path);
          ug.runShellQuery("POST",params.path, params.payload);
          break;
        case hasMatchLength(/^\s*delete\s*\//i) :
          path = encodePathString(s.substring(7));
          printLnToShell(path);
          ug.runShellQuery("DELETE",path,null);
          break;
        case hasMatchLength(/^\s*clear|cls\s*/i) :
          $scope.shell.output = "";
            shouldScroll = true;
          break;
        case hasMatchLength(/(^\s*help\s*|\?{1,2})/i)  :
          shouldScroll = true;

          printLnToShell("/&lt;path&gt; - API get request");
          printLnToShell("get /&lt;path&gt; - API get request");
          printLnToShell("put /&lt;path&gt; {&lt;json&gt;} - API put request");
          printLnToShell("post /&lt;path&gt; {&lt;json&gt;} - API post request");
          printLnToShell("delete /&lt;path&gt; - API delete request");
          printLnToShell("cls, clear - clear the screen");
          printLnToShell("help - show this help");
          break;
        case s ==="":
          shouldScroll = true;
          printLnToShell("ok");
          break;
        default :
          shouldScroll = true;
          printLnToShell('<strong>syntax error!</strong>');
          break;
      }
      }catch(e){
        $log.error(e);
        printLnToShell('<strong>syntax error!</strong>');
      }
      shouldScroll && scroll();
    };

    var  printLnToShell =  function (s) {
      if (!s) s = "&nbsp;";
      $scope.shell.outputhidden = s;
      var html = '<div class="shell-output-line"><div class="shell-output-line-content">' + s + '</div></div>';
      html += ' ';
      var trustedHtml = $sce.trustAsHtml( html);
      $scope.shell.output += trustedHtml.toString();
    }

    $scope.$on('shell-success',function(evt,data){
      printLnToShell(JSON.stringify(data, null, "  "));
      scroll();
    });

    $scope.$on('shell-error',function(evt,data){
      printLnToShell(JSON.stringify(data, null, "  "));
      scroll();
    });

    var scroll = function(){
      $scope.shell.output += '<hr />';
      $scope.applyScope();
      setTimeout(function(){
        var myshell = $('#shell-output');
        myshell.animate({scrollTop:myshell[0].scrollHeight},800);

      },200);
    }

    function encodePathString(path, returnParams) {

      var i = 0;
      var segments = new Array();
      var payload = null;
      while (i < path.length) {
        var c = path.charAt(i);
        if (c == '{') {
          var bracket_start = i;
          i++;
          var bracket_count = 1;
          while ((i < path.length) && (bracket_count > 0)) {
            c = path.charAt(i);
            if (c == '{') {
              bracket_count++;
            } else if (c == '}') {
              bracket_count--;
            }
            i++;
          }
          if (i > bracket_start) {
            var segment = path.substring(bracket_start, i);
            segments.push(JSON.parse(segment));
          }
          continue;
        } else if (c == '/') {
          i++;
          var segment_start = i;
          while (i < path.length) {
            c = path.charAt(i);
            if ((c == ' ') || (c == '/') || (c == '{')) {
              break;
            }
            i++;
          }
          if (i > segment_start) {
            var segment = path.substring(segment_start, i);
            segments.push(segment);
          }
          continue;
        } else if (c == ' ') {
          i++;
          var payload_start = i;
          while (i < path.length) {
            c = path.charAt(i);
            i++;
          }
          if (i > payload_start) {
            var json = path.substring(payload_start, i).trim();
            payload = JSON.parse(json);
          }
          break;
        }
        i++;
      }

      var newPath = "";
      for (i = 0; i < segments.length; i++) {
        var segment = segments[i];
        if (typeof segment === "string") {
          newPath += "/" + segment;
        } else {
          if (i == (segments.length - 1)) {
            if (returnParams) {
              return {path : newPath, params: segment, payload: payload};
            }
            newPath += "?";
          } else {
            newPath += ";";
          }
          newPath += encodeParams(segment);
        }
      }
      if (returnParams) {
        return {path : newPath, params: null, payload: payload};
      }
      return newPath;
    }

    function encodeParams(params) {
      var tail = [];
      if (params instanceof Array) {
        for (i in params) {
          var item = params[i];
          if ((item instanceof Array) && (item.length > 1)) {
            tail.push(item[0] + "=" + encodeURIComponent(item[1]));
          }
        }
      } else {
        for (var key in params) {
          if (params.hasOwnProperty(key)) {
            var value = params[key];
            if (value instanceof Array) {
              for (i in value) {
                var item = value[i];
                tail.push(key + "=" + encodeURIComponent(item));
              }
            } else {
              tail.push(key + "=" + encodeURIComponent(value));
            }
          }
        }
      }
      return tail.join("&");
    }

  }]
);