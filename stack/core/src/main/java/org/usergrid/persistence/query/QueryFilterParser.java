// $ANTLR 3.1.3 Mar 17, 2009 19:23:44 org/usergrid/persistence/query/QueryFilter.g 2012-02-28 15:25:28

package org.usergrid.persistence.query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.Query.FilterPredicate;
import org.usergrid.persistence.Query.SortPredicate;



import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

public class QueryFilterParser extends Parser {
    public static final String[] tokenNames = new String[] {
        "<invalid>", "<EOR>", "<DOWN>", "<UP>", "ID", "INT", "EXPONENT", "FLOAT", "ESC_SEQ", "STRING", "BOOLEAN", "HEX_DIGIT", "UUID", "UNICODE_ESC", "OCTAL_ESC", "WS", "'<'", "'<='", "'='", "'>'", "'>='", "'in'", "'eq'", "'lt'", "'gt'", "'lte'", "'gte'", "'contains'", "','", "':'", "'asc'", "'desc'", "'*'", "'{'", "'}'", "'select'", "'where'", "'and'", "'order by'"
    };
    public static final int EXPONENT=6;
    public static final int T__29=29;
    public static final int T__28=28;
    public static final int T__27=27;
    public static final int T__26=26;
    public static final int UUID=12;
    public static final int T__25=25;
    public static final int T__24=24;
    public static final int T__23=23;
    public static final int T__22=22;
    public static final int T__21=21;
    public static final int UNICODE_ESC=13;
    public static final int T__20=20;
    public static final int OCTAL_ESC=14;
    public static final int HEX_DIGIT=11;
    public static final int FLOAT=7;
    public static final int INT=5;
    public static final int ID=4;
    public static final int EOF=-1;
    public static final int T__19=19;
    public static final int T__30=30;
    public static final int T__31=31;
    public static final int T__32=32;
    public static final int WS=15;
    public static final int BOOLEAN=10;
    public static final int ESC_SEQ=8;
    public static final int T__33=33;
    public static final int T__16=16;
    public static final int T__34=34;
    public static final int T__35=35;
    public static final int T__18=18;
    public static final int T__36=36;
    public static final int T__17=17;
    public static final int T__37=37;
    public static final int T__38=38;
    public static final int STRING=9;

    // delegates
    // delegators


        public QueryFilterParser(TokenStream input) {
            this(input, new RecognizerSharedState());
        }
        public QueryFilterParser(TokenStream input, RecognizerSharedState state) {
            super(input, state);
             
        }
        

    public String[] getTokenNames() { return QueryFilterParser.tokenNames; }
    public String getGrammarFileName() { return "org/usergrid/persistence/query/QueryFilter.g"; }


    	Query query = new Query();

      private static final Logger logger = LoggerFactory
          .getLogger(QueryFilterLexer.class);

    	@Override
    	public void emitErrorMessage(String msg) {
    		logger.info(msg);
    	}


    public static class property_return extends ParserRuleReturnScope {
    };

    // $ANTLR start "property"
    // org/usergrid/persistence/query/QueryFilter.g:101:1: property : ( ID ) ;
    public final QueryFilterParser.property_return property() throws RecognitionException {
        QueryFilterParser.property_return retval = new QueryFilterParser.property_return();
        retval.start = input.LT(1);

        try {
            // org/usergrid/persistence/query/QueryFilter.g:102:2: ( ( ID ) )
            // org/usergrid/persistence/query/QueryFilter.g:102:5: ( ID )
            {
            // org/usergrid/persistence/query/QueryFilter.g:102:5: ( ID )
            // org/usergrid/persistence/query/QueryFilter.g:102:6: ID
            {
            match(input,ID,FOLLOW_ID_in_property585); 

            }


            }

            retval.stop = input.LT(-1);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "property"

    public static class operator_return extends ParserRuleReturnScope {
    };

    // $ANTLR start "operator"
    // org/usergrid/persistence/query/QueryFilter.g:104:1: operator : ( '<' | '<=' | '=' | '>' | '>=' | 'in' | 'eq' | 'lt' | 'gt' | 'lte' | 'gte' | 'contains' ) ;
    public final QueryFilterParser.operator_return operator() throws RecognitionException {
        QueryFilterParser.operator_return retval = new QueryFilterParser.operator_return();
        retval.start = input.LT(1);

        try {
            // org/usergrid/persistence/query/QueryFilter.g:105:2: ( ( '<' | '<=' | '=' | '>' | '>=' | 'in' | 'eq' | 'lt' | 'gt' | 'lte' | 'gte' | 'contains' ) )
            // org/usergrid/persistence/query/QueryFilter.g:105:4: ( '<' | '<=' | '=' | '>' | '>=' | 'in' | 'eq' | 'lt' | 'gt' | 'lte' | 'gte' | 'contains' )
            {
            if ( (input.LA(1)>=16 && input.LA(1)<=27) ) {
                input.consume();
                state.errorRecovery=false;
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                throw mse;
            }


            }

            retval.stop = input.LT(-1);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "operator"

    public static class value_return extends ParserRuleReturnScope {
    };

    // $ANTLR start "value"
    // org/usergrid/persistence/query/QueryFilter.g:107:1: value : ( BOOLEAN | STRING | INT | FLOAT | UUID ) ;
    public final QueryFilterParser.value_return value() throws RecognitionException {
        QueryFilterParser.value_return retval = new QueryFilterParser.value_return();
        retval.start = input.LT(1);

        try {
            // org/usergrid/persistence/query/QueryFilter.g:107:8: ( ( BOOLEAN | STRING | INT | FLOAT | UUID ) )
            // org/usergrid/persistence/query/QueryFilter.g:107:10: ( BOOLEAN | STRING | INT | FLOAT | UUID )
            {
            if ( input.LA(1)==INT||input.LA(1)==FLOAT||(input.LA(1)>=STRING && input.LA(1)<=BOOLEAN)||input.LA(1)==UUID ) {
                input.consume();
                state.errorRecovery=false;
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                throw mse;
            }


            }

            retval.stop = input.LT(-1);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "value"

    public static class second_value_return extends ParserRuleReturnScope {
    };

    // $ANTLR start "second_value"
    // org/usergrid/persistence/query/QueryFilter.g:109:1: second_value : ( BOOLEAN | STRING | INT | FLOAT | UUID ) ;
    public final QueryFilterParser.second_value_return second_value() throws RecognitionException {
        QueryFilterParser.second_value_return retval = new QueryFilterParser.second_value_return();
        retval.start = input.LT(1);

        try {
            // org/usergrid/persistence/query/QueryFilter.g:109:15: ( ( BOOLEAN | STRING | INT | FLOAT | UUID ) )
            // org/usergrid/persistence/query/QueryFilter.g:109:17: ( BOOLEAN | STRING | INT | FLOAT | UUID )
            {
            if ( input.LA(1)==INT||input.LA(1)==FLOAT||(input.LA(1)>=STRING && input.LA(1)<=BOOLEAN)||input.LA(1)==UUID ) {
                input.consume();
                state.errorRecovery=false;
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                throw mse;
            }


            }

            retval.stop = input.LT(-1);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "second_value"


    // $ANTLR start "filter"
    // org/usergrid/persistence/query/QueryFilter.g:111:1: filter returns [FilterPredicate filter] : property operator value ( ',' second_value )? EOF ;
    public final FilterPredicate filter() throws RecognitionException {
        FilterPredicate filter = null;

        QueryFilterParser.property_return property1 = null;

        QueryFilterParser.operator_return operator2 = null;

        QueryFilterParser.value_return value3 = null;

        QueryFilterParser.second_value_return second_value4 = null;


        try {
            // org/usergrid/persistence/query/QueryFilter.g:112:5: ( property operator value ( ',' second_value )? EOF )
            // org/usergrid/persistence/query/QueryFilter.g:112:9: property operator value ( ',' second_value )? EOF
            {
            pushFollow(FOLLOW_property_in_filter716);
            property1=property();

            state._fsp--;

            pushFollow(FOLLOW_operator_in_filter718);
            operator2=operator();

            state._fsp--;

            pushFollow(FOLLOW_value_in_filter720);
            value3=value();

            state._fsp--;

            // org/usergrid/persistence/query/QueryFilter.g:112:33: ( ',' second_value )?
            int alt1=2;
            int LA1_0 = input.LA(1);

            if ( (LA1_0==28) ) {
                alt1=1;
            }
            switch (alt1) {
                case 1 :
                    // org/usergrid/persistence/query/QueryFilter.g:112:34: ',' second_value
                    {
                    match(input,28,FOLLOW_28_in_filter723); 
                    pushFollow(FOLLOW_second_value_in_filter725);
                    second_value4=second_value();

                    state._fsp--;


                    }
                    break;

            }


                
            String property = (property1!=null?input.toString(property1.start,property1.stop):null);
            String operator = (operator2!=null?input.toString(operator2.start,operator2.stop):null);
            String value = (value3!=null?input.toString(value3.start,value3.stop):null);
            String second_value = (second_value4!=null?input.toString(second_value4.start,second_value4.stop):null);
            filter = new FilterPredicate(property, operator, value, second_value);
            //System.out.println("Parsed query filter: " + property + " " + operator + " " + value + " " + second_value);
                

            match(input,EOF,FOLLOW_EOF_in_filter731); 

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return filter;
    }
    // $ANTLR end "filter"

    public static class select_subject_return extends ParserRuleReturnScope {
    };

    // $ANTLR start "select_subject"
    // org/usergrid/persistence/query/QueryFilter.g:123:1: select_subject : ID ;
    public final QueryFilterParser.select_subject_return select_subject() throws RecognitionException {
        QueryFilterParser.select_subject_return retval = new QueryFilterParser.select_subject_return();
        retval.start = input.LT(1);

        try {
            // org/usergrid/persistence/query/QueryFilter.g:124:2: ( ID )
            // org/usergrid/persistence/query/QueryFilter.g:124:4: ID
            {
            match(input,ID,FOLLOW_ID_in_select_subject741); 


            query.addSelect(input.toString(retval.start,input.LT(-1)));



            }

            retval.stop = input.LT(-1);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "select_subject"

    public static class select_assign_target_return extends ParserRuleReturnScope {
    };

    // $ANTLR start "select_assign_target"
    // org/usergrid/persistence/query/QueryFilter.g:130:1: select_assign_target : ID ;
    public final QueryFilterParser.select_assign_target_return select_assign_target() throws RecognitionException {
        QueryFilterParser.select_assign_target_return retval = new QueryFilterParser.select_assign_target_return();
        retval.start = input.LT(1);

        try {
            // org/usergrid/persistence/query/QueryFilter.g:131:2: ( ID )
            // org/usergrid/persistence/query/QueryFilter.g:131:4: ID
            {
            match(input,ID,FOLLOW_ID_in_select_assign_target753); 

            }

            retval.stop = input.LT(-1);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "select_assign_target"

    public static class select_assign_source_return extends ParserRuleReturnScope {
    };

    // $ANTLR start "select_assign_source"
    // org/usergrid/persistence/query/QueryFilter.g:133:1: select_assign_source : ID ;
    public final QueryFilterParser.select_assign_source_return select_assign_source() throws RecognitionException {
        QueryFilterParser.select_assign_source_return retval = new QueryFilterParser.select_assign_source_return();
        retval.start = input.LT(1);

        try {
            // org/usergrid/persistence/query/QueryFilter.g:134:2: ( ID )
            // org/usergrid/persistence/query/QueryFilter.g:134:4: ID
            {
            match(input,ID,FOLLOW_ID_in_select_assign_source764); 

            }

            retval.stop = input.LT(-1);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "select_assign_source"


    // $ANTLR start "select_assign"
    // org/usergrid/persistence/query/QueryFilter.g:136:1: select_assign : select_assign_target ':' select_assign_source ;
    public final void select_assign() throws RecognitionException {
        QueryFilterParser.select_assign_source_return select_assign_source5 = null;

        QueryFilterParser.select_assign_target_return select_assign_target6 = null;


        try {
            // org/usergrid/persistence/query/QueryFilter.g:137:2: ( select_assign_target ':' select_assign_source )
            // org/usergrid/persistence/query/QueryFilter.g:137:4: select_assign_target ':' select_assign_source
            {
            pushFollow(FOLLOW_select_assign_target_in_select_assign777);
            select_assign_target6=select_assign_target();

            state._fsp--;

            match(input,29,FOLLOW_29_in_select_assign779); 
            pushFollow(FOLLOW_select_assign_source_in_select_assign781);
            select_assign_source5=select_assign_source();

            state._fsp--;



            query.addSelect((select_assign_source5!=null?input.toString(select_assign_source5.start,select_assign_source5.stop):null), (select_assign_target6!=null?input.toString(select_assign_target6.start,select_assign_target6.stop):null));



            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end "select_assign"


    // $ANTLR start "where"
    // org/usergrid/persistence/query/QueryFilter.g:143:1: where : ( property operator value ( ',' second_value )? ) ;
    public final void where() throws RecognitionException {
        QueryFilterParser.property_return property7 = null;

        QueryFilterParser.operator_return operator8 = null;

        QueryFilterParser.value_return value9 = null;

        QueryFilterParser.second_value_return second_value10 = null;


        try {
            // org/usergrid/persistence/query/QueryFilter.g:144:2: ( ( property operator value ( ',' second_value )? ) )
            // org/usergrid/persistence/query/QueryFilter.g:144:4: ( property operator value ( ',' second_value )? )
            {
            // org/usergrid/persistence/query/QueryFilter.g:144:4: ( property operator value ( ',' second_value )? )
            // org/usergrid/persistence/query/QueryFilter.g:144:5: property operator value ( ',' second_value )?
            {
            pushFollow(FOLLOW_property_in_where795);
            property7=property();

            state._fsp--;

            pushFollow(FOLLOW_operator_in_where797);
            operator8=operator();

            state._fsp--;

            pushFollow(FOLLOW_value_in_where799);
            value9=value();

            state._fsp--;

            // org/usergrid/persistence/query/QueryFilter.g:144:29: ( ',' second_value )?
            int alt2=2;
            int LA2_0 = input.LA(1);

            if ( (LA2_0==28) ) {
                alt2=1;
            }
            switch (alt2) {
                case 1 :
                    // org/usergrid/persistence/query/QueryFilter.g:144:30: ',' second_value
                    {
                    match(input,28,FOLLOW_28_in_where802); 
                    pushFollow(FOLLOW_second_value_in_where804);
                    second_value10=second_value();

                    state._fsp--;


                    }
                    break;

            }


                
            String property = (property7!=null?input.toString(property7.start,property7.stop):null);
            String operator = (operator8!=null?input.toString(operator8.start,operator8.stop):null);
            String value = (value9!=null?input.toString(value9.start,value9.stop):null);
            int value_type = (value9!=null?((Token)value9.start):null) != null ? (value9!=null?((Token)value9.start):null).getType() : 0;
            String second_value = (second_value10!=null?input.toString(second_value10.start,second_value10.stop):null);
            int second_value_type = (second_value10!=null?((Token)second_value10.start):null) != null ? (second_value10!=null?((Token)second_value10.start):null).getType() : 0;
            FilterPredicate filter = new FilterPredicate(property, operator, value, value_type, second_value, second_value_type);
            query.addFilter(filter);
            //System.out.println("Parsed query filter: " + property + " " + operator + " " + value + " " + second_value);
                


            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end "where"

    public static class direction_return extends ParserRuleReturnScope {
    };

    // $ANTLR start "direction"
    // org/usergrid/persistence/query/QueryFilter.g:158:1: direction : ( 'asc' | 'desc' ) ;
    public final QueryFilterParser.direction_return direction() throws RecognitionException {
        QueryFilterParser.direction_return retval = new QueryFilterParser.direction_return();
        retval.start = input.LT(1);

        try {
            // org/usergrid/persistence/query/QueryFilter.g:158:12: ( ( 'asc' | 'desc' ) )
            // org/usergrid/persistence/query/QueryFilter.g:158:14: ( 'asc' | 'desc' )
            {
            if ( (input.LA(1)>=30 && input.LA(1)<=31) ) {
                input.consume();
                state.errorRecovery=false;
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                throw mse;
            }


            }

            retval.stop = input.LT(-1);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "direction"


    // $ANTLR start "order"
    // org/usergrid/persistence/query/QueryFilter.g:160:1: order : ( property ( direction )? ) ;
    public final void order() throws RecognitionException {
        QueryFilterParser.property_return property11 = null;

        QueryFilterParser.direction_return direction12 = null;


        try {
            // org/usergrid/persistence/query/QueryFilter.g:161:2: ( ( property ( direction )? ) )
            // org/usergrid/persistence/query/QueryFilter.g:161:4: ( property ( direction )? )
            {
            // org/usergrid/persistence/query/QueryFilter.g:161:4: ( property ( direction )? )
            // org/usergrid/persistence/query/QueryFilter.g:161:5: property ( direction )?
            {
            pushFollow(FOLLOW_property_in_order835);
            property11=property();

            state._fsp--;

            // org/usergrid/persistence/query/QueryFilter.g:161:14: ( direction )?
            int alt3=2;
            int LA3_0 = input.LA(1);

            if ( ((LA3_0>=30 && LA3_0<=31)) ) {
                alt3=1;
            }
            switch (alt3) {
                case 1 :
                    // org/usergrid/persistence/query/QueryFilter.g:161:14: direction
                    {
                    pushFollow(FOLLOW_direction_in_order837);
                    direction12=direction();

                    state._fsp--;


                    }
                    break;

            }


            }


                
            String property = (property11!=null?input.toString(property11.start,property11.stop):null);
            String direction = (direction12!=null?input.toString(direction12.start,direction12.stop):null);
            SortPredicate sort = new SortPredicate(property, direction);
            query.addSort(sort);
            System.out.println("Parsed query order: " + property + " " + direction);
                


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end "order"


    // $ANTLR start "select_expr"
    // org/usergrid/persistence/query/QueryFilter.g:171:1: select_expr : ( '*' | select_subject ( ',' select_subject )* | '{' select_assign ( ',' select_assign )* '}' ) ;
    public final void select_expr() throws RecognitionException {
        try {
            // org/usergrid/persistence/query/QueryFilter.g:172:2: ( ( '*' | select_subject ( ',' select_subject )* | '{' select_assign ( ',' select_assign )* '}' ) )
            // org/usergrid/persistence/query/QueryFilter.g:172:4: ( '*' | select_subject ( ',' select_subject )* | '{' select_assign ( ',' select_assign )* '}' )
            {
            // org/usergrid/persistence/query/QueryFilter.g:172:4: ( '*' | select_subject ( ',' select_subject )* | '{' select_assign ( ',' select_assign )* '}' )
            int alt6=3;
            switch ( input.LA(1) ) {
            case 32:
                {
                alt6=1;
                }
                break;
            case ID:
                {
                alt6=2;
                }
                break;
            case 33:
                {
                alt6=3;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 6, 0, input);

                throw nvae;
            }

            switch (alt6) {
                case 1 :
                    // org/usergrid/persistence/query/QueryFilter.g:172:5: '*'
                    {
                    match(input,32,FOLLOW_32_in_select_expr851); 

                    }
                    break;
                case 2 :
                    // org/usergrid/persistence/query/QueryFilter.g:172:11: select_subject ( ',' select_subject )*
                    {
                    pushFollow(FOLLOW_select_subject_in_select_expr855);
                    select_subject();

                    state._fsp--;

                    // org/usergrid/persistence/query/QueryFilter.g:172:26: ( ',' select_subject )*
                    loop4:
                    do {
                        int alt4=2;
                        int LA4_0 = input.LA(1);

                        if ( (LA4_0==28) ) {
                            alt4=1;
                        }


                        switch (alt4) {
                    	case 1 :
                    	    // org/usergrid/persistence/query/QueryFilter.g:172:27: ',' select_subject
                    	    {
                    	    match(input,28,FOLLOW_28_in_select_expr858); 
                    	    pushFollow(FOLLOW_select_subject_in_select_expr860);
                    	    select_subject();

                    	    state._fsp--;


                    	    }
                    	    break;

                    	default :
                    	    break loop4;
                        }
                    } while (true);


                    }
                    break;
                case 3 :
                    // org/usergrid/persistence/query/QueryFilter.g:172:51: '{' select_assign ( ',' select_assign )* '}'
                    {
                    match(input,33,FOLLOW_33_in_select_expr867); 
                    pushFollow(FOLLOW_select_assign_in_select_expr869);
                    select_assign();

                    state._fsp--;

                    // org/usergrid/persistence/query/QueryFilter.g:172:69: ( ',' select_assign )*
                    loop5:
                    do {
                        int alt5=2;
                        int LA5_0 = input.LA(1);

                        if ( (LA5_0==28) ) {
                            alt5=1;
                        }


                        switch (alt5) {
                    	case 1 :
                    	    // org/usergrid/persistence/query/QueryFilter.g:172:70: ',' select_assign
                    	    {
                    	    match(input,28,FOLLOW_28_in_select_expr872); 
                    	    pushFollow(FOLLOW_select_assign_in_select_expr874);
                    	    select_assign();

                    	    state._fsp--;


                    	    }
                    	    break;

                    	default :
                    	    break loop5;
                        }
                    } while (true);

                    match(input,34,FOLLOW_34_in_select_expr879); 

                    }
                    break;

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end "select_expr"


    // $ANTLR start "ql"
    // org/usergrid/persistence/query/QueryFilter.g:174:1: ql returns [Query q] : 'select' select_expr ( 'where' where ( 'and' where )* )? ( 'order by' order ( ',' order )* )? ;
    public final Query ql() throws RecognitionException {
        Query q = null;

        try {
            // org/usergrid/persistence/query/QueryFilter.g:175:2: ( 'select' select_expr ( 'where' where ( 'and' where )* )? ( 'order by' order ( ',' order )* )? )
            // org/usergrid/persistence/query/QueryFilter.g:175:4: 'select' select_expr ( 'where' where ( 'and' where )* )? ( 'order by' order ( ',' order )* )?
            {
            match(input,35,FOLLOW_35_in_ql896); 
            pushFollow(FOLLOW_select_expr_in_ql898);
            select_expr();

            state._fsp--;

            // org/usergrid/persistence/query/QueryFilter.g:175:25: ( 'where' where ( 'and' where )* )?
            int alt8=2;
            int LA8_0 = input.LA(1);

            if ( (LA8_0==36) ) {
                alt8=1;
            }
            switch (alt8) {
                case 1 :
                    // org/usergrid/persistence/query/QueryFilter.g:175:26: 'where' where ( 'and' where )*
                    {
                    match(input,36,FOLLOW_36_in_ql901); 
                    pushFollow(FOLLOW_where_in_ql903);
                    where();

                    state._fsp--;

                    // org/usergrid/persistence/query/QueryFilter.g:175:40: ( 'and' where )*
                    loop7:
                    do {
                        int alt7=2;
                        int LA7_0 = input.LA(1);

                        if ( (LA7_0==37) ) {
                            alt7=1;
                        }


                        switch (alt7) {
                    	case 1 :
                    	    // org/usergrid/persistence/query/QueryFilter.g:175:41: 'and' where
                    	    {
                    	    match(input,37,FOLLOW_37_in_ql906); 
                    	    pushFollow(FOLLOW_where_in_ql908);
                    	    where();

                    	    state._fsp--;


                    	    }
                    	    break;

                    	default :
                    	    break loop7;
                        }
                    } while (true);


                    }
                    break;

            }

            // org/usergrid/persistence/query/QueryFilter.g:175:57: ( 'order by' order ( ',' order )* )?
            int alt10=2;
            int LA10_0 = input.LA(1);

            if ( (LA10_0==38) ) {
                alt10=1;
            }
            switch (alt10) {
                case 1 :
                    // org/usergrid/persistence/query/QueryFilter.g:175:58: 'order by' order ( ',' order )*
                    {
                    match(input,38,FOLLOW_38_in_ql915); 
                    pushFollow(FOLLOW_order_in_ql917);
                    order();

                    state._fsp--;

                    // org/usergrid/persistence/query/QueryFilter.g:175:75: ( ',' order )*
                    loop9:
                    do {
                        int alt9=2;
                        int LA9_0 = input.LA(1);

                        if ( (LA9_0==28) ) {
                            alt9=1;
                        }


                        switch (alt9) {
                    	case 1 :
                    	    // org/usergrid/persistence/query/QueryFilter.g:175:76: ',' order
                    	    {
                    	    match(input,28,FOLLOW_28_in_ql920); 
                    	    pushFollow(FOLLOW_order_in_ql922);
                    	    order();

                    	    state._fsp--;


                    	    }
                    	    break;

                    	default :
                    	    break loop9;
                        }
                    } while (true);


                    }
                    break;

            }



            q = query;



            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return q;
    }
    // $ANTLR end "ql"

    // Delegated rules


 

    public static final BitSet FOLLOW_ID_in_property585 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_set_in_operator597 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_set_in_value652 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_set_in_second_value679 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_property_in_filter716 = new BitSet(new long[]{0x000000000FFF0000L});
    public static final BitSet FOLLOW_operator_in_filter718 = new BitSet(new long[]{0x00000000000016A0L});
    public static final BitSet FOLLOW_value_in_filter720 = new BitSet(new long[]{0x0000000010000000L});
    public static final BitSet FOLLOW_28_in_filter723 = new BitSet(new long[]{0x00000000000016A0L});
    public static final BitSet FOLLOW_second_value_in_filter725 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_filter731 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_select_subject741 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_select_assign_target753 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_select_assign_source764 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_select_assign_target_in_select_assign777 = new BitSet(new long[]{0x0000000020000000L});
    public static final BitSet FOLLOW_29_in_select_assign779 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_select_assign_source_in_select_assign781 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_property_in_where795 = new BitSet(new long[]{0x000000000FFF0000L});
    public static final BitSet FOLLOW_operator_in_where797 = new BitSet(new long[]{0x00000000000016A0L});
    public static final BitSet FOLLOW_value_in_where799 = new BitSet(new long[]{0x0000000010000002L});
    public static final BitSet FOLLOW_28_in_where802 = new BitSet(new long[]{0x00000000000016A0L});
    public static final BitSet FOLLOW_second_value_in_where804 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_set_in_direction819 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_property_in_order835 = new BitSet(new long[]{0x00000000C0000002L});
    public static final BitSet FOLLOW_direction_in_order837 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_32_in_select_expr851 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_select_subject_in_select_expr855 = new BitSet(new long[]{0x0000000010000002L});
    public static final BitSet FOLLOW_28_in_select_expr858 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_select_subject_in_select_expr860 = new BitSet(new long[]{0x0000000010000002L});
    public static final BitSet FOLLOW_33_in_select_expr867 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_select_assign_in_select_expr869 = new BitSet(new long[]{0x0000000410000000L});
    public static final BitSet FOLLOW_28_in_select_expr872 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_select_assign_in_select_expr874 = new BitSet(new long[]{0x0000000410000000L});
    public static final BitSet FOLLOW_34_in_select_expr879 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_35_in_ql896 = new BitSet(new long[]{0x0000000300000010L});
    public static final BitSet FOLLOW_select_expr_in_ql898 = new BitSet(new long[]{0x0000005000000002L});
    public static final BitSet FOLLOW_36_in_ql901 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_where_in_ql903 = new BitSet(new long[]{0x0000006000000002L});
    public static final BitSet FOLLOW_37_in_ql906 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_where_in_ql908 = new BitSet(new long[]{0x0000006000000002L});
    public static final BitSet FOLLOW_38_in_ql915 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_order_in_ql917 = new BitSet(new long[]{0x0000000010000002L});
    public static final BitSet FOLLOW_28_in_ql920 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_order_in_ql922 = new BitSet(new long[]{0x0000000010000002L});

}