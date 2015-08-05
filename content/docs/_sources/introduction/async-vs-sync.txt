# Async vs. sync calls
The Usergrid SDKs work by making RESTful API calls from your application to the API. In some cases, both synchronous and asynchronous calls are supported for most methods, as in the case of the Usergrid Android SDK, while in others only asynchronous calls are supported, as in the Usergrid JavaScript SDK. The following is a brief explanation of synchronous vs. asynchronous API calls.

## Synchronous
If an API call is synchronous, it means that code execution will block (or wait) for the API call to return before continuing. This means that until a response is returned by the API, your application will not execute any further, which could be perceived by the user as latency or performance lag in your app. Making an API call synchronously can be beneficial, however, if there if code in your app that will only execute properly once the API response is received.

## Asynchronous
Asynchronous calls do not block (or wait) for the API call to return from the server. Execution continues on in your program, and when the call returns from the server, a "callback" function is executed. For example, in the following code using the Usergrid JavScript SDK, the function called dogCreateCallback will be called when the create dog API call returns from the server. Meanwhile, execution will continue:

    function dogCreateCallback(err, dog) {
        alert('I will probably be called second');
        if (err) {
            //Error - Dog not created
        } else {
            //Success - Dog was created

        }
    }

    client.createEntity({type:'dogs'}, dogCreateCallback);
    alert('I will probably be called first');
    
The result of this is that we cannot guarantee the order of the two alert statements. Most likely, the alert right after the createEntity function call will be called first since the API call will take a second or so to complete.

The important point is that program execution will continue, and asynchronously, the callback function will be called once program execution completes.