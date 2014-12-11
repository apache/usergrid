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


use Apache\Usergrid\Guzzle\Plugin\Oauth2\Oauth2Plugin;
use Guzzle\Common\Event;
use Guzzle\Plugin\ErrorResponse\ErrorResponsePlugin;
use Guzzle\Service\Description\ServiceDescription;
use InvalidArgumentException;

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
     *  Header Bearer Token
     * @var
     */
    protected $token;


    /**
     * @var \Apache\Usergrid\Guzzle\Plugin\Oauth2\GrantType\GrantTypeInterface
     */
    protected $grantType;
    /**
     * The Usergrid API version.
     *
     * @var string
     */
    protected $version = '1.0.1';
    /**
     * The manifests path.
     *
     * @var string
     */
    protected $manifestPath;
    /**
     * The Base URL.
     *
     * @var string
     */
    protected $baseUrl;
    /**
     * Oauth2 Guzzle Plugin.
     *
     * @var \Apache\Usergrid\Guzzle\Plugin\Oauth2\Oauth2Plugin
     */
    protected $oauth2_plugin;
    /**
     * The cached manifests data.
     *
     * @var array
     */
    protected $manifests = [];
    /**
     * The user agent.
     *
     * @var string
     */
    protected $userAgent = 'BaaS-Usergrid/1.0.0';
    /**
     * The headers to be sent to the Guzzle client.
     *
     * @var array
     */
    protected $headers = [];
    private $org_name;
    private $app_name;

    /**
     * @param null $orgName
     * @param null $appName
     * @param $manifestPath
     * @param $version
     * @param $baseUrl
     * @param Oauth2Plugin $oauth2_plugin
     */
    function __construct(
        $orgName = null,
        $appName = null,
        $manifestPath,
        $version,
        $baseUrl,
        Oauth2Plugin $oauth2_plugin = null
    ) {
        //Set Version so its added to header
        $this->setVersion($version ?: $this->version);

        $this->baseUrl = $baseUrl;

        $this->org_name = $orgName;
        $this->app_name = $appName;

        //check if OAuth2 plugin is enabled
        if ($oauth2_plugin != null) {
            $this->oauth2_plugin = $oauth2_plugin;
            $this->grantType = $this->oauth2_plugin->getGrantType();
        }

        // Set the manifest path
        $this->setManifestPath($manifestPath ?: dirname(dirname(__FILE__)) . '/Manifests');
    }

    /**
     * @return mixed
     */
    public function getToken()
    {
        return $this->token;
    }

    /**
     * @param mixed $token
     * @return $this
     */
    public function setToken($token)
    {
        $this->token = $token;

        $this->setHeaders([
            'Authorization' => (string)'Bearer ' . $token,
        ]);

        return $this;
    }

    /**
     * Dynamically handle missing methods.
     *
     * @param  string $method
     * @param  array $arguments
     * @return mixed
     */
    public function __call($method, array $arguments = [])
    {
        if (substr($method, -8) === 'Iterator') {
            return $this->handleIteratorRequest($method, $arguments);
        } elseif ($this->isSingleRequest($method)) {
            return $this->handleSingleRequest($method, $arguments);
        }

        return $this->handleRequest($method);
    }

    /**
     * Handles an iterator request.
     *
     * @param  string $method
     * @param  array $arguments
     * @return \Apache\Usergrid\Api\ResourceIterator
     * @throws \InvalidArgumentException
     */
    protected function handleIteratorRequest($method, array $arguments = [])
    {
        $client = $this->handleRequest(substr($method, 0, -8));

        $command = $client->getCommand('all', array_get($arguments, 0, []));

        return new ResourceIterator($command, array_get($arguments, 1, []));
    }

    /**
     * Handles the current request.
     *
     * @param  string $method
     * @throws InvalidArgumentException
     * @return \Guzzle\Service\Client
     */
    protected function handleRequest($method)
    {
        if (!$this->manifestExists($method)) {
            throw new InvalidArgumentException("Undefined method [{$method}] called.");
        }

        // Initialize the Guzzle client
        $client = new GuzzleClient('',
            ['command.params' => ['app_name_or_uuid' => $this->app_name, 'org_name_or_uuid' => $this->org_name]]);

        // Set our own usergrid api client for internal
        // usage within our api models.
        $client->setApiClient($this);

        // Set the client user agent
        $client->setUserAgent($this->getUserAgent(), true);


        // Set the headers
        $client->setDefaultOption('headers', $this->getHeaders());

        // Get the Guzzle event dispatcher
        $dispatcher = $client->getEventDispatcher();

        // Register the error response plugin for our custom exceptions
        $dispatcher->addSubscriber(new ErrorResponsePlugin);

        // Listen to the "command.after_prepare" event fired by Guzzle
        $dispatcher->addListener('command.after_prepare', function (Event $event) {
            $request = $event['command']->getRequest();

            $request->getQuery()->setAggregator(new QueryAggregator());
        });

        //check if Oauth 2 plugin is a instance of Oauth2Plugin
        if ($this->oauth2_plugin instanceof Oauth2Plugin) {
            $dispatcher->addSubscriber($this->oauth2_plugin);
        }


        // Set the manifest payload into the Guzzle client
        $client->setDescription(ServiceDescription::factory(
            $this->buildPayload($method)
        ));

        // Return the Guzzle client
        return $client;
    }

    /**
     * Checks if the manifest file for the current request exists.
     *
     * @param  string $file
     * @return bool
     */
    protected function manifestExists($file)
    {
        return file_exists($this->getManifestFilePath($file));
    }

    /**
     * Returns the given request manifest file path.
     *
     * @param  string $file
     * @return string
     */
    protected function getManifestFilePath($file)
    {
        return $this->getFullManifestPath() . '/' . ucwords($file) . '.php';
    }

    /**
     * Returns the full versioned manifests path.
     *
     * @return string
     */
    protected function getFullManifestPath()
    {
        return $this->getManifestPath() . '/' . $this->getVersion();
    }

    /**
     * @return string
     */
    public function getManifestPath()
    {
        return $this->manifestPath;
    }

    /**
     * @param string $manifestPath
     * @return $this
     */
    public function setManifestPath($manifestPath)
    {
        $this->manifestPath = $manifestPath;
        return $this;
    }

    /**
     * @return string
     */
    public function getVersion()
    {
        return $this->version;
    }

    /**
     * @param string $version
     * @return $this
     */
    public function setVersion($version)
    {
        $this->version = $version;

        $this->setHeaders([
            'Usergrid-Version' => (string)$version,
        ]);
        return $this;
    }

    /**
     * @return string
     */
    public function getUserAgent()
    {
        return $this->userAgent;
    }

    /**
     * @param string $userAgent
     * @return $this
     */
    public function setUserAgent($userAgent)
    {
        $this->userAgent = $userAgent;

        return $this;
    }

    /**
     * Returns the Guzzle client headers.
     *
     * @return array
     */
    public function getHeaders()
    {
        return $this->headers;
    }

    /**
     * Sets the Guzzle client headers.
     *
     * @param  array $headers
     * @return $this
     */
    public function setHeaders(array $headers = [])
    {
        $this->headers = array_merge($this->headers, $headers);

        return $this;
    }

    /**
     * Returns the current request payload.
     *
     * @param  string $method
     * @return array
     */
    protected function buildPayload($method)
    {
        $operations = $this->getRequestManifestPayload($method);

        $manifest = $this->getRequestManifestPayload('manifest', false);

        return array_merge($manifest, compact('operations'));
    }

    /**
     * Returns the given file manifest data.
     *
     * @param  string $file
     * @param  bool $includeErrors
     * @return array
     */
    protected function getRequestManifestPayload($file, $includeErrors = true)
    {
        $file = ucwords($file);

        /** @noinspection PhpUnusedLocalVariableInspection */
        $baseURL = $this->baseUrl;

        if (!$manifest = array_get($this->manifests, $file)) {
            if ($includeErrors) {
                /** @noinspection PhpUnusedLocalVariableInspection */
                $errors = $this->getRequestManifestPayload('errors', false);
            }

            /** @noinspection PhpIncludeInspection */
            $manifest = require_once $this->getManifestFilePath($file);

            array_set($this->manifests, $file, $manifest);
        }

        return $manifest;
    }

    /**
     * Determines if the request is a single request.
     *
     * @param $method
     * @return bool
     */
    protected function isSingleRequest($method)
    {
        return (str_singular($method) === $method && $this->manifestExists(str_plural($method)));
    }

    /**
     * Handles a single request.
     *
     * @param  string $method
     * @param  array $arguments
     * @return \Guzzle\Service\Client
     * @throws \InvalidArgumentException
     */
    protected function handleSingleRequest($method, array $arguments = [])
    {
        // Check if we have any arguments
        if (empty($arguments)) {
            throw new InvalidArgumentException('Not enough arguments provided!');
        }

        // Pluralize the method name
        $pluralMethod = str_plural($method);

        // Get the request manifest payload data
        $manifest = $this->getRequestManifestPayload($pluralMethod);

        if (!$parameters = array_get($manifest, 'find')) {
            throw new InvalidArgumentException("Undefined method [{$method}] called.");;
        }

        // Get the required parameters for the request
        $required = array_where(array_get($parameters, 'parameters'), function ($key, $value) {
            return $value['required'] === true;
        });

        // Prepare the arguments for the request
        $arguments = array_combine(
            array_keys($required),
            count($required) === 1 ? (array)$arguments[0] : $arguments
        );

        // Execute the request
        return $this->handleRequest($pluralMethod)->find($arguments);
    }

} 