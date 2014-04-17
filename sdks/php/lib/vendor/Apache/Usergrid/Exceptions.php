#!/usr/bin/env php
<?php
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
* @file
* Request - a data structure to hold all request-related parameters
*
* @author Daniel Johnson <djohnson@apigee.com>
* @author Rod Simpson <rod@apigee.com>
* @since 26-Apr-2013
*/


namespace Apache\Usergrid;


class UGException extends \Exception { }
class UG_400_BadRequest extends UGException {}
class UG_401_Unauthorized extends UGException {}
class UG_403_Forbidden extends UGException {}
class UG_404_NotFound extends UGException {}
class UG_500_ServerError extends UGException {}

?>
