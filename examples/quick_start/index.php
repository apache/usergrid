<?php
//include autoloader to make sure all files are included
include '../autoloader.inc.php';
usergrid_autoload('Apigee\\Usergrid\\Client');

//initialize the SDK
$client = new Apigee\Usergrid\Client('yourorgname','sandbox');

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
