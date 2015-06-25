---
title: Messagee Example
category: docs
layout: docs
---

Messagee Example
================

*Messagee* is a simple Twitter-style messaging application that
leverages the extensive functionality of App services. This section
describes some of the features of Messagee.

There are three client versions of Messagee:

-   An [iOS client](#iOS_client)
-   An [Android client](#android_client)
-   An [HTML5 client](#html5_client)

The sections below describe how to create a new app, enter some test
users, and run the app. You also learn how to use the App services admin
portal, a user interface that streamlines data and application
management in the App services system. The portal is also a reference
application that shows how to incorporate App services APIs with
JavaScript. For a more detailed discussion of the portal's
functionality, see [Admin portal](/admin-portal).

Creating a user account in App services
---------------------------------------

Go to [https://apigee.com/usergrid/](https://apigee.com/usergrid/) to
access the login screen for the App services admin portal. If you are
new to App services, sign up for an account to access the portal,
specifying an organization (e.g., the name of your company or project
team) and a username and password that you can use to authenticate.
Because App services are designed for use by development teams, the same
username can be associated with one or more organizations.

If you create a new account, you receive a confirmation email that
contains a URL that you must click to activate the account. After this,
simply log in to the portal with your username and password.

![](/docs/sites/docs/files/styles/large/public/login.png?itok=ws5DhLDI)

Creating an app and users
-------------------------

When you have logged in, you need to create a new application.

![](/docs/sites/docs/files/styles/large/public/portal-1.png?itok=4o63Ia-y)

1.  2.  Enter a unique application name. The name must be unique to
    avoid a conflict with another user running the same application.

    ![](/docs/sites/docs/files/styles/large/public/portal-2.png?itok=yz8QznI7)

3.  4.  Make sure that the portal shows the application name you entered
    as the active application beneath the Applications menu **(1)**.

    If the correct name is not displayed, click the menu and select your
    application.

    Next, you need to populate a test user *user-test-1* that is going
    to log in to your copy of the server-side Messagee app.

5.  6.  Click the Users box on the left side of the console **(2)**.

    ![](/docs/sites/docs/files/styles/large/public/portal-3.png?itok=ncRlqPGQ)

7.  8.  Click the Add button and enter the user information for your
    application in the pop-up window (be sure to create a password you
    can remember), and then click the Create button. Repeat these steps
    to create a second username *test-user-2*.

    ![](/docs/sites/docs/files/styles/large/public/portal-4.png?itok=X_kKe5Ke)

9.  

When you have finished all these steps, you have a new application and
two new users.

![](/docs/sites/docs/files/styles/large/public/portal-5.png?itok=jSmAcwc3)

Now that you have created a uniquely named copy of the Messagee
application as well as two user accounts (*test-user-1* and
*test-user-2*), you are ready to test out the Messagee app. Use
test-user-1 to log in to the app, and test-user-2 as the user to follow.

To continue with the example, follow the instructions for a client app
([iOS client](#iOS_client), [Android client](#android_client), or [HTML5
client](#html5_client)).

iOS client
----------

Messagee is available as an iPhone app that uses Apache Usergrid and
RestKit. The source for the iOS version of Messagee is available in the
/samples/messagee directory of the [Apache Usergrid iOS SDK](#ios_sdk)

Here are the steps to run the Messagee app on iOS:

1.  2.  Run Steps 1-6 under [Creating an app and users](#app_user).

    These steps create a unique instance of the Messagee app on the
    server and two test users, *test-user-1* and *test-user-2*.

3.  4.  Access the Messagee server app by typing a URL similar to the
    following into your iOS mobile client (replace \<Messagee\> with the
    unique name of your application):

    [https://api.usergrid.com](https://api.usergrid.com)/\<Messagee\>/index.html

    ![](/docs/sites/docs/files/styles/large/public/iOS-1-chooseapp.jpg?itok=NSQcdJrj)

5.  6.  On the mobile client, complete the registration information and
    click Register.

    ![](/docs/sites/docs/files/styles/large/public/iOS-2-register.jpg?itok=Xzqe7mXV)

7.  8.  Log in to the Messagee app by entering the *test-user-1* account
    information created previously, and click the Sign in button.

    ![](/docs/sites/docs/files/styles/large/public/iOS-4-signin.jpg?itok=0M9QYU7b)

    At this point, the message board is empty:

    ![](/docs/sites/docs/files/styles/large/public/iOS-5-emptyfeed.jpg?itok=EWcUVMQN)

9.  10. To post a message using the app, click the top-right icon in the
    message board to create a message, write the message text, and send
    it by clicking Post.

    ![](/docs/sites/docs/files/styles/large/public/iOS-6-newmsg.jpg?itok=Wj16l_5V)

    **Note:** The posted message should appear in the message board
    within few seconds.

    ![](/docs/sites/docs/files/styles/large/public/iOS-7-feed1msg.jpg?itok=RclSnHuU)

    For your instance of the Messagee application to act like Twitter,
    you need to "follow" another user.

11. 12. In the message board, click Add People and add *test-user-2* as
    a person that *test-user-1* follows.

    ![](/docs/sites/docs/files/styles/large/public/iOS-9-follow.jpg?itok=yJeaYnEj)

13. 14. Log in as *test-user-2* and post a message as this user to the
    message board.
15. 16. Go back and log in as user *test-user-1*.

    Because *test-user-1* follows *test-user-2*, you should see the
    message sent by *test-user-2* in the message board.

    ![](/docs/sites/docs/files/styles/large/public/iOS-10-seeusermsg.jpg?itok=Wcd7avBh)

17. 

iOS SDK
-------

If you want to write iOS applications that connect to App services,
[download the Apache Usergrid iOS
SDK](http://www.apigee.com/docs/content/ios-sdk-redirect)

**Note:** The Messagee iOS application uses RestKit, a popular REST
framework for iOS, to communicate with App services. Because App
services use a REST API, you can use any REST framework to talk with the
service. However, the official App services iOS SDK provides a more
convenient communication mechanism that you should use unless you're
already using RestKit or some other framework in your project.

Android client and SDK
----------------------

Messagee is available as a sample Android app that acts as a App
services client. The source for the Android version is packaged with the
Apache Usergrid Android SDK in the /samples/messagee directory.

[Download the Apache Usergrid Android
SDK](http://www.apigee.com/docs/content/android-sdk-redirect).

Javascript/HTML5 client and SDK
-------------------------------

Messagee is also available as a sample HTML5 app that behaves in much
the same way as the two previous examples. The source for the HTML5
version is packaged with the Apache Usergrid Javascript/HTML5 SDK in the
/samples/messagee directory.

[Download the Apache Usergrid Javascript/HTML5
SDK](http://www.apigee.com/docs/content/javascript-sdk-redirect).
