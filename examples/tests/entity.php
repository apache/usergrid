<?php

//--------------------------------------------------------------
// Entity tests
//--------------------------------------------------------------
$testname = 'Create Entity';
//@han {create-entity}
$data = array('name' => 'Dino', 'type' => 'dog');
$dog = $client->create_entity($data);
if ($dog) {
	//once you have your entity, use the get() method to retrieve properties
	$name = $dog->get('name');
} else {
	//there was an error creating / retrieving the entity
}
//@solo
if ($dog->get('name') == 'Dino' && $dog->get('type') == 'dog'){
  $tester->success($testname);
} else {
	$tester->error($testname);
}

$testname = 'Get Entity';
//@han {get-entity}
$data = array('name' => 'Dino', 'type' => 'dog');
$dog = $client->get_entity($data);
if ($dog) {
	$name = $dog->get('name');
} else {
	//entity doesn't exist on the server
}
//@solo
if ($name == 'Dino' && $dog->get('type') == 'dog'){
  $tester->success($testname);
} else {
	$tester->error($testname);
}

//@han {refresh-entity}
$dog->fetch();
//@solo


$testname = 'Set key on entity';
//@han {set-entity}
$dog->set('master', 'Fred');
$dog->set('breed', 'dinosaur');
$dog->set('color', 'purple');
//@solo
if ($dog->get('color') == 'purple') {
	$tester->success($testname);
} else {
	$tester->error($testname);
}

$testname = 'Save key on entity';
//@han {save-entity}
$result = $dog->save();
if (!$result->get_error()) {
	//all is well
	$tester->success($testname);
} else {
	//there was a problem!
	$tester->error($testname);
}
//@solo

$testname = 'Refresh entity and check key';
$dog->set('color', 'red'); //set the value
$result = $dog->fetch(); //refresh to overwrite
if ($dog->get('color') == 'purple') {
	$tester->success($testname);
} else {
	$tester->error($testname);
}

$testname = 'Delete entity';
//@han {destroy-entity}
$result = $dog->destroy();
//@solo
if (isset($result->data['action']) && $result->data['action'] == 'delete') {
	$tester->success($testname);
} else {
	$tester->error($testname);
}

?>
