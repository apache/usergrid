grammar QueryFilter;

options {
    output=AST;
    ASTLabelType=CommonTree;
}

@header {
package org.usergrid.persistence.query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.Query.FilterPredicate;
import org.usergrid.persistence.Query.SortPredicate;

}

@lexer::header {
package org.usergrid.persistence.query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

}

@members {
	Query query = new Query();

  private static final Logger logger = LoggerFactory
      .getLogger(QueryFilterLexer.class);

	@Override
	public void emitErrorMessage(String msg) {
		logger.info(msg);
	}
}
  
@lexer::members {

  private static final Logger logger = LoggerFactory
      .getLogger(QueryFilterLexer.class);

	@Override
	public void emitErrorMessage(String msg) {
		logger.info(msg);
	}
}

ID  :	('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_'|'.')*
    ;

INT :	('-')? '0'..'9'+
    ;

FLOAT
    :  ('-')? ( ('0'..'9')+ '.' ('0'..'9')* EXPONENT?
    |   '.' ('0'..'9')+ EXPONENT?
    |   ('0'..'9')+ EXPONENT)
    ;
    
STRING
    :  '\'' ( ESC_SEQ | ~('\\'|'\'') )* '\''
    ;
    
fragment
BOOLEAN :	('true' |'false');
    
UUID :	HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT
	HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT '-' 
	HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT '-' 
	HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT '-' 
	HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT '-' 
	HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT
	HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT
	HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT
	;

fragment
EXPONENT : ('e'|'E') ('+'|'-')? ('0'..'9')+ ;

fragment
HEX_DIGIT : ('0'..'9'|'a'..'f'|'A'..'F') ;

fragment
ESC_SEQ
    :   '\\' ('b'|'t'|'n'|'f'|'r'|'\"'|'\''|'\\')
    |   UNICODE_ESC
    |   OCTAL_ESC
    ;

fragment
OCTAL_ESC
    :   '\\' ('0'..'3') ('0'..'7') ('0'..'7')
    |   '\\' ('0'..'7') ('0'..'7')
    |   '\\' ('0'..'7')
    ;

fragment
UNICODE_ESC
    :   '\\' 'u' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT
    ;

WS : (' ' | '\t' | '\n' | '\r' | '\f')+  {$channel=HIDDEN;};

LT 	:	'<' | 'lt';

LTE	:	'<=' |  'lte';

EQ  : '=' | 'eq';

GT 	:	'>' | 'gt';

GTE	:	'>=' |  'gte';	

NE : '!=';



property 
	:	 ID;
	
value   : BOOLEAN | STRING | INT | FLOAT | UUID;
	
//mathmatical equality operations
equalityop :	
  property ( LT 
	| LTE 
	| EQ
	| GT 
	| GTE
	| NE ) value;

//geo location search
locationop:

  property 'within' FLOAT 'of' FLOAT ',' FLOAT {
  //TODO construct within op object
  };
  
//string search
containsop : 
  property 'contains' STRING ;
  
//
operation :
 '('! expression ')'! | equalityop | locationop | containsop;

//negations of operations
notexp :
 'not'^ operation|operation;
    

//and expressions contain operands.  These should always be closer to the leaves of a tree, it allows
//for faster result intersection sooner in the query execution
andexp :
 notexp ('and' notexp)*;
 
//or expression should always be after AND expressions.  This will give us a smaller result set to union when evaluating trees
orexp :
 andexp ('or' andexp)*;

//root level boolean expression
expression:
  orexp;

//direction for ordering
direction  : ('asc' | 'desc');

//order cause
order
  : (property direction?);

ql returns [Query q]
  : ('where' expression )? ('order by' order (',' order)*)? {

q = query;

//TODO other stuff
};

//
//
//second_value 	:	(BOOLEAN | STRING | INT | FLOAT | UUID);
//
//third_value 	:	(BOOLEAN | STRING | INT | FLOAT | UUID);
//
//filter returns [FilterPredicate filter] 
//    :   property operator value ((',' | 'of') second_value ( ',' third_value)?)?  {
//    
//String property = $property.text;
//String operator = $operator.text;
//String value = $value.text;
//String second_value = $second_value.text;
//String third_value = $third_value.text;
//filter = new FilterPredicate(property, operator, value, second_value, third_value);
////System.out.println("Parsed query filter: " + property + " " + operator + " " + value + " " + second_value);
//    
//} EOF ;


//select_subject
//	:	ID {
//
//query.addSelect($select_subject.text);
//
//};
//
//select_assign_target 
//	:	ID;
//	
//select_assign_source 
//	:	ID;	 	 
//
//select_assign
//	:	select_assign_target ':' select_assign_source {
//
//query.addSelect($select_assign_source.text, $select_assign_target.text);
//
//};
	 

//operation
//	:	(property operator value ((',' | 'of') second_value ( ',' third_value)?)? {
//    
//String property = $property.text;
//String operator = $operator.text;
//String value = $value.text;
//int value_type = $value.start != null ? $value.start.getType() : 0;
//String second_value = $second_value.text;
//int second_value_type = $second_value.start != null ? $second_value.start.getType() : 0;
//String third_value = $third_value.text;
//int third_value_type = $third_value.start != null ? $third_value.start.getType() : 0;
//FilterPredicate filter = new FilterPredicate(property, operator, value, value_type, second_value, second_value_type, third_value, third_value_type);
//query.addFilter(filter);
////System.out.println("Parsed query filter: " + property + " " + operator + " " + value + " " + second_value);
//    
//} );


 
//where :
//  expression ;
//
//direction 	:	('asc' | 'desc');
//
//order
//	: (property direction?){
//    
//String property = $property.text; 
//String direction = $direction.text;
//SortPredicate sort = new SortPredicate(property, direction);
//query.addSort(sort);
//System.out.println("Parsed query order: " + property + " " + direction);
//    
//};
//
//select_expr 
//	:	('*' | select_subject (',' select_subject) * | '{' select_assign (',' select_assign) * '}');	
//	 
//ql returns [Query q]
//	:	'select' select_expr ('where' where )? ('order by' order (',' order)*)? {
//
//q = query;
//
//};


