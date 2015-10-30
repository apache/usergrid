# Changing token expiration (time-to-live)

An access token has a “time-to-live” (ttl), which is the maximum time that the access token will be valid for use within the application. With the Usergrid, you can change the default ttl for all application user tokens, set the ttl for an individual token at the time of creation, or revoke one or more tokens. This gives you a high degree of control over access to your Usergrid account and data store.

## Default ttl
By default, all tokens have a system-defined time-to-live of 7 days (604800 seconds). Note that Token ttl is specified in milliseconds, but when a token is created, the API response will return the ttl in seconds.

## Changing the default ttl
You can change the default ttl for all application user tokens (that is, tokens associated with a user entity) by updating the application entity’s accesstokenttl property. Changing the default ttl will only affect new tokens. Any existing tokens will not be affected.

Please note that this does not apply to application client, organization client or admin user tokens. For more on obtaining tokens for these other authorization levels, see [Authenticating users and application clients](authenticating-users-and-application-clients.html).

__Note__: If you set ttl=0, the token will never expire. This can pose a security risk and should be used with caution.

### Request syntax

    curl -X PUT https://api.usergrid.com/<org_name>/<app_name> -d '{"accesstokenttl":<ttl_in_milliseconds>}'
    
### Example Request

    curl -X PUT https://api.usergrid.com/your-org/your-app -d '{"accesstokenttl":"1800000"}'
    
### Example response

    {
      "action" : "put",
      "application" : "d878de4r-99a7-11e3-b31d-5373d7165c2d",
      "params" : {
        "access_token" : [ "DFR4d5M1mJmoEeOGVPncm-g9qgAAAURv_lfQ7uu6aYHjJJn7QCrGoVnvU-ob5Ko" ]
      },
      "uri" : "https://api.usergrid.com/amuramoto/secured",
      "entities" : [ {
        "uuid" : "d878de4r-99a7-11e3-b31d-5373d7165c2d",
        "type" : "application",
        "name" : "your-org/your-app",
        "created" : 1392843003032,
        "modified" : 1392843615777,
        "accesstokenttl" : 1800000,
        "organizationName" : "your-org",
        "applicationName" : "your-app",
        "apigeeMobileConfig" : "{...}",
        "metadata" : {
          "collections" : [ "activities", "assets", "devices", "events", "folders", "groups", "roles", "users" ]
        }
      } ],
      "timestamp" : 1392843615767,
      "duration" : 28,
      "organization" : "your-org",
      "applicationName" : "your-app"
    }

## Changing ttl when a token is created
When you request an access token, you can override its ttl by including a ttl property in the body of the request when the token is created. This applies to tokens for all authentication levels, including application user, admin user, organization client, and application client authentication levels.

The ttl must be equal to or less than the value of the application entity's accesstokenttl property. If you specify a ttl value greater than the value of accesstokenttl, an error message is returned that indicates the maximum time to live value that can be specified.

For example, the following would create an application user token with a ttl of 180000000 milliseconds:

    curl -X POST https://api.usergrid.com/your-org/your-app/token -d '{"username":"someUser", "password":"somePassword", "grant_type":"password", "ttl":"180000000"}'
    
__Note__: If you set ttl=0, the token will never expire. This can pose a security risk and should be used with caution.