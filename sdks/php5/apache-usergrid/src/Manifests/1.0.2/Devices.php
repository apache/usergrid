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

    'all' => [
        'httpMethod' => 'GET',
        'uri' => '/{org_name_or_uuid}/{app_name_or_uuid}/{collection}',
        'notes' => 'Get All devices.',
        'summary' => 'Get all Device collection limit 10000',
        'responseClass' => 'Apache\Usergrid\Api\Models\Device',
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
            ],
            'collection' => [
                'description' => 'collection name (entity type)',
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'default' => 'devices'
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
                'required' => false

            ],
            'filter' => [
                'description' => 'a condition to filter on',
                'location' => 'query',
                'type' => 'integer',
                'required' => false,
            ]
        ],
        'additionalParameters' => [
            "description" => "Other parameters",
            'location' => 'query'
        ]
    ],
    'find' => [
        'httpMethod' => 'GET',
        'uri' => '/{org_name_or_uuid}/{app_name_or_uuid}/{collection}',
        'notes' => 'Query Devices.',
        'summary' => 'Query the devices collection',
        'responseClass' => 'Apache\Usergrid\Api\Models\Device',
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
            ],
            'collection' => [
                'description' => 'collection name (entity type)',
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'default' => 'devices'
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
            ]
        ],
        'additionalParameters' => [
            "description" => "Other parameters",
            'location' => 'query'
        ]
    ],
    'findById' => [
        'httpMethod' => 'GET',
        'uri' => '/{org_name_or_uuid}/{app_name_or_uuid}/{collection}/{uuid}',
        'notes' => 'Find Device by uuid.',
        'summary' => 'Find device by uuid',
        'responseClass' => 'Apache\Usergrid\Api\Models\Device',
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
            ],
            'collection' => [
                'description' => 'collection name (entity type)',
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'default' => 'devices'
            ],
            'uuid' => [
                'description' => 'Group UUID (entity uuid)',
                'location' => 'uri',
                'type' => 'string',
                'required' => true
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
            ]
        ],
        'additionalParameters' => [
            "description" => "Other parameters",
            'location' => 'query'
        ]
    ],
    'create' => [
        'httpMethod' => 'POST',
        'uri' => '/{org_name_or_uuid}/{app_name_or_uuid}/{collection}',
        'notes' => 'Create new Device.  See Usergrid documentation for JSON format of body.',
        'summary' => 'Create new Device entity',
        'responseClass' => 'Apache\Usergrid\Api\Models\Device',
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
                'default' => 'devices'
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
    'destroy' => [
        'httpMethod' => 'DELETE',
        'uri' => '/{org_name_or_uuid}/{app_name_or_uuid}/{collection}/{entity_name_or_uuid}',
        'notes' => 'Delete a Device entity.',
        'summary' => 'Delete a Device entity by name or uuid',
        'responseClass' => 'Apache\Usergrid\Api\Models\Device',
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
                'default' => 'devices'
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
    'update' => [
        'httpMethod' => 'PUT',
        'uri' => '/{org_name_or_uuid}/{app_name_or_uuid}/{collection}/{entity_name_or_uuid}',
        'notes' => 'Update a Device entity.',
        'summary' => 'Update a Device entity by name or uuid and using JSON data',
        'responseClass' => 'Apache\Usergrid\Api\Models\Device',
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
                'default' => 'devices'
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
    'notifiers' => [
        'httpMethod' => 'GET',
        'uri' => '/{org_name_or_uuid}/{app_name_or_uuid}/{collection}/{device_uuid}/notifier',
        'notes' => 'Get All device notifiers.',
        'summary' => 'Get all Device notifiers',
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
            'org_name_or_uuid' => [
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'description' => 'Organization name or uuid'
            ],
            'collection' => [
                'description' => 'collection name (entity type)',
                'location' => 'uri',
                'type' => 'string',
                'required' => true,
                'default' => 'devices'
            ],
            'device_uuid' => [
                'description' => 'device uuid (entity type)',
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
                'required' => false

            ],
            'filter' => [
                'description' => 'a condition to filter on',
                'location' => 'query',
                'type' => 'integer',
                'required' => false,
            ]
        ],
        'additionalParameters' => [
            "description" => "Other parameters",
            'location' => 'query'
        ]
    ],
];