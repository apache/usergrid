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

namespace Apache\Usergrid\Native;


use Apache\Usergrid\Api\Usergrid;
use Apache\Usergrid\Guzzle\Plugin\Oauth2\GrantType\ClientCredentials;
use Apache\Usergrid\Guzzle\Plugin\Oauth2\GrantType\PasswordCredentials;
use Apache\Usergrid\Guzzle\Plugin\Oauth2\GrantType\RefreshToken;
use Apache\Usergrid\Guzzle\Plugin\Oauth2\Oauth2Plugin;
use Guzzle\Http\Client;

/**
 * Class UsergridBootstrapper
 *
 * @package    Apache/Usergrid
 * @version    1.0.0
 * @author     Jason Kristian <jasonkristian@gmail.com>
 * @license    Apache License, Version  2.0
 * @copyright  (c) 2008-2014, Baas Platform Pty. Ltd
 * @link       http://baas-platform.com
 */
class UsergridBootstrapper
{
    /**
     * The Usergrid configuration.
     *
     * @var array
     */
    protected $config;

    /**
     * The Oauth2 Plugin.
     *
     * @var \Apache\Usergrid\Guzzle\Plugin\Oauth2\Oauth2Plugin
     */
    protected $oauth2Plugin = null;

    /**
     * Constructor.
     *
     * @param  mixed $config
     * @return \Apache\Usergrid\Native\UsergridBootstrapper
     */
    public function __construct($config = null)
    {
        $this->config = $config ?: new ConfigRepository($config);
    }

    /**
     * Creates the Usergrid instance.
     *
     * @return \Apache\Usergrid\Api\Usergrid
     */
    public function createUsergrid()
    {
        $baseUrl = array_get($this->config, 'usergrid.url');

        $orgName = array_get($this->config, 'usergrid.orgName');

        $appName = array_get($this->config, 'usergrid.appName');

        $manifestPath = array_get($this->config, 'usergrid.manifestPath');

        $version = array_get($this->config, 'usergrid.version');

        $enable_oauth2_plugin = array_get($this->config, 'usergrid.enable_oauth2_plugin');

        //check if user wants to manage there own Oauth 2 auth flow
        if ($enable_oauth2_plugin) {

            $this->createOauth2Plugin();

            return new Usergrid($orgName, $appName, $manifestPath, $version, $baseUrl, $this->oauth2Plugin);
        } else {
            return new Usergrid($orgName, $appName, $manifestPath, $version, $baseUrl);
        }

    }

    private function createOauth2Plugin()
    {
        $base_url = array_get($this->config, 'usergrid.url');

        $client_id = array_get($this->config, 'usergrid.clientId');

        $client_secret = array_get($this->config, 'usergrid.clientSecret');

        $grant_type = array_get($this->config, 'usergrid.grant_type');

        $auth_type = array_get($this->config, 'usergrid.auth_type');

        $username = array_get($this->config, 'usergrid.username');

        $password = array_get($this->config, 'usergrid.password');


        $org_name = array_get($this->config, 'usergrid.orgName');

        $app_name = array_get($this->config, 'usergrid.appName');

        if ($auth_type == 'organization') {

            $url = $base_url . '/management/token';

        } elseif ($auth_type == 'application') {
            $url = $base_url . '/' . $org_name . '/' . $app_name . '/token';
        }

        $oauth2Client = new Client($url);


        if ($grant_type == 'client_credentials') {
            $config = [
                'client_id' => $client_id,
                'client_secret' => $client_secret,

            ];
            $grantType = new ClientCredentials($oauth2Client, $config);
            $refreshTokenGrantType = new RefreshToken($oauth2Client, $config);
            $this->oauth2Plugin = new Oauth2Plugin($grantType, $refreshTokenGrantType);

        } elseif ($grant_type == 'password') {
            $config = [
                'username' => $username,
                'password' => $password,
                'client_id' => $client_id,
                'client_secret' => $client_secret
            ];
            $grantType = new PasswordCredentials($oauth2Client, $config);
            $refreshTokenGrantType = new RefreshToken($oauth2Client, $config);
            $this->oauth2Plugin = new Oauth2Plugin($grantType, $refreshTokenGrantType);
        }
    }

}