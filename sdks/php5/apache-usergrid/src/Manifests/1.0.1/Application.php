<?php
/**
 * Copyright 2010-2014 baas-platform.com, Pty Ltd. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

return [

    'AuthPasswordGet' => [
        'httpMethod' => 'GET',
        'uri' => '/{org_name_or_uuid}/{app_name_or_uuid}/token',
        'notes' => 'Get the app access token.  See the OAuth2 specification for details.',
        'summary' => 'Get app access token',
        'responseClass' => '',
        'responseType' => 'class',
        'errorResponses' => $errors,
        'parameters' => [
            'grant_type' => [
                'description' => 'Grant type.',
                'location' => 'query',
                'type' => 'string',
                'defaultValue' => 'password',
                'required' => true,
            ],
            'app_name_or_uuid' => [
                'description' => 'app name or uuid',
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
            ],
            'username' => [
                'description' => 'Username (for grant_type=password).',
                'location' => 'query',
                'type' => 'string',
                'required' => false,
            ],
            'password' => [
                'description' => 'Password (for grant_type=password).',
                'location' => 'query',
                'type' => 'string',
                'required' => false,
            ],
            'client_id' => [
                'description' => 'Client ID (for grant_type=client_credentials).',
                'location' => 'query',
                'type' => 'string',
                'required' => false,
            ],
            'client_secret' => [
                'description' => 'Client Secret (for grant_type=client_credentials).',
                'location' => 'query',
                'type' => 'string',
                'required' => false,
            ],
            'org_name_or_uuid' => [
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'description' => 'Organization name or uuid'
            ]
        ]
    ],
    'AuthPasswordPost' => [
        'httpMethod' => 'POST',
        'uri' => '/{org_name_or_uuid}/{app_name_or_uuid}/token',
        'notes' => 'Get the app access token.  See the OAuth2 specification for details.',
        'summary' => 'Get app access token',
        'responseClass' => '',
        'responseType' => 'class',
        'errorResponses' => $errors,
        'parameters' => [
            'grant_type' => [
                'description' => 'Grant type.',
                'location' => 'postField',
                'type' => 'string',
                'defaultValue' => 'password',
                'required' => true,
            ],
            'app_name_or_uuid' => [
                'description' => 'app name or uuid',
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
            ],
            'username' => [
                'description' => 'Username (for grant_type=password).',
                'location' => 'postField',
                'type' => 'string',
                'required' => false,
            ],
            'password' => [
                'description' => 'Password (for grant_type=password).',
                'location' => 'postField',
                'type' => 'string',
                'required' => false,
            ],
            'client_id' => [
                'description' => 'Client ID (for grant_type=client_credentials).',
                'location' => 'postField',
                'type' => 'string',
                'required' => false,
            ],
            'client_secret' => [
                'description' => 'Client Secret (for grant_type=client_credentials).',
                'location' => 'postField',
                'type' => 'string',
                'required' => false,
            ],
            'org_name_or_uuid' => [
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'description' => 'Organization name or uuid'
            ]
        ]
    ],
    'AuthorizeGet' => [
        'httpMethod' => 'GET',
        'uri' => '/{org_name_or_uuid}/{app_name_or_uuid}/authorize',
        'notes' => 'Authorize the app client.  See the OAuth2 specification.',
        'summary' => 'Authorize app client',
        'responseClass' => '',
        'responseType' => 'class',
        'errorResponses' => $errors,
        'parameters' => [
            'app_name_or_uuid' => [
                'description' => 'app name or uuid',
                'location' => 'uri',
                'type' => 'string',
                'required' => false,
            ],
            'response_type' => [
                'description' => 'Response type',
                'location' => 'query',
                'type' => 'string',
                'required' => false,
                'default' => 'token'
            ],
            'redirect_uri' => [
                'description' => 'Redirect URI',
                'location' => 'query',
                'type' => 'string',
                'required' => false,
            ],
            'client_id' => [
                'description' => 'Client ID',
                'location' => 'query',
                'type' => 'string',
                'required' => false,
            ],
            'scope' => [
                'description' => 'Access Token Scope.',
                'location' => 'query',
                'type' => 'string',
                'required' => false,
            ],
            'state' => [
                'description' => 'Client State.',
                'location' => 'query',
                'type' => 'string',
                'required' => false,
            ],
            'org_name_or_uuid' => [
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'description' => 'Organization name or uuid'
            ]
        ]
    ],
    'AuthorizePost' => [
        'httpMethod' => 'POST',
        'uri' => '/{org_name_or_uuid}/{app_name_or_uuid}/authorize',
        'notes' => 'Authorize the app client.  See the OAuth2 specification.',
        'summary' => 'Authorize app client',
        'responseClass' => '',
        'responseType' => 'class',
        'errorResponses' => $errors,
        'parameters' => [
            'app_name_or_uuid' => [
                'description' => 'app name or uuid',
                'location' => 'uri',
                'type' => 'string',
                'required' => false,
            ],
            'response_type' => [
                'description' => 'Response type',
                'location' => 'query',
                'type' => 'string',
                'required' => false,
                'default' => 'token'
            ],
            'redirect_uri' => [
                'description' => 'Redirect URI',
                'location' => 'query',
                'type' => 'string',
                'required' => false,
            ],
            'client_id' => [
                'description' => 'Client ID',
                'location' => 'query',
                'type' => 'string',
                'required' => false,
            ],
            'scope' => [
                'description' => 'Access Token Scope.',
                'location' => 'query',
                'type' => 'string',
                'required' => false,
            ],
            'state' => [
                'description' => 'Client State.',
                'location' => 'query',
                'type' => 'string',
                'required' => false,
            ],
            'org_name_or_uuid' => [
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'description' => 'Organization name or uuid'
            ]
        ]
    ],
    'CredentialsGet' => [
        'httpMethod' => 'GET',
        'uri' => '/{org_name_or_uuid}/{app_name_or_uuid}/credentials',
        'notes' => 'Get the app client credentials.',
        'summary' => 'Get app client credentials',
        'responseClass' => '',
        'responseType' => 'class',
        'errorResponses' => $errors,
        'parameters' => [
            'app_name_or_uuid' => [
                'description' => 'app name or uuid',
                'location' => 'uri',
                'type' => 'string',
                'required' => false,
            ],
            'access_token' => [
                'location' => 'query',
                'type' => 'string',
                'required' => true,
                'description' => 'The OAuth2 access token'
            ],
            'org_name_or_uuid' => [
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'description' => 'Organization name or uuid'
            ]
        ]
    ],
    'CredentialsPost' => [
        'httpMethod' => 'POST',
        'uri' => '/{org_name_or_uuid}/{app_name_or_uuid}/credentials',
        'notes' => 'Generate new app client credentials',
        'summary' => 'Generate app client credentials',
        'responseClass' => '',
        'responseType' => 'class',
        'errorResponses' => $errors,
        'parameters' => [
            'app_name_or_uuid' => [
                'description' => 'app name or uuid',
                'location' => 'uri',
                'type' => 'string',
                'required' => false,
            ],
            'access_token' => [
                'location' => 'query',
                'type' => 'string',
                'required' => true,
                'description' => 'The OAuth2 access token'
            ],
            'org_name_or_uuid' => [
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'description' => 'Organization name or uuid'
            ]
        ]
    ],
    'UserJsonPost' => [
        'httpMethod' => 'POST',
        'uri' => '/{org_name_or_uuid}/{app_name_or_uuid}/users',
        'notes' => 'Create new app user',
        'summary' => 'Create new app user.  See Usergrid documentation for JSON format of body.',
        'responseClass' => '',
        'responseType' => 'class',
        'errorResponses' => $errors,
        'parameters' => [
            'app_name_or_uuid' => [
                'description' => 'app name or uuid',
                'location' => 'uri',
                'type' => 'string',
                'required' => false,
            ],
            'username' => [
                'location' => 'json',
                'type' => 'string',
                'required' => true,
                'description' => 'Admin Username'
            ],
            'name' => [
                'location' => 'json',
                'type' => 'string',
                'required' => true,
                'description' => 'Admin Name'
            ],
            'email' => [
                'location' => 'json',
                'type' => 'string',
                'required' => true,
                'description' => 'Admin Email'
            ],
            'password' => [
                'location' => 'json',
                'type' => 'string',
                'required' => true,
                'description' => 'Admin Password'
            ],
            'org_name_or_uuid' => [
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'description' => 'Organization name or uuid'
            ]
        ]
    ],
    'UserFormPost' => [
        'httpMethod' => 'POST',
        'uri' => '/{org_name_or_uuid}/{app_name_or_uuid}/users',
        'notes' => 'Create new app user',
        'summary' => 'Create new app user using form post parameters.',
        'responseClass' => '',
        'responseType' => 'class',
        'errorResponses' => $errors,
        'parameters' => [
            'app_name_or_uuid' => [
                'description' => 'app name or uuid',
                'location' => 'uri',
                'type' => 'string',
                'required' => false,
            ],
            'username' => [
                'location' => 'postField',
                'type' => 'string',
                'required' => true,
                'description' => 'Admin Username'
            ],
            'name' => [
                'location' => 'postField',
                'type' => 'string',
                'required' => true,
                'description' => 'Admin Name'
            ],
            'email' => [
                'location' => 'postField',
                'type' => 'string',
                'required' => true,
                'description' => 'Admin Email'
            ],
            'password' => [
                'location' => 'postField',
                'type' => 'string',
                'required' => true,
                'description' => 'Admin Password'
            ],
            'org_name_or_uuid' => [
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'description' => 'Organization name or uuid'
            ]
        ]
    ],
    'UserPasswordRestGet' => [
        'httpMethod' => 'GET',
        'uri' => '/{org_name_or_uuid}/{app_name_or_uuid}/users/resetpw',
        'notes' => 'Initiate a user password reset.  Returns browser-viewable HTML page.',
        'summary' => 'Initiate a user password reset',
        'responseClass' => '',
        'responseType' => 'class',
        'errorResponses' => $errors,
        'parameters' => [
            'app_name_or_uuid' => [
                'description' => 'app name or uuid',
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
            ],
            'org_name_or_uuid' => [
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'description' => 'Organization name or uuid'
            ]
        ]
    ],
    'UserPasswordFormPost' => [
        'httpMethod' => 'POST',
        'uri' => '/{org_name_or_uuid]/{app_name_or_uuid}/users/resetpw',
        'notes' => 'Complete a user password reset.  Handles form POST response.',
        'summary' => 'Complete a user password reset',
        'responseClass' => '',
        'responseType' => 'class',
        'errorResponses' => $errors,
        'parameters' => [
            'app_name_or_uuid' => [
                'description' => 'app name or uuid',
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
            ],
            'email' => [
                'description' => 'User Email',
                'location' => 'postField',
                'type' => 'string',
                'required' => true,
            ],
            'recaptcha_challenge_field' => [
                'description' => 'Recaptcha Challenge Field',
                'location' => 'postField',
                'type' => 'string',
                'required' => true,
            ],
            'recaptcha_response_field' => [
                'description' => 'Recaptcha Response Field',
                'location' => 'postField',
                'type' => 'string',
                'required' => true,
            ],
            'org_name_or_uuid' => [
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'description' => 'Organization name or uuid'
            ]
        ]
    ],
    'UserGet' => [
        'httpMethod' => 'GET',
        'uri' => '/{org_name_or_uuid}/{app_name_or_uuid}/users/{user_username_email_or_uuid}',
        'notes' => 'Returns the app user details.',
        'summary' => 'Returns the app user details',
        'responseClass' => '',
        'responseType' => 'class',
        'errorResponses' => $errors,
        'parameters' => [
            'app_name_or_uuid' => [
                'description' => 'app name or uuid',
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
            ],
            'access_token' => [
                'description' => 'The OAuth2 access token',
                'location' => 'query',
                'type' => 'string',
                'required' => true,
            ],
            'user_username_email_or_uuid' => [
                'description' => 'User username, email or uuid',
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
            ],
            'org_name_or_uuid' => [
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'description' => 'Organization name or uuid'
            ]
        ]
    ],
    'UserJsonPut' => [
        'httpMethod' => 'PUT',
        'uri' => '/{org_name_or_uuid}/{app_name_or_uuid}/users/{user_username_email_or_uuid}',
        'notes' => 'Updates the app user details.',
        'summary' => 'Updates the app user details',
        'responseClass' => '',
        'responseType' => 'class',
        'errorResponses' => $errors,
        'parameters' => [
            'app_name_or_uuid' => [
                'description' => 'app name or uuid',
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
            ],
            'access_token' => [
                'description' => 'The OAuth2 access token',
                'location' => 'query',
                'type' => 'string',
                'required' => true,
            ],
            'user_username_email_or_uuid' => [
                'description' => 'User username, email or uuid',
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
            ],
            'org_name_or_uuid' => [
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'description' => 'Organization name or uuid'
            ]
        ],
        'additionalParameters' => [
            'location' => 'json'
        ]
    ],
    'UserActivateGet' => [
        'httpMethod' => 'GET',
        'uri' => '/{org_name_or_uuid}/{app_name_or_uuid}/users/{user_username_email_or_uuid}/activate',
        'notes' => 'Activates the app user from link provided in email notification.',
        'summary' => 'Activates the app user',
        'responseClass' => '',
        'responseType' => 'class',
        'errorResponses' => $errors,
        'parameters' => [
            'app_name_or_uuid' => [
                'description' => 'app name or uuid',
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
            ],
            'token' => [
                'description' => 'Activation Token (supplied via email)',
                'location' => 'query',
                'type' => 'string',
                'required' => true,
            ],
            'user_username_email_or_uuid' => [
                'description' => 'User username, email or uuid',
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
            ],
            'confirm' => [
                'description' => 'Send confirmation email',
                'location' => 'query',
                'type' => 'boolean',
                'required' => true,
            ],
            'org_name_or_uuid' => [
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'description' => 'Organization name or uuid'
            ]
        ]
    ],
    'UserReactivateGet' => [
        'httpMethod' => 'GET',
        'uri' => '/{org_name_or_uuid}/{app_name_or_uuid}/users/{user_username_email_or_uuid}/reactivate',
        'notes' => 'Request app user reactivation.',
        'summary' => 'Reactivates the app user',
        'responseClass' => '',
        'responseType' => 'class',
        'errorResponses' => $errors,
        'parameters' => [
            'app_name_or_uuid' => [
                'description' => 'app name or uuid',
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
            ],
            'user_username_email_or_uuid' => [
                'description' => 'User username, email or uuid',
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
            ],
            'org_name_or_uuid' => [
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'description' => 'Organization name or uuid'
            ]
        ]
    ],
    'UserFeedGet' => [
        'httpMethod' => 'GET',
        'uri' => '/{org_name_or_uuid}/{app_name_or_uuid}/users/{user_username_email_or_uuid}/feed',
        'notes' => 'Get app user activity feed.',
        'summary' => 'Get app user activity feed',
        'responseClass' => '',
        'responseType' => 'class',
        'errorResponses' => $errors,
        'parameters' => [
            'app_name_or_uuid' => [
                'description' => 'app name or uuid',
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
            ],
            'user_username_email_or_uuid' => [
                'description' => 'User username, email or uuid',
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
            ],
            'access_token' => [
                'description' => 'The OAuth2 access token',
                'location' => 'query',
                'type' => 'string',
                'required' => true,
            ],
            'org_name_or_uuid' => [
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'description' => 'Organization name or uuid'
            ]
        ]
    ],
    'UserPasswordJsonPut' => [
        'httpMethod' => 'PUT',
        'uri' => '/{org_name_or_uuid}/{app_name_or_uuid}/users/{user_username_email_or_uuid}/password',
        'notes' => 'Set app user password.  See Usergrid documentation for JSON format of body.',
        'summary' => 'Set app user password',
        'responseClass' => '',
        'responseType' => 'class',
        'errorResponses' => $errors,
        'parameters' => [
            'app_name_or_uuid' => [
                'description' => 'app name or uuid',
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
            ],
            'user_username_email_or_uuid' => [
                'description' => 'User username, email or uuid',
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
            ],
            'access_token' => [
                'description' => 'The OAuth2 access token',
                'location' => 'query',
                'type' => 'string',
                'required' => true,
            ],
            'org_name_or_uuid' => [
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'description' => 'Organization name or uuid'
            ]
        ],
        'additionalParameters' => [
            "description" => "Old and new password",
            'location' => 'json'
        ]
    ],
    'UserResetPasswordGet' => [
        'httpMethod' => 'GET',
        'uri' => '/{org_name_or_uuid}/{app_name_or_uuid}/users/{user_username_email_or_uuid}/resetpw',
        'notes' => 'Initiate a user password reset.  Returns browser-viewable HTML page.',
        'summary' => 'Initiate a user password reset',
        'responseClass' => '',
        'responseType' => 'class',
        'errorResponses' => $errors,
        'parameters' => [
            'app_name_or_uuid' => [
                'description' => 'app name or uuid',
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
            ],
            'user_username_email_or_uuid' => [
                'description' => 'User username, email or uuid',
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
            ],
            'org_name_or_uuid' => [
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'description' => 'Organization name or uuid'
            ]
        ]
    ],
    'UserResetPasswordFormPost' => [
        'httpMethod' => 'POST',
        'uri' => '/{org_name_or_uuid}/{app_name_or_uuid}/users/{user_username_email_or_uuid}/resetpw',
        'notes' => 'Complete a user password reset.  Handles form POST response.',
        'summary' => 'Complete a user password reset',
        'responseClass' => '',
        'responseType' => 'class',
        'errorResponses' => $errors,
        'parameters' => [
            'app_name_or_uuid' => [
                'description' => 'app name or uuid',
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
            ],
            'user_username_email_or_uuid' => [
                'description' => 'User username, email or uuid',
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
            ],
            'recaptcha_challenge_field' => [
                'description' => 'Recaptcha Challenge Field',
                'location' => 'postField',
                'type' => 'string',
                'required' => true,
            ],
            'recaptcha_response_field' => [
                'description' => 'Recaptcha Response Field',
                'location' => 'postField',
                'type' => 'string',
                'required' => true,
            ],
            'org_name_or_uuid' => [
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'description' => 'Organization name or uuid'
            ]
        ]
    ],
    'EntityGet' => [
        'httpMethod' => 'GET',
        'uri' => '/{org_name_or_uuid}/{app_name_or_uuid}/{collection}',
        'notes' => 'Query an app collection.',
        'summary' => 'Query an app collection',
        'responseClass' => 'Apache\Usergrid\Api\Models\Collection',
        'responseType' => 'class',
        'errorResponses' => $errors,
        'parameters' => [
            'app_name_or_uuid' => [
                'description' => 'app name or uuid',
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
            ],
            'collection' => [
                'description' => 'collection name (entity type)',
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
            ],
            'access_token' => [
                'description' => 'The OAuth2 access token',
                'location' => 'query',
                'type' => 'string',
                'required' => false,
            ],
            'ql' => [
                'description' => 'a query in the query language',
                'location' => 'query',
                'type' => 'string',
                'required' => false,
            ],
            'reversed' => [
                'description' => 'return results in reverse order',
                'location' => 'query',
                'type' => 'boolean',
                'required' => false,
            ],
            'start' => [
                'description' => 'the first entity UUID to return',
                'location' => 'query',
                'type' => 'string',
                'required' => false,
            ],
            'cursor' => [
                'description' => 'an encoded representation of the query position for paging',
                'location' => 'query',
                'type' => 'string',
                'required' => false,
            ],
            'limit' => [
                'description' => 'an encoded representation of the query position for paging',
                'location' => 'query',
                'type' => 'integer',
                'required' => false,
            ],
            'filter' => [
                'description' => 'a condition to filter on',
                'location' => 'query',
                'type' => 'integer',
                'required' => false,
            ],
            'org_name_or_uuid' => [
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'description' => 'Organization name or uuid'
            ]
        ]
    ],
    'EntityJsonPost' => [
        'httpMethod' => 'POST',
        'uri' => '/{org_name_or_uuid}/{app_name_or_uuid}/{collection}',
        'notes' => 'Create new app entity.  See Usergrid documentation for JSON format of body.',
        'summary' => 'Create new app entity',
        'responseClass' => '',
        'responseType' => 'class',
        'errorResponses' => $errors,
        'parameters' => [
            'app_name_or_uuid' => [
                'description' => 'app name or uuid',
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
            ],
            'collection' => [
                'description' => 'collection name (entity type)',
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
            ],
            'access_token' => [
                'description' => 'The OAuth2 access token',
                'location' => 'query',
                'type' => 'string',
                'required' => false,
            ],
            'org_name_or_uuid' => [
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'description' => 'Organization name or uuid'
            ]
        ],
        'additionalParameters' => [
            "description" => "Entity data",
            'location' => 'json'
        ]
    ],
    'EntityPut' => [
        'httpMethod' => 'PUT',
        'uri' => '/{org_name_or_uuid}/{app_name_or_uuid}/{collection}/{entity_name_or_uuid}',
        'notes' => 'Update an app entity in a collection.',
        'summary' => 'Update an app entity',
        'responseClass' => '',
        'responseType' => 'class',
        'errorResponses' => $errors,
        'parameters' => [
            'app_name_or_uuid' => [
                'description' => 'app name or uuid',
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
            ],
            'entity_name_or_uuid' => [
                'description' => 'entity name or uuid',
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
            ],
            'collection' => [
                'description' => 'collection name (entity type)',
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
            ],
            'access_token' => [
                'description' => 'The OAuth2 access token',
                'location' => 'query',
                'type' => 'string',
                'required' => false,
            ],
            'org_name_or_uuid' => [
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'description' => 'Organization name or uuid'
            ]
        ],
        'additionalParameters' => [
            "description" => "Entity data",
            'location' => 'json'
        ]
    ],
    'EntityDelete' => [
        'httpMethod' => 'DELETE',
        'uri' => '/{org_name_or_uuid}/{app_name_or_uuid}/{collection}/{entity_name_or_uuid}',
        'notes' => 'Delete an app entity.',
        'summary' => 'Delete an app entity',
        'responseClass' => '',
        'responseType' => 'class',
        'errorResponses' => $errors,
        'parameters' => [
            'app_name_or_uuid' => [
                'description' => 'app name or uuid',
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
            ],
            'entity_name_or_uuid' => [
                'description' => 'entity name or uuid',
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
            ],
            'collection' => [
                'description' => 'collection name (entity type)',
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
            ],
            'access_token' => [
                'description' => 'The OAuth2 access token',
                'location' => 'query',
                'type' => 'string',
                'required' => false,
            ],
            'org_name_or_uuid' => [
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'description' => 'Organization name or uuid'
            ]
        ]
    ],
    'PostEvents' => [
        'httpMethod' => 'POST',
        'uri' => '/{org_name_or_uuid}/{app_name_or_uuid}/{collection}',
        'notes' => 'Create Event.  See Usergrid documentation for JSON format of body.',
        'summary' => 'Create new app event',
        'responseClass' => '',
        'responseType' => 'class',
        'errorResponses' => $errors,
        'parameters' => [
            'app_name_or_uuid' => [
                'description' => 'app name or uuid',
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
            ],
            'collection' => [
                'description' => 'collection name (entity type)',
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
            ],
            'access_token' => [
                'description' => 'The OAuth2 access token',
                'location' => 'query',
                'type' => 'string',
                'required' => false,
            ]
        ],
        'additionalParameters' => [
            "description" => "Entity data",
            'location' => 'json'
        ]
    ],
    //Application Collection Relationships
    'GetRelationship' => [
        'httpMethod' => 'GET',
        'uri' => '/{org_name_or_uuid}/{app_name_or_uuid}/{collection}/{entity_id}/{relationship}',
        'notes' => 'Query an Entity Relationship ',
        'summary' => 'Query an Entity Relationship',
        'responseClass' => 'Apache\Usergrid\Api\Models\Collection',
        'responseType' => 'class',
        'errorResponses' => $errors,
        'parameters' => [
            'app_name_or_uuid' => [
                'description' => 'app name or uuid',
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
            ],
            'collection' => [
                'description' => 'collection name (entity type)',
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
            ],
            'entity_id' => [
                'description' => 'Entity  ID (uuid)',
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
            ],
            'relationship' => [
                'description' => 'Relationship',
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
            ],
            'access_token' => [
                'description' => 'The OAuth2 access token',
                'location' => 'query',
                'type' => 'string',
                'required' => false,
            ],
            'ql' => [
                'description' => 'a query in the query language',
                'location' => 'query',
                'type' => 'string',
                'required' => false,
            ],
            'reversed' => [
                'description' => 'return results in reverse order',
                'location' => 'query',
                'type' => 'boolean',
                'required' => false,
            ],
            'start' => [
                'description' => 'the first entity UUID to return',
                'location' => 'query',
                'type' => 'string',
                'required' => false,
            ],
            'cursor' => [
                'description' => 'an encoded representation of the query position for paging',
                'location' => 'query',
                'type' => 'string',
                'required' => false,
            ],
            'limit' => [
                'description' => 'an encoded representation of the query position for paging',
                'location' => 'query',
                'type' => 'integer',
                'required' => false,
            ],
            'filter' => [
                'description' => 'a condition to filter on',
                'location' => 'query',
                'type' => 'integer',
                'required' => false,
            ],
            'org_name_or_uuid' => [
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'description' => 'Organization name or uuid'
            ]
        ]
    ],
    'CreateRelationship' => [
        'httpMethod' => 'POST',
        'uri' => '/{org_name_or_uuid}/{app_name_or_uuid}/{collection}/{first_entity_id}/{relationship}/{second_entity_id}',
        'notes' => 'Create new app entity.  See Usergrid documentation for JSON format of body.',
        'summary' => 'Create new app entity',
        'responseClass' => '',
        'responseType' => 'class',
        'errorResponses' => $errors,
        'parameters' => [
            'app_name_or_uuid' => [
                'description' => 'app name or uuid',
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
            ],
            'collection' => [
                'description' => 'collection name (entity type)',
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
            ],
            'first_entity_id' => [
                'description' => 'first entity id (uuid)',
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
            ],
            'relationship' => [
                'description' => 'relationship',
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
            ],
            'second_entity_id' => [
                'description' => '2nd entity id (uuid)',
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
            ],
            'access_token' => [
                'description' => 'The OAuth2 access token',
                'location' => 'query',
                'type' => 'string',
                'required' => false,
            ],
            'org_name_or_uuid' => [
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'description' => 'Organization name or uuid'
            ]
        ],
        'additionalParameters' => [
            "description" => "Entity data",
            'location' => 'json'
        ]
    ],
];