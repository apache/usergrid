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
namespace Apache\Usergrid\Laravel;


use Apache\Usergrid\Api\Usergrid;
use Apache\Usergrid\Guzzle\Plugin\Oauth2\GrantType\ClientCredentials;
use Apache\Usergrid\Guzzle\Plugin\Oauth2\GrantType\PasswordCredentials;
use Apache\Usergrid\Guzzle\Plugin\Oauth2\GrantType\RefreshToken;
use Apache\Usergrid\Guzzle\Plugin\Oauth2\Oauth2Plugin;
use Guzzle\Http\Client;
use Illuminate\Support\ServiceProvider;

/**
 * Class ApacheUsergridServiceProvider
 *
 * @package    Apache/Usergrid
 * @version    1.0.0
 * @author     Jason Kristian <jasonkristian@gmail.com>
 * @license    Apache License, Version  2.0
 * @copyright  (c) 2008-2014, Baas Platform Pty. Ltd
 * @link       http://baas-platform.com
 */
class ApacheUsergridServiceProvider extends ServiceProvider
{

    protected $oauth2Plugin = null;

    /**
     *
     */
    public function boot()
    {
        $this->package('apache/usergrid', 'apache/usergrid', __DIR__ . '/..');
    }

    /**
     * Register the service provider.
     *
     * @return void
     */
    public function register()
    {
        // register Usergrid
        $this->registerUsergrid();

    }

    protected function registerUsergrid()
    {

        $this->app['usergrid'] = $this->app->share(function ($app) {

            /** Note: I had to move this to here from the register function as the below config values would not get set and would be null
             * unless I has this with the package namespace missing but doing that would mean that it would not find the  enable_oauth2_plugin
             * value .. This has been driving me crazy as I tried to read the config values from a rout and they would not show up
             * then I did . Also this would not find the config values if the boot function did not have the package method called with
             * all 3 args
             * $enable_oauth2_plugin = $this->app['config']->get('usergrid.enable_oauth2_plugin');
             *
             * //check if user managed oauth auth flow
             * if($enable_oauth2_plugin){
             * // Create the Oauth2 Guzzle Plugin.
             * $this->createGuzzleOauth2Plugin();
             * }
             */
            $enable_oauth2_plugin = $app['config']->get('apache/usergrid::usergrid.enable_oauth2_plugin');

            //check if user managed oauth auth flow
            if ($enable_oauth2_plugin) {
                // Create the Oauth2 Guzzle Plugin.
                $this->createGuzzleOauth2Plugin();
            }

            $baseUrl = $app['config']->get('apache/usergrid::usergrid.url');

            $orgName = $app['config']->get('apache/usergrid::usergrid.orgName');

            $appName = $app['config']->get('apache/usergrid::usergrid.appName');

            $manifestPath = $app['config']->get('apache/usergrid::usergrid.manifestPath');

            $version = $app['config']->get('apache/usergrid::usergrid.version');

            return new Usergrid($orgName, $appName, $manifestPath, $version, $baseUrl, $this->oauth2Plugin);
        });

        $this->app->alias('usergrid', 'Apache\Usergrid\Api\Usergrid');
    }


    protected function createGuzzleOauth2Plugin()
    {

        $base_url = $this->app['config']->get('apache/usergrid::usergrid.url');

        $org_name = $this->app['config']->get('apache/usergrid::usergrid.orgName');

        $app_name = $this->app['config']->get('apache/usergrid::usergrid.appName');

        $grant_type = $this->app['config']->get('apache/usergrid::usergrid.grant_type');

        $client_id = $this->app['config']->get('apache/usergrid::usergrid.clientId');

        $client_secret = $this->app['config']->get('apache/usergrid::usergrid.clientSecret');

        $username = $this->app['config']->get('apache/usergrid::usergrid.username');

        $password = $this->app['config']->get('apache/usergrid::usergrid.password');


        if ($this->app['config']->get('apache/usergrid::usergrid.auth_type') == 'organization') {

            $url = $base_url . '/management/token';

        } elseif ($this->app['config']->get('apache/usergrid::usergrid.auth_type') == 'application') {
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