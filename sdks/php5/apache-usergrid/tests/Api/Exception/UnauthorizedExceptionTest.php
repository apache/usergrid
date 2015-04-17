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

namespace Apache\Usergrid\Tests\Api\Exception;


use Apache\Usergrid\Api\Exception\UnauthorizedException;
use Guzzle\Http\Message\Response;
use PHPUnit_Framework_TestCase;

/**
 * Class UnauthorizedExceptionTest
 *
 * @package    Apache/Usergrid
 * @version    1.0.0
 * @author     Jason Kristian <jasonkristian@gmail.com>
 * @license    Apache License, Version  2.0
 * @copyright  (c) 2008-2014, Baas Platform Pty. Ltd
 * @link       http://baas-platform.com
 */
class UnauthorizedExceptionTest extends PHPUnit_Framework_TestCase
{
    /** @test */
    public function it_can_create_the_exception()
    {
        $command = $this->getMock('Guzzle\Service\Command\CommandInterface');
        $command
            ->expects($this->once())
            ->method('getRequest')
            ->will($this->returnValue(
                $this->getMock('Guzzle\Http\Message\Request', [], [], '', false)
            ));

        $response = new Response(401);
        $response->setBody('');

        /** @noinspection PhpParamsInspection */
        $exception = UnauthorizedException::fromCommand($command, $response);

        $this->assertInstanceOf(
            'Apache\Usergrid\Api\Exception\UnauthorizedException',
            $exception
        );
    }
} 