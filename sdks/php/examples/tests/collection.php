<?php
/**
 * @file
 * Collection tests
 *
 * @author Rod Simpson <rod@apigee.com>
 * @since 09-Mar-2013
 */

//--------------------------------------------------------------
// Collection tests
//--------------------------------------------------------------
$testname = 'Create test data - ';
for($i=0;$i<30;$i++) {
	$testname = 'Create dog_'.$i;
	$data = array('name' => 'dog_'.$i, 'type' => 'dogs');
	$entity = $client->create_entity($data);
	if ($entity->get('name') == 'dog_'.$i && $entity->get('type') == 'dog'){
	  $tester->success($testname);
	} else {
		$tester->error($testname);
	}
}

$testname = 'Get dogs collection';
//@han {get-collection}
$dogs = $client->get_collection('dogs');
//@solo
//echo '<pre>'.$dogs->get_json().'</pre><br>';
if ($dogs->get_type() == 'dogs') {
  $tester->success($testname);
} else {
	$tester->error($testname);
}

//@han {iterate-collection}
while ($dogs->has_next_entity()) {
	$dog = $dogs->get_next_entity();
	//do something with dog
	$name = $dog->get('name');
}
//@solo

//@han {re-iterate-collection}
$dogs->reset_entity_pointer();
while ($dogs->has_next_entity()) {
	$dog = $dogs->get_next_entity();
	//do something with dog
	$name = $dog->get('name');
}
//@solo

//@han {get-next-page-collection}
$dogs->get_next_page();
while ($dogs->has_next_entity()) {
	$dog = $dogs->get_next_entity();
	$name = $dog->get('name');
}
//@solo

//@han {get-prev-page-collection}
$dogs->get_prev_page();
while ($dogs->has_next_entity()) {
	$dog = $dogs->get_next_entity();
	$name = $dog->get('name');
}
//@solo


$testname = 'Get next page of dogs collection';
$testname2 = 'Verify next page of dogs collection - ';
//@han {iterate-over-entire-collection}
while ($dogs->get_next_page()) {
	$tester->success($testname);
	while ($dogs->has_next_entity()) {
		$dog = $dogs->get_next_entity();
		$name = $dog->get('name');
		//@solo
		if ($entity->get('type') == 'dog'){
			$tester->success($testname2.$name);
		} else {
			$tester->error($testname2.$name);
		}
		//@han {iterate-over-entire-collection2}
	}
}
//@solo

//@han {get-previous-page-collection}
while ($dogs->get_prev_page()) {
	$testname = 'Get next page of dogs collection';
	$tester->success($testname);
	$testname = 'Verify next page of dogs collection - ';
	while ($dogs->has_next_entity()) {
		$dog = $dogs->get_next_entity();
		$name = $dog->get('name');
		if ($entity->get('type') == 'dog'){
			$tester->success($testname.$name);
		} else {
			$tester->error($testname.$name);
		}
	}
}

$testname = 'Add new dog to collection';
//@han {add-entity-to-collection}
$data = array('name' => 'Dino', 'type' => 'dogs');
$dino = $dogs->add_entity($data);
//@solo
if ($dino->get('name') == 'Dino') {
	$tester->success($testname);
} else {
	$tester->error($testname);
}

//@han {set-limit-collection}
$data = array('ql'=>'select * where created > 0', 'limit'=>'40');
$dogs = $client->get_collection('dogs', $data);
//@solo

$testname = 'Clear dogs sample data - ';
$dogs->reset_entity_pointer();
while ($dogs->has_next_entity()) {
	$dog = $dogs->get_next_entity();
	$name = $dog->get('name');
	$result = $dog->destroy();
	$response_data = $result->get_data();
	if (isset($response_data['action']) && $response_data['action'] == 'delete') {
		$tester->success($testname.$name);
	} else {
		$tester->error($testname.$name);
	}
}

?>
