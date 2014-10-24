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

use Apache\Usergrid\Native\UsergridBootstrapper;
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

    protected $config = [
        'usergrid' => [
            'url' => 'https://api.usergrid.com',
            'version' => '1.0.0',
            'orgName' => null,
            'appName' => null,
            'manifestPath' => './src/Manifests/1.0.1',
            'clientId' => null,
            'clientSecret' => null,
            'username' => null,
            'password' => null,
            /**
             * The Auth Type setting is the Oauth 2 end point you want to get the OAuth 2
             * Token from.  You have two options here one is 'application' the other is 'organization'
             *
             *  organization will get the the token from http://example.com/management using  client_credentials or password grant type
             *  application will get the token from http://example.com/managment/org_name/app_name using client_credentials or password grant type
             */
            'auth_type' => 'organization',
            /** The Grant Type to use
             *
             * This has to be set to one of the 2 grant types that Apache Usergrid
             * supports which at the moment is client_credentials or password but at
             * 2 level organization or application
             */
            'grant_type' => 'client_credentials'

        ]
    ];
    /**
     * Setup resources and dependencies
     *
     * @return void
     */
    public function setup()
    {
        $boostrap = new UsergridBootstrapper($this->config);
        $this->usergrid = $boostrap->createUsergrid();
    }

    /** @test */
    public function it_can_retrieve_oauth2_token()
    {
        //TODO: phpunit test method implementation
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

        $this->assertEquals('./src/Manifests/1.0.1', $this->usergrid->getManifestPath());
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
        $this->assertEquals('1.0.0', $this->usergrid->getVersion());
    }

    /** @test */
    public function it_can_set_api_version()
    {
        $this->usergrid->setVersion('1.0.0');

        $this->assertEquals('1.0.0', $this->usergrid->getVersion());
    }


    /** @test */
    public function it_can_retrieve_the_client_header()
    {
        $headers = $this->usergrid->getHeaders();

        $expected = [
            'Usergrid-Version' => '1.0.0',
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
            'some-header'    => 'foo-bar',
            'Usergrid-Version' => '1.0.0',
        ];

        $this->assertEquals($headers, $expected);
    }

    /** @test */
    public function it_can_retrieve_client_id_and_secret()
    {
        //TODO: phpunit test method implementation
    }

    /** @test */
    public function it_can_set_client_id_and_secret()
    {
        //TODO: phpunit test method implementation
    }


}