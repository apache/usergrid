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
        'uri' => '/management/token',
        'summary' => 'Get management access token',
        'responseClass' => '',
        'responseType' => 'object',
        'errorResponses' => $errors,
        'parameters' => [
            'grant_type' => [
                'description' => 'Grant type.',
                'location' => 'query',
                'type' => 'string',
                'defaultValue' => 'password',
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
            ]
        ]
    ],
    'AuthorizeGet' => [
        'httpMethod' => 'GET',
        'uri' => '/management/authorize',
        'summary' => 'Authorize the client.  See the OAuth2 specification.',
        'responseClass' => '',
        'responseType' => 'object',
        'errorResponses' => $errors,
        'parameters' => [
            'response_type' => [
                'description' => 'Response type.',
                'location' => 'query',
                'type' => 'string',
                'defaultValue' => 'token',
                'required' => true,
                'allowableValues' => ['code', 'token']
            ],
            'client_id' => [
                'description' => 'Client ID.',
                'location' => 'query',
                'type' => 'string',
                'required' => true,
            ],
            'redirect_uri' => [
                'description' => 'Redirect URI.',
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
            ]
        ]

    ],
    'OrgJsonPost' => [
        'httpMethod' => 'POST',
        'uri' => '/management/orgs',
        'summary' => 'Create new organization.  See Usergrid documentation for JSON format of body.',
        'responseClass' => 'Apache\Usergrid\Api\Models\Organization',
        'responseType' => 'class',
        'errorResponses' => $errors,
        'parameters' => [
            'organization' => [
                'location' => 'json',
                'type' => 'string',
                'required' => true,
                'description' => 'Organization Name'
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
            ]
        ],
        'additionalParameters' => [
            'location' => 'json'
        ]
    ],
    'OrgGet' => [
        'httpMethod' => 'GET',
        'uri' => '/management/orgs/{org_name_or_uuid}',
        'summary' => 'Find organization by name or UUID',
        'responseClass' => 'Apache\Usergrid\Api\Models\Organization',
        'responseType' => 'class',
        'errorResponses' => $errors,
        'parameters' => [
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
    'OrgActivateGet' => [
        'httpMethod' => 'GET',
        'uri' => '/management/orgs/{org_name_or_uuid}/activate',
        'summary' => 'Activates the organization',
        'responseClass' => 'Apache\Usergrid\Api\Models\Organization',
        'responseType' => 'class',
        'errorResponses' => $errors,
        'parameters' => [
            'org_name_or_uuid' => [
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'description' => 'Organization name or uuid'
            ],
            'token' => [
                'location' => 'query',
                'type' => 'string',
                'required' => true,
                'description' => 'The OAuth2 access token'
            ],
            'confirm' => [
                'location' => 'query',
                'type' => 'boolean',
                'required' => false,
                'description' => 'Send confirmation email'
            ]

        ]
    ],
    'OrgReactivateGet' => [
        'httpMethod' => 'GET',
        'uri' => '/management/orgs/{org_name_or_uuid}/reactivate',
        'summary' => 'Reactivates the organization',
        'responseClass' => 'Apache\Usergrid\Api\Models\Organization',
        'responseType' => 'class',
        'errorResponses' => $errors,
        'parameters' => [
            'org_name_or_uuid' => [
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'description' => 'Organization name or uuid'
            ],
        ]
    ],
    'OrgFeedGet' => [
        'httpMethod' => 'GET',
        'uri' => '/management/orgs/{org_name_or_uuid}/feed',
        'summary' => 'Get organization activity feed',
        'responseClass' => '',
        'responseType' => 'object',
        'errorResponses' => $errors,
        'parameters' => [
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
    'OrgCredentialsGet' => [
        'httpMethod' => 'GET',
        'uri' => '/management/orgs/{org_name_or_uuid}/credentials',
        'summary' => 'Get organization client credentials',
        'responseClass' => '',
        'responseType' => 'object',
        'errorResponses' => $errors,
        'parameters' => [
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
    'OrgCredentialsPost' => [
        'httpMethod' => 'POST',
        'uri' => '/management/orgs/{org_name_or_uuid}/credentials',
        'summary' => 'Generate organization client credentials',
        'responseClass' => '',
        'responseType' => 'object',
        'errorResponses' => $errors,
        'parameters' => [
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
    'OrgUsersGet' => [
        'httpMethod' => 'GET',
        'uri' => '/management/orgs/{org_name_or_uuid}/users',
        'summary' => 'Get admin users for organization',
        'responseClass' => 'Apache\Usergrid\Api\Models\User',
        'responseType' => 'class',
        'errorResponses' => $errors,
        'parameters' => [
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
    'OrgUsersJsonPost' => [
        'httpMethod' => 'POST',
        'uri' => '/management/orgs/{org_name_or_uuid}/users',
        'summary' => 'Create new admin user for organization using JSON payload.',
        'responseClass' => 'Apache\Usergrid\Api\Models\User',
        'responseType' => 'class',
        'errorResponses' => $errors,
        'parameters' => [
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
            ]
        ],
        'additionalParameters' => [
            'location' => 'json'
        ]
    ],
    'OrgUsersFormPost' => [
        'httpMethod' => 'POST',
        'uri' => '/management/orgs/{org_name_or_uuid}/users',
        'summary' => 'Create new admin user for organization using form parameters.',
        'responseClass' => 'Apache\Usergrid\Api\Models\User',
        'responseType' => 'class',
        'errorResponses' => $errors,
        'parameters' => [
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
            ]
        ],
        'additionalParameters' => [
            'location' => 'postField'
        ]
    ],
    'OrgUserPut' => [
        'httpMethod' => 'PUT',
        'uri' => '/management/orgs/{org_name_or_uuid}/users/{user_username_email_or_uuid}',
        'summary' => 'Adds existing admin users for organization.',
        'responseClass' => 'Apache\Usergrid\Api\Models\User',
        'responseType' => 'class',
        'errorResponses' => $errors,
        'parameters' => [
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
            ],
            'user_username_email_or_uuid' => [
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'description' => 'Admin user username, email, or uuid'
            ]
        ]
    ],
    'OrgUserDelete' => [
        'httpMethod' => 'DELETE',
        'uri' => '/management/orgs/{org_name_or_uuid}/users/{user_username_email_or_uuid}',
        'summary' => 'Remove an admin user from organization.',
        'responseClass' => 'Apache\Usergrid\Api\Models\User',
        'responseType' => 'class',
        'errorResponses' => $errors,
        'parameters' => [
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
            ],
            'user_username_email_or_uuid' => [
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'description' => 'Admin user username, email, or uuid'
            ]
        ]
    ],
    'OrgAppsGet' => [
        'httpMethod' => 'GET',
        'uri' => '/management/orgs/{org_name_or_uuid}/apps',
        'summary' => 'Get apps for organization',
        'responseClass' => 'Apache\Usergrid\Api\Models\Application',
        'responseType' => 'class',
        'errorResponses' => $errors,
        'parameters' => [
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
    'OrgAppsJsonPost' => [
        'httpMethod' => 'POST',
        'uri' => '/management/orgs/{org_name_or_uuid}/apps',
        'summary' => 'Create new application for organization using JSON payload.',
        'responseClass' => 'Apache\Usergrid\Api\Models\Application',
        'responseType' => 'class',
        'errorResponses' => $errors,
        'parameters' => [
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
            ],
            'name' => [
                'location' => 'json',
                'type' => 'string',
                'required' => true,
                'description' => 'Application Name'
            ]
        ],
        'additionalParameters' => [
            'location' => 'json'
        ]
    ],
    'OrgAppsFormPost' => [
        'httpMethod' => 'POST',
        'uri' => '/management/orgs/{org_name_or_uuid}/apps',
        'summary' => 'Create new application for organization using form parameters.',
        'responseClass' => 'Apache\Usergrid\Api\Models\Application',
        'responseType' => 'class',
        'errorResponses' => $errors,
        'parameters' => [
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
            ],
            'name' => [
                'location' => 'postField',
                'type' => 'string',
                'required' => true,
                'description' => 'Application Name'
            ]
        ],
        'additionalParameters' => [
            'location' => 'postField'
        ]
    ],
    'OrgAppDelete' => [
        'httpMethod' => 'DELETE',
        'uri' => '/management/orgs/{org_name_or_uuid}/apps/{app_name_or_uuid}',
        'summary' => 'Delete an application in an organization.',
        'responseClass' => 'Apache\Usergrid\Api\Models\Application',
        'responseType' => 'class',
        'errorResponses' => $errors,
        'parameters' => [
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
            ],
            'app_name_or_uuid' => [
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'description' => 'Application name or uuid'
            ]
        ]
    ],
    'OrgAppCredentialsGet' => [
        'httpMethod' => 'GET',
        'uri' => '/management/orgs/{org_name_or_uuid}/apps/{app_name_or_uuid}/credentials',
        'summary' => 'Get application keys.',
        'responseClass' => '',
        'responseType' => 'object',
        'errorResponses' => $errors,
        'parameters' => [
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
            ],
            'app_name_or_uuid' => [
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'description' => 'Application name or uuid'
            ]
        ]
    ],
    'OrgAppCredentialsPost' => [
        'httpMethod' => 'POST',
        'uri' => '/management/orgs/{org_name_or_uuid}/apps/{app_name_or_uuid}/credentials',
        'summary' => 'Generate application keys.',
        'responseClass' => '',
        'responseType' => 'object',
        'errorResponses' => $errors,
        'parameters' => [
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
            ],
            'app_name_or_uuid' => [
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'description' => 'Application name or uuid'
            ]
        ]
    ],
    'OrgUserJsonPost' => [
        'httpMethod' => 'POST',
        'uri' => '/management/users',
        'summary' => 'Create new admin user.  See Usergrid documentation for JSON format of body.',
        'responseClass' => 'Apache\Usergrid\Api\Models\User',
        'responseType' => 'class',
        'errorResponses' => $errors,
        'parameters' => [
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
            ]
        ],
        'additionalParameters' => [
            'location' => 'json'
        ]
    ],
    'OrgUserFormPost' => [
        'httpMethod' => 'POST',
        'uri' => '/management/users',
        'summary' => 'Create new admin using form post parameters.',
        'responseClass' => 'Apache\Usergrid\Api\Models\User',
        'responseType' => 'class',
        'errorResponses' => $errors,
        'parameters' => [
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
            ]
        ],
        'additionalParameters' => [
            'location' => 'postField'
        ]

    ],
    'OrgUserResetPasswordGet' => [
        'httpMethod' => 'GET',
        'uri' => '/management/users/resetpw',
        'summary' => 'Initiate a user password reset.  Returns browser-viewable HTML page.',
        'responseClass' => '',
        'responseType' => 'object',
        'errorResponses' => $errors,
    ],
    'OrgUserResetPasswordFormPost' => [
        'httpMethod' => 'POST',
        'uri' => '/management/users/resetpw',
        'summary' => 'Complete a user password reset.  Handles form POST response.',
        'responseClass' => '',
        'responseType' => 'object',
        'errorResponses' => $errors,
        'parameters' => [
            'email' => [
                'location' => 'postField',
                'type' => 'string',
                'required' => true,
                'description' => 'Admin Email'
            ],
            'recaptcha_challenge_field' => [
                'location' => 'postField',
                'type' => 'string',
                'required' => true,
                'description' => 'Recaptcha Challenge Field'
            ],
            'recaptcha_response_field' => [
                'location' => 'postField',
                'type' => 'string',
                'required' => true,
                'description' => 'Recaptcha Response Field'
            ],
        ]
    ],
    'AdminUserGet' => [
        'httpMethod' => 'GET',
        'uri' => '/management/users/{user_username_email_or_uuid}',
        'summary' => 'Returns the admin user details',
        'responseClass' => 'Apache\Usergrid\Api\Models\User',
        'responseType' => 'class',
        'errorResponses' => $errors,
        'parameters' => [
            'access_token' => [
                'location' => 'query',
                'type' => 'string',
                'required' => true,
                'description' => 'The OAuth2 access token'
            ],
            'user_username_email_or_uuid' => [
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'description' => 'Admin username, email or uuid'
            ],
        ]

    ],
    'AdminUserJsonPut' => [
        'httpMethod' => 'PUT',
        'uri' => '/management/users/{user_username_email_or_uuid}',
        'summary' => 'Updates the admin user details.',
        'responseClass' => 'Apache\Usergrid\Api\Models\User',
        'responseType' => 'class',
        'errorResponses' => $errors,
        'parameters' => [
            'access_token' => [
                'location' => 'query',
                'type' => 'string',
                'required' => true,
                'description' => 'The OAuth2 access token'
            ],
            'user_username_email_or_uuid' => [
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'description' => 'Admin username, email or uuid'
            ],
        ],
        'additionalParameters' => [
            'location' => 'json'
        ]

    ],
    'AdminUserActivateGet' => [
        'httpMethod' => 'GET',
        'uri' => '/management/users/{user_username_email_or_uuid}/activate',
        'summary' => 'Activates the admin user from link provided in email notification.',
        'responseClass' => 'Apache\Usergrid\Api\Models\User',
        'responseType' => 'class',
        'errorResponses' => $errors,
        'parameters' => [
            'access_token' => [
                'location' => 'query',
                'type' => 'string',
                'required' => true,
                'description' => 'The OAuth2 access token'
            ],
            'user_username_email_or_uuid' => [
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'description' => 'Admin username, email or uuid'
            ],
            'confirm' => [
                'location' => 'uri',
                'type' => 'boolean',
                'required' => false,
                'description' => 'Send confirmation email'
            ],
        ]
    ],
    'AdminUserReactivateGet' => [
        'httpMethod' => 'GET',
        'uri' => '/management/users/{user_username_email_or_uuid}/reactivate',
        'summary' => 'Request admin user reactivation.',
        'responseClass' => '',
        'responseType' => 'object',
        'errorResponses' => $errors,
        'parameters' => [
            'user_username_email_or_uuid' => [
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'description' => 'Admin username, email or uuid'
            ]
        ]
    ],
    'AdminUserFeedGet' => [
        'httpMethod' => 'GET',
        'uri' => '/management/users/{user_username_email_or_uuid}/feed',
        'summary' => 'Get admin user activity feed.',
        'responseClass' => '',
        'responseType' => 'object',
        'errorResponses' => $errors,
        'parameters' => [
            'user_username_email_or_uuid' => [
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'description' => 'Admin username, email or uuid'
            ],
            'access_token' => [
                'location' => 'query',
                'type' => 'string',
                'required' => true,
                'description' => 'The OAuth2 access token'
            ],
        ]
    ],
    'AdminUserPasswordJsonPut' => [
        'httpMethod' => 'PUT',
        'uri' => '/management/users/{user_username_email_or_uuid}/password',
        'summary' => 'Set admin user password.  See Usergrid documentation for JSON format of body.',
        'responseClass' => '',
        'responseType' => 'object',
        'errorResponses' => $errors,
        'parameters' => [
            'user_username_email_or_uuid' => [
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'description' => 'Admin username, email or uuid'
            ],
            'access_token' => [
                'location' => 'query',
                'type' => 'string',
                'required' => true,
                'description' => 'The OAuth2 access token'
            ],
            'old_password' => [
                'location' => 'json',
                'type' => 'string',
                'required' => true,
                'description' => 'Old and new password'
            ],
            'new_password' => [
                'location' => 'json',
                'type' => 'string',
                'required' => true,
                'description' => 'Old and new password'
            ],
        ]
    ],
    'AdminUserResetPasswordGet' => [
        'httpMethod' => 'GET',
        'uri' => '/management/users/{user_username_email_or_uuid}/resetpw',
        'summary' => 'Initiate a user password reset.  Returns browser-viewable HTML page.',
        'responseClass' => '',
        'responseType' => 'object',
        'errorResponses' => $errors,
        'parameters' => [
            'user_username_email_or_uuid' => [
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'description' => 'Admin username, email or uuid'
            ]
        ]
    ],
    'AdminUserResetPasswordFormPost' => [
        'httpMethod' => 'GET',
        'uri' => '/management/users/{user_username_email_or_uuid}/resetpw',
        'summary' => 'Complete a user password reset.  Handles form POST response.',
        'responseClass' => '',
        'responseType' => 'object',
        'errorResponses' => $errors,
        'parameters' => [
            'user_username_email_or_uuid' => [
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'description' => 'Admin username, email or uuid'
            ],
            'recaptcha_challenge_field' => [
                'location' => 'postField',
                'type' => 'string',
                'required' => true,
                'description' => 'Recaptcha Challenge Field'
            ],
            'recaptcha_response_field' => [
                'location' => 'postField',
                'type' => 'string',
                'required' => true,
                'description' => 'Recaptcha Response Field'
            ]
        ]
    ],
    'AdminUserOrgsGet' => [
        'httpMethod' => 'GET',
        'uri' => '/management/users/{user_username_email_or_uuid}/orgs',
        'summary' => 'Get organizations for admin user.',
        'responseClass' => 'Apache\Usergrid\Api\Models\Organization',
        'responseType' => 'class',
        'errorResponses' => $errors,
        'parameters' => [
            'access_token' => [
                'location' => 'query',
                'type' => 'string',
                'required' => true,
                'description' => 'The OAuth2 access token'
            ],
            'user_username_email_or_uuid' => [
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'description' => 'Admin username, email or uuid'
            ],

        ]
    ],
    'AdminUserOrgsJsonPost' => [
        'httpMethod' => 'POST',
        'uri' => '/management/users/{user_username_email_or_uuid}/orgs',
        'summary' => 'Create new organization for admin user using JSON payload.',
        'responseClass' => 'Apache\Usergrid\Api\Models\Organization',
        'responseType' => 'class',
        'errorResponses' => $errors,
        'parameters' => [
            'access_token' => [
                'location' => 'query',
                'type' => 'string',
                'required' => true,
                'description' => 'The OAuth2 access token'
            ],
            'user_username_email_or_uuid' => [
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'description' => 'Admin username, email or uuid'
            ],
            'organization' => [
                'location' => 'json',
                'type' => 'string',
                'required' => true,
                'description' => 'Admin username, email or uuid'
            ],

        ],
        'additionalParameters' => [
            'location' => 'json'
        ]
    ],
    'AdminUserOrgsFormPost' => [
        'httpMethod' => 'POST',
        'uri' => '/management/users/{user_username_email_or_uuid}/orgs',
        'summary' => 'Create new organization for admin user using form parameters.',
        'responseClass' => 'Apache\Usergrid\Api\Models\Organization',
        'responseType' => 'class',
        'errorResponses' => $errors,
        'parameters' => [
            'access_token' => [
                'location' => 'query',
                'type' => 'string',
                'required' => true,
                'description' => 'The OAuth2 access token'
            ],
            'user_username_email_or_uuid' => [
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'description' => 'Admin username, email or uuid'
            ],
            'organization' => [
                'location' => 'postField',
                'type' => 'string',
                'required' => true,
                'description' => 'Admin username, email or uuid'
            ],

        ],
        'additionalParameters' => [
            'location' => 'postField'
        ]
    ],
    'AdminUserOrgPut' => [
        'httpMethod' => 'PUT',
        'uri' => '/management/users/{user_username_email_or_uuid}/orgs/{org_name_or_uuid}',
        'summary' => 'Add admin users to organization.',
        'responseClass' => '',
        'responseType' => 'object',
        'errorResponses' => $errors,
        'parameters' => [
            'access_token' => [
                'location' => 'query',
                'type' => 'string',
                'required' => true,
                'description' => 'The OAuth2 access token'
            ],
            'user_username_email_or_uuid' => [
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'description' => 'Admin username, email or uuid'
            ],
            'org_name_or_uuid' => [
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'description' => 'Organization name or uuid'
            ],

        ],
        'additionalParameters' => [
            'location' => 'json'
        ]
    ],
    'AdminUserOrgDelete' => [
        'httpMethod' => 'DELETE',
        'uri' => '/management/users/{user_username_email_or_uuid}/orgs/{org_name_or_uuid}',
        'summary' => 'Remove an admin user from organization.',
        'responseClass' => '',
        'responseType' => 'object',
        'errorResponses' => $errors,
        'parameters' => [
            'access_token' => [
                'location' => 'query',
                'type' => 'string',
                'required' => true,
                'description' => 'The OAuth2 access token'
            ],
            'user_username_email_or_uuid' => [
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'description' => 'Admin username, email or uuid'
            ],
            'org_name_or_uuid' => [
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'description' => 'Organization name or uuid'
            ],

        ]
    ]

];