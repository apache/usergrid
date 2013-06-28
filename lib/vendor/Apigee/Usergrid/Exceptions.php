<?php
/**
* @file
* Request - a data structure to hold all request-related parameters
*
* @author Daniel Johnson <djohnson@apigee.com>
* @author Rod Simpson <rod@apigee.com>
* @since 26-Apr-2013
*/


namespace Apigee\Usergrid;


class UGException extends \Exception { }
class UG_400_BadRequest extends UGException {}
class UG_401_Unauthorized extends UGException {}
class UG_403_Forbidden extends UGException {}
class UG_404_NotFound extends UGException {}
class UG_500_ServerError extends UGException {}

?>
