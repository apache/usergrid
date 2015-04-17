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

namespace Apache\Usergrid\Tests\Api;

use Apache\Usergrid\Api\Exception\UnauthorizedException;
use PHPUnit_Framework_TestCase;

/**
 * Class UsergridTest
 *
 * @package    Apache/Usergrid
 * @version    1.0.0
 * @author     Jason Kristian <jasonkristian@gmail.com>
 * @license    Apache License, Version  2.0
 * @copyright  (c) 2008-2014, Baas Platform Pty. Ltd
 * @link       http://baas-platform.com
 */
class UsergridTest extends PHPUnit_Framework_TestCase
{

    /** @var  Usergrid Api Client */
    protected $usergrid;

    protected $config;

    /**
     * Setup resources and dependencies
     *
     * @return void
     */
    public function setup()
    {
        $this->usergrid = $GLOBALS['usergrid'];
    }

    /**
     * @test
     * @group internet
     */
    public function it_can_retrieve_oauth2_token()
    {
        $error = null;

        try {
            $this->usergrid->application()->EntityGet(['collection' => 'roles']);
        } catch (UnauthorizedException $e) {
            $error = $e;
        }

        $this->assertNull($error, 'Exception should be null if authorized');
    }

    /** @test */
    public function it_can_set_the_oauth2_token()
    {
        $this->usergrid->setToken('ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890');

        $this->assertEquals('ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890', $this->usergrid->getToken());
    }

    /** @test */
    public function it_can_retrieve_user_agent()
    {

        $this->assertEquals('BaaS-Usergrid/1.0.0', $this->usergrid->getUserAgent());
    }

    /** @test */
    public function it_can_set_the_user_agent()
    {

        $this->usergrid->setUserAgent('Foo/Bar');

        $this->assertEquals('Foo/Bar', $this->usergrid->getUserAgent());
    }

    /** @test */
    public function it_can_retrieve_the_manifest_path()
    {

        $this->assertEquals($this->usergrid->getManifestPath(), $this->usergrid->getManifestPath());
    }

    /** @test */
    public function it_can_set_the_manifest_path()
    {
        $this->usergrid->setManifestPath('/usr/foo/bar');

        $this->assertEquals('/usr/foo/bar', $this->usergrid->getManifestPath());
    }

    /** @test */
    public function it_can_retrieve_api_version()
    {
        $this->assertEquals('1.0.1', $this->usergrid->getVersion());
    }

    /** @test */
    public function it_can_set_api_version()
    {
        $this->usergrid->setVersion('1.0.1');

        $this->assertEquals('1.0.1', $this->usergrid->getVersion());
    }


    /** @test */
    public function it_can_retrieve_the_client_header()
    {
        $headers = $this->usergrid->getHeaders();

        $expected = [
            'Usergrid-Version' => '1.0.1',
            'Authorization' => 'Bearer ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890'
        ];

        $this->assertEquals($headers, $expected);
    }

    /** @test */
    public function it_can_set_client_headers()
    {
        $this->usergrid->setHeaders([
            'some-header' => 'foo-bar',
        ]);

        $headers = $this->usergrid->getHeaders();

        $expected = [
            'Usergrid-Version' => '1.0.1',
            'Authorization' => 'Bearer ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890',
            'some-header' => 'foo-bar',
        ];

        $this->assertEquals($headers, $expected);
    }


}