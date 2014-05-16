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
// $ANTLR 3.1.3 Mar 17, 2009 19:23:44 org/usergrid/persistence/query/QueryFilter.g 2012-03-07 22:54:28

package org.apache.usergrid.mq;


import org.antlr.runtime.BitSet;
import org.antlr.runtime.MismatchedSetException;
import org.antlr.runtime.NoViableAltException;
import org.antlr.runtime.Parser;
import org.antlr.runtime.ParserRuleReturnScope;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.RecognizerSharedState;
import org.antlr.runtime.Token;
import org.antlr.runtime.TokenStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.mq.Query.FilterPredicate;
import org.apache.usergrid.mq.Query.SortPredicate;


public class QueryFilterParser extends Parser {
    public static final String[] tokenNames = new String[] {
            "<invalid>", "<EOR>", "<DOWN>", "<UP>", "ID", "INT", "EXPONENT", "FLOAT", "ESC_SEQ", "STRING", "BOOLEAN",
            "HEX_DIGIT", "UUID", "UNICODE_ESC", "OCTAL_ESC", "WS", "'<'", "'<='", "'='", "'>'", "'>='", "'in'", "'eq'",
            "'lt'", "'gt'", "'lte'", "'gte'", "'contains'", "'within'", "','", "'of'", "':'", "'asc'", "'desc'", "'*'",
            "'{'", "'}'", "'select'", "'where'", "'and'", "'order by'"
    };
    public static final int T__40 = 40;
    public static final int EXPONENT = 6;
    public static final int T__29 = 29;
    public static final int T__28 = 28;
    public static final int T__27 = 27;
    public static final int T__26 = 26;
    public static final int UUID = 12;
    public static final int T__25 = 25;
    public static final int T__24 = 24;
    public static final int T__23 = 23;
    public static final int T__22 = 22;
    public static final int T__21 = 21;
    public static final int UNICODE_ESC = 13;
    public static final int T__20 = 20;
    public static final int OCTAL_ESC = 14;
    public static final int HEX_DIGIT = 11;
    public static final int FLOAT = 7;
    public static final int INT = 5;
    public static final int ID = 4;
    public static final int EOF = -1;
    public static final int T__19 = 19;
    public static final int T__30 = 30;
    public static final int T__31 = 31;
    public static final int T__32 = 32;
    public static final int WS = 15;
    public static final int BOOLEAN = 10;
    public static final int ESC_SEQ = 8;
    public static final int T__33 = 33;
    public static final int T__16 = 16;
    public static final int T__34 = 34;
    public static final int T__35 = 35;
    public static final int T__18 = 18;
    public static final int T__36 = 36;
    public static final int T__17 = 17;
    public static final int T__37 = 37;
    public static final int T__38 = 38;
    public static final int T__39 = 39;
    public static final int STRING = 9;

    // delegates
    // delegators


    public QueryFilterParser( TokenStream input ) {
        this( input, new RecognizerSharedState() );
    }


    public QueryFilterParser( TokenStream input, RecognizerSharedState state ) {
        super( input, state );
    }


    public String[] getTokenNames() { return QueryFilterParser.tokenNames; }


    public String getGrammarFileName() { return "org/usergrid/persistence/query/QueryFilter.g"; }


    Query query = new Query();

    private static final Logger logger = LoggerFactory.getLogger( QueryFilterLexer.class );


    @Override
    public void emitErrorMessage( String msg ) {
        logger.info( msg );
    }


    public static class property_return extends ParserRuleReturnScope {}


    // $ANTLR start "property"
    // org/usergrid/persistence/query/QueryFilter.g:101:1: property : ( ID ) ;
    public final QueryFilterParser.property_return property() throws RecognitionException {
        QueryFilterParser.property_return retval = new QueryFilterParser.property_return();
        retval.start = input.LT( 1 );

        try {
            // org/usergrid/persistence/query/QueryFilter.g:102:2: ( ( ID ) )
            // org/usergrid/persistence/query/QueryFilter.g:102:5: ( ID )
            {
                // org/usergrid/persistence/query/QueryFilter.g:102:5: ( ID )
                // org/usergrid/persistence/query/QueryFilter.g:102:6: ID
                {
                    match( input, ID, FOLLOW_ID_in_property597 );
                }
            }

            retval.stop = input.LT( -1 );
        }
        catch ( RecognitionException re ) {
            reportError( re );
            recover( input, re );
        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "property"


    public static class operator_return extends ParserRuleReturnScope {}


    // $ANTLR start "operator"
    // org/usergrid/persistence/query/QueryFilter.g:104:1: operator : ( '<' | '<=' | '=' | '>' | '>=' | 'in' | 'eq' |
    // 'lt' | 'gt' | 'lte' | 'gte' | 'contains' | 'within' ) ;
    public final QueryFilterParser.operator_return operator() throws RecognitionException {
        QueryFilterParser.operator_return retval = new QueryFilterParser.operator_return();
        retval.start = input.LT( 1 );

        try {
            // org/usergrid/persistence/query/QueryFilter.g:105:2: ( ( '<' | '<=' | '=' | '>' | '>=' | 'in' | 'eq' |
            // 'lt' | 'gt' | 'lte' | 'gte' | 'contains' | 'within' ) )
            // org/usergrid/persistence/query/QueryFilter.g:105:4: ( '<' | '<=' | '=' | '>' | '>=' | 'in' | 'eq' |
            // 'lt' | 'gt' | 'lte' | 'gte' | 'contains' | 'within' )
            {
                if ( ( input.LA( 1 ) >= 16 && input.LA( 1 ) <= 28 ) ) {
                    input.consume();
                    state.errorRecovery = false;
                }
                else {
                    MismatchedSetException mse = new MismatchedSetException( null, input );
                    throw mse;
                }
            }

            retval.stop = input.LT( -1 );
        }
        catch ( RecognitionException re ) {
            reportError( re );
            recover( input, re );
        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "operator"


    public static class value_return extends ParserRuleReturnScope {}


    // $ANTLR start "value"
    // org/usergrid/persistence/query/QueryFilter.g:107:1: value : ( BOOLEAN | STRING | INT | FLOAT | UUID ) ;
    public final QueryFilterParser.value_return value() throws RecognitionException {
        QueryFilterParser.value_return retval = new QueryFilterParser.value_return();
        retval.start = input.LT( 1 );

        try {
            // org/usergrid/persistence/query/QueryFilter.g:107:8: ( ( BOOLEAN | STRING | INT | FLOAT | UUID ) )
            // org/usergrid/persistence/query/QueryFilter.g:107:10: ( BOOLEAN | STRING | INT | FLOAT | UUID )
            {
                if ( input.LA( 1 ) == INT || input.LA( 1 ) == FLOAT || ( input.LA( 1 ) >= STRING
                        && input.LA( 1 ) <= BOOLEAN ) || input.LA( 1 ) == UUID ) {
                    input.consume();
                    state.errorRecovery = false;
                }
                else {
                    MismatchedSetException mse = new MismatchedSetException( null, input );
                    throw mse;
                }
            }

            retval.stop = input.LT( -1 );
        }
        catch ( RecognitionException re ) {
            reportError( re );
            recover( input, re );
        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "value"


    public static class second_value_return extends ParserRuleReturnScope {}


    // $ANTLR start "second_value"
    // org/usergrid/persistence/query/QueryFilter.g:109:1: second_value : ( BOOLEAN | STRING | INT | FLOAT | UUID ) ;
    public final QueryFilterParser.second_value_return second_value() throws RecognitionException {
        QueryFilterParser.second_value_return retval = new QueryFilterParser.second_value_return();
        retval.start = input.LT( 1 );

        try {
            // org/usergrid/persistence/query/QueryFilter.g:109:15: ( ( BOOLEAN | STRING | INT | FLOAT | UUID ) )
            // org/usergrid/persistence/query/QueryFilter.g:109:17: ( BOOLEAN | STRING | INT | FLOAT | UUID )
            {
                if ( input.LA( 1 ) == INT || input.LA( 1 ) == FLOAT || ( input.LA( 1 ) >= STRING
                        && input.LA( 1 ) <= BOOLEAN ) || input.LA( 1 ) == UUID ) {
                    input.consume();
                    state.errorRecovery = false;
                }
                else {
                    MismatchedSetException mse = new MismatchedSetException( null, input );
                    throw mse;
                }
            }

            retval.stop = input.LT( -1 );
        }
        catch ( RecognitionException re ) {
            reportError( re );
            recover( input, re );
        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "second_value"


    public static class third_value_return extends ParserRuleReturnScope {}


    // $ANTLR start "third_value"
    // org/usergrid/persistence/query/QueryFilter.g:111:1: third_value : ( BOOLEAN | STRING | INT | FLOAT | UUID ) ;
    public final QueryFilterParser.third_value_return third_value() throws RecognitionException {
        QueryFilterParser.third_value_return retval = new QueryFilterParser.third_value_return();
        retval.start = input.LT( 1 );

        try {
            // org/usergrid/persistence/query/QueryFilter.g:111:14: ( ( BOOLEAN | STRING | INT | FLOAT | UUID ) )
            // org/usergrid/persistence/query/QueryFilter.g:111:16: ( BOOLEAN | STRING | INT | FLOAT | UUID )
            {
                if ( input.LA( 1 ) == INT || input.LA( 1 ) == FLOAT || ( input.LA( 1 ) >= STRING
                        && input.LA( 1 ) <= BOOLEAN ) || input.LA( 1 ) == UUID ) {
                    input.consume();
                    state.errorRecovery = false;
                }
                else {
                    MismatchedSetException mse = new MismatchedSetException( null, input );
                    throw mse;
                }
            }

            retval.stop = input.LT( -1 );
        }
        catch ( RecognitionException re ) {
            reportError( re );
            recover( input, re );
        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "third_value"


    // $ANTLR start "filter"
    // org/usergrid/persistence/query/QueryFilter.g:113:1: filter returns [FilterPredicate filter] : property
    // operator value ( ( ',' | 'of' ) second_value ( ',' third_value )? )? EOF ;
    public final FilterPredicate filter() throws RecognitionException {
        FilterPredicate filter = null;

        QueryFilterParser.property_return property1 = null;

        QueryFilterParser.operator_return operator2 = null;

        QueryFilterParser.value_return value3 = null;

        QueryFilterParser.second_value_return second_value4 = null;

        QueryFilterParser.third_value_return third_value5 = null;


        try {
            // org/usergrid/persistence/query/QueryFilter.g:114:5: ( property operator value ( ( ',
            // ' | 'of' ) second_value ( ',' third_value )? )? EOF )
            // org/usergrid/persistence/query/QueryFilter.g:114:9: property operator value ( ( ',
            // ' | 'of' ) second_value ( ',' third_value )? )? EOF
            {
                pushFollow( FOLLOW_property_in_filter759 );
                property1 = property();

                state._fsp--;

                pushFollow( FOLLOW_operator_in_filter761 );
                operator2 = operator();

                state._fsp--;

                pushFollow( FOLLOW_value_in_filter763 );
                value3 = value();

                state._fsp--;

                // org/usergrid/persistence/query/QueryFilter.g:114:33: ( ( ',' | 'of' ) second_value ( ',
                // ' third_value )? )?
                int alt2 = 2;
                int LA2_0 = input.LA( 1 );

                if ( ( ( LA2_0 >= 29 && LA2_0 <= 30 ) ) ) {
                    alt2 = 1;
                }
                switch ( alt2 ) {
                    case 1:
                        // org/usergrid/persistence/query/QueryFilter.g:114:34: ( ',' | 'of' ) second_value ( ',
                        // ' third_value )?
                    {
                        if ( ( input.LA( 1 ) >= 29 && input.LA( 1 ) <= 30 ) ) {
                            input.consume();
                            state.errorRecovery = false;
                        }
                        else {
                            MismatchedSetException mse = new MismatchedSetException( null, input );
                            throw mse;
                        }

                        pushFollow( FOLLOW_second_value_in_filter774 );
                        second_value4 = second_value();

                        state._fsp--;

                        // org/usergrid/persistence/query/QueryFilter.g:114:60: ( ',' third_value )?
                        int alt1 = 2;
                        int LA1_0 = input.LA( 1 );

                        if ( ( LA1_0 == 29 ) ) {
                            alt1 = 1;
                        }
                        switch ( alt1 ) {
                            case 1:
                                // org/usergrid/persistence/query/QueryFilter.g:114:62: ',' third_value
                            {
                                match( input, 29, FOLLOW_29_in_filter778 );
                                pushFollow( FOLLOW_third_value_in_filter780 );
                                third_value5 = third_value();

                                state._fsp--;
                            }
                            break;
                        }
                    }
                    break;
                }


                String property = ( property1 != null ? input.toString( property1.start, property1.stop ) : null );
                String operator = ( operator2 != null ? input.toString( operator2.start, operator2.stop ) : null );
                String value = ( value3 != null ? input.toString( value3.start, value3.stop ) : null );
                String second_value =
                        ( second_value4 != null ? input.toString( second_value4.start, second_value4.stop ) : null );
                String third_value =
                        ( third_value5 != null ? input.toString( third_value5.start, third_value5.stop ) : null );
                filter = new FilterPredicate( property, operator, value, second_value, third_value );
                //System.out.println("Parsed query filter: " + property + " " + operator + " " + value + " " +
                // second_value);


                match( input, EOF, FOLLOW_EOF_in_filter789 );
            }
        }
        catch ( RecognitionException re ) {
            reportError( re );
            recover( input, re );
        }
        finally {
        }
        return filter;
    }
    // $ANTLR end "filter"


    public static class select_subject_return extends ParserRuleReturnScope {}


    // $ANTLR start "select_subject"
    // org/usergrid/persistence/query/QueryFilter.g:127:1: select_subject : ID ;
    public final QueryFilterParser.select_subject_return select_subject() throws RecognitionException {
        QueryFilterParser.select_subject_return retval = new QueryFilterParser.select_subject_return();
        retval.start = input.LT( 1 );

        try {
            // org/usergrid/persistence/query/QueryFilter.g:128:2: ( ID )
            // org/usergrid/persistence/query/QueryFilter.g:128:4: ID
            {
                match( input, ID, FOLLOW_ID_in_select_subject800 );


                query.addSelect( input.toString( retval.start, input.LT( -1 ) ) );
            }

            retval.stop = input.LT( -1 );
        }
        catch ( RecognitionException re ) {
            reportError( re );
            recover( input, re );
        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "select_subject"


    public static class select_assign_target_return extends ParserRuleReturnScope {}


    // $ANTLR start "select_assign_target"
    // org/usergrid/persistence/query/QueryFilter.g:134:1: select_assign_target : ID ;
    public final QueryFilterParser.select_assign_target_return select_assign_target() throws RecognitionException {
        QueryFilterParser.select_assign_target_return retval = new QueryFilterParser.select_assign_target_return();
        retval.start = input.LT( 1 );

        try {
            // org/usergrid/persistence/query/QueryFilter.g:135:2: ( ID )
            // org/usergrid/persistence/query/QueryFilter.g:135:4: ID
            {
                match( input, ID, FOLLOW_ID_in_select_assign_target812 );
            }

            retval.stop = input.LT( -1 );
        }
        catch ( RecognitionException re ) {
            reportError( re );
            recover( input, re );
        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "select_assign_target"


    public static class select_assign_source_return extends ParserRuleReturnScope {}


    // $ANTLR start "select_assign_source"
    // org/usergrid/persistence/query/QueryFilter.g:137:1: select_assign_source : ID ;
    public final QueryFilterParser.select_assign_source_return select_assign_source() throws RecognitionException {
        QueryFilterParser.select_assign_source_return retval = new QueryFilterParser.select_assign_source_return();
        retval.start = input.LT( 1 );

        try {
            // org/usergrid/persistence/query/QueryFilter.g:138:2: ( ID )
            // org/usergrid/persistence/query/QueryFilter.g:138:4: ID
            {
                match( input, ID, FOLLOW_ID_in_select_assign_source823 );
            }

            retval.stop = input.LT( -1 );
        }
        catch ( RecognitionException re ) {
            reportError( re );
            recover( input, re );
        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "select_assign_source"


    // $ANTLR start "select_assign"
    // org/usergrid/persistence/query/QueryFilter.g:140:1: select_assign : select_assign_target ':'
    // select_assign_source ;
    public final void select_assign() throws RecognitionException {
        QueryFilterParser.select_assign_source_return select_assign_source6 = null;

        QueryFilterParser.select_assign_target_return select_assign_target7 = null;


        try {
            // org/usergrid/persistence/query/QueryFilter.g:141:2: ( select_assign_target ':' select_assign_source )
            // org/usergrid/persistence/query/QueryFilter.g:141:4: select_assign_target ':' select_assign_source
            {
                pushFollow( FOLLOW_select_assign_target_in_select_assign836 );
                select_assign_target7 = select_assign_target();

                state._fsp--;

                match( input, 31, FOLLOW_31_in_select_assign838 );
                pushFollow( FOLLOW_select_assign_source_in_select_assign840 );
                select_assign_source6 = select_assign_source();

                state._fsp--;


                query.addSelect( ( select_assign_source6 != null ?
                                   input.toString( select_assign_source6.start, select_assign_source6.stop ) : null ),
                        ( select_assign_target7 != null ?
                          input.toString( select_assign_target7.start, select_assign_target7.stop ) : null ) );
            }
        }
        catch ( RecognitionException re ) {
            reportError( re );
            recover( input, re );
        }
        finally {
        }
        return;
    }
    // $ANTLR end "select_assign"


    // $ANTLR start "where"
    // org/usergrid/persistence/query/QueryFilter.g:148:1: where : ( property operator value ( ( ',
    // ' | 'of' ) second_value ( ',' third_value )? )? ) ;
    public final void where() throws RecognitionException {
        QueryFilterParser.property_return property8 = null;

        QueryFilterParser.operator_return operator9 = null;

        QueryFilterParser.value_return value10 = null;

        QueryFilterParser.second_value_return second_value11 = null;

        QueryFilterParser.third_value_return third_value12 = null;


        try {
            // org/usergrid/persistence/query/QueryFilter.g:149:2: ( ( property operator value ( ( ',
            // ' | 'of' ) second_value ( ',' third_value )? )? ) )
            // org/usergrid/persistence/query/QueryFilter.g:149:4: ( property operator value ( ( ',
            // ' | 'of' ) second_value ( ',' third_value )? )? )
            {
                // org/usergrid/persistence/query/QueryFilter.g:149:4: ( property operator value ( ( ',
                // ' | 'of' ) second_value ( ',' third_value )? )? )
                // org/usergrid/persistence/query/QueryFilter.g:149:5: property operator value ( ( ',
                // ' | 'of' ) second_value ( ',' third_value )? )?
                {
                    pushFollow( FOLLOW_property_in_where855 );
                    property8 = property();

                    state._fsp--;

                    pushFollow( FOLLOW_operator_in_where857 );
                    operator9 = operator();

                    state._fsp--;

                    pushFollow( FOLLOW_value_in_where859 );
                    value10 = value();

                    state._fsp--;

                    // org/usergrid/persistence/query/QueryFilter.g:149:29: ( ( ',' | 'of' ) second_value ( ',
                    // ' third_value )? )?
                    int alt4 = 2;
                    int LA4_0 = input.LA( 1 );

                    if ( ( ( LA4_0 >= 29 && LA4_0 <= 30 ) ) ) {
                        alt4 = 1;
                    }
                    switch ( alt4 ) {
                        case 1:
                            // org/usergrid/persistence/query/QueryFilter.g:149:30: ( ',' | 'of' ) second_value ( ',
                            // ' third_value )?
                        {
                            if ( ( input.LA( 1 ) >= 29 && input.LA( 1 ) <= 30 ) ) {
                                input.consume();
                                state.errorRecovery = false;
                            }
                            else {
                                MismatchedSetException mse = new MismatchedSetException( null, input );
                                throw mse;
                            }

                            pushFollow( FOLLOW_second_value_in_where870 );
                            second_value11 = second_value();

                            state._fsp--;

                            // org/usergrid/persistence/query/QueryFilter.g:149:56: ( ',' third_value )?
                            int alt3 = 2;
                            int LA3_0 = input.LA( 1 );

                            if ( ( LA3_0 == 29 ) ) {
                                alt3 = 1;
                            }
                            switch ( alt3 ) {
                                case 1:
                                    // org/usergrid/persistence/query/QueryFilter.g:149:58: ',' third_value
                                {
                                    match( input, 29, FOLLOW_29_in_where874 );
                                    pushFollow( FOLLOW_third_value_in_where876 );
                                    third_value12 = third_value();

                                    state._fsp--;
                                }
                                break;
                            }
                        }
                        break;
                    }


                    String property = ( property8 != null ? input.toString( property8.start, property8.stop ) : null );
                    String operator = ( operator9 != null ? input.toString( operator9.start, operator9.stop ) : null );
                    String value = ( value10 != null ? input.toString( value10.start, value10.stop ) : null );
                    int value_type = ( value10 != null ? ( ( Token ) value10.start ) : null ) != null ?
                                     ( value10 != null ? ( ( Token ) value10.start ) : null ).getType() : 0;
                    String second_value =
                            ( second_value11 != null ? input.toString( second_value11.start, second_value11.stop ) :
                              null );
                    int second_value_type =
                            ( second_value11 != null ? ( ( Token ) second_value11.start ) : null ) != null ?
                            ( second_value11 != null ? ( ( Token ) second_value11.start ) : null ).getType() : 0;
                    String third_value =
                            ( third_value12 != null ? input.toString( third_value12.start, third_value12.stop ) :
                              null );
                    int third_value_type =
                            ( third_value12 != null ? ( ( Token ) third_value12.start ) : null ) != null ?
                            ( third_value12 != null ? ( ( Token ) third_value12.start ) : null ).getType() : 0;
                    FilterPredicate filter =
                            new FilterPredicate( property, operator, value, value_type, second_value, second_value_type,
                                    third_value, third_value_type );
                    query.addFilter( filter );
                    //System.out.println("Parsed query filter: " + property + " " + operator + " " + value + " " +
                    // second_value);


                }
            }
        }
        catch ( RecognitionException re ) {
            reportError( re );
            recover( input, re );
        }
        finally {
        }
        return;
    }
    // $ANTLR end "where"


    public static class direction_return extends ParserRuleReturnScope {}


    // $ANTLR start "direction"
    // org/usergrid/persistence/query/QueryFilter.g:165:1: direction : ( 'asc' | 'desc' ) ;
    public final QueryFilterParser.direction_return direction() throws RecognitionException {
        QueryFilterParser.direction_return retval = new QueryFilterParser.direction_return();
        retval.start = input.LT( 1 );

        try {
            // org/usergrid/persistence/query/QueryFilter.g:165:12: ( ( 'asc' | 'desc' ) )
            // org/usergrid/persistence/query/QueryFilter.g:165:14: ( 'asc' | 'desc' )
            {
                if ( ( input.LA( 1 ) >= 32 && input.LA( 1 ) <= 33 ) ) {
                    input.consume();
                    state.errorRecovery = false;
                }
                else {
                    MismatchedSetException mse = new MismatchedSetException( null, input );
                    throw mse;
                }
            }

            retval.stop = input.LT( -1 );
        }
        catch ( RecognitionException re ) {
            reportError( re );
            recover( input, re );
        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "direction"


    // $ANTLR start "order"
    // org/usergrid/persistence/query/QueryFilter.g:167:1: order : ( property ( direction )? ) ;
    public final void order() throws RecognitionException {
        QueryFilterParser.property_return property13 = null;

        QueryFilterParser.direction_return direction14 = null;


        try {
            // org/usergrid/persistence/query/QueryFilter.g:168:2: ( ( property ( direction )? ) )
            // org/usergrid/persistence/query/QueryFilter.g:168:4: ( property ( direction )? )
            {
                // org/usergrid/persistence/query/QueryFilter.g:168:4: ( property ( direction )? )
                // org/usergrid/persistence/query/QueryFilter.g:168:5: property ( direction )?
                {
                    pushFollow( FOLLOW_property_in_order909 );
                    property13 = property();

                    state._fsp--;

                    // org/usergrid/persistence/query/QueryFilter.g:168:14: ( direction )?
                    int alt5 = 2;
                    int LA5_0 = input.LA( 1 );

                    if ( ( ( LA5_0 >= 32 && LA5_0 <= 33 ) ) ) {
                        alt5 = 1;
                    }
                    switch ( alt5 ) {
                        case 1:
                            // org/usergrid/persistence/query/QueryFilter.g:168:14: direction
                        {
                            pushFollow( FOLLOW_direction_in_order911 );
                            direction14 = direction();

                            state._fsp--;
                        }
                        break;
                    }
                }


                String property = ( property13 != null ? input.toString( property13.start, property13.stop ) : null );
                String direction =
                        ( direction14 != null ? input.toString( direction14.start, direction14.stop ) : null );
                SortPredicate sort = new SortPredicate( property, direction );
                query.addSort( sort );
                System.out.println( "Parsed query order: " + property + " " + direction );
            }
        }
        catch ( RecognitionException re ) {
            reportError( re );
            recover( input, re );
        }
        finally {
        }
        return;
    }
    // $ANTLR end "order"


    // $ANTLR start "select_expr"
    // org/usergrid/persistence/query/QueryFilter.g:178:1: select_expr : ( '*' | select_subject ( ',
    // ' select_subject )* | '{' select_assign ( ',' select_assign )* '}' ) ;
    public final void select_expr() throws RecognitionException {
        try {
            // org/usergrid/persistence/query/QueryFilter.g:179:2: ( ( '*' | select_subject ( ',
            // ' select_subject )* | '{' select_assign ( ',' select_assign )* '}' ) )
            // org/usergrid/persistence/query/QueryFilter.g:179:4: ( '*' | select_subject ( ',
            // ' select_subject )* | '{' select_assign ( ',' select_assign )* '}' )
            {
                // org/usergrid/persistence/query/QueryFilter.g:179:4: ( '*' | select_subject ( ',
                // ' select_subject )* | '{' select_assign ( ',' select_assign )* '}' )
                int alt8 = 3;
                switch ( input.LA( 1 ) ) {
                    case 34: {
                        alt8 = 1;
                    }
                    break;
                    case ID: {
                        alt8 = 2;
                    }
                    break;
                    case 35: {
                        alt8 = 3;
                    }
                    break;
                    default:
                        NoViableAltException nvae = new NoViableAltException( "", 8, 0, input );

                        throw nvae;
                }

                switch ( alt8 ) {
                    case 1:
                        // org/usergrid/persistence/query/QueryFilter.g:179:5: '*'
                    {
                        match( input, 34, FOLLOW_34_in_select_expr925 );
                    }
                    break;
                    case 2:
                        // org/usergrid/persistence/query/QueryFilter.g:179:11: select_subject ( ',' select_subject )*
                    {
                        pushFollow( FOLLOW_select_subject_in_select_expr929 );
                        select_subject();

                        state._fsp--;

                        // org/usergrid/persistence/query/QueryFilter.g:179:26: ( ',' select_subject )*
                        loop6:
                        do {
                            int alt6 = 2;
                            int LA6_0 = input.LA( 1 );

                            if ( ( LA6_0 == 29 ) ) {
                                alt6 = 1;
                            }


                            switch ( alt6 ) {
                                case 1:
                                    // org/usergrid/persistence/query/QueryFilter.g:179:27: ',' select_subject
                                {
                                    match( input, 29, FOLLOW_29_in_select_expr932 );
                                    pushFollow( FOLLOW_select_subject_in_select_expr934 );
                                    select_subject();

                                    state._fsp--;
                                }
                                break;

                                default:
                                    break loop6;
                            }
                        }
                        while ( true );
                    }
                    break;
                    case 3:
                        // org/usergrid/persistence/query/QueryFilter.g:179:51: '{' select_assign ( ',
                        // ' select_assign )* '}'
                    {
                        match( input, 35, FOLLOW_35_in_select_expr941 );
                        pushFollow( FOLLOW_select_assign_in_select_expr943 );
                        select_assign();

                        state._fsp--;

                        // org/usergrid/persistence/query/QueryFilter.g:179:69: ( ',' select_assign )*
                        loop7:
                        do {
                            int alt7 = 2;
                            int LA7_0 = input.LA( 1 );

                            if ( ( LA7_0 == 29 ) ) {
                                alt7 = 1;
                            }


                            switch ( alt7 ) {
                                case 1:
                                    // org/usergrid/persistence/query/QueryFilter.g:179:70: ',' select_assign
                                {
                                    match( input, 29, FOLLOW_29_in_select_expr946 );
                                    pushFollow( FOLLOW_select_assign_in_select_expr948 );
                                    select_assign();

                                    state._fsp--;
                                }
                                break;

                                default:
                                    break loop7;
                            }
                        }
                        while ( true );

                        match( input, 36, FOLLOW_36_in_select_expr953 );
                    }
                    break;
                }
            }
        }
        catch ( RecognitionException re ) {
            reportError( re );
            recover( input, re );
        }
        finally {
        }
        return;
    }
    // $ANTLR end "select_expr"


    // $ANTLR start "ql"
    // org/usergrid/persistence/query/QueryFilter.g:181:1: ql returns [Query q] : 'select' select_expr ( 'where'
    // where ( 'and' where )* )? ( 'order by' order ( ',' order )* )? ;
    public final Query ql() throws RecognitionException {
        Query q = null;

        try {
            // org/usergrid/persistence/query/QueryFilter.g:182:2: ( 'select' select_expr ( 'where' where ( 'and'
            // where )* )? ( 'order by' order ( ',' order )* )? )
            // org/usergrid/persistence/query/QueryFilter.g:182:4: 'select' select_expr ( 'where' where ( 'and' where
            // )* )? ( 'order by' order ( ',' order )* )?
            {
                match( input, 37, FOLLOW_37_in_ql970 );
                pushFollow( FOLLOW_select_expr_in_ql972 );
                select_expr();

                state._fsp--;

                // org/usergrid/persistence/query/QueryFilter.g:182:25: ( 'where' where ( 'and' where )* )?
                int alt10 = 2;
                int LA10_0 = input.LA( 1 );

                if ( ( LA10_0 == 38 ) ) {
                    alt10 = 1;
                }
                switch ( alt10 ) {
                    case 1:
                        // org/usergrid/persistence/query/QueryFilter.g:182:26: 'where' where ( 'and' where )*
                    {
                        match( input, 38, FOLLOW_38_in_ql975 );
                        pushFollow( FOLLOW_where_in_ql977 );
                        where();

                        state._fsp--;

                        // org/usergrid/persistence/query/QueryFilter.g:182:40: ( 'and' where )*
                        loop9:
                        do {
                            int alt9 = 2;
                            int LA9_0 = input.LA( 1 );

                            if ( ( LA9_0 == 39 ) ) {
                                alt9 = 1;
                            }


                            switch ( alt9 ) {
                                case 1:
                                    // org/usergrid/persistence/query/QueryFilter.g:182:41: 'and' where
                                {
                                    match( input, 39, FOLLOW_39_in_ql980 );
                                    pushFollow( FOLLOW_where_in_ql982 );
                                    where();

                                    state._fsp--;
                                }
                                break;

                                default:
                                    break loop9;
                            }
                        }
                        while ( true );
                    }
                    break;
                }

                // org/usergrid/persistence/query/QueryFilter.g:182:57: ( 'order by' order ( ',' order )* )?
                int alt12 = 2;
                int LA12_0 = input.LA( 1 );

                if ( ( LA12_0 == 40 ) ) {
                    alt12 = 1;
                }
                switch ( alt12 ) {
                    case 1:
                        // org/usergrid/persistence/query/QueryFilter.g:182:58: 'order by' order ( ',' order )*
                    {
                        match( input, 40, FOLLOW_40_in_ql989 );
                        pushFollow( FOLLOW_order_in_ql991 );
                        order();

                        state._fsp--;

                        // org/usergrid/persistence/query/QueryFilter.g:182:75: ( ',' order )*
                        loop11:
                        do {
                            int alt11 = 2;
                            int LA11_0 = input.LA( 1 );

                            if ( ( LA11_0 == 29 ) ) {
                                alt11 = 1;
                            }


                            switch ( alt11 ) {
                                case 1:
                                    // org/usergrid/persistence/query/QueryFilter.g:182:76: ',' order
                                {
                                    match( input, 29, FOLLOW_29_in_ql994 );
                                    pushFollow( FOLLOW_order_in_ql996 );
                                    order();

                                    state._fsp--;
                                }
                                break;

                                default:
                                    break loop11;
                            }
                        }
                        while ( true );
                    }
                    break;
                }


                q = query;
            }
        }
        catch ( RecognitionException re ) {
            reportError( re );
            recover( input, re );
        }
        finally {
        }
        return q;
    }
    // $ANTLR end "ql"

    // Delegated rules


    public static final BitSet FOLLOW_ID_in_property597 = new BitSet( new long[] { 0x0000000000000002L } );
    public static final BitSet FOLLOW_set_in_operator609 = new BitSet( new long[] { 0x0000000000000002L } );
    public static final BitSet FOLLOW_set_in_value668 = new BitSet( new long[] { 0x0000000000000002L } );
    public static final BitSet FOLLOW_set_in_second_value695 = new BitSet( new long[] { 0x0000000000000002L } );
    public static final BitSet FOLLOW_set_in_third_value722 = new BitSet( new long[] { 0x0000000000000002L } );
    public static final BitSet FOLLOW_property_in_filter759 = new BitSet( new long[] { 0x000000001FFF0000L } );
    public static final BitSet FOLLOW_operator_in_filter761 = new BitSet( new long[] { 0x00000000000016A0L } );
    public static final BitSet FOLLOW_value_in_filter763 = new BitSet( new long[] { 0x0000000060000000L } );
    public static final BitSet FOLLOW_set_in_filter766 = new BitSet( new long[] { 0x00000000000016A0L } );
    public static final BitSet FOLLOW_second_value_in_filter774 = new BitSet( new long[] { 0x0000000020000000L } );
    public static final BitSet FOLLOW_29_in_filter778 = new BitSet( new long[] { 0x00000000000016A0L } );
    public static final BitSet FOLLOW_third_value_in_filter780 = new BitSet( new long[] { 0x0000000000000000L } );
    public static final BitSet FOLLOW_EOF_in_filter789 = new BitSet( new long[] { 0x0000000000000002L } );
    public static final BitSet FOLLOW_ID_in_select_subject800 = new BitSet( new long[] { 0x0000000000000002L } );
    public static final BitSet FOLLOW_ID_in_select_assign_target812 = new BitSet( new long[] { 0x0000000000000002L } );
    public static final BitSet FOLLOW_ID_in_select_assign_source823 = new BitSet( new long[] { 0x0000000000000002L } );
    public static final BitSet FOLLOW_select_assign_target_in_select_assign836 =
            new BitSet( new long[] { 0x0000000080000000L } );
    public static final BitSet FOLLOW_31_in_select_assign838 = new BitSet( new long[] { 0x0000000000000010L } );
    public static final BitSet FOLLOW_select_assign_source_in_select_assign840 =
            new BitSet( new long[] { 0x0000000000000002L } );
    public static final BitSet FOLLOW_property_in_where855 = new BitSet( new long[] { 0x000000001FFF0000L } );
    public static final BitSet FOLLOW_operator_in_where857 = new BitSet( new long[] { 0x00000000000016A0L } );
    public static final BitSet FOLLOW_value_in_where859 = new BitSet( new long[] { 0x0000000060000002L } );
    public static final BitSet FOLLOW_set_in_where862 = new BitSet( new long[] { 0x00000000000016A0L } );
    public static final BitSet FOLLOW_second_value_in_where870 = new BitSet( new long[] { 0x0000000020000002L } );
    public static final BitSet FOLLOW_29_in_where874 = new BitSet( new long[] { 0x00000000000016A0L } );
    public static final BitSet FOLLOW_third_value_in_where876 = new BitSet( new long[] { 0x0000000000000002L } );
    public static final BitSet FOLLOW_set_in_direction893 = new BitSet( new long[] { 0x0000000000000002L } );
    public static final BitSet FOLLOW_property_in_order909 = new BitSet( new long[] { 0x0000000300000002L } );
    public static final BitSet FOLLOW_direction_in_order911 = new BitSet( new long[] { 0x0000000000000002L } );
    public static final BitSet FOLLOW_34_in_select_expr925 = new BitSet( new long[] { 0x0000000000000002L } );
    public static final BitSet FOLLOW_select_subject_in_select_expr929 =
            new BitSet( new long[] { 0x0000000020000002L } );
    public static final BitSet FOLLOW_29_in_select_expr932 = new BitSet( new long[] { 0x0000000000000010L } );
    public static final BitSet FOLLOW_select_subject_in_select_expr934 =
            new BitSet( new long[] { 0x0000000020000002L } );
    public static final BitSet FOLLOW_35_in_select_expr941 = new BitSet( new long[] { 0x0000000000000010L } );
    public static final BitSet FOLLOW_select_assign_in_select_expr943 =
            new BitSet( new long[] { 0x0000001020000000L } );
    public static final BitSet FOLLOW_29_in_select_expr946 = new BitSet( new long[] { 0x0000000000000010L } );
    public static final BitSet FOLLOW_select_assign_in_select_expr948 =
            new BitSet( new long[] { 0x0000001020000000L } );
    public static final BitSet FOLLOW_36_in_select_expr953 = new BitSet( new long[] { 0x0000000000000002L } );
    public static final BitSet FOLLOW_37_in_ql970 = new BitSet( new long[] { 0x0000000C00000010L } );
    public static final BitSet FOLLOW_select_expr_in_ql972 = new BitSet( new long[] { 0x0000014000000002L } );
    public static final BitSet FOLLOW_38_in_ql975 = new BitSet( new long[] { 0x0000000000000010L } );
    public static final BitSet FOLLOW_where_in_ql977 = new BitSet( new long[] { 0x0000018000000002L } );
    public static final BitSet FOLLOW_39_in_ql980 = new BitSet( new long[] { 0x0000000000000010L } );
    public static final BitSet FOLLOW_where_in_ql982 = new BitSet( new long[] { 0x0000018000000002L } );
    public static final BitSet FOLLOW_40_in_ql989 = new BitSet( new long[] { 0x0000000000000010L } );
    public static final BitSet FOLLOW_order_in_ql991 = new BitSet( new long[] { 0x0000000020000002L } );
    public static final BitSet FOLLOW_29_in_ql994 = new BitSet( new long[] { 0x0000000000000010L } );
    public static final BitSet FOLLOW_order_in_ql996 = new BitSet( new long[] { 0x0000000020000002L } );
}
