grammar QueryFilter;
//NOTES:  '^' denotes operator, all others in the string become operands


options {
    output=AST;
//    ASTLabelType=CommonTree;
}

@header {
package org.usergrid.persistence.query.tree;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.Query.FilterPredicate;
import org.usergrid.persistence.Query.SortPredicate;

}

@lexer::header {
package org.usergrid.persistence.query.tree;

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

//NE : '!=';



property returns [Property property]
	:	 ID<Property>;
	
booleanliteral returns [BooleanLiteral value] :
 BOOLEAN<BooleanLiteral>;


intliteral returns [IntegerLiteral value] :
  INT<IntegerLiteral>;

uuidliteral returns [UUIDLiteral value] :
  UUID<UUIDLiteral>;

stringliteral returns [StringLiteral value] :
  STRING<StringLiteral>;
  
floatliteral returns [FloatLiteral value] :
  FLOAT<FloatLiteral>;

//We delegate to each sub class literal so we can get each type	
value returns [Literal value]  : 
  booleanliteral {retval.value = $booleanliteral.value;}
  | intliteral {retval.value = $intliteral.value;}
  | uuidliteral {retval.value = $uuidliteral.value;}
  | stringliteral {retval.value = $stringliteral.value;}
  | floatliteral {retval.value = $floatliteral.value;}
  ;
  


//Every operand returns with the name of 'op'.  This is used because all subtrees require operands,
//this allows us to link the java code easily by using the same name as a converntion

//begin search expressions
  
//mathmatical equality operations
equalityop returns [EqualityOperand op]:
  p=property LT v=value { retval.op = new LessThan(p.property, v.value);}
  |p=property LTE v=value { retval.op = new LessThanEqual(p.property, v.value);}
  | p=property EQ v=value { retval.op = new Equal(p.property, v.value);}
  |p=property GT v=value { retval.op = new GreaterThan(p.property, v.value);}
  |p=property GTE v=value { retval.op = new LessThan(p.property, v.value);}
  ; 

//geo location search
locationop returns [Within op]:
  p=property 'within' distance=floatliteral 'of' lattitude=floatliteral ',' longitude=floatliteral {
    retval.op = new Within(p.property, distance.value, lattitude.value, longitude.value);
  };//-> ^('within'<Within>[$property, $distance, $lat, $long]);
  
//string search
containsop returns [Contains op] : 
  p=property 'contains' s=stringliteral {
    retval.op = new Contains(p.property, s.value);
  };
  
//
operation returns [Operand op] :
 '(' exp=expression ')'  { retval.op = exp.op; }
   | equalityop { retval.op = $equalityop.op; }
   | locationop { retval.op = $locationop.op; }
   | containsop { retval.op = $containsop.op; }
   ;

//negations of expressions
notexp returns [Operand op]:
//only link if we have the not
 'not'^ operation { retval.op = new NotOperand($operation.op);} 
 |operation { retval.op = $operation.op; }
 ;

//and expressions contain operands.  These should always be closer to the leaves of a tree, it allows
//for faster result intersection sooner in the query execution
andexp returns [AndOperand op]:
 left=notexp ('and' right=notexp {retval.op = new AndOperand(left.op, right.op); } )*;
 
//or expression should always be after AND expressions.  This will give us a smaller result set to union when evaluating trees
orexp returns [OrOperand op]:
 left=andexp ('or' right=andexp {retval.op = new OrOperand(left.op, right.op); })*;

//root level boolean expression
expression returns [Operand op]:
  orexp;

//end expressions

//begin order clauses

//direction for ordering
direction  : ('asc' | 'desc');

//order clause
order
  : (property direction?);

//end order clauses
  
//Begin select clauses

select_subject
  : ID {

  query.addSelect($select_subject.text);

};

 

select_assign
  : target=ID ':' source=ID {

  query.addSelect($target.text, $source.text);

};

select_expr 
  : ('*' | select_subject (',' select_subject) * | '{' select_assign (',' select_assign) * '}');  
   
//end select clauses

ql returns [Query q]
  : 'select' select_expr ('where' expression )? ('order by' order (',' order)*)? {

//q = query;

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


