#App Services .NET Location-based sample application

##Setup Instructions
First, you must load the store data into a collection on your App Services org.  This is done with the following curl command:
	curl -X POST -d '@stores.json' https://api.usergrid.com/<YOUR ORGANIZATION>/<YOUR APP>/<YOUR COLLECTION>
Next you must make sure all of your references are in order.  It is assumed that you have already built the App Services .NET SDK and have those
available.  From within Visual Studio, choose Project > Add Reference and be sure to add references to:
- Usergrid.Sdk.dll
- RestSharp.dll
= Newtonsoft.Json.dll
Then, you will need to add references to the GMap.Net libraries
- GMap.NET.Core.dll
- GMap.NET.WindowsForm.dll
These files can be found off the Hot Build page at http://greatmaps.codeplex.com/releases/view/73162
Next, install the Geocoding NuGet library by typing "Install-Package Geocoder" in the Package Manager

In the file Form1.cs, change the org and app names to match your account.  Note that it is assumed you are using a sandbox account that does not
require a separate login step.  If that is not the case, add a call to client.Login() with the appropriate credentials.

After these setup steps, you should be able to compile and run the application.