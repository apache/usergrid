# Advanced query usage
Query examples in this content are shown unencoded to make them easier to read. Keep in mind that you might need to encode query strings if you're sending them as part of URLs, such as when you're executing them with the cURL tool.
Attaching a query to all API calls

## JavaScript SDK only
The following feature is currently only supported by the Apigee JavaScript SDK
In some cases, it may be convenient to attach a query or other URI parameter to every call you make to API BaaS, such as a custom identifier or token. To do this with the Apigee JavaScript SDK, add a qs property to your Apigee.Client object when you initialize the SDK. For more on initializing the SDK, see our install guide.

For example, the following would append ?custom_id=1234 to every call sent from the Apigee JavaScript SDK to API BaaS:

    var options = {
        orgName:'yourOrg',
        appName:'yourApp',
        qs:'custom_id=1234'
    }
    var dataClient = new Apigee.Client(options);

