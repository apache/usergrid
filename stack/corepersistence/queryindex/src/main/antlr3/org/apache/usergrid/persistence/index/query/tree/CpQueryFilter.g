grammar CpQueryFilter;
//NOTES:  '^' denotes operator, all others in the string become operands

options {
    output=AST;
//    ASTLabelType=CommonTree;
}

@rulecatch { }


@header {
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.usergrid.persistence.index.query.tree;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.persistence.index.query.ParsedQuery;
import org.apache.usergrid.persistence.index.query.SortPredicate;

}


@members {

  private static final Logger logger = LoggerFactory
      .getLogger(CpQueryFilterLexer.class);

   //the object that represents a parsed query
   private ParsedQuery parsedQuery = new ParsedQuery();

	@Override
	public void emitErrorMessage(String msg) {
		logger.info(msg);
	}
}


@lexer::header {
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.usergrid.persistence.index.query.tree;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.persistence.index.exceptions.QueryTokenException;

}

@lexer::members {



  private static final Logger logger = LoggerFactory
      .getLogger(CpQueryFilterLexer.class);




	@Override
	public void emitErrorMessage(String msg) {
		logger.info(msg);
	}

	@Override
    public void recover(RecognitionException e) {
         //We don't want to recover, we want to re-throw to the user since they passed us invalid input
         throw new QueryTokenException(e);
    }


}

//these must come before ID. Otherwise lt, lte, eq, etc will be returned as id tokens
LT  : '<' | 'lt';

LTE : '<=' |  'lte';

EQ  : '=' | 'eq';

GT  : '>' | 'gt';

GTE : '>=' |  'gte';  


//keywords before var ids
BOOLEAN : (TRUE|FALSE);

AND : ('A'|'a')('N'|'n')('D'|'d') | '&&';

OR  : ('O'|'o')('R'|'r') | '||' ;

NOT : ('N'|'n')('O'|'o')('T'|'t');

ASC : ('A'|'a')('S'|'s')('C'|'c');

DESC : ('D'|'d')('E'|'e')('S'|'s')('C'|'c');

CONTAINS : ('C'|'c')('O'|'o')('N'|'n')('T'|'t')('A'|'a')('I'|'i')('N'|'n')('S'|'s');

WITHIN : ('W'|'w')('I'|'i')('T'|'t')('H'|'h')('I'|'i')('N'|'n');

OF : ('O'|'o')('F'|'f');

UUID :  HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT
  HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT '-' 
  HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT '-' 
  HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT '-' 
  HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT '-' 
  HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT
  HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT
  HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT
  ;

//ids and values
ID  :	('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_'|'.'|'-')*
    ;

LONG :	('-')? '0'..'9'+
    ;

FLOAT
    :  ('-')? ( ('0'..'9')+ '.' ('0'..'9')* EXPONENT?
    |   '.' ('0'..'9')+ EXPONENT?
    |   ('0'..'9')+ EXPONENT)
    ;
    
STRING
    :  '\'' ( ESC_SEQ | ~('\\'|'\'') )* '\''
    ;


    
WS : (' ' | '\t' | '\n' | '\r' | '\f')+  {$channel=HIDDEN;};



    



fragment TRUE : ('T'|'t')('R'|'r')('U'|'u')('E'|'e');

fragment FALSE : ('F'|'f')('A'|'a')('L'|'l')('S'|'s')('E'|'e');


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




//NE : '!=';



property :	ID<Property>;

containsproperty : ID<ContainsProperty>;

withinproperty : ID<WithinProperty>;
	
booleanliteral: BOOLEAN<BooleanLiteral>;


longliteral :
  LONG<LongLiteral> ;

uuidliteral :
  UUID<UUIDLiteral>;

stringliteral :
  STRING<StringLiteral>;
  
floatliteral :
  FLOAT<FloatLiteral> ;

//We delegate to each sub class literal so we can get each type	
value : 
  booleanliteral
  | longliteral
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
  withinproperty WITHIN<WithinOperand>^ (floatliteral|longliteral) OF! (floatliteral|longliteral) ','! (floatliteral|longliteral);
  
//string search
containsop :
  containsproperty CONTAINS<ContainsOperand>^ stringliteral;

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
 NOT<NotOperand>^ operation  
 |operation 
 ;

//and expressions contain operands.  These should always be closer to the leaves of a tree, it allows
//for faster result intersection sooner in the query execution
andexp :
 notexp (AND<AndOperand>^ notexp )*;
 
 
//or expression should always be after AND expressions.  This will give us a smaller result set to union when evaluating trees
//also a root level expression
expression :
 andexp (OR<OrOperand>^ andexp )*;



//end expressions

//begin order clauses

//direction for ordering
direction  : (ASC | DESC);

//order clause
order
  : (property direction?){
		String property = $property.text; 
		String direction = $direction.text;
		parsedQuery.addSort(new SortPredicate(property, direction));
    
  };

//end order clauses
  
//Begin select clauses

select_subject
  : ID {

  parsedQuery.addSelect($ID.text);

};

 

select_assign
  : target=ID ':' source=ID {

  parsedQuery.addSelect($target.text, $source.text);

};

select_expr 
  : ('*' | select_subject (',' select_subject) * | '{' select_assign (',' select_assign) * '}');  
   
//end select clauses

ql returns [ParsedQuery parsedQuery]
  : ('select'! select_expr!)? ('where'!? expression)? ('order by'! order! (','! order!)*)? {

  if($expression.tree instanceof Operand){
    parsedQuery.setRootOperand((Operand)$expression.tree);
  }
  
  retval.parsedQuery = parsedQuery;


};


