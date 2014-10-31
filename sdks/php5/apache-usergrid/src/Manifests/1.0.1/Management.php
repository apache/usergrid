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
    // Management Tokens
    'OrgTokenGet' => [
        'httpMethod' => 'GET',
        'uri' => '/management/token',
        'notes' => 'Get the org or admin user access token.  See the OAuth2 specification for details.',
        'summary' => 'Get organization access token',
        'responseClass' => '',
        'responseType' => 'model',
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
    'AppTokenGet' => [
        'httpMethod' => 'GET',
        'uri' => '/management/{org_name_or_uuid}/{app_name_or_uuid}/token',
        'notes' => 'Get the Application user access token.  See the OAuth2 specification for details.',
        'summary' => 'Get Application access token',
        'responseClass' => '',
        'responseType' => 'model',
        'errorResponses' => $errors,
        'parameters' => [
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
            ],
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
    'AdminUserPost' => [
        'httpMethod' => 'POST',
        'uri' => '/management/{org_name_or_uuid}/users',
        'notes' => 'Create Admin User .  See Usergrid documentation for JSON format of body.',
        'summary' => 'Create Admin User',
        'responseClass' => '',
        'responseType' => 'model',
        'errorResponses' => $errors,
        'parameters' => [
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
                'description' => 'Admin User username'
            ],
            'email' => [
                'location' => 'json',
                'type' => 'string',
                'required' => true,
                'description' => 'Admin User email address'
            ],
            'name' => [
                'location' => 'json',
                'type' => 'string',
                'required' => true,
                'description' => 'Admin User Full name'
            ],
            'password' => [
                'location' => 'json',
                'type' => 'string',
                'required' => true,
                'description' => 'Admin User Password Word'
            ],
            'access_token' => [
                'description' => 'The OAuth2 access token',
                'location' => 'query',
                'type' => 'string',
                'required' => false,
            ],
        ],
        'additionalParameters' => [
            'location' => 'json'
        ]
    ],
    'AdminUserPut' => [
        'httpMethod' => 'PUT',
        'uri' => '/management/{org_name_or_uuid}/users/{user_name_or_email}',
        'notes' => 'Update Admin User .  See Usergrid documentation for JSON format of body.',
        'summary' => 'Create Admin User',
        'responseClass' => '',
        'responseType' => 'model',
        'errorResponses' => $errors,
        'parameters' => [
            'org_name_or_uuid' => [
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'description' => 'Organization name or uuid'
            ],
            'user_name_or_email' => [
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'description' => 'username or email of admin user'
            ],
            'access_token' => [
                'description' => 'The OAuth2 access token',
                'location' => 'query',
                'type' => 'string',
                'required' => false,
            ]
        ],
        'additionalParameters' => [
            'location' => 'json'
        ]
    ],
    'AdminUserGet' => [
        'httpMethod' => 'GET',
        'uri' => '/management/{org_name_or_uuid}/users/{user_name_or_email}',
        'notes' => 'Get Admin User .  See Usergrid documentation for JSON format of body.',
        'summary' => 'Get Admin User',
        'responseClass' => '',
        'responseType' => 'model',
        'errorResponses' => $errors,
        'parameters' => [
            'org_name_or_uuid' => [
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'description' => 'Organization name or uuid'
            ],
            'user_name_or_email' => [
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'description' => 'username or email of admin user'
            ],
            'access_token' => [
                'description' => 'The OAuth2 access token',
                'location' => 'query',
                'type' => 'string',
                'required' => false,
            ]
        ]
    ],
    'AdminUserPassword' => [
        'httpMethod' => 'PUT',
        'uri' => '/management/{org_name_or_uuid}/users/{user_name_or_email}/password',
        'notes' => 'Set Admin User Password.  See Usergrid documentation for JSON format of body.',
        'summary' => 'Get Admin User',
        'responseClass' => '',
        'responseType' => 'model',
        'errorResponses' => $errors,
        'parameters' => [
            'org_name_or_uuid' => [
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'description' => 'Organization name or uuid'
            ],
            'user_name_or_email' => [
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'description' => 'username or email of admin user'
            ],
            'access_token' => [
                'description' => 'The OAuth2 access token',
                'location' => 'query',
                'type' => 'string',
                'required' => false,
            ],
            'password' => [
                'description' => 'The old password',
                'location' => 'json',
                'type' => 'string',
                'required' => true,
            ],
            'newpassword' => [
                'description' => 'The new password',
                'location' => 'json',
                'type' => 'string',
                'required' => true,
            ]
        ]
    ],

    //Management Organizations
    'CreateOrg' => [
        'httpMethod' => 'POST',
        'uri' => '/management/organizations',
        'notes' => 'Create new Organization.  See Usergrid documentation for JSON format of body.',
        'summary' => 'Create New Organization',
        'responseClass' => '',
        'responseType' => 'model',
        'errorResponses' => $errors,
        'parameters' => [
            'access_token' => [
                'description' => 'The OAuth2 access token',
                'location' => 'query',
                'type' => 'string',
                'required' => false,
            ],
            'organization' => [
                'description' => 'Organization Name',
                'location' => 'json',
                'type' => 'string',
                'required' => true,
            ],
            'username' => [
                'description' => 'Admin User  Name',
                'location' => 'json',
                'type' => 'string',
                'required' => true,
            ],
            'name' => [
                'description' => 'Admin Users Full Name',
                'location' => 'json',
                'type' => 'string',
                'required' => true,
            ],
            'email' => [
                'description' => 'Admin Users email',
                'location' => 'json',
                'type' => 'string',
                'required' => true,
            ],
            'password' => [
                'description' => 'Admin Users password',
                'location' => 'json',
                'type' => 'string',
                'required' => true,
            ]
        ],
        'additionalParameters' => [
            'location' => 'json'
        ]
    ],
];