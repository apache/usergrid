// $ANTLR 3.1.3 Mar 17, 2009 19:23:44 org/usergrid/persistence/query/tree/QueryFilter.g 2012-03-15 18:09:38

package org.usergrid.persistence.query.tree;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.Query.FilterPredicate;
import org.usergrid.persistence.Query.SortPredicate;



import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;


import org.antlr.runtime.tree.*;

public class QueryFilterParser extends Parser {
    public static final String[] tokenNames = new String[] {
        "<invalid>", "<EOR>", "<DOWN>", "<UP>", "ID", "INT", "EXPONENT", "FLOAT", "ESC_SEQ", "STRING", "BOOLEAN", "HEX_DIGIT", "UUID", "UNICODE_ESC", "OCTAL_ESC", "WS", "LT", "LTE", "EQ", "GT", "GTE", "'within'", "'of'", "','", "'contains'", "'('", "')'", "'not'", "'and'", "'or'", "'asc'", "'desc'", "':'", "'*'", "'{'", "'}'", "'select'", "'where'", "'order by'"
    };
    public static final int LT=16;
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
    public static final int GTE=20;
    public static final int OCTAL_ESC=14;
    public static final int HEX_DIGIT=11;
    public static final int FLOAT=7;
    public static final int INT=5;
    public static final int ID=4;
    public static final int EOF=-1;
    public static final int LTE=17;
    public static final int T__30=30;
    public static final int T__31=31;
    public static final int T__32=32;
    public static final int WS=15;
    public static final int BOOLEAN=10;
    public static final int ESC_SEQ=8;
    public static final int T__33=33;
    public static final int T__34=34;
    public static final int T__35=35;
    public static final int T__36=36;
    public static final int T__37=37;
    public static final int T__38=38;
    public static final int GT=19;
    public static final int EQ=18;
    public static final int STRING=9;

    // delegates
    // delegators


        public QueryFilterParser(TokenStream input) {
            this(input, new RecognizerSharedState());
        }
        public QueryFilterParser(TokenStream input, RecognizerSharedState state) {
            super(input, state);
             
        }
        
    protected TreeAdaptor adaptor = new CommonTreeAdaptor();

    public void setTreeAdaptor(TreeAdaptor adaptor) {
        this.adaptor = adaptor;
    }
    public TreeAdaptor getTreeAdaptor() {
        return adaptor;
    }

    public String[] getTokenNames() { return QueryFilterParser.tokenNames; }
    public String getGrammarFileName() { return "org/usergrid/persistence/query/tree/QueryFilter.g"; }


    	Query query = new Query();

      private static final Logger logger = LoggerFactory
          .getLogger(QueryFilterLexer.class);

    	@Override
    	public void emitErrorMessage(String msg) {
    		logger.info(msg);
    	}


    public static class property_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "property"
    // org/usergrid/persistence/query/tree/QueryFilter.g:122:1: property : ID ;
    public final QueryFilterParser.property_return property() throws RecognitionException {
        QueryFilterParser.property_return retval = new QueryFilterParser.property_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token ID1=null;

        Object ID1_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:123:2: ( ID )
            // org/usergrid/persistence/query/tree/QueryFilter.g:123:5: ID
            {
            root_0 = (Object)adaptor.nil();

            ID1=(Token)match(input,ID,FOLLOW_ID_in_property679); 
            ID1_tree = new Property(ID1) ;
            adaptor.addChild(root_0, ID1_tree);


            }

            retval.stop = input.LT(-1);

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "property"

    public static class value_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "value"
    // org/usergrid/persistence/query/tree/QueryFilter.g:125:1: value : ( BOOLEAN | STRING | INT | FLOAT | UUID );
    public final QueryFilterParser.value_return value() throws RecognitionException {
        QueryFilterParser.value_return retval = new QueryFilterParser.value_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token set2=null;

        Object set2_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:125:9: ( BOOLEAN | STRING | INT | FLOAT | UUID )
            // org/usergrid/persistence/query/tree/QueryFilter.g:
            {
            root_0 = (Object)adaptor.nil();

            set2=(Token)input.LT(1);
            if ( input.LA(1)==INT||input.LA(1)==FLOAT||(input.LA(1)>=STRING && input.LA(1)<=BOOLEAN)||input.LA(1)==UUID ) {
                input.consume();
                adaptor.addChild(root_0, (Object)adaptor.create(set2));
                state.errorRecovery=false;
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                throw mse;
            }


            }

            retval.stop = input.LT(-1);

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "value"

    public static class lessthan_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "lessthan"
    // org/usergrid/persistence/query/tree/QueryFilter.g:130:1: lessthan : property LT value ;
    public final QueryFilterParser.lessthan_return lessthan() throws RecognitionException {
        QueryFilterParser.lessthan_return retval = new QueryFilterParser.lessthan_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token LT4=null;
        QueryFilterParser.property_return property3 = null;

        QueryFilterParser.value_return value5 = null;


        Object LT4_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:130:9: ( property LT value )
            // org/usergrid/persistence/query/tree/QueryFilter.g:131:3: property LT value
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_property_in_lessthan737);
            property3=property();

            state._fsp--;

            adaptor.addChild(root_0, property3.getTree());
            LT4=(Token)match(input,LT,FOLLOW_LT_in_lessthan739); 
            LT4_tree = new LessThan(LT4) ;
            root_0 = (Object)adaptor.becomeRoot(LT4_tree, root_0);

            pushFollow(FOLLOW_value_in_lessthan745);
            value5=value();

            state._fsp--;

            adaptor.addChild(root_0, value5.getTree());

            }

            retval.stop = input.LT(-1);

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "lessthan"

    public static class equalityop_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "equalityop"
    // org/usergrid/persistence/query/tree/QueryFilter.g:133:1: equalityop : lessthan ;
    public final QueryFilterParser.equalityop_return equalityop() throws RecognitionException {
        QueryFilterParser.equalityop_return retval = new QueryFilterParser.equalityop_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        QueryFilterParser.lessthan_return lessthan6 = null;



        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:133:12: ( lessthan )
            // org/usergrid/persistence/query/tree/QueryFilter.g:134:3: lessthan
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_lessthan_in_equalityop757);
            lessthan6=lessthan();

            state._fsp--;

            adaptor.addChild(root_0, lessthan6.getTree());

            }

            retval.stop = input.LT(-1);

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "equalityop"

    public static class locationop_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "locationop"
    // org/usergrid/persistence/query/tree/QueryFilter.g:143:1: locationop : property 'within' FLOAT 'of' FLOAT ',' FLOAT ;
    public final QueryFilterParser.locationop_return locationop() throws RecognitionException {
        QueryFilterParser.locationop_return retval = new QueryFilterParser.locationop_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token string_literal8=null;
        Token FLOAT9=null;
        Token string_literal10=null;
        Token FLOAT11=null;
        Token char_literal12=null;
        Token FLOAT13=null;
        QueryFilterParser.property_return property7 = null;


        Object string_literal8_tree=null;
        Object FLOAT9_tree=null;
        Object string_literal10_tree=null;
        Object FLOAT11_tree=null;
        Object char_literal12_tree=null;
        Object FLOAT13_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:143:11: ( property 'within' FLOAT 'of' FLOAT ',' FLOAT )
            // org/usergrid/persistence/query/tree/QueryFilter.g:144:3: property 'within' FLOAT 'of' FLOAT ',' FLOAT
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_property_in_locationop773);
            property7=property();

            state._fsp--;

            adaptor.addChild(root_0, property7.getTree());
            string_literal8=(Token)match(input,21,FOLLOW_21_in_locationop775); 
            string_literal8_tree = new Within(string_literal8) ;
            root_0 = (Object)adaptor.becomeRoot(string_literal8_tree, root_0);

            FLOAT9=(Token)match(input,FLOAT,FOLLOW_FLOAT_in_locationop781); 
            FLOAT9_tree = (Object)adaptor.create(FLOAT9);
            adaptor.addChild(root_0, FLOAT9_tree);

            string_literal10=(Token)match(input,22,FOLLOW_22_in_locationop783); 
            string_literal10_tree = (Object)adaptor.create(string_literal10);
            adaptor.addChild(root_0, string_literal10_tree);

            FLOAT11=(Token)match(input,FLOAT,FOLLOW_FLOAT_in_locationop785); 
            FLOAT11_tree = (Object)adaptor.create(FLOAT11);
            adaptor.addChild(root_0, FLOAT11_tree);

            char_literal12=(Token)match(input,23,FOLLOW_23_in_locationop787); 
            char_literal12_tree = (Object)adaptor.create(char_literal12);
            adaptor.addChild(root_0, char_literal12_tree);

            FLOAT13=(Token)match(input,FLOAT,FOLLOW_FLOAT_in_locationop789); 
            FLOAT13_tree = (Object)adaptor.create(FLOAT13);
            adaptor.addChild(root_0, FLOAT13_tree);


            }

            retval.stop = input.LT(-1);

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "locationop"

    public static class containsop_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "containsop"
    // org/usergrid/persistence/query/tree/QueryFilter.g:147:1: containsop : property 'contains' STRING ;
    public final QueryFilterParser.containsop_return containsop() throws RecognitionException {
        QueryFilterParser.containsop_return retval = new QueryFilterParser.containsop_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token string_literal15=null;
        Token STRING16=null;
        QueryFilterParser.property_return property14 = null;


        Object string_literal15_tree=null;
        Object STRING16_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:147:12: ( property 'contains' STRING )
            // org/usergrid/persistence/query/tree/QueryFilter.g:148:3: property 'contains' STRING
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_property_in_containsop804);
            property14=property();

            state._fsp--;

            adaptor.addChild(root_0, property14.getTree());
            string_literal15=(Token)match(input,24,FOLLOW_24_in_containsop806); 
            string_literal15_tree = new Contains(string_literal15) ;
            root_0 = (Object)adaptor.becomeRoot(string_literal15_tree, root_0);

            STRING16=(Token)match(input,STRING,FOLLOW_STRING_in_containsop812); 
            STRING16_tree = (Object)adaptor.create(STRING16);
            adaptor.addChild(root_0, STRING16_tree);


            }

            retval.stop = input.LT(-1);

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "containsop"

    public static class operation_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "operation"
    // org/usergrid/persistence/query/tree/QueryFilter.g:151:1: operation : ( '(' expression ')' | equalityop | locationop | containsop );
    public final QueryFilterParser.operation_return operation() throws RecognitionException {
        QueryFilterParser.operation_return retval = new QueryFilterParser.operation_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token char_literal17=null;
        Token char_literal19=null;
        QueryFilterParser.expression_return expression18 = null;

        QueryFilterParser.equalityop_return equalityop20 = null;

        QueryFilterParser.locationop_return locationop21 = null;

        QueryFilterParser.containsop_return containsop22 = null;


        Object char_literal17_tree=null;
        Object char_literal19_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:151:11: ( '(' expression ')' | equalityop | locationop | containsop )
            int alt1=4;
            int LA1_0 = input.LA(1);

            if ( (LA1_0==25) ) {
                alt1=1;
            }
            else if ( (LA1_0==ID) ) {
                switch ( input.LA(2) ) {
                case LT:
                    {
                    alt1=2;
                    }
                    break;
                case 24:
                    {
                    alt1=4;
                    }
                    break;
                case 21:
                    {
                    alt1=3;
                    }
                    break;
                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 1, 2, input);

                    throw nvae;
                }

            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 1, 0, input);

                throw nvae;
            }
            switch (alt1) {
                case 1 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:152:2: '(' expression ')'
                    {
                    root_0 = (Object)adaptor.nil();

                    char_literal17=(Token)match(input,25,FOLLOW_25_in_operation825); 
                    pushFollow(FOLLOW_expression_in_operation828);
                    expression18=expression();

                    state._fsp--;

                    adaptor.addChild(root_0, expression18.getTree());
                    char_literal19=(Token)match(input,26,FOLLOW_26_in_operation830); 

                    }
                    break;
                case 2 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:152:25: equalityop
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_equalityop_in_operation835);
                    equalityop20=equalityop();

                    state._fsp--;

                    adaptor.addChild(root_0, equalityop20.getTree());

                    }
                    break;
                case 3 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:152:38: locationop
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_locationop_in_operation839);
                    locationop21=locationop();

                    state._fsp--;

                    adaptor.addChild(root_0, locationop21.getTree());

                    }
                    break;
                case 4 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:152:51: containsop
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_containsop_in_operation843);
                    containsop22=containsop();

                    state._fsp--;

                    adaptor.addChild(root_0, containsop22.getTree());

                    }
                    break;

            }
            retval.stop = input.LT(-1);

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "operation"

    public static class notexp_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "notexp"
    // org/usergrid/persistence/query/tree/QueryFilter.g:155:1: notexp : ( 'not' operation | operation );
    public final QueryFilterParser.notexp_return notexp() throws RecognitionException {
        QueryFilterParser.notexp_return retval = new QueryFilterParser.notexp_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token string_literal23=null;
        QueryFilterParser.operation_return operation24 = null;

        QueryFilterParser.operation_return operation25 = null;


        Object string_literal23_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:155:8: ( 'not' operation | operation )
            int alt2=2;
            int LA2_0 = input.LA(1);

            if ( (LA2_0==27) ) {
                alt2=1;
            }
            else if ( (LA2_0==ID||LA2_0==25) ) {
                alt2=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 2, 0, input);

                throw nvae;
            }
            switch (alt2) {
                case 1 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:156:2: 'not' operation
                    {
                    root_0 = (Object)adaptor.nil();

                    string_literal23=(Token)match(input,27,FOLLOW_27_in_notexp853); 
                    string_literal23_tree = (Object)adaptor.create(string_literal23);
                    root_0 = (Object)adaptor.becomeRoot(string_literal23_tree, root_0);

                    pushFollow(FOLLOW_operation_in_notexp856);
                    operation24=operation();

                    state._fsp--;

                    adaptor.addChild(root_0, operation24.getTree());

                    }
                    break;
                case 2 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:156:19: operation
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_operation_in_notexp858);
                    operation25=operation();

                    state._fsp--;

                    adaptor.addChild(root_0, operation25.getTree());

                    }
                    break;

            }
            retval.stop = input.LT(-1);

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "notexp"

    public static class andexp_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "andexp"
    // org/usergrid/persistence/query/tree/QueryFilter.g:161:1: andexp : notexp ( 'and' notexp )* ;
    public final QueryFilterParser.andexp_return andexp() throws RecognitionException {
        QueryFilterParser.andexp_return retval = new QueryFilterParser.andexp_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token string_literal27=null;
        QueryFilterParser.notexp_return notexp26 = null;

        QueryFilterParser.notexp_return notexp28 = null;


        Object string_literal27_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:161:8: ( notexp ( 'and' notexp )* )
            // org/usergrid/persistence/query/tree/QueryFilter.g:162:2: notexp ( 'and' notexp )*
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_notexp_in_andexp874);
            notexp26=notexp();

            state._fsp--;

            adaptor.addChild(root_0, notexp26.getTree());
            // org/usergrid/persistence/query/tree/QueryFilter.g:162:9: ( 'and' notexp )*
            loop3:
            do {
                int alt3=2;
                int LA3_0 = input.LA(1);

                if ( (LA3_0==28) ) {
                    alt3=1;
                }


                switch (alt3) {
            	case 1 :
            	    // org/usergrid/persistence/query/tree/QueryFilter.g:162:10: 'and' notexp
            	    {
            	    string_literal27=(Token)match(input,28,FOLLOW_28_in_andexp877); 
            	    string_literal27_tree = (Object)adaptor.create(string_literal27);
            	    root_0 = (Object)adaptor.becomeRoot(string_literal27_tree, root_0);

            	    pushFollow(FOLLOW_notexp_in_andexp880);
            	    notexp28=notexp();

            	    state._fsp--;

            	    adaptor.addChild(root_0, notexp28.getTree());

            	    }
            	    break;

            	default :
            	    break loop3;
                }
            } while (true);


            }

            retval.stop = input.LT(-1);

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "andexp"

    public static class orexp_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "orexp"
    // org/usergrid/persistence/query/tree/QueryFilter.g:165:1: orexp : andexp ( 'or' andexp )* ;
    public final QueryFilterParser.orexp_return orexp() throws RecognitionException {
        QueryFilterParser.orexp_return retval = new QueryFilterParser.orexp_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token string_literal30=null;
        QueryFilterParser.andexp_return andexp29 = null;

        QueryFilterParser.andexp_return andexp31 = null;


        Object string_literal30_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:165:7: ( andexp ( 'or' andexp )* )
            // org/usergrid/persistence/query/tree/QueryFilter.g:166:2: andexp ( 'or' andexp )*
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_andexp_in_orexp893);
            andexp29=andexp();

            state._fsp--;

            adaptor.addChild(root_0, andexp29.getTree());
            // org/usergrid/persistence/query/tree/QueryFilter.g:166:9: ( 'or' andexp )*
            loop4:
            do {
                int alt4=2;
                int LA4_0 = input.LA(1);

                if ( (LA4_0==29) ) {
                    alt4=1;
                }


                switch (alt4) {
            	case 1 :
            	    // org/usergrid/persistence/query/tree/QueryFilter.g:166:10: 'or' andexp
            	    {
            	    string_literal30=(Token)match(input,29,FOLLOW_29_in_orexp896); 
            	    string_literal30_tree = (Object)adaptor.create(string_literal30);
            	    root_0 = (Object)adaptor.becomeRoot(string_literal30_tree, root_0);

            	    pushFollow(FOLLOW_andexp_in_orexp899);
            	    andexp31=andexp();

            	    state._fsp--;

            	    adaptor.addChild(root_0, andexp31.getTree());

            	    }
            	    break;

            	default :
            	    break loop4;
                }
            } while (true);


            }

            retval.stop = input.LT(-1);

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "orexp"

    public static class expression_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "expression"
    // org/usergrid/persistence/query/tree/QueryFilter.g:169:1: expression : orexp ;
    public final QueryFilterParser.expression_return expression() throws RecognitionException {
        QueryFilterParser.expression_return retval = new QueryFilterParser.expression_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        QueryFilterParser.orexp_return orexp32 = null;



        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:169:11: ( orexp )
            // org/usergrid/persistence/query/tree/QueryFilter.g:170:3: orexp
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_orexp_in_expression911);
            orexp32=orexp();

            state._fsp--;

            adaptor.addChild(root_0, orexp32.getTree());

            }

            retval.stop = input.LT(-1);

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "expression"

    public static class direction_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "direction"
    // org/usergrid/persistence/query/tree/QueryFilter.g:177:1: direction : ( 'asc' | 'desc' ) ;
    public final QueryFilterParser.direction_return direction() throws RecognitionException {
        QueryFilterParser.direction_return retval = new QueryFilterParser.direction_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token set33=null;

        Object set33_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:177:12: ( ( 'asc' | 'desc' ) )
            // org/usergrid/persistence/query/tree/QueryFilter.g:177:14: ( 'asc' | 'desc' )
            {
            root_0 = (Object)adaptor.nil();

            set33=(Token)input.LT(1);
            if ( (input.LA(1)>=30 && input.LA(1)<=31) ) {
                input.consume();
                adaptor.addChild(root_0, (Object)adaptor.create(set33));
                state.errorRecovery=false;
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                throw mse;
            }


            }

            retval.stop = input.LT(-1);

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "direction"

    public static class order_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "order"
    // org/usergrid/persistence/query/tree/QueryFilter.g:180:1: order : ( property ( direction )? ) ;
    public final QueryFilterParser.order_return order() throws RecognitionException {
        QueryFilterParser.order_return retval = new QueryFilterParser.order_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        QueryFilterParser.property_return property34 = null;

        QueryFilterParser.direction_return direction35 = null;



        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:181:3: ( ( property ( direction )? ) )
            // org/usergrid/persistence/query/tree/QueryFilter.g:181:5: ( property ( direction )? )
            {
            root_0 = (Object)adaptor.nil();

            // org/usergrid/persistence/query/tree/QueryFilter.g:181:5: ( property ( direction )? )
            // org/usergrid/persistence/query/tree/QueryFilter.g:181:6: property ( direction )?
            {
            pushFollow(FOLLOW_property_in_order943);
            property34=property();

            state._fsp--;

            adaptor.addChild(root_0, property34.getTree());
            // org/usergrid/persistence/query/tree/QueryFilter.g:181:15: ( direction )?
            int alt5=2;
            int LA5_0 = input.LA(1);

            if ( ((LA5_0>=30 && LA5_0<=31)) ) {
                alt5=1;
            }
            switch (alt5) {
                case 1 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:181:15: direction
                    {
                    pushFollow(FOLLOW_direction_in_order945);
                    direction35=direction();

                    state._fsp--;

                    adaptor.addChild(root_0, direction35.getTree());

                    }
                    break;

            }


            }


            }

            retval.stop = input.LT(-1);

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "order"

    public static class select_subject_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "select_subject"
    // org/usergrid/persistence/query/tree/QueryFilter.g:187:1: select_subject : ID ;
    public final QueryFilterParser.select_subject_return select_subject() throws RecognitionException {
        QueryFilterParser.select_subject_return retval = new QueryFilterParser.select_subject_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token ID36=null;

        Object ID36_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:188:3: ( ID )
            // org/usergrid/persistence/query/tree/QueryFilter.g:188:5: ID
            {
            root_0 = (Object)adaptor.nil();

            ID36=(Token)match(input,ID,FOLLOW_ID_in_select_subject963); 
            ID36_tree = (Object)adaptor.create(ID36);
            adaptor.addChild(root_0, ID36_tree);



              query.addSelect(input.toString(retval.start,input.LT(-1)));



            }

            retval.stop = input.LT(-1);

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "select_subject"

    public static class select_assign_target_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "select_assign_target"
    // org/usergrid/persistence/query/tree/QueryFilter.g:194:1: select_assign_target : ID ;
    public final QueryFilterParser.select_assign_target_return select_assign_target() throws RecognitionException {
        QueryFilterParser.select_assign_target_return retval = new QueryFilterParser.select_assign_target_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token ID37=null;

        Object ID37_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:195:3: ( ID )
            // org/usergrid/persistence/query/tree/QueryFilter.g:195:5: ID
            {
            root_0 = (Object)adaptor.nil();

            ID37=(Token)match(input,ID,FOLLOW_ID_in_select_assign_target976); 
            ID37_tree = (Object)adaptor.create(ID37);
            adaptor.addChild(root_0, ID37_tree);


            }

            retval.stop = input.LT(-1);

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "select_assign_target"

    public static class select_assign_source_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "select_assign_source"
    // org/usergrid/persistence/query/tree/QueryFilter.g:197:1: select_assign_source : ID ;
    public final QueryFilterParser.select_assign_source_return select_assign_source() throws RecognitionException {
        QueryFilterParser.select_assign_source_return retval = new QueryFilterParser.select_assign_source_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token ID38=null;

        Object ID38_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:198:3: ( ID )
            // org/usergrid/persistence/query/tree/QueryFilter.g:198:5: ID
            {
            root_0 = (Object)adaptor.nil();

            ID38=(Token)match(input,ID,FOLLOW_ID_in_select_assign_source989); 
            ID38_tree = (Object)adaptor.create(ID38);
            adaptor.addChild(root_0, ID38_tree);


            }

            retval.stop = input.LT(-1);

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "select_assign_source"

    public static class select_assign_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "select_assign"
    // org/usergrid/persistence/query/tree/QueryFilter.g:200:1: select_assign : select_assign_target ':' select_assign_source ;
    public final QueryFilterParser.select_assign_return select_assign() throws RecognitionException {
        QueryFilterParser.select_assign_return retval = new QueryFilterParser.select_assign_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token char_literal40=null;
        QueryFilterParser.select_assign_target_return select_assign_target39 = null;

        QueryFilterParser.select_assign_source_return select_assign_source41 = null;


        Object char_literal40_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:201:3: ( select_assign_target ':' select_assign_source )
            // org/usergrid/persistence/query/tree/QueryFilter.g:201:5: select_assign_target ':' select_assign_source
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_select_assign_target_in_select_assign1003);
            select_assign_target39=select_assign_target();

            state._fsp--;

            adaptor.addChild(root_0, select_assign_target39.getTree());
            char_literal40=(Token)match(input,32,FOLLOW_32_in_select_assign1005); 
            char_literal40_tree = (Object)adaptor.create(char_literal40);
            adaptor.addChild(root_0, char_literal40_tree);

            pushFollow(FOLLOW_select_assign_source_in_select_assign1007);
            select_assign_source41=select_assign_source();

            state._fsp--;

            adaptor.addChild(root_0, select_assign_source41.getTree());


              query.addSelect((select_assign_source41!=null?input.toString(select_assign_source41.start,select_assign_source41.stop):null), (select_assign_target39!=null?input.toString(select_assign_target39.start,select_assign_target39.stop):null));



            }

            retval.stop = input.LT(-1);

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "select_assign"

    public static class select_expr_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "select_expr"
    // org/usergrid/persistence/query/tree/QueryFilter.g:207:1: select_expr : ( '*' | select_subject ( ',' select_subject )* | '{' select_assign ( ',' select_assign )* '}' ) ;
    public final QueryFilterParser.select_expr_return select_expr() throws RecognitionException {
        QueryFilterParser.select_expr_return retval = new QueryFilterParser.select_expr_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token char_literal42=null;
        Token char_literal44=null;
        Token char_literal46=null;
        Token char_literal48=null;
        Token char_literal50=null;
        QueryFilterParser.select_subject_return select_subject43 = null;

        QueryFilterParser.select_subject_return select_subject45 = null;

        QueryFilterParser.select_assign_return select_assign47 = null;

        QueryFilterParser.select_assign_return select_assign49 = null;


        Object char_literal42_tree=null;
        Object char_literal44_tree=null;
        Object char_literal46_tree=null;
        Object char_literal48_tree=null;
        Object char_literal50_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:208:3: ( ( '*' | select_subject ( ',' select_subject )* | '{' select_assign ( ',' select_assign )* '}' ) )
            // org/usergrid/persistence/query/tree/QueryFilter.g:208:5: ( '*' | select_subject ( ',' select_subject )* | '{' select_assign ( ',' select_assign )* '}' )
            {
            root_0 = (Object)adaptor.nil();

            // org/usergrid/persistence/query/tree/QueryFilter.g:208:5: ( '*' | select_subject ( ',' select_subject )* | '{' select_assign ( ',' select_assign )* '}' )
            int alt8=3;
            switch ( input.LA(1) ) {
            case 33:
                {
                alt8=1;
                }
                break;
            case ID:
                {
                alt8=2;
                }
                break;
            case 34:
                {
                alt8=3;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 8, 0, input);

                throw nvae;
            }

            switch (alt8) {
                case 1 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:208:6: '*'
                    {
                    char_literal42=(Token)match(input,33,FOLLOW_33_in_select_expr1021); 
                    char_literal42_tree = (Object)adaptor.create(char_literal42);
                    adaptor.addChild(root_0, char_literal42_tree);


                    }
                    break;
                case 2 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:208:12: select_subject ( ',' select_subject )*
                    {
                    pushFollow(FOLLOW_select_subject_in_select_expr1025);
                    select_subject43=select_subject();

                    state._fsp--;

                    adaptor.addChild(root_0, select_subject43.getTree());
                    // org/usergrid/persistence/query/tree/QueryFilter.g:208:27: ( ',' select_subject )*
                    loop6:
                    do {
                        int alt6=2;
                        int LA6_0 = input.LA(1);

                        if ( (LA6_0==23) ) {
                            alt6=1;
                        }


                        switch (alt6) {
                    	case 1 :
                    	    // org/usergrid/persistence/query/tree/QueryFilter.g:208:28: ',' select_subject
                    	    {
                    	    char_literal44=(Token)match(input,23,FOLLOW_23_in_select_expr1028); 
                    	    char_literal44_tree = (Object)adaptor.create(char_literal44);
                    	    adaptor.addChild(root_0, char_literal44_tree);

                    	    pushFollow(FOLLOW_select_subject_in_select_expr1030);
                    	    select_subject45=select_subject();

                    	    state._fsp--;

                    	    adaptor.addChild(root_0, select_subject45.getTree());

                    	    }
                    	    break;

                    	default :
                    	    break loop6;
                        }
                    } while (true);


                    }
                    break;
                case 3 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:208:52: '{' select_assign ( ',' select_assign )* '}'
                    {
                    char_literal46=(Token)match(input,34,FOLLOW_34_in_select_expr1037); 
                    char_literal46_tree = (Object)adaptor.create(char_literal46);
                    adaptor.addChild(root_0, char_literal46_tree);

                    pushFollow(FOLLOW_select_assign_in_select_expr1039);
                    select_assign47=select_assign();

                    state._fsp--;

                    adaptor.addChild(root_0, select_assign47.getTree());
                    // org/usergrid/persistence/query/tree/QueryFilter.g:208:70: ( ',' select_assign )*
                    loop7:
                    do {
                        int alt7=2;
                        int LA7_0 = input.LA(1);

                        if ( (LA7_0==23) ) {
                            alt7=1;
                        }


                        switch (alt7) {
                    	case 1 :
                    	    // org/usergrid/persistence/query/tree/QueryFilter.g:208:71: ',' select_assign
                    	    {
                    	    char_literal48=(Token)match(input,23,FOLLOW_23_in_select_expr1042); 
                    	    char_literal48_tree = (Object)adaptor.create(char_literal48);
                    	    adaptor.addChild(root_0, char_literal48_tree);

                    	    pushFollow(FOLLOW_select_assign_in_select_expr1044);
                    	    select_assign49=select_assign();

                    	    state._fsp--;

                    	    adaptor.addChild(root_0, select_assign49.getTree());

                    	    }
                    	    break;

                    	default :
                    	    break loop7;
                        }
                    } while (true);

                    char_literal50=(Token)match(input,35,FOLLOW_35_in_select_expr1049); 
                    char_literal50_tree = (Object)adaptor.create(char_literal50);
                    adaptor.addChild(root_0, char_literal50_tree);


                    }
                    break;

            }


            }

            retval.stop = input.LT(-1);

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "select_expr"

    public static class ql_return extends ParserRuleReturnScope {
        public Query q;
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "ql"
    // org/usergrid/persistence/query/tree/QueryFilter.g:212:1: ql returns [Query q] : 'select' select_expr ( 'where' expression )? ( 'order by' order ( ',' order )* )? ;
    public final QueryFilterParser.ql_return ql() throws RecognitionException {
        QueryFilterParser.ql_return retval = new QueryFilterParser.ql_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token string_literal51=null;
        Token string_literal53=null;
        Token string_literal55=null;
        Token char_literal57=null;
        QueryFilterParser.select_expr_return select_expr52 = null;

        QueryFilterParser.expression_return expression54 = null;

        QueryFilterParser.order_return order56 = null;

        QueryFilterParser.order_return order58 = null;


        Object string_literal51_tree=null;
        Object string_literal53_tree=null;
        Object string_literal55_tree=null;
        Object char_literal57_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:213:3: ( 'select' select_expr ( 'where' expression )? ( 'order by' order ( ',' order )* )? )
            // org/usergrid/persistence/query/tree/QueryFilter.g:213:5: 'select' select_expr ( 'where' expression )? ( 'order by' order ( ',' order )* )?
            {
            root_0 = (Object)adaptor.nil();

            string_literal51=(Token)match(input,36,FOLLOW_36_in_ql1071); 
            string_literal51_tree = (Object)adaptor.create(string_literal51);
            adaptor.addChild(root_0, string_literal51_tree);

            pushFollow(FOLLOW_select_expr_in_ql1073);
            select_expr52=select_expr();

            state._fsp--;

            adaptor.addChild(root_0, select_expr52.getTree());
            // org/usergrid/persistence/query/tree/QueryFilter.g:213:26: ( 'where' expression )?
            int alt9=2;
            int LA9_0 = input.LA(1);

            if ( (LA9_0==37) ) {
                alt9=1;
            }
            switch (alt9) {
                case 1 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:213:27: 'where' expression
                    {
                    string_literal53=(Token)match(input,37,FOLLOW_37_in_ql1076); 
                    string_literal53_tree = (Object)adaptor.create(string_literal53);
                    adaptor.addChild(root_0, string_literal53_tree);

                    pushFollow(FOLLOW_expression_in_ql1078);
                    expression54=expression();

                    state._fsp--;

                    adaptor.addChild(root_0, expression54.getTree());

                    }
                    break;

            }

            // org/usergrid/persistence/query/tree/QueryFilter.g:213:49: ( 'order by' order ( ',' order )* )?
            int alt11=2;
            int LA11_0 = input.LA(1);

            if ( (LA11_0==38) ) {
                alt11=1;
            }
            switch (alt11) {
                case 1 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:213:50: 'order by' order ( ',' order )*
                    {
                    string_literal55=(Token)match(input,38,FOLLOW_38_in_ql1084); 
                    string_literal55_tree = (Object)adaptor.create(string_literal55);
                    adaptor.addChild(root_0, string_literal55_tree);

                    pushFollow(FOLLOW_order_in_ql1086);
                    order56=order();

                    state._fsp--;

                    adaptor.addChild(root_0, order56.getTree());
                    // org/usergrid/persistence/query/tree/QueryFilter.g:213:67: ( ',' order )*
                    loop10:
                    do {
                        int alt10=2;
                        int LA10_0 = input.LA(1);

                        if ( (LA10_0==23) ) {
                            alt10=1;
                        }


                        switch (alt10) {
                    	case 1 :
                    	    // org/usergrid/persistence/query/tree/QueryFilter.g:213:68: ',' order
                    	    {
                    	    char_literal57=(Token)match(input,23,FOLLOW_23_in_ql1089); 
                    	    char_literal57_tree = (Object)adaptor.create(char_literal57);
                    	    adaptor.addChild(root_0, char_literal57_tree);

                    	    pushFollow(FOLLOW_order_in_ql1091);
                    	    order58=order();

                    	    state._fsp--;

                    	    adaptor.addChild(root_0, order58.getTree());

                    	    }
                    	    break;

                    	default :
                    	    break loop10;
                        }
                    } while (true);


                    }
                    break;

            }



            //q = query;

            //TODO other stuff


            }

            retval.stop = input.LT(-1);

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "ql"

    // Delegated rules


 

    public static final BitSet FOLLOW_ID_in_property679 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_set_in_value0 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_property_in_lessthan737 = new BitSet(new long[]{0x0000000000010000L});
    public static final BitSet FOLLOW_LT_in_lessthan739 = new BitSet(new long[]{0x00000000000016A0L});
    public static final BitSet FOLLOW_value_in_lessthan745 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_lessthan_in_equalityop757 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_property_in_locationop773 = new BitSet(new long[]{0x0000000000200000L});
    public static final BitSet FOLLOW_21_in_locationop775 = new BitSet(new long[]{0x0000000000000080L});
    public static final BitSet FOLLOW_FLOAT_in_locationop781 = new BitSet(new long[]{0x0000000000400000L});
    public static final BitSet FOLLOW_22_in_locationop783 = new BitSet(new long[]{0x0000000000000080L});
    public static final BitSet FOLLOW_FLOAT_in_locationop785 = new BitSet(new long[]{0x0000000000800000L});
    public static final BitSet FOLLOW_23_in_locationop787 = new BitSet(new long[]{0x0000000000000080L});
    public static final BitSet FOLLOW_FLOAT_in_locationop789 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_property_in_containsop804 = new BitSet(new long[]{0x0000000001000000L});
    public static final BitSet FOLLOW_24_in_containsop806 = new BitSet(new long[]{0x0000000000000200L});
    public static final BitSet FOLLOW_STRING_in_containsop812 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_25_in_operation825 = new BitSet(new long[]{0x000000000A000010L});
    public static final BitSet FOLLOW_expression_in_operation828 = new BitSet(new long[]{0x0000000004000000L});
    public static final BitSet FOLLOW_26_in_operation830 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_equalityop_in_operation835 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_locationop_in_operation839 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_containsop_in_operation843 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_27_in_notexp853 = new BitSet(new long[]{0x000000000A000010L});
    public static final BitSet FOLLOW_operation_in_notexp856 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_operation_in_notexp858 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_notexp_in_andexp874 = new BitSet(new long[]{0x0000000010000002L});
    public static final BitSet FOLLOW_28_in_andexp877 = new BitSet(new long[]{0x000000000A000010L});
    public static final BitSet FOLLOW_notexp_in_andexp880 = new BitSet(new long[]{0x0000000010000002L});
    public static final BitSet FOLLOW_andexp_in_orexp893 = new BitSet(new long[]{0x0000000020000002L});
    public static final BitSet FOLLOW_29_in_orexp896 = new BitSet(new long[]{0x000000000A000010L});
    public static final BitSet FOLLOW_andexp_in_orexp899 = new BitSet(new long[]{0x0000000020000002L});
    public static final BitSet FOLLOW_orexp_in_expression911 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_set_in_direction925 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_property_in_order943 = new BitSet(new long[]{0x00000000C0000002L});
    public static final BitSet FOLLOW_direction_in_order945 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_select_subject963 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_select_assign_target976 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_select_assign_source989 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_select_assign_target_in_select_assign1003 = new BitSet(new long[]{0x0000000100000000L});
    public static final BitSet FOLLOW_32_in_select_assign1005 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_select_assign_source_in_select_assign1007 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_33_in_select_expr1021 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_select_subject_in_select_expr1025 = new BitSet(new long[]{0x0000000000800002L});
    public static final BitSet FOLLOW_23_in_select_expr1028 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_select_subject_in_select_expr1030 = new BitSet(new long[]{0x0000000000800002L});
    public static final BitSet FOLLOW_34_in_select_expr1037 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_select_assign_in_select_expr1039 = new BitSet(new long[]{0x0000000800800000L});
    public static final BitSet FOLLOW_23_in_select_expr1042 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_select_assign_in_select_expr1044 = new BitSet(new long[]{0x0000000800800000L});
    public static final BitSet FOLLOW_35_in_select_expr1049 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_36_in_ql1071 = new BitSet(new long[]{0x0000000600000010L});
    public static final BitSet FOLLOW_select_expr_in_ql1073 = new BitSet(new long[]{0x0000006000000002L});
    public static final BitSet FOLLOW_37_in_ql1076 = new BitSet(new long[]{0x000000000A000010L});
    public static final BitSet FOLLOW_expression_in_ql1078 = new BitSet(new long[]{0x0000004000000002L});
    public static final BitSet FOLLOW_38_in_ql1084 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_order_in_ql1086 = new BitSet(new long[]{0x0000000000800002L});
    public static final BitSet FOLLOW_23_in_ql1089 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_order_in_ql1091 = new BitSet(new long[]{0x0000000000800002L});

}