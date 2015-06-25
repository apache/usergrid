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

namespace Apache\Usergrid\Guzzle\Plugin\Oauth2\GrantType;

use Guzzle\Common\Collection;
use Guzzle\Http\ClientInterface;

/**
 * Authorization code grant type.
 *
 * @package    Apache/Usergrid
 * @version    1.0.0
 * @author     Jason Kristian <jasonkristian@gmail.com>
 * @license    Apache License, Version  2.0
 * @copyright  (c) 2008-2014, Baas Platform Pty. Ltd
 *
 * @link http://tools.ietf.org/html/rfc6749#section-4.1
 */
class AuthorizationCode implements GrantTypeInterface
{
    /** @var ClientInterface The token endpoint client */
    protected $client;

    /** @var Collection Configuration settings */
    protected $config;

    public function __construct(ClientInterface $client, $config)
    {
        $this->client = $client;
        $this->config = Collection::fromConfig($config, array(
            'client_secret' => '',
            'scope' => '',
            'redirect_uri' => '',
        ), array(
            'client_id',
            'code',
        ));
    }

    /**
     * {@inheritdoc}
     */
    public function getTokenData()
    {
        $postBody = array(
            'grant_type' => 'authorization_code',
            'code' => $this->config['code'],
        );
        if ($this->config['scope']) {
            $postBody['scope'] = $this->config['scope'];
        }
        if ($this->config['redirect_uri']) {
            $postBody['redirect_uri'] = $this->config['redirect_uri'];
        }
        $request = $this->client->post(null, array(), $postBody);
        $request->setAuth($this->config['client_id'], $this->config['client_secret']);
        $response = $request->send();
        $data = $response->json();

        $requiredData = array_flip(array('access_token', 'expires_in', 'refresh_token'));
        return array_intersect_key($data, $requiredData);
    }
}
