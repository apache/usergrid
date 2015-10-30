# Advanced query usage

<div class="admonition note"> <p class="first admonition-title">Note</p> <p class="last"> 
Query examples in this content are shown unencoded to make them easier to read. Keep in mind that you might need to encode query strings if you're sending them as part of URLs, such as when you're executing them with the cURL tool.
</p></div>

## Attaching a query to all API calls


<div class="admonition note"> <p class="first admonition-title">Note</p> <p class="last"> 
<p>JavaScript SDK only.</p>
In some cases, it may be convenient to attach a query or other URI parameter to every call you make to Usergrid, such as a custom identifier or token. To do this with the Usergrid JavaScript SDK, add a qs property to your Usergrid.Client object when you initialize the SDK. For more on initializing the SDK, see our install guide.
</p></div>

For example, the following would append ?custom_id=1234 to every call sent from the Usergrid JavaScript SDK to Usergrid:

    var options = {
        orgName:'yourOrg',
        appName:'yourApp',
        qs:'custom_id=1234'
    }
    var dataClient = new Usergrid.Client(options);

