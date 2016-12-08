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

    'createApple' => [
        'httpMethod' => 'POST',
        'uri' => '/{org_name_or_uuid}/{app_name_or_uuid}/notifiers',
        'notes' => 'Create new Apple Notifier.  See Usergrid documentation for the format of body.',
        'summary' => 'Create new Notifier entity',
        'responseClass' => 'Apache\Usergrid\Api\Models\Notifier',
        'responseType' => 'class',
        'errorResponses' => $errors,
        'parameters' => [
            'app_name_or_uuid' => [
                'description' => 'app name or uuid',
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
            ],
            'name' => [
                'description' => 'notifier name (entity type)',
                'location' => 'postField',
                'type' => 'string',
                'required' => true,
            ],
            'provider' => [
                'description' => 'notifier provider',
                'location' => 'postField',
                'type' => 'string',
                'required' => true,
                'default' => 'apple'
            ],
            'environment' => [
                'description' => 'notifier environment',
                'location' => 'postField',
                'type' => 'string',
                'required' => true,
            ],
            'p12Certificate' => [
                'description' => 'p12Certificate',
                'location' => 'postFile',
                'type' => 'string',
                'required' => true,
                'default' => 'users'
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
            'location' => 'postField'
        ]

    ],
    'createGoogle' => [
        'httpMethod' => 'POST',
        'uri' => '/{org_name_or_uuid}/{app_name_or_uuid}/notifiers',
        'notes' => 'Create new Notifier.  See Usergrid documentation for the format of body.',
        'summary' => 'Create new Notifier entity',
        'responseClass' => 'Apache\Usergrid\Api\Models\Notifier',
        'responseType' => 'class',
        'errorResponses' => $errors,
        'parameters' => [
            'app_name_or_uuid' => [
                'description' => 'app name or uuid',
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
            ],
            'name' => [
                'description' => 'notifier name (entity type)',
                'location' => 'json',
                'type' => 'string',
                'required' => false,
            ],
            'provider' => [
                'description' => 'notifier provider',
                'location' => 'json',
                'type' => 'string',
                'required' => true,
                'default' => 'google'
            ],
            'apiKey' => [
                'description' => 'apiKey',
                'location' => 'json',
                'type' => 'string',
                'required' => true
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
    'all' => [
        'httpMethod' => 'GET',
        'uri' => '/{org_name_or_uuid}/{app_name_or_uuid}/notifiers',
        'notes' => 'Get all  Notifier.  See Usergrid documentation for the format of body.',
        'summary' => 'Get all  Notifier entity',
        'responseClass' => 'Apache\Usergrid\Api\Models\Notifier',
        'responseType' => 'class',
        'errorResponses' => $errors,
        'parameters' => [
            'app_name_or_uuid' => [
                'description' => 'app name or uuid',
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
            ],
            'name' => [
                'description' => 'notifier name (entity type)',
                'location' => 'json',
                'type' => 'string',
                'required' => false,
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
    ]
];