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

//include autoloader to make sure all files are included
include '../autoloader.inc.php';
usergrid_autoload('Apache\\Usergrid\\Client');

//initialize the SDK
$client = new Apache\Usergrid\Client('yourorgname','sandbox');

//reading data
$books = $client->get_collection('books');
//do something with the data
while ($books->has_next_entity()) {
	$book = $books->get_next_entity();
	$title = $book->get('title');
	echo "Next Book's title is: " . $title . "<br>";
}

//writing data
$data = array('title' => 'the old man and the sea', 'type' => 'books');
$book = $books->add_entity($data);
if ($book == FALSE) {
	echo 'write failed';
} else {
	echo 'write succeeded';
}
?>
