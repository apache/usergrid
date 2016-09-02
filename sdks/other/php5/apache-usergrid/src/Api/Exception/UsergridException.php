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
namespace Apache\Usergrid\Api\Exception;


use Exception;
use Guzzle\Http\Message\Request;
use Guzzle\Http\Message\Response;
use Guzzle\Plugin\ErrorResponse\ErrorResponseExceptionInterface;
use Guzzle\Service\Command\CommandInterface;

/**
 * Class UsergridException
 *
 * @package    Apache/Usergrid
 * @version    1.0.0
 * @author     Jason Kristian <jasonkristian@gmail.com>
 * @license    Apache License, Version  2.0
 * @copyright  (c) 2008-2014, Baas Platform Pty. Ltd
 * @link       http://baas-platform.com
 */
class UsergridException extends Exception implements ErrorResponseExceptionInterface
{
    /**
     * The Guzzle request.
     *
     * @var \Guzzle\Http\Message\Request
     */
    protected $request;

    /**
     * The Guzzle response.
     *
     * @var \Guzzle\Http\Message\Response
     */
    protected $response;

    /**
     * The error type returned by Usergrid.
     *
     * @var string
     */
    protected $errorType;

    /**
     * {@inheritDoc}
     */
    public static function fromCommand(CommandInterface $command, Response $response)
    {
        $errors = json_decode($response->getBody(true), true);

        $type = array_get($errors, 'error.type', null);

        $code = array_get($errors, 'error.code', null);

        $message = array_get($errors, 'error.message', null);

        $class = '\\Apache\\Usergrid\\Api\\Exception\\' . studly_case($type) . 'Exception';

        if (class_exists($class)) {
            $exception = new $class($message, $response->getStatusCode());
        } else {
            $exception = new static($message, $response->getStatusCode());
        }

        $exception->setErrorType($type);

        $exception->setResponse($response);

        $exception->setRequest($command->getRequest());

        return $exception;
    }

    /**
     * Returns the Guzzle request.
     *
     * @return \Guzzle\Http\Message\Request
     */
    public function getRequest()
    {
        return $this->request;
    }

    /**
     * Sets the Guzzle request.
     *
     * @param  \Guzzle\Http\Message\Request $request
     * @return void
     */
    public function setRequest(Request $request)
    {
        $this->request = $request;
    }

    /**
     * Returns the Guzzle response.
     *
     * @return \Guzzle\Http\Message\Response
     */
    public function getResponse()
    {
        return $this->response;
    }

    /**
     * Sets the Guzzle response.
     *
     * @param  \Guzzle\Http\Message\Response $response
     * @return void
     */
    public function setResponse(Response $response)
    {
        $this->response = $response;
    }

    /**
     * Returns the error type returned by Usergrid.
     *
     * @return string
     */
    public function getErrorType()
    {
        return $this->errorType;
    }

    /**
     * Sets the error type returned by Usergrid.
     *
     * @param  string $errorType
     * @return void
     */
    public function setErrorType($errorType)
    {
        $this->errorType = $errorType;
    }


} 