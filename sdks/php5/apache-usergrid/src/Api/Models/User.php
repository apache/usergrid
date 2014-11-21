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

namespace Apache\Usergrid\Api\Models;


use Guzzle\Service\Command\ResponseClassInterface;

/**
 * Class User
 *
 * @package    Apache/Usergrid
 * @version    1.0.0
 * @author     Jason Kristian <jasonkristian@gmail.com>
 * @license    Apache License, Version  2.0
 * @copyright  (c) 2008-2014, Baas Platform Pty. Ltd
 * @link       http://baas-platform.com
 */
class User extends BaseCollection implements ResponseClassInterface
{

    use GuzzleCommandTrait;

    public function deviceAttribute()
    {
        return $this->getApiClient()->application()->GetRelationship([
            'collection' => 'users',
            'entity_id' => $this->entities->fetch('uuid')->first(),
            'relationship' => 'devices'
        ])->toArray();
    }

    public function notificationAttribute()
    {

    }

    public function groupsAttribute()
    {
        return $this->getApiClient()->application()->GetRelationship([
            'collection' => 'users',
            'entity_id' => $this->entities->fetch('uuid')->first(),
            'relationship' => 'groups'
        ])->toArray();
    }

    public function rolesAttribute()
    {
        return $this->getApiClient()->application()->GetRelationship([
            'collection' => 'users',
            'entity_id' => $this->entities->fetch('uuid')->first(),
            'relationship' => 'roles'
        ])->toArray();
    }

    public function connectionsAttribute()
    {
        return $this->getApiClient()->application()->GetRelationship([
            'collection' => 'users',
            'entity_id' => $this->entities->fetch('uuid')->first(),
            'relationship' => 'connections'
        ])->toArray();
    }
}