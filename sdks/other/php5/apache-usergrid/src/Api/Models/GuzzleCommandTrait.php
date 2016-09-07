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

use Guzzle\Service\Command\OperationCommand;

/**
 * Class GuzzleCommandTrait
 *
 * @package    Apache/Usergrid
 * @version    1.0.0
 * @author     Jason Kristian <jasonkristian@gmail.com>
 * @license    Apache License, Version  2.0
 * @copyright  (c) 2008-2014, Baas Platform Pty. Ltd
 * @link       http://baas-platform.com
 */
trait GuzzleCommandTrait
{
    /**
     * Create a response model object from a completed command.
     *
     * @param OperationCommand $command That serialized the request
     * @return \Illuminate\Support\Collection
     */
    public static function fromCommand(OperationCommand $command)
    {
        // Initialize the collection
        $collection = new self($command->getResponse()->json());

        // Set the Usergrid API client on the collection
        $collection->setApiClient($command->getClient()->getApiClient());

        // Return the collection
        return $collection;
    }

    /**
     *  Returns true if there is a cursor or false if there is not
     * @return bool
     */
    public function hasNextPage()
    {
        return $this->has('cursor');
    }
} 