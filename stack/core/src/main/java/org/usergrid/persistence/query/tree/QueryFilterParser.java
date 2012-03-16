// $ANTLR 3.1.3 Mar 17, 2009 19:23:44 org/usergrid/persistence/query/tree/QueryFilter.g 2012-03-16 13:52:57

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
        public Property property;
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "property"
    // org/usergrid/persistence/query/tree/QueryFilter.g:122:1: property returns [Property property] : ID ;
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

            ID1=(Token)match(input,ID,FOLLOW_ID_in_property682); 
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

    public static class booleanliteral_return extends ParserRuleReturnScope {
        public BooleanLiteral value;
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "booleanliteral"
    // org/usergrid/persistence/query/tree/QueryFilter.g:125:1: booleanliteral returns [BooleanLiteral value] : BOOLEAN ;
    public final QueryFilterParser.booleanliteral_return booleanliteral() throws RecognitionException {
        QueryFilterParser.booleanliteral_return retval = new QueryFilterParser.booleanliteral_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token BOOLEAN2=null;

        Object BOOLEAN2_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:125:47: ( BOOLEAN )
            // org/usergrid/persistence/query/tree/QueryFilter.g:126:2: BOOLEAN
            {
            root_0 = (Object)adaptor.nil();

            BOOLEAN2=(Token)match(input,BOOLEAN,FOLLOW_BOOLEAN_in_booleanliteral699); 
            BOOLEAN2_tree = new BooleanLiteral(BOOLEAN2) ;
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
        }
        return retval;
    }
    // $ANTLR end "booleanliteral"

    public static class intliteral_return extends ParserRuleReturnScope {
        public IntegerLiteral value;
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "intliteral"
    // org/usergrid/persistence/query/tree/QueryFilter.g:129:1: intliteral returns [IntegerLiteral value] : INT ;
    public final QueryFilterParser.intliteral_return intliteral() throws RecognitionException {
        QueryFilterParser.intliteral_return retval = new QueryFilterParser.intliteral_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token INT3=null;

        Object INT3_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:129:43: ( INT )
            // org/usergrid/persistence/query/tree/QueryFilter.g:130:3: INT
            {
            root_0 = (Object)adaptor.nil();

            INT3=(Token)match(input,INT,FOLLOW_INT_in_intliteral717); 
            INT3_tree = new IntegerLiteral(INT3) ;
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
        }
        return retval;
    }
    // $ANTLR end "intliteral"

    public static class uuidliteral_return extends ParserRuleReturnScope {
        public UUIDLiteral value;
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "uuidliteral"
    // org/usergrid/persistence/query/tree/QueryFilter.g:132:1: uuidliteral returns [UUIDLiteral value] : UUID ;
    public final QueryFilterParser.uuidliteral_return uuidliteral() throws RecognitionException {
        QueryFilterParser.uuidliteral_return retval = new QueryFilterParser.uuidliteral_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token UUID4=null;

        Object UUID4_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:132:41: ( UUID )
            // org/usergrid/persistence/query/tree/QueryFilter.g:133:3: UUID
            {
            root_0 = (Object)adaptor.nil();

            UUID4=(Token)match(input,UUID,FOLLOW_UUID_in_uuidliteral734); 
            UUID4_tree = new UUIDLiteral(UUID4) ;
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
        }
        return retval;
    }
    // $ANTLR end "uuidliteral"

    public static class stringliteral_return extends ParserRuleReturnScope {
        public StringLiteral value;
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "stringliteral"
    // org/usergrid/persistence/query/tree/QueryFilter.g:135:1: stringliteral returns [StringLiteral value] : STRING ;
    public final QueryFilterParser.stringliteral_return stringliteral() throws RecognitionException {
        QueryFilterParser.stringliteral_return retval = new QueryFilterParser.stringliteral_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token STRING5=null;

        Object STRING5_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:135:45: ( STRING )
            // org/usergrid/persistence/query/tree/QueryFilter.g:136:3: STRING
            {
            root_0 = (Object)adaptor.nil();

            STRING5=(Token)match(input,STRING,FOLLOW_STRING_in_stringliteral751); 
            STRING5_tree = new StringLiteral(STRING5) ;
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
        }
        return retval;
    }
    // $ANTLR end "stringliteral"

    public static class floatliteral_return extends ParserRuleReturnScope {
        public FloatLiteral value;
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "floatliteral"
    // org/usergrid/persistence/query/tree/QueryFilter.g:138:1: floatliteral returns [FloatLiteral value] : FLOAT ;
    public final QueryFilterParser.floatliteral_return floatliteral() throws RecognitionException {
        QueryFilterParser.floatliteral_return retval = new QueryFilterParser.floatliteral_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token FLOAT6=null;

        Object FLOAT6_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:138:43: ( FLOAT )
            // org/usergrid/persistence/query/tree/QueryFilter.g:139:3: FLOAT
            {
            root_0 = (Object)adaptor.nil();

            FLOAT6=(Token)match(input,FLOAT,FOLLOW_FLOAT_in_floatliteral770); 
            FLOAT6_tree = new FloatLiteral(FLOAT6) ;
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
        }
        return retval;
    }
    // $ANTLR end "floatliteral"

    public static class value_return extends ParserRuleReturnScope {
        public Literal value;
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "value"
    // org/usergrid/persistence/query/tree/QueryFilter.g:142:1: value returns [Literal value] : ( booleanliteral | intliteral | uuidliteral | stringliteral | floatliteral );
    public final QueryFilterParser.value_return value() throws RecognitionException {
        QueryFilterParser.value_return retval = new QueryFilterParser.value_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        QueryFilterParser.booleanliteral_return booleanliteral7 = null;

        QueryFilterParser.intliteral_return intliteral8 = null;

        QueryFilterParser.uuidliteral_return uuidliteral9 = null;

        QueryFilterParser.stringliteral_return stringliteral10 = null;

        QueryFilterParser.floatliteral_return floatliteral11 = null;



        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:142:32: ( booleanliteral | intliteral | uuidliteral | stringliteral | floatliteral )
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
                    // org/usergrid/persistence/query/tree/QueryFilter.g:143:3: booleanliteral
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_booleanliteral_in_value790);
                    booleanliteral7=booleanliteral();

                    state._fsp--;

                    adaptor.addChild(root_0, booleanliteral7.getTree());
                    retval.value = (booleanliteral7!=null?booleanliteral7.value:null);

                    }
                    break;
                case 2 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:144:5: intliteral
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_intliteral_in_value798);
                    intliteral8=intliteral();

                    state._fsp--;

                    adaptor.addChild(root_0, intliteral8.getTree());
                    retval.value = (intliteral8!=null?intliteral8.value:null);

                    }
                    break;
                case 3 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:145:5: uuidliteral
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_uuidliteral_in_value806);
                    uuidliteral9=uuidliteral();

                    state._fsp--;

                    adaptor.addChild(root_0, uuidliteral9.getTree());
                    retval.value = (uuidliteral9!=null?uuidliteral9.value:null);

                    }
                    break;
                case 4 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:146:5: stringliteral
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_stringliteral_in_value814);
                    stringliteral10=stringliteral();

                    state._fsp--;

                    adaptor.addChild(root_0, stringliteral10.getTree());
                    retval.value = (stringliteral10!=null?stringliteral10.value:null);

                    }
                    break;
                case 5 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:147:5: floatliteral
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_floatliteral_in_value822);
                    floatliteral11=floatliteral();

                    state._fsp--;

                    adaptor.addChild(root_0, floatliteral11.getTree());
                    retval.value = (floatliteral11!=null?floatliteral11.value:null);

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
    // $ANTLR end "value"

    public static class equalityop_return extends ParserRuleReturnScope {
        public EqualityOperand op;
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "equalityop"
    // org/usergrid/persistence/query/tree/QueryFilter.g:158:1: equalityop returns [EqualityOperand op] : (p= property LT v= value | p= property LTE v= value | p= property EQ v= value | p= property GT v= value | p= property GTE v= value );
    public final QueryFilterParser.equalityop_return equalityop() throws RecognitionException {
        QueryFilterParser.equalityop_return retval = new QueryFilterParser.equalityop_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token LT12=null;
        Token LTE13=null;
        Token EQ14=null;
        Token GT15=null;
        Token GTE16=null;
        QueryFilterParser.property_return p = null;

        QueryFilterParser.value_return v = null;


        Object LT12_tree=null;
        Object LTE13_tree=null;
        Object EQ14_tree=null;
        Object GT15_tree=null;
        Object GTE16_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:158:40: (p= property LT v= value | p= property LTE v= value | p= property EQ v= value | p= property GT v= value | p= property GTE v= value )
            int alt2=5;
            int LA2_0 = input.LA(1);

            if ( (LA2_0==ID) ) {
                switch ( input.LA(2) ) {
                case LTE:
                    {
                    alt2=2;
                    }
                    break;
                case GTE:
                    {
                    alt2=5;
                    }
                    break;
                case GT:
                    {
                    alt2=4;
                    }
                    break;
                case LT:
                    {
                    alt2=1;
                    }
                    break;
                case EQ:
                    {
                    alt2=3;
                    }
                    break;
                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 2, 1, input);

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
                    // org/usergrid/persistence/query/tree/QueryFilter.g:159:3: p= property LT v= value
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_property_in_equalityop854);
                    p=property();

                    state._fsp--;

                    adaptor.addChild(root_0, p.getTree());
                    LT12=(Token)match(input,LT,FOLLOW_LT_in_equalityop856); 
                    LT12_tree = (Object)adaptor.create(LT12);
                    adaptor.addChild(root_0, LT12_tree);

                    pushFollow(FOLLOW_value_in_equalityop860);
                    v=value();

                    state._fsp--;

                    adaptor.addChild(root_0, v.getTree());
                     retval.op = new LessThan(p.property, v.value);

                    }
                    break;
                case 2 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:160:4: p= property LTE v= value
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_property_in_equalityop869);
                    p=property();

                    state._fsp--;

                    adaptor.addChild(root_0, p.getTree());
                    LTE13=(Token)match(input,LTE,FOLLOW_LTE_in_equalityop871); 
                    LTE13_tree = (Object)adaptor.create(LTE13);
                    adaptor.addChild(root_0, LTE13_tree);

                    pushFollow(FOLLOW_value_in_equalityop875);
                    v=value();

                    state._fsp--;

                    adaptor.addChild(root_0, v.getTree());
                     retval.op = new LessThanEqual(p.property, v.value);

                    }
                    break;
                case 3 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:161:5: p= property EQ v= value
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_property_in_equalityop885);
                    p=property();

                    state._fsp--;

                    adaptor.addChild(root_0, p.getTree());
                    EQ14=(Token)match(input,EQ,FOLLOW_EQ_in_equalityop887); 
                    EQ14_tree = (Object)adaptor.create(EQ14);
                    adaptor.addChild(root_0, EQ14_tree);

                    pushFollow(FOLLOW_value_in_equalityop891);
                    v=value();

                    state._fsp--;

                    adaptor.addChild(root_0, v.getTree());
                     retval.op = new Equal(p.property, v.value);

                    }
                    break;
                case 4 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:162:4: p= property GT v= value
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_property_in_equalityop900);
                    p=property();

                    state._fsp--;

                    adaptor.addChild(root_0, p.getTree());
                    GT15=(Token)match(input,GT,FOLLOW_GT_in_equalityop902); 
                    GT15_tree = (Object)adaptor.create(GT15);
                    adaptor.addChild(root_0, GT15_tree);

                    pushFollow(FOLLOW_value_in_equalityop906);
                    v=value();

                    state._fsp--;

                    adaptor.addChild(root_0, v.getTree());
                     retval.op = new GreaterThan(p.property, v.value);

                    }
                    break;
                case 5 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:163:4: p= property GTE v= value
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_property_in_equalityop915);
                    p=property();

                    state._fsp--;

                    adaptor.addChild(root_0, p.getTree());
                    GTE16=(Token)match(input,GTE,FOLLOW_GTE_in_equalityop917); 
                    GTE16_tree = (Object)adaptor.create(GTE16);
                    adaptor.addChild(root_0, GTE16_tree);

                    pushFollow(FOLLOW_value_in_equalityop921);
                    v=value();

                    state._fsp--;

                    adaptor.addChild(root_0, v.getTree());
                     retval.op = new LessThan(p.property, v.value);

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
    // $ANTLR end "equalityop"

    public static class locationop_return extends ParserRuleReturnScope {
        public Within op;
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "locationop"
    // org/usergrid/persistence/query/tree/QueryFilter.g:167:1: locationop returns [Within op] : p= property 'within' distance= floatliteral 'of' lattitude= floatliteral ',' longitude= floatliteral ;
    public final QueryFilterParser.locationop_return locationop() throws RecognitionException {
        QueryFilterParser.locationop_return retval = new QueryFilterParser.locationop_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token string_literal17=null;
        Token string_literal18=null;
        Token char_literal19=null;
        QueryFilterParser.property_return p = null;

        QueryFilterParser.floatliteral_return distance = null;

        QueryFilterParser.floatliteral_return lattitude = null;

        QueryFilterParser.floatliteral_return longitude = null;


        Object string_literal17_tree=null;
        Object string_literal18_tree=null;
        Object char_literal19_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:167:31: (p= property 'within' distance= floatliteral 'of' lattitude= floatliteral ',' longitude= floatliteral )
            // org/usergrid/persistence/query/tree/QueryFilter.g:168:3: p= property 'within' distance= floatliteral 'of' lattitude= floatliteral ',' longitude= floatliteral
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_property_in_locationop943);
            p=property();

            state._fsp--;

            adaptor.addChild(root_0, p.getTree());
            string_literal17=(Token)match(input,21,FOLLOW_21_in_locationop945); 
            string_literal17_tree = (Object)adaptor.create(string_literal17);
            adaptor.addChild(root_0, string_literal17_tree);

            pushFollow(FOLLOW_floatliteral_in_locationop949);
            distance=floatliteral();

            state._fsp--;

            adaptor.addChild(root_0, distance.getTree());
            string_literal18=(Token)match(input,22,FOLLOW_22_in_locationop951); 
            string_literal18_tree = (Object)adaptor.create(string_literal18);
            adaptor.addChild(root_0, string_literal18_tree);

            pushFollow(FOLLOW_floatliteral_in_locationop955);
            lattitude=floatliteral();

            state._fsp--;

            adaptor.addChild(root_0, lattitude.getTree());
            char_literal19=(Token)match(input,23,FOLLOW_23_in_locationop957); 
            char_literal19_tree = (Object)adaptor.create(char_literal19);
            adaptor.addChild(root_0, char_literal19_tree);

            pushFollow(FOLLOW_floatliteral_in_locationop961);
            longitude=floatliteral();

            state._fsp--;

            adaptor.addChild(root_0, longitude.getTree());

                retval.op = new Within(p.property, distance.value, lattitude.value, longitude.value);
              

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
        public Contains op;
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "containsop"
    // org/usergrid/persistence/query/tree/QueryFilter.g:173:1: containsop returns [Contains op] : p= property 'contains' s= stringliteral ;
    public final QueryFilterParser.containsop_return containsop() throws RecognitionException {
        QueryFilterParser.containsop_return retval = new QueryFilterParser.containsop_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token string_literal20=null;
        QueryFilterParser.property_return p = null;

        QueryFilterParser.stringliteral_return s = null;


        Object string_literal20_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:173:34: (p= property 'contains' s= stringliteral )
            // org/usergrid/persistence/query/tree/QueryFilter.g:174:3: p= property 'contains' s= stringliteral
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_property_in_containsop983);
            p=property();

            state._fsp--;

            adaptor.addChild(root_0, p.getTree());
            string_literal20=(Token)match(input,24,FOLLOW_24_in_containsop985); 
            string_literal20_tree = (Object)adaptor.create(string_literal20);
            adaptor.addChild(root_0, string_literal20_tree);

            pushFollow(FOLLOW_stringliteral_in_containsop989);
            s=stringliteral();

            state._fsp--;

            adaptor.addChild(root_0, s.getTree());

                retval.op = new Contains(p.property, s.value);
              

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
        public Operand op;
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "operation"
    // org/usergrid/persistence/query/tree/QueryFilter.g:179:1: operation returns [Operand op] : ( '(' exp= expression ')' | equalityop | locationop | containsop );
    public final QueryFilterParser.operation_return operation() throws RecognitionException {
        QueryFilterParser.operation_return retval = new QueryFilterParser.operation_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token char_literal21=null;
        Token char_literal22=null;
        QueryFilterParser.expression_return exp = null;

        QueryFilterParser.equalityop_return equalityop23 = null;

        QueryFilterParser.locationop_return locationop24 = null;

        QueryFilterParser.containsop_return containsop25 = null;


        Object char_literal21_tree=null;
        Object char_literal22_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:179:32: ( '(' exp= expression ')' | equalityop | locationop | containsop )
            int alt3=4;
            int LA3_0 = input.LA(1);

            if ( (LA3_0==25) ) {
                alt3=1;
            }
            else if ( (LA3_0==ID) ) {
                switch ( input.LA(2) ) {
                case LT:
                case LTE:
                case EQ:
                case GT:
                case GTE:
                    {
                    alt3=2;
                    }
                    break;
                case 24:
                    {
                    alt3=4;
                    }
                    break;
                case 21:
                    {
                    alt3=3;
                    }
                    break;
                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 3, 2, input);

                    throw nvae;
                }

            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 3, 0, input);

                throw nvae;
            }
            switch (alt3) {
                case 1 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:180:2: '(' exp= expression ')'
                    {
                    root_0 = (Object)adaptor.nil();

                    char_literal21=(Token)match(input,25,FOLLOW_25_in_operation1007); 
                    char_literal21_tree = (Object)adaptor.create(char_literal21);
                    adaptor.addChild(root_0, char_literal21_tree);

                    pushFollow(FOLLOW_expression_in_operation1011);
                    exp=expression();

                    state._fsp--;

                    adaptor.addChild(root_0, exp.getTree());
                    char_literal22=(Token)match(input,26,FOLLOW_26_in_operation1013); 
                    char_literal22_tree = (Object)adaptor.create(char_literal22);
                    adaptor.addChild(root_0, char_literal22_tree);

                     retval.op = exp.op; 

                    }
                    break;
                case 2 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:181:6: equalityop
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_equalityop_in_operation1023);
                    equalityop23=equalityop();

                    state._fsp--;

                    adaptor.addChild(root_0, equalityop23.getTree());
                     retval.op = (equalityop23!=null?equalityop23.op:null); 

                    }
                    break;
                case 3 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:182:6: locationop
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_locationop_in_operation1032);
                    locationop24=locationop();

                    state._fsp--;

                    adaptor.addChild(root_0, locationop24.getTree());
                     retval.op = (locationop24!=null?locationop24.op:null); 

                    }
                    break;
                case 4 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:183:6: containsop
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_containsop_in_operation1041);
                    containsop25=containsop();

                    state._fsp--;

                    adaptor.addChild(root_0, containsop25.getTree());
                     retval.op = (containsop25!=null?containsop25.op:null); 

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
        public Operand op;
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "notexp"
    // org/usergrid/persistence/query/tree/QueryFilter.g:187:1: notexp returns [Operand op] : ( 'not' operation | operation );
    public final QueryFilterParser.notexp_return notexp() throws RecognitionException {
        QueryFilterParser.notexp_return retval = new QueryFilterParser.notexp_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token string_literal26=null;
        QueryFilterParser.operation_return operation27 = null;

        QueryFilterParser.operation_return operation28 = null;


        Object string_literal26_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:187:28: ( 'not' operation | operation )
            int alt4=2;
            int LA4_0 = input.LA(1);

            if ( (LA4_0==27) ) {
                alt4=1;
            }
            else if ( (LA4_0==ID||LA4_0==25) ) {
                alt4=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 4, 0, input);

                throw nvae;
            }
            switch (alt4) {
                case 1 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:189:2: 'not' operation
                    {
                    root_0 = (Object)adaptor.nil();

                    string_literal26=(Token)match(input,27,FOLLOW_27_in_notexp1061); 
                    string_literal26_tree = (Object)adaptor.create(string_literal26);
                    root_0 = (Object)adaptor.becomeRoot(string_literal26_tree, root_0);

                    pushFollow(FOLLOW_operation_in_notexp1064);
                    operation27=operation();

                    state._fsp--;

                    adaptor.addChild(root_0, operation27.getTree());
                     retval.op = new NotOperand((operation27!=null?operation27.op:null));

                    }
                    break;
                case 2 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:190:3: operation
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_operation_in_notexp1071);
                    operation28=operation();

                    state._fsp--;

                    adaptor.addChild(root_0, operation28.getTree());
                     retval.op = (operation28!=null?operation28.op:null); 

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
        public AndOperand op;
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "andexp"
    // org/usergrid/persistence/query/tree/QueryFilter.g:195:1: andexp returns [AndOperand op] : left= notexp ( 'and' right= notexp )* ;
    public final QueryFilterParser.andexp_return andexp() throws RecognitionException {
        QueryFilterParser.andexp_return retval = new QueryFilterParser.andexp_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token string_literal29=null;
        QueryFilterParser.notexp_return left = null;

        QueryFilterParser.notexp_return right = null;


        Object string_literal29_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:195:31: (left= notexp ( 'and' right= notexp )* )
            // org/usergrid/persistence/query/tree/QueryFilter.g:196:2: left= notexp ( 'and' right= notexp )*
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_notexp_in_andexp1091);
            left=notexp();

            state._fsp--;

            adaptor.addChild(root_0, left.getTree());
            // org/usergrid/persistence/query/tree/QueryFilter.g:196:14: ( 'and' right= notexp )*
            loop5:
            do {
                int alt5=2;
                int LA5_0 = input.LA(1);

                if ( (LA5_0==28) ) {
                    alt5=1;
                }


                switch (alt5) {
            	case 1 :
            	    // org/usergrid/persistence/query/tree/QueryFilter.g:196:15: 'and' right= notexp
            	    {
            	    string_literal29=(Token)match(input,28,FOLLOW_28_in_andexp1094); 
            	    string_literal29_tree = (Object)adaptor.create(string_literal29);
            	    adaptor.addChild(root_0, string_literal29_tree);

            	    pushFollow(FOLLOW_notexp_in_andexp1098);
            	    right=notexp();

            	    state._fsp--;

            	    adaptor.addChild(root_0, right.getTree());
            	    retval.op = new AndOperand(left.op, right.op); 

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
    // $ANTLR end "andexp"

    public static class orexp_return extends ParserRuleReturnScope {
        public OrOperand op;
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "orexp"
    // org/usergrid/persistence/query/tree/QueryFilter.g:199:1: orexp returns [OrOperand op] : left= andexp ( 'or' right= andexp )* ;
    public final QueryFilterParser.orexp_return orexp() throws RecognitionException {
        QueryFilterParser.orexp_return retval = new QueryFilterParser.orexp_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token string_literal30=null;
        QueryFilterParser.andexp_return left = null;

        QueryFilterParser.andexp_return right = null;


        Object string_literal30_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:199:29: (left= andexp ( 'or' right= andexp )* )
            // org/usergrid/persistence/query/tree/QueryFilter.g:200:2: left= andexp ( 'or' right= andexp )*
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_andexp_in_orexp1119);
            left=andexp();

            state._fsp--;

            adaptor.addChild(root_0, left.getTree());
            // org/usergrid/persistence/query/tree/QueryFilter.g:200:14: ( 'or' right= andexp )*
            loop6:
            do {
                int alt6=2;
                int LA6_0 = input.LA(1);

                if ( (LA6_0==29) ) {
                    alt6=1;
                }


                switch (alt6) {
            	case 1 :
            	    // org/usergrid/persistence/query/tree/QueryFilter.g:200:15: 'or' right= andexp
            	    {
            	    string_literal30=(Token)match(input,29,FOLLOW_29_in_orexp1122); 
            	    string_literal30_tree = (Object)adaptor.create(string_literal30);
            	    adaptor.addChild(root_0, string_literal30_tree);

            	    pushFollow(FOLLOW_andexp_in_orexp1126);
            	    right=andexp();

            	    state._fsp--;

            	    adaptor.addChild(root_0, right.getTree());
            	    retval.op = new OrOperand(left.op, right.op); 

            	    }
            	    break;

            	default :
            	    break loop6;
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
        public Operand op;
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "expression"
    // org/usergrid/persistence/query/tree/QueryFilter.g:203:1: expression returns [Operand op] : orexp ;
    public final QueryFilterParser.expression_return expression() throws RecognitionException {
        QueryFilterParser.expression_return retval = new QueryFilterParser.expression_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        QueryFilterParser.orexp_return orexp31 = null;



        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:203:32: ( orexp )
            // org/usergrid/persistence/query/tree/QueryFilter.g:204:3: orexp
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_orexp_in_expression1144);
            orexp31=orexp();

            state._fsp--;

            adaptor.addChild(root_0, orexp31.getTree());

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
    // org/usergrid/persistence/query/tree/QueryFilter.g:211:1: direction : ( 'asc' | 'desc' ) ;
    public final QueryFilterParser.direction_return direction() throws RecognitionException {
        QueryFilterParser.direction_return retval = new QueryFilterParser.direction_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token set32=null;

        Object set32_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:211:12: ( ( 'asc' | 'desc' ) )
            // org/usergrid/persistence/query/tree/QueryFilter.g:211:14: ( 'asc' | 'desc' )
            {
            root_0 = (Object)adaptor.nil();

            set32=(Token)input.LT(1);
            if ( (input.LA(1)>=30 && input.LA(1)<=31) ) {
                input.consume();
                adaptor.addChild(root_0, (Object)adaptor.create(set32));
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
    // org/usergrid/persistence/query/tree/QueryFilter.g:214:1: order : ( property ( direction )? ) ;
    public final QueryFilterParser.order_return order() throws RecognitionException {
        QueryFilterParser.order_return retval = new QueryFilterParser.order_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        QueryFilterParser.property_return property33 = null;

        QueryFilterParser.direction_return direction34 = null;



        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:215:3: ( ( property ( direction )? ) )
            // org/usergrid/persistence/query/tree/QueryFilter.g:215:5: ( property ( direction )? )
            {
            root_0 = (Object)adaptor.nil();

            // org/usergrid/persistence/query/tree/QueryFilter.g:215:5: ( property ( direction )? )
            // org/usergrid/persistence/query/tree/QueryFilter.g:215:6: property ( direction )?
            {
            pushFollow(FOLLOW_property_in_order1176);
            property33=property();

            state._fsp--;

            adaptor.addChild(root_0, property33.getTree());
            // org/usergrid/persistence/query/tree/QueryFilter.g:215:15: ( direction )?
            int alt7=2;
            int LA7_0 = input.LA(1);

            if ( ((LA7_0>=30 && LA7_0<=31)) ) {
                alt7=1;
            }
            switch (alt7) {
                case 1 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:215:15: direction
                    {
                    pushFollow(FOLLOW_direction_in_order1178);
                    direction34=direction();

                    state._fsp--;

                    adaptor.addChild(root_0, direction34.getTree());

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
    // org/usergrid/persistence/query/tree/QueryFilter.g:221:1: select_subject : ID ;
    public final QueryFilterParser.select_subject_return select_subject() throws RecognitionException {
        QueryFilterParser.select_subject_return retval = new QueryFilterParser.select_subject_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token ID35=null;

        Object ID35_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:222:3: ( ID )
            // org/usergrid/persistence/query/tree/QueryFilter.g:222:5: ID
            {
            root_0 = (Object)adaptor.nil();

            ID35=(Token)match(input,ID,FOLLOW_ID_in_select_subject1196); 
            ID35_tree = (Object)adaptor.create(ID35);
            adaptor.addChild(root_0, ID35_tree);



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

    public static class select_assign_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "select_assign"
    // org/usergrid/persistence/query/tree/QueryFilter.g:230:1: select_assign : target= ID ':' source= ID ;
    public final QueryFilterParser.select_assign_return select_assign() throws RecognitionException {
        QueryFilterParser.select_assign_return retval = new QueryFilterParser.select_assign_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token target=null;
        Token source=null;
        Token char_literal36=null;

        Object target_tree=null;
        Object source_tree=null;
        Object char_literal36_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:231:3: (target= ID ':' source= ID )
            // org/usergrid/persistence/query/tree/QueryFilter.g:231:5: target= ID ':' source= ID
            {
            root_0 = (Object)adaptor.nil();

            target=(Token)match(input,ID,FOLLOW_ID_in_select_assign1213); 
            target_tree = (Object)adaptor.create(target);
            adaptor.addChild(root_0, target_tree);

            char_literal36=(Token)match(input,32,FOLLOW_32_in_select_assign1215); 
            char_literal36_tree = (Object)adaptor.create(char_literal36);
            adaptor.addChild(root_0, char_literal36_tree);

            source=(Token)match(input,ID,FOLLOW_ID_in_select_assign1219); 
            source_tree = (Object)adaptor.create(source);
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
        }
        return retval;
    }
    // $ANTLR end "select_assign"

    public static class select_expr_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "select_expr"
    // org/usergrid/persistence/query/tree/QueryFilter.g:237:1: select_expr : ( '*' | select_subject ( ',' select_subject )* | '{' select_assign ( ',' select_assign )* '}' ) ;
    public final QueryFilterParser.select_expr_return select_expr() throws RecognitionException {
        QueryFilterParser.select_expr_return retval = new QueryFilterParser.select_expr_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token char_literal37=null;
        Token char_literal39=null;
        Token char_literal41=null;
        Token char_literal43=null;
        Token char_literal45=null;
        QueryFilterParser.select_subject_return select_subject38 = null;

        QueryFilterParser.select_subject_return select_subject40 = null;

        QueryFilterParser.select_assign_return select_assign42 = null;

        QueryFilterParser.select_assign_return select_assign44 = null;


        Object char_literal37_tree=null;
        Object char_literal39_tree=null;
        Object char_literal41_tree=null;
        Object char_literal43_tree=null;
        Object char_literal45_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:238:3: ( ( '*' | select_subject ( ',' select_subject )* | '{' select_assign ( ',' select_assign )* '}' ) )
            // org/usergrid/persistence/query/tree/QueryFilter.g:238:5: ( '*' | select_subject ( ',' select_subject )* | '{' select_assign ( ',' select_assign )* '}' )
            {
            root_0 = (Object)adaptor.nil();

            // org/usergrid/persistence/query/tree/QueryFilter.g:238:5: ( '*' | select_subject ( ',' select_subject )* | '{' select_assign ( ',' select_assign )* '}' )
            int alt10=3;
            switch ( input.LA(1) ) {
            case 33:
                {
                alt10=1;
                }
                break;
            case ID:
                {
                alt10=2;
                }
                break;
            case 34:
                {
                alt10=3;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 10, 0, input);

                throw nvae;
            }

            switch (alt10) {
                case 1 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:238:6: '*'
                    {
                    char_literal37=(Token)match(input,33,FOLLOW_33_in_select_expr1233); 
                    char_literal37_tree = (Object)adaptor.create(char_literal37);
                    adaptor.addChild(root_0, char_literal37_tree);


                    }
                    break;
                case 2 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:238:12: select_subject ( ',' select_subject )*
                    {
                    pushFollow(FOLLOW_select_subject_in_select_expr1237);
                    select_subject38=select_subject();

                    state._fsp--;

                    adaptor.addChild(root_0, select_subject38.getTree());
                    // org/usergrid/persistence/query/tree/QueryFilter.g:238:27: ( ',' select_subject )*
                    loop8:
                    do {
                        int alt8=2;
                        int LA8_0 = input.LA(1);

                        if ( (LA8_0==23) ) {
                            alt8=1;
                        }


                        switch (alt8) {
                    	case 1 :
                    	    // org/usergrid/persistence/query/tree/QueryFilter.g:238:28: ',' select_subject
                    	    {
                    	    char_literal39=(Token)match(input,23,FOLLOW_23_in_select_expr1240); 
                    	    char_literal39_tree = (Object)adaptor.create(char_literal39);
                    	    adaptor.addChild(root_0, char_literal39_tree);

                    	    pushFollow(FOLLOW_select_subject_in_select_expr1242);
                    	    select_subject40=select_subject();

                    	    state._fsp--;

                    	    adaptor.addChild(root_0, select_subject40.getTree());

                    	    }
                    	    break;

                    	default :
                    	    break loop8;
                        }
                    } while (true);


                    }
                    break;
                case 3 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:238:52: '{' select_assign ( ',' select_assign )* '}'
                    {
                    char_literal41=(Token)match(input,34,FOLLOW_34_in_select_expr1249); 
                    char_literal41_tree = (Object)adaptor.create(char_literal41);
                    adaptor.addChild(root_0, char_literal41_tree);

                    pushFollow(FOLLOW_select_assign_in_select_expr1251);
                    select_assign42=select_assign();

                    state._fsp--;

                    adaptor.addChild(root_0, select_assign42.getTree());
                    // org/usergrid/persistence/query/tree/QueryFilter.g:238:70: ( ',' select_assign )*
                    loop9:
                    do {
                        int alt9=2;
                        int LA9_0 = input.LA(1);

                        if ( (LA9_0==23) ) {
                            alt9=1;
                        }


                        switch (alt9) {
                    	case 1 :
                    	    // org/usergrid/persistence/query/tree/QueryFilter.g:238:71: ',' select_assign
                    	    {
                    	    char_literal43=(Token)match(input,23,FOLLOW_23_in_select_expr1254); 
                    	    char_literal43_tree = (Object)adaptor.create(char_literal43);
                    	    adaptor.addChild(root_0, char_literal43_tree);

                    	    pushFollow(FOLLOW_select_assign_in_select_expr1256);
                    	    select_assign44=select_assign();

                    	    state._fsp--;

                    	    adaptor.addChild(root_0, select_assign44.getTree());

                    	    }
                    	    break;

                    	default :
                    	    break loop9;
                        }
                    } while (true);

                    char_literal45=(Token)match(input,35,FOLLOW_35_in_select_expr1261); 
                    char_literal45_tree = (Object)adaptor.create(char_literal45);
                    adaptor.addChild(root_0, char_literal45_tree);


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
    // org/usergrid/persistence/query/tree/QueryFilter.g:242:1: ql returns [Query q] : 'select' select_expr ( 'where' expression )? ( 'order by' order ( ',' order )* )? ;
    public final QueryFilterParser.ql_return ql() throws RecognitionException {
        QueryFilterParser.ql_return retval = new QueryFilterParser.ql_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token string_literal46=null;
        Token string_literal48=null;
        Token string_literal50=null;
        Token char_literal52=null;
        QueryFilterParser.select_expr_return select_expr47 = null;

        QueryFilterParser.expression_return expression49 = null;

        QueryFilterParser.order_return order51 = null;

        QueryFilterParser.order_return order53 = null;


        Object string_literal46_tree=null;
        Object string_literal48_tree=null;
        Object string_literal50_tree=null;
        Object char_literal52_tree=null;

        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:243:3: ( 'select' select_expr ( 'where' expression )? ( 'order by' order ( ',' order )* )? )
            // org/usergrid/persistence/query/tree/QueryFilter.g:243:5: 'select' select_expr ( 'where' expression )? ( 'order by' order ( ',' order )* )?
            {
            root_0 = (Object)adaptor.nil();

            string_literal46=(Token)match(input,36,FOLLOW_36_in_ql1283); 
            string_literal46_tree = (Object)adaptor.create(string_literal46);
            adaptor.addChild(root_0, string_literal46_tree);

            pushFollow(FOLLOW_select_expr_in_ql1285);
            select_expr47=select_expr();

            state._fsp--;

            adaptor.addChild(root_0, select_expr47.getTree());
            // org/usergrid/persistence/query/tree/QueryFilter.g:243:26: ( 'where' expression )?
            int alt11=2;
            int LA11_0 = input.LA(1);

            if ( (LA11_0==37) ) {
                alt11=1;
            }
            switch (alt11) {
                case 1 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:243:27: 'where' expression
                    {
                    string_literal48=(Token)match(input,37,FOLLOW_37_in_ql1288); 
                    string_literal48_tree = (Object)adaptor.create(string_literal48);
                    adaptor.addChild(root_0, string_literal48_tree);

                    pushFollow(FOLLOW_expression_in_ql1290);
                    expression49=expression();

                    state._fsp--;

                    adaptor.addChild(root_0, expression49.getTree());

                    }
                    break;

            }

            // org/usergrid/persistence/query/tree/QueryFilter.g:243:49: ( 'order by' order ( ',' order )* )?
            int alt13=2;
            int LA13_0 = input.LA(1);

            if ( (LA13_0==38) ) {
                alt13=1;
            }
            switch (alt13) {
                case 1 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:243:50: 'order by' order ( ',' order )*
                    {
                    string_literal50=(Token)match(input,38,FOLLOW_38_in_ql1296); 
                    string_literal50_tree = (Object)adaptor.create(string_literal50);
                    adaptor.addChild(root_0, string_literal50_tree);

                    pushFollow(FOLLOW_order_in_ql1298);
                    order51=order();

                    state._fsp--;

                    adaptor.addChild(root_0, order51.getTree());
                    // org/usergrid/persistence/query/tree/QueryFilter.g:243:67: ( ',' order )*
                    loop12:
                    do {
                        int alt12=2;
                        int LA12_0 = input.LA(1);

                        if ( (LA12_0==23) ) {
                            alt12=1;
                        }


                        switch (alt12) {
                    	case 1 :
                    	    // org/usergrid/persistence/query/tree/QueryFilter.g:243:68: ',' order
                    	    {
                    	    char_literal52=(Token)match(input,23,FOLLOW_23_in_ql1301); 
                    	    char_literal52_tree = (Object)adaptor.create(char_literal52);
                    	    adaptor.addChild(root_0, char_literal52_tree);

                    	    pushFollow(FOLLOW_order_in_ql1303);
                    	    order53=order();

                    	    state._fsp--;

                    	    adaptor.addChild(root_0, order53.getTree());

                    	    }
                    	    break;

                    	default :
                    	    break loop12;
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


 

    public static final BitSet FOLLOW_ID_in_property682 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_BOOLEAN_in_booleanliteral699 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_INT_in_intliteral717 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_UUID_in_uuidliteral734 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_STRING_in_stringliteral751 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_FLOAT_in_floatliteral770 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_booleanliteral_in_value790 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_intliteral_in_value798 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_uuidliteral_in_value806 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_stringliteral_in_value814 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_floatliteral_in_value822 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_property_in_equalityop854 = new BitSet(new long[]{0x0000000000010000L});
    public static final BitSet FOLLOW_LT_in_equalityop856 = new BitSet(new long[]{0x00000000000016A0L});
    public static final BitSet FOLLOW_value_in_equalityop860 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_property_in_equalityop869 = new BitSet(new long[]{0x0000000000020000L});
    public static final BitSet FOLLOW_LTE_in_equalityop871 = new BitSet(new long[]{0x00000000000016A0L});
    public static final BitSet FOLLOW_value_in_equalityop875 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_property_in_equalityop885 = new BitSet(new long[]{0x0000000000040000L});
    public static final BitSet FOLLOW_EQ_in_equalityop887 = new BitSet(new long[]{0x00000000000016A0L});
    public static final BitSet FOLLOW_value_in_equalityop891 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_property_in_equalityop900 = new BitSet(new long[]{0x0000000000080000L});
    public static final BitSet FOLLOW_GT_in_equalityop902 = new BitSet(new long[]{0x00000000000016A0L});
    public static final BitSet FOLLOW_value_in_equalityop906 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_property_in_equalityop915 = new BitSet(new long[]{0x0000000000100000L});
    public static final BitSet FOLLOW_GTE_in_equalityop917 = new BitSet(new long[]{0x00000000000016A0L});
    public static final BitSet FOLLOW_value_in_equalityop921 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_property_in_locationop943 = new BitSet(new long[]{0x0000000000200000L});
    public static final BitSet FOLLOW_21_in_locationop945 = new BitSet(new long[]{0x00000000000016A0L});
    public static final BitSet FOLLOW_floatliteral_in_locationop949 = new BitSet(new long[]{0x0000000000400000L});
    public static final BitSet FOLLOW_22_in_locationop951 = new BitSet(new long[]{0x00000000000016A0L});
    public static final BitSet FOLLOW_floatliteral_in_locationop955 = new BitSet(new long[]{0x0000000000800000L});
    public static final BitSet FOLLOW_23_in_locationop957 = new BitSet(new long[]{0x00000000000016A0L});
    public static final BitSet FOLLOW_floatliteral_in_locationop961 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_property_in_containsop983 = new BitSet(new long[]{0x0000000001000000L});
    public static final BitSet FOLLOW_24_in_containsop985 = new BitSet(new long[]{0x0000000000000200L});
    public static final BitSet FOLLOW_stringliteral_in_containsop989 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_25_in_operation1007 = new BitSet(new long[]{0x000000000A000010L});
    public static final BitSet FOLLOW_expression_in_operation1011 = new BitSet(new long[]{0x0000000004000000L});
    public static final BitSet FOLLOW_26_in_operation1013 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_equalityop_in_operation1023 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_locationop_in_operation1032 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_containsop_in_operation1041 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_27_in_notexp1061 = new BitSet(new long[]{0x000000000A000010L});
    public static final BitSet FOLLOW_operation_in_notexp1064 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_operation_in_notexp1071 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_notexp_in_andexp1091 = new BitSet(new long[]{0x0000000010000002L});
    public static final BitSet FOLLOW_28_in_andexp1094 = new BitSet(new long[]{0x000000000A000010L});
    public static final BitSet FOLLOW_notexp_in_andexp1098 = new BitSet(new long[]{0x0000000010000002L});
    public static final BitSet FOLLOW_andexp_in_orexp1119 = new BitSet(new long[]{0x0000000020000002L});
    public static final BitSet FOLLOW_29_in_orexp1122 = new BitSet(new long[]{0x000000000A000010L});
    public static final BitSet FOLLOW_andexp_in_orexp1126 = new BitSet(new long[]{0x0000000020000002L});
    public static final BitSet FOLLOW_orexp_in_expression1144 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_set_in_direction1158 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_property_in_order1176 = new BitSet(new long[]{0x00000000C0000002L});
    public static final BitSet FOLLOW_direction_in_order1178 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_select_subject1196 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_select_assign1213 = new BitSet(new long[]{0x0000000100000000L});
    public static final BitSet FOLLOW_32_in_select_assign1215 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_ID_in_select_assign1219 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_33_in_select_expr1233 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_select_subject_in_select_expr1237 = new BitSet(new long[]{0x0000000000800002L});
    public static final BitSet FOLLOW_23_in_select_expr1240 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_select_subject_in_select_expr1242 = new BitSet(new long[]{0x0000000000800002L});
    public static final BitSet FOLLOW_34_in_select_expr1249 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_select_assign_in_select_expr1251 = new BitSet(new long[]{0x0000000800800000L});
    public static final BitSet FOLLOW_23_in_select_expr1254 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_select_assign_in_select_expr1256 = new BitSet(new long[]{0x0000000800800000L});
    public static final BitSet FOLLOW_35_in_select_expr1261 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_36_in_ql1283 = new BitSet(new long[]{0x0000000600000010L});
    public static final BitSet FOLLOW_select_expr_in_ql1285 = new BitSet(new long[]{0x0000006000000002L});
    public static final BitSet FOLLOW_37_in_ql1288 = new BitSet(new long[]{0x000000000A000010L});
    public static final BitSet FOLLOW_expression_in_ql1290 = new BitSet(new long[]{0x0000004000000002L});
    public static final BitSet FOLLOW_38_in_ql1296 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_order_in_ql1298 = new BitSet(new long[]{0x0000000000800002L});
    public static final BitSet FOLLOW_23_in_ql1301 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_order_in_ql1303 = new BitSet(new long[]{0x0000000000800002L});

}