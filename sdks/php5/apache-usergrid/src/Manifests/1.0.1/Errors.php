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
    [
        'class' => 'Apache\Usergrid\Api\Exception\BadRequestException',
        'code' => 400,
    ],
    [
        'class' => 'Apache\Usergrid\Api\Exception\UnauthorizedException',
        'code' => 401,
    ],
    [
        'class' => 'Apache\Usergrid\Api\Exception\RequestFailedException',
        'code' => 402,
    ],
    [
        'class' => 'Apache\Usergrid\Api\Exception\NotFoundException',
        'code' => 404,
    ],
    [
        'class' => 'Apache\Usergrid\Api\Exception\ServerErrorException',
        'code' => 500,
    ],
    [
        'class' => 'Apache\Usergrid\Api\Exception\ServerErrorException',
        'code' => 502,
    ],
    [
        'class' => 'Apache\Usergrid\Api\Exception\ServerErrorException',
        'code' => 503,
    ],
    [
        'class' => 'Apache\Usergrid\Api\Exception\ServerErrorException',
        'code' => 504,
    ],
];