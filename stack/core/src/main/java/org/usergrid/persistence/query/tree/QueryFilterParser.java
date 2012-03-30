// $ANTLR 3.4 org/usergrid/persistence/query/tree/QueryFilter.g 2012-03-30 16:25:34

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


@SuppressWarnings({"all", "warnings", "unchecked"})
public class QueryFilterParser extends Parser {
    public static final String[] tokenNames = new String[] {
        "<invalid>", "<EOR>", "<DOWN>", "<UP>", "BOOLEAN", "EQ", "ESC_SEQ", "EXPONENT", "FLOAT", "GT", "GTE", "HEX_DIGIT", "ID", "INT", "LT", "LTE", "OCTAL_ESC", "STRING", "UNICODE_ESC", "UUID", "WS", "'('", "')'", "'*'", "','", "':'", "'and'", "'asc'", "'contains'", "'desc'", "'not'", "'of'", "'or'", "'order by'", "'select'", "'where'", "'within'", "'{'", "'}'"
    };

    public static final int EOF=-1;
    public static final int T__21=21;
    public static final int T__22=22;
    public static final int T__23=23;
    public static final int T__24=24;
    public static final int T__25=25;
    public static final int T__26=26;
    public static final int T__27=27;
    public static final int T__28=28;
    public static final int T__29=29;
    public static final int T__30=30;
    public static final int T__31=31;
    public static final int T__32=32;
    public static final int T__33=33;
    public static final int T__34=34;
    public static final int T__35=35;
    public static final int T__36=36;
    public static final int T__37=37;
    public static final int T__38=38;
    public static final int BOOLEAN=4;
    public static final int EQ=5;
    public static final int ESC_SEQ=6;
    public static final int EXPONENT=7;
    public static final int FLOAT=8;
    public static final int GT=9;
    public static final int GTE=10;
    public static final int HEX_DIGIT=11;
    public static final int ID=12;
    public static final int INT=13;
    public static final int LT=14;
    public static final int LTE=15;
    public static final int OCTAL_ESC=16;
    public static final int STRING=17;
    public static final int UNICODE_ESC=18;
    public static final int UUID=19;
    public static final int WS=20;

    // delegates
    public Parser[] getDelegates() {
        return new Parser[] {};
    }

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
    // org/usergrid/persistence/query/tree/QueryFilter.g:125:1: property : ID ;
    public final QueryFilterParser.property_return property() throws RecognitionException {
        QueryFilterParser.property_return retval = new QueryFilterParser.property_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token ID1=null;

        Object ID1_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:125:10: ( ID )
            // org/usergrid/persistence/query/tree/QueryFilter.g:125:12: ID
            {
            root_0 = (Object)adaptor.nil();


            ID1=(Token)match(input,ID,FOLLOW_ID_in_property680); 
            ID1_tree = 
            new Property(ID1) 
            ;
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
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "property"


    public static class booleanliteral_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "booleanliteral"
    // org/usergrid/persistence/query/tree/QueryFilter.g:127:1: booleanliteral : BOOLEAN ;
    public final QueryFilterParser.booleanliteral_return booleanliteral() throws RecognitionException {
        QueryFilterParser.booleanliteral_return retval = new QueryFilterParser.booleanliteral_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token BOOLEAN2=null;

        Object BOOLEAN2_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:127:15: ( BOOLEAN )
            // org/usergrid/persistence/query/tree/QueryFilter.g:127:17: BOOLEAN
            {
            root_0 = (Object)adaptor.nil();


            BOOLEAN2=(Token)match(input,BOOLEAN,FOLLOW_BOOLEAN_in_booleanliteral691); 
            BOOLEAN2_tree = 
            new BooleanLiteral(BOOLEAN2) 
            ;
            adaptor.addChild(root_0, BOOLEAN2_tree);


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
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "booleanliteral"


    public static class intliteral_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "intliteral"
    // org/usergrid/persistence/query/tree/QueryFilter.g:130:1: intliteral : INT ;
    public final QueryFilterParser.intliteral_return intliteral() throws RecognitionException {
        QueryFilterParser.intliteral_return retval = new QueryFilterParser.intliteral_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token INT3=null;

        Object INT3_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:130:12: ( INT )
            // org/usergrid/persistence/query/tree/QueryFilter.g:131:3: INT
            {
            root_0 = (Object)adaptor.nil();


            INT3=(Token)match(input,INT,FOLLOW_INT_in_intliteral705); 
            INT3_tree = 
            new IntegerLiteral(INT3) 
            ;
            adaptor.addChild(root_0, INT3_tree);


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
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "intliteral"


    public static class uuidliteral_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "uuidliteral"
    // org/usergrid/persistence/query/tree/QueryFilter.g:133:1: uuidliteral : UUID ;
    public final QueryFilterParser.uuidliteral_return uuidliteral() throws RecognitionException {
        QueryFilterParser.uuidliteral_return retval = new QueryFilterParser.uuidliteral_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token UUID4=null;

        Object UUID4_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:133:13: ( UUID )
            // org/usergrid/persistence/query/tree/QueryFilter.g:134:3: UUID
            {
            root_0 = (Object)adaptor.nil();


            UUID4=(Token)match(input,UUID,FOLLOW_UUID_in_uuidliteral719); 
            UUID4_tree = 
            new UUIDLiteral(UUID4) 
            ;
            adaptor.addChild(root_0, UUID4_tree);


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
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "uuidliteral"


    public static class stringliteral_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "stringliteral"
    // org/usergrid/persistence/query/tree/QueryFilter.g:136:1: stringliteral : STRING ;
    public final QueryFilterParser.stringliteral_return stringliteral() throws RecognitionException {
        QueryFilterParser.stringliteral_return retval = new QueryFilterParser.stringliteral_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token STRING5=null;

        Object STRING5_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:136:15: ( STRING )
            // org/usergrid/persistence/query/tree/QueryFilter.g:137:3: STRING
            {
            root_0 = (Object)adaptor.nil();


            STRING5=(Token)match(input,STRING,FOLLOW_STRING_in_stringliteral732); 
            STRING5_tree = 
            new StringLiteral(STRING5) 
            ;
            adaptor.addChild(root_0, STRING5_tree);


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
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "stringliteral"


    public static class floatliteral_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "floatliteral"
    // org/usergrid/persistence/query/tree/QueryFilter.g:139:1: floatliteral : FLOAT ;
    public final QueryFilterParser.floatliteral_return floatliteral() throws RecognitionException {
        QueryFilterParser.floatliteral_return retval = new QueryFilterParser.floatliteral_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token FLOAT6=null;

        Object FLOAT6_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:139:14: ( FLOAT )
            // org/usergrid/persistence/query/tree/QueryFilter.g:140:3: FLOAT
            {
            root_0 = (Object)adaptor.nil();


            FLOAT6=(Token)match(input,FLOAT,FOLLOW_FLOAT_in_floatliteral747); 
            FLOAT6_tree = 
            new FloatLiteral(FLOAT6) 
            ;
            adaptor.addChild(root_0, FLOAT6_tree);


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
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "floatliteral"


    public static class value_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "value"
    // org/usergrid/persistence/query/tree/QueryFilter.g:143:1: value : ( booleanliteral | intliteral | uuidliteral | stringliteral | floatliteral );
    public final QueryFilterParser.value_return value() throws RecognitionException {
        QueryFilterParser.value_return retval = new QueryFilterParser.value_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        QueryFilterParser.booleanliteral_return booleanliteral7 =null;

        QueryFilterParser.intliteral_return intliteral8 =null;

        QueryFilterParser.uuidliteral_return uuidliteral9 =null;

        QueryFilterParser.stringliteral_return stringliteral10 =null;

        QueryFilterParser.floatliteral_return floatliteral11 =null;



        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:143:7: ( booleanliteral | intliteral | uuidliteral | stringliteral | floatliteral )
            int alt1=5;
            switch ( input.LA(1) ) {
            case BOOLEAN:
                {
                alt1=1;
                }
                break;
            case INT:
                {
                alt1=2;
                }
                break;
            case UUID:
                {
                alt1=3;
                }
                break;
            case STRING:
                {
                alt1=4;
                }
                break;
            case FLOAT:
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
                    // org/usergrid/persistence/query/tree/QueryFilter.g:144:3: booleanliteral
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_booleanliteral_in_value763);
                    booleanliteral7=booleanliteral();

                    state._fsp--;

                    adaptor.addChild(root_0, booleanliteral7.getTree());

                    }
                    break;
                case 2 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:145:5: intliteral
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_intliteral_in_value769);
                    intliteral8=intliteral();

                    state._fsp--;

                    adaptor.addChild(root_0, intliteral8.getTree());

                    }
                    break;
                case 3 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:146:5: uuidliteral
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_uuidliteral_in_value775);
                    uuidliteral9=uuidliteral();

                    state._fsp--;

                    adaptor.addChild(root_0, uuidliteral9.getTree());

                    }
                    break;
                case 4 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:147:5: stringliteral
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_stringliteral_in_value781);
                    stringliteral10=stringliteral();

                    state._fsp--;

                    adaptor.addChild(root_0, stringliteral10.getTree());

                    }
                    break;
                case 5 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:148:5: floatliteral
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_floatliteral_in_value787);
                    floatliteral11=floatliteral();

                    state._fsp--;

                    adaptor.addChild(root_0, floatliteral11.getTree());

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
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "value"


    public static class equalityop_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "equalityop"
    // org/usergrid/persistence/query/tree/QueryFilter.g:159:1: equalityop : ( property LT ^ value | property LTE ^ value | property EQ ^ value | property GT ^ value | property GTE ^ value );
    public final QueryFilterParser.equalityop_return equalityop() throws RecognitionException {
        QueryFilterParser.equalityop_return retval = new QueryFilterParser.equalityop_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token LT13=null;
        Token LTE16=null;
        Token EQ19=null;
        Token GT22=null;
        Token GTE25=null;
        QueryFilterParser.property_return property12 =null;

        QueryFilterParser.value_return value14 =null;

        QueryFilterParser.property_return property15 =null;

        QueryFilterParser.value_return value17 =null;

        QueryFilterParser.property_return property18 =null;

        QueryFilterParser.value_return value20 =null;

        QueryFilterParser.property_return property21 =null;

        QueryFilterParser.value_return value23 =null;

        QueryFilterParser.property_return property24 =null;

        QueryFilterParser.value_return value26 =null;


        Object LT13_tree=null;
        Object LTE16_tree=null;
        Object EQ19_tree=null;
        Object GT22_tree=null;
        Object GTE25_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:159:12: ( property LT ^ value | property LTE ^ value | property EQ ^ value | property GT ^ value | property GTE ^ value )
            int alt2=5;
            switch ( input.LA(1) ) {
            case ID:
                {
                switch ( input.LA(2) ) {
                case LT:
                    {
                    alt2=1;
                    }
                    break;
                case LTE:
                    {
                    alt2=2;
                    }
                    break;
                case EQ:
                    {
                    alt2=3;
                    }
                    break;
                case GT:
                    {
                    alt2=4;
                    }
                    break;
                case GTE:
                    {
                    alt2=5;
                    }
                    break;
                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 2, 1, input);

                    throw nvae;

                }

                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 2, 0, input);

                throw nvae;

            }

            switch (alt2) {
                case 1 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:160:3: property LT ^ value
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_property_in_equalityop812);
                    property12=property();

                    state._fsp--;

                    adaptor.addChild(root_0, property12.getTree());

                    LT13=(Token)match(input,LT,FOLLOW_LT_in_equalityop814); 
                    LT13_tree = 
                    new LessThan(LT13) 
                    ;
                    root_0 = (Object)adaptor.becomeRoot(LT13_tree, root_0);


                    pushFollow(FOLLOW_value_in_equalityop820);
                    value14=value();

                    state._fsp--;

                    adaptor.addChild(root_0, value14.getTree());

                    }
                    break;
                case 2 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:161:4: property LTE ^ value
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_property_in_equalityop825);
                    property15=property();

                    state._fsp--;

                    adaptor.addChild(root_0, property15.getTree());

                    LTE16=(Token)match(input,LTE,FOLLOW_LTE_in_equalityop827); 
                    LTE16_tree = 
                    new LessThanEqual(LTE16) 
                    ;
                    root_0 = (Object)adaptor.becomeRoot(LTE16_tree, root_0);


                    pushFollow(FOLLOW_value_in_equalityop833);
                    value17=value();

                    state._fsp--;

                    adaptor.addChild(root_0, value17.getTree());

                    }
                    break;
                case 3 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:162:4: property EQ ^ value
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_property_in_equalityop838);
                    property18=property();

                    state._fsp--;

                    adaptor.addChild(root_0, property18.getTree());

                    EQ19=(Token)match(input,EQ,FOLLOW_EQ_in_equalityop840); 
                    EQ19_tree = 
                    new Equal(EQ19) 
                    ;
                    root_0 = (Object)adaptor.becomeRoot(EQ19_tree, root_0);


                    pushFollow(FOLLOW_value_in_equalityop846);
                    value20=value();

                    state._fsp--;

                    adaptor.addChild(root_0, value20.getTree());

                    }
                    break;
                case 4 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:163:4: property GT ^ value
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_property_in_equalityop851);
                    property21=property();

                    state._fsp--;

                    adaptor.addChild(root_0, property21.getTree());

                    GT22=(Token)match(input,GT,FOLLOW_GT_in_equalityop853); 
                    GT22_tree = 
                    new GreaterThan(GT22) 
                    ;
                    root_0 = (Object)adaptor.becomeRoot(GT22_tree, root_0);


                    pushFollow(FOLLOW_value_in_equalityop859);
                    value23=value();

                    state._fsp--;

                    adaptor.addChild(root_0, value23.getTree());

                    }
                    break;
                case 5 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:164:4: property GTE ^ value
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_property_in_equalityop864);
                    property24=property();

                    state._fsp--;

                    adaptor.addChild(root_0, property24.getTree());

                    GTE25=(Token)match(input,GTE,FOLLOW_GTE_in_equalityop866); 
                    GTE25_tree = 
                    new GreaterThanEqual(GTE25) 
                    ;
                    root_0 = (Object)adaptor.becomeRoot(GTE25_tree, root_0);


                    pushFollow(FOLLOW_value_in_equalityop872);
                    value26=value();

                    state._fsp--;

                    adaptor.addChild(root_0, value26.getTree());

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
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "equalityop"


    public static class locationop_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "locationop"
    // org/usergrid/persistence/query/tree/QueryFilter.g:168:1: locationop : property 'within' ^ ( floatliteral | intliteral ) 'of' ! ( floatliteral | intliteral ) ',' ! ( floatliteral | intliteral ) ;
    public final QueryFilterParser.locationop_return locationop() throws RecognitionException {
        QueryFilterParser.locationop_return retval = new QueryFilterParser.locationop_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token string_literal28=null;
        Token string_literal31=null;
        Token char_literal34=null;
        QueryFilterParser.property_return property27 =null;

        QueryFilterParser.floatliteral_return floatliteral29 =null;

        QueryFilterParser.intliteral_return intliteral30 =null;

        QueryFilterParser.floatliteral_return floatliteral32 =null;

        QueryFilterParser.intliteral_return intliteral33 =null;

        QueryFilterParser.floatliteral_return floatliteral35 =null;

        QueryFilterParser.intliteral_return intliteral36 =null;


        Object string_literal28_tree=null;
        Object string_literal31_tree=null;
        Object char_literal34_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:168:12: ( property 'within' ^ ( floatliteral | intliteral ) 'of' ! ( floatliteral | intliteral ) ',' ! ( floatliteral | intliteral ) )
            // org/usergrid/persistence/query/tree/QueryFilter.g:169:3: property 'within' ^ ( floatliteral | intliteral ) 'of' ! ( floatliteral | intliteral ) ',' ! ( floatliteral | intliteral )
            {
            root_0 = (Object)adaptor.nil();


            pushFollow(FOLLOW_property_in_locationop887);
            property27=property();

            state._fsp--;

            adaptor.addChild(root_0, property27.getTree());

            string_literal28=(Token)match(input,36,FOLLOW_36_in_locationop889); 
            string_literal28_tree = 
            new WithinOperand(string_literal28) 
            ;
            root_0 = (Object)adaptor.becomeRoot(string_literal28_tree, root_0);


            // org/usergrid/persistence/query/tree/QueryFilter.g:169:37: ( floatliteral | intliteral )
            int alt3=2;
            switch ( input.LA(1) ) {
            case FLOAT:
                {
                alt3=1;
                }
                break;
            case INT:
                {
                alt3=2;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 3, 0, input);

                throw nvae;

            }

            switch (alt3) {
                case 1 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:169:38: floatliteral
                    {
                    pushFollow(FOLLOW_floatliteral_in_locationop896);
                    floatliteral29=floatliteral();

                    state._fsp--;

                    adaptor.addChild(root_0, floatliteral29.getTree());

                    }
                    break;
                case 2 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:169:51: intliteral
                    {
                    pushFollow(FOLLOW_intliteral_in_locationop898);
                    intliteral30=intliteral();

                    state._fsp--;

                    adaptor.addChild(root_0, intliteral30.getTree());

                    }
                    break;

            }


            string_literal31=(Token)match(input,31,FOLLOW_31_in_locationop901); 

            // org/usergrid/persistence/query/tree/QueryFilter.g:169:69: ( floatliteral | intliteral )
            int alt4=2;
            switch ( input.LA(1) ) {
            case FLOAT:
                {
                alt4=1;
                }
                break;
            case INT:
                {
                alt4=2;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 4, 0, input);

                throw nvae;

            }

            switch (alt4) {
                case 1 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:169:70: floatliteral
                    {
                    pushFollow(FOLLOW_floatliteral_in_locationop905);
                    floatliteral32=floatliteral();

                    state._fsp--;

                    adaptor.addChild(root_0, floatliteral32.getTree());

                    }
                    break;
                case 2 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:169:83: intliteral
                    {
                    pushFollow(FOLLOW_intliteral_in_locationop907);
                    intliteral33=intliteral();

                    state._fsp--;

                    adaptor.addChild(root_0, intliteral33.getTree());

                    }
                    break;

            }


            char_literal34=(Token)match(input,24,FOLLOW_24_in_locationop910); 

            // org/usergrid/persistence/query/tree/QueryFilter.g:169:100: ( floatliteral | intliteral )
            int alt5=2;
            switch ( input.LA(1) ) {
            case FLOAT:
                {
                alt5=1;
                }
                break;
            case INT:
                {
                alt5=2;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 5, 0, input);

                throw nvae;

            }

            switch (alt5) {
                case 1 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:169:101: floatliteral
                    {
                    pushFollow(FOLLOW_floatliteral_in_locationop914);
                    floatliteral35=floatliteral();

                    state._fsp--;

                    adaptor.addChild(root_0, floatliteral35.getTree());

                    }
                    break;
                case 2 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:169:114: intliteral
                    {
                    pushFollow(FOLLOW_intliteral_in_locationop916);
                    intliteral36=intliteral();

                    state._fsp--;

                    adaptor.addChild(root_0, intliteral36.getTree());

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
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "locationop"


    public static class containsop_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "containsop"
    // org/usergrid/persistence/query/tree/QueryFilter.g:172:1: containsop : property 'contains' ^ stringliteral ;
    public final QueryFilterParser.containsop_return containsop() throws RecognitionException {
        QueryFilterParser.containsop_return retval = new QueryFilterParser.containsop_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token string_literal38=null;
        QueryFilterParser.property_return property37 =null;

        QueryFilterParser.stringliteral_return stringliteral39 =null;


        Object string_literal38_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:172:12: ( property 'contains' ^ stringliteral )
            // org/usergrid/persistence/query/tree/QueryFilter.g:173:3: property 'contains' ^ stringliteral
            {
            root_0 = (Object)adaptor.nil();


            pushFollow(FOLLOW_property_in_containsop930);
            property37=property();

            state._fsp--;

            adaptor.addChild(root_0, property37.getTree());

            string_literal38=(Token)match(input,28,FOLLOW_28_in_containsop932); 
            string_literal38_tree = 
            new ContainsOperand(string_literal38) 
            ;
            root_0 = (Object)adaptor.becomeRoot(string_literal38_tree, root_0);


            pushFollow(FOLLOW_stringliteral_in_containsop938);
            stringliteral39=stringliteral();

            state._fsp--;

            adaptor.addChild(root_0, stringliteral39.getTree());

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
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "containsop"


    public static class operation_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "operation"
    // org/usergrid/persistence/query/tree/QueryFilter.g:175:1: operation : ( '(' ! expression ')' !| equalityop | locationop | containsop );
    public final QueryFilterParser.operation_return operation() throws RecognitionException {
        QueryFilterParser.operation_return retval = new QueryFilterParser.operation_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token char_literal40=null;
        Token char_literal42=null;
        QueryFilterParser.expression_return expression41 =null;

        QueryFilterParser.equalityop_return equalityop43 =null;

        QueryFilterParser.locationop_return locationop44 =null;

        QueryFilterParser.containsop_return containsop45 =null;


        Object char_literal40_tree=null;
        Object char_literal42_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:175:11: ( '(' ! expression ')' !| equalityop | locationop | containsop )
            int alt6=4;
            switch ( input.LA(1) ) {
            case 21:
                {
                alt6=1;
                }
                break;
            case ID:
                {
                switch ( input.LA(2) ) {
                case EQ:
                case GT:
                case GTE:
                case LT:
                case LTE:
                    {
                    alt6=2;
                    }
                    break;
                case 36:
                    {
                    alt6=3;
                    }
                    break;
                case 28:
                    {
                    alt6=4;
                    }
                    break;
                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 6, 2, input);

                    throw nvae;

                }

                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 6, 0, input);

                throw nvae;

            }

            switch (alt6) {
                case 1 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:176:2: '(' ! expression ')' !
                    {
                    root_0 = (Object)adaptor.nil();


                    char_literal40=(Token)match(input,21,FOLLOW_21_in_operation947); 

                    pushFollow(FOLLOW_expression_in_operation950);
                    expression41=expression();

                    state._fsp--;

                    adaptor.addChild(root_0, expression41.getTree());

                    char_literal42=(Token)match(input,22,FOLLOW_22_in_operation952); 

                    }
                    break;
                case 2 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:177:6: equalityop
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_equalityop_in_operation960);
                    equalityop43=equalityop();

                    state._fsp--;

                    adaptor.addChild(root_0, equalityop43.getTree());

                    }
                    break;
                case 3 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:178:6: locationop
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_locationop_in_operation968);
                    locationop44=locationop();

                    state._fsp--;

                    adaptor.addChild(root_0, locationop44.getTree());

                    }
                    break;
                case 4 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:179:6: containsop
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_containsop_in_operation976);
                    containsop45=containsop();

                    state._fsp--;

                    adaptor.addChild(root_0, containsop45.getTree());

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
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "operation"


    public static class notexp_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "notexp"
    // org/usergrid/persistence/query/tree/QueryFilter.g:183:1: notexp : ( 'not' ^ operation | operation );
    public final QueryFilterParser.notexp_return notexp() throws RecognitionException {
        QueryFilterParser.notexp_return retval = new QueryFilterParser.notexp_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token string_literal46=null;
        QueryFilterParser.operation_return operation47 =null;

        QueryFilterParser.operation_return operation48 =null;


        Object string_literal46_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:183:8: ( 'not' ^ operation | operation )
            int alt7=2;
            switch ( input.LA(1) ) {
            case 30:
                {
                alt7=1;
                }
                break;
            case ID:
            case 21:
                {
                alt7=2;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 7, 0, input);

                throw nvae;

            }

            switch (alt7) {
                case 1 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:185:2: 'not' ^ operation
                    {
                    root_0 = (Object)adaptor.nil();


                    string_literal46=(Token)match(input,30,FOLLOW_30_in_notexp992); 
                    string_literal46_tree = 
                    new NotOperand(string_literal46) 
                    ;
                    root_0 = (Object)adaptor.becomeRoot(string_literal46_tree, root_0);


                    pushFollow(FOLLOW_operation_in_notexp998);
                    operation47=operation();

                    state._fsp--;

                    adaptor.addChild(root_0, operation47.getTree());

                    }
                    break;
                case 2 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:186:3: operation
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_operation_in_notexp1004);
                    operation48=operation();

                    state._fsp--;

                    adaptor.addChild(root_0, operation48.getTree());

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
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "notexp"


    public static class andexp_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "andexp"
    // org/usergrid/persistence/query/tree/QueryFilter.g:191:1: andexp : notexp ( 'and' ^ notexp )* ;
    public final QueryFilterParser.andexp_return andexp() throws RecognitionException {
        QueryFilterParser.andexp_return retval = new QueryFilterParser.andexp_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token string_literal50=null;
        QueryFilterParser.notexp_return notexp49 =null;

        QueryFilterParser.notexp_return notexp51 =null;


        Object string_literal50_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:191:8: ( notexp ( 'and' ^ notexp )* )
            // org/usergrid/persistence/query/tree/QueryFilter.g:192:2: notexp ( 'and' ^ notexp )*
            {
            root_0 = (Object)adaptor.nil();


            pushFollow(FOLLOW_notexp_in_andexp1018);
            notexp49=notexp();

            state._fsp--;

            adaptor.addChild(root_0, notexp49.getTree());

            // org/usergrid/persistence/query/tree/QueryFilter.g:192:9: ( 'and' ^ notexp )*
            loop8:
            do {
                int alt8=2;
                switch ( input.LA(1) ) {
                case 26:
                    {
                    alt8=1;
                    }
                    break;

                }

                switch (alt8) {
            	case 1 :
            	    // org/usergrid/persistence/query/tree/QueryFilter.g:192:10: 'and' ^ notexp
            	    {
            	    string_literal50=(Token)match(input,26,FOLLOW_26_in_andexp1021); 
            	    string_literal50_tree = 
            	    new AndOperand(string_literal50) 
            	    ;
            	    root_0 = (Object)adaptor.becomeRoot(string_literal50_tree, root_0);


            	    pushFollow(FOLLOW_notexp_in_andexp1027);
            	    notexp51=notexp();

            	    state._fsp--;

            	    adaptor.addChild(root_0, notexp51.getTree());

            	    }
            	    break;

            	default :
            	    break loop8;
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
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "andexp"


    public static class expression_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "expression"
    // org/usergrid/persistence/query/tree/QueryFilter.g:197:1: expression : andexp ( 'or' ^ andexp )* ;
    public final QueryFilterParser.expression_return expression() throws RecognitionException {
        QueryFilterParser.expression_return retval = new QueryFilterParser.expression_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token string_literal53=null;
        QueryFilterParser.andexp_return andexp52 =null;

        QueryFilterParser.andexp_return andexp54 =null;


        Object string_literal53_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:197:12: ( andexp ( 'or' ^ andexp )* )
            // org/usergrid/persistence/query/tree/QueryFilter.g:198:2: andexp ( 'or' ^ andexp )*
            {
            root_0 = (Object)adaptor.nil();


            pushFollow(FOLLOW_andexp_in_expression1044);
            andexp52=andexp();

            state._fsp--;

            adaptor.addChild(root_0, andexp52.getTree());

            // org/usergrid/persistence/query/tree/QueryFilter.g:198:9: ( 'or' ^ andexp )*
            loop9:
            do {
                int alt9=2;
                switch ( input.LA(1) ) {
                case 32:
                    {
                    alt9=1;
                    }
                    break;

                }

                switch (alt9) {
            	case 1 :
            	    // org/usergrid/persistence/query/tree/QueryFilter.g:198:10: 'or' ^ andexp
            	    {
            	    string_literal53=(Token)match(input,32,FOLLOW_32_in_expression1047); 
            	    string_literal53_tree = 
            	    new OrOperand(string_literal53) 
            	    ;
            	    root_0 = (Object)adaptor.becomeRoot(string_literal53_tree, root_0);


            	    pushFollow(FOLLOW_andexp_in_expression1053);
            	    andexp54=andexp();

            	    state._fsp--;

            	    adaptor.addChild(root_0, andexp54.getTree());

            	    }
            	    break;

            	default :
            	    break loop9;
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
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "expression"


    public static class direction_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "direction"
    // org/usergrid/persistence/query/tree/QueryFilter.g:207:1: direction : ( 'asc' | 'desc' ) ;
    public final QueryFilterParser.direction_return direction() throws RecognitionException {
        QueryFilterParser.direction_return retval = new QueryFilterParser.direction_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token set55=null;

        Object set55_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:207:12: ( ( 'asc' | 'desc' ) )
            // org/usergrid/persistence/query/tree/QueryFilter.g:
            {
            root_0 = (Object)adaptor.nil();


            set55=(Token)input.LT(1);

            if ( input.LA(1)==27||input.LA(1)==29 ) {
                input.consume();
                adaptor.addChild(root_0, 
                (Object)adaptor.create(set55)
                );
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
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "direction"


    public static class order_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "order"
    // org/usergrid/persistence/query/tree/QueryFilter.g:210:1: order : ( property ( direction )? ) ;
    public final QueryFilterParser.order_return order() throws RecognitionException {
        QueryFilterParser.order_return retval = new QueryFilterParser.order_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        QueryFilterParser.property_return property56 =null;

        QueryFilterParser.direction_return direction57 =null;



        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:211:3: ( ( property ( direction )? ) )
            // org/usergrid/persistence/query/tree/QueryFilter.g:211:5: ( property ( direction )? )
            {
            root_0 = (Object)adaptor.nil();


            // org/usergrid/persistence/query/tree/QueryFilter.g:211:5: ( property ( direction )? )
            // org/usergrid/persistence/query/tree/QueryFilter.g:211:6: property ( direction )?
            {
            pushFollow(FOLLOW_property_in_order1090);
            property56=property();

            state._fsp--;

            adaptor.addChild(root_0, property56.getTree());

            // org/usergrid/persistence/query/tree/QueryFilter.g:211:15: ( direction )?
            int alt10=2;
            switch ( input.LA(1) ) {
                case 27:
                case 29:
                    {
                    alt10=1;
                    }
                    break;
            }

            switch (alt10) {
                case 1 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:211:15: direction
                    {
                    pushFollow(FOLLOW_direction_in_order1092);
                    direction57=direction();

                    state._fsp--;

                    adaptor.addChild(root_0, direction57.getTree());

                    }
                    break;

            }


            }



            		String property = (property56!=null?input.toString(property56.start,property56.stop):null); 
            		String direction = (direction57!=null?input.toString(direction57.start,direction57.stop):null);
            		query.addSort(new SortPredicate(property, direction));
                
              

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
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "order"


    public static class select_subject_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "select_subject"
    // org/usergrid/persistence/query/tree/QueryFilter.g:222:1: select_subject : ID ;
    public final QueryFilterParser.select_subject_return select_subject() throws RecognitionException {
        QueryFilterParser.select_subject_return retval = new QueryFilterParser.select_subject_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token ID58=null;

        Object ID58_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:223:3: ( ID )
            // org/usergrid/persistence/query/tree/QueryFilter.g:223:5: ID
            {
            root_0 = (Object)adaptor.nil();


            ID58=(Token)match(input,ID,FOLLOW_ID_in_select_subject1111); 
            ID58_tree = 
            (Object)adaptor.create(ID58)
            ;
            adaptor.addChild(root_0, ID58_tree);




              query.addSelect((ID58!=null?ID58.getText():null));



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
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "select_subject"


    public static class select_assign_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "select_assign"
    // org/usergrid/persistence/query/tree/QueryFilter.g:231:1: select_assign : target= ID ':' source= ID ;
    public final QueryFilterParser.select_assign_return select_assign() throws RecognitionException {
        QueryFilterParser.select_assign_return retval = new QueryFilterParser.select_assign_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token target=null;
        Token source=null;
        Token char_literal59=null;

        Object target_tree=null;
        Object source_tree=null;
        Object char_literal59_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:232:3: (target= ID ':' source= ID )
            // org/usergrid/persistence/query/tree/QueryFilter.g:232:5: target= ID ':' source= ID
            {
            root_0 = (Object)adaptor.nil();


            target=(Token)match(input,ID,FOLLOW_ID_in_select_assign1128); 
            target_tree = 
            (Object)adaptor.create(target)
            ;
            adaptor.addChild(root_0, target_tree);


            char_literal59=(Token)match(input,25,FOLLOW_25_in_select_assign1130); 
            char_literal59_tree = 
            (Object)adaptor.create(char_literal59)
            ;
            adaptor.addChild(root_0, char_literal59_tree);


            source=(Token)match(input,ID,FOLLOW_ID_in_select_assign1134); 
            source_tree = 
            (Object)adaptor.create(source)
            ;
            adaptor.addChild(root_0, source_tree);




              query.addSelect((target!=null?target.getText():null), (source!=null?source.getText():null));



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
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "select_assign"


    public static class select_expr_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "select_expr"
    // org/usergrid/persistence/query/tree/QueryFilter.g:238:1: select_expr : ( '*' | select_subject ( ',' select_subject )* | '{' select_assign ( ',' select_assign )* '}' ) ;
    public final QueryFilterParser.select_expr_return select_expr() throws RecognitionException {
        QueryFilterParser.select_expr_return retval = new QueryFilterParser.select_expr_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token char_literal60=null;
        Token char_literal62=null;
        Token char_literal64=null;
        Token char_literal66=null;
        Token char_literal68=null;
        QueryFilterParser.select_subject_return select_subject61 =null;

        QueryFilterParser.select_subject_return select_subject63 =null;

        QueryFilterParser.select_assign_return select_assign65 =null;

        QueryFilterParser.select_assign_return select_assign67 =null;


        Object char_literal60_tree=null;
        Object char_literal62_tree=null;
        Object char_literal64_tree=null;
        Object char_literal66_tree=null;
        Object char_literal68_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:239:3: ( ( '*' | select_subject ( ',' select_subject )* | '{' select_assign ( ',' select_assign )* '}' ) )
            // org/usergrid/persistence/query/tree/QueryFilter.g:239:5: ( '*' | select_subject ( ',' select_subject )* | '{' select_assign ( ',' select_assign )* '}' )
            {
            root_0 = (Object)adaptor.nil();


            // org/usergrid/persistence/query/tree/QueryFilter.g:239:5: ( '*' | select_subject ( ',' select_subject )* | '{' select_assign ( ',' select_assign )* '}' )
            int alt13=3;
            switch ( input.LA(1) ) {
            case 23:
                {
                alt13=1;
                }
                break;
            case ID:
                {
                alt13=2;
                }
                break;
            case 37:
                {
                alt13=3;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 13, 0, input);

                throw nvae;

            }

            switch (alt13) {
                case 1 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:239:6: '*'
                    {
                    char_literal60=(Token)match(input,23,FOLLOW_23_in_select_expr1148); 
                    char_literal60_tree = 
                    (Object)adaptor.create(char_literal60)
                    ;
                    adaptor.addChild(root_0, char_literal60_tree);


                    }
                    break;
                case 2 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:239:12: select_subject ( ',' select_subject )*
                    {
                    pushFollow(FOLLOW_select_subject_in_select_expr1152);
                    select_subject61=select_subject();

                    state._fsp--;

                    adaptor.addChild(root_0, select_subject61.getTree());

                    // org/usergrid/persistence/query/tree/QueryFilter.g:239:27: ( ',' select_subject )*
                    loop11:
                    do {
                        int alt11=2;
                        switch ( input.LA(1) ) {
                        case 24:
                            {
                            alt11=1;
                            }
                            break;

                        }

                        switch (alt11) {
                    	case 1 :
                    	    // org/usergrid/persistence/query/tree/QueryFilter.g:239:28: ',' select_subject
                    	    {
                    	    char_literal62=(Token)match(input,24,FOLLOW_24_in_select_expr1155); 
                    	    char_literal62_tree = 
                    	    (Object)adaptor.create(char_literal62)
                    	    ;
                    	    adaptor.addChild(root_0, char_literal62_tree);


                    	    pushFollow(FOLLOW_select_subject_in_select_expr1157);
                    	    select_subject63=select_subject();

                    	    state._fsp--;

                    	    adaptor.addChild(root_0, select_subject63.getTree());

                    	    }
                    	    break;

                    	default :
                    	    break loop11;
                        }
                    } while (true);


                    }
                    break;
                case 3 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:239:52: '{' select_assign ( ',' select_assign )* '}'
                    {
                    char_literal64=(Token)match(input,37,FOLLOW_37_in_select_expr1164); 
                    char_literal64_tree = 
                    (Object)adaptor.create(char_literal64)
                    ;
                    adaptor.addChild(root_0, char_literal64_tree);


                    pushFollow(FOLLOW_select_assign_in_select_expr1166);
                    select_assign65=select_assign();

                    state._fsp--;

                    adaptor.addChild(root_0, select_assign65.getTree());

                    // org/usergrid/persistence/query/tree/QueryFilter.g:239:70: ( ',' select_assign )*
                    loop12:
                    do {
                        int alt12=2;
                        switch ( input.LA(1) ) {
                        case 24:
                            {
                            alt12=1;
                            }
                            break;

                        }

                        switch (alt12) {
                    	case 1 :
                    	    // org/usergrid/persistence/query/tree/QueryFilter.g:239:71: ',' select_assign
                    	    {
                    	    char_literal66=(Token)match(input,24,FOLLOW_24_in_select_expr1169); 
                    	    char_literal66_tree = 
                    	    (Object)adaptor.create(char_literal66)
                    	    ;
                    	    adaptor.addChild(root_0, char_literal66_tree);


                    	    pushFollow(FOLLOW_select_assign_in_select_expr1171);
                    	    select_assign67=select_assign();

                    	    state._fsp--;

                    	    adaptor.addChild(root_0, select_assign67.getTree());

                    	    }
                    	    break;

                    	default :
                    	    break loop12;
                        }
                    } while (true);


                    char_literal68=(Token)match(input,38,FOLLOW_38_in_select_expr1176); 
                    char_literal68_tree = 
                    (Object)adaptor.create(char_literal68)
                    ;
                    adaptor.addChild(root_0, char_literal68_tree);


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
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "select_expr"


    public static class ql_return extends ParserRuleReturnScope {
        public Query query;
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "ql"
    // org/usergrid/persistence/query/tree/QueryFilter.g:243:1: ql returns [Query query] : ( 'select' ! select_expr !)? ( ( 'where' !)? expression )? ( 'order by' ! order ! ( ',' ! order !)* )? ;
    public final QueryFilterParser.ql_return ql() throws RecognitionException {
        QueryFilterParser.ql_return retval = new QueryFilterParser.ql_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token string_literal69=null;
        Token string_literal71=null;
        Token string_literal73=null;
        Token char_literal75=null;
        QueryFilterParser.select_expr_return select_expr70 =null;

        QueryFilterParser.expression_return expression72 =null;

        QueryFilterParser.order_return order74 =null;

        QueryFilterParser.order_return order76 =null;


        Object string_literal69_tree=null;
        Object string_literal71_tree=null;
        Object string_literal73_tree=null;
        Object char_literal75_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:244:3: ( ( 'select' ! select_expr !)? ( ( 'where' !)? expression )? ( 'order by' ! order ! ( ',' ! order !)* )? )
            // org/usergrid/persistence/query/tree/QueryFilter.g:244:5: ( 'select' ! select_expr !)? ( ( 'where' !)? expression )? ( 'order by' ! order ! ( ',' ! order !)* )?
            {
            root_0 = (Object)adaptor.nil();


            // org/usergrid/persistence/query/tree/QueryFilter.g:244:5: ( 'select' ! select_expr !)?
            int alt14=2;
            switch ( input.LA(1) ) {
                case 34:
                    {
                    alt14=1;
                    }
                    break;
            }

            switch (alt14) {
                case 1 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:244:6: 'select' ! select_expr !
                    {
                    string_literal69=(Token)match(input,34,FOLLOW_34_in_ql1199); 

                    pushFollow(FOLLOW_select_expr_in_ql1202);
                    select_expr70=select_expr();

                    state._fsp--;


                    }
                    break;

            }


            // org/usergrid/persistence/query/tree/QueryFilter.g:244:31: ( ( 'where' !)? expression )?
            int alt16=2;
            switch ( input.LA(1) ) {
                case ID:
                case 21:
                case 30:
                case 35:
                    {
                    alt16=1;
                    }
                    break;
            }

            switch (alt16) {
                case 1 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:244:32: ( 'where' !)? expression
                    {
                    // org/usergrid/persistence/query/tree/QueryFilter.g:244:39: ( 'where' !)?
                    int alt15=2;
                    switch ( input.LA(1) ) {
                        case 35:
                            {
                            alt15=1;
                            }
                            break;
                    }

                    switch (alt15) {
                        case 1 :
                            // org/usergrid/persistence/query/tree/QueryFilter.g:244:39: 'where' !
                            {
                            string_literal71=(Token)match(input,35,FOLLOW_35_in_ql1208); 

                            }
                            break;

                    }


                    pushFollow(FOLLOW_expression_in_ql1212);
                    expression72=expression();

                    state._fsp--;

                    adaptor.addChild(root_0, expression72.getTree());

                    }
                    break;

            }


            // org/usergrid/persistence/query/tree/QueryFilter.g:244:55: ( 'order by' ! order ! ( ',' ! order !)* )?
            int alt18=2;
            switch ( input.LA(1) ) {
                case 33:
                    {
                    alt18=1;
                    }
                    break;
            }

            switch (alt18) {
                case 1 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:244:56: 'order by' ! order ! ( ',' ! order !)*
                    {
                    string_literal73=(Token)match(input,33,FOLLOW_33_in_ql1217); 

                    pushFollow(FOLLOW_order_in_ql1220);
                    order74=order();

                    state._fsp--;


                    // org/usergrid/persistence/query/tree/QueryFilter.g:244:75: ( ',' ! order !)*
                    loop17:
                    do {
                        int alt17=2;
                        switch ( input.LA(1) ) {
                        case 24:
                            {
                            alt17=1;
                            }
                            break;

                        }

                        switch (alt17) {
                    	case 1 :
                    	    // org/usergrid/persistence/query/tree/QueryFilter.g:244:76: ',' ! order !
                    	    {
                    	    char_literal75=(Token)match(input,24,FOLLOW_24_in_ql1224); 

                    	    pushFollow(FOLLOW_order_in_ql1227);
                    	    order76=order();

                    	    state._fsp--;


                    	    }
                    	    break;

                    	default :
                    	    break loop17;
                        }
                    } while (true);


                    }
                    break;

            }




              if((expression72!=null?((Object)expression72.tree):null) instanceof Operand){
                query.setRootOperand((Operand)(expression72!=null?((Object)expression72.tree):null));
              }
              
              retval.query = query;




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
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "ql"

    // Delegated rules


 

    public static final BitSet FOLLOW_ID_in_property680 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_BOOLEAN_in_booleanliteral691 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_INT_in_intliteral705 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_UUID_in_uuidliteral719 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_STRING_in_stringliteral732 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_FLOAT_in_floatliteral747 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_booleanliteral_in_value763 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_intliteral_in_value769 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_uuidliteral_in_value775 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_stringliteral_in_value781 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_floatliteral_in_value787 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_property_in_equalityop812 = new BitSet(new long[]{0x0000000000004000L});
    public static final BitSet FOLLOW_LT_in_equalityop814 = new BitSet(new long[]{0x00000000000A2110L});
    public static final BitSet FOLLOW_value_in_equalityop820 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_property_in_equalityop825 = new BitSet(new long[]{0x0000000000008000L});
    public static final BitSet FOLLOW_LTE_in_equalityop827 = new BitSet(new long[]{0x00000000000A2110L});
    public static final BitSet FOLLOW_value_in_equalityop833 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_property_in_equalityop838 = new BitSet(new long[]{0x0000000000000020L});
    public static final BitSet FOLLOW_EQ_in_equalityop840 = new BitSet(new long[]{0x00000000000A2110L});
    public static final BitSet FOLLOW_value_in_equalityop846 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_property_in_equalityop851 = new BitSet(new long[]{0x0000000000000200L});
    public static final BitSet FOLLOW_GT_in_equalityop853 = new BitSet(new long[]{0x00000000000A2110L});
    public static final BitSet FOLLOW_value_in_equalityop859 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_property_in_equalityop864 = new BitSet(new long[]{0x0000000000000400L});
    public static final BitSet FOLLOW_GTE_in_equalityop866 = new BitSet(new long[]{0x00000000000A2110L});
    public static final BitSet FOLLOW_value_in_equalityop872 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_property_in_locationop887 = new BitSet(new long[]{0x0000001000000000L});
    public static final BitSet FOLLOW_36_in_locationop889 = new BitSet(new long[]{0x0000000000002100L});
    public static final BitSet FOLLOW_floatliteral_in_locationop896 = new BitSet(new long[]{0x0000000080000000L});
    public static final BitSet FOLLOW_intliteral_in_locationop898 = new BitSet(new long[]{0x0000000080000000L});
    public static final BitSet FOLLOW_31_in_locationop901 = new BitSet(new long[]{0x0000000000002100L});
    public static final BitSet FOLLOW_floatliteral_in_locationop905 = new BitSet(new long[]{0x0000000001000000L});
    public static final BitSet FOLLOW_intliteral_in_locationop907 = new BitSet(new long[]{0x0000000001000000L});
    public static final BitSet FOLLOW_24_in_locationop910 = new BitSet(new long[]{0x0000000000002100L});
    public static final BitSet FOLLOW_floatliteral_in_locationop914 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_intliteral_in_locationop916 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_property_in_containsop930 = new BitSet(new long[]{0x0000000010000000L});
    public static final BitSet FOLLOW_28_in_containsop932 = new BitSet(new long[]{0x0000000000020000L});
    public static final BitSet FOLLOW_stringliteral_in_containsop938 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_21_in_operation947 = new BitSet(new long[]{0x0000000040201000L});
    public static final BitSet FOLLOW_expression_in_operation950 = new BitSet(new long[]{0x0000000000400000L});
    public static final BitSet FOLLOW_22_in_operation952 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_equalityop_in_operation960 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_locationop_in_operation968 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_containsop_in_operation976 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_30_in_notexp992 = new BitSet(new long[]{0x0000000000201000L});
    public static final BitSet FOLLOW_operation_in_notexp998 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_operation_in_notexp1004 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_notexp_in_andexp1018 = new BitSet(new long[]{0x0000000004000002L});
    public static final BitSet FOLLOW_26_in_andexp1021 = new BitSet(new long[]{0x0000000040201000L});
    public static final BitSet FOLLOW_notexp_in_andexp1027 = new BitSet(new long[]{0x0000000004000002L});
    public static final BitSet FOLLOW_andexp_in_expression1044 = new BitSet(new long[]{0x0000000100000002L});
    public static final BitSet FOLLOW_32_in_expression1047 = new BitSet(new long[]{0x0000000040201000L});
    public static final BitSet FOLLOW_andexp_in_expression1053 = new BitSet(new long[]{0x0000000100000002L});
    public static final BitSet FOLLOW_property_in_order1090 = new BitSet(new long[]{0x0000000028000002L});
    public static final BitSet FOLLOW_direction_in_order1092 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_select_subject1111 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_select_assign1128 = new BitSet(new long[]{0x0000000002000000L});
    public static final BitSet FOLLOW_25_in_select_assign1130 = new BitSet(new long[]{0x0000000000001000L});
    public static final BitSet FOLLOW_ID_in_select_assign1134 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_23_in_select_expr1148 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_select_subject_in_select_expr1152 = new BitSet(new long[]{0x0000000001000002L});
    public static final BitSet FOLLOW_24_in_select_expr1155 = new BitSet(new long[]{0x0000000000001000L});
    public static final BitSet FOLLOW_select_subject_in_select_expr1157 = new BitSet(new long[]{0x0000000001000002L});
    public static final BitSet FOLLOW_37_in_select_expr1164 = new BitSet(new long[]{0x0000000000001000L});
    public static final BitSet FOLLOW_select_assign_in_select_expr1166 = new BitSet(new long[]{0x0000004001000000L});
    public static final BitSet FOLLOW_24_in_select_expr1169 = new BitSet(new long[]{0x0000000000001000L});
    public static final BitSet FOLLOW_select_assign_in_select_expr1171 = new BitSet(new long[]{0x0000004001000000L});
    public static final BitSet FOLLOW_38_in_select_expr1176 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_34_in_ql1199 = new BitSet(new long[]{0x0000002000801000L});
    public static final BitSet FOLLOW_select_expr_in_ql1202 = new BitSet(new long[]{0x0000000A40201002L});
    public static final BitSet FOLLOW_35_in_ql1208 = new BitSet(new long[]{0x0000000040201000L});
    public static final BitSet FOLLOW_expression_in_ql1212 = new BitSet(new long[]{0x0000000200000002L});
    public static final BitSet FOLLOW_33_in_ql1217 = new BitSet(new long[]{0x0000000000001000L});
    public static final BitSet FOLLOW_order_in_ql1220 = new BitSet(new long[]{0x0000000001000002L});
    public static final BitSet FOLLOW_24_in_ql1224 = new BitSet(new long[]{0x0000000000001000L});
    public static final BitSet FOLLOW_order_in_ql1227 = new BitSet(new long[]{0x0000000001000002L});

}