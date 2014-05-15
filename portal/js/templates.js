angular.module('appservices').run(['$templateCache', function($templateCache) {
  'use strict';

  $templateCache.put('activities/activities.html',
    "<section class=\"row-fluid\">\n" +
    "  <div class=\"span12\">\n" +
    "    <div class=\"page-filters\">\n" +
    "      <h1 class=\"title\" class=\"pull-left\"><i class=\"pictogram title\">&#128241;</i> Activities</h1>\n" +
    "    </div>\n" +
    "  </div>\n" +
    "\n" +
    "</section>\n" +
    "<section class=\"row-fluid\">\n" +
    "  <div class=\"span12 tab-content\">\n" +
    "    <table class=\"table table-striped\">\n" +
    "      <tbody>\n" +
    "      <tr class=\"table-header\">\n" +
    "        <td>Date</td>\n" +
    "        <td></td>\n" +
    "        <td>User</td>\n" +
    "        <td>Content</td>\n" +
    "        <td>Verb</td>\n" +
    "        <td>UUID</td>\n" +
    "      </tr>\n" +
    "      <tr class=\"zebraRows\" ng-repeat=\"activity in activities\">\n" +
    "        <td>{{formatDate(activity.created)}}</td>\n" +
    "        <td class=\"gravatar20\"> <img ng-src=\"{{activity.actor.picture}}\"/>\n" +
    "        </td>\n" +
    "        <td>{{activity.actor.displayName}}</td>\n" +
    "        <td>{{activity.content}}</td>\n" +
    "        <td>{{activity.verb}}</td>\n" +
    "        <td>{{activity.uuid}}</td>\n" +
    "      </tr>\n" +
    "      </tbody>\n" +
    "    </table>\n" +
    "  </div>\n" +
    "</section>"
  );


  $templateCache.put('app-overview/app-overview.html',
    "<div class=\"app-overview-content\" >\n" +
    "  <section class=\"row-fluid\">\n" +
    "\n" +
    "      <page-title title=\" Summary\" icon=\"&#128241;\"></page-title>\n" +
    "  <section class=\"row-fluid\">\n" +
    "      <h2 class=\"title\" id=\"app-overview-title\">{{currentApp}}</h2>\n" +
    "  </section>\n" +
    "  <section class=\"row-fluid\">\n" +
    "\n" +
    "    <div class=\"span6\">\n" +
    "      <chart id=\"appOverview\"\n" +
    "             chartdata=\"appOverview.chart\"\n" +
    "             type=\"column\"></chart>\n" +
    "    </div>\n" +
    "\n" +
    "    <div class=\"span6\">\n" +
    "      <table class=\"table table-striped\">\n" +
    "        <tr class=\"table-header\">\n" +
    "          <td>Path</td>\n" +
    "          <td>Title</td>\n" +
    "        </tr>\n" +
    "        <tr class=\"zebraRows\" ng-repeat=\"(k,v) in collections\">\n" +
    "          <td>{{v.title}}</td>\n" +
    "          <td>{{v.count}}</td>\n" +
    "        </tr>\n" +
    "      </table>\n" +
    "    </div>\n" +
    "\n" +
    "  </section>\n" +
    "</div>"
  );


  $templateCache.put('app-overview/doc-includes/android.html',
    "<h2>1. Integrate the SDK into your project</h2>\n" +
    "<p>You can integrate Apigee features into your app by including the SDK in your project.&nbsp;&nbsp;You can do one of the following:</p>\n" +
    "\n" +
    "<ul class=\"nav nav-tabs\" id=\"myTab\">\n" +
    "\t<li class=\"active\"><a data-toggle=\"tab\" href=\"#existing_project\">Existing project</a></li>\n" +
    "\t<li><a data-toggle=\"tab\" href=\"#new_project\">New project</a></li>\n" +
    "</ul>\n" +
    "\n" +
    "<div class=\"tab-content\">\n" +
    "\t<div class=\"tab-pane active\" id=\"existing_project\">\n" +
    "\t\t<a class=\"jumplink\" name=\"add_the_sdk_to_an_existing_project\"></a>\n" +
    "\t\t<p>If you've already got&nbsp;an Android&nbsp;project, you can integrate the&nbsp;Apigee&nbsp;SDK into your project as you normally would:</p>\n" +
    "\t\t<div id=\"collapse\">\n" +
    "\t\t\t<a href=\"#jar_collapse\" class=\"btn\" data-toggle=\"collapse\"><i class=\"icon-white icon-chevron-down\"></i> Details</a>\t\t\t\n" +
    "\t\t</div>\n" +
    "\t\t<div id=\"jar_collapse\" class=\"collapse\">\n" +
    "\t\t\t<p>Add <code>apigee-android-&lt;version&gt;.jar</code> to your class path by doing the following:</p>\n" +
    "\t\n" +
    "\t\t\t<h3>Android 4.0 (or later) projects</h3>\n" +
    "\t\t\t<p>Copy the jar file into the <code>/libs</code> folder in your project.</p>\n" +
    "\t\t\t\n" +
    "\t\t\t<h3>Android 3.0 (or earlier) projects</h3>\n" +
    "\t\t\t<ol>\n" +
    "\t\t\t\t<li>In the&nbsp;Eclipse <strong>Package Explorer</strong>, select your application's project folder.</li>\n" +
    "\t\t\t\t<li>Click the&nbsp;<strong>File &gt; Properties</strong>&nbsp;menu.</li>\n" +
    "\t\t\t\t<li>In the <strong>Java Build Path</strong> section, click the <strong>Libraries</strong> tab, click <strong>Add External JARs</strong>.</li>\n" +
    "\t\t\t\t<li>Browse to <code>apigee-android-&lt;version&gt;.jar</code>, then click&nbsp;<strong>Open</strong>.</li>\n" +
    "\t\t\t\t<li>Order the <code>apigee-android-&lt;version&gt;.jar</code> at the top of the class path:\n" +
    "\t\t\t\t\t<ol>\n" +
    "\t\t\t\t\t\t<li>In the Eclipse <strong>Package Explorer</strong>, select your application's project folder.</li>\n" +
    "\t\t\t\t\t\t<li>Click the&nbsp;<strong>File &gt; Properties</strong> menu.</li>\n" +
    "\t\t\t\t\t\t<li>In the properties dialog, in the&nbsp;<strong>Java Build Path</strong> section,&nbsp;click&nbsp;the <strong>Order and Export</strong>&nbsp;tab.</li>\n" +
    "\t\t\t\t\t\t<li>\n" +
    "\t\t\t\t\t\t\t<p><strong>IMPORTANT:</strong> Select the checkbox for <code>apigee-android-&lt;version&gt;.jar</code>, then click the <strong>Top</strong>&nbsp;button.</p>\n" +
    "\t\t\t\t\t\t</li>\n" +
    "\t\t\t\t\t</ol>\n" +
    "\t\t\t\t</li>\n" +
    "\t\t\t</ol>\n" +
    "\t\t\t<div class=\"warning\">\n" +
    "\t\t\t\t<h3>Applications using Ant</h3>\n" +
    "\t\t\t\t<p>If you are using Ant to build your application, you must also copy <code>apigee-android-&lt;version&gt;.jar</code> to the <code>/libs</code> folder in your application.</p>\n" +
    "\t\t\t</div>\n" +
    "\t\t</div>\n" +
    "\t</div>\n" +
    "\t<div class=\"tab-pane\" id=\"new_project\">\n" +
    "\t\t<a class=\"jumplink\" name=\"create_a_new_project_based_on_the_SDK\"></a>\n" +
    "\t\t<p>If you don't have a&nbsp;project yet, you can begin by using the project template included with the SDK. The template includes support for SDK features.</p>\n" +
    "\t\t<ul>\n" +
    "\t\t\t<li>Locate the project template in the expanded SDK. It should be at the following location:\n" +
    "\t\t\t\t<pre>&lt;sdk_root&gt;/new-project-template</pre>\n" +
    "\t\t\t</li>\n" +
    "\t\t</ul>\n" +
    "\t</div>\n" +
    "</div>\n" +
    "<h2>2. Update permissions in AndroidManifest.xml</h2>\n" +
    "<p>Add the following Internet permissions to your application's <code>AndroidManifest.xml</code> file if they have not already been added. Note that with the exception of INTERNET, enabling all other permissions are optional.</p>\n" +
    "<pre>\n" +
    "&lt;uses-permission android:name=\"android.permission.INTERNET\" /&gt;\n" +
    "&lt;uses-permission android:name=\"android.permission.READ_PHONE_STATE\" /&gt;\n" +
    "&lt;uses-permission android:name=\"android.permission.ACCESS_NETWORK_STATE\" /&gt;\n" +
    "&lt;uses-permission android:name=\"android.permission.ACCESS_FINE_LOCATION\" /&gt;\n" +
    "&lt;uses-permission android:name=\"android.permission.ACCESS_COARSE_LOCATION\" /&gt;\n" +
    "</pre>\n" +
    "<h2>3. Initialize the SDK</h2>\n" +
    "<p>To initialize the App Services SDK, you must instantiate the <code>ApigeeClient</code> class. There are multiple ways to handle this step, but we recommend that you do the following:</p>\n" +
    "<ol>\n" +
    "\t<li>Subclass the <code>Application</code> class, and add an instance variable for the <code>ApigeeClient</code> to it, along with getter and setter methods.\n" +
    "\t\t<pre>\n" +
    "public class YourApplication extends Application\n" +
    "{\n" +
    "        \n" +
    "        private ApigeeClient apigeeClient;\n" +
    "        \n" +
    "        public YourApplication()\n" +
    "        {\n" +
    "                this.apigeeClient = null;\n" +
    "        }\n" +
    "        \n" +
    "        public ApigeeClient getApigeeClient()\n" +
    "        {\n" +
    "                return this.apigeeClient;\n" +
    "        }\n" +
    "        \n" +
    "        public void setApigeeClient(ApigeeClient apigeeClient)\n" +
    "        {\n" +
    "                this.apigeeClient = apigeeClient;\n" +
    "        }\n" +
    "}\t\t\t\n" +
    "\t\t</pre>\n" +
    "\t</li>\n" +
    "\t<li>Declare the <code>Application</code> subclass in your <code>AndroidManifest.xml</code>. For example:\n" +
    "\t\t<pre>\n" +
    "&lt;application&gt;\n" +
    "    android:allowBackup=\"true\"\n" +
    "    android:icon=\"@drawable/ic_launcher\"\n" +
    "    android:label=\"@string/app_name\"\n" +
    "    android:name=\".YourApplication\"\n" +
    "\t…\n" +
    "&lt;/application&gt;\t\t\t\n" +
    "\t\t</pre>\n" +
    "\t</li>\n" +
    "\t<li>Instantiate the <code>ApigeeClient</code> class in the <code>onCreate</code> method of your first <code>Activity</code> class:\n" +
    "\t\t<pre>\n" +
    "import com.apigee.sdk.ApigeeClient;\n" +
    "\n" +
    "@Override\n" +
    "protected void onCreate(Bundle savedInstanceState) {\n" +
    "    super.onCreate(savedInstanceState);\t\t\n" +
    "\t\n" +
    "\tString ORGNAME = \"{{currentOrg}}\";\n" +
    "\tString APPNAME = \"{{currentApp}}\";\n" +
    "\t\n" +
    "\tApigeeClient apigeeClient = new ApigeeClient(ORGNAME,APPNAME,this.getBaseContext());\n" +
    "\n" +
    "\t// hold onto the ApigeeClient instance in our application object.\n" +
    "\tYourApplication yourApp = (YourApplication) getApplication;\n" +
    "\tyourApp.setApigeeClient(apigeeClient);\t\t\t\n" +
    "}\n" +
    "\t\t</pre>\n" +
    "\t\t<p>This will make the instance of <code>ApigeeClient</code> available to your <code>Application</code> class.</p>\n" +
    "\t</li>\n" +
    "</ol>\n" +
    "<h2>4. Import additional SDK classes</h2>\n" +
    "<p>The following classes will enable you to call common SDK methods:</p>\n" +
    "<pre>\n" +
    "import com.apigee.sdk.data.client.DataClient; //App Services data methods\n" +
    "import com.apigee.sdk.apm.android.MonitoringClient; //App Monitoring methods\n" +
    "import com.apigee.sdk.data.client.callbacks.ApiResponseCallback; //API response handling\n" +
    "import com.apigee.sdk.data.client.response.ApiResponse; //API response object\n" +
    "</pre>\n" +
    "\t\t\n" +
    "<h2>5. Verify SDK installation</h2>\n" +
    "\n" +
    "<p>Once initialized, App Services will also automatically instantiate the <code>MonitoringClient</code> class and begin logging usage, crash and error metrics for your app.</p>\n" +
    "<p><img src=\"img/verify.png\" alt=\"screenshot of data in admin portal\"/></p>\n" +
    "<p>To verify that the SDK has been properly initialized, run your app, then go to 'Monitoring' > 'App Usage' in the <a href=\"https://www.apigee.com/usergrid\">App Services admin portal</a> to verify that data is being sent.</p>\n" +
    "<div class=\"warning\">It may take up to two minutes for data to appear in the admin portal after you run your app.</div>\n" +
    "\n" +
    "<h2>Installation complete! Try these next steps</h2>\n" +
    "<ul>\n" +
    "\t<li>\n" +
    "\t\t<h3><strong>Call additional SDK methods in your code</strong></h3>\n" +
    "\t\t<p>The <code>DataClient</code> and <code>MonitoringClient</code> classes are also automatically instantiated for you, and accessible with the following accessors:</p>\n" +
    "\t\t<ul>\n" +
    "\t\t\t<li>\n" +
    "\t\t\t\t<pre>DataClient dataClient = apigeeClient.getDataClient();</pre>\n" +
    "\t\t\t\t<p>Use this object to access the data methods of the App Services SDK, including those for push notifications, data store, and geolocation.</p>\n" +
    "\t\t\t</li>\n" +
    "\t\t\t<li>\n" +
    "\t\t\t\t<pre>MonitoringClient monitoringClient = apigeeClient.getMonitoringClient();</pre>\n" +
    "\t\t\t\t<p>Use this object to access the app configuration and monitoring methods of the App Services SDK, including advanced logging, and A/B testing.</p>\n" +
    "\t\t\t</li>\n" +
    "\t\t</ul>\n" +
    "\t</li>\t\n" +
    "\t<li>\t\n" +
    "\t\t<h3><strong>Add App Services features to your app</strong></h3>\n" +
    "\t\t<p>With App Services you can quickly add valuable features to your mobile or web app, including push notifications, a custom data store, geolocation and more. Check out these links to get started with a few of our most popular features:</p>\n" +
    "\t\t<ul>\n" +
    "\t\t\t<li><strong><a href=\"http://apigee.com/docs/node/8410\">Push notifications</a></strong>: Send offers, alerts and other messages directly to user devices to dramatically increase engagement. With App Services you can send 10 million push notification per month for free!</li>\n" +
    "\t\t\t<li><strong>App Monitoring</strong>: When you initialize the App Services SDK, a suite of valuable, <a href=\"http://apigee.com/docs/node/13190\">customizable</a> application monitoring features are automatically enabled that deliver the data you need to fine tune performance, analyze issues, and improve user experience.\n" +
    "\t\t\t\t<ul>\n" +
    "\t\t\t\t\t<li><strong><a href=\"http://apigee.com/docs/node/13176\">App Usage Monitoring</a></strong>: Visit the <a href=\"https://apigee.com/usergrid\">App Services admin portal</a> to view usage data for your app, including data on device models, platforms and OS versions running your app.</li>\t\t\t\t\n" +
    "\t\t\t\t\t<li><strong><a href=\"http://apigee.com/docs/node/12861\">API Performance Monitoring</a></strong>: Network performance is key to a solid user experience. In the <a href=\"https://apigee.com/usergrid\">App Services admin portal</a> you can view key metrics, including response time, number of requests and raw API request logs.</li>\t\n" +
    "\t\t\t\t\t<li><strong><a href=\"http://apigee.com/docs/node/13177\">Error &amp; Crash Monitoring</a></strong>: Get alerted to any errors or crashes, then view them in the <a href=\"https://apigee.com/usergrid\">App Services admin portal</a>, where you can also analyze raw error and crash logs.</li>\n" +
    "\t\t\t\t</ul>\t\t\n" +
    "\t\t\t</li>\n" +
    "\t\t\t<li><strong><a href=\"http://apigee.com/docs/node/410\">Geolocation</a></strong>: Target users or return result sets based on user location to keep your app highly-relevant.</li>\n" +
    "\t\t\t<li><strong><a href=\"http://apigee.com/docs/node/10152\">Data storage</a></strong>: Store all your application data on our high-availability infrastructure, and never worry about dealing with a database ever again.</li>\n" +
    "\t\t\t<li><strong><a href=\"http://apigee.com/docs/node/376\">User management and authentication</a></strong>: Every app needs users. Use App Services to easily implement user registration, as well as OAuth 2.0-compliant login and authentication.</li>\n" +
    "\t\t</ul>\n" +
    "\t</li>\n" +
    "\t<li>\t\n" +
    "\t\t<h3><strong>Check out the sample apps</strong></h3>\n" +
    "\t\t<p>The SDK includes samples that illustrate Apigee&nbsp;features. You'll find the samples in the following location in your SDK download:</p>\n" +
    "\t\t<pre>\n" +
    "apigee-android-sdk-&lt;version&gt;\n" +
    "\t...\n" +
    "\t/samples\n" +
    "\t\t</pre>\n" +
    "\t\t<div id=\"collapse\">\n" +
    "\t\t\t<a href=\"#samples_collapse\" class=\"btn\" data-toggle=\"collapse\"><i class=\"icon-white icon-chevron-down\"></i> Details</a>\n" +
    "\t\t</div>\n" +
    "\t\t<div id=\"samples_collapse\" class=\"collapse\">\n" +
    "\t\t\t<p>The samples include the following:</p>\n" +
    "\t\t\t<table class=\"table\">\n" +
    "\t\t\t\t<thead>\n" +
    "\t\t\t\t\t<tr>\n" +
    "\t\t\t\t\t\t<th scope=\"col\">Sample</th>\n" +
    "\t\t\t\t\t\t<th scope=\"col\">Description</th>\n" +
    "\t\t\t\t\t</tr>\n" +
    "\t\t\t\t</thead>\n" +
    "\t\t\t\t<tbody>\n" +
    "\t\t\t\t\t<tr>\n" +
    "\t\t\t\t\t\t<td>books</td>\n" +
    "\t\t\t\t\t\t<td>An app for storing a list of books that shows Apigee database operations such as reading, creating, and deleting.</td>\n" +
    "\t\t\t\t\t</tr>\n" +
    "\t\t\t\t\t<tr>\n" +
    "\t\t\t\t\t\t<td>messagee</td>\n" +
    "\t\t\t\t\t\t<td>An app for sending and receiving messages that shows Apigee database operations (reading, creating).</td>\n" +
    "\t\t\t\t\t</tr>\n" +
    "\t\t\t\t\t<tr>\n" +
    "\t\t\t\t\t\t<td>push</td>\n" +
    "\t\t\t\t\t\t<td>An app that uses the push feature to send notifications to the devices of users who have subscribed for them.</td>\n" +
    "\t\t\t\t\t</tr>\n" +
    "\t\t\t\t</tbody>\n" +
    "\t\t\t</table>\n" +
    "\t\t</div>\n" +
    "\t</li>\n" +
    "</ul>\n"
  );


  $templateCache.put('app-overview/doc-includes/ios.html',
    "<h2>1. Integrate ApigeeiOSSDK.framework</h2>\n" +
    "<a class=\"jumplink\" name=\"add_the_sdk_to_an_existing_project\"></a>\n" +
    "<ul class=\"nav nav-tabs\" id=\"myTab\">\n" +
    "\t<li class=\"active\"><a data-toggle=\"tab\" href=\"#existing_project\">Existing project</a></li>\n" +
    "\t<li><a data-toggle=\"tab\" href=\"#new_project\">New project</a></li>\n" +
    "</ul>\n" +
    "<div class=\"tab-content\">\n" +
    "\t<div class=\"tab-pane active\" id=\"existing_project\">\n" +
    "\t\t<p>If you've already got&nbsp;an Xcode iOS project, add it into your project as you normally would.</p>\n" +
    "\t\t<div id=\"collapse\"><a class=\"btn\" data-toggle=\"collapse\" href=\"#framework_collapse\">Details</a></div>\n" +
    "\t\t<div class=\"collapse\" id=\"framework_collapse\">\n" +
    "\t\t\t<ol>\n" +
    "\t\t\t\t<li>\n" +
    "\t\t\t\t\t<p>Locate the SDK framework file so you can add it to your project. For example, you'll find the file at the following path:</p>\n" +
    "\t\t\t\t\t<pre>\n" +
    "&lt;sdk_root&gt;/bin/ApigeeiOSSDK.framework</pre>\n" +
    "\t\t\t\t</li>\n" +
    "\t\t\t\t<li>In the <strong>Project Navigator</strong>, click on your project file, and then the <strong>Build Phases</strong> tab. Expand <strong>Link Binary With Libraries</strong>.</li>\n" +
    "\t\t\t\t<li>Link the Apigee iOS SDK into your project.\n" +
    "\t\t\t\t\t<ul>\n" +
    "\t\t\t\t\t\t<li>Drag ApigeeiOSSDK.framework into the Frameworks group created by Xcode.</li>\n" +
    "\t\t\t\t\t</ul>\n" +
    "\t\t\t\t\t<p>OR</p>\n" +
    "\t\t\t\t\t<ol>\n" +
    "\t\t\t\t\t\t<li>At the bottom of the <strong>Link Binary With Libraries</strong> group, click the <strong>+</strong> button. Then click&nbsp;<strong>Add Other</strong>.</li>\n" +
    "\t\t\t\t\t\t<li>Navigate to the directory that contains ApigeeiOSSDK.framework, and choose the ApigeeiOSSDK.framework folder.</li>\n" +
    "\t\t\t\t\t</ol>\n" +
    "\t\t\t\t</li>\n" +
    "\t\t\t</ol>\n" +
    "\t\t</div>\n" +
    "\t</div>\n" +
    "\t<div class=\"tab-pane\" id=\"new_project\"><a class=\"jumplink\" name=\"create_a_new_project_based_on_the_SDK\"></a>\n" +
    "\t\t<p>If you're starting with a clean slate (you don't have a&nbsp;project yet), you can begin by using the project template included with the SDK. The template includes support for SDK features.</p>\n" +
    "\t\t<ol>\n" +
    "\t\t\t<li>\n" +
    "\t\t\t\t<p>Locate the project template in the expanded SDK. It should be at the following location:</p>\n" +
    "\t\t\t\t<pre>\n" +
    "&lt;sdk_root&gt;/new-project-template</pre>\n" +
    "\t\t\t</li>\n" +
    "\t\t\t<li>In the project template directory, open the project file:&nbsp;Apigee App Services iOS Template.xcodeproj.</li>\n" +
    "\t\t\t<li>Get acquainted with the template by looking at its readme file.</li>\n" +
    "\t\t</ol>\n" +
    "\t</div>\n" +
    "</div>\n" +
    "<h2>2. Add required iOS frameworks</h2>\n" +
    "<p>Ensure that the following iOS frameworks are part of your project. To add them, under the <strong>Link Binary With Libraries</strong> group, click the <strong>+</strong> button, type the name of the framework you want to add, select the framework found by Xcode, then click <strong>Add</strong>.</p>\n" +
    "<ul>\n" +
    "\t<li>QuartzCore.framework</li>\n" +
    "\t<li>CoreLocation.framework</li>\n" +
    "\t<li>CoreTelephony.framework&nbsp;</li>\n" +
    "\t<li>Security.framework</li>\n" +
    "\t<li>SystemConfiguration.framework</li>\n" +
    "\t<li>UIKit.framework</li>\n" +
    "</ul>\n" +
    "<h2>3. Update 'Other Linker Flags'</h2>\n" +
    "<p>In the <strong>Build Settings</strong> panel, add the following under <strong>Other Linker Flags</strong>:</p>\n" +
    "<pre>\n" +
    "-ObjC -all_load</pre>\n" +
    "<p>Confirm that flags are set for both <strong>DEBUG</strong> and <strong>RELEASE</strong>.</p>\n" +
    "<h2>4. Initialize the SDK</h2>\n" +
    "<p>The <em>ApigeeClient</em> class initializes the App Services SDK. To do this you will need your organization name and application name, which are available in the <em>Getting Started</em> tab of the <a href=\"https://www.apigee.com/usergrid/\">App Service admin portal</a>, under <strong>Mobile SDK Keys</strong>.</p>\n" +
    "<ol>\n" +
    "\t<li>Import the SDK\n" +
    "\t\t<p>Add the following to your source code to import the SDK:</p>\n" +
    "\t\t<pre>\n" +
    "#import &lt;ApigeeiOSSDK/Apigee.h&gt;</pre>\n" +
    "\t</li>\n" +
    "\t<li>\n" +
    "\t\t<p>Declare the following properties in <code>AppDelegate.h</code>:</p>\n" +
    "\t\t<pre>\n" +
    "@property (strong, nonatomic) ApigeeClient *apigeeClient; \n" +
    "@property (strong, nonatomic) ApigeeMonitoringClient *monitoringClient;\n" +
    "@property (strong, nonatomic) ApigeeDataClient *dataClient;\t\n" +
    "\t\t</pre>\n" +
    "\t</li>\n" +
    "\t<li>\n" +
    "\t\t<p>Instantiate the <code>ApigeeClient</code> class inside the \u0010<code>didFinishLaunching</code> method of <code>AppDelegate.m</code>:</p>\n" +
    "\t\t<pre>\n" +
    "//Replace 'AppDelegate' with the name of your app delegate class to instantiate it\n" +
    "AppDelegate *appDelegate = (AppDelegate *)[[UIApplication sharedApplication] delegate];\n" +
    "\n" +
    "//Sepcify your App Services organization and application names\n" +
    "NSString *orgName = @\"{{currentOrg}}\";\n" +
    "NSString *appName = @\"{{currentApp}}\";\n" +
    "\n" +
    "//Instantiate ApigeeClient to initialize the SDK\n" +
    "appDelegate.apigeeClient = [[ApigeeClient alloc]\n" +
    "                            initWithOrganizationId:orgName\n" +
    "                            applicationId:appName];\n" +
    "                            \n" +
    "//Retrieve instances of ApigeeClient.monitoringClient and ApigeeClient.dataClient\n" +
    "self.monitoringClient = [appDelegate.apigeeClient monitoringClient]; \n" +
    "self.dataClient = [appDelegate.apigeeClient dataClient]; \n" +
    "\t\t</pre>\n" +
    "\t</li>\n" +
    "</ol>\n" +
    "\n" +
    "<h2>5. Verify SDK installation</h2>\n" +
    "\n" +
    "<p>Once initialized, App Services will also automatically instantiate the <code>ApigeeMonitoringClient</code> class and begin logging usage, crash and error metrics for your app.</p>\n" +
    "\n" +
    "<p>To verify that the SDK has been properly initialized, run your app, then go to <strong>'Monitoring' > 'App Usage'</strong> in the <a href=\"https://www.apigee.com/usergrid\">App Services admin portal</a> to verify that data is being sent.</p>\n" +
    "<p><img src=\"img/verify.png\" alt=\"screenshot of data in admin portal\"/></p>\n" +
    "<div class=\"warning\">It may take up to two minutes for data to appear in the admin portal after you run your app.</div>\n" +
    "\n" +
    "<h2>Installation complete! Try these next steps</h2>\n" +
    "<ul>\t\n" +
    "\t<li>\n" +
    "\t\t<h3><strong>Call additional SDK methods in your code</strong></h3>\n" +
    "\t\t<p>Create an instance of the AppDelegate class, then use <code>appDelegate.dataClient</code> or <code>appDelegate.monitoringClient</code> to call SDK methods:</p>\n" +
    "\t\t<div id=\"collapse\"><a class=\"btn\" data-toggle=\"collapse\" href=\"#client_collapse\">Details</a></div>\n" +
    "\t\t<div class=\"collapse\" id=\"client_collapse\">\n" +
    "\t\t\t<ul>\n" +
    "\t\t\t\t<li><code>appDelegate.dataClient</code>: Used to access the data methods of the App Services SDK, including those for push notifications, data store, and geolocation.</li>\n" +
    "\t\t\t\t<li><code>appDelegate.monitoringClient</code>: Used to access the app configuration and monitoring methods of the App Services SDK, including advanced logging, and A/B testing.</li>\n" +
    "\t\t\t</ul>\n" +
    "\t\t\t<h3>Example</h3>\n" +
    "\t\t\t<p>For example, you could create a new entity with the following:</p>\n" +
    "\t\t\t<pre>\n" +
    "AppDelegate *appDelegate = (AppDelegate *)[[UIApplication sharedApplication] delegate];\n" +
    "ApigeeClientResponse *response = [appDelegate.dataClient createEntity:entity];\n" +
    "\t\t\t</pre>\n" +
    "\t\t</div>\n" +
    "\n" +
    "\t</li>\n" +
    "\t<li>\n" +
    "\t\t<h3><strong>Add App Services features to your app</strong></h3>\n" +
    "\t\t<p>With App Services you can quickly add valuable features to your mobile or web app, including push notifications, a custom data store, geolocation and more. Check out these links to get started with a few of our most popular features:</p>\n" +
    "\t\t<ul>\n" +
    "\t\t\t<li><strong><a href=\"http://apigee.com/docs/node/8410\">Push notifications</a></strong>: Send offers, alerts and other messages directly to user devices to dramatically increase engagement. With App Services you can send 10 million push notification per month for free!</li>\n" +
    "\t\t\t<li><strong><a href=\"http://apigee.com/docs/node/410\">Geolocation</a></strong>: Target users or return result sets based on user location to keep your app highly-relevant.</li>\n" +
    "\t\t\t<li><strong><a href=\"http://apigee.com/docs/node/10152\">Data storage</a></strong>: Store all your application data on our high-availability infrastructure, and never worry about dealing with a database ever again.</li>\n" +
    "\t\t\t<li><strong><a href=\"http://apigee.com/docs/node/376\">User management and authentication</a></strong>: Every app needs users. Use App Services to easily implement user registration, as well as OAuth 2.0-compliant login and authentication.</li>\n" +
    "\t\t</ul>\n" +
    "\t</li>\n" +
    "\t<li>\n" +
    "\t\t<h3><strong>Check out the sample apps</strong></h3>\n" +
    "\t\t<p>The SDK includes samples that illustrate Apigee&nbsp;features. To look at them, open the .xcodeproj file for each in Xcode. To get a sample app running, open its project file, then follow the steps described in the section, <a target=\"_blank\" href=\"http://apigee.com/docs/app-services/content/installing-apigee-sdk-ios\">Add the SDK to an existing project</a>.</p>\n" +
    "\t\t<p>You'll find the samples in the following location in your SDK download:</p>\n" +
    "\t\t<pre>\n" +
    "apigee-ios-sdk-&lt;version&gt;\n" +
    "    ...\n" +
    "    /samples\n" +
    "\t\t</pre>\n" +
    "\t\t<div id=\"collapse\"><a class=\"btn\" data-toggle=\"collapse\" href=\"#samples_collapse\">Details</a></div>\n" +
    "\t\t<div class=\"collapse\" id=\"samples_collapse\">\n" +
    "\t\t\t<p>The samples include the following:</p>\n" +
    "\t\t\t<table class=\"table\">\n" +
    "\t\t\t\t<thead>\n" +
    "\t\t\t\t\t<tr>\n" +
    "\t\t\t\t\t\t<th scope=\"col\">Sample</th>\n" +
    "\t\t\t\t\t\t<th scope=\"col\">Description</th>\n" +
    "\t\t\t\t\t</tr>\n" +
    "\t\t\t\t</thead>\n" +
    "\t\t\t\t<tbody>\n" +
    "\t\t\t\t\t<tr>\n" +
    "\t\t\t\t\t\t<td>books</td>\n" +
    "\t\t\t\t\t\t<td>An app for storing a list of books that shows Apigee database operations such as reading, creating, and deleting.</td>\n" +
    "\t\t\t\t\t</tr>\n" +
    "\t\t\t\t\t<tr>\n" +
    "\t\t\t\t\t\t<td>messagee</td>\n" +
    "\t\t\t\t\t\t<td>An app for sending and receiving messages that shows Apigee database operations (reading, creating).</td>\n" +
    "\t\t\t\t\t</tr>\n" +
    "\t\t\t\t\t<tr>\n" +
    "\t\t\t\t\t\t<td>push</td>\n" +
    "\t\t\t\t\t\t<td>An app that uses the push feature to send notifications to the devices of users who have subscribed for them.</td>\n" +
    "\t\t\t\t\t</tr>\n" +
    "\t\t\t\t</tbody>\n" +
    "\t\t\t</table>\n" +
    "\t\t</div>\n" +
    "\t\t<p>&nbsp;</p>\n" +
    "\t</li>\n" +
    "</ul>\n"
  );


  $templateCache.put('app-overview/doc-includes/javascript.html',
    "<h2>1. Import the SDK into your HTML</h2>\n" +
    "<p>To enable support for Apigee-related functions in your HTML, you'll need to&nbsp;include <code>apigee.js</code> in your app. To do this, add the following to the <code>head</code> block of your HTML:</p>\n" +
    "<pre>\n" +
    "&lt;script type=\"text/javascript\" src=\"path/to/js/sdk/apigee.js\"&gt;&lt;/script&gt;\n" +
    "</pre>\n" +
    "<h2>2. Instantiate Apigee.Client</h2>\n" +
    "<p>Apigee.Client initializes the App Services SDK, and gives you access to all of the App Services SDK methods.</p>\n" +
    "<p>You will need to pass a JSON object with the UUID or name for your App Services organization and application when you instantiate it.</p>\n" +
    "<pre>\n" +
    "//Apigee account credentials, available in the App Services admin portal \n" +
    "var client_creds = {\n" +
    "        orgName:'{{currentOrg}}',\n" +
    "        appName:'{{currentApp}}'\n" +
    "    }\n" +
    "\n" +
    "//Initializes the SDK. Also instantiates Apigee.MonitoringClient\n" +
    "var dataClient = new Apigee.Client(client_creds);  \n" +
    "</pre>\n" +
    "\n" +
    "<h2>3. Verify SDK installation</h2>\n" +
    "\n" +
    "<p>Once initialized, App Services will also automatically instantiate <code>Apigee.MonitoringClient</code> and begin logging usage, crash and error metrics for your app.</p>\n" +
    "\n" +
    "<p>To verify that the SDK has been properly initialized, run your app, then go to <strong>'Monitoring' > 'App Usage'</strong> in the <a href=\"https://www.apigee.com/usergrid\">App Services admin portal</a> to verify that data is being sent.</p>\n" +
    "<p><img src=\"img/verify.png\" alt=\"screenshot of data in admin portal\"/></p>\n" +
    "<div class=\"warning\">It may take up to two minutes for data to appear in the admin portal after you run your app.</div>\n" +
    "\n" +
    "<h2>Installation complete! Try these next steps</h2>\n" +
    "<ul>\n" +
    "\t<li>\t\n" +
    "\t\t<h3><strong>Call additional SDK methods in your code</strong></h3>\n" +
    "\t\t<p>Use <code>dataClient</code> or <code>dataClient.monitor</code> to call SDK methods:</p>\n" +
    "\t\t<div id=\"collapse\">\n" +
    "\t\t\t<a href=\"#client_collapse\" class=\"btn\" data-toggle=\"collapse\"><i class=\"icon-white icon-chevron-down\"></i> Details</a>\n" +
    "\t\t</div>\n" +
    "\t\t<div id=\"client_collapse\" class=\"collapse\">\n" +
    "\t\t\t<ul>\n" +
    "\t\t\t\t<li><code>dataClient</code>: Used to access the data methods of the App Services SDK, including those for push notifications, data store, and geolocation.</li>\n" +
    "\t\t\t\t<li><code>dataClient.monitor</code>: Used to access the app configuration and monitoring methods of the App Services SDK, including advanced logging, and A/B testing.</li>\n" +
    "\t\t\t</ul>\n" +
    "\t\t</div>\n" +
    "\t</li>\t\n" +
    "\t<li>\n" +
    "\t\t<h3><strong>Add App Services features to your app</strong></h3>\n" +
    "\t\t<p>With App Services you can quickly add valuable features to your mobile or web app, including push notifications, a custom data store, geolocation and more. Check out these links to get started with a few of our most popular features:</p>\n" +
    "\t\t<ul>\n" +
    "\t\t\t<li><strong><a href=\"http://apigee.com/docs/node/8410\">Push notifications</a></strong>: Send offers, alerts and other messages directly to user devices to dramatically increase engagement. With App Services you can send 10 million push notification per month for free!</li>\n" +
    "\t\t\t<li><strong><a href=\"http://apigee.com/docs/node/410\">Geolocation</a></strong>: Keep your app highly-relevant by targeting users or returning result sets based on user location.</li>\n" +
    "\t\t\t<li><strong><a href=\"http://apigee.com/docs/node/10152\">Data storage</a></strong>: Store all your application data on our high-availability infrastructure, and never worry about dealing with a database ever again.</li>\n" +
    "\t\t\t<li><strong><a href=\"http://apigee.com/docs/node/376\">User management and authentication</a></strong>: Every app needs users. Use App Services to easily implement registration, login and OAuth 2.0-compliant authentication.</li>\n" +
    "\t\t</ul>\n" +
    "\t</li>\n" +
    "\t<li>\n" +
    "\t\t<h3><strong>Check out the sample apps</strong></h3>\n" +
    "\t\t<p>The SDK includes samples that illustrate Apigee&nbsp;features. To look at them, open the .xcodeproj file for each in Xcode. You'll find the samples in the following location in your SDK download:</p>\n" +
    "\t\t<pre>\n" +
    "apigee-javascript-sdk-master\n" +
    "    ...\n" +
    "    /samples\t\t\n" +
    "\t\t</pre>\n" +
    "\t\t<div id=\"collapse\">\n" +
    "\t\t\t<a href=\"#samples_collapse\" class=\"btn\" data-toggle=\"collapse\"><i class=\"icon-white icon-chevron-down\"></i> Details</a>\n" +
    "\t\t</div>\n" +
    "\t\t<div id=\"samples_collapse\" class=\"collapse\">\n" +
    "\t\t\t<p>The samples include the following:</p>\n" +
    "\t\t\t<table class=\"table\">\n" +
    "\t\t\t\t<thead>\n" +
    "\t\t\t\t\t<tr>\n" +
    "\t\t\t\t\t\t<th scope=\"col\">Sample</th>\n" +
    "\t\t\t\t\t\t<th scope=\"col\">Description</th>\n" +
    "\t\t\t\t\t</tr>\n" +
    "\t\t\t\t</thead>\n" +
    "\t\t\t\t<tbody>\n" +
    "\t\t\t\t\t<tr>\n" +
    "\t\t\t\t\t\t<td>booksSample.html</td>\n" +
    "\t\t\t\t\t\t<td>An app for storing a list of books that shows Apigee database operations such as reading, creating, and deleting.</td>\n" +
    "\t\t\t\t\t</tr>\n" +
    "\t\t\t\t\t<tr>\n" +
    "\t\t\t\t\t\t<td>messagee</td>\n" +
    "\t\t\t\t\t\t<td>An app for sending and receiving messages that shows Apigee database operations (reading, creating).</td>\n" +
    "\t\t\t\t\t</tr>\n" +
    "\t\t\t\t\t<tr>\n" +
    "\t\t\t\t\t\t<td>monitoringSample.html</td>\n" +
    "\t\t\t\t\t\t<td>Shows basic configuration and initialization of the HTML5 app monitoring functionality. Works in browser, PhoneGap, Appcelerator, and Trigger.io.</td>\n" +
    "\t\t\t\t\t</tr>\n" +
    "\t\t\t\t\t<tr>\n" +
    "\t\t\t\t\t\t<td>readmeSample.html</td>\n" +
    "\t\t\t\t\t\t<td>A simple app for reading data from an Apigee database.</td>\n" +
    "\t\t\t\t\t</tr>\n" +
    "\t\t\t\t</tbody>\n" +
    "\t\t\t</table>\n" +
    "\t\t</div>\t\n" +
    "\t</li>\t\t\t\t\n" +
    "</ul>\n"
  );


  $templateCache.put('app-overview/doc-includes/net.html',
    ""
  );


  $templateCache.put('app-overview/doc-includes/node.html',
    ""
  );


  $templateCache.put('app-overview/doc-includes/ruby.html',
    ""
  );


  $templateCache.put('app-overview/getting-started.html',
    "<div class=\"setup-sdk-content\" >\n" +
    "\n" +
    "  <bsmodal id=\"regenerateCredentials\"\n" +
    "           title=\"Confirmation\"\n" +
    "           close=\"hideModal\"\n" +
    "           closelabel=\"Cancel\"\n" +
    "           extrabutton=\"regenerateCredentialsDialog\"\n" +
    "           extrabuttonlabel=\"Yes\"\n" +
    "           ng-cloak>\n" +
    "    Are you sure you want to regenerate the credentials?\n" +
    "  </bsmodal>\n" +
    "\n" +
    "    <page-title icon=\"&#128640;\" title=\"Getting Started\"></page-title>\n" +
    "\n" +
    "  <section class=\"row-fluid\">\n" +
    "\n" +
    "\n" +
    "\n" +
    "\n" +
    "    <div class=\"span8\">\n" +
    "\n" +
    "      <h2 class=\"title\">Install the SDK for app {{currentApp}}</h2>\n" +
    "      <p>Click on a platform icon below to view SDK installation instructions for that platform.</p>\n" +
    "      <ul class=\"inline unstyled\">\n" +
    "        <!--<li><a target=\"_blank\" href=\"http://apigee.com/docs/usergrid/content/sdks-and-examples#ios\"><i class=\"sdk-icon-large-ios\"></i></a></li>-->\n" +
    "        <!--<li><a target=\"_blank\" href=\"http://apigee.com/docs/usergrid/content/sdks-and-examples#android\"><i class=\"sdk-icon-large-android\"></i></a></li>-->\n" +
    "        <!--<li><a target=\"_blank\" href=\"http://apigee.com/docs/usergrid/content/sdks-and-examples#javascript\"><i class=\"sdk-icon-large-js\"></i></a></li>-->\n" +
    "\n" +
    "\n" +
    "        <li ng-click=\"showSDKDetail('ios')\"\n" +
    "            analytics-on=\"click\"\n" +
    "            analytics-label=\"App Services\"\n" +
    "            analytics-category=\"Getting Started\"\n" +
    "            analytics-event=\"iOS SDK\"><i class=\"sdk-icon-large-ios\"></i></li>\n" +
    "        <li ng-click=\"showSDKDetail('android')\"\n" +
    "            analytics-on=\"click\"\n" +
    "            analytics-label=\"App Services\"\n" +
    "            analytics-category=\"Getting Started\"\n" +
    "            analytics-event=\"Android SDK\"><i class=\"sdk-icon-large-android\"></i></li>\n" +
    "        <li ng-click=\"showSDKDetail('javascript')\"\n" +
    "            analytics-on=\"click\"\n" +
    "            analytics-label=\"App Services\"\n" +
    "            analytics-category=\"Getting Started\"\n" +
    "            analytics-event=\"JS SDK\"><i class=\"sdk-icon-large-js\"></i></li>\n" +
    "        <li><a target=\"_blank\"\n" +
    "               ng-click=\"showSDKDetail('nocontent')\"\n" +
    "               href=\"http://apigee.com/docs/usergrid/content/sdks-and-examples#nodejs\"\n" +
    "               analytics-on=\"click\"\n" +
    "               analytics-label=\"App Services\"\n" +
    "               analytics-category=\"Getting Started\"\n" +
    "               analytics-event=\"Node SDK\"><i class=\"sdk-icon-large-node\"></i></a></li>\n" +
    "        <li><a target=\"_blank\"\n" +
    "               ng-click=\"showSDKDetail('nocontent')\"\n" +
    "               href=\"http://apigee.com/docs/usergrid/content/sdks-and-examples#ruby\"\n" +
    "               analytics-on=\"click\"\n" +
    "               analytics-label=\"App Services\"\n" +
    "               analytics-category=\"Getting Started\"\n" +
    "               analytics-event=\"Ruby SDK\"><i class=\"sdk-icon-large-ruby\"></i></a></li>\n" +
    "        <li><a target=\"_blank\"\n" +
    "               ng-click=\"showSDKDetail('nocontent')\"\n" +
    "               href=\"http://apigee.com/docs/usergrid/content/sdks-and-examples#c\"\n" +
    "               analytics-on=\"click\"\n" +
    "               analytics-label=\"App Services\"\n" +
    "               analytics-category=\"Getting Started\"\n" +
    "               analytics-event=\"DotNet SDK\"><i class=\"sdk-icon-large-net\"></i></a></li>\n" +
    "       </ul>\n" +
    "\n" +
    "      <section id=\"intro-container\" class=\"row-fluid intro-container\">\n" +
    "\n" +
    "        <div class=\"sdk-intro\">\n" +
    "        </div>\n" +
    "\n" +
    "        <div class=\"sdk-intro-content\">\n" +
    "\n" +
    "          <a class=\"btn normal white pull-right\" ng-href=\"{{sdkLink}}\" target=\"_blank\">\n" +
    "            Download SDK\n" +
    "          </a>\n" +
    "          <a class=\"btn normal white pull-right\" ng-href=\"{{docsLink}}\" target=\"_blank\">\n" +
    "            More Docs\n" +
    "          </a>\n" +
    "          <h3 class=\"title\"><i class=\"pictogram\">&#128213;</i>{{contentTitle}}</h3>\n" +
    "\n" +
    "          <div ng-include=\"getIncludeURL()\"></div>\n" +
    "        </div>\n" +
    "\n" +
    "      </section>\n" +
    "    </div>\n" +
    "\n" +
    "    <div class=\"span4 keys-creds\">\n" +
    "      <h2 class=\"title\">Mobile sdk keys</h2>\n" +
    "      <p>For mobile SDK initialization.</p>\n" +
    "      <dl class=\"app-creds\">\n" +
    "        <dt>Org Name</dt>\n" +
    "        <dd>{{currentOrg}}</dd>\n" +
    "        <dt>App Name</dt>\n" +
    "        <dd>{{currentApp}}</dd>\n" +
    "      </dl>\n" +
    "      <h2 class=\"title\">Server app credentials</h2>\n" +
    "      <p>For authenticating from a server side app (i.e. Ruby, .NET, etc.)</p>\n" +
    "      <dl class=\"app-creds\">\n" +
    "        <dt>Client ID</dt>\n" +
    "        <dd>{{clientID}}</dd>\n" +
    "        <dt>Client Secret</dt>\n" +
    "        <dd>{{clientSecret}}</dd>\n" +
    "        <dt>\n" +
    "           &nbsp;\n" +
    "        </dt>\n" +
    "        <dd>&nbsp;</dd>\n" +
    "\n" +
    "        <dt>\n" +
    "          <a class=\"btn filter-selector\" ng-click=\"showModal('regenerateCredentials')\">Regenerate</a>\n" +
    "        </dt>\n" +
    "        <dd></dd>\n" +
    "      </dl>\n" +
    "\n" +
    "    </div>\n" +
    "\n" +
    "  </section>\n" +
    "</div>"
  );


  $templateCache.put('data/data.html',
    "<div class=\"content-page\">\n" +
    "\n" +
    "  <bsmodal id=\"newCollection\"\n" +
    "           title=\"Create new collection\"\n" +
    "           close=\"hideModal\"\n" +
    "           closelabel=\"Cancel\"\n" +
    "           extrabutton=\"newCollectionDialog\"\n" +
    "           extrabuttonlabel=\"Create\"\n" +
    "           buttonid=\"collection\"\n" +
    "           ng-cloak>\n" +
    "    <fieldset>\n" +
    "      <div class=\"control-group\">\n" +
    "        <label for=\"new-collection-name\">Collection Name:</label>\n" +
    "        <div class=\"controls\">\n" +
    "          <input type=\"text\" ug-validate required ng-pattern=\"collectionNameRegex\" ng-attr-title=\"{{collectionNameRegexDescription}}\" ng-model=\"$parent.newCollection.name\" name=\"collection\" id=\"new-collection-name\" class=\"input-xlarge\"/>\n" +
    "          <p class=\"help-block hide\"></p>\n" +
    "        </div>\n" +
    "      </div>\n" +
    "    </fieldset>\n" +
    "  </bsmodal>\n" +
    "\n" +
    "  <div id=\"intro-page\">    \n" +
    "    <page-title title=\" Collections\" icon=\"&#128254;\"></page-title>\n" +
    "  </div>\n" +
    "\n" +
    "  <section class=\"row-fluid\">\n" +
    "    <div id=\"intro-list\" class=\"span3 user-col\">\n" +
    "      <a class=\"btn btn-primary\" id=\"new-collection-link\" ng-click=\"showModal('newCollection')\">New Collection</a>\n" +
    "      <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('data new collection')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_new_collection}}\" tooltip-placement=\"right\">(?)</a>\n" +
    "        <ul  class=\"user-list\">\n" +
    "          <li ng-class=\"queryCollection._type === entity.name ? 'selected' : ''\" ng-repeat=\"entity in collectionList\" ng-click=\"loadCollection('/'+entity.name);\">\n" +
    "            <a id=\"collection-{{entity.name}}-link\" href=\"javaScript:void(0)\">/{{entity.name}} </a>\n" +
    "          </li>\n" +
    "        </ul>\n" +
    "\n" +
    "  </div>\n" +
    "\n" +
    "    <div class=\"span9 tab-content\">\n" +
    "      <div class=\"content-page\">\n" +
    "        <form id=\"intro-collection-query\" name=\"dataForm\" ng-submit=\"run();\">\n" +
    "        <fieldset>\n" +
    "          <div class=\"control-group\">\n" +
    "            <div class=\"\" data-toggle=\"buttons-radio\">\n" +
    "              <!--a class=\"btn\" id=\"button-query-back\">&#9664; Back</a-->\n" +
    "              <!--Added disabled class to change the way button looks but their functionality is as usual -->\n" +
    "              <label class=\"control-label\" style=\"display:none\"><strong>Method</strong> <a id=\"query-method-help\" href=\"#\" class=\"help-link\">get help</a></label>\n" +
    "              <input type=\"radio\" id=\"create-rb\" name=\"query-action\" style=\"margin-top: -2px;\" ng-click=\"selectPOST();\" ng-checked=\"verb=='POST'\"> CREATE &nbsp; &nbsp;\n" +
    "              <input type=\"radio\" id=\"read-rb\" name=\"query-action\" style=\"margin-top: -2px;\" ng-click=\"selectGET();\" ng-checked=\"verb=='GET'\"> READ &nbsp; &nbsp;\n" +
    "              <input type=\"radio\" id=\"update-rb\" name=\"query-action\" style=\"margin-top: -2px;\" ng-click=\"selectPUT();\" ng-checked=\"verb=='PUT'\"> UPDATE &nbsp; &nbsp;\n" +
    "                <input type=\"radio\" id=\"delete-rb\" name=\"query-action\" style=\"margin-top: -2px;\" ng-click=\"selectDELETE();\" ng-checked=\"verb=='DELETE'\"> DELETE             <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('data query verbs')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_verb_buttons}}\" tooltip-placement=\"right\">(?)</a>\n" +
    "            </div>\n" +
    "          </div>\n" +
    "\n" +
    "          <div class=\"control-group\">\n" +
    "            <strong>Path </strong>\n" +
    "            <div class=\"controls\">\n" +
    "              <input ng-model=\"data.queryPath\" type=\"text\" ug-validate id=\"pathDataQuery\" ng-attr-title=\"{{pathRegexDescription}}\" ng-pattern=\"pathRegex\" class=\"span6\" autocomplete=\"off\" placeholder=\"ex: /users\" required/>\n" +
    "              <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('data query path')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_path_box}}\" tooltip-placement=\"right\">(?)</a>\n" +
    "            </div>\n" +
    "          </div>\n" +
    "          <div class=\"control-group\">\n" +
    "            <a id=\"back-to-collection\" class=\"outside-link\" style=\"display:none\">Back to collection</a>\n" +
    "          </div>\n" +
    "          <div class=\"control-group\">\n" +
    "            <strong>Query</strong>\n" +
    "            <div class=\"controls\">\n" +
    "              <input ng-model=\"data.searchString\" type=\"text\" class=\"span6\" autocomplete=\"off\" placeholder=\"ex: select * where name='fred'\"/>\n" +
    "              <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('data query string')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_query_box}}\" tooltip-placement=\"right\">(?)</a>\n" +
    "              <div style=\"display:none\">\n" +
    "                <a class=\"btn dropdown-toggle \" data-toggle=\"dropdown\">\n" +
    "                  <span id=\"query-collections-caret\" class=\"caret\"></span>\n" +
    "                </a>\n" +
    "                <ul id=\"query-collections-indexes-list\" class=\"dropdown-menu \">\n" +
    "                </ul>\n" +
    "              </div>\n" +
    "            </div>\n" +
    "          </div>\n" +
    "\n" +
    "\n" +
    "          <div class=\"control-group\" ng-show=\"verb=='GET' || verb=='DELETE'\">\n" +
    "            <label class=\"control-label\" for=\"query-limit\"><strong>Limit</strong>\n" +
    "              <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('data limit')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_limit}}\" tooltip-placement=\"right\">(?)</a><a id=\"query-limit-help\" href=\"#\" ng-show=\"false\" class=\"help-link\">get help</a></label>\n" +
    "            <div class=\"controls\">\n" +
    "              <div class=\"input-append\">\n" +
    "                <input ng-model=\"data.queryLimit\" type=\"text\" class=\"span5\" id=\"query-limit\" placeholder=\"ex: 10\">\n" +
    "              </div>\n" +
    "            </div>\n" +
    "          </div>\n" +
    "\n" +
    "          <div class=\"control-group\" style=\"display:{{queryBodyDisplay}}\">\n" +
    "            <label class=\"control-label\" for=\"query-source\"><strong>JSON Body</strong>\n" +
    "              <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('data json body')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_json_body}}\" tooltip-placement=\"right\">(?)</a> <a id=\"query-json-help\" href=\"#\" ng-show=\"false\" class=\"help-link\">get help</a></label>\n" +
    "            <div class=\"controls\">\n" +
    "            <textarea ng-model=\"data.queryBody\" id=\"query-source\" class=\"span6 pull-left\" rows=\"4\">\n" +
    "      { \"name\":\"value\" }\n" +
    "            </textarea>\n" +
    "              <br>\n" +
    "            <a class=\"btn pull-left\" ng-click=\"validateJson();\">Validate JSON</a>\n" +
    "                <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('data validate json')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_json_validate}}\" tooltip-placement=\"right\">(?)</a>\n" +
    "            </div>\n" +
    "          </div>\n" +
    "          <div style=\"clear: both; height: 10px;\"></div>\n" +
    "          <div class=\"control-group\">\n" +
    "            <input type=\"submit\" ng-disabled=\"!dataForm.$valid || loading\" class=\"btn btn-primary\" id=\"button-query\"  value=\"{{loading ? loadingText : 'Run Query'}}\"/>\n" +
    "            <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('data run query')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_run_query}}\" tooltip-placement=\"right\">(?)</a>\n" +
    "          </div>\n" +
    "        </fieldset>\n" +
    "       </form>\n" +
    "        <div id=\"intro-entity-list\">\n" +
    "          <div ng-include=\"display=='generic' ? 'data/display-generic.html' : ''\"></div>\n" +
    "          <div ng-include=\"display=='users' ? 'data/display-users.html' : ''\"></div>\n" +
    "          <div ng-include=\"display=='groups' ? 'data/display-groups.html' : ''\"></div>\n" +
    "          <div ng-include=\"display=='roles' ? 'data/display-roles.html' : ''\"></div>\n" +
    "        </div>\n" +
    "\n" +
    "      </div>\n" +
    "\n" +
    "      </div>\n" +
    "    </section>\n" +
    "\n" +
    "</div>\n" +
    "\n"
  );


  $templateCache.put('data/display-generic.html',
    "\n" +
    "\n" +
    "<bsmodal id=\"deleteEntities\"\n" +
    "         title=\"Are you sure you want to delete the entities(s)?\"\n" +
    "         close=\"hideModal\"\n" +
    "         closelabel=\"Cancel\"\n" +
    "         extrabutton=\"deleteEntitiesDialog\"\n" +
    "         extrabuttonlabel=\"Delete\"\n" +
    "         buttonid=\"del-entity\"\n" +
    "         ng-cloak>\n" +
    "    <fieldset>\n" +
    "        <div class=\"control-group\">\n" +
    "        </div>\n" +
    "    </fieldset>\n" +
    "</bsmodal>\n" +
    "\n" +
    "<span class=\"button-strip\">\n" +
    "  <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('data entities list')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_entities_list}}\" tooltip-placement=\"left\">(?)</a>\n" +
    "  <button class=\"btn btn-primary\" ng-disabled=\"!valueSelected(queryCollection._list) || deleteLoading\" ng-click=\"deleteEntitiesDialog()\">{{deleteLoading ? loadingText : 'Delete Entity(s)'}}</button>\n" +
    "</span>\n" +
    "<table class=\"table table-striped collection-list\">\n" +
    "  <thead>\n" +
    "  <tr class=\"table-header\">\n" +
    "    <th><input type=\"checkbox\" ng-show=\"queryCollection._list.length > 0\" id=\"selectAllCheckbox\" ng-model=\"queryBoxesSelected\" ng-click=\"selectAllEntities(queryCollection._list,$parent,'queryBoxesSelected',true)\"></th>\n" +
    "    <th ng-if=\"hasProperty('name')\">Name</th>\n" +
    "    <th>UUID</th>\n" +
    "    <th></th>\n" +
    "  </tr>\n" +
    "  </thead>\n" +
    "  <tbody ng-repeat=\"entity in queryCollection._list\">\n" +
    "  <tr class=\"zebraRows\" >\n" +
    "    <td>\n" +
    "      <input\n" +
    "        type=\"checkbox\"\n" +
    "        id=\"entity-{{entity._data.name}}-cb\"\n" +
    "        ng-value=\"entity._data.uuid\"\n" +
    "        ng-model=\"entity.checked\"\n" +
    "        >\n" +
    "    </td>\n" +
    "    <td ng-if=\"hasProperty('name')\">{{entity._data.name}}</td>\n" +
    "    <td>{{entity._data.uuid}}</td>\n" +
    "    <td><a href=\"javaScript:void(0)\" ng-click=\"entitySelected[$index] = !entitySelected[$index]; selectEntity(entity._data.uuid, entitySelected[$index]);\">{{entitySelected[$index] ? 'Hide' : 'View'}} Details</a></td>\n" +
    "  </tr>\n" +
    "  <tr ng-if=\"entitySelected[$index]\">\n" +
    "    <td colspan=\"5\">\n" +
    "\n" +
    "\n" +
    "      <h4 style=\"margin: 0 0 20px 0\">Entity Detail</h4>\n" +
    "\n" +
    "\n" +
    "      <ul class=\"formatted-json\">\n" +
    "        <li ng-repeat=\"(k,v) in entity._data track by $index\">\n" +
    "          <span class=\"key\">{{k}} :</span>\n" +
    "          <!--todo - doing manual recursion to get this out the door for launch, please fix-->\n" +
    "          <span ng-switch on=\"isDeep(v)\">\n" +
    "            <ul ng-switch-when=\"true\">\n" +
    "              <li ng-repeat=\"(k2,v2) in v\"><span class=\"key\">{{k2}} :</span>\n" +
    "\n" +
    "                <span ng-switch on=\"isDeep(v2)\">\n" +
    "                  <ul ng-switch-when=\"true\">\n" +
    "                    <li ng-repeat=\"(k3,v3) in v2\"><span class=\"key\">{{k3}} :</span><span class=\"value\">{{v3}}</span></li>\n" +
    "                  </ul>\n" +
    "                  <span ng-switch-when=\"false\">\n" +
    "                    <span class=\"value\">{{v2}}</span>\n" +
    "                  </span>\n" +
    "                </span>\n" +
    "              </li>\n" +
    "            </ul>\n" +
    "            <span ng-switch-when=\"false\">\n" +
    "              <span class=\"value\">{{v}}</span>\n" +
    "            </span>\n" +
    "          </span>\n" +
    "        </li>\n" +
    "      </ul>\n" +
    "\n" +
    "    <div class=\"control-group\">\n" +
    "      <h4 style=\"margin: 20px 0 20px 0\">Edit Entity <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('data edit entity')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_edit_entity}}\" tooltip-placement=\"right\">(?)</a></h4>\n" +
    "      <div class=\"controls\">\n" +
    "        <textarea ng-model=\"entity._json\" class=\"span12\" rows=\"12\"></textarea>\n" +
    "        <br>\n" +
    "        <a class=\"btn btn-primary toolbar pull-left\" ng-click=\"validateJson();\">Validate JSON</a><button type=\"button\" class=\"btn btn-primary pull-right\" id=\"button-query\" ng-click=\"saveEntity(entity);\">Save</button>\n" +
    "      </div>\n" +
    "    </div>\n" +
    "  </td>\n" +
    "  </tr>\n" +
    "\n" +
    "  <tr ng-show=\"queryCollection._list.length == 0\">\n" +
    "    <td colspan=\"4\">No data found</td>\n" +
    "  </tr>\n" +
    "  </tbody>\n" +
    "</table>\n" +
    "<div style=\"padding: 10px 5px 10px 5px\">\n" +
    "  <button class=\"btn btn-primary toolbar\" ng-click=\"getPrevious()\" style=\"display:{{previous_display}}\">< Previous</button>\n" +
    "  <button class=\"btn btn-primary toolbar\" ng-click=\"getNext()\" style=\"display:{{next_display}}; float:right;\">Next ></button>\n" +
    "</div>\n" +
    "\n"
  );


  $templateCache.put('data/display-groups.html',
    ""
  );


  $templateCache.put('data/display-roles.html',
    "roles---------------------------------"
  );


  $templateCache.put('data/display-users.html',
    "\n" +
    "<table id=\"query-response-table\" class=\"table\">\n" +
    "  <tbody>\n" +
    "  <tr class=\"zebraRows users-row\">\n" +
    "    <td class=\"checkboxo\">\n" +
    "      <input type=\"checkbox\" onclick=\"Usergrid.console.selectAllEntities(this);\"></td>\n" +
    "    <td class=\"gravatar50-td\">&nbsp;</td>\n" +
    "    <td class=\"user-details bold-header\">Username</td>\n" +
    "    <td class=\"user-details bold-header\">Display Name</td>\n" +
    "    <td class=\"user-details bold-header\">UUID</td>\n" +
    "    <td class=\"view-details\">&nbsp;</td>\n" +
    "  </tr>\n" +
    "  <tr class=\"zebraRows users-row\">\n" +
    "    <td class=\"checkboxo\">\n" +
    "      <input class=\"listItem\" type=\"checkbox\" name=\"/users/8bb9a3fa-d508-11e2-875d-a59031a365e8/following/bf9a95da-d508-11e2-bf44-236d2eee13a7\" value=\"bf9a95da-d508-11e2-bf44-236d2eee13a7\">\n" +
    "    </td>\n" +
    "    <td class=\"gravatar50-td\">\n" +
    "      <img src=\"http://www.gravatar.com/avatar/01b37aa66496988ca780b3f515bc768e\" class=\"gravatar50\">\n" +
    "    </td>\n" +
    "    <td class=\"details\">\n" +
    "      <a onclick=\"Usergrid.console.getCollection('GET', '/users/8bb9a3fa-d508-11e2-875d-a59031a365e8/following/'+'bf9a95da-d508-11e2-bf44-236d2eee13a7'); $('#data-explorer').show(); return false;\" class=\"view-details\">10</a>\n" +
    "    </td>\n" +
    "    <td class=\"details\">      #\"&gt;&lt;img src=x onerror=prompt(1);&gt;   </td>\n" +
    "    <td class=\"details\">     bf9a95da-d508-11e2-bf44-236d2eee13a7   </td>\n" +
    "    <td class=\"view-details\">\n" +
    "      <a href=\"\" onclick=\"$('#query-row-bf9a95da-d508-11e2-bf44-236d2eee13a7').toggle(); $('#data-explorer').show(); return false;\" class=\"view-details\">Details</a>\n" +
    "    </td>\n" +
    "  </tr>\n" +
    "  <tr id=\"query-row-bf9a95da-d508-11e2-bf44-236d2eee13a7\" style=\"display:none\">\n" +
    "    <td colspan=\"5\">\n" +
    "      <div>\n" +
    "        <div style=\"padding-bottom: 10px;\">\n" +
    "          <button type=\"button\" class=\"btn btn-small query-button active\" id=\"button-query-show-row-JSON\" onclick=\"Usergrid.console.activateQueryRowJSONButton(); $('#query-row-JSON-bf9a95da-d508-11e2-bf44-236d2eee13a7').show(); $('#query-row-content-bf9a95da-d508-11e2-bf44-236d2eee13a7').hide(); return false;\">JSON</button>\n" +
    "          <button type=\"button\" class=\"btn btn-small query-button disabled\" id=\"button-query-show-row-content\" onclick=\"Usergrid.console.activateQueryRowContentButton();$('#query-row-content-bf9a95da-d508-11e2-bf44-236d2eee13a7').show(); $('#query-row-JSON-bf9a95da-d508-11e2-bf44-236d2eee13a7').hide(); return false;\">Content</button>\n" +
    "        </div>\n" +
    "        <div id=\"query-row-JSON-bf9a95da-d508-11e2-bf44-236d2eee13a7\">\n" +
    "              <pre>{\n" +
    "  \"picture\": \"http://www.gravatar.com/avatar/01b37aa66496988ca780b3f515bc768e\",\n" +
    "  \"uuid\": \"bf9a95da-d508-11e2-bf44-236d2eee13a7\",\n" +
    "  \"type\": \"user\",\n" +
    "  \"name\": \"#\"&gt;&lt;img src=x onerror=prompt(1);&gt;\",\n" +
    "  \"created\": 1371224432557,\n" +
    "  \"modified\": 1371851347024,\n" +
    "  \"username\": \"10\",\n" +
    "  \"email\": \"fdsafdsa@ookfd.com\",\n" +
    "  \"activated\": \"true\",\n" +
    "  \"adr\": {\n" +
    "    \"addr1\": \"\",\n" +
    "    \"addr2\": \"\",\n" +
    "    \"city\": \"\",\n" +
    "    \"state\": \"\",\n" +
    "    \"zip\": \"\",\n" +
    "    \"country\": \"\"\n" +
    "  },\n" +
    "  \"metadata\": {\n" +
    "    \"path\": \"/users/8bb9a3fa-d508-11e2-875d-a59031a365e8/following/bf9a95da-d508-11e2-bf44-236d2eee13a7\",\n" +
    "    \"sets\": {\n" +
    "      \"rolenames\": \"/users/8bb9a3fa-d508-11e2-875d-a59031a365e8/following/bf9a95da-d508-11e2-bf44-236d2eee13a7/rolenames\",\n" +
    "      \"permissions\": \"/users/8bb9a3fa-d508-11e2-875d-a59031a365e8/following/bf9a95da-d508-11e2-bf44-236d2eee13a7/permissions\"\n" +
    "    },\n" +
    "    \"collections\": {\n" +
    "      \"activities\": \"/users/8bb9a3fa-d508-11e2-875d-a59031a365e8/following/bf9a95da-d508-11e2-bf44-236d2eee13a7/activities\",\n" +
    "      \"devices\": \"/users/8bb9a3fa-d508-11e2-875d-a59031a365e8/following/bf9a95da-d508-11e2-bf44-236d2eee13a7/devices\",\n" +
    "      \"feed\": \"/users/8bb9a3fa-d508-11e2-875d-a59031a365e8/following/bf9a95da-d508-11e2-bf44-236d2eee13a7/feed\",\n" +
    "      \"groups\": \"/users/8bb9a3fa-d508-11e2-875d-a59031a365e8/following/bf9a95da-d508-11e2-bf44-236d2eee13a7/groups\",\n" +
    "      \"roles\": \"/users/8bb9a3fa-d508-11e2-875d-a59031a365e8/following/bf9a95da-d508-11e2-bf44-236d2eee13a7/roles\",\n" +
    "      \"following\": \"/users/8bb9a3fa-d508-11e2-875d-a59031a365e8/following/bf9a95da-d508-11e2-bf44-236d2eee13a7/following\",\n" +
    "      \"followers\": \"/users/8bb9a3fa-d508-11e2-875d-a59031a365e8/following/bf9a95da-d508-11e2-bf44-236d2eee13a7/followers\"\n" +
    "    }\n" +
    "  },\n" +
    "  \"title\": \"#\"&gt;&lt;img src=x onerror=prompt(1);&gt;\"\n" +
    "}</pre>\n" +
    "        </div>\n" +
    "        <div id=\"query-row-content-bf9a95da-d508-11e2-bf44-236d2eee13a7\" style=\"display:none\">\n" +
    "          <table>\n" +
    "            <tbody>\n" +
    "            <tr>\n" +
    "              <td>picture</td>\n" +
    "              <td>http://www.gravatar.com/avatar/01b37aa66496988ca780b3f515bc768e</td></tr><tr><td>uuid</td><td>bf9a95da-d508-11e2-bf44-236d2eee13a7</td></tr><tr><td>type</td><td>user</td></tr><tr><td>name</td><td>#&amp;quot;&amp;gt;&amp;lt;img src=x onerror=prompt(1);&amp;gt;</td></tr><tr><td>created</td><td>1371224432557</td></tr><tr><td>modified</td><td>1371851347024</td></tr><tr><td>username</td><td>10</td></tr><tr><td>email</td><td>fdsafdsa@ookfd.com</td></tr><tr><td>activated</td><td>true</td></tr><tr><td></td><td style=\"padding: 0\"><table><tbody><tr></tr><tr><td>addr1</td><td></td></tr><tr><td>addr2</td><td></td></tr><tr><td>city</td><td></td></tr><tr><td>state</td><td></td></tr><tr><td>zip</td><td></td></tr><tr><td>country</td><td></td></tr></tbody></table></td></tr><tr><td></td><td style=\"padding: 0\"><table><tbody><tr></tr><tr><td>path</td><td>/users/8bb9a3fa-d508-11e2-875d-a59031a365e8/following/bf9a95da-d508-11e2-bf44-236d2eee13a7</td></tr><tr><td></td><td style=\"padding: 0\"><table><tbody><tr></tr><tr><td>rolenames</td><td>/users/8bb9a3fa-d508-11e2-875d-a59031a365e8/following/bf9a95da-d508-11e2-bf44-236d2eee13a7/rolenames</td></tr><tr><td>permissions</td><td>/users/8bb9a3fa-d508-11e2-875d-a59031a365e8/following/bf9a95da-d508-11e2-bf44-236d2eee13a7/permissions</td></tr></tbody></table></td></tr><tr><td></td><td style=\"padding: 0\"><table><tbody><tr></tr><tr><td>activities</td><td>/users/8bb9a3fa-d508-11e2-875d-a59031a365e8/following/bf9a95da-d508-11e2-bf44-236d2eee13a7/activities</td></tr><tr><td>devices</td><td>/users/8bb9a3fa-d508-11e2-875d-a59031a365e8/following/bf9a95da-d508-11e2-bf44-236d2eee13a7/devices</td></tr><tr><td>feed</td><td>/users/8bb9a3fa-d508-11e2-875d-a59031a365e8/following/bf9a95da-d508-11e2-bf44-236d2eee13a7/feed</td></tr><tr><td>groups</td><td>/users/8bb9a3fa-d508-11e2-875d-a59031a365e8/following/bf9a95da-d508-11e2-bf44-236d2eee13a7/groups</td></tr><tr><td>roles</td><td>/users/8bb9a3fa-d508-11e2-875d-a59031a365e8/following/bf9a95da-d508-11e2-bf44-236d2eee13a7/roles</td></tr><tr><td>following</td><td>/users/8bb9a3fa-d508-11e2-875d-a59031a365e8/following/bf9a95da-d508-11e2-bf44-236d2eee13a7/following</td></tr><tr><td>followers</td><td>/users/8bb9a3fa-d508-11e2-875d-a59031a365e8/following/bf9a95da-d508-11e2-bf44-236d2eee13a7/followers</td></tr></tbody></table></td></tr></tbody></table></td></tr><tr><td>title</td><td>#&amp;quot;&amp;gt;&amp;lt;img src=x onerror=prompt(1);&amp;gt;</td>\n" +
    "            </tr>\n" +
    "            </tbody>\n" +
    "          </table>\n" +
    "        </div>\n" +
    "      </div>\n" +
    "    </td>\n" +
    "  </tr>\n" +
    "  </tbody>\n" +
    "</table>"
  );


  $templateCache.put('data/entity.html',
    "<div class=\"content-page\">\n" +
    "\n" +
    "  <h4>Entity Detail</h4>\n" +
    "  <div class=\"well\">\n" +
    "    <a href=\"#!/data\" class=\"outside-link\"><< Back to collection</a>\n" +
    "  </div>\n" +
    "  <fieldset>\n" +
    "    <div class=\"control-group\">\n" +
    "      <strong>Path </strong>\n" +
    "      <div class=\"controls\">\n" +
    "        {{entityType}}/{{entityUUID}}\n" +
    "      </div>\n" +
    "    </div>\n" +
    "\n" +
    "    <div class=\"control-group\">\n" +
    "      <label class=\"control-label\" for=\"query-source\"><strong>JSON Body</strong></label>\n" +
    "      <div class=\"controls\">\n" +
    "        <textarea ng-model=\"queryBody\" class=\"span6 pull-left\" rows=\"12\">{{queryBody}}</textarea>\n" +
    "        <br>\n" +
    "        <a class=\"btn pull-left\" ng-click=\"validateJson();\">Validate JSON</a>\n" +
    "      </div>\n" +
    "    </div>\n" +
    "    <div style=\"clear: both; height: 10px;\"></div>\n" +
    "    <div class=\"control-group\">\n" +
    "      <button type=\"button\" class=\"btn btn-primary\" id=\"button-query\" ng-click=\"saveEntity();\">Save</button>\n" +
    "      <!--button type=\"button\" class=\"btn btn-primary\" id=\"button-query\" ng-click=\"run();\">Delete</button-->\n" +
    "    </div>\n" +
    "  </fieldset>\n" +
    "\n" +
    "</div>\n" +
    "\n"
  );


  $templateCache.put('dialogs/modal.html',
    "    <div class=\"modal show fade\" tabindex=\"-1\" role=\"dialog\" aria-hidden=\"true\">\n" +
    "        <form ng-submit=\"extraDelegate(extrabutton)\" name=\"dialogForm\" novalidate>\n" +
    "\n" +
    "        <div class=\"modal-header\">\n" +
    "            <h1 class=\"title\">{{title}}</h1>\n" +
    "        </div>\n" +
    "\n" +
    "        <div class=\"modal-body\" ng-transclude></div>\n" +
    "        <div class=\"modal-footer\">\n" +
    "            {{footertext}}\n" +
    "            <input type=\"submit\" class=\"btn\" id=\"dialogButton-{{buttonId}}\" ng-if=\"extrabutton\" ng-disabled=\"!dialogForm.$valid\" aria-hidden=\"true\" ng-value=\"extrabuttonlabel\"/>\n" +
    "            <button class=\"btn cancel pull-left\" data-dismiss=\"modal\" aria-hidden=\"true\"\n" +
    "                    ng-click=\"closeDelegate(close)\">{{closelabel}}\n" +
    "            </button>\n" +
    "        </div>\n" +
    "        </form>    </div>\n"
  );


  $templateCache.put('global/appswitcher-template.html',
    "<li id=\"globalNav\" class=\"dropdown dropdownContainingSubmenu active\">\n" +
    "  <a class=\"dropdown-toggle\" data-toggle=\"dropdown\">API Platform<b class=\"caret\"></b></a>\n" +
    "  <ul class=\"dropdown-menu pull-right\">\n" +
    "    <li id=\"globalNavSubmenuContainer\">\n" +
    "      <ul>\n" +
    "        <li data-globalNavDetail=\"globalNavDetailApigeeHome\"><a target=\"_blank\" href=\"http://apigee.com\">Apigee Home</a></li>\n" +
    "        <li data-globalNavDetail=\"globalNavDetailAppServices\" class=\"active\"><a target=\"_blank\" href=\"https://apigee.com/usergrid/\">App Services</a></li>\n" +
    "        <li data-globalNavDetail=\"globalNavDetailApiPlatform\" ><a target=\"_blank\" href=\"https://enterprise.apigee.com\">API Platform</a></li>\n" +
    "        <li data-globalNavDetail=\"globalNavDetailApiConsoles\"><a target=\"_blank\" href=\"http://apigee.com/providers\">API Consoles</a></li>\n" +
    "      </ul>\n" +
    "    </li>\n" +
    "    <li id=\"globalNavDetail\">\n" +
    "      <div id=\"globalNavDetailApigeeHome\">\n" +
    "        <div class=\"globalNavDetailApigeeLogo\"></div>\n" +
    "        <div class=\"globalNavDetailDescription\">You need apps and apps need APIs. Apigee is the leading API platform for enterprises and developers.</div>\n" +
    "      </div>\n" +
    "      <div id=\"globalNavDetailAppServices\">\n" +
    "        <div class=\"globalNavDetailSubtitle\">For App Developers</div>\n" +
    "        <div class=\"globalNavDetailTitle\">App Services</div>\n" +
    "        <div class=\"globalNavDetailDescription\">Build engaging applications, store data, manage application users, and more.</div>\n" +
    "      </div>\n" +
    "      <div id=\"globalNavDetailApiPlatform\">\n" +
    "        <div class=\"globalNavDetailSubtitle\">For API Developers</div>\n" +
    "        <div class=\"globalNavDetailTitle\">API Platform</div>\n" +
    "        <div class=\"globalNavDetailDescription\">Create, configure, manage and analyze your APIs and resources.</div>\n" +
    "      </div>\n" +
    "      <div id=\"globalNavDetailApiConsoles\">\n" +
    "        <div class=\"globalNavDetailSubtitle\">For API Developers</div>\n" +
    "        <div class=\"globalNavDetailTitle\">API Consoles</div>\n" +
    "        <div class=\"globalNavDetailDescription\">Explore over 100 APIs with the Apigee API Console, or create and embed your own API Console.</div>\n" +
    "      </div>\n" +
    "    </li>\n" +
    "  </ul>\n" +
    "</li>"
  );


  $templateCache.put('global/insecure-banner.html',
    "<div ng-if=\"securityWarning\" ng-cloak class=\"demo-holder\">\n" +
    "    <div class=\"alert alert-demo alert-animate\">\n" +
    "        <div class=\"alert-text\">\n" +
    "            <i class=\"pictogram\">&#9888;</i>Warning: This application has \"sandbox\" permissions and is not production ready. <a target=\"_blank\" href=\"http://apigee.com/docs/app-services/content/securing-your-app\">Please go to our security documentation to find out more.</a></span>\n" +
    "        </div>\n" +
    "    </div>\n" +
    "</div>"
  );


  $templateCache.put('global/page-title.html',
    "<section class=\"row-fluid\">\n" +
    "    <div class=\"span12\">\n" +
    "        <div class=\"page-filters\">\n" +
    "            <h1 class=\"title pull-left\" id=\"pageTitle\"><i class=\"pictogram title\" style=\"padding-right: 5px;\">{{icon}}</i>{{title}} <a class=\"super-help\" href=\"http://community.apigee.com/content/apigee-customer-support\" target=\"_blank\"  >(need help?)</a></h1>\n" +
    "        </div>\n" +
    "    </div>\n" +
    "    <bsmodal id=\"need-help\"\n" +
    "             title=\"Need Help?\"\n" +
    "             close=\"hideModal\"\n" +
    "             closelabel=\"Cancel\"\n" +
    "             extrabutton=\"sendHelp\"\n" +
    "             extrabuttonlabel=\"Get Help\"\n" +
    "             ng-cloak>\n" +
    "        <p>Do you want to contact support? Support will get in touch with you as soon as possible.</p>\n" +
    "    </bsmodal>\n" +
    "</section>\n" +
    "\n"
  );


  $templateCache.put('groups/groups-activities.html',
    "<div class=\"content-page\" ng-controller=\"GroupsActivitiesCtrl\">\n" +
    "\n" +
    "  <br>\n" +
    "  <div>\n" +
    "    <table class=\"table table-striped\">\n" +
    "      <tbody>\n" +
    "      <tr class=\"table-header\">\n" +
    "        <td>Date</td>\n" +
    "        <td>Content</td>\n" +
    "        <td>Verb</td>\n" +
    "        <td>UUID</td>\n" +
    "      </tr>\n" +
    "      <tr class=\"zebraRows\" ng-repeat=\"activity in selectedGroup.activities\">\n" +
    "        <td>{{activity.createdDate}}</td>\n" +
    "        <td>{{activity.content}}</td>\n" +
    "        <td>{{activity.verb}}</td>\n" +
    "        <td>{{activity.uuid}}</td>\n" +
    "      </tr>\n" +
    "      </tbody>\n" +
    "    </table>\n" +
    "  </div>\n" +
    "\n" +
    "\n" +
    "</div>"
  );


  $templateCache.put('groups/groups-details.html',
    "<div class=\"content-page\" ng-controller=\"GroupsDetailsCtrl\">\n" +
    "\n" +
    "  <div>\n" +
    "      <form name=\"updateGroupDetailForm\" ng-submit=\"saveSelectedGroup()\" novalidate>\n" +
    "          <div style=\"float: left; padding-right: 30px;\">\n" +
    "              <h4 class=\"ui-dform-legend\">Group Information</h4>\n" +
    "              <label for=\"group-title\" class=\"ui-dform-label\">Group Title</label>\n" +
    "              <input type=\"text\" id=\"group-title\" ng-pattern=\"titleRegex\" ng-attr-title=\"{{titleRegexDescription}}\" required class=\"ui-dform-text\" ng-model=\"group.title\" ug-validate>\n" +
    "                  <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('group title box')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_details_title}}\" tooltip-placement=\"right\">(?)</a>\n" +
    "              <br/>\n" +
    "            <label for=\"group-path\" class=\"ui-dform-label\">Group Path</label>\n" +
    "            <input type=\"text\" id=\"group-path\" required ng-attr-title=\"{{pathRegexDescription}}\" placeholder=\"ex: /mydata\" ng-pattern=\"pathRegex\" class=\"ui-dform-text\" ng-model=\"group.path\" ug-validate>\n" +
    "                <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('group path box')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_details_path}}\" tooltip-placement=\"right\">(?)</a>\n" +
    "            <br/>\n" +
    "          </div>\n" +
    "          <br style=\"clear:both\"/>\n" +
    "\n" +
    "          <div style=\"width:100%;float:left;padding: 20px 0\">\n" +
    "              <input type=\"submit\" value=\"Save Group\" style=\"margin-right: 15px;\" ng-disabled=\"!updateGroupDetailForm.$valid\" class=\"btn btn-primary\" />\n" +
    "          </div>\n" +
    "\n" +
    "          <div class=\"content-container\">\n" +
    "              <h4>JSON Group Object <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('group json object')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_groups_json_object}}\" tooltip-placement=\"right\">(?)</a></h4>\n" +
    "              <pre id=\"{{help.showJsonId}}\">{{json}}</pre>\n" +
    "          </div>\n" +
    "      </form>\n" +
    "  </div>\n" +
    "\n" +
    "\n" +
    "</div>"
  );


  $templateCache.put('groups/groups-members.html',
    "<div class=\"content-page\" ng-controller=\"GroupsMembersCtrl\">\n" +
    "\n" +
    "\n" +
    "  <bsmodal id=\"removeFromGroup\"\n" +
    "           title=\"Confirmation\"\n" +
    "           close=\"hideModal\"\n" +
    "           closelabel=\"Cancel\"\n" +
    "           extrabutton=\"removeUsersFromGroupDialog\"\n" +
    "           extrabuttonlabel=\"Delete\"\n" +
    "           ng-cloak>\n" +
    "    <p>Are you sure you want to remove the users from the seleted group(s)?</p>\n" +
    "  </bsmodal>\n" +
    "\n" +
    "  <bsmodal id=\"addGroupToUser\"\n" +
    "           title=\"Add user to group\"\n" +
    "           close=\"hideModal\"\n" +
    "           closelabel=\"Cancel\"\n" +
    "           extrabutton=\"addGroupToUserDialog\"\n" +
    "           extrabuttonlabel=\"Add\"\n" +
    "           ng-cloak>\n" +
    "    <div class=\"btn-group\">\n" +
    "      <a class=\"btn dropdown-toggle filter-selector\" data-toggle=\"dropdown\">\n" +
    "        <span class=\"filter-label\">{{$parent.user != '' ? $parent.user.username : 'Select a user...'}}</span>\n" +
    "        <span class=\"caret\"></span>\n" +
    "      </a>\n" +
    "      <ul class=\"dropdown-menu\">\n" +
    "        <li ng-repeat=\"user in $parent.usersTypeaheadValues\" class=\"filterItem\"><a ng-click=\"$parent.$parent.user = user\">{{user.username}}</a></li>\n" +
    "      </ul>\n" +
    "    </div>\n" +
    "  </bsmodal>\n" +
    "\n" +
    "\n" +
    "  <div class=\"button-strip\">\n" +
    "    <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('group add user button')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_groups_users_add_user}}\" tooltip-placement=\"left\">(?)</a>\n" +
    "    <button class=\"btn btn-primary\"  ng-click=\"showModal('addGroupToUser')\">Add User to Group</button>\n" +
    "    <button class=\"btn btn-primary\" ng-disabled=\"!hasMembers || !valueSelected(groupsCollection.users._list)\" ng-click=\"showModal('removeFromGroup')\">Remove User(s) from Group</button>\n" +
    "  </div>\n" +
    "  <table class=\"table table-striped\">\n" +
    "    <tr class=\"table-header\">\n" +
    "      <td style=\"width: 30px;\"><input type=\"checkbox\" ng-show=\"hasMembers\" id=\"selectAllCheckbox\" ng-model=\"groupMembersSelected\" ng-click=\"selectAllEntities(groupsCollection.users._list,this,'groupMembersSelected')\"></td>\n" +
    "      <td style=\"width: 50px;\"></td>\n" +
    "      <td>Username</td>\n" +
    "      <td>Display Name</td>\n" +
    "    </tr>\n" +
    "    <tr class=\"zebraRows\" ng-repeat=\"user in groupsCollection.users._list\">\n" +
    "      <td>\n" +
    "        <input\n" +
    "          type=\"checkbox\"\n" +
    "          ng-model=\"user.checked\"\n" +
    "          >\n" +
    "      </td>\n" +
    "      <td><img style=\"width:30px;height:30px;\" ng-src=\"{{user._portal_image_icon}}\"></td>\n" +
    "      <td>{{user.get('username')}}</td>\n" +
    "      <td>{{user.get('name')}}</td>\n" +
    "    </tr>\n" +
    "  </table>\n" +
    "  <div style=\"padding: 10px 5px 10px 5px\">\n" +
    "    <button class=\"btn btn-primary\" ng-click=\"getPrevious()\" style=\"display:{{previous_display}}\">< Previous</button>\n" +
    "    <button class=\"btn btn-primary\" ng-click=\"getNext()\" style=\"display:{{next_display}}; float:right;\">Next ></button>\n" +
    "  </div>\n" +
    "</div>"
  );


  $templateCache.put('groups/groups-roles.html',
    "<div class=\"content-page\" ng-controller=\"GroupsRolesCtrl\">\n" +
    "\n" +
    "  <bsmodal id=\"addGroupToRole\"\n" +
    "           title=\"Add group to role\"\n" +
    "           close=\"hideModal\"\n" +
    "           closelabel=\"Cancel\"\n" +
    "           extrabutton=\"addGroupToRoleDialog\"\n" +
    "           extrabuttonlabel=\"Add\"\n" +
    "           ng-cloak>\n" +
    "    <div class=\"btn-group\">\n" +
    "      <a class=\"btn dropdown-toggle filter-selector\" data-toggle=\"dropdown\">\n" +
    "        <span class=\"filter-label\">{{$parent.name != '' ? $parent.name : 'Role name...'}}</span>\n" +
    "        <span class=\"caret\"></span>\n" +
    "      </a>\n" +
    "      <ul class=\"dropdown-menu\">\n" +
    "        <li ng-repeat=\"role in $parent.rolesTypeaheadValues\" class=\"filterItem\"><a ng-click=\"$parent.$parent.name = role.name\">{{role.name}}</a></li>\n" +
    "      </ul>\n" +
    "    </div>\n" +
    "  </bsmodal>\n" +
    "\n" +
    "  <bsmodal id=\"leaveRoleFromGroup\"\n" +
    "           title=\"Confirmation\"\n" +
    "           close=\"hideModal\"\n" +
    "           closelabel=\"Cancel\"\n" +
    "           extrabutton=\"leaveRoleDialog\"\n" +
    "           extrabuttonlabel=\"Leave\"\n" +
    "           ng-cloak>\n" +
    "    <p>Are you sure you want to remove the group from the role(s)?</p>\n" +
    "  </bsmodal>\n" +
    "\n" +
    "\n" +
    "  <div class=\"button-strip\">\n" +
    "    <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('groups roles add role button')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_groups_roles_add_leave_role}}\" tooltip-placement=\"left\">(?)</a>\n" +
    "    <button class=\"btn btn-primary\" ng-click=\"showModal('addGroupToRole')\">Add Role to Group</button>\n" +
    "    <button class=\"btn btn-primary\" ng-disabled=\"!hasRoles || !valueSelected(groupsCollection.roles._list)\" ng-click=\"showModal('leaveRoleFromGroup')\">Remove Role(s) from Group</button>\n" +
    "  </div>\n" +
    "  <h4>Roles <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('groups roles roles list')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_groups_roles_roles}}\" tooltip-placement=\"top\">(?)</a></h4>\n" +
    "  <table class=\"table table-striped\">\n" +
    "    <tbody>\n" +
    "    <tr class=\"table-header\">\n" +
    "      <td style=\"width: 30px;\"><input type=\"checkbox\" ng-show=\"hasRoles\" id=\"groupsSelectAllCheckBox\" ng-model=\"groupRoleSelected\" ng-click=\"selectAllEntities(groupsCollection.roles._list,this,'groupRoleSelected')\" ></td>\n" +
    "      <td>Role Name</td>\n" +
    "      <td>Role title</td>\n" +
    "    </tr>\n" +
    "    <tr class=\"zebraRows\" ng-repeat=\"role in groupsCollection.roles._list\">\n" +
    "      <td>\n" +
    "        <input\n" +
    "          type=\"checkbox\"\n" +
    "          ng-model=\"role.checked\"\n" +
    "          >\n" +
    "      </td>\n" +
    "      <td>{{role._data.name}}</td>\n" +
    "      <td>{{role._data.title}}</td>\n" +
    "    </tr>\n" +
    "    </tbody>\n" +
    "  </table>\n" +
    "  <div style=\"padding: 10px 5px 10px 5px\">\n" +
    "    <button class=\"btn btn-primary\" ng-click=\"getPreviousRoles()\" style=\"display:{{roles_previous_display}}\">< Previous</button>\n" +
    "    <button class=\"btn btn-primary\" ng-click=\"getNextRoles()\" style=\"display:{{roles_next_display}};float:right;\">Next ></button>\n" +
    "  </div>\n" +
    "\n" +
    "\n" +
    "  <bsmodal id=\"deletePermission\"\n" +
    "           title=\"Confirmation\"\n" +
    "           close=\"hideModal\"\n" +
    "           closelabel=\"Cancel\"\n" +
    "           extrabutton=\"deleteGroupPermissionDialog\"\n" +
    "           extrabuttonlabel=\"Delete\"\n" +
    "           ng-cloak>\n" +
    "    <p>Are you sure you want to delete the permission(s)?</p>\n" +
    "  </bsmodal>\n" +
    "\n" +
    "\n" +
    "  <bsmodal id=\"addPermission\"\n" +
    "           title=\"New Permission\"\n" +
    "           close=\"hideModal\"\n" +
    "           closelabel=\"Cancel\"\n" +
    "           extrabutton=\"addGroupPermissionDialog\"\n" +
    "           extrabuttonlabel=\"Add\"\n" +
    "           ng-cloak>\n" +
    "    <p>Path: <input ng-model=\"$parent.permissions.path\" placeholder=\"ex: /mydata\" id=\"groupsrolespermissions\" type=\"text\" ng-pattern=\"pathRegex\" ng-attr-title=\"{{pathRegexDescription}}\" required ug-validate  /> <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('users roles new permission path box')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_groups_roles_new_permission_path}}\" tooltip-placement=\"right\">(?)</a></p>\n" +
    "    <div class=\"control-group\">\n" +
    "      <input type=\"checkbox\" ng-model=\"$parent.permissions.getPerm\"> GET <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('users roles add permission button')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_groups_roles_new_permission_verbs}}\" tooltip-placement=\"right\">(?)</a>\n" +
    "    </div>\n" +
    "    <div class=\"control-group\">\n" +
    "      <input type=\"checkbox\" ng-model=\"$parent.permissions.postPerm\"> POST\n" +
    "    </div>\n" +
    "    <div class=\"control-group\">\n" +
    "      <input type=\"checkbox\" ng-model=\"$parent.permissions.putPerm\"> PUT\n" +
    "    </div>\n" +
    "    <div class=\"control-group\">\n" +
    "      <input type=\"checkbox\" ng-model=\"$parent.permissions.deletePerm\"> DELETE\n" +
    "    </div>\n" +
    "  </bsmodal>\n" +
    "\n" +
    "\n" +
    "  <div class=\"button-strip\">\n" +
    "    <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('groups roles add permission button')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_groups_roles_add_delete_permission}}\" tooltip-placement=\"left\">(?)</a>\n" +
    "    <button class=\"btn btn-primary\" ng-click=\"showModal('addPermission')\">Add Permission</button>\n" +
    "    <button class=\"btn btn-primary\" ng-disabled=\"!hasPermissions || !valueSelected(selectedGroup.permissions)\" ng-click=\"showModal('deletePermission')\">Delete Permission(s)</button>\n" +
    "  </div>\n" +
    "  <h4>Permissions <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('groups roles permissions list')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_groups_roles_permissions}}\" tooltip-placement=\"top\">(?)</a>\n" +
    "  </h4>\n" +
    "  <table class=\"table table-striped\">\n" +
    "    <tbody>\n" +
    "    <tr class=\"table-header\">\n" +
    "      <td style=\"width: 30px;\"><input ng-show=\"hasPermissions\" type=\"checkbox\" id=\"permissionsSelectAllCheckBox\" ng-model=\"groupPermissionsSelected\" ng-click=\"selectAllEntities(selectedGroup.permissions,this,'groupPermissionsSelected')\"  ></td>\n" +
    "      <td>Path</td>\n" +
    "      <td>GET</td>\n" +
    "      <td>POST</td>\n" +
    "      <td>PUT</td>\n" +
    "      <td>DELETE</td>\n" +
    "    </tr>\n" +
    "    <tr class=\"zebraRows\" ng-repeat=\"permission in selectedGroup.permissions\">\n" +
    "      <td>\n" +
    "        <input\n" +
    "          type=\"checkbox\"\n" +
    "          ng-model=\"permission.checked\"\n" +
    "          >\n" +
    "      </td>\n" +
    "      <td>{{permission.path}}</td>\n" +
    "      <td>{{permission.operations.get}}</td>\n" +
    "      <td>{{permission.operations.post}}</td>\n" +
    "      <td>{{permission.operations.put}}</td>\n" +
    "      <td>{{permission.operations.delete}}</td>\n" +
    "    </tr>\n" +
    "    </tbody>\n" +
    "  </table>\n" +
    "\n" +
    "</div>"
  );


  $templateCache.put('groups/groups-tabs.html',
    "<div class=\"content-page\">\n" +
    "\n" +
    "  <section class=\"row-fluid\">\n" +
    "\n" +
    "    <div class=\"span12\">\n" +
    "      <div class=\"page-filters\">\n" +
    "        <h1 class=\"title\" class=\"pull-left\"><i class=\"pictogram title\">&#128101;</i> Groups</h1>\n" +
    "      </div>\n" +
    "    </div>\n" +
    "\n" +
    "  </section>\n" +
    "\n" +
    "  <div id=\"user-panel\" class=\"panel-buffer\">\n" +
    "    <ul id=\"user-panel-tab-bar\" class=\"nav nav-tabs\">\n" +
    "      <li><a href=\"javaScript:void(0);\" ng-click=\"gotoPage('groups')\">Group List</a></li>\n" +
    "      <li ng-class=\"detailsSelected\"><a href=\"javaScript:void(0);\" ng-click=\"gotoPage('groups/details')\">Details</a></li>\n" +
    "      <li ng-class=\"membersSelected\"><a href=\"javaScript:void(0);\" ng-click=\"gotoPage('groups/members')\">Users</a></li>\n" +
    "      <li ng-class=\"activitiesSelected\"><a href=\"javaScript:void(0);\" ng-click=\"gotoPage('groups/activities')\">Activities</a></li>\n" +
    "      <li ng-class=\"rolesSelected\"><a href=\"javaScript:void(0);\" ng-click=\"gotoPage('groups/roles')\">Roles &amp; Permissions</a></li>\n" +
    "    </ul>\n" +
    "  </div>\n" +
    "\n" +
    "  <div style=\"float: left; margin-right: 10px;\">\n" +
    "    <div style=\"float: left;\">\n" +
    "      <div class=\"user-header-title\"><strong>Group Path: </strong>{{selectedGroup.get('path')}}</div>\n" +
    "      <div class=\"user-header-title\"><strong>Group Title: </strong>{{selectedGroup.get('title')}}</div>\n" +
    "    </div>\n" +
    "  </div>\n" +
    "</div>\n" +
    "<br>\n" +
    "<br>\n"
  );


  $templateCache.put('groups/groups.html',
    "<div class=\"content-page\">\n" +
    "\n" +
    "  <div id=\"intro-page\" >\n" +
    "  <page-title title=\" Groups\" icon=\"&#128101;\"></page-title>\n" +
    "    \n" +
    "  </div>\n" +
    "  <bsmodal id=\"newGroup\"\n" +
    "           title=\"New Group\"\n" +
    "           close=\"hideModal\"\n" +
    "           closelabel=\"Cancel\"\n" +
    "           extrabutton=\"newGroupDialog\"\n" +
    "           extrabuttonlabel=\"Add\"\n" +
    "           ng-model=\"dialog\"\n" +
    "           ng-cloak>\n" +
    "    <fieldset>\n" +
    "      <div class=\"control-group\">\n" +
    "        <label for=\"title\">Title</label>\n" +
    "        <div class=\"controls\">\n" +
    "          <input type=\"text\" id=\"title\" ng-pattern=\"titleRegex\" ng-attr-title=\"{{titleRegexDescription}}\" required ng-model=\"newGroup.title\"class=\"input-xlarge\" ug-validate/>\n" +
    "          <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('group title box')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_details_title}}\" tooltip-placement=\"right\">(?)</a>\n" +
    "        </div>\n" +
    "      </div>\n" +
    "      <div class=\"control-group\">\n" +
    "        <label for=\"path\">Path</label>\n" +
    "        <div class=\"controls\">\n" +
    "          <input id=\"path\" type=\"text\" ng-attr-title=\"{{pathRegexDescription}}\" placeholder=\"ex: /mydata\" ng-pattern=\"pathRegex\" required ng-model=\"newGroup.path\" class=\"input-xlarge\" ug-validate/>\n" +
    "          <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('group path box')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_details_path}}\" tooltip-placement=\"right\">(?)</a>\n" +
    "        </div>\n" +
    "      </div>\n" +
    "    </fieldset>\n" +
    "  </bsmodal>\n" +
    "\n" +
    "  <bsmodal id=\"deleteGroup\"\n" +
    "           title=\"Delete Group\"\n" +
    "           close=\"hideModal\"\n" +
    "           closelabel=\"Cancel\"\n" +
    "           extrabutton=\"deleteGroupsDialog\"\n" +
    "           extrabuttonlabel=\"Delete\"\n" +
    "           ng-cloak>\n" +
    "    <p>Are you sure you want to delete the group(s)?</p>\n" +
    "  </bsmodal>\n" +
    "\n" +
    "\n" +
    "  <section class=\"row-fluid\">\n" +
    "    <div id=\"intro-list\" class=\"span3 user-col\">\n" +
    "      <div class=\"button-toolbar span12\">\n" +
    "        <a title=\"Select All\" class=\"btn btn-primary select-all toolbar\" ng-show=\"hasGroups\" ng-click=\"selectAllEntities(groupsCollection._list,this,'groupBoxesSelected',true)\"> <i class=\"pictogram\">&#8863;</i></a>\n" +
    "        <button title=\"Delete\" class=\"btn btn-primary toolbar\" ng-disabled=\"!hasGroups || !valueSelected(groupsCollection._list)\" ng-click=\"showModal('deleteGroup')\"><i class=\"pictogram\">&#9749;</i></button>\n" +
    "        <button title=\"Add\" class=\"btn btn-primary toolbar\" ng-click=\"showModal('newGroup')\"><i class=\"pictogram\">&#59136;</i></button>\n" +
    "        <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('users list')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_groups_add_remove_buttons}}\" tooltip-placement=\"right\">(?)</a>\n" +
    "      </div>\n" +
    "      <ul class=\"user-list\">\n" +
    "        <li ng-class=\"selectedGroup._data.uuid === group._data.uuid ? 'selected' : ''\" ng-repeat=\"group in groupsCollection._list\" ng-click=\"selectGroup(group._data.uuid)\">\n" +
    "          <input\n" +
    "              type=\"checkbox\"\n" +
    "              ng-value=\"group._data.uuid\"\n" +
    "              ng-checked=\"group.checked\"\n" +
    "              ng-model=\"group.checked\"\n" +
    "              >\n" +
    "          <a href=\"javaScript:void(0)\" >{{group.get('title')}}</a>\n" +
    "          <br/>\n" +
    "          <span ng-if=\"group.get('path')\" class=\"label\">Path:</span>/{{group.get('path')}}\n" +
    "        </li>\n" +
    "      </ul>\n" +
    "\n" +
    "\n" +
    "      <div style=\"padding: 10px 5px 10px 5px\">\n" +
    "        <button class=\"btn btn-primary\" ng-click=\"getPrevious()\" style=\"display:{{previous_display}}\">< Previous</button>\n" +
    "        <button class=\"btn btn-primary\" ng-click=\"getNext()\" style=\"display:{{next_display}}; float:right;\">Next ></button>\n" +
    "      </div>\n" +
    "    </div>\n" +
    "\n" +
    "    <div id=\"{{help.showTabsId}}\" class=\"span9 tab-content\" ng-show=\"selectedGroup.get\" >\n" +
    "      <div class=\"menu-toolbar\">\n" +
    "        <ul class=\"inline\" >\n" +
    "          <li class=\"tab\" ng-class=\"currentGroupsPage.route === '/groups/details' ? 'selected' : ''\"><div class=\"btn btn-primary toolbar\" ><a class=\"btn-content\" ng-click=\"selectGroupPage('/groups/details')\"><i class=\"pictogram\">&#59170;</i>Details</a>\n" +
    "             <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('groups details tab')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_groups_details_tab}}\" tooltip-placement=\"right\">(?)</a></div>\n" +
    "          </li>\n" +
    "          <li class=\"tab\" ng-class=\"currentGroupsPage.route === '/groups/members' ? 'selected' : ''\"><div class=\"btn btn-primary toolbar\" ><a class=\"btn-content\" ng-click=\"selectGroupPage('/groups/members')\"><i class=\"pictogram\">&#128101;</i>Users</a>\n" +
    "             <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('groups users tab')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_groups_users_tab}}\" tooltip-placement=\"right\">(?)</a></div>\n" +
    "          </li>\n" +
    "          <li class=\"tab\" ng-class=\"currentGroupsPage.route === '/groups/activities' ? 'selected' : ''\"><div class=\"btn btn-primary toolbar\" ><a class=\"btn-content\" ng-click=\"selectGroupPage('/groups/activities')\"><i class=\"pictogram\">&#59194;</i>Activities</a>\n" +
    "             <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('groups activities tab')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_groups_activities_tab}}\" tooltip-placement=\"right\">(?)</a></div>\n" +
    "          </li>\n" +
    "          <li class=\"tab\" ng-class=\"currentGroupsPage.route === '/groups/roles' ? 'selected' : ''\"><div class=\"btn btn-primary toolbar\" ><a class=\"btn-content\" ng-click=\"selectGroupPage('/groups/roles')\"><i class=\"pictogram\">&#127758;</i>Roles &amp; Permissions</a>\n" +
    "             <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('groups role tab')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_groups_roles_tab}}\" tooltip-placement=\"right\">(?)</a></div>\n" +
    "          </li>\n" +
    "        </ul>\n" +
    "      </div>\n" +
    "      <span ng-include=\"currentGroupsPage.template\"></span>\n" +
    "\n" +
    "  </section>\n" +
    "</div>\n"
  );


  $templateCache.put('login/forgot-password.html',
    "<div class=\"login-content\" ng-controller=\"ForgotPasswordCtrl\">\n" +
    "\t<iframe class=\"container\" ng-src=\"{{forgotPWiframeURL}}\" id=\"forgot-password-frame\" border=\"0\" style=\"border:0;width:600px;height:620px;\">\n" +
    "\t<p>Email Address: <input id=\"resetPasswordEmail\" name=\"resetPasswordEmail\" /></p>\n" +
    "\t<button class=\"btn btn-primary\" ng-click=\"\">Reset Password</button>\n" +
    "</div>\n"
  );


  $templateCache.put('login/loading.html',
    "\n" +
    "\n" +
    "<h1>Loading...</h1>"
  );


  $templateCache.put('login/login.html',
    "<div class=\"login-content\">\r" +
    "\n" +
    "  <bsmodal id=\"sendActivationLink\"\r" +
    "\n" +
    "           title=\"Resend Activation Link\"\r" +
    "\n" +
    "           close=\"hideModal\"\r" +
    "\n" +
    "           closelabel=\"Cancel\"\r" +
    "\n" +
    "           extrabutton=\"resendActivationLink\"\r" +
    "\n" +
    "           extrabuttonlabel=\"Send Activation\"\r" +
    "\n" +
    "           ng-cloak>\r" +
    "\n" +
    "    <fieldset>\r" +
    "\n" +
    "      <p>Email to send to: <input type=\"email\" required ng-model=\"$parent.activation.id\" ng-pattern=\"emailRegex\" ng-attr-title=\"{{emailRegexDescription}}\" name=\"activationId\" id=\"user-activationId\" class=\"input-xlarge\"/></p>\r" +
    "\n" +
    "    </fieldset>\r" +
    "\n" +
    "  </bsmodal>\r" +
    "\n" +
    "  <div class=\"login-holder\">\r" +
    "\n" +
    "  <form name=\"loginForm\" id=\"login-form\"  ng-submit=\"login()\" class=\"form-horizontal\" novalidate>\r" +
    "\n" +
    "    <h1 class=\"title\">Enter your credentials</h1>\r" +
    "\n" +
    "    <div class=\"alert-error\" id=\"loginError\" ng-if=\"loginMessage\">{{loginMessage}}</div>\r" +
    "\n" +
    "    <div class=\"control-group\">\r" +
    "\n" +
    "      <label class=\"control-label\" for=\"login-username\">Email or Username:</label>\r" +
    "\n" +
    "      <div class=\"controls\">\r" +
    "\n" +
    "        <input type=\"text\" ng-model=\"login.username\"  title=\"Please add a username or email.\"  class=\"\" id=\"login-username\" required ng-value=\"login.username\" size=\"20\" ug-validate>\r" +
    "\n" +
    "      </div>\r" +
    "\n" +
    "    </div>\r" +
    "\n" +
    "    <div class=\"control-group\">\r" +
    "\n" +
    "      <label class=\"control-label\" for=\"login-password\">Password:</label>\r" +
    "\n" +
    "      <div class=\"controls\">\r" +
    "\n" +
    "        <input type=\"password\" ng-model=\"login.password\"  required id=\"login-password\" class=\"\" ng-value=\"login.password\" size=\"20\" ug-validate>\r" +
    "\n" +
    "      </div>\r" +
    "\n" +
    "    </div>\r" +
    "\n" +
    "    <div class=\"control-group\" ng-show=\"requiresDeveloperKey\">\r" +
    "\n" +
    "      <label class=\"control-label\" for=\"login-developerkey\">Developer Key:</label>\r" +
    "\n" +
    "      <div class=\"controls\">\r" +
    "\n" +
    "        <input type=\"text\" ng-model=\"login.developerkey\" id=\"login-developerkey\" class=\"\" ng-value=\"login.developerkey\" size=\"20\" ug-validate>\r" +
    "\n" +
    "      </div>\r" +
    "\n" +
    "    </div>\r" +
    "\n" +
    "    <div class=\"form-actions\">\r" +
    "\n" +
    "      <div class=\"submit\">\r" +
    "\n" +
    "        <input type=\"submit\" name=\"button-login\" id=\"button-login\" ng-disabled=\"!loginForm.$valid || loading\" value=\"{{loading ? loadingText : 'Log In'}}\" class=\"btn btn-primary pull-right\">\r" +
    "\n" +
    "      </div>\r" +
    "\n" +
    "    </div>\r" +
    "\n" +
    "  </form>\r" +
    "\n" +
    "  </div>\r" +
    "\n" +
    "  <div class=\"extra-actions\">\r" +
    "\n" +
    "    <div class=\"submit\">\r" +
    "\n" +
    "      <a ng-click=\"gotoSignUp()\"   name=\"button-signUp\" id=\"button-signUp\" value=\"Sign Up\"\r" +
    "\n" +
    "         class=\"btn btn-primary pull-left\">Register</a>\r" +
    "\n" +
    "    </div>\r" +
    "\n" +
    "    <div class=\"submit\">\r" +
    "\n" +
    "      <a ng-click=\"gotoForgotPasswordPage()\" name=\"button-forgot-password\" id=\"button-forgot-password\"\r" +
    "\n" +
    "         value=\"\" class=\"btn btn-primary pull-left\">Forgot Password?</a>\r" +
    "\n" +
    "    </div>\r" +
    "\n" +
    "    <a ng-click=\"showModal('sendActivationLink')\"  name=\"button-resend-activation\" id=\"button-resend-activation\"\r" +
    "\n" +
    "       value=\"\" class=\"btn btn-primary pull-left\">Resend Activation Link</a>\r" +
    "\n" +
    "  </div>\r" +
    "\n" +
    "  <div id=\"gtm\" style=\"width: 450px;margin-top: 4em;\" />\r" +
    "\n" +
    "</div>\r" +
    "\n"
  );


  $templateCache.put('login/logout.html',
    "<div id=\"logut\">Logging out...</div>"
  );


  $templateCache.put('login/register.html',
    "<div class=\"signUp-content\">\n" +
    "  <div class=\"signUp-holder\">\n" +
    "    <form name=\"signUpform\" id=\"signUp-form\" ng-submit=\"register()\" class=\"form-horizontal\" ng-show=\"!signUpSuccess\" novalidate>\n" +
    "      <h1 class=\"title\">Register</h1>\n" +
    "\n" +
    "      <div class=\"alert\" ng-if=\"loginMessage\">{{loginMessage}}</div>\n" +
    "      <div class=\"control-group\">\n" +
    "        <label class=\"control-label\" for=\"register-orgName\">Organization:</label>\n" +
    "\n" +
    "        <div class=\"controls\">\n" +
    "          <input type=\"text\" ng-model=\"registeredUser.orgName\" id=\"register-orgName\" ng-pattern=\"appNameRegex\" ng-attr-title=\"{{appNameRegexDescription}}\" ug-validate  required class=\"\" size=\"20\">\n" +
    "        </div>\n" +
    "      </div>\n" +
    "\n" +
    "      <div class=\"control-group\">\n" +
    "        <label class=\"control-label\" for=\"register-name\">Name:</label>\n" +
    "\n" +
    "        <div class=\"controls\">\n" +
    "          <input type=\"text\" ng-model=\"registeredUser.name\" id=\"register-name\" ng-pattern=\"nameRegex\" ng-attr-title=\"{{nameRegexDescription}}\" ug-validate required class=\"\" size=\"20\">\n" +
    "        </div>\n" +
    "      </div>\n" +
    "\n" +
    "      <div class=\"control-group\">\n" +
    "        <label class=\"control-label\" for=\"register-userName\">Username:</label>\n" +
    "\n" +
    "        <div class=\"controls\">\n" +
    "          <input type=\"text\" ng-model=\"registeredUser.userName\" id=\"register-userName\" ng-pattern=\"usernameRegex\" ng-attr-title=\"{{usernameRegexDescription}}\" ug-validate required class=\"\" size=\"20\">\n" +
    "        </div>\n" +
    "      </div>\n" +
    "\n" +
    "      <div class=\"control-group\">\n" +
    "        <label class=\"control-label\" for=\"register-email\">Email:</label>\n" +
    "\n" +
    "        <div class=\"controls\">\n" +
    "          <input type=\"email\" ng-model=\"registeredUser.email\" id=\"register-email\"  ng-pattern=\"emailRegex\" ng-attr-title=\"{{emailRegexDescription}}\"  required class=\"\" ug-validate size=\"20\">\n" +
    "        </div>\n" +
    "      </div>\n" +
    "\n" +
    "\n" +
    "      <div class=\"control-group\">\n" +
    "        <label class=\"control-label\" for=\"register-password\">Password:</label>\n" +
    "\n" +
    "        <div class=\"controls\">\n" +
    "          <input type=\"password\" ng-pattern=\"passwordRegex\" ng-attr-title=\"{{passwordRegexDescription}}\" ug-validate ng-model=\"registeredUser.password\" id=\"register-password\" required class=\"\"\n" +
    "                 size=\"20\">\n" +
    "        </div>\n" +
    "      </div>\n" +
    "      <div class=\"control-group\">\n" +
    "        <label class=\"control-label\" for=\"register-confirmPassword\">Re-enter Password:</label>\n" +
    "\n" +
    "        <div class=\"controls\">\n" +
    "          <input type=\"password\" ng-model=\"registeredUser.confirmPassword\" required id=\"register-confirmPassword\" ug-validate class=\"\" size=\"20\">\n" +
    "        </div>\n" +
    "      </div>\n" +
    "      <div class=\"form-actions\">\n" +
    "        <div class=\"submit\">\n" +
    "          <input type=\"submit\" name=\"button-login\" ng-disabled=\"!signUpform.$valid\" id=\"button-login\" value=\"Register\"\n" +
    "                 class=\"btn btn-primary pull-right\">\n" +
    "        </div>\n" +
    "        <div class=\"submit\">\n" +
    "          <a ng-click=\"cancel()\" type=\"submit\" name=\"button-cancel\" id=\"button-cancel\"\n" +
    "             class=\"btn btn-primary pull-right\">Cancel</a>\n" +
    "        </div>\n" +
    "      </div>\n" +
    "    </form>\n" +
    "    <div class=\"console-section well thingy\" ng-show=\"signUpSuccess\">\n" +
    "      <span class=\"title\">We're holding a seat for you!</span>\n" +
    "      <br><br>\n" +
    "\n" +
    "      <p>Thanks for signing up for a spot on our private beta. We will send you an email as soon as we're ready for\n" +
    "        you!</p>\n" +
    "\n" +
    "      <p>In the mean time, you can stay up to date with App Services on our <a\n" +
    "          href=\"https://groups.google.com/forum/?fromgroups#!forum/usergrid\">GoogleGroup</a>.</p>\n" +
    "\n" +
    "      <p> <a href=\"#!/login\">Back to login</a></p>\n" +
    "    </div>\n" +
    "  </div>\n" +
    "\n" +
    "</div>\n"
  );


  $templateCache.put('menu.html',
    "<ul class=\"nav nav-list\" menu=\"sideMenu\">\n" +
    "    <li class=\"option {{item.active ? 'active' : ''}}\" ng-cloak=\"\" ng-repeat=\"item in menuItems\"><a data-ng-href=\"{{item.path}}\"><i class=\"pictogram\" ng-bind-html=\"item.pic\"></i>{{item.title}}</a>\n" +
    "        <ul class=\"nav nav-list\" ng-if=\"item.items\">\n" +
    "            <li ng-repeat=\"subItem in item.items\"><a data-ng-href=\"{{subItem.path}}\"><i class=\"pictogram sub\" ng-bind-html=\"subItem.pic\"></i>{{subItem.title}}</a>\n" +
    "            </li>\n" +
    "        </ul>\n" +
    "    </li>\n" +
    "</ul>"
  );


  $templateCache.put('menus/appMenu.html',
    "<ul id=\"app-menu\" class=\"nav top-nav span12\">\n" +
    "    <li class=\"span7\">\n" +
    "      <bsmodal id=\"newApplication\"\n" +
    "               title=\"Create New Application\"\n" +
    "               close=\"hideModal\"\n" +
    "               closelabel=\"Cancel\"\n" +
    "               extrabutton=\"newApplicationDialog\"\n" +
    "               extrabuttonlabel=\"Create\"\n" +
    "               buttonid=\"app\"\n" +
    "               ng-cloak>\n" +
    "        <div ng-show=\"!hasApplications\" class=\"modal-instructions\" >You have no applications, please create one.</div>\n" +
    "        <div  ng-show=\"hasCreateApplicationError\" class=\"alert-error\">Application already exists!</div>\n" +
    "        <p>New application name: <input ng-model=\"$parent.newApp.name\" id=\"app-name-input\" ng-pattern=\"appNameRegex\"  ng-attr-title=\"{{appNameRegexDescription}}\" type=\"text\" required ug-validate /></p>\n" +
    "      </bsmodal>\n" +
    "        <div class=\"btn-group\">\n" +
    "            <a class=\"btn dropdown-toggle top-selector app-selector\" id=\"current-app-selector\" data-toggle=\"dropdown\">\n" +
    "                <i class=\"pictogram\">&#9881;</i> {{myApp.currentApp}}\n" +
    "                <span class=\"caret\"></span>\n" +
    "            </a>\n" +
    "            <ul class=\"dropdown-menu app-nav\">\n" +
    "                <li name=\"app-selector\" ng-repeat=\"app in applications\">\n" +
    "                    <a id=\"app-{{app.name}}-link-id\" ng-click=\"appChange(app.name)\">{{app.name}}</a>\n" +
    "                </li>\n" +
    "            </ul>\n" +
    "        </div>\n" +
    "    </li>\n" +
    "    <li class=\"span5\">\n" +
    "      <a ng-if=\"activeUI\"\n" +
    "         class=\"btn btn-create zero-out pull-right\"\n" +
    "         ng-click=\"showModal('newApplication')\"\n" +
    "         analytics-on=\"click\"\n" +
    "         analytics-category=\"App Services\"\n" +
    "         analytics-label=\"Button\"\n" +
    "         analytics-event=\"Add New App\"\n" +
    "        >\n" +
    "        <i class=\"pictogram\">&#8862;</i>\n" +
    "        Add New App\n" +
    "      </a>\n" +
    "    </li>\n" +
    "</ul>"
  );


  $templateCache.put('menus/orgMenu.html',
    "<ul class=\"nav top-nav org-nav\">\n" +
    "  <li>\n" +
    "<div class=\"btn-group \">\n" +
    "    <a class=\"btn dropdown-toggle top-selector org-selector\" id=\"current-org-selector\" data-toggle=\"dropdown\">\n" +
    "        <i class=\"pictogram\">&#128193</i> {{currentOrg}}<span class=\"caret\"></span>\n" +
    "        </a>\n" +
    "    <ul class=\"dropdown-menu org-nav\">\n" +
    "          <li name=\"org-selector\" ng-repeat=\"(k,v) in organizations\">\n" +
    "              <a id=\"org-{{v.name}}-selector\" class=\"org-overview\" ng-click=\"orgChange(v.name)\"> {{v.name}}</a>\n" +
    "            </li>\n" +
    "         </ul>\n" +
    "    </div>\n" +
    "  </li></ul>"
  );


  $templateCache.put('org-overview/org-overview.html',
    "<div class=\"org-overview-content\" ng-show=\"activeUI\">\n" +
    "\n" +
    "  <page-title title=\" Org Administration\" icon=\"&#128362;\"></page-title>\n" +
    "\n" +
    "  <section class=\"row-fluid\">\n" +
    "\n" +
    "  <div class=\"span6\">\n" +
    "  \t<bsmodal id=\"introjs\"\n" +
    "             title=\"Welcome to the API BaaS Admin Portal\"\n" +
    "             close=\"hideModal\"\n" +
    "             closelabel=\"Skip\"\n" +
    "             extrabutton=\"startFirstTimeUser\"\n" +
    "             extrabuttonlabel=\"Take the tour\"\n" +
    "             ng-cloak>\n" +
    "      <p>To get started, click 'Take the tour' for a full walkthrough of the admin portal, or click 'Skip' to start working right away.</p>\n" +
    "    </bsmodal>\n" +
    "\t\t<div id=\"intro-4-current-org\">\t\n" +
    "\t    <h2 class=\"title\">Current Organization <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('current org')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_current_org}}\" tooltip-placement=\"right\">(?)</a></h2>\n" +
    "\t    <table class=\"table table-striped\">\n" +
    "\t      <tr>\n" +
    "\t        <td id=\"org-overview-name\">{{currentOrganization.name}}</td>\n" +
    "\t        <td style=\"text-align: right\">{{currentOrganization.uuid}}</td>\n" +
    "\t      </tr>\n" +
    "\t    </table>\n" +
    "\t\t</div>\n" +
    "\n" +
    "    <bsmodal id=\"newApplication\"\n" +
    "             title=\"Create New Application\"\n" +
    "             close=\"hideModal\"\n" +
    "             closelabel=\"Cancel\"\n" +
    "             extrabutton=\"newApplicationDialog\"\n" +
    "             extrabuttonlabel=\"Create\"\n" +
    "             ng-cloak>\n" +
    "      <p>New application name: <input ng-model=\"$parent.newApp.name\"  ug-validate required type=\"text\" ng-pattern=\"appNameRegex\" ng-attr-title=\"{{appNameRegexDescription}}\" /></p>\n" +
    "    </bsmodal>\n" +
    "\t\t<div id=\"intro-5-applications\">\t\t\n" +
    "\t    <h2 class=\"title\" > Applications <a class=\"help_tooltip\" ng-show=\"help.helpTooltipsEnabled\" ng-mouseover=\"help.sendTooltipGA('applications')\" href=\"#\" ng-attr-tooltip=\"{{tooltip_applications}}\" tooltip-placement=\"right\">(?)</a>\n" +
    "\t      <div class=\"header-button btn-group pull-right\">\n" +
    "\t        <a class=\"btn filter-selector\" style=\"{{applicationsSize === 10 ? 'width:290px':''}}\"  ng-click=\"showModal('newApplication')\">\n" +
    "\t          <span class=\"filter-label\">Add New App</span>\n" +
    "\t        </a>\n" +
    "\t      </div>\n" +
    "\t    </h2>\n" +
    "\t\t\n" +
    "\t    <table class=\"table table-striped\">\n" +
    "\t      <tr ng-repeat=\"application in applications\">\n" +
    "\t        <td>{{application.name}}</td>\n" +
    "\t        <td style=\"text-align: right\">{{application.uuid}}</td>\n" +
    "\t      </tr>\n" +
    "\t    </table>\n" +
    "\t\t</div>\n" +
    "    <bsmodal id=\"regenerateCredentials\"\n" +
    "             title=\"Confirmation\"\n" +
    "             close=\"hideModal\"\n" +
    "             closelabel=\"Cancel\"\n" +
    "             extrabutton=\"regenerateCredentialsDialog\"\n" +
    "             extrabuttonlabel=\"Yes\"\n" +
    "             ng-cloak>\n" +
    "      Are you sure you want to regenerate the credentials?\n" +
    "    </bsmodal>\n" +
    "\t\t<div id=\"intro-6-org-api-creds\">\n" +
    "\t    <h2 class=\"title\" >Organization API Credentials <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('api org credentials')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_org_api_creds}}\" tooltip-placement=\"right\">(?)</a>\n" +
    "\t      <div class=\"header-button btn-group pull-right\">\n" +
    "\t        <a class=\"btn filter-selector\" ng-click=\"showModal('regenerateCredentials')\">\n" +
    "\t          <span class=\"filter-label\">Regenerate Org Credentials</span>\n" +
    "\t        </a>\n" +
    "\t      </div>\n" +
    "\t    </h2>\n" +
    "\t\n" +
    "\t    <table class=\"table table-striped\">\n" +
    "\t      <tr>\n" +
    "\t        <td >Client ID</td>\n" +
    "\t        <td style=\"text-align: right\" >{{orgAPICredentials.client_id}}</td>\n" +
    "\t      </tr>\n" +
    "\t      <tr>\n" +
    "\t        <td>Client Secret</td>\n" +
    "\t        <td style=\"text-align: right\">{{orgAPICredentials.client_secret}}</td>\n" +
    "\t      </tr>\n" +
    "\t    </table>\n" +
    "\t\t</div>\n" +
    "    <bsmodal id=\"newAdministrator\"\n" +
    "             title=\"Create New Administrator\"\n" +
    "             close=\"hideModal\"\n" +
    "             closelabel=\"Cancel\"\n" +
    "             extrabutton=\"newAdministratorDialog\"\n" +
    "             extrabuttonlabel=\"Create\"\n" +
    "             ng-cloak>\n" +
    "      <p>New administrator email: <input id=\"newAdminInput\" ug-validate ng-model=\"$parent.admin.email\" pattern=\"emailRegex\" ng-attr-title=\"{{emailRegexDescription}}\" required type=\"email\" /></p>\n" +
    "    </bsmodal>\n" +
    "\t\t<div id=\"intro-7-org-admins\">\n" +
    "\t    <h2 class=\"title\" >Organization Administrators <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('org admins')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_org_admins}}\" tooltip-placement=\"right\">(?)</a>\n" +
    "\t      <div class=\"header-button btn-group pull-right\">\n" +
    "\t        <a class=\"btn filter-selector\" ng-click=\"showModal('newAdministrator')\">\n" +
    "\t          <span class=\"filter-label\">Add New Administrator</span>\n" +
    "\t        </a>\n" +
    "\t      </div>\n" +
    "\t    </h2>\n" +
    "\t\n" +
    "\t    <table class=\"table table-striped\">\n" +
    "\t      <tr ng-repeat=\"administrator in orgAdministrators\">\n" +
    "\t        <td><img style=\"width:30px;height:30px;\" ng-src=\"{{administrator.image}}\"> {{administrator.name}}</td>\n" +
    "\t        <td style=\"text-align: right\">{{administrator.email}}</td>\n" +
    "\t      </tr>\n" +
    "\t    </table>\n" +
    "\t\t\t</div>\n" +
    "  </div>\n" +
    "\n" +
    "  <div class=\"span6\">\n" +
    "  \t<div id=\"intro-8-activities\">\n" +
    "\t    <h2 class=\"title\">Activities <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('activities')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_activities}}\" tooltip-placement=\"right\">(?)</a></h2>\n" +
    "\t    <table class=\"table table-striped\">\n" +
    "\t      <tr ng-repeat=\"activity in activities\">\n" +
    "\t        <td>{{activity.title}}</td>\n" +
    "\t        <td style=\"text-align: right\">{{activity.date}}</td>\n" +
    "\t      </tr>\n" +
    "\t    </table>\n" +
    "\t</div>\n" +
    "  </div>\n" +
    "\n" +
    "\n" +
    "  </section>\n" +
    "</div>"
  );


  $templateCache.put('profile/account.html',
    "<page-title title=\" Account Settings\" icon=\"&#59170\"></page-title>\n" +
    "\n" +
    "<section class=\"row-fluid\">\n" +
    "  <div class=\"span12 tab-content\">\n" +
    "    <div class=\"menu-toolbar\">\n" +
    "      <ul class=\"inline\">\n" +
    "        <li class=\"tab\" ng-show=\"!use_sso\" ng-class=\"currentAccountPage.route === '/profile/profile' ? 'selected' : ''\"><a class=\"btn btn-primary toolbar\" id=\"profile-link\" ng-click=\"selectAccountPage('/profile/profile')\"><i class=\"pictogram\">&#59170;</i>Profile</a></li>\n" +
    "        <li class=\"tab\" ng-class=\"currentAccountPage.route === '/profile/organizations' ? 'selected' : ''\"><a class=\"btn btn-primary toolbar\" id=\"account-link\" ng-click=\"selectAccountPage('/profile/organizations')\"><i class=\"pictogram\">&#128101;</i>Organizations</a></li>\n" +
    "      </ul>\n" +
    "    </div>\n" +
    "    <span ng-include=\"currentAccountPage.template\"></span>\n" +
    "  </div>\n" +
    "</section>"
  );


  $templateCache.put('profile/organizations.html',
    "<div class=\"content-page\"   ng-controller=\"OrgCtrl\">\n" +
    "\n" +
    "<page-title title=\" Organizations\" icon=\"&#128362;\"></page-title>\n" +
    "\n" +
    "\n" +
    "  <bsmodal id=\"newOrganization\"\n" +
    "           title=\"Create New Organization\"\n" +
    "           close=\"hideModal\"\n" +
    "           closelabel=\"Cancel\"\n" +
    "           extrabutton=\"addOrganization\"\n" +
    "           extrabuttonlabel=\"Create\"\n" +
    "           ng-cloak>\n" +
    "    <fieldset>\n" +
    "\n" +
    "      <div class=\"control-group\">\n" +
    "        <label for=\"new-user-orgname\">Organization Name</label>\n" +
    "\n" +
    "        <div class=\"controls\">\n" +
    "          <input type=\"text\" required title=\"Name\" ug-validate ng-pattern=\"nameRegex\" ng-attr-title=\"{{nameRegexDescription}}\" ng-model=\"$parent.org.name\" name=\"name\" id=\"new-user-orgname\" class=\"input-xlarge\"/>\n" +
    "\n" +
    "          <p class=\"help-block hide\"></p>\n" +
    "        </div>\n" +
    "      </div>\n" +
    "\n" +
    "    </fieldset>\n" +
    "  </bsmodal>\n" +
    "\n" +
    "\n" +
    "      <div class=\"row-fluid\" >\n" +
    "      <div class=\"span3 user-col \">\n" +
    "\n" +
    "        <div class=\"button-toolbar span12\">\n" +
    "\n" +
    "          <button class=\"btn btn-primary toolbar\" ng-click=\"showModal('newOrganization')\" ng-show=\"true\"><i class=\"pictogram\">&#59136;</i>\n" +
    "          </button>\n" +
    "        </div>\n" +
    "        <ul class=\"user-list\">\n" +
    "          <li ng-class=\"selectedOrg.uuid === org.uuid ? 'selected' : ''\"\n" +
    "              ng-repeat=\"org in orgs\" ng-click=\" selectOrganization(org)\">\n" +
    "\n" +
    "            <a href=\"javaScript:void(0)\">{{org.name}}</a>\n" +
    "          </li>\n" +
    "        </ul>\n" +
    "      </div>\n" +
    "      <div class=\"span9\">\n" +
    "        <div class=\"row-fluid\" >\n" +
    "          <h4>Organization Information</h4>\n" +
    "          <div class=\"span11\" ng-show=\"selectedOrg\">\n" +
    "            <label  class=\"ui-dform-label\">Applications</label>\n" +
    "            <table class=\"table table-striped\">\n" +
    "              <tr ng-repeat=\"app in selectedOrg.applicationsArray\">\n" +
    "                <td> {{app.name}}</td>\n" +
    "                <td style=\"text-align: right\">{{app.uuid}}</td>\n" +
    "              </tr>\n" +
    "            </table>\n" +
    "            <br/>\n" +
    "            <label  class=\"ui-dform-label\">Users</label>\n" +
    "            <table class=\"table table-striped\">\n" +
    "              <tr ng-repeat=\"user in selectedOrg.usersArray\">\n" +
    "                <td> {{user.name}}</td>\n" +
    "                <td style=\"text-align: right\">{{user.email}}</td>\n" +
    "              </tr>\n" +
    "            </table>\n" +
    "            <form ng-submit=\"leaveOrganization(selectedOrg)\">\n" +
    "              <input type=\"submit\" name=\"button-leave-org\" id=\"button-leave-org\" title=\"Can only leave if organization has more than 1 user.\" ng-disabled=\"!doesOrgHaveUsers(selectedOrg)\" value=\"Leave Organization\" class=\"btn btn-primary pull-right\">\n" +
    "            </form>\n" +
    "          </div>\n" +
    "        </div>\n" +
    "\n" +
    "      </div>\n" +
    "        </div>\n" +
    "</div>"
  );


  $templateCache.put('profile/profile.html',
    "\n" +
    "<div class=\"content-page\" ng-controller=\"ProfileCtrl\">\n" +
    "<page-title title=\" Profile\" icon=\"&#59170\"></page-title>\n" +
    "\n" +
    "  <div id=\"account-panels\">\n" +
    "    <div class=\"panel-content\">\n" +
    "      <div class=\"console-section\">\n" +
    "        <div class=\"console-section-contents\">\n" +
    "          <form name=\"updateAccountForm\" id=\"update-account-form\" ng-submit=\"saveUserInfo()\" class=\"form-horizontal\">\n" +
    "            <fieldset>\n" +
    "              <div class=\"control-group\">\n" +
    "                <label id=\"update-account-id-label\" class=\"control-label\" for=\"update-account-id\">UUID</label>\n" +
    "                <div class=\"controls\">\n" +
    "                  <span id=\"update-account-id\" class=\"monospace\">{{user.uuid}}</span>\n" +
    "                </div>\n" +
    "              </div>\n" +
    "              <div class=\"control-group\">\n" +
    "                <label class=\"control-label\" for=\"update-account-username\">Username </label>\n" +
    "                <div class=\"controls\">\n" +
    "                  <input type=\"text\" ug-validate name=\"update-account-username\" required ng-pattern=\"usernameRegex\" id=\"update-account-username\" ng-attr-title=\"{{usernameRegexDescription}}\"  class=\"span4\" ng-model=\"user.username\" size=\"20\"/>\n" +
    "                </div>\n" +
    "              </div>\n" +
    "              <div class=\"control-group\">\n" +
    "                <label class=\"control-label\" for=\"update-account-name\">Name </label>\n" +
    "                <div class=\"controls\">\n" +
    "                  <input type=\"text\" ug-validate name=\"update-account-name\" id=\"update-account-name\" ng-pattern=\"nameRegex\" ng-attr-title=\"{{nameRegexDescription}}\" class=\"span4\" ng-model=\"user.name\" size=\"20\"/>\n" +
    "                </div>\n" +
    "              </div>\n" +
    "              <div class=\"control-group\">\n" +
    "                <label class=\"control-label\" for=\"update-account-email\"> Email</label>\n" +
    "                <div class=\"controls\">\n" +
    "                  <input type=\"email\" ug-validate required name=\"update-account-email\" ng-pattern=\"emailRegex\" ng-attr-title=\"{{emailRegexDescription}}\"  id=\"update-account-email\" class=\"span4\" ng-model=\"user.email\" size=\"20\"/>\n" +
    "                </div>\n" +
    "              </div>\n" +
    "              <div class=\"control-group\">\n" +
    "                <label class=\"control-label\" for=\"update-account-picture-img\">Picture <br />(from <a href=\"http://gravatar.com\">gravatar.com</a>) </label>\n" +
    "                <div class=\"controls\">\n" +
    "                  <img id=\"update-account-picture-img\" ng-src=\"{{user.profileImg}}\" width=\"50\" />\n" +
    "                </div>\n" +
    "              </div>\n" +
    "              <span class=\"help-block\">Leave blank any of the following to keep the current password unchanged</span>\n" +
    "              <br />\n" +
    "              <div class=\"control-group\">\n" +
    "                <label class=\"control-label\" for=\"old-account-password\">Old Password</label>\n" +
    "                <div class=\"controls\">\n" +
    "                  <input type=\"password\" ug-validate name=\"old-account-password\"  id=\"old-account-password\" class=\"span4\" ng-model=\"user.oldPassword\" size=\"20\"/>\n" +
    "                </div>\n" +
    "              </div>\n" +
    "              <div class=\"control-group\">\n" +
    "                <label class=\"control-label\" for=\"update-account-password\">New Password</label>\n" +
    "                <div class=\"controls\">\n" +
    "                  <input type=\"password\"  ug-validate name=\"update-account-password\" ng-pattern=\"passwordRegex\" ng-attr-title=\"{{passwordRegexDescription}}\" id=\"update-account-password\" class=\"span4\" ng-model=\"user.newPassword\" size=\"20\"/>\n" +
    "                </div>\n" +
    "              </div>\n" +
    "              <div class=\"control-group\" style=\"display:none\">\n" +
    "                <label class=\"control-label\" for=\"update-account-password-repeat\">Confirm New Password</label>\n" +
    "                <div class=\"controls\">\n" +
    "                  <input type=\"password\" ug-validate name=\"update-account-password-repeat\" ng-pattern=\"passwordRegex\" ng-attr-title=\"{{passwordRegexDescription}}\" id=\"update-account-password-repeat\" class=\"span4\" ng-model=\"user.newPasswordConfirm\" size=\"20\"/>\n" +
    "                </div>\n" +
    "              </div>\n" +
    "            </fieldset>\n" +
    "            <div class=\"form-actions\">\n" +
    "              <input type=\"submit\"  class=\"btn btn-primary\"  name=\"button-update-account\" ng-disabled=\"!updateAccountForm.$valid || loading\" id=\"button-update-account\" value=\"{{loading ? loadingText : 'Update'}}\"  class=\"btn btn-usergrid\"/>\n" +
    "            </div>\n" +
    "          </form>\n" +
    "        </div>\n" +
    "      </div>\n" +
    "    </div>\n" +
    "  </div>\n" +
    "</div>"
  );


  $templateCache.put('roles/roles-groups.html',
    "<div class=\"content-page\" ng-controller=\"RolesGroupsCtrl\">\n" +
    "\n" +
    "\n" +
    "  <bsmodal id=\"addRoleToGroup\"\n" +
    "           title=\"Add group to role\"\n" +
    "           close=\"hideModal\"\n" +
    "           closelabel=\"Cancel\"\n" +
    "           extrabutton=\"addRoleToGroupDialog\"\n" +
    "           extrabuttonlabel=\"Add\"\n" +
    "           ng-cloak>\n" +
    "    <div class=\"btn-group\">\n" +
    "      <a class=\"btn dropdown-toggle filter-selector\" data-toggle=\"dropdown\">\n" +
    "        <span class=\"filter-label\">{{$parent.path !== '' ? $parent.title : 'Select a group...'}}</span>\n" +
    "        <span class=\"caret\"></span>\n" +
    "      </a>\n" +
    "      <ul class=\"dropdown-menu\">\n" +
    "        <li ng-repeat=\"group in $parent.groupsTypeaheadValues\" class=\"filterItem\"><a ng-click=\"setRoleModal(group)\">{{group.title}}</a></li>\n" +
    "      </ul>\n" +
    "    </div>\n" +
    "  </bsmodal>\n" +
    "\n" +
    "  <bsmodal id=\"removeGroupFromRole\"\n" +
    "           title=\"Confirmation\"\n" +
    "           close=\"hideModal\"\n" +
    "           closelabel=\"Cancel\"\n" +
    "           extrabutton=\"removeGroupFromRoleDialog\"\n" +
    "           extrabuttonlabel=\"Leave\"\n" +
    "           ng-cloak>\n" +
    "    <p>Are you sure you want to remove the group from the role(s)?</p>\n" +
    "  </bsmodal>\n" +
    "\n" +
    "\n" +
    "  <div class=\"users-section\">\n" +
    "    <div class=\"button-strip\">\n" +
    "      <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('group add user button')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_roles_groups_add_group}}\" tooltip-placement=\"left\">(?)</a>\n" +
    "        <button class=\"btn btn-primary\" ng-click=\"showModal('addRoleToGroup')\">Add Group to Role</button>\n" +
    "        <button class=\"btn btn-primary\"  ng-disabled=\"!hasGroups || !valueSelected(rolesCollection.groups._list)\" ng-click=\"showModal('removeGroupFromRole')\">Remove Group(s) from Role</button>\n" +
    "    </div>\n" +
    "    <table class=\"table table-striped\">\n" +
    "      <tr class=\"table-header\">\n" +
    "        <td style=\"width: 30px;\"><input type=\"checkbox\" ng-show=\"hasGroups\" id=\"selectAllCheckBox\" ng-model=\"roleGroupsSelected\" ng-click=\"selectAllEntities(rolesCollection.groups._list,this,'roleGroupsSelected')\"></td>\n" +
    "        <td>Title</td>\n" +
    "        <td>Path</td>\n" +
    "      </tr>\n" +
    "      <tr class=\"zebraRows\" ng-repeat=\"group in rolesCollection.groups._list\">\n" +
    "        <td>\n" +
    "          <input\n" +
    "            type=\"checkbox\"\n" +
    "            ng-model=\"group.checked\"\n" +
    "            >\n" +
    "        </td>\n" +
    "        <td>{{group._data.title}}</td>\n" +
    "        <td>{{group._data.path}}</td>\n" +
    "      </tr>\n" +
    "    </table>\n" +
    "    <div style=\"padding: 10px 5px 10px 5px\">\n" +
    "      <button class=\"btn btn-primary\" ng-click=\"getPrevious()\" style=\"display:{{previous_display}}\">< Previous</button>\n" +
    "      <button class=\"btn btn-primary\" ng-click=\"getNext()\" style=\"display:{{next_display}}\" style=\"float:right;\">Next ></button>\n" +
    "    </div>\n" +
    "  </div>\n" +
    "</div>"
  );


  $templateCache.put('roles/roles-settings.html',
    "<div class=\"content-page\" ng-controller=\"RolesSettingsCtrl\">\n" +
    "  <bsmodal id=\"deletePermission\"\n" +
    "           title=\"Confirmation\"\n" +
    "           close=\"hideModal\"\n" +
    "           closelabel=\"Cancel\"\n" +
    "           extrabutton=\"deleteRolePermissionDialog\"\n" +
    "           extrabuttonlabel=\"Delete\"\n" +
    "           ng-cloak>\n" +
    "    <p>Are you sure you want to delete the permission(s)?</p>\n" +
    "  </bsmodal>\n" +
    "\n" +
    "\n" +
    "  <bsmodal id=\"addPermission\"\n" +
    "           title=\"New Permission\"\n" +
    "           close=\"hideModal\"\n" +
    "           closelabel=\"Cancel\"\n" +
    "           extrabutton=\"addRolePermissionDialog\"\n" +
    "           extrabuttonlabel=\"Add\"\n" +
    "           ng-cloak>\n" +
    "    <p>Path: <input ng-model=\"$parent.permissions.path\" placeholder=\"ex: /mydata\" required ng-pattern=\"pathRegex\" ng-attr-title=\"{{pathRegexDescription}}\" ug-validate id=\"rolePermissionsPath\"/> <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('users roles new permission path box')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_roles_roles_new_permission_path}}\" tooltip-placement=\"right\">(?)</a></p>\n" +
    "    <div class=\"control-group\">\n" +
    "      <input type=\"checkbox\" ng-model=\"$parent.permissions.getPerm\"> GET <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('users roles add permission verbs')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_roles_new_permission_verbs}}\" tooltip-placement=\"right\">(?)</a>\n" +
    "    </div>\n" +
    "    <div class=\"control-group\">\n" +
    "      <input type=\"checkbox\" ng-model=\"$parent.permissions.postPerm\"> POST\n" +
    "    </div>\n" +
    "    <div class=\"control-group\">\n" +
    "      <input type=\"checkbox\" ng-model=\"$parent.permissions.putPerm\"> PUT\n" +
    "    </div>\n" +
    "    <div class=\"control-group\">\n" +
    "      <input type=\"checkbox\" ng-model=\"$parent.permissions.deletePerm\"> DELETE\n" +
    "    </div>\n" +
    "  </bsmodal>\n" +
    "\n" +
    "  <div>\n" +
    "    <h4>Inactivity <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('users roles inactivity')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_roles_inactivity}}\" tooltip-placement=\"right\">(?)</a></h4>\n" +
    "    <div id=\"role-permissions\">\n" +
    "        <p>Integer only. 0 (zero) means no expiration.</p>\n" +
    "\n" +
    "        <form name=\"updateActivity\" ng-submit=\"updateInactivity()\" novalidate>\n" +
    "            Seconds: <input style=\"margin: 0\" type=\"number\" required name=\"role-inactivity\"\n" +
    "                            id=\"role-inactivity-input\" min=\"0\" ng-model=\"role._data.inactivity\" title=\"Please input a positive integer >= 0.\"  step = \"any\" ug-validate >\n" +
    "            <input type=\"submit\" class=\"btn btn-primary\" ng-disabled=\"!updateActivity.$valid\" value=\"Set\"/>\n" +
    "        </form>\n" +
    "    </div>\n" +
    "\n" +
    "    <br/>\n" +
    "    <div class=\"button-strip\">\n" +
    "      <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('roles details add permission button')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_roles_add_delete_permission}}\" tooltip-placement=\"left\">(?)</a>\n" +
    "      <button class=\"btn btn-primary\" ng-click=\"showModal('addPermission')\">Add Permission</button>\n" +
    "      <button class=\"btn btn-primary\"  ng-disabled=\"!hasSettings || !valueSelected(role.permissions)\" ng-click=\"showModal('deletePermission')\">Delete Permission(s)</button>\n" +
    "    </div>\n" +
    "\n" +
    "    <h4>Permissions <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('roles settings permissions list')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_roles_permissions}}\" tooltip-placement=\"top\">(?)</a></h4>\n" +
    "    <table class=\"table table-striped\">\n" +
    "      <tbody>\n" +
    "      <tr class=\"table-header\">\n" +
    "        <td style=\"width: 30px;\"><input type=\"checkbox\" ng-show=\"hasSettings\" id=\"selectAllCheckBox\" ng-model=\"permissionsSelected\" ng-click=\"selectAllEntities(role.permissions,this,'permissionsSelected')\" ></td>\n" +
    "        <td>Path</td>\n" +
    "        <td>GET</td>\n" +
    "        <td>POST</td>\n" +
    "        <td>PUT</td>\n" +
    "        <td>DELETE</td>\n" +
    "      </tr>\n" +
    "      <tr class=\"zebraRows\" ng-repeat=\"permission in role.permissions\">\n" +
    "        <td>\n" +
    "          <input\n" +
    "            type=\"checkbox\"\n" +
    "            ng-model=\"permission.checked\"\n" +
    "            >\n" +
    "        </td>\n" +
    "        <td>{{permission.path}}</td>\n" +
    "        <td>{{permission.operations.get}}</td>\n" +
    "        <td>{{permission.operations.post}}</td>\n" +
    "        <td>{{permission.operations.put}}</td>\n" +
    "        <td>{{permission.operations.delete}}</td>\n" +
    "      </tr>\n" +
    "      </tbody>\n" +
    "    </table>\n" +
    "  </div>\n" +
    "</div>"
  );


  $templateCache.put('roles/roles-tabs.html',
    "<div class=\"content-page\">\n" +
    "  <section class=\"row-fluid\">\n" +
    "\n" +
    "    <div class=\"span12\">\n" +
    "      <div class=\"page-filters\">\n" +
    "        <h1 class=\"title\" class=\"pull-left\"><i class=\"pictogram title\">&#59170;</i> Roles </h1>\n" +
    "        <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('roles page title')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_roles_page_title}}\" tooltip-placement=\"right\">(?)</a>\n" +
    "      </div>\n" +
    "    </div>\n" +
    "\n" +
    "  </section>\n" +
    "\n" +
    "  <div id=\"user-panel\" class=\"panel-buffer\">\n" +
    "    <ul id=\"user-panel-tab-bar\" class=\"nav nav-tabs\">\n" +
    "      <li><a href=\"javaScript:void(0);\" ng-click=\"gotoPage('roles')\">Roles List</a></li>\n" +
    "      <li ng-class=\"settingsSelected\"><a href=\"javaScript:void(0);\" ng-click=\"gotoPage('roles/settings')\">Settings</a></li>\n" +
    "      <li ng-class=\"usersSelected\"><a href=\"javaScript:void(0);\" ng-click=\"gotoPage('roles/users')\">Users</a></li>\n" +
    "      <li ng-class=\"groupsSelected\"><a href=\"javaScript:void(0);\" ng-click=\"gotoPage('roles/groups')\">Groups</a></li>\n" +
    "    </ul>\n" +
    "  </div>\n" +
    "\n" +
    "  <div style=\"float: left; margin-right: 10px;\">\n" +
    "    <div style=\"float: left;\">\n" +
    "      <div class=\"user-header-title\"><strong>Role Name: </strong>{{selectedRole.name}}</div>\n" +
    "      <div class=\"user-header-title\"><strong>Role Title: </strong>{{selectedRole.title}}</div>\n" +
    "    </div>\n" +
    "  </div>\n" +
    "\n" +
    "</div>\n" +
    "<br>\n" +
    "<br>"
  );


  $templateCache.put('roles/roles-users.html',
    "<div class=\"content-page\" ng-controller=\"RolesUsersCtrl\">\n" +
    "  <bsmodal id=\"removeFromRole\"\n" +
    "           title=\"Confirmation\"\n" +
    "           close=\"hideModal\"\n" +
    "           closelabel=\"Cancel\"\n" +
    "           extrabutton=\"removeUsersFromGroupDialog\"\n" +
    "           extrabuttonlabel=\"Delete\"\n" +
    "           ng-cloak>\n" +
    "    <p>Are you sure you want to remove the users from the seleted role(s)?</p>\n" +
    "  </bsmodal>\n" +
    "\n" +
    "  <bsmodal id=\"addRoleToUser\"\n" +
    "           title=\"Add user to role\"\n" +
    "           close=\"hideModal\"\n" +
    "           closelabel=\"Cancel\"\n" +
    "           extrabutton=\"addRoleToUserDialog\"\n" +
    "           extrabuttonlabel=\"Add\"\n" +
    "           ng-cloak>\n" +
    "    <div class=\"btn-group\">\n" +
    "      <a class=\"btn dropdown-toggle filter-selector\" data-toggle=\"dropdown\">\n" +
    "        <span class=\"filter-label\">{{$parent.user.username ? $parent.user.username : 'Select a user...'}}</span>\n" +
    "        <span class=\"caret\"></span>\n" +
    "      </a>\n" +
    "      <ul class=\"dropdown-menu\">\n" +
    "        <li ng-repeat=\"user in $parent.usersTypeaheadValues\" class=\"filterItem\"><a ng-click=\"$parent.$parent.user = user\">{{user.username}}</a></li>\n" +
    "      </ul>\n" +
    "    </div>\n" +
    "  </bsmodal>\n" +
    "\n" +
    "  <div class=\"users-section\">\n" +
    "    <div class=\"button-strip\">\n" +
    "      <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('roles add user button')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_roles_users_add_user}}\" tooltip-placement=\"left\">(?)</a>\n" +
    "        <button class=\"btn btn-primary\" ng-click=\"showModal('addRoleToUser')\">Add User to Role</button>\n" +
    "        <button class=\"btn btn-primary\"  ng-disabled=\"!hasUsers || !valueSelected(rolesCollection.users._list)\" ng-click=\"showModal('removeFromRole')\">Remove User(s) from Role</button>\n" +
    "    </div>\n" +
    "    <table class=\"table table-striped\">\n" +
    "      <tr class=\"table-header\">\n" +
    "        <td style=\"width: 30px;\"><input type=\"checkbox\" ng-show=\"hasUsers\" id=\"selectAllCheckBox\" ng-model=\"roleUsersSelected\" ng-click=\"selectAllEntities(rolesCollection.users._list,this,'roleUsersSelected')\" ng-model=\"master\"></td>\n" +
    "        <td style=\"width: 50px;\"></td>\n" +
    "        <td>Username</td>\n" +
    "        <td>Display Name</td>\n" +
    "      </tr>\n" +
    "      <tr class=\"zebraRows\" ng-repeat=\"user in rolesCollection.users._list\">\n" +
    "        <td>\n" +
    "          <input\n" +
    "            type=\"checkbox\"\n" +
    "            ng-model=\"user.checked\"\n" +
    "            >\n" +
    "        </td>\n" +
    "        <td><img style=\"width:30px;height:30px;\" ng-src=\"{{user._portal_image_icon}}\"></td>\n" +
    "        <td>{{user._data.username}}</td>\n" +
    "        <td>{{user._data.name}}</td>\n" +
    "      </tr>\n" +
    "    </table>\n" +
    "    <div style=\"padding: 10px 5px 10px 5px\">\n" +
    "      <button class=\"btn btn-primary\" ng-click=\"getPrevious()\" style=\"display:{{previous_display}}\">< Previous</button>\n" +
    "      <button class=\"btn btn-primary\" ng-click=\"getNext()\" style=\"display:{{next_display}}; float:right;\">Next ></button>\n" +
    "    </div>\n" +
    "  </div>\n" +
    "</div>"
  );


  $templateCache.put('roles/roles.html',
    "<div class=\"content-page\">\n" +
    "  <div id=\"intro-page\" >\n" +
    "    <page-title title=\" Roles\" icon=\"&#59170;\"></page-title>\n" +
    "  </div>\n" +
    "\n" +
    "  <bsmodal id=\"newRole\"\n" +
    "           title=\"New Role\"\n" +
    "           close=\"hideModal\"\n" +
    "           closelabel=\"Cancel\"\n" +
    "           extrabutton=\"newRoleDialog\"\n" +
    "           extrabuttonlabel=\"Create\"\n" +
    "           buttonid=\"roles\"\n" +
    "           ng-cloak>\n" +
    "          <fieldset>\n" +
    "            <div class=\"control-group\">\n" +
    "              <label for=\"new-role-roletitle\">Title</label>\n" +
    "              <div class=\"controls\">\n" +
    "                <input type=\"text\" ng-pattern=\"titleRegex\" ng-attr-title=\"{{titleRegexDescription}}\" required ng-model=\"$parent.newRole.title\" name=\"roletitle\" id=\"new-role-roletitle\" class=\"input-xlarge\" ug-validate/>\n" +
    "                <p class=\"help-block hide\"></p>\n" +
    "              </div>\n" +
    "            </div>\n" +
    "            <div class=\"control-group\">\n" +
    "              <label for=\"new-role-rolename\">Role Name</label>\n" +
    "              <div class=\"controls\">\n" +
    "                <input type=\"text\" required ng-pattern=\"roleNameRegex\" ng-attr-title=\"{{roleNameRegexDescription}}\" ng-model=\"$parent.newRole.name\" name=\"rolename\" id=\"new-role-rolename\" class=\"input-xlarge\" ug-validate/>\n" +
    "                <p class=\"help-block hide\"></p>\n" +
    "              </div>\n" +
    "            </div>\n" +
    "          </fieldset>\n" +
    "  </bsmodal>\n" +
    "\n" +
    "  <bsmodal id=\"deleteRole\"\n" +
    "           title=\"Delete Role\"\n" +
    "           close=\"hideModal\"\n" +
    "           closelabel=\"Cancel\"\n" +
    "           buttonid=\"deleteroles\"\n" +
    "           extrabutton=\"deleteRoleDialog\"\n" +
    "           extrabuttonlabel=\"Delete\"\n" +
    "           ng-cloak>\n" +
    "    <p>Are you sure you want to delete the role(s)?</p>\n" +
    "  </bsmodal>\n" +
    "\n" +
    "  <section class=\"row-fluid\">\n" +
    "    <div id=\"intro-list\" class=\"span3 user-col\">\n" +
    "\n" +
    "      <div class=\"button-toolbar span12\">\n" +
    "        <a title=\"Select All\" class=\"btn btn-primary select-all toolbar\" ng-show=\"hasRoles\" ng-click=\"selectAllEntities(rolesCollection._list,this,'rolesSelected',true)\"> <i class=\"pictogram\">&#8863;</i></a>\n" +
    "        <button id=\"delete-role-btn\" title=\"Delete\" class=\"btn btn-primary toolbar\"  ng-disabled=\"!hasRoles || !valueSelected(rolesCollection._list)\" ng-click=\"showModal('deleteRole')\"><i class=\"pictogram\">&#9749;</i></button>\n" +
    "        <button id=\"add-role-btn\" title=\"Add\" class=\"btn btn-primary toolbar\" ng-click=\"showModal('newRole')\"><i class=\"pictogram\">&#59136;</i></button>\n" +
    "        <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('users add remove buttons')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_roles_add_remove_buttons}}\" tooltip-placement=\"right\">(?)</a>      </div>\n" +
    "\n" +
    "      <ul class=\"user-list\">\n" +
    "        <li ng-class=\"selectedRole._data.uuid === role._data.uuid ? 'selected' : ''\" ng-repeat=\"role in rolesCollection._list\" ng-click=\"selectRole(role._data.uuid)\">\n" +
    "          <input\n" +
    "              type=\"checkbox\"\n" +
    "              ng-value=\"role.get('uuid')\"\n" +
    "              ng-checked=\"master\"\n" +
    "              ng-model=\"role.checked\"\n" +
    "              id=\"role-{{role.get('title')}}-cb\"\n" +
    "              >\n" +
    "          <a id=\"role-{{role.get('title')}}-link\">{{role.get('title')}}</a>\n" +
    "          <br/>\n" +
    "          <span ng-if=\"role.get('name')\" class=\"label\">Role Name:</span>{{role.get('name')}}\n" +
    "        </li>\n" +
    "      </ul>\n" +
    "\n" +
    "\n" +
    "\n" +
    "  <div style=\"padding: 10px 5px 10px 5px\">\n" +
    "    <button class=\"btn btn-primary\" ng-click=\"getPrevious()\" style=\"display:{{previous_display}}\">< Previous</button>\n" +
    "    <button class=\"btn btn-primary\" ng-click=\"getNext()\" style=\"display:{{next_display}};float:right;\">Next ></button>\n" +
    "  </div>\n" +
    "\n" +
    "    </div>\n" +
    "    \n" +
    "    <div id=\"intro-information-tabs\" class=\"span9 tab-content\" ng-show=\"hasRoles\">\n" +
    "      <div class=\"menu-toolbar\">\n" +
    "        <ul class=\"inline\">\n" +
    "          <li class=\"tab\" ng-class=\"currentRolesPage.route === '/roles/settings' ? 'selected' : ''\"><div class=\"btn btn-primary toolbar\" ><a class=\"btn-content\" ng-click=\"selectRolePage('/roles/settings')\"><i class=\"pictogram\">&#59170;</i>Settings</a> <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('roles settings tab')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_roles_settings_tab}}\" tooltip-placement=\"right\">(?)</a></div></li>\n" +
    "          <li class=\"tab\" ng-class=\"currentRolesPage.route === '/roles/users' ? 'selected' : ''\"><div class=\"btn btn-primary toolbar\" ><a class=\"btn-content\" ng-click=\"selectRolePage('/roles/users')\"><i class=\"pictogram\">&#128101;</i>Users</a> <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('roles users tab')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_roles_users_tab}}\" tooltip-placement=\"right\">(?)</a></div></li>\n" +
    "          <li class=\"tab\" ng-class=\"currentRolesPage.route === '/roles/groups' ? 'selected' : ''\"><div class=\"btn btn-primary toolbar\" ><a class=\"btn-content\" ng-click=\"selectRolePage('/roles/groups')\"><i class=\"pictogram\">&#59194;</i>Groups</a> <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('roles groups tab')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_roles_groups_tab}}\" tooltip-placement=\"right\">(?)</a></div></li>\n" +
    "        </ul>\n" +
    "      </div>\n" +
    "      <span ng-include=\"currentRolesPage.template\"></span>\n" +
    "    </div>\n" +
    "  </section>\n" +
    "</div>"
  );


  $templateCache.put('shell/shell.html',
    "<page-title title=\" Shell\" icon=\"&#128241;\"></page-title>\n" +
    "\n" +
    "<section class=\"row-fluid\">\n" +
    "  <div class=\"console-section-contents\" id=\"shell-panel\">\n" +
    "    <div id=\"shell-input-div\">\n" +
    "      <p> Type \"help\" to view a list of the available commands.</p>\n" +
    "      <hr>\n" +
    "\n" +
    "      <form name=\"shellForm\" ng-submit=\"submitCommand()\" >\n" +
    "        <span>&nbsp;&gt;&gt; </span>\n" +
    "        <input  type=\"text\" id=\"shell-input\"  ng-model=\"shell.input\" autofocus=\"autofocus\" required\n" +
    "                  ng-form=\"shellForm\">\n" +
    "        <input style=\"display: none\" type=\"submit\" ng-form=\"shellForm\" value=\"submit\" ng-disabled=\"!shell.input\"/>\n" +
    "      </form>\n" +
    "    </div>\n" +
    "    <pre id=\"shell-output\" class=\"prettyprint lang-js\" style=\"overflow-x: auto; height: 400px;\" ng-bind-html=\"shell.output\">\n" +
    "\n" +
    "    </pre>\n" +
    "    <div id=\"lastshelloutput\" ng-bind-html=\"shell.outputhidden\" style=\"visibility:hidden\"></div>\n" +
    "  </div>\n" +
    "</section>\n"
  );


  $templateCache.put('users/users-activities.html',
    "<div class=\"content-page\" ng-controller=\"UsersActivitiesCtrl\" >\n" +
    "\n" +
    "  <bsmodal id=\"addActivityToUser\"\n" +
    "           title=\"Add activity to user\"\n" +
    "           close=\"hideModal\"\n" +
    "           closelabel=\"Cancel\"\n" +
    "           extrabutton=\"addActivityToUserDialog\"\n" +
    "           extrabuttonlabel=\"Add\"\n" +
    "           ng-cloak>\n" +
    "    <p>Content: <input id=\"activityMessage\" ng-model=\"$parent.newActivity.activityToAdd\" required name=\"activityMessage\" ug-validate /> <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('users add activity content')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_activities_add_activity_content}}\" tooltip-placement=\"right\">(?)</a></p>\n" +
    "  </bsmodal>\n" +
    "\n" +
    "\n" +
    "  <div ng:include=\"'users/users-tabs.html'\"></div>\n" +
    "  <br>\n" +
    "    <div class=\"button-strip\"><a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('users add activity button')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_activities_add_activity}}\" tooltip-placement=\"left\">(?)</a> \n" +
    "    <button class=\"btn btn-primary\" ng-click=\"showModal('addActivityToUser')\">Add activity to user</button>\n" +
    "  </div>\n" +
    "  <div>\n" +
    "    <table class=\"table table-striped\">\n" +
    "      <tbody>\n" +
    "      <tr class=\"table-header\">\n" +
    "        <td>Date</td>\n" +
    "        <td>Content</td>\n" +
    "        <td>Verb</td>\n" +
    "        <td>UUID</td>\n" +
    "      </tr>\n" +
    "      <tr class=\"zebraRows\" ng-repeat=\"activity in activities\">\n" +
    "        <td>{{activity.createdDate}}</td>\n" +
    "        <td>{{activity.content}}</td>\n" +
    "        <td>{{activity.verb}}</td>\n" +
    "        <td>{{activity.uuid}}</td>\n" +
    "      </tr>\n" +
    "      </tbody>\n" +
    "    </table>\n" +
    "  </div>\n" +
    "\n" +
    "\n" +
    "</div>\n"
  );


  $templateCache.put('users/users-feed.html',
    "<div class=\"content-page\" ng-controller=\"UsersFeedCtrl\" >\n" +
    "\n" +
    "    <div ng:include=\"'users/users-tabs.html'\"></div>\n" +
    "    <br>\n" +
    "    <div>\n" +
    "        <table class=\"table table-striped\">\n" +
    "            <tbody>\n" +
    "            <tr class=\"table-header\">\n" +
    "                <td>Date</td>\n" +
    "                <td>User</td>\n" +
    "                <td>Content</td>\n" +
    "                <td>Verb</td>\n" +
    "                <td>UUID</td>\n" +
    "            </tr>\n" +
    "            <tr class=\"zebraRows\" ng-repeat=\"activity in activities\">\n" +
    "                <td>{{activity.createdDate}}</td>\n" +
    "                <td>{{activity.actor.displayName}}</td>\n" +
    "                <td>{{activity.content}}</td>\n" +
    "                <td>{{activity.verb}}</td>\n" +
    "                <td>{{activity.uuid}}</td>\n" +
    "            </tr>\n" +
    "            </tbody>\n" +
    "        </table>\n" +
    "    </div>\n" +
    "\n" +
    "\n" +
    "</div>\n"
  );


  $templateCache.put('users/users-graph.html',
    "<div class=\"content-page\" ng-controller=\"UsersGraphCtrl\">\n" +
    "\n" +
    "  <div ng:include=\"'users/users-tabs.html'\"></div>\n" +
    "\n" +
    "  <div>\n" +
    "\n" +
    "    <bsmodal id=\"followUser\"\n" +
    "             title=\"Follow User\"\n" +
    "             close=\"hideModal\"\n" +
    "             closelabel=\"Cancel\"\n" +
    "             extrabutton=\"followUserDialog\"\n" +
    "             extrabuttonlabel=\"Add\"\n" +
    "             ng-cloak>\n" +
    "      <div class=\"btn-group\">\n" +
    "        <a class=\"btn dropdown-toggle filter-selector\" data-toggle=\"dropdown\">\n" +
    "          <span class=\"filter-label\">{{$parent.user != '' ? $parent.user.username : 'Select a user...'}}</span>\n" +
    "          <span class=\"caret\"></span>\n" +
    "        </a>\n" +
    "        <ul class=\"dropdown-menu\">\n" +
    "          <li ng-repeat=\"user in $parent.usersTypeaheadValues\" class=\"filterItem\"><a ng-click=\"$parent.$parent.user = user\">{{user.username}}</a></li>\n" +
    "        </ul>\n" +
    "      </div>\n" +
    "    </bsmodal>\n" +
    "\n" +
    "\n" +
    "    <div class=\"button-strip\">\n" +
    "      <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('users follow user')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_graph_follow_user}}\" tooltip-placement=\"left\">(?)</a> \n" +
    "      <button class=\"btn btn-primary\" ng-click=\"showModal('followUser')\">Follow User</button>\n" +
    "    </div>\n" +
    "    <br>\n" +
    "      <h4>Following <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('users following list')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_graph_following}}\" tooltip-placement=\"right\">(?)</a></h4>\n" +
    "    <table class=\"table table-striped\">\n" +
    "      <tbody>\n" +
    "      <tr class=\"table-header\">\n" +
    "        <td>Image</td>\n" +
    "        <td>Username</td>\n" +
    "        <td>Email</td>\n" +
    "        <td>UUID</td>\n" +
    "      </tr>\n" +
    "      <tr class=\"zebraRows\" ng-repeat=\"user in selectedUser.following\">\n" +
    "        <td><img style=\"width:30px;height:30px;\" ng-src=\"{{user._portal_image_icon}}\"></td>\n" +
    "        <td>{{user.username}}</td>\n" +
    "        <td>{{user.email}}</td>\n" +
    "        <td>{{user.uuid}}</td>\n" +
    "      </tr>\n" +
    "      </tbody>\n" +
    "    </table>\n" +
    "\n" +
    "      <h4>Followers <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('users followers list')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_graph_followers}}\" tooltip-placement=\"right\">(?)</a></h4>\n" +
    "    <table class=\"table table-striped\">\n" +
    "      <tbody>\n" +
    "      <tr class=\"table-header\">\n" +
    "        <td>Image</td>\n" +
    "        <td>Username</td>\n" +
    "        <td>Email</td>\n" +
    "        <td>UUID</td>\n" +
    "      </tr>\n" +
    "      <tr class=\"zebraRows\" ng-repeat=\"user in selectedUser.followers\">\n" +
    "        <td><img style=\"width:30px;height:30px;\" ng-src=\"{{user._portal_image_icon}}\"></td>\n" +
    "        <td>{{user.username}}</td>\n" +
    "        <td>{{user.email}}</td>\n" +
    "        <td>{{user.uuid}}</td>\n" +
    "      </tr>\n" +
    "      </tbody>\n" +
    "    </table>\n" +
    "\n" +
    "  </div>\n" +
    "</div>"
  );


  $templateCache.put('users/users-groups.html',
    "<div class=\"content-page\" ng-controller=\"UsersGroupsCtrl\">\n" +
    "\n" +
    "  <div ng:include=\"'users/users-tabs.html'\"></div>\n" +
    "\n" +
    "  <div>\n" +
    "\n" +
    "    <bsmodal id=\"addUserToGroup\"\n" +
    "             title=\"Add user to group\"\n" +
    "             close=\"hideModal\"\n" +
    "             closelabel=\"Cancel\"\n" +
    "             extrabutton=\"addUserToGroupDialog\"\n" +
    "             extrabuttonlabel=\"Add\"\n" +
    "             ng-cloak>\n" +
    "      <div class=\"btn-group\">\n" +
    "        <a class=\"btn dropdown-toggle filter-selector\" data-toggle=\"dropdown\">\n" +
    "          <span class=\"filter-label\">{{$parent.title && $parent.title !== '' ? $parent.title : 'Select a group...'}}</span>\n" +
    "          <span class=\"caret\"></span>\n" +
    "        </a>\n" +
    "        <ul class=\"dropdown-menu\">\n" +
    "          <li ng-repeat=\"group in $parent.groupsTypeaheadValues\" class=\"filterItem\"><a ng-click=\"selectGroup(group)\">{{group.title}}</a></li>\n" +
    "        </ul>\n" +
    "      </div>\n" +
    "    </bsmodal>\n" +
    "\n" +
    "    <bsmodal id=\"leaveGroup\"\n" +
    "             title=\"Confirmation\"\n" +
    "             close=\"hideModal\"\n" +
    "             closelabel=\"Cancel\"\n" +
    "             extrabutton=\"leaveGroupDialog\"\n" +
    "             extrabuttonlabel=\"Leave\"\n" +
    "             ng-cloak>\n" +
    "      <p>Are you sure you want to remove the user from the seleted group(s)?</p>\n" +
    "    </bsmodal>\n" +
    "\n" +
    "    <div class=\"button-strip\">\n" +
    "      <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('users add leave group buttons')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_groups_add_leave_buttons}}\" tooltip-placement=\"left\">(?)</a>\n" +
    "      <button class=\"btn btn-primary\" ng-click=\"showModal('addUserToGroup')\">Add to group</button>\n" +
    "      <button class=\"btn btn-primary\" ng-disabled=\"!hasGroups || !valueSelected(userGroupsCollection._list)\" ng-click=\"showModal('leaveGroup')\">Leave group(s)</button>\n" +
    "    </div>\n" +
    "    <table class=\"table table-striped\">\n" +
    "      <tbody>\n" +
    "      <tr class=\"table-header\">\n" +
    "        <td>\n" +
    "          <input type=\"checkbox\" ng-show=\"hasGroups\" id=\"selectAllCheckBox\" ng-model=\"userGroupsSelected\" ng-click=\"selectAllEntities(userGroupsCollection._list,this,'userGroupsSelected')\" >\n" +
    "        </td>\n" +
    "        <td>Group Name <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('add user group list')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_groups_group_name}}\" tooltip-placement=\"top\">(?)</a></td>\n" +
    "        <td>Path</td>\n" +
    "      </tr>\n" +
    "      <tr class=\"zebraRows\" ng-repeat=\"group in userGroupsCollection._list\">\n" +
    "        <td>\n" +
    "          <input\n" +
    "            type=\"checkbox\"\n" +
    "            ng-value=\"group.get('uuid')\"\n" +
    "            ng-model=\"group.checked\"\n" +
    "            >\n" +
    "        </td>\n" +
    "        <td>{{group.get('title')}}</td>\n" +
    "        <td>{{group.get('path')}}</td>\n" +
    "      </tr>\n" +
    "      </tbody>\n" +
    "    </table>\n" +
    "  </div>\n" +
    "  <div style=\"padding: 10px 5px 10px 5px\">\n" +
    "    <button class=\"btn btn-primary\" ng-click=\"getPrevious()\" style=\"display:{{previous_display}}\">< Previous</button>\n" +
    "    <button class=\"btn btn-primary\" ng-click=\"getNext()\" style=\"display:{{next_display}}; float:right;\">Next ></button>\n" +
    "  </div>\n" +
    "\n" +
    "</div>\n"
  );


  $templateCache.put('users/users-profile.html',
    "<div class=\"content-page\" ng-controller=\"UsersProfileCtrl\">\n" +
    "\n" +
    "  <div ng:include=\"'users/users-tabs.html'\"></div>\n" +
    "\n" +
    "  <div class=\"row-fluid\">\n" +
    "\n" +
    "  <form ng-submit=\"saveSelectedUser()\" name=\"profileForm\" novalidate>\n" +
    "    <div class=\"span6\">\n" +
    "      <h4>User Information <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('users profile information')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_profile_information}}\" tooltip-placement=\"top\">(?)</a></h4>\n" +
    "      <label for=\"ui-form-username\" class=\"ui-dform-label\">Username</label>\n" +
    "      <input type=\"text\" ug-validate required  name=\"ui-form-username\" ng-pattern=\"usernameRegex\" ng-attr-title=\"{{usernameRegexDescription}}\" id=\"ui-form-username\" class=\"ui-dform-text\" ng-model=\"user.username\">\n" +
    "      <br/>\n" +
    "      <label for=\"ui-form-name\" class=\"ui-dform-label\">Full Name</label>\n" +
    "      <input type=\"text\" ug-validate ng-pattern=\"nameRegex\" ng-attr-title=\"{{nameRegexDescription}}\" required name=\"ui-form-name\" id=\"ui-form-name\" class=\"ui-dform-text\" ng-model=\"user.name\">\n" +
    "      <br/>\n" +
    "      <label for=\"ui-form-title\" class=\"ui-dform-label\">Title</label>\n" +
    "      <input type=\"text\" ug-validate name=\"ui-form-title\" id=\"ui-form-title\" class=\"ui-dform-text\" ng-model=\"user.title\">\n" +
    "      <br/>\n" +
    "      <label for=\"ui-form-url\" class=\"ui-dform-label\">Home Page</label>\n" +
    "      <input type=\"url\" ug-validate name=\"ui-form-url\" id=\"ui-form-url\" title=\"Please enter a valid url.\" class=\"ui-dform-text\" ng-model=\"user.url\">\n" +
    "      <br/>\n" +
    "      <label for=\"ui-form-email\" class=\"ui-dform-label\">Email</label>\n" +
    "      <input type=\"email\" ug-validate required name=\"ui-form-email\" id=\"ui-form-email\"  ng-pattern=\"emailRegex\" ng-attr-title=\"{{emailRegexDescription}}\" class=\"ui-dform-text\" ng-model=\"user.email\">\n" +
    "      <br/>\n" +
    "      <label for=\"ui-form-tel\" class=\"ui-dform-label\">Telephone</label>\n" +
    "      <input type=\"tel\" ug-validate name=\"ui-form-tel\" id=\"ui-form-tel\" class=\"ui-dform-text\" ng-model=\"user.tel\">\n" +
    "      <br/>\n" +
    "      <label for=\"ui-form-picture\" class=\"ui-dform-label\">Picture URL</label>\n" +
    "      <input type=\"url\" ug-validate name=\"ui-form-picture\" id=\"ui-form-picture\" title=\"Please enter a valid url.\" ng class=\"ui-dform-text\" ng-model=\"user.picture\">\n" +
    "      <br/>\n" +
    "      <label for=\"ui-form-bday\" class=\"ui-dform-label\">Birthday</label>\n" +
    "      <input type=\"date\" ug-validate name=\"ui-form-bday\" id=\"ui-form-bday\" class=\"ui-dform-text\" ng-model=\"user.bday\">\n" +
    "      <br/>\n" +
    "    </div>\n" +
    "    <div class=\"span6\">\n" +
    "      <h4>Address</h4>\n" +
    "      <label for=\"ui-form-addr1\" class=\"ui-dform-label\">Street 1</label>\n" +
    "      <input type=\"text\" ug-validate name=\"ui-form-addr1\" id=\"ui-form-addr1\" class=\"ui-dform-text\" ng-model=\"user.adr.addr1\">\n" +
    "      <br/>\n" +
    "      <label for=\"ui-form-addr2\" class=\"ui-dform-label\">Street 2</label>\n" +
    "      <input type=\"text\" ug-validate name=\"ui-form-addr2\" id=\"ui-form-addr2\" class=\"ui-dform-text\" ng-model=\"user.adr.addr2\">\n" +
    "      <br/>\n" +
    "      <label for=\"ui-form-city\" class=\"ui-dform-label\">City</label>\n" +
    "      <input type=\"text\" ug-validate name=\"ui-form-city\" id=\"ui-form-city\" class=\"ui-dform-text\" ng-model=\"user.adr.city\">\n" +
    "      <br/>\n" +
    "      <label for=\"ui-form-state\" class=\"ui-dform-label\">State</label>\n" +
    "      <input type=\"text\" ug-validate name=\"ui-form-state\" id=\"ui-form-state\"  ng-attr-title=\"{{stateRegexDescription}}\" ng-pattern=\"stateRegex\" class=\"ui-dform-text\" ng-model=\"user.adr.state\">\n" +
    "      <br/>\n" +
    "      <label for=\"ui-form-zip\" class=\"ui-dform-label\">Zip</label>\n" +
    "      <input type=\"text\" ug-validate name=\"ui-form-zip\" ng-pattern=\"zipRegex\" ng-attr-title=\"{{zipRegexDescription}}\" id=\"ui-form-zip\" class=\"ui-dform-text\" ng-model=\"user.adr.zip\">\n" +
    "      <br/>\n" +
    "      <label for=\"ui-form-country\" class=\"ui-dform-label\">Country</label>\n" +
    "      <input type=\"text\" ug-validate name=\"ui-form-country\" ng-attr-title=\"{{countryRegexDescription}}\" ng-pattern=\"countryRegex\" id=\"ui-form-country\" class=\"ui-dform-text\" ng-model=\"user.adr.country\">\n" +
    "      <br/>\n" +
    "    </div>\n" +
    "\n" +
    "      <div class=\"span6\">\n" +
    "        <input type=\"submit\" class=\"btn btn-primary margin-35\" ng-disabled=\"!profileForm.$valid\"  value=\"Save User\"/>\n" +
    "      </div>\n" +
    "\n" +
    "\n" +
    "    <div class=\"content-container\">\n" +
    "      <legend>JSON User Object <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('users profile json')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_profile_json}}\" tooltip-placement=\"top\">(?)</a></legend>\n" +
    "      <pre id=\"{{help.showJsonId}}\">{{user.json}}</pre>\n" +
    "    </div>\n" +
    "    </form>\n" +
    "  </div>\n" +
    "\n" +
    "</div>\n"
  );


  $templateCache.put('users/users-roles.html',
    "<div class=\"content-page\" ng-controller=\"UsersRolesCtrl\">\n" +
    "\n" +
    "  <div ng:include=\"'users/users-tabs.html'\"></div>\n" +
    "\n" +
    "  <div>\n" +
    "\n" +
    "    <bsmodal id=\"addRole\"\n" +
    "             title=\"Add user to role\"\n" +
    "             close=\"hideModal\"\n" +
    "             closelabel=\"Cancel\"\n" +
    "             extrabutton=\"addUserToRoleDialog\"\n" +
    "             extrabuttonlabel=\"Add\"\n" +
    "             ng-cloak>\n" +
    "      <div class=\"btn-group\">\n" +
    "        <a class=\"btn dropdown-toggle filter-selector\" data-toggle=\"dropdown\">\n" +
    "          <span class=\"filter-label\">{{$parent.name != '' ? $parent.name : 'Select a Role...'}}</span>\n" +
    "          <span class=\"caret\"></span>\n" +
    "        </a>\n" +
    "        <ul class=\"dropdown-menu\">\n" +
    "          <li ng-repeat=\"role in $parent.rolesTypeaheadValues\" class=\"filterItem\"><a ng-click=\"$parent.$parent.name = role.name\">{{role.name}}</a></li>\n" +
    "        </ul>\n" +
    "      </div>\n" +
    "    </bsmodal>\n" +
    "\n" +
    "    <bsmodal id=\"leaveRole\"\n" +
    "             title=\"Confirmation\"\n" +
    "             close=\"hideModal\"\n" +
    "             closelabel=\"Cancel\"\n" +
    "             extrabutton=\"leaveRoleDialog\"\n" +
    "             extrabuttonlabel=\"Leave\"\n" +
    "             ng-cloak>\n" +
    "      <p>Are you sure you want to remove the user from the role(s)?</p>\n" +
    "    </bsmodal>\n" +
    "\n" +
    "<div ng-controller=\"UsersRolesCtrl\">\n" +
    "\n" +
    "<div class=\"button-strip\">\n" +
    "  <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('users roles add role button')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_roles_add_leave_role}}\" tooltip-placement=\"left\">(?)</a>\n" +
    "      <button class=\"btn btn-primary\" ng-click=\"showModal('addRole')\">Add Role</button>\n" +
    "      <button class=\"btn btn-primary\" ng-disabled=\"!hasRoles || !valueSelected(selectedUser.roles)\" ng-click=\"showModal('leaveRole')\">Leave role(s)</button>\n" +
    "    </div>\n" +
    "    <br>\n" +
    "      <h4>Roles <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('users roles roles list')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_roles_roles}}\" tooltip-placement=\"top\">(?)</a></h4>\n" +
    "    <table class=\"table table-striped\">\n" +
    "      <tbody>\n" +
    "      <tr class=\"table-header\">\n" +
    "        <td style=\"width: 30px;\"><input type=\"checkbox\" ng-show=\"hasRoles\" id=\"rolesSelectAllCheckBox\" ng-model=\"usersRolesSelected\" ng-click=\"selectAllEntities(selectedUser.roles,this,'usersRolesSelected',true)\" ></td>\n" +
    "        <td>Role Name</td>\n" +
    "        <td>Role title</td>\n" +
    "      </tr>\n" +
    "      <tr class=\"zebraRows\" ng-repeat=\"role in selectedUser.roles\">\n" +
    "        <td>\n" +
    "          <input\n" +
    "            type=\"checkbox\"\n" +
    "            ng-model=\"role.checked\"\n" +
    "            >\n" +
    "        </td>\n" +
    "        <td>{{role.name}}</td>\n" +
    "        <td>{{role.title}}</td>\n" +
    "      </tr>\n" +
    "      </tbody>\n" +
    "    </table>\n" +
    "\n" +
    "    <bsmodal id=\"deletePermission\"\n" +
    "             title=\"Confirmation\"\n" +
    "             close=\"hideModal\"\n" +
    "             closelabel=\"Cancel\"\n" +
    "             extrabutton=\"deletePermissionDialog\"\n" +
    "             extrabuttonlabel=\"Delete\"\n" +
    "             ng-cloak>\n" +
    "      <p>Are you sure you want to delete the permission(s)?</p>\n" +
    "    </bsmodal>\n" +
    "\n" +
    "    <bsmodal id=\"addPermission\"\n" +
    "             title=\"New Permission\"\n" +
    "             close=\"hideModal\"\n" +
    "             closelabel=\"Cancel\"\n" +
    "             extrabutton=\"addUserPermissionDialog\"\n" +
    "             extrabuttonlabel=\"Add\"\n" +
    "             ng-cloak>\n" +
    "      <p>Path: <input ng-model=\"$parent.permissions.path\" placeholder=\"ex: /mydata\" id=\"usersRolePermissions\" type=\"text\" ng-pattern=\"pathRegex\" required ug-validate ng-attr-title=\"{{pathRegexDescription}}\" /> <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('users roles new permission path box')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_roles_new_permission_path}}\" tooltip-placement=\"right\">(?)</a></p>\n" +
    "      <div class=\"control-group\">\n" +
    "        <input type=\"checkbox\" ng-model=\"$parent.permissions.getPerm\"> GET <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('users roles new permission verbs check boxes')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_roles_new_permission_verbs}}\" tooltip-placement=\"right\">(?)</a>\n" +
    "      </div>\n" +
    "      <div class=\"control-group\">\n" +
    "        <input type=\"checkbox\" ng-model=\"$parent.permissions.postPerm\"> POST\n" +
    "      </div>\n" +
    "      <div class=\"control-group\">\n" +
    "        <input type=\"checkbox\" ng-model=\"$parent.permissions.putPerm\"> PUT\n" +
    "      </div>\n" +
    "      <div class=\"control-group\">\n" +
    "        <input type=\"checkbox\" ng-model=\"$parent.permissions.deletePerm\"> DELETE\n" +
    "      </div>\n" +
    "    </bsmodal>\n" +
    "\n" +
    "    <div class=\"button-strip\">\n" +
    "      <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('users roles add permission button')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_roles_add_delete_permission}}\" tooltip-placement=\"left\">(?)</a>\n" +
    "      <button class=\"btn btn-primary\" ng-click=\"showModal('addPermission')\">Add Permission</button>\n" +
    "      <button class=\"btn btn-primary\" ng-disabled=\"!hasPermissions || !valueSelected(selectedUser.permissions)\" ng-click=\"showModal('deletePermission')\">Delete Permission(s)</button>\n" +
    "    </div>\n" +
    "    <br>\n" +
    "      <h4>Permissions <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('users roles permissions list')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_roles_permissions}}\" tooltip-placement=\"top\">(?)</a></h4>\n" +
    "    <table class=\"table table-striped\">\n" +
    "      <tbody>\n" +
    "      <tr class=\"table-header\">\n" +
    "        <td style=\"width: 30px;\"><input type=\"checkbox\" ng-show=\"hasPermissions\"  id=\"permissionsSelectAllCheckBox\" ng-model=\"usersPermissionsSelected\" ng-click=\"selectAllEntities(selectedUser.permissions,this,'usersPermissionsSelected',true)\"  ></td>\n" +
    "        <td>Path</td>\n" +
    "        <td>GET</td>\n" +
    "        <td>POST</td>\n" +
    "        <td>PUT</td>\n" +
    "        <td>DELETE</td>\n" +
    "      </tr>\n" +
    "      <tr class=\"zebraRows\" ng-repeat=\"permission in selectedUser.permissions\">\n" +
    "        <td>\n" +
    "          <input\n" +
    "            type=\"checkbox\"\n" +
    "            ng-model=\"permission.checked\"\n" +
    "            >\n" +
    "        </td>\n" +
    "        <td>{{permission.path}}</td>\n" +
    "        <td>{{permission.operations.get}}</td>\n" +
    "        <td>{{permission.operations.post}}</td>\n" +
    "        <td>{{permission.operations.put}}</td>\n" +
    "        <td>{{permission.operations.delete}}</td>\n" +
    "      </tr>\n" +
    "      </tbody>\n" +
    "    </table>\n" +
    "  </div>\n" +
    " </div>\n" +
    "\n" +
    "</div>\n"
  );


  $templateCache.put('users/users-tabs.html',
    "\n" +
    "\n" +
    "\n"
  );


  $templateCache.put('users/users.html',
    "<div class=\"content-page\">\n" +
    "\n" +
    "  <div id=\"intro-page\">    \n" +
    "    <page-title title=\" Users\" icon=\"&#128100;\"></page-title>\n" +
    "  </div>\n" +
    "  <bsmodal id=\"newUser\"\n" +
    "           title=\"Create New User\"\n" +
    "           close=\"hideModal\"\n" +
    "           closelabel=\"Cancel\"\n" +
    "           buttonid=\"users\"\n" +
    "           extrabutton=\"newUserDialog\"\n" +
    "           extrabuttonlabel=\"Create\"\n" +
    "           ng-cloak>\n" +
    "    <fieldset>\n" +
    "      <div class=\"control-group\">\n" +
    "        <label for=\"new-user-username\">Username</label>\n" +
    "\n" +
    "        <div class=\"controls\">\n" +
    "          <input type=\"text\" required ng-model=\"$parent.newUser.newusername\" ng-pattern=\"usernameRegex\" ng-attr-title=\"{{usernameRegexDescription}}\"  name=\"username\" id=\"new-user-username\" class=\"input-xlarge\" ug-validate/>\n" +
    "          <p class=\"help-block hide\"></p>\n" +
    "        </div>\n" +
    "      </div>\n" +
    "      <div class=\"control-group\">\n" +
    "        <label for=\"new-user-fullname\">Full name</label>\n" +
    "\n" +
    "        <div class=\"controls\">\n" +
    "          <input type=\"text\" required  ng-attr-title=\"{{nameRegexDescription}}\" ng-pattern=\"nameRegex\" ng-model=\"$parent.newUser.name\" name=\"name\" id=\"new-user-fullname\" class=\"input-xlarge\" ug-validate/>\n" +
    "\n" +
    "          <p class=\"help-block hide\"></p>\n" +
    "        </div>\n" +
    "      </div>\n" +
    "      <div class=\"control-group\">\n" +
    "        <label for=\"new-user-email\">Email</label>\n" +
    "\n" +
    "        <div class=\"controls\">\n" +
    "          <input type=\"email\" required  ng-model=\"$parent.newUser.email\" pattern=\"emailRegex\"   ng-attr-title=\"{{emailRegexDescription}}\"  name=\"email\" id=\"new-user-email\" class=\"input-xlarge\" ug-validate/>\n" +
    "\n" +
    "          <p class=\"help-block hide\"></p>\n" +
    "        </div>\n" +
    "      </div>\n" +
    "      <div class=\"control-group\">\n" +
    "        <label for=\"new-user-password\">Password</label>\n" +
    "\n" +
    "        <div class=\"controls\">\n" +
    "          <input type=\"password\" required ng-pattern=\"passwordRegex\"  ng-attr-title=\"{{passwordRegexDescription}}\" ng-model=\"$parent.newUser.newpassword\" name=\"password\" id=\"new-user-password\" ug-validate\n" +
    "                 class=\"input-xlarge\"/>\n" +
    "\n" +
    "          <p class=\"help-block hide\"></p>\n" +
    "        </div>\n" +
    "      </div>\n" +
    "      <div class=\"control-group\">\n" +
    "        <label for=\"new-user-re-password\">Confirm password</label>\n" +
    "\n" +
    "        <div class=\"controls\">\n" +
    "          <input type=\"password\" required ng-pattern=\"passwordRegex\"  ng-attr-title=\"{{passwordRegexDescription}}\" ng-model=\"$parent.newUser.repassword\" name=\"re-password\" id=\"new-user-re-password\" ug-validate\n" +
    "                 class=\"input-xlarge\"/>\n" +
    "\n" +
    "          <p class=\"help-block hide\"></p>\n" +
    "        </div>\n" +
    "      </div>\n" +
    "    </fieldset>\n" +
    "  </bsmodal>\n" +
    "\n" +
    "  <bsmodal id=\"deleteUser\"\n" +
    "           title=\"Delete User\"\n" +
    "           close=\"hideModal\"\n" +
    "           closelabel=\"Cancel\"\n" +
    "           extrabutton=\"deleteUsersDialog\"\n" +
    "           extrabuttonlabel=\"Delete\"\n" +
    "           buttonid=\"deleteusers\"\n" +
    "           ng-cloak>\n" +
    "    <p>Are you sure you want to delete the user(s)?</p>\n" +
    "  </bsmodal>\n" +
    "\n" +
    "  <section class=\"row-fluid\">\n" +
    "    <div id=\"intro-list\" class=\"span3 user-col\">\n" +
    "      \n" +
    "      <div class=\"button-toolbar span12\">\n" +
    "          <a title=\"Select All\" class=\"btn btn-primary toolbar select-all\" ng-show=\"hasUsers\" ng-click=\"selectAllEntities(usersCollection._list,this,'usersSelected',true)\" ng-model=\"usersSelected\"> <i class=\"pictogram\">&#8863;</i></a>\n" +
    "          <button title=\"Delete\" class=\"btn btn-primary toolbar\" ng-disabled=\"!hasUsers || !valueSelected(usersCollection._list)\" ng-click=\"showModal('deleteUser')\" id=\"delete-user-button\"><i class=\"pictogram\">&#9749;</i></button>\n" +
    "        <button title=\"Add\" class=\"btn btn-primary toolbar\" ng-click=\"showModal('newUser')\" id=\"new-user-button\" ng-attr-id=\"new-user-button\"><i class=\"pictogram\">&#59136;</i></button>\n" +
    "        <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('users add remove buttons')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_users_add_remove_buttons}}\" tooltip-placement=\"right\">(?)</a>\n" +
    "      </div>\n" +
    "      <ul class=\"user-list\">\n" +
    "          <li ng-class=\"selectedUser._data.uuid === user._data.uuid ? 'selected' : ''\" ng-repeat=\"user in usersCollection._list\" ng-click=\"selectUser(user._data.uuid)\">\n" +
    "            <input\n" +
    "                type=\"checkbox\"\n" +
    "                id=\"user-{{user.get('username')}}-checkbox\"\n" +
    "                ng-value=\"user.get('uuid')\"\n" +
    "                ng-checked=\"master\"\n" +
    "                ng-model=\"user.checked\"\n" +
    "                >\n" +
    "              <a href=\"javaScript:void(0)\"  id=\"user-{{user.get('username')}}-link\" >{{user.get('username')}}</a>\n" +
    "              <span ng-if=\"user.name\" class=\"label\">Display Name:</span>{{user.name}}\n" +
    "            </li>\n" +
    "        </ul>\n" +
    "        <div style=\"padding: 10px 5px 10px 5px\">\n" +
    "          <button class=\"btn btn-primary toolbar\" ng-click=\"getPrevious()\" style=\"display:{{previous_display}}\">< Previous\n" +
    "          </button>\n" +
    "          <button class=\"btn btn-primary toolbar\" ng-click=\"getNext()\" style=\"display:{{next_display}}; float:right;\">Next >\n" +
    "          </button>\n" +
    "        </div>\n" +
    "      \n" +
    "    </div>\n" +
    "\n" +
    "    <div id=\"{{help.showTabsId}}\" class=\"span9 tab-content\" ng-show=\"hasUsers\">\n" +
    "      <div class=\"menu-toolbar\">\n" +
    "        <ul class=\"inline\">\n" +
    "          <li class=\"tab\" ng-class=\"currentUsersPage.route === '/users/profile' ? 'selected' : ''\"><div class=\"btn btn-primary toolbar\" ><a class=\"btn-content\" ng-click=\"selectUserPage('/users/profile')\"><i class=\"pictogram\">&#59170;</i>Profile</a> <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('users profile tab')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_profile_tab}}\" tooltip-placement=\"right\">(?)</a></div></li>\n" +
    "          <li class=\"tab\" ng-class=\"currentUsersPage.route === '/users/groups' ? 'selected' : ''\"><div class=\"btn btn-primary toolbar\" ><a class=\"btn-content\" ng-click=\"selectUserPage('/users/groups')\"><i class=\"pictogram\">&#128101;</i>Groups</a> <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('users groups tab')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_groups_tab}}\" tooltip-placement=\"right\">(?)</a></div></li>\n" +
    "          <li class=\"tab\" ng-class=\"currentUsersPage.route === '/users/activities' ? 'selected' : ''\"><div class=\"btn btn-primary toolbar\" ><a class=\"btn-content\" ng-click=\"selectUserPage('/users/activities')\"><i class=\"pictogram\">&#59194;</i>Activities</a> <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('users activities tab')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_activities_tab}}\" tooltip-placement=\"right\">(?)</a></div></li>\n" +
    "          <li class=\"tab\" ng-class=\"currentUsersPage.route === '/users/feed' ? 'selected' : ''\"><div class=\"btn btn-primary toolbar\" ><a class=\"btn-content\" ng-click=\"selectUserPage('/users/feed')\"><i class=\"pictogram\">&#128196;</i>Feed</a> <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('users feed tab')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_feed_tab}}\" tooltip-placement=\"right\">(?)</a></div></li>\n" +
    "          <li class=\"tab\" ng-class=\"currentUsersPage.route === '/users/graph' ? 'selected' : ''\"><div class=\"btn btn-primary toolbar\" ><a class=\"btn-content\" ng-click=\"selectUserPage('/users/graph')\"><i class=\"pictogram\">&#9729;</i>Graph</a> <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('users graph tab')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_graph_tab}}\" tooltip-placement=\"top\">(?)</a></div></li>\n" +
    "          <li class=\"tab\" ng-class=\"currentUsersPage.route === '/users/roles' ? 'selected' : ''\"><div class=\"btn btn-primary toolbar\" ><a class=\"btn-content\" ng-click=\"selectUserPage('/users/roles')\"><i class=\"pictogram\">&#127758;</i>Roles &amp; Permissions</a> <a class=\"help_tooltip\" ng-mouseover=\"help.sendTooltipGA('users roles tab')\" ng-show=\"help.helpTooltipsEnabled\" href=\"#\" ng-attr-tooltip=\"{{tooltip_roles_tab}}\" tooltip-placement=\"top\">(?)</a></div></li>\n" +
    "        </ul>\n" +
    "      </div>\n" +
    "      <span ng-include=\"currentUsersPage.template\"></span>\n" +
    "    </div>\n" +
    "  </section>\n" +
    "</div>"
  );

}]);
