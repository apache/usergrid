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

use HWI\Bundle\OAuthBundle\Security\Http\ResourceOwnerMap;
use Symfony\Component\Security\Core\SecurityContextInterface;

/**
 * HWIOAuthBundle Aware Refresh token grant type.
 *
 * @package    Apache/Usergrid
 * @version    1.0.0
 * @author     Jason Kristian <jasonkristian@gmail.com>
 * @license    Apache License, Version  2.0
 * @copyright  (c) 2008-2014, Baas Platform Pty. Ltd
 *
 * @link http://tools.ietf.org/html/rfc6749#section-6
 */
class HWIOAuthBundleRefreshToken implements GrantTypeInterface
{
    /** @var SecurityContextInterface Symfony2 security component */
    protected $securityContext;

    /** @var ResourceOwnerMap         HWIOAuthBundle OAuth2 ResourceOwnerMap */
    protected $resourceOwnerMap;

    public function __construct(ResourceOwnerMap $resourceOwnerMap, SecurityContextInterface $securityContext)
    {
        $this->securityContext = $securityContext;
        $this->resourceOwnerMap = $resourceOwnerMap;
    }

    /**
     * {@inheritdoc}
     */
    public function getTokenData($refreshToken = null)
    {
        $token = $this->securityContext->getToken();
        $resourceName = $token->getResourceOwnerName();
        $resourceOwner = $this->resourceOwnerMap->getResourceOwnerByName($resourceName);

        $data = $resourceOwner->refreshAccessToken($refreshToken);
        $token->setRawToken($data);
        $requiredData = array_flip(array('access_token', 'expires_in', 'refresh_token'));

        return array_intersect_key($data, $requiredData);
    }
}
