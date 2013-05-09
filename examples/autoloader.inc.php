<?php

function usergrid_autoload($class) {


  if (strpos($class, '\\') !== FALSE) {
    $path_parts = explode('\\', $class);
    if ($path_parts[0] == 'Apigee') {
      $lib_path = realpath(dirname(__FILE__) . '/../lib/vendor');
      $class_path = $lib_path . '/' . join('/', $path_parts) . '.php';
      if (file_exists($class_path)) {
        require_once($class_path);
      }
    }
  }
}

spl_autoload_register('usergrid_autoload');
