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

function usergrid_autoload($class) {


  if (strpos($class, '\\') !== FALSE) {
    $path_parts = explode('\\', $class);
    if ($path_parts[0] == 'Apache') {
      $lib_path = realpath(dirname(__FILE__) . '/../lib/vendor');
      $class_path = $lib_path . '/' . join('/', $path_parts) . '.php';
      if (file_exists($class_path)) {
        require_once($class_path);
      }
    }
  }
}

spl_autoload_register('usergrid_autoload');
