---
title: Client authorization
category: docs
layout: docs
---

Client authorization
====================

[See all management
resources](/docs/usergrid/content/management-resources)[![](/docs/sites/docs/files/learnmore%20arrow_0.png)](/docs/usergrid/content/management-resources)

Using the App services API, you can authorize a client.

Authorizing a client
--------------------

Use the GET method to authorize a client.

### Request URI

GET
/management/authorize?response\_type={response\_type}&client\_id={client\_id}

### Parameters

  Parameter               Description
  ----------------------- -------------------------------------------------------------------------------------------------
  string response\_type   The [OAuth 2.0 response type](http://tools.ietf.org/html/draft-ietf-oauth-v2-31#section-3.1.1).
  string client\_id       Organization client ID

 

### Example - Request

-   [cURL](#curl_auth_client)
-   [JavaScript (HTML5)](#javascript_auth_client)
-   [Ruby](#ruby_auth_client)
-   [Node.js](#nodejs_auth_client)

<!-- -->

    curl -X GET "https://api.usergrid.com/management/authorize?response_type=token&client_id=b0U5N81ME96NSeG78xIxQFxelQ"

It is recommended that you use the [Admin
Portal](http://apigee.com/usergrid) for administrative activities
instead of using JavaScript to do them programmatically in your app.

**Note:**You can see the response below in the Admin Portal by using the
[JavaScript
Console](/docs/usergrid/content/displaying-app-services-api-calls-curl-commands).

The example assumes use of the [Ruby
SDK](https://github.com/scottganyo/usergrid_iron).

    mgmt = Usergrid::Management.new 'https://api.usergrid.com/'
    mgmt['authorize'].get params: { response_type: 'token', client_id: 'b0U5N81ME96NSeG78xIxQFxelQ'}

The example assumes use of the [Node.js
module](https://github.com/apigee/usergrid-node-module).

    var options = {
        method:'GET',
        endpoint:'management/authorize',
        qs:{response_type:'token', client_id:'b0U5N81ME96NSeG78xIxQFxelQ'}
    };
    client.request(options, function (err, data) {
        if (err) {
            //error — GET failed
        } else {
            //success — data will contain raw results from API call       
        }
    });

### Example - Response

    <!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
    <html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
        <title>Sign In</title>
        <link rel="stylesheet" type="text/css" href="../css/styles.css" />
    </head>
    <body>

        <div class="dialog-area">
            
            <form class="dialog-form" action="" method="post">
                <input type="hidden" name="response_type" value="token">
                <input type="hidden" name="client_id" value="b3U6M90FY80MEeG89xIxPRxEkQ">
                <input type="hidden" name="redirect_uri" value="">
                <input type="hidden" name="scope" value="">
                <input type="hidden" name="state" value="">
                <fieldset>
                    <p>
                        <label for="username">Username</label>
                    </p>
                    <p>
                        <input class="text_field" id="username" name="username" type="text" />
                    </p>
                    <p>
                        <label for="password">Password</label>
                    </p>
                    <p>
                        <input class="text_field" id="password" name="password" type="password" />
                    </p>
                    <p class="buttons">
                        <button type="submit">Submit</button>
                    </p>
                </fieldset>
            </form>
        </div>

 
