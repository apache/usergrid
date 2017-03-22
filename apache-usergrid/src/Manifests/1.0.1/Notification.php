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
    'ToGroup' => [
        'httpMethod' => 'POST',
        'uri' => '/{org_name_or_uuid}/{app_name_or_uuid}/groups/{group}/notifications',
        'notes' => 'Create Notification for group.  See Usergrid documentation for JSON format of body.',
        'summary' => 'Create new app notification',
        'responseClass' => '',
        'responseType' => 'object',
        'errorResponses' => $errors,
        'parameters' => [
            'access_token' => [
                'description' => 'The OAuth2 access token',
                'location' => 'query',
                'type' => 'string',
                'required' => false,
            ],
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
            ],
            'group' => [
                'description' => 'group name',
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
            ],

        ]
    ],
    'ToDevice' => [
        'httpMethod' => 'POST',
        'uri' => '/{org_name_or_uuid}/{app_name_or_uuid}/devices/{device_uuid}/notifications',
        'notes' => 'Create Notification for single Device.  See Usergrid documentation for JSON format of body.',
        'summary' => 'Create new app notification',
        'responseClass' => '',
        'responseType' => 'object',
        'errorResponses' => $errors,
        'parameters' => [
            'access_token' => [
                'description' => 'The OAuth2 access token',
                'location' => 'query',
                'type' => 'string',
                'required' => false,
            ],
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
            ],
            'device_uuid' => [
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'description' => 'device name or uuid'
            ],

        ]
    ],
    'ToDevices' => [
        'httpMethod' => 'POST',
        'uri' => '/{org_name_or_uuid}/{app_name_or_uuid}/devices/*/notifications',
        'notes' => 'Create Notification all Devices.  See Usergrid documentation for JSON format of body.',
        'summary' => 'Create new app notification',
        'responseClass' => '',
        'responseType' => 'object',
        'errorResponses' => $errors,
        'parameters' => [
            'access_token' => [
                'description' => 'The OAuth2 access token',
                'location' => 'query',
                'type' => 'string',
                'required' => false,
            ],
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
    'ToUser' => [
        'httpMethod' => 'POST',
        'uri' => '/{org_name_or_uuid}/{app_name_or_uuid}/users/{user_name}/notifications',
        'notes' => 'Create Notification single User.  See Usergrid documentation for JSON format of body.',
        'summary' => 'Create new app notification',
        'responseClass' => '',
        'responseType' => 'object',
        'errorResponses' => $errors,
        'parameters' => [
            'access_token' => [
                'description' => 'The OAuth2 access token',
                'location' => 'query',
                'type' => 'string',
                'required' => false,
            ],
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
            ],
            'user_name' => [
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'description' => 'User name or uuid'
            ],
        ]
    ]
];