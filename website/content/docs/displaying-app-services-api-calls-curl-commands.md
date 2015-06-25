---
title: Displaying Apache Usergrid API calls as Curl commands  
category: docs
layout: docs
---

Displaying Apache Usergrid API calls as Curl commands
==================================================

 

If you use a web application, such as the JavaScript (HTML 5) version of
Messagee (see [HTML 5 example - Messagee](/html-5-example-messagee)),
you can easily see the equivalent Curl syntax for each API call that the
web application makes. The calls are displayed in the console area of
any of the following browsers:  Chrome, Internet Explorer (in the
debugger), Firefox (in Firebug), and Safari.

This is possible because web applications such as the JavaScript version
of Message takes advantage of a JavaScript SDK provied by Apigee (see
[HTML5/JavaScript SDK](/html5javascript-sdk)) to issue Apache Usergrid API
calls. The SDK automatically translates each API call into a Curl
command, which is displayed in the console. If you use any web
application that is built on the JavaScript SDK, you can view the
applications API's calls in Curl syntax.

The [admin portal](http://apigee.com/usergrid/) is another example of a
web application that is built on the JavaScript SDK and issues App
Services API calls from JavaScript. When a user clicks a button in the
admin portal, such as “Users”, the admin portal makes an API request to
retrieve the appropriate data. In addition, the JavaScript SDK in the
web application automatically translates the API call into the following
Curl command:

    curl -X GET "https://api.usergrid.com/edort1/sandbox/users?ql=order%20by%20username”

The request retrieves the users in the application and orders the result
by username.

If you turn on the JavaScript console, here’s what it displays for the
call. This example shows the JavaScript console display in the Chrome
browser. You can turn on the JavaScript console in Chrome by clicking
the “wrench” button (customize and control Chrome) and then selecting
Tools \> JavaScript console. 

![Curl command in Chrome
console](/docs/sites/docs/files/jsconsole_chrome.png)

Notice that in addition to displaying the API call in Curl syntax, the
console displays the time to retrieve the user entities.

To display the JavaScript console in Internet Explorer, you need to turn
on the debugger. You do that by selecting F12 developer tools in the
tools menu. Here’s what the Curl version of the API call looks like in
Internet Explorer’s console.

![Curl command in Internet Explorer
console](/docs/sites/docs/files/jsconsole_ie.png)

Here’s what the curl version of the API call looks like in Firefox’s
JavaScript console.

![Curl command in Firefox
console](/docs/sites/docs/files/jsconsole_firefox.png)

You need to have the [Firebug tool](http://getfirebug.com/) installed
and enabled in your Firefox browser to view the curl commands in the
console.

And here’s what the API call looks like in the Safari console. The
console is part of Safari’s Web Inspector tool
([https://developer.apple.com/technologies/safari/developer-tools.html](https://developer.apple.com/technologies/safari/developer-tools.html)).

![Curl command in SaFARI](/docs/sites/docs/files/jsconsole_safari.png)
