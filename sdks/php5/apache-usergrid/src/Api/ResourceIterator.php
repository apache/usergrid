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


namespace Apache\Usergrid\Api;

use Guzzle\Service\Command\CommandInterface;
use Guzzle\Service\Resource\ResourceIterator as BaseIterator;

/**
 * Class ResourceIterator
 *
 * @package    Apache/Usergrid
 * @version    1.0.0
 * @author     Jason Kristian <jasonkristian@gmail.com>
 * @license    Apache License, Version  2.0
 * @copyright  (c) 2008-2014, Baas Platform Pty. Ltd
 * @link       http://baas-platform.com
 */
class ResourceIterator extends BaseIterator
{
    /**
     * {@inheritDoc}
     */
    public function __construct(CommandInterface $command, array $data = [])
    {
        parent::__construct($command, $data);

        $this->pageSize = 20;
    }

    /**
     * Send a request to retrieve the next page of results. Hook for subclasses to implement.
     *
     * @return array Returns the newly loaded resources
     */
    protected function sendRequest()
    {
        $this->command->set('limit', $this->pageSize);

        if ($this->nextToken) {
            $this->command->set('cursor', $this->nextToken);
        }

        $result = $this->command->execute();

        $data = $result['entities'];

        if ($result->has('cursor')) {
            $this->nextToken = $result['cursor'] ? $result['cursor'] : false;
        }

        return $data;
    }

} 