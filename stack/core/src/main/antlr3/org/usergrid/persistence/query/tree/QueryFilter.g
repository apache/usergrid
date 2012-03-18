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



property :	ID<Property>;
	
booleanliteral: BOOLEAN<BooleanLiteral>;


intliteral :
  INT<IntegerLiteral> ;

uuidliteral :
  UUID<UUIDLiteral>;

stringliteral :
  STRING<StringLiteral>;
  
floatliteral :
  FLOAT<FloatLiteral> ;

//We delegate to each sub class literal so we can get each type	
value : 
  booleanliteral
  | intliteral
  | uuidliteral
  | stringliteral
  | floatliteral
  ;
  


//Every operand returns with the name of 'op'.  This is used because all subtrees require operands,
//this allows us to link the java code easily by using the same name as a converntion

//begin search expressions
  
//mathmatical equality operations
equalityop :
  property LT<LessThan>^ value
  |property LTE<LessThanEqual>^ value
  |property EQ<Equal>^ value
  |property GT<GreaterThan>^ value
  |property GTE<GreaterThanEqual>^ value
  ; 

//geo location search
locationop :
  property 'within'<WithinOperand>^ floatliteral 'of'! floatliteral ','! floatliteral;
  
//string search
containsop :
  property 'contains'<ContainsOperand>^ stringliteral;
//
operation :
 '('! expression ')'!
   | equalityop 
   | locationop 
   | containsop 
   ;

//negations of expressions
notexp :
//only link if we have the not
 'not'<NotOperand>^ operation  
 |operation 
 ;

//and expressions contain operands.  These should always be closer to the leaves of a tree, it allows
//for faster result intersection sooner in the query execution
andexp :
 notexp ('and'<AndOperand>^ notexp )*;
 
 
//or expression should always be after AND expressions.  This will give us a smaller result set to union when evaluating trees
//also a root level expression
expression :
 andexp ('or'<OrOperand>^ andexp )*;



//end expressions

//begin order clauses

//direction for ordering
direction  : ('asc' | 'desc');

//order clause
order
  : (property direction?){
		String property = $property.text; 
		String direction = $direction.text;
		query.addSort(new SortPredicate(property, direction));
    
  };

//end order clauses
  
//Begin select clauses

select_subject
  : ID {

  query.addSelect($ID.text);

};

 

select_assign
  : target=ID ':' source=ID {

  query.addSelect($target.text, $source.text);

};

select_expr 
  : ('*' | select_subject (',' select_subject) * | '{' select_assign (',' select_assign) * '}');  
   
//end select clauses

ql returns [Query query]
  : 'select'! select_expr! ('where'! expression )? ('order by'! order! (','! order!)*)? {

  
  query.setRootOperand((Operand)$expression.tree);
  
  retval.query = query;


};



