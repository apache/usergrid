## Examples Read Me ##

I don't show every method that you can call in the examples but I show you the most common api calls you will make but to get the most out of this api 
you really should spend around 10 to 15 minutes to read all the guzzle web service descriptors files in the Manifest folders that are versioned the most 
up to date version is 1.0.1 folder.

### Manifest files ###
Guzzle web service descriptors.

You can use the files included with the sdk but you are able to create your own , so you can pass the path the the manifest folder in the config to the sdk. 
The reason you might create you own is to support you own custom collections. So the way to do that is to copy the manifest folder to the location you want
then there is a file called custom that you need to copy and edit so for example if I had a custom Collection called Books and I want to be able to call
the sdk like ```Usergrid::books()->findById(['uuid'=> '12121']); ``` . All I have to do is copy the manifest file called custom and name it books and then 
in the file replace the ```$custom``` with the word ```'books'``` .
From that point on I can make api calls to my custom collection just like any of the default collection usergrid give you its a easy as that Ive designed this
SDK to be extended so you can get the most out of your usergrid install .

