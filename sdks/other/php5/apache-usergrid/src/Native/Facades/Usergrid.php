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

namespace Apache\Usergrid\Native\Facades;


use Apache\Usergrid\Native\UsergridBootstrapper;

/**
 * Class Usergrid
 *
 * @package    Apache/Usergrid
 * @version    1.0.0
 * @author     Jason Kristian <jasonkristian@gmail.com>
 * @license    Apache License, Version  2.0
 * @copyright  (c) 2008-2014, Baas Platform Pty. Ltd
 * @link       http://baas-platform.com
 */
class Usergrid
{
    /**
     * The Native Bootstrap instance.
     *
     * @var \Apache\Usergrid\Native\UsergridBootstrapper
     */
    protected static $instance;
    /**
     * The Usergrid API instance.
     *
     * @var \Apache\Usergrid\Api\Usergrid
     */
    protected $usergrid;

    /**
     * Constructor.
     *
     * @param  \Apache\Usergrid\Native\UsergridBootstrapper $bootstraper
     * @return \Apache\Usergrid\Native\Facades\Usergrid
     */
    public function __construct(UsergridBootstrapper $bootstraper = null)
    {
        if (!$bootstraper) {
            $bootstraper = new UsergridBootstrapper;
        }

        $this->usergrid = $bootstraper->createUsergrid();
    }

    /**
     * Handle dynamic, static calls to the object.
     *
     * @param  string $method
     * @param  array $args
     * @return mixed
     */
    public static function __callStatic($method, $args)
    {
        $instance = static::instance()->getUsergrid();

        switch (count($args)) {
            case 0:
                return $instance->{$method}();

            case 1:
                return $instance->{$method}($args[0]);

            case 2:
                return $instance->{$method}($args[0], $args[1]);

            case 3:
                return $instance->{$method}($args[0], $args[1], $args[2]);

            case 4:
                return $instance->{$method}($args[0], $args[1], $args[2], $args[3]);

            default:
                return call_user_func_array([$instance, $method], $args);
        }
    }

    /**
     * Creates a new Native Bootstraper instance.
     *
     * @param  \Apache\Usergrid\Native\UsergridBootstrapper $bootstrapper
     * @return static
     */
    public static function instance(UsergridBootstrapper $bootstrapper = null)
    {
        if (static::$instance === null) {
            static::$instance = new static($bootstrapper);
        }

        return static::$instance;
    }

    /**
     * Returns the Usergrid API instance.
     *
     * @return \Apache\Usergrid\Api\Usergrid
     */
    public function getUsergrid()
    {
        return $this->usergrid;
    }
} 