// $ANTLR 3.1.3 Mar 17, 2009 19:23:44 org/usergrid/persistence/query/tree/QueryFilter.g 2012-03-15 17:01:25

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

    public static class equalityop_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "equalityop"
    // org/usergrid/persistence/query/tree/QueryFilter.g:130:1: equalityop : property ( LT | LTE | EQ | GT | GTE ) value ;
    public final QueryFilterParser.equalityop_return equalityop() throws RecognitionException {
        QueryFilterParser.equalityop_return retval = new QueryFilterParser.equalityop_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token LT4=null;
        Token LTE5=null;
        Token EQ6=null;
        Token GT7=null;
        Token GTE8=null;
        QueryFilterParser.property_return property3 = null;

        QueryFilterParser.value_return value9 = null;


        Object LT4_tree=null;
        Object LTE5_tree=null;
        Object EQ6_tree=null;
        Object GT7_tree=null;
        Object GTE8_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:130:12: ( property ( LT | LTE | EQ | GT | GTE ) value )
            // org/usergrid/persistence/query/tree/QueryFilter.g:131:3: property ( LT | LTE | EQ | GT | GTE ) value
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_property_in_equalityop739);
            property3=property();

            state._fsp--;

            adaptor.addChild(root_0, property3.getTree());
            // org/usergrid/persistence/query/tree/QueryFilter.g:131:12: ( LT | LTE | EQ | GT | GTE )
            int alt1=5;
            switch ( input.LA(1) ) {
            case LT:
                {
                alt1=1;
                }
                break;
            case LTE:
                {
                alt1=2;
                }
                break;
            case EQ:
                {
                alt1=3;
                }
                break;
            case GT:
                {
                alt1=4;
                }
                break;
            case GTE:
                {
                alt1=5;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 1, 0, input);

                throw nvae;
            }

            switch (alt1) {
                case 1 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:131:14: LT
                    {
                    LT4=(Token)match(input,LT,FOLLOW_LT_in_equalityop743); 
                    LT4_tree = new LessThan(LT4) ;
                    root_0 = (Object)adaptor.becomeRoot(LT4_tree, root_0);


                    }
                    break;
                case 2 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:132:4: LTE
                    {
                    LTE5=(Token)match(input,LTE,FOLLOW_LTE_in_equalityop752); 
                    LTE5_tree = new LessThanEqual(LTE5) ;
                    root_0 = (Object)adaptor.becomeRoot(LTE5_tree, root_0);


                    }
                    break;
                case 3 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:133:4: EQ
                    {
                    EQ6=(Token)match(input,EQ,FOLLOW_EQ_in_equalityop762); 
                    EQ6_tree = new Equal(EQ6) ;
                    root_0 = (Object)adaptor.becomeRoot(EQ6_tree, root_0);


                    }
                    break;
                case 4 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:134:4: GT
                    {
                    GT7=(Token)match(input,GT,FOLLOW_GT_in_equalityop772); 
                    GT7_tree = new GreaterThan(GT7) ;
                    root_0 = (Object)adaptor.becomeRoot(GT7_tree, root_0);


                    }
                    break;
                case 5 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:135:4: GTE
                    {
                    GTE8=(Token)match(input,GTE,FOLLOW_GTE_in_equalityop782); 
                    GTE8_tree = new GreaterThanEqual(GTE8) ;
                    root_0 = (Object)adaptor.becomeRoot(GTE8_tree, root_0);


                    }
                    break;

            }

            pushFollow(FOLLOW_value_in_equalityop790);
            value9=value();

            state._fsp--;

            adaptor.addChild(root_0, value9.getTree());

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
    // org/usergrid/persistence/query/tree/QueryFilter.g:139:1: locationop : property 'within' FLOAT 'of' FLOAT ',' FLOAT ;
    public final QueryFilterParser.locationop_return locationop() throws RecognitionException {
        QueryFilterParser.locationop_return retval = new QueryFilterParser.locationop_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token string_literal11=null;
        Token FLOAT12=null;
        Token string_literal13=null;
        Token FLOAT14=null;
        Token char_literal15=null;
        Token FLOAT16=null;
        QueryFilterParser.property_return property10 = null;


        Object string_literal11_tree=null;
        Object FLOAT12_tree=null;
        Object string_literal13_tree=null;
        Object FLOAT14_tree=null;
        Object char_literal15_tree=null;
        Object FLOAT16_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:139:11: ( property 'within' FLOAT 'of' FLOAT ',' FLOAT )
            // org/usergrid/persistence/query/tree/QueryFilter.g:140:3: property 'within' FLOAT 'of' FLOAT ',' FLOAT
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_property_in_locationop801);
            property10=property();

            state._fsp--;

            adaptor.addChild(root_0, property10.getTree());
            string_literal11=(Token)match(input,21,FOLLOW_21_in_locationop803); 
            string_literal11_tree = new Within(string_literal11) ;
            root_0 = (Object)adaptor.becomeRoot(string_literal11_tree, root_0);

            FLOAT12=(Token)match(input,FLOAT,FOLLOW_FLOAT_in_locationop809); 
            FLOAT12_tree = (Object)adaptor.create(FLOAT12);
            adaptor.addChild(root_0, FLOAT12_tree);

            string_literal13=(Token)match(input,22,FOLLOW_22_in_locationop811); 
            string_literal13_tree = (Object)adaptor.create(string_literal13);
            adaptor.addChild(root_0, string_literal13_tree);

            FLOAT14=(Token)match(input,FLOAT,FOLLOW_FLOAT_in_locationop813); 
            FLOAT14_tree = (Object)adaptor.create(FLOAT14);
            adaptor.addChild(root_0, FLOAT14_tree);

            char_literal15=(Token)match(input,23,FOLLOW_23_in_locationop815); 
            char_literal15_tree = (Object)adaptor.create(char_literal15);
            adaptor.addChild(root_0, char_literal15_tree);

            FLOAT16=(Token)match(input,FLOAT,FOLLOW_FLOAT_in_locationop817); 
            FLOAT16_tree = (Object)adaptor.create(FLOAT16);
            adaptor.addChild(root_0, FLOAT16_tree);


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
    // org/usergrid/persistence/query/tree/QueryFilter.g:143:1: containsop : property 'contains' STRING ;
    public final QueryFilterParser.containsop_return containsop() throws RecognitionException {
        QueryFilterParser.containsop_return retval = new QueryFilterParser.containsop_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token string_literal18=null;
        Token STRING19=null;
        QueryFilterParser.property_return property17 = null;


        Object string_literal18_tree=null;
        Object STRING19_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:143:12: ( property 'contains' STRING )
            // org/usergrid/persistence/query/tree/QueryFilter.g:144:3: property 'contains' STRING
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_property_in_containsop832);
            property17=property();

            state._fsp--;

            adaptor.addChild(root_0, property17.getTree());
            string_literal18=(Token)match(input,24,FOLLOW_24_in_containsop834); 
            string_literal18_tree = new Contains(string_literal18) ;
            root_0 = (Object)adaptor.becomeRoot(string_literal18_tree, root_0);

            STRING19=(Token)match(input,STRING,FOLLOW_STRING_in_containsop840); 
            STRING19_tree = (Object)adaptor.create(STRING19);
            adaptor.addChild(root_0, STRING19_tree);


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
    // org/usergrid/persistence/query/tree/QueryFilter.g:147:1: operation : ( '(' expression ')' | equalityop | locationop | containsop );
    public final QueryFilterParser.operation_return operation() throws RecognitionException {
        QueryFilterParser.operation_return retval = new QueryFilterParser.operation_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token char_literal20=null;
        Token char_literal22=null;
        QueryFilterParser.expression_return expression21 = null;

        QueryFilterParser.equalityop_return equalityop23 = null;

        QueryFilterParser.locationop_return locationop24 = null;

        QueryFilterParser.containsop_return containsop25 = null;


        Object char_literal20_tree=null;
        Object char_literal22_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:147:11: ( '(' expression ')' | equalityop | locationop | containsop )
            int alt2=4;
            int LA2_0 = input.LA(1);

            if ( (LA2_0==25) ) {
                alt2=1;
            }
            else if ( (LA2_0==ID) ) {
                switch ( input.LA(2) ) {
                case 24:
                    {
                    alt2=4;
                    }
                    break;
                case LT:
                case LTE:
                case EQ:
                case GT:
                case GTE:
                    {
                    alt2=2;
                    }
                    break;
                case 21:
                    {
                    alt2=3;
                    }
                    break;
                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 2, 2, input);

                    throw nvae;
                }

            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 2, 0, input);

                throw nvae;
            }
            switch (alt2) {
                case 1 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:148:2: '(' expression ')'
                    {
                    root_0 = (Object)adaptor.nil();

                    char_literal20=(Token)match(input,25,FOLLOW_25_in_operation853); 
                    pushFollow(FOLLOW_expression_in_operation856);
                    expression21=expression();

                    state._fsp--;

                    adaptor.addChild(root_0, expression21.getTree());
                    char_literal22=(Token)match(input,26,FOLLOW_26_in_operation858); 

                    }
                    break;
                case 2 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:148:25: equalityop
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_equalityop_in_operation863);
                    equalityop23=equalityop();

                    state._fsp--;

                    adaptor.addChild(root_0, equalityop23.getTree());

                    }
                    break;
                case 3 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:148:38: locationop
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_locationop_in_operation867);
                    locationop24=locationop();

                    state._fsp--;

                    adaptor.addChild(root_0, locationop24.getTree());

                    }
                    break;
                case 4 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:148:51: containsop
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_containsop_in_operation871);
                    containsop25=containsop();

                    state._fsp--;

                    adaptor.addChild(root_0, containsop25.getTree());

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
    // org/usergrid/persistence/query/tree/QueryFilter.g:151:1: notexp : ( 'not' operation | operation );
    public final QueryFilterParser.notexp_return notexp() throws RecognitionException {
        QueryFilterParser.notexp_return retval = new QueryFilterParser.notexp_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token string_literal26=null;
        QueryFilterParser.operation_return operation27 = null;

        QueryFilterParser.operation_return operation28 = null;


        Object string_literal26_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:151:8: ( 'not' operation | operation )
            int alt3=2;
            int LA3_0 = input.LA(1);

            if ( (LA3_0==27) ) {
                alt3=1;
            }
            else if ( (LA3_0==ID||LA3_0==25) ) {
                alt3=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 3, 0, input);

                throw nvae;
            }
            switch (alt3) {
                case 1 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:152:2: 'not' operation
                    {
                    root_0 = (Object)adaptor.nil();

                    string_literal26=(Token)match(input,27,FOLLOW_27_in_notexp881); 
                    string_literal26_tree = (Object)adaptor.create(string_literal26);
                    root_0 = (Object)adaptor.becomeRoot(string_literal26_tree, root_0);

                    pushFollow(FOLLOW_operation_in_notexp884);
                    operation27=operation();

                    state._fsp--;

                    adaptor.addChild(root_0, operation27.getTree());

                    }
                    break;
                case 2 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:152:19: operation
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_operation_in_notexp886);
                    operation28=operation();

                    state._fsp--;

                    adaptor.addChild(root_0, operation28.getTree());

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
    // org/usergrid/persistence/query/tree/QueryFilter.g:157:1: andexp : notexp ( 'and' notexp )* ;
    public final QueryFilterParser.andexp_return andexp() throws RecognitionException {
        QueryFilterParser.andexp_return retval = new QueryFilterParser.andexp_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token string_literal30=null;
        QueryFilterParser.notexp_return notexp29 = null;

        QueryFilterParser.notexp_return notexp31 = null;


        Object string_literal30_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:157:8: ( notexp ( 'and' notexp )* )
            // org/usergrid/persistence/query/tree/QueryFilter.g:158:2: notexp ( 'and' notexp )*
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_notexp_in_andexp902);
            notexp29=notexp();

            state._fsp--;

            adaptor.addChild(root_0, notexp29.getTree());
            // org/usergrid/persistence/query/tree/QueryFilter.g:158:9: ( 'and' notexp )*
            loop4:
            do {
                int alt4=2;
                int LA4_0 = input.LA(1);

                if ( (LA4_0==28) ) {
                    alt4=1;
                }


                switch (alt4) {
            	case 1 :
            	    // org/usergrid/persistence/query/tree/QueryFilter.g:158:10: 'and' notexp
            	    {
            	    string_literal30=(Token)match(input,28,FOLLOW_28_in_andexp905); 
            	    string_literal30_tree = (Object)adaptor.create(string_literal30);
            	    root_0 = (Object)adaptor.becomeRoot(string_literal30_tree, root_0);

            	    pushFollow(FOLLOW_notexp_in_andexp908);
            	    notexp31=notexp();

            	    state._fsp--;

            	    adaptor.addChild(root_0, notexp31.getTree());

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
    // $ANTLR end "andexp"

    public static class orexp_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "orexp"
    // org/usergrid/persistence/query/tree/QueryFilter.g:161:1: orexp : andexp ( 'or' andexp )* ;
    public final QueryFilterParser.orexp_return orexp() throws RecognitionException {
        QueryFilterParser.orexp_return retval = new QueryFilterParser.orexp_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token string_literal33=null;
        QueryFilterParser.andexp_return andexp32 = null;

        QueryFilterParser.andexp_return andexp34 = null;


        Object string_literal33_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:161:7: ( andexp ( 'or' andexp )* )
            // org/usergrid/persistence/query/tree/QueryFilter.g:162:2: andexp ( 'or' andexp )*
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_andexp_in_orexp921);
            andexp32=andexp();

            state._fsp--;

            adaptor.addChild(root_0, andexp32.getTree());
            // org/usergrid/persistence/query/tree/QueryFilter.g:162:9: ( 'or' andexp )*
            loop5:
            do {
                int alt5=2;
                int LA5_0 = input.LA(1);

                if ( (LA5_0==29) ) {
                    alt5=1;
                }


                switch (alt5) {
            	case 1 :
            	    // org/usergrid/persistence/query/tree/QueryFilter.g:162:10: 'or' andexp
            	    {
            	    string_literal33=(Token)match(input,29,FOLLOW_29_in_orexp924); 
            	    string_literal33_tree = (Object)adaptor.create(string_literal33);
            	    root_0 = (Object)adaptor.becomeRoot(string_literal33_tree, root_0);

            	    pushFollow(FOLLOW_andexp_in_orexp927);
            	    andexp34=andexp();

            	    state._fsp--;

            	    adaptor.addChild(root_0, andexp34.getTree());

            	    }
            	    break;

            	default :
            	    break loop5;
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
    // org/usergrid/persistence/query/tree/QueryFilter.g:165:1: expression : orexp ;
    public final QueryFilterParser.expression_return expression() throws RecognitionException {
        QueryFilterParser.expression_return retval = new QueryFilterParser.expression_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        QueryFilterParser.orexp_return orexp35 = null;



        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:165:11: ( orexp )
            // org/usergrid/persistence/query/tree/QueryFilter.g:166:3: orexp
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_orexp_in_expression939);
            orexp35=orexp();

            state._fsp--;

            adaptor.addChild(root_0, orexp35.getTree());

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
    // org/usergrid/persistence/query/tree/QueryFilter.g:173:1: direction : ( 'asc' | 'desc' ) ;
    public final QueryFilterParser.direction_return direction() throws RecognitionException {
        QueryFilterParser.direction_return retval = new QueryFilterParser.direction_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token set36=null;

        Object set36_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:173:12: ( ( 'asc' | 'desc' ) )
            // org/usergrid/persistence/query/tree/QueryFilter.g:173:14: ( 'asc' | 'desc' )
            {
            root_0 = (Object)adaptor.nil();

            set36=(Token)input.LT(1);
            if ( (input.LA(1)>=30 && input.LA(1)<=31) ) {
                input.consume();
                adaptor.addChild(root_0, (Object)adaptor.create(set36));
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
    // org/usergrid/persistence/query/tree/QueryFilter.g:176:1: order : ( property ( direction )? ) ;
    public final QueryFilterParser.order_return order() throws RecognitionException {
        QueryFilterParser.order_return retval = new QueryFilterParser.order_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        QueryFilterParser.property_return property37 = null;

        QueryFilterParser.direction_return direction38 = null;



        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:177:3: ( ( property ( direction )? ) )
            // org/usergrid/persistence/query/tree/QueryFilter.g:177:5: ( property ( direction )? )
            {
            root_0 = (Object)adaptor.nil();

            // org/usergrid/persistence/query/tree/QueryFilter.g:177:5: ( property ( direction )? )
            // org/usergrid/persistence/query/tree/QueryFilter.g:177:6: property ( direction )?
            {
            pushFollow(FOLLOW_property_in_order971);
            property37=property();

            state._fsp--;

            adaptor.addChild(root_0, property37.getTree());
            // org/usergrid/persistence/query/tree/QueryFilter.g:177:15: ( direction )?
            int alt6=2;
            int LA6_0 = input.LA(1);

            if ( ((LA6_0>=30 && LA6_0<=31)) ) {
                alt6=1;
            }
            switch (alt6) {
                case 1 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:177:15: direction
                    {
                    pushFollow(FOLLOW_direction_in_order973);
                    direction38=direction();

                    state._fsp--;

                    adaptor.addChild(root_0, direction38.getTree());

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
    // org/usergrid/persistence/query/tree/QueryFilter.g:183:1: select_subject : ID ;
    public final QueryFilterParser.select_subject_return select_subject() throws RecognitionException {
        QueryFilterParser.select_subject_return retval = new QueryFilterParser.select_subject_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token ID39=null;

        Object ID39_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:184:3: ( ID )
            // org/usergrid/persistence/query/tree/QueryFilter.g:184:5: ID
            {
            root_0 = (Object)adaptor.nil();

            ID39=(Token)match(input,ID,FOLLOW_ID_in_select_subject991); 
            ID39_tree = (Object)adaptor.create(ID39);
            adaptor.addChild(root_0, ID39_tree);



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
    // org/usergrid/persistence/query/tree/QueryFilter.g:190:1: select_assign_target : ID ;
    public final QueryFilterParser.select_assign_target_return select_assign_target() throws RecognitionException {
        QueryFilterParser.select_assign_target_return retval = new QueryFilterParser.select_assign_target_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token ID40=null;

        Object ID40_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:191:3: ( ID )
            // org/usergrid/persistence/query/tree/QueryFilter.g:191:5: ID
            {
            root_0 = (Object)adaptor.nil();

            ID40=(Token)match(input,ID,FOLLOW_ID_in_select_assign_target1004); 
            ID40_tree = (Object)adaptor.create(ID40);
            adaptor.addChild(root_0, ID40_tree);


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
    // org/usergrid/persistence/query/tree/QueryFilter.g:193:1: select_assign_source : ID ;
    public final QueryFilterParser.select_assign_source_return select_assign_source() throws RecognitionException {
        QueryFilterParser.select_assign_source_return retval = new QueryFilterParser.select_assign_source_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token ID41=null;

        Object ID41_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:194:3: ( ID )
            // org/usergrid/persistence/query/tree/QueryFilter.g:194:5: ID
            {
            root_0 = (Object)adaptor.nil();

            ID41=(Token)match(input,ID,FOLLOW_ID_in_select_assign_source1017); 
            ID41_tree = (Object)adaptor.create(ID41);
            adaptor.addChild(root_0, ID41_tree);


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
    // org/usergrid/persistence/query/tree/QueryFilter.g:196:1: select_assign : select_assign_target ':' select_assign_source ;
    public final QueryFilterParser.select_assign_return select_assign() throws RecognitionException {
        QueryFilterParser.select_assign_return retval = new QueryFilterParser.select_assign_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token char_literal43=null;
        QueryFilterParser.select_assign_target_return select_assign_target42 = null;

        QueryFilterParser.select_assign_source_return select_assign_source44 = null;


        Object char_literal43_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:197:3: ( select_assign_target ':' select_assign_source )
            // org/usergrid/persistence/query/tree/QueryFilter.g:197:5: select_assign_target ':' select_assign_source
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_select_assign_target_in_select_assign1031);
            select_assign_target42=select_assign_target();

            state._fsp--;

            adaptor.addChild(root_0, select_assign_target42.getTree());
            char_literal43=(Token)match(input,32,FOLLOW_32_in_select_assign1033); 
            char_literal43_tree = (Object)adaptor.create(char_literal43);
            adaptor.addChild(root_0, char_literal43_tree);

            pushFollow(FOLLOW_select_assign_source_in_select_assign1035);
            select_assign_source44=select_assign_source();

            state._fsp--;

            adaptor.addChild(root_0, select_assign_source44.getTree());


              query.addSelect((select_assign_source44!=null?input.toString(select_assign_source44.start,select_assign_source44.stop):null), (select_assign_target42!=null?input.toString(select_assign_target42.start,select_assign_target42.stop):null));



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
    // org/usergrid/persistence/query/tree/QueryFilter.g:203:1: select_expr : ( '*' | select_subject ( ',' select_subject )* | '{' select_assign ( ',' select_assign )* '}' ) ;
    public final QueryFilterParser.select_expr_return select_expr() throws RecognitionException {
        QueryFilterParser.select_expr_return retval = new QueryFilterParser.select_expr_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token char_literal45=null;
        Token char_literal47=null;
        Token char_literal49=null;
        Token char_literal51=null;
        Token char_literal53=null;
        QueryFilterParser.select_subject_return select_subject46 = null;

        QueryFilterParser.select_subject_return select_subject48 = null;

        QueryFilterParser.select_assign_return select_assign50 = null;

        QueryFilterParser.select_assign_return select_assign52 = null;


        Object char_literal45_tree=null;
        Object char_literal47_tree=null;
        Object char_literal49_tree=null;
        Object char_literal51_tree=null;
        Object char_literal53_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:204:3: ( ( '*' | select_subject ( ',' select_subject )* | '{' select_assign ( ',' select_assign )* '}' ) )
            // org/usergrid/persistence/query/tree/QueryFilter.g:204:5: ( '*' | select_subject ( ',' select_subject )* | '{' select_assign ( ',' select_assign )* '}' )
            {
            root_0 = (Object)adaptor.nil();

            // org/usergrid/persistence/query/tree/QueryFilter.g:204:5: ( '*' | select_subject ( ',' select_subject )* | '{' select_assign ( ',' select_assign )* '}' )
            int alt9=3;
            switch ( input.LA(1) ) {
            case 33:
                {
                alt9=1;
                }
                break;
            case ID:
                {
                alt9=2;
                }
                break;
            case 34:
                {
                alt9=3;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 9, 0, input);

                throw nvae;
            }

            switch (alt9) {
                case 1 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:204:6: '*'
                    {
                    char_literal45=(Token)match(input,33,FOLLOW_33_in_select_expr1049); 
                    char_literal45_tree = (Object)adaptor.create(char_literal45);
                    adaptor.addChild(root_0, char_literal45_tree);


                    }
                    break;
                case 2 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:204:12: select_subject ( ',' select_subject )*
                    {
                    pushFollow(FOLLOW_select_subject_in_select_expr1053);
                    select_subject46=select_subject();

                    state._fsp--;

                    adaptor.addChild(root_0, select_subject46.getTree());
                    // org/usergrid/persistence/query/tree/QueryFilter.g:204:27: ( ',' select_subject )*
                    loop7:
                    do {
                        int alt7=2;
                        int LA7_0 = input.LA(1);

                        if ( (LA7_0==23) ) {
                            alt7=1;
                        }


                        switch (alt7) {
                    	case 1 :
                    	    // org/usergrid/persistence/query/tree/QueryFilter.g:204:28: ',' select_subject
                    	    {
                    	    char_literal47=(Token)match(input,23,FOLLOW_23_in_select_expr1056); 
                    	    char_literal47_tree = (Object)adaptor.create(char_literal47);
                    	    adaptor.addChild(root_0, char_literal47_tree);

                    	    pushFollow(FOLLOW_select_subject_in_select_expr1058);
                    	    select_subject48=select_subject();

                    	    state._fsp--;

                    	    adaptor.addChild(root_0, select_subject48.getTree());

                    	    }
                    	    break;

                    	default :
                    	    break loop7;
                        }
                    } while (true);


                    }
                    break;
                case 3 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:204:52: '{' select_assign ( ',' select_assign )* '}'
                    {
                    char_literal49=(Token)match(input,34,FOLLOW_34_in_select_expr1065); 
                    char_literal49_tree = (Object)adaptor.create(char_literal49);
                    adaptor.addChild(root_0, char_literal49_tree);

                    pushFollow(FOLLOW_select_assign_in_select_expr1067);
                    select_assign50=select_assign();

                    state._fsp--;

                    adaptor.addChild(root_0, select_assign50.getTree());
                    // org/usergrid/persistence/query/tree/QueryFilter.g:204:70: ( ',' select_assign )*
                    loop8:
                    do {
                        int alt8=2;
                        int LA8_0 = input.LA(1);

                        if ( (LA8_0==23) ) {
                            alt8=1;
                        }


                        switch (alt8) {
                    	case 1 :
                    	    // org/usergrid/persistence/query/tree/QueryFilter.g:204:71: ',' select_assign
                    	    {
                    	    char_literal51=(Token)match(input,23,FOLLOW_23_in_select_expr1070); 
                    	    char_literal51_tree = (Object)adaptor.create(char_literal51);
                    	    adaptor.addChild(root_0, char_literal51_tree);

                    	    pushFollow(FOLLOW_select_assign_in_select_expr1072);
                    	    select_assign52=select_assign();

                    	    state._fsp--;

                    	    adaptor.addChild(root_0, select_assign52.getTree());

                    	    }
                    	    break;

                    	default :
                    	    break loop8;
                        }
                    } while (true);

                    char_literal53=(Token)match(input,35,FOLLOW_35_in_select_expr1077); 
                    char_literal53_tree = (Object)adaptor.create(char_literal53);
                    adaptor.addChild(root_0, char_literal53_tree);


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
    // org/usergrid/persistence/query/tree/QueryFilter.g:208:1: ql returns [Query q] : 'select' select_expr ( 'where' expression )? ( 'order by' order ( ',' order )* )? ;
    public final QueryFilterParser.ql_return ql() throws RecognitionException {
        QueryFilterParser.ql_return retval = new QueryFilterParser.ql_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token string_literal54=null;
        Token string_literal56=null;
        Token string_literal58=null;
        Token char_literal60=null;
        QueryFilterParser.select_expr_return select_expr55 = null;

        QueryFilterParser.expression_return expression57 = null;

        QueryFilterParser.order_return order59 = null;

        QueryFilterParser.order_return order61 = null;


        Object string_literal54_tree=null;
        Object string_literal56_tree=null;
        Object string_literal58_tree=null;
        Object char_literal60_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:209:3: ( 'select' select_expr ( 'where' expression )? ( 'order by' order ( ',' order )* )? )
            // org/usergrid/persistence/query/tree/QueryFilter.g:209:5: 'select' select_expr ( 'where' expression )? ( 'order by' order ( ',' order )* )?
            {
            root_0 = (Object)adaptor.nil();

            string_literal54=(Token)match(input,36,FOLLOW_36_in_ql1099); 
            string_literal54_tree = (Object)adaptor.create(string_literal54);
            adaptor.addChild(root_0, string_literal54_tree);

            pushFollow(FOLLOW_select_expr_in_ql1101);
            select_expr55=select_expr();

            state._fsp--;

            adaptor.addChild(root_0, select_expr55.getTree());
            // org/usergrid/persistence/query/tree/QueryFilter.g:209:26: ( 'where' expression )?
            int alt10=2;
            int LA10_0 = input.LA(1);

            if ( (LA10_0==37) ) {
                alt10=1;
            }
            switch (alt10) {
                case 1 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:209:27: 'where' expression
                    {
                    string_literal56=(Token)match(input,37,FOLLOW_37_in_ql1104); 
                    string_literal56_tree = (Object)adaptor.create(string_literal56);
                    adaptor.addChild(root_0, string_literal56_tree);

                    pushFollow(FOLLOW_expression_in_ql1106);
                    expression57=expression();

                    state._fsp--;

                    adaptor.addChild(root_0, expression57.getTree());

                    }
                    break;

            }

            // org/usergrid/persistence/query/tree/QueryFilter.g:209:49: ( 'order by' order ( ',' order )* )?
            int alt12=2;
            int LA12_0 = input.LA(1);

            if ( (LA12_0==38) ) {
                alt12=1;
            }
            switch (alt12) {
                case 1 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:209:50: 'order by' order ( ',' order )*
                    {
                    string_literal58=(Token)match(input,38,FOLLOW_38_in_ql1112); 
                    string_literal58_tree = (Object)adaptor.create(string_literal58);
                    adaptor.addChild(root_0, string_literal58_tree);

                    pushFollow(FOLLOW_order_in_ql1114);
                    order59=order();

                    state._fsp--;

                    adaptor.addChild(root_0, order59.getTree());
                    // org/usergrid/persistence/query/tree/QueryFilter.g:209:67: ( ',' order )*
                    loop11:
                    do {
                        int alt11=2;
                        int LA11_0 = input.LA(1);

                        if ( (LA11_0==23) ) {
                            alt11=1;
                        }


                        switch (alt11) {
                    	case 1 :
                    	    // org/usergrid/persistence/query/tree/QueryFilter.g:209:68: ',' order
                    	    {
                    	    char_literal60=(Token)match(input,23,FOLLOW_23_in_ql1117); 
                    	    char_literal60_tree = (Object)adaptor.create(char_literal60);
                    	    adaptor.addChild(root_0, char_literal60_tree);

                    	    pushFollow(FOLLOW_order_in_ql1119);
                    	    order61=order();

                    	    state._fsp--;

                    	    adaptor.addChild(root_0, order61.getTree());

                    	    }
                    	    break;

                    	default :
                    	    break loop11;
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
    public static final BitSet FOLLOW_property_in_equalityop739 = new BitSet(new long[]{0x00000000001F0000L});
    public static final BitSet FOLLOW_LT_in_equalityop743 = new BitSet(new long[]{0x00000000000016A0L});
    public static final BitSet FOLLOW_LTE_in_equalityop752 = new BitSet(new long[]{0x00000000000016A0L});
    public static final BitSet FOLLOW_EQ_in_equalityop762 = new BitSet(new long[]{0x00000000000016A0L});
    public static final BitSet FOLLOW_GT_in_equalityop772 = new BitSet(new long[]{0x00000000000016A0L});
    public static final BitSet FOLLOW_GTE_in_equalityop782 = new BitSet(new long[]{0x00000000000016A0L});
    public static final BitSet FOLLOW_value_in_equalityop790 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_property_in_locationop801 = new BitSet(new long[]{0x0000000000200000L});
    public static final BitSet FOLLOW_21_in_locationop803 = new BitSet(new long[]{0x0000000000000080L});
    public static final BitSet FOLLOW_FLOAT_in_locationop809 = new BitSet(new long[]{0x0000000000400000L});
    public static final BitSet FOLLOW_22_in_locationop811 = new BitSet(new long[]{0x0000000000000080L});
    public static final BitSet FOLLOW_FLOAT_in_locationop813 = new BitSet(new long[]{0x0000000000800000L});
    public static final BitSet FOLLOW_23_in_locationop815 = new BitSet(new long[]{0x0000000000000080L});
    public static final BitSet FOLLOW_FLOAT_in_locationop817 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_property_in_containsop832 = new BitSet(new long[]{0x0000000001000000L});
    public static final BitSet FOLLOW_24_in_containsop834 = new BitSet(new long[]{0x0000000000000200L});
    public static final BitSet FOLLOW_STRING_in_containsop840 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_25_in_operation853 = new BitSet(new long[]{0x000000000A000010L});
    public static final BitSet FOLLOW_expression_in_operation856 = new BitSet(new long[]{0x0000000004000000L});
    public static final BitSet FOLLOW_26_in_operation858 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_equalityop_in_operation863 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_locationop_in_operation867 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_containsop_in_operation871 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_27_in_notexp881 = new BitSet(new long[]{0x000000000A000010L});
    public static final BitSet FOLLOW_operation_in_notexp884 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_operation_in_notexp886 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_notexp_in_andexp902 = new BitSet(new long[]{0x0000000010000002L});
    public static final BitSet FOLLOW_28_in_andexp905 = new BitSet(new long[]{0x000000000A000010L});
    public static final BitSet FOLLOW_notexp_in_andexp908 = new BitSet(new long[]{0x0000000010000002L});
    public static final BitSet FOLLOW_andexp_in_orexp921 = new BitSet(new long[]{0x0000000020000002L});
    public static final BitSet FOLLOW_29_in_orexp924 = new BitSet(new long[]{0x000000000A000010L});
    public static final BitSet FOLLOW_andexp_in_orexp927 = new BitSet(new long[]{0x0000000020000002L});
    public static final BitSet FOLLOW_orexp_in_expression939 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_set_in_direction953 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_property_in_order971 = new BitSet(new long[]{0x00000000C0000002L});
    public static final BitSet FOLLOW_direction_in_order973 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_select_subject991 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_select_assign_target1004 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_select_assign_source1017 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_select_assign_target_in_select_assign1031 = new BitSet(new long[]{0x0000000100000000L});
    public static final BitSet FOLLOW_32_in_select_assign1033 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_select_assign_source_in_select_assign1035 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_33_in_select_expr1049 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_select_subject_in_select_expr1053 = new BitSet(new long[]{0x0000000000800002L});
    public static final BitSet FOLLOW_23_in_select_expr1056 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_select_subject_in_select_expr1058 = new BitSet(new long[]{0x0000000000800002L});
    public static final BitSet FOLLOW_34_in_select_expr1065 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_select_assign_in_select_expr1067 = new BitSet(new long[]{0x0000000800800000L});
    public static final BitSet FOLLOW_23_in_select_expr1070 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_select_assign_in_select_expr1072 = new BitSet(new long[]{0x0000000800800000L});
    public static final BitSet FOLLOW_35_in_select_expr1077 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_36_in_ql1099 = new BitSet(new long[]{0x0000000600000010L});
    public static final BitSet FOLLOW_select_expr_in_ql1101 = new BitSet(new long[]{0x0000006000000002L});
    public static final BitSet FOLLOW_37_in_ql1104 = new BitSet(new long[]{0x000000000A000010L});
    public static final BitSet FOLLOW_expression_in_ql1106 = new BitSet(new long[]{0x0000004000000002L});
    public static final BitSet FOLLOW_38_in_ql1112 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_order_in_ql1114 = new BitSet(new long[]{0x0000000000800002L});
    public static final BitSet FOLLOW_23_in_ql1117 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_order_in_ql1119 = new BitSet(new long[]{0x0000000000800002L});

}