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


use Apache\Usergrid\Api\Usergrid;
use Illuminate\Support\Collection;

/**
 * Class BaseCollection
 *
 * @package    Apache/Usergrid
 * @version    1.0.0
 * @author     Jason Kristian <jasonkristian@gmail.com>
 * @license    Apache License, Version  2.0
 * @copyright  (c) 2008-2014, Baas Platform Pty. Ltd
 * @link       http://baas-platform.com
 */
class BaseCollection extends Collection
{
    /**
     * List of API response properties that'll be
     * automatically converted into collections.
     *
     * @var array
     */
    protected $collections = ['entities'];

    /**
     * The Usergrid API client instance.
     *
     * @var /Apache\Usergrid\Api\Usergrid
     */
    protected $apiClient;

    /**
     * Returns the given key value from the collection.
     *
     * @param  mixed $key
     * @return mixed
     */
    public function __get($key)
    {
        if (in_array($key, $this->collections) || array_key_exists($key, $this->collections)) {
            if ($mappedKey = array_get($this->collections, $key, [])) {
                $key = strstr($mappedKey, '.', true);

                $query = ltrim(strstr($mappedKey, '.'), '.');

                $data = array_get($this->get($key), $query, []);
            } else {
                $data = $this->get($key, []);
            }
            // create a new class of self so subclasses get new collection from entities of the correct model
            // eg: before when asking  the User Model for its entities returned a collection of the BaseCollection class not the user class..
            $class = self::instance($data);
            //set the api client on collection so api attribute properties can make api calls to get related models
            $class->setApiClient($this->getApiClient());
            // return class.
            return $class;

        }
        // check to see if the property you asked for is a attribute method
        if (method_exists($this, $method = "{$key}Attribute")) {
            return $this->{$method}($this->get($key));
        }

        //else return key
        return $this->get($key, null);
    }

    /**
     * @param $data
     * @return static
     */
    private static function instance($data)
    {
        return new static($data);
    }

    /**
     * Returns the Usergrid API client instance.
     *
     * @return  /Apache\Usergrid\Api\Usergrid  /Apache\Usergrid\Api\Usergrid
     */
    public function getApiClient()
    {
        return $this->apiClient;
    }

    /**
     * Sets the Usergrid API client instance.
     *
     * @param Usergrid $client
     * @internal param $ /Apache\Usergrid\Api\Usergrid $client
     * @return void
     */
    public function setApiClient(Usergrid $client)
    {
        $this->apiClient = $client;
    }
} 