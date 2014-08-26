//
// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements.  See the NOTICE file distributed with
// this work for additional information regarding copyright ownership.
// The ASF licenses this file to You under the Apache License, Version 2.0
// (the "License"); you may not use this file except in compliance with
// the License.  You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

$(document).ready(function () {

//call the runner function to start the process
  $('#start-button').bind('click', function() {
    $('#start-button').attr("disabled", "disabled");
    $('#test-output').html('');
    runner(0);
  });

  var logSuccess = true;
  var successCount = 0;
  var logError = true;
  var errorCount = 0;
  var logNotice = true;

  var client = new Usergrid.Client({
    orgName:'yourorgname',
    appName:'sandbox',
    logging: true, //optional - turn on logging, off by default
    buildCurl: true //optional - turn on curl commands, off by default
  });

  function runner(step, arg, arg2){
    step++;
    switch(step)
    {
      case 1:
        notice('-----running step '+step+': create and serialize collection');
        createAndSerialzeCollection(step);
        break;
      case 2:
        notice('-----running step '+step+': de-serialize collection');
        deserializeCollection(step);
        break;
      default:
        notice('-----test complete!-----');
        notice('Success count= ' + successCount);
        notice('Error count= ' + errorCount);
        notice('-----thank you for playing!-----');
        $('#start-button').removeAttr("disabled");
    }
  }

//logging functions
  function success(message){
    successCount++;
    if (logSuccess) {
      console.log('SUCCESS: ' + message);
      var html = $('#test-output').html();
      html += ('SUCCESS: ' + message + '\r\n');
      $('#test-output').html(html);
    }
  }

  function error(message){
    errorCount++
    if (logError) {
      console.log('ERROR: ' + message);
      var html = $('#test-output').html();
      html += ('ERROR: ' + message + '\r\n');
      $('#test-output').html(html);
    }
  }

  function notice(message){
    if (logNotice) {
      console.log('NOTICE: ' + message);
      var html = $('#test-output').html();
      html += (message + '\r\n');
      $('#test-output').html(html);
    }
  }


  function createAndSerialzeCollection(step){
    var options = {
      type:'books',
      qs:{ql:'order by name'}
    }

    client.createCollection(options, function (err, books) {
      if (err) {
        error('could not make collection');
      } else {

        //collection made, now serialize and store
        localStorage.setItem('item', books.serialize());

        success('new Collection created and data stored');

        runner(step);

      }
    });
  }


  function deserializeCollection(step){
    var books = client.restoreCollection(localStorage.getItem('item'));

    while(books.hasNextEntity()) {
      //get a reference to the book
      book = books.getNextEntity();
      var name = book.get('name');
      notice('book is called ' + name);
    }

    success('looped through books');

    runner(step);
  }

});