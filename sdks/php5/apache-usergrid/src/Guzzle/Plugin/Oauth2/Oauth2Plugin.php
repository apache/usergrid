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
namespace Apache\Usergrid\Guzzle\Plugin\Oauth2;

use Apache\Usergrid\Guzzle\Plugin\Oauth2\GrantType\GrantTypeInterface;
use Guzzle\Common\Event;
use Guzzle\Http\Exception\BadResponseException;
use Symfony\Component\EventDispatcher\EventSubscriberInterface;

/**
 * OAuth2 plugin
 *
 * @package    Apache/Usergrid
 * @version    1.0.0
 * @author     Jason Kristian <jasonkristian@gmail.com>
 * @license    Apache License, Version  2.0
 * @copyright  (c) 2008-2014, Baas Platform Pty. Ltd
 *
 * @link http://tools.ietf.org/html/rfc6749
 */
class Oauth2Plugin implements EventSubscriberInterface
{

    /** @var GrantTypeInterface The grant type implementation used to acquire access tokens */
    protected $grantType;

    /** @var GrantTypeInterface The grant type implementation used to refresh access tokens */
    protected $refreshTokenGrantType;

    /** @var array An array with the "access_token" and "expires" keys */
    protected $accessToken;

    /** @var string The refresh token string. * */
    protected $refreshToken;

    /**
     * Create a new Oauth2 plugin
     * @param GrantTypeInterface $grantType
     * @param GrantTypeInterface $refreshTokenGrantType
     */
    public function __construct(GrantTypeInterface $grantType = null, GrantTypeInterface $refreshTokenGrantType = null)
    {
        $this->grantType = $grantType;
        $this->refreshTokenGrantType = $refreshTokenGrantType;
    }

    /**
     * {@inheritdoc}
     */
    public static function getSubscribedEvents()
    {
        return array(
            'request.before_send' => 'onRequestBeforeSend',
            'request.error' => 'onRequestError',
        );
    }

    /**
     * Request before-send event handler.
     *
     * Adds the Authorization header if an access token was found.
     *
     * @param Event $event Event received
     */
    public function onRequestBeforeSend(Event $event)
    {
        $accessToken = $this->getAccessToken();
        if ($accessToken) {
            $event['request']->setHeader('Authorization', 'Bearer ' . $accessToken['access_token']);
        }
    }

    /**
     * Get the access token.
     *
     * Handles token expiration for tokens with an "expires" timestamp.
     * In case no valid token was found, tries to acquire a new one.
     *
     * @return array|null
     */
    public function getAccessToken()
    {
        if (isset($this->accessToken['expires']) && $this->accessToken['expires'] < time()) {
            // The access token has expired.
            $this->accessToken = null;
        }
        if (!$this->accessToken) {
            // Try to acquire a new access token from the server.
            $this->acquireAccessToken();
        }

        return $this->accessToken;
    }

    /**
     * Set the access token.
     *
     * @param array|string $accessToken The access token
     */
    public function setAccessToken(array $accessToken)
    {
        $this->accessToken = $accessToken;
    }

    /**
     * Acquire a new access token from the server.
     *
     * @return array|null
     */
    protected function acquireAccessToken()
    {
        if ($this->refreshTokenGrantType && $this->refreshToken) {
            try {
                // Get an access token using the stored refresh token.
                $tokenData = $this->refreshTokenGrantType->getTokenData($this->refreshToken);
            } catch (BadResponseException $e) {
                // The refresh token has probably expired.
                $this->refreshToken = null;
            }
        }
        if ($this->grantType && !isset($tokenData)) {
            // Get a new access token.
            $tokenData = $this->grantType->getTokenData();
        }

        $this->accessToken = null;
        if (isset($tokenData)) {
            // Process the returned data. Both expired_in and refresh_token
            // are optional parameters.
            $this->accessToken = array(
                'access_token' => $tokenData['access_token'],
            );
            if (isset($tokenData['expires_in'])) {
                $this->accessToken['expires'] = time() + $tokenData['expires_in'];
            }
            if (isset($tokenData['refresh_token'])) {
                $this->refreshToken = $tokenData['refresh_token'];
            }
        }

        return $this->accessToken;
    }

    /**
     * Request error event handler.
     *
     * Handles unauthorized errors by acquiring a new access token and
     * retrying the request.
     *
     * @param Event $event Event received
     */
    public function onRequestError(Event $event)
    {
        if ($event['response']->getStatusCode() == 401) {
            if ($event['request']->getHeader('X-Guzzle-Retry')) {
                // We already retried once, give up.
                return;
            }

            // Acquire a new access token, and retry the request.
            $newAccessToken = $this->acquireAccessToken();
            if ($newAccessToken) {
                $newRequest = clone $event['request'];
                $newRequest->setHeader('Authorization', 'Bearer ' . $newAccessToken['access_token']);
                $newRequest->setHeader('X-Guzzle-Retry', '1');
                $event['response'] = $newRequest->send();
                $event->stopPropagation();
            }
        }
    }

    /**
     * Get the refresh token.
     *
     * @return string
     */
    public function getRefreshToken()
    {
        return $this->refreshToken;
    }

    /**
     * Set the refresh token.
     *
     * @param string $refreshToken The refresh token
     */
    public function setRefreshToken($refreshToken)
    {
        $this->refreshToken = $refreshToken;
    }

    /**
     * @return GrantTypeInterface
     */
    public function getGrantType()
    {
        return $this->grantType;
    }
}
