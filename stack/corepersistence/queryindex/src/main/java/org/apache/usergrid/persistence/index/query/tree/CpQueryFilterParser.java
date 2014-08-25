// $ANTLR 3.4 org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g 2014-08-25 10:56:14

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
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.persistence.index.query.Query.SortPredicate;



import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

import org.antlr.runtime.tree.*;


@SuppressWarnings({"all", "warnings", "unchecked"})
public class CpQueryFilterParser extends Parser {
    public static final String[] tokenNames = new String[] {
        "<invalid>", "<EOR>", "<DOWN>", "<UP>", "AND", "ASC", "BOOLEAN", "CONTAINS", "DESC", "EQ", "ESC_SEQ", "EXPONENT", "FALSE", "FLOAT", "GT", "GTE", "HEX_DIGIT", "ID", "LONG", "LT", "LTE", "NOT", "OCTAL_ESC", "OF", "OR", "STRING", "TRUE", "UNICODE_ESC", "UUID", "WITHIN", "WS", "'('", "')'", "'*'", "','", "':'", "'order by'", "'select'", "'where'", "'{'", "'}'"
    };

    public static final int EOF=-1;
    public static final int T__31=31;
    public static final int T__32=32;
    public static final int T__33=33;
    public static final int T__34=34;
    public static final int T__35=35;
    public static final int T__36=36;
    public static final int T__37=37;
    public static final int T__38=38;
    public static final int T__39=39;
    public static final int T__40=40;
    public static final int AND=4;
    public static final int ASC=5;
    public static final int BOOLEAN=6;
    public static final int CONTAINS=7;
    public static final int DESC=8;
    public static final int EQ=9;
    public static final int ESC_SEQ=10;
    public static final int EXPONENT=11;
    public static final int FALSE=12;
    public static final int FLOAT=13;
    public static final int GT=14;
    public static final int GTE=15;
    public static final int HEX_DIGIT=16;
    public static final int ID=17;
    public static final int LONG=18;
    public static final int LT=19;
    public static final int LTE=20;
    public static final int NOT=21;
    public static final int OCTAL_ESC=22;
    public static final int OF=23;
    public static final int OR=24;
    public static final int STRING=25;
    public static final int TRUE=26;
    public static final int UNICODE_ESC=27;
    public static final int UUID=28;
    public static final int WITHIN=29;
    public static final int WS=30;

    // delegates
    public Parser[] getDelegates() {
        return new Parser[] {};
    }

    // delegators


    public CpQueryFilterParser(TokenStream input) {
        this(input, new RecognizerSharedState());
    }
    public CpQueryFilterParser(TokenStream input, RecognizerSharedState state) {
        super(input, state);
    }

protected TreeAdaptor adaptor = new CommonTreeAdaptor();

public void setTreeAdaptor(TreeAdaptor adaptor) {
    this.adaptor = adaptor;
}
public TreeAdaptor getTreeAdaptor() {
    return adaptor;
}
    public String[] getTokenNames() { return CpQueryFilterParser.tokenNames; }
    public String getGrammarFileName() { return "org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g"; }


    	Query query = new Query();

      private static final Logger logger = LoggerFactory
          .getLogger(CpQueryFilterLexer.class);

    	@Override
    	public void emitErrorMessage(String msg) {
    		logger.info(msg);
    	}


    public static class property_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "property"
    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:209:1: property : ID ;
    public final CpQueryFilterParser.property_return property() throws RecognitionException {
        CpQueryFilterParser.property_return retval = new CpQueryFilterParser.property_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token ID1=null;

        Object ID1_tree=null;

        try {
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:209:10: ( ID )
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:209:12: ID
            {
            root_0 = (Object)adaptor.nil();


            ID1=(Token)match(input,ID,FOLLOW_ID_in_property991); 
            ID1_tree = 
            new Property(ID1) 
            ;
            adaptor.addChild(root_0, ID1_tree);


            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
         
        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "property"


    public static class containsproperty_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "containsproperty"
    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:211:1: containsproperty : ID ;
    public final CpQueryFilterParser.containsproperty_return containsproperty() throws RecognitionException {
        CpQueryFilterParser.containsproperty_return retval = new CpQueryFilterParser.containsproperty_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token ID2=null;

        Object ID2_tree=null;

        try {
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:211:18: ( ID )
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:211:20: ID
            {
            root_0 = (Object)adaptor.nil();


            ID2=(Token)match(input,ID,FOLLOW_ID_in_containsproperty1002); 
            ID2_tree = 
            new ContainsProperty(ID2) 
            ;
            adaptor.addChild(root_0, ID2_tree);


            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
         
        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "containsproperty"


    public static class withinproperty_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "withinproperty"
    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:213:1: withinproperty : ID ;
    public final CpQueryFilterParser.withinproperty_return withinproperty() throws RecognitionException {
        CpQueryFilterParser.withinproperty_return retval = new CpQueryFilterParser.withinproperty_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token ID3=null;

        Object ID3_tree=null;

        try {
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:213:16: ( ID )
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:213:18: ID
            {
            root_0 = (Object)adaptor.nil();


            ID3=(Token)match(input,ID,FOLLOW_ID_in_withinproperty1013); 
            ID3_tree = 
            new WithinProperty(ID3) 
            ;
            adaptor.addChild(root_0, ID3_tree);


            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
         
        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "withinproperty"


    public static class booleanliteral_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "booleanliteral"
    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:215:1: booleanliteral : BOOLEAN ;
    public final CpQueryFilterParser.booleanliteral_return booleanliteral() throws RecognitionException {
        CpQueryFilterParser.booleanliteral_return retval = new CpQueryFilterParser.booleanliteral_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token BOOLEAN4=null;

        Object BOOLEAN4_tree=null;

        try {
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:215:15: ( BOOLEAN )
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:215:17: BOOLEAN
            {
            root_0 = (Object)adaptor.nil();


            BOOLEAN4=(Token)match(input,BOOLEAN,FOLLOW_BOOLEAN_in_booleanliteral1024); 
            BOOLEAN4_tree = 
            new BooleanLiteral(BOOLEAN4) 
            ;
            adaptor.addChild(root_0, BOOLEAN4_tree);


            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
         
        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "booleanliteral"


    public static class longliteral_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "longliteral"
    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:218:1: longliteral : LONG ;
    public final CpQueryFilterParser.longliteral_return longliteral() throws RecognitionException {
        CpQueryFilterParser.longliteral_return retval = new CpQueryFilterParser.longliteral_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token LONG5=null;

        Object LONG5_tree=null;

        try {
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:218:13: ( LONG )
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:219:3: LONG
            {
            root_0 = (Object)adaptor.nil();


            LONG5=(Token)match(input,LONG,FOLLOW_LONG_in_longliteral1038); 
            LONG5_tree = 
            new LongLiteral(LONG5) 
            ;
            adaptor.addChild(root_0, LONG5_tree);


            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
         
        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "longliteral"


    public static class uuidliteral_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "uuidliteral"
    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:221:1: uuidliteral : UUID ;
    public final CpQueryFilterParser.uuidliteral_return uuidliteral() throws RecognitionException {
        CpQueryFilterParser.uuidliteral_return retval = new CpQueryFilterParser.uuidliteral_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token UUID6=null;

        Object UUID6_tree=null;

        try {
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:221:13: ( UUID )
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:222:3: UUID
            {
            root_0 = (Object)adaptor.nil();


            UUID6=(Token)match(input,UUID,FOLLOW_UUID_in_uuidliteral1052); 
            UUID6_tree = 
            new UUIDLiteral(UUID6) 
            ;
            adaptor.addChild(root_0, UUID6_tree);


            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

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
    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:224:1: stringliteral : STRING ;
    public final CpQueryFilterParser.stringliteral_return stringliteral() throws RecognitionException {
        CpQueryFilterParser.stringliteral_return retval = new CpQueryFilterParser.stringliteral_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token STRING7=null;

        Object STRING7_tree=null;

        try {
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:224:15: ( STRING )
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:225:3: STRING
            {
            root_0 = (Object)adaptor.nil();


            STRING7=(Token)match(input,STRING,FOLLOW_STRING_in_stringliteral1065); 
            STRING7_tree = 
            new StringLiteral(STRING7) 
            ;
            adaptor.addChild(root_0, STRING7_tree);


            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

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
    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:227:1: floatliteral : FLOAT ;
    public final CpQueryFilterParser.floatliteral_return floatliteral() throws RecognitionException {
        CpQueryFilterParser.floatliteral_return retval = new CpQueryFilterParser.floatliteral_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token FLOAT8=null;

        Object FLOAT8_tree=null;

        try {
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:227:14: ( FLOAT )
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:228:3: FLOAT
            {
            root_0 = (Object)adaptor.nil();


            FLOAT8=(Token)match(input,FLOAT,FOLLOW_FLOAT_in_floatliteral1080); 
            FLOAT8_tree = 
            new FloatLiteral(FLOAT8) 
            ;
            adaptor.addChild(root_0, FLOAT8_tree);


            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

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
    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:231:1: value : ( booleanliteral | longliteral | uuidliteral | stringliteral | floatliteral );
    public final CpQueryFilterParser.value_return value() throws RecognitionException {
        CpQueryFilterParser.value_return retval = new CpQueryFilterParser.value_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        CpQueryFilterParser.booleanliteral_return booleanliteral9 =null;

        CpQueryFilterParser.longliteral_return longliteral10 =null;

        CpQueryFilterParser.uuidliteral_return uuidliteral11 =null;

        CpQueryFilterParser.stringliteral_return stringliteral12 =null;

        CpQueryFilterParser.floatliteral_return floatliteral13 =null;



        try {
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:231:7: ( booleanliteral | longliteral | uuidliteral | stringliteral | floatliteral )
            int alt1=5;
            switch ( input.LA(1) ) {
            case BOOLEAN:
                {
                alt1=1;
                }
                break;
            case LONG:
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
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:232:3: booleanliteral
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_booleanliteral_in_value1096);
                    booleanliteral9=booleanliteral();

                    state._fsp--;

                    adaptor.addChild(root_0, booleanliteral9.getTree());

                    }
                    break;
                case 2 :
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:233:5: longliteral
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_longliteral_in_value1102);
                    longliteral10=longliteral();

                    state._fsp--;

                    adaptor.addChild(root_0, longliteral10.getTree());

                    }
                    break;
                case 3 :
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:234:5: uuidliteral
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_uuidliteral_in_value1108);
                    uuidliteral11=uuidliteral();

                    state._fsp--;

                    adaptor.addChild(root_0, uuidliteral11.getTree());

                    }
                    break;
                case 4 :
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:235:5: stringliteral
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_stringliteral_in_value1114);
                    stringliteral12=stringliteral();

                    state._fsp--;

                    adaptor.addChild(root_0, stringliteral12.getTree());

                    }
                    break;
                case 5 :
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:236:5: floatliteral
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_floatliteral_in_value1120);
                    floatliteral13=floatliteral();

                    state._fsp--;

                    adaptor.addChild(root_0, floatliteral13.getTree());

                    }
                    break;

            }
            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

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
    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:247:1: equalityop : ( property LT ^ value | property LTE ^ value | property EQ ^ value | property GT ^ value | property GTE ^ value );
    public final CpQueryFilterParser.equalityop_return equalityop() throws RecognitionException {
        CpQueryFilterParser.equalityop_return retval = new CpQueryFilterParser.equalityop_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token LT15=null;
        Token LTE18=null;
        Token EQ21=null;
        Token GT24=null;
        Token GTE27=null;
        CpQueryFilterParser.property_return property14 =null;

        CpQueryFilterParser.value_return value16 =null;

        CpQueryFilterParser.property_return property17 =null;

        CpQueryFilterParser.value_return value19 =null;

        CpQueryFilterParser.property_return property20 =null;

        CpQueryFilterParser.value_return value22 =null;

        CpQueryFilterParser.property_return property23 =null;

        CpQueryFilterParser.value_return value25 =null;

        CpQueryFilterParser.property_return property26 =null;

        CpQueryFilterParser.value_return value28 =null;


        Object LT15_tree=null;
        Object LTE18_tree=null;
        Object EQ21_tree=null;
        Object GT24_tree=null;
        Object GTE27_tree=null;

        try {
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:247:12: ( property LT ^ value | property LTE ^ value | property EQ ^ value | property GT ^ value | property GTE ^ value )
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
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:248:3: property LT ^ value
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_property_in_equalityop1145);
                    property14=property();

                    state._fsp--;

                    adaptor.addChild(root_0, property14.getTree());

                    LT15=(Token)match(input,LT,FOLLOW_LT_in_equalityop1147); 
                    LT15_tree = 
                    new LessThan(LT15) 
                    ;
                    root_0 = (Object)adaptor.becomeRoot(LT15_tree, root_0);


                    pushFollow(FOLLOW_value_in_equalityop1153);
                    value16=value();

                    state._fsp--;

                    adaptor.addChild(root_0, value16.getTree());

                    }
                    break;
                case 2 :
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:249:4: property LTE ^ value
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_property_in_equalityop1158);
                    property17=property();

                    state._fsp--;

                    adaptor.addChild(root_0, property17.getTree());

                    LTE18=(Token)match(input,LTE,FOLLOW_LTE_in_equalityop1160); 
                    LTE18_tree = 
                    new LessThanEqual(LTE18) 
                    ;
                    root_0 = (Object)adaptor.becomeRoot(LTE18_tree, root_0);


                    pushFollow(FOLLOW_value_in_equalityop1166);
                    value19=value();

                    state._fsp--;

                    adaptor.addChild(root_0, value19.getTree());

                    }
                    break;
                case 3 :
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:250:4: property EQ ^ value
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_property_in_equalityop1171);
                    property20=property();

                    state._fsp--;

                    adaptor.addChild(root_0, property20.getTree());

                    EQ21=(Token)match(input,EQ,FOLLOW_EQ_in_equalityop1173); 
                    EQ21_tree = 
                    new Equal(EQ21) 
                    ;
                    root_0 = (Object)adaptor.becomeRoot(EQ21_tree, root_0);


                    pushFollow(FOLLOW_value_in_equalityop1179);
                    value22=value();

                    state._fsp--;

                    adaptor.addChild(root_0, value22.getTree());

                    }
                    break;
                case 4 :
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:251:4: property GT ^ value
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_property_in_equalityop1184);
                    property23=property();

                    state._fsp--;

                    adaptor.addChild(root_0, property23.getTree());

                    GT24=(Token)match(input,GT,FOLLOW_GT_in_equalityop1186); 
                    GT24_tree = 
                    new GreaterThan(GT24) 
                    ;
                    root_0 = (Object)adaptor.becomeRoot(GT24_tree, root_0);


                    pushFollow(FOLLOW_value_in_equalityop1192);
                    value25=value();

                    state._fsp--;

                    adaptor.addChild(root_0, value25.getTree());

                    }
                    break;
                case 5 :
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:252:4: property GTE ^ value
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_property_in_equalityop1197);
                    property26=property();

                    state._fsp--;

                    adaptor.addChild(root_0, property26.getTree());

                    GTE27=(Token)match(input,GTE,FOLLOW_GTE_in_equalityop1199); 
                    GTE27_tree = 
                    new GreaterThanEqual(GTE27) 
                    ;
                    root_0 = (Object)adaptor.becomeRoot(GTE27_tree, root_0);


                    pushFollow(FOLLOW_value_in_equalityop1205);
                    value28=value();

                    state._fsp--;

                    adaptor.addChild(root_0, value28.getTree());

                    }
                    break;

            }
            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

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
    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:256:1: locationop : withinproperty WITHIN ^ ( floatliteral | longliteral ) OF ! ( floatliteral | longliteral ) ',' ! ( floatliteral | longliteral ) ;
    public final CpQueryFilterParser.locationop_return locationop() throws RecognitionException {
        CpQueryFilterParser.locationop_return retval = new CpQueryFilterParser.locationop_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token WITHIN30=null;
        Token OF33=null;
        Token char_literal36=null;
        CpQueryFilterParser.withinproperty_return withinproperty29 =null;

        CpQueryFilterParser.floatliteral_return floatliteral31 =null;

        CpQueryFilterParser.longliteral_return longliteral32 =null;

        CpQueryFilterParser.floatliteral_return floatliteral34 =null;

        CpQueryFilterParser.longliteral_return longliteral35 =null;

        CpQueryFilterParser.floatliteral_return floatliteral37 =null;

        CpQueryFilterParser.longliteral_return longliteral38 =null;


        Object WITHIN30_tree=null;
        Object OF33_tree=null;
        Object char_literal36_tree=null;

        try {
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:256:12: ( withinproperty WITHIN ^ ( floatliteral | longliteral ) OF ! ( floatliteral | longliteral ) ',' ! ( floatliteral | longliteral ) )
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:257:3: withinproperty WITHIN ^ ( floatliteral | longliteral ) OF ! ( floatliteral | longliteral ) ',' ! ( floatliteral | longliteral )
            {
            root_0 = (Object)adaptor.nil();


            pushFollow(FOLLOW_withinproperty_in_locationop1220);
            withinproperty29=withinproperty();

            state._fsp--;

            adaptor.addChild(root_0, withinproperty29.getTree());

            WITHIN30=(Token)match(input,WITHIN,FOLLOW_WITHIN_in_locationop1222); 
            WITHIN30_tree = 
            new WithinOperand(WITHIN30) 
            ;
            root_0 = (Object)adaptor.becomeRoot(WITHIN30_tree, root_0);


            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:257:41: ( floatliteral | longliteral )
            int alt3=2;
            switch ( input.LA(1) ) {
            case FLOAT:
                {
                alt3=1;
                }
                break;
            case LONG:
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
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:257:42: floatliteral
                    {
                    pushFollow(FOLLOW_floatliteral_in_locationop1229);
                    floatliteral31=floatliteral();

                    state._fsp--;

                    adaptor.addChild(root_0, floatliteral31.getTree());

                    }
                    break;
                case 2 :
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:257:55: longliteral
                    {
                    pushFollow(FOLLOW_longliteral_in_locationop1231);
                    longliteral32=longliteral();

                    state._fsp--;

                    adaptor.addChild(root_0, longliteral32.getTree());

                    }
                    break;

            }


            OF33=(Token)match(input,OF,FOLLOW_OF_in_locationop1234); 

            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:257:72: ( floatliteral | longliteral )
            int alt4=2;
            switch ( input.LA(1) ) {
            case FLOAT:
                {
                alt4=1;
                }
                break;
            case LONG:
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
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:257:73: floatliteral
                    {
                    pushFollow(FOLLOW_floatliteral_in_locationop1238);
                    floatliteral34=floatliteral();

                    state._fsp--;

                    adaptor.addChild(root_0, floatliteral34.getTree());

                    }
                    break;
                case 2 :
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:257:86: longliteral
                    {
                    pushFollow(FOLLOW_longliteral_in_locationop1240);
                    longliteral35=longliteral();

                    state._fsp--;

                    adaptor.addChild(root_0, longliteral35.getTree());

                    }
                    break;

            }


            char_literal36=(Token)match(input,34,FOLLOW_34_in_locationop1243); 

            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:257:104: ( floatliteral | longliteral )
            int alt5=2;
            switch ( input.LA(1) ) {
            case FLOAT:
                {
                alt5=1;
                }
                break;
            case LONG:
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
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:257:105: floatliteral
                    {
                    pushFollow(FOLLOW_floatliteral_in_locationop1247);
                    floatliteral37=floatliteral();

                    state._fsp--;

                    adaptor.addChild(root_0, floatliteral37.getTree());

                    }
                    break;
                case 2 :
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:257:118: longliteral
                    {
                    pushFollow(FOLLOW_longliteral_in_locationop1249);
                    longliteral38=longliteral();

                    state._fsp--;

                    adaptor.addChild(root_0, longliteral38.getTree());

                    }
                    break;

            }


            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

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
    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:260:1: containsop : containsproperty CONTAINS ^ stringliteral ;
    public final CpQueryFilterParser.containsop_return containsop() throws RecognitionException {
        CpQueryFilterParser.containsop_return retval = new CpQueryFilterParser.containsop_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token CONTAINS40=null;
        CpQueryFilterParser.containsproperty_return containsproperty39 =null;

        CpQueryFilterParser.stringliteral_return stringliteral41 =null;


        Object CONTAINS40_tree=null;

        try {
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:260:12: ( containsproperty CONTAINS ^ stringliteral )
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:261:3: containsproperty CONTAINS ^ stringliteral
            {
            root_0 = (Object)adaptor.nil();


            pushFollow(FOLLOW_containsproperty_in_containsop1263);
            containsproperty39=containsproperty();

            state._fsp--;

            adaptor.addChild(root_0, containsproperty39.getTree());

            CONTAINS40=(Token)match(input,CONTAINS,FOLLOW_CONTAINS_in_containsop1265); 
            CONTAINS40_tree = 
            new ContainsOperand(CONTAINS40) 
            ;
            root_0 = (Object)adaptor.becomeRoot(CONTAINS40_tree, root_0);


            pushFollow(FOLLOW_stringliteral_in_containsop1271);
            stringliteral41=stringliteral();

            state._fsp--;

            adaptor.addChild(root_0, stringliteral41.getTree());

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

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
    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:264:1: operation : ( '(' ! expression ')' !| equalityop | locationop | containsop );
    public final CpQueryFilterParser.operation_return operation() throws RecognitionException {
        CpQueryFilterParser.operation_return retval = new CpQueryFilterParser.operation_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token char_literal42=null;
        Token char_literal44=null;
        CpQueryFilterParser.expression_return expression43 =null;

        CpQueryFilterParser.equalityop_return equalityop45 =null;

        CpQueryFilterParser.locationop_return locationop46 =null;

        CpQueryFilterParser.containsop_return containsop47 =null;


        Object char_literal42_tree=null;
        Object char_literal44_tree=null;

        try {
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:264:11: ( '(' ! expression ')' !| equalityop | locationop | containsop )
            int alt6=4;
            switch ( input.LA(1) ) {
            case 31:
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
                case WITHIN:
                    {
                    alt6=3;
                    }
                    break;
                case CONTAINS:
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
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:265:2: '(' ! expression ')' !
                    {
                    root_0 = (Object)adaptor.nil();


                    char_literal42=(Token)match(input,31,FOLLOW_31_in_operation1281); 

                    pushFollow(FOLLOW_expression_in_operation1284);
                    expression43=expression();

                    state._fsp--;

                    adaptor.addChild(root_0, expression43.getTree());

                    char_literal44=(Token)match(input,32,FOLLOW_32_in_operation1286); 

                    }
                    break;
                case 2 :
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:266:6: equalityop
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_equalityop_in_operation1294);
                    equalityop45=equalityop();

                    state._fsp--;

                    adaptor.addChild(root_0, equalityop45.getTree());

                    }
                    break;
                case 3 :
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:267:6: locationop
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_locationop_in_operation1302);
                    locationop46=locationop();

                    state._fsp--;

                    adaptor.addChild(root_0, locationop46.getTree());

                    }
                    break;
                case 4 :
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:268:6: containsop
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_containsop_in_operation1310);
                    containsop47=containsop();

                    state._fsp--;

                    adaptor.addChild(root_0, containsop47.getTree());

                    }
                    break;

            }
            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

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
    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:272:1: notexp : ( NOT ^ operation | operation );
    public final CpQueryFilterParser.notexp_return notexp() throws RecognitionException {
        CpQueryFilterParser.notexp_return retval = new CpQueryFilterParser.notexp_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token NOT48=null;
        CpQueryFilterParser.operation_return operation49 =null;

        CpQueryFilterParser.operation_return operation50 =null;


        Object NOT48_tree=null;

        try {
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:272:8: ( NOT ^ operation | operation )
            int alt7=2;
            switch ( input.LA(1) ) {
            case NOT:
                {
                alt7=1;
                }
                break;
            case ID:
            case 31:
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
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:274:2: NOT ^ operation
                    {
                    root_0 = (Object)adaptor.nil();


                    NOT48=(Token)match(input,NOT,FOLLOW_NOT_in_notexp1326); 
                    NOT48_tree = 
                    new NotOperand(NOT48) 
                    ;
                    root_0 = (Object)adaptor.becomeRoot(NOT48_tree, root_0);


                    pushFollow(FOLLOW_operation_in_notexp1332);
                    operation49=operation();

                    state._fsp--;

                    adaptor.addChild(root_0, operation49.getTree());

                    }
                    break;
                case 2 :
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:275:3: operation
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_operation_in_notexp1338);
                    operation50=operation();

                    state._fsp--;

                    adaptor.addChild(root_0, operation50.getTree());

                    }
                    break;

            }
            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

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
    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:280:1: andexp : notexp ( AND ^ notexp )* ;
    public final CpQueryFilterParser.andexp_return andexp() throws RecognitionException {
        CpQueryFilterParser.andexp_return retval = new CpQueryFilterParser.andexp_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token AND52=null;
        CpQueryFilterParser.notexp_return notexp51 =null;

        CpQueryFilterParser.notexp_return notexp53 =null;


        Object AND52_tree=null;

        try {
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:280:8: ( notexp ( AND ^ notexp )* )
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:281:2: notexp ( AND ^ notexp )*
            {
            root_0 = (Object)adaptor.nil();


            pushFollow(FOLLOW_notexp_in_andexp1352);
            notexp51=notexp();

            state._fsp--;

            adaptor.addChild(root_0, notexp51.getTree());

            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:281:9: ( AND ^ notexp )*
            loop8:
            do {
                int alt8=2;
                switch ( input.LA(1) ) {
                case AND:
                    {
                    alt8=1;
                    }
                    break;

                }

                switch (alt8) {
            	case 1 :
            	    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:281:10: AND ^ notexp
            	    {
            	    AND52=(Token)match(input,AND,FOLLOW_AND_in_andexp1355); 
            	    AND52_tree = 
            	    new AndOperand(AND52) 
            	    ;
            	    root_0 = (Object)adaptor.becomeRoot(AND52_tree, root_0);


            	    pushFollow(FOLLOW_notexp_in_andexp1361);
            	    notexp53=notexp();

            	    state._fsp--;

            	    adaptor.addChild(root_0, notexp53.getTree());

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
    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:286:1: expression : andexp ( OR ^ andexp )* ;
    public final CpQueryFilterParser.expression_return expression() throws RecognitionException {
        CpQueryFilterParser.expression_return retval = new CpQueryFilterParser.expression_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token OR55=null;
        CpQueryFilterParser.andexp_return andexp54 =null;

        CpQueryFilterParser.andexp_return andexp56 =null;


        Object OR55_tree=null;

        try {
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:286:12: ( andexp ( OR ^ andexp )* )
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:287:2: andexp ( OR ^ andexp )*
            {
            root_0 = (Object)adaptor.nil();


            pushFollow(FOLLOW_andexp_in_expression1378);
            andexp54=andexp();

            state._fsp--;

            adaptor.addChild(root_0, andexp54.getTree());

            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:287:9: ( OR ^ andexp )*
            loop9:
            do {
                int alt9=2;
                switch ( input.LA(1) ) {
                case OR:
                    {
                    alt9=1;
                    }
                    break;

                }

                switch (alt9) {
            	case 1 :
            	    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:287:10: OR ^ andexp
            	    {
            	    OR55=(Token)match(input,OR,FOLLOW_OR_in_expression1381); 
            	    OR55_tree = 
            	    new OrOperand(OR55) 
            	    ;
            	    root_0 = (Object)adaptor.becomeRoot(OR55_tree, root_0);


            	    pushFollow(FOLLOW_andexp_in_expression1387);
            	    andexp56=andexp();

            	    state._fsp--;

            	    adaptor.addChild(root_0, andexp56.getTree());

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
    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:296:1: direction : ( ASC | DESC ) ;
    public final CpQueryFilterParser.direction_return direction() throws RecognitionException {
        CpQueryFilterParser.direction_return retval = new CpQueryFilterParser.direction_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token set57=null;

        Object set57_tree=null;

        try {
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:296:12: ( ( ASC | DESC ) )
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:
            {
            root_0 = (Object)adaptor.nil();


            set57=(Token)input.LT(1);

            if ( input.LA(1)==ASC||input.LA(1)==DESC ) {
                input.consume();
                adaptor.addChild(root_0, 
                (Object)adaptor.create(set57)
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
    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:299:1: order : ( property ( direction )? ) ;
    public final CpQueryFilterParser.order_return order() throws RecognitionException {
        CpQueryFilterParser.order_return retval = new CpQueryFilterParser.order_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        CpQueryFilterParser.property_return property58 =null;

        CpQueryFilterParser.direction_return direction59 =null;



        try {
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:300:3: ( ( property ( direction )? ) )
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:300:5: ( property ( direction )? )
            {
            root_0 = (Object)adaptor.nil();


            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:300:5: ( property ( direction )? )
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:300:6: property ( direction )?
            {
            pushFollow(FOLLOW_property_in_order1424);
            property58=property();

            state._fsp--;

            adaptor.addChild(root_0, property58.getTree());

            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:300:15: ( direction )?
            int alt10=2;
            switch ( input.LA(1) ) {
                case ASC:
                case DESC:
                    {
                    alt10=1;
                    }
                    break;
            }

            switch (alt10) {
                case 1 :
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:300:15: direction
                    {
                    pushFollow(FOLLOW_direction_in_order1426);
                    direction59=direction();

                    state._fsp--;

                    adaptor.addChild(root_0, direction59.getTree());

                    }
                    break;

            }


            }



            		String property = (property58!=null?input.toString(property58.start,property58.stop):null); 
            		String direction = (direction59!=null?input.toString(direction59.start,direction59.stop):null);
            		query.addSort(new SortPredicate(property, direction));
                
              

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

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
    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:311:1: select_subject : ID ;
    public final CpQueryFilterParser.select_subject_return select_subject() throws RecognitionException {
        CpQueryFilterParser.select_subject_return retval = new CpQueryFilterParser.select_subject_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token ID60=null;

        Object ID60_tree=null;

        try {
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:312:3: ( ID )
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:312:5: ID
            {
            root_0 = (Object)adaptor.nil();


            ID60=(Token)match(input,ID,FOLLOW_ID_in_select_subject1445); 
            ID60_tree = 
            (Object)adaptor.create(ID60)
            ;
            adaptor.addChild(root_0, ID60_tree);




              query.addSelect((ID60!=null?ID60.getText():null));



            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

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
    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:320:1: select_assign : target= ID ':' source= ID ;
    public final CpQueryFilterParser.select_assign_return select_assign() throws RecognitionException {
        CpQueryFilterParser.select_assign_return retval = new CpQueryFilterParser.select_assign_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token target=null;
        Token source=null;
        Token char_literal61=null;

        Object target_tree=null;
        Object source_tree=null;
        Object char_literal61_tree=null;

        try {
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:321:3: (target= ID ':' source= ID )
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:321:5: target= ID ':' source= ID
            {
            root_0 = (Object)adaptor.nil();


            target=(Token)match(input,ID,FOLLOW_ID_in_select_assign1462); 
            target_tree = 
            (Object)adaptor.create(target)
            ;
            adaptor.addChild(root_0, target_tree);


            char_literal61=(Token)match(input,35,FOLLOW_35_in_select_assign1464); 
            char_literal61_tree = 
            (Object)adaptor.create(char_literal61)
            ;
            adaptor.addChild(root_0, char_literal61_tree);


            source=(Token)match(input,ID,FOLLOW_ID_in_select_assign1468); 
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
    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:327:1: select_expr : ( '*' | select_subject ( ',' select_subject )* | '{' select_assign ( ',' select_assign )* '}' ) ;
    public final CpQueryFilterParser.select_expr_return select_expr() throws RecognitionException {
        CpQueryFilterParser.select_expr_return retval = new CpQueryFilterParser.select_expr_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token char_literal62=null;
        Token char_literal64=null;
        Token char_literal66=null;
        Token char_literal68=null;
        Token char_literal70=null;
        CpQueryFilterParser.select_subject_return select_subject63 =null;

        CpQueryFilterParser.select_subject_return select_subject65 =null;

        CpQueryFilterParser.select_assign_return select_assign67 =null;

        CpQueryFilterParser.select_assign_return select_assign69 =null;


        Object char_literal62_tree=null;
        Object char_literal64_tree=null;
        Object char_literal66_tree=null;
        Object char_literal68_tree=null;
        Object char_literal70_tree=null;

        try {
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:328:3: ( ( '*' | select_subject ( ',' select_subject )* | '{' select_assign ( ',' select_assign )* '}' ) )
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:328:5: ( '*' | select_subject ( ',' select_subject )* | '{' select_assign ( ',' select_assign )* '}' )
            {
            root_0 = (Object)adaptor.nil();


            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:328:5: ( '*' | select_subject ( ',' select_subject )* | '{' select_assign ( ',' select_assign )* '}' )
            int alt13=3;
            switch ( input.LA(1) ) {
            case 33:
                {
                alt13=1;
                }
                break;
            case ID:
                {
                alt13=2;
                }
                break;
            case 39:
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
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:328:6: '*'
                    {
                    char_literal62=(Token)match(input,33,FOLLOW_33_in_select_expr1482); 
                    char_literal62_tree = 
                    (Object)adaptor.create(char_literal62)
                    ;
                    adaptor.addChild(root_0, char_literal62_tree);


                    }
                    break;
                case 2 :
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:328:12: select_subject ( ',' select_subject )*
                    {
                    pushFollow(FOLLOW_select_subject_in_select_expr1486);
                    select_subject63=select_subject();

                    state._fsp--;

                    adaptor.addChild(root_0, select_subject63.getTree());

                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:328:27: ( ',' select_subject )*
                    loop11:
                    do {
                        int alt11=2;
                        switch ( input.LA(1) ) {
                        case 34:
                            {
                            alt11=1;
                            }
                            break;

                        }

                        switch (alt11) {
                    	case 1 :
                    	    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:328:28: ',' select_subject
                    	    {
                    	    char_literal64=(Token)match(input,34,FOLLOW_34_in_select_expr1489); 
                    	    char_literal64_tree = 
                    	    (Object)adaptor.create(char_literal64)
                    	    ;
                    	    adaptor.addChild(root_0, char_literal64_tree);


                    	    pushFollow(FOLLOW_select_subject_in_select_expr1491);
                    	    select_subject65=select_subject();

                    	    state._fsp--;

                    	    adaptor.addChild(root_0, select_subject65.getTree());

                    	    }
                    	    break;

                    	default :
                    	    break loop11;
                        }
                    } while (true);


                    }
                    break;
                case 3 :
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:328:52: '{' select_assign ( ',' select_assign )* '}'
                    {
                    char_literal66=(Token)match(input,39,FOLLOW_39_in_select_expr1498); 
                    char_literal66_tree = 
                    (Object)adaptor.create(char_literal66)
                    ;
                    adaptor.addChild(root_0, char_literal66_tree);


                    pushFollow(FOLLOW_select_assign_in_select_expr1500);
                    select_assign67=select_assign();

                    state._fsp--;

                    adaptor.addChild(root_0, select_assign67.getTree());

                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:328:70: ( ',' select_assign )*
                    loop12:
                    do {
                        int alt12=2;
                        switch ( input.LA(1) ) {
                        case 34:
                            {
                            alt12=1;
                            }
                            break;

                        }

                        switch (alt12) {
                    	case 1 :
                    	    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:328:71: ',' select_assign
                    	    {
                    	    char_literal68=(Token)match(input,34,FOLLOW_34_in_select_expr1503); 
                    	    char_literal68_tree = 
                    	    (Object)adaptor.create(char_literal68)
                    	    ;
                    	    adaptor.addChild(root_0, char_literal68_tree);


                    	    pushFollow(FOLLOW_select_assign_in_select_expr1505);
                    	    select_assign69=select_assign();

                    	    state._fsp--;

                    	    adaptor.addChild(root_0, select_assign69.getTree());

                    	    }
                    	    break;

                    	default :
                    	    break loop12;
                        }
                    } while (true);


                    char_literal70=(Token)match(input,40,FOLLOW_40_in_select_expr1510); 
                    char_literal70_tree = 
                    (Object)adaptor.create(char_literal70)
                    ;
                    adaptor.addChild(root_0, char_literal70_tree);


                    }
                    break;

            }


            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

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
    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:332:1: ql returns [Query query] : ( 'select' ! select_expr !)? ( ( 'where' !)? expression )? ( 'order by' ! order ! ( ',' ! order !)* )? ;
    public final CpQueryFilterParser.ql_return ql() throws RecognitionException {
        CpQueryFilterParser.ql_return retval = new CpQueryFilterParser.ql_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token string_literal71=null;
        Token string_literal73=null;
        Token string_literal75=null;
        Token char_literal77=null;
        CpQueryFilterParser.select_expr_return select_expr72 =null;

        CpQueryFilterParser.expression_return expression74 =null;

        CpQueryFilterParser.order_return order76 =null;

        CpQueryFilterParser.order_return order78 =null;


        Object string_literal71_tree=null;
        Object string_literal73_tree=null;
        Object string_literal75_tree=null;
        Object char_literal77_tree=null;

        try {
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:333:3: ( ( 'select' ! select_expr !)? ( ( 'where' !)? expression )? ( 'order by' ! order ! ( ',' ! order !)* )? )
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:333:5: ( 'select' ! select_expr !)? ( ( 'where' !)? expression )? ( 'order by' ! order ! ( ',' ! order !)* )?
            {
            root_0 = (Object)adaptor.nil();


            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:333:5: ( 'select' ! select_expr !)?
            int alt14=2;
            switch ( input.LA(1) ) {
                case 37:
                    {
                    alt14=1;
                    }
                    break;
            }

            switch (alt14) {
                case 1 :
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:333:6: 'select' ! select_expr !
                    {
                    string_literal71=(Token)match(input,37,FOLLOW_37_in_ql1533); 

                    pushFollow(FOLLOW_select_expr_in_ql1536);
                    select_expr72=select_expr();

                    state._fsp--;


                    }
                    break;

            }


            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:333:31: ( ( 'where' !)? expression )?
            int alt16=2;
            switch ( input.LA(1) ) {
                case ID:
                case NOT:
                case 31:
                case 38:
                    {
                    alt16=1;
                    }
                    break;
            }

            switch (alt16) {
                case 1 :
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:333:32: ( 'where' !)? expression
                    {
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:333:39: ( 'where' !)?
                    int alt15=2;
                    switch ( input.LA(1) ) {
                        case 38:
                            {
                            alt15=1;
                            }
                            break;
                    }

                    switch (alt15) {
                        case 1 :
                            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:333:39: 'where' !
                            {
                            string_literal73=(Token)match(input,38,FOLLOW_38_in_ql1542); 

                            }
                            break;

                    }


                    pushFollow(FOLLOW_expression_in_ql1546);
                    expression74=expression();

                    state._fsp--;

                    adaptor.addChild(root_0, expression74.getTree());

                    }
                    break;

            }


            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:333:55: ( 'order by' ! order ! ( ',' ! order !)* )?
            int alt18=2;
            switch ( input.LA(1) ) {
                case 36:
                    {
                    alt18=1;
                    }
                    break;
            }

            switch (alt18) {
                case 1 :
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:333:56: 'order by' ! order ! ( ',' ! order !)*
                    {
                    string_literal75=(Token)match(input,36,FOLLOW_36_in_ql1551); 

                    pushFollow(FOLLOW_order_in_ql1554);
                    order76=order();

                    state._fsp--;


                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:333:75: ( ',' ! order !)*
                    loop17:
                    do {
                        int alt17=2;
                        switch ( input.LA(1) ) {
                        case 34:
                            {
                            alt17=1;
                            }
                            break;

                        }

                        switch (alt17) {
                    	case 1 :
                    	    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:333:76: ',' ! order !
                    	    {
                    	    char_literal77=(Token)match(input,34,FOLLOW_34_in_ql1558); 

                    	    pushFollow(FOLLOW_order_in_ql1561);
                    	    order78=order();

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




              if((expression74!=null?((Object)expression74.tree):null) instanceof Operand){
                query.setRootOperand((Operand)(expression74!=null?((Object)expression74.tree):null));
              }
              
              retval.query = query;




            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
         
        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "ql"

    // Delegated rules


 

    public static final BitSet FOLLOW_ID_in_property991 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_containsproperty1002 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_withinproperty1013 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_BOOLEAN_in_booleanliteral1024 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LONG_in_longliteral1038 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_UUID_in_uuidliteral1052 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_STRING_in_stringliteral1065 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_FLOAT_in_floatliteral1080 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_booleanliteral_in_value1096 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_longliteral_in_value1102 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_uuidliteral_in_value1108 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_stringliteral_in_value1114 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_floatliteral_in_value1120 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_property_in_equalityop1145 = new BitSet(new long[]{0x0000000000080000L});
    public static final BitSet FOLLOW_LT_in_equalityop1147 = new BitSet(new long[]{0x0000000012042040L});
    public static final BitSet FOLLOW_value_in_equalityop1153 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_property_in_equalityop1158 = new BitSet(new long[]{0x0000000000100000L});
    public static final BitSet FOLLOW_LTE_in_equalityop1160 = new BitSet(new long[]{0x0000000012042040L});
    public static final BitSet FOLLOW_value_in_equalityop1166 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_property_in_equalityop1171 = new BitSet(new long[]{0x0000000000000200L});
    public static final BitSet FOLLOW_EQ_in_equalityop1173 = new BitSet(new long[]{0x0000000012042040L});
    public static final BitSet FOLLOW_value_in_equalityop1179 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_property_in_equalityop1184 = new BitSet(new long[]{0x0000000000004000L});
    public static final BitSet FOLLOW_GT_in_equalityop1186 = new BitSet(new long[]{0x0000000012042040L});
    public static final BitSet FOLLOW_value_in_equalityop1192 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_property_in_equalityop1197 = new BitSet(new long[]{0x0000000000008000L});
    public static final BitSet FOLLOW_GTE_in_equalityop1199 = new BitSet(new long[]{0x0000000012042040L});
    public static final BitSet FOLLOW_value_in_equalityop1205 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_withinproperty_in_locationop1220 = new BitSet(new long[]{0x0000000020000000L});
    public static final BitSet FOLLOW_WITHIN_in_locationop1222 = new BitSet(new long[]{0x0000000000042000L});
    public static final BitSet FOLLOW_floatliteral_in_locationop1229 = new BitSet(new long[]{0x0000000000800000L});
    public static final BitSet FOLLOW_longliteral_in_locationop1231 = new BitSet(new long[]{0x0000000000800000L});
    public static final BitSet FOLLOW_OF_in_locationop1234 = new BitSet(new long[]{0x0000000000042000L});
    public static final BitSet FOLLOW_floatliteral_in_locationop1238 = new BitSet(new long[]{0x0000000400000000L});
    public static final BitSet FOLLOW_longliteral_in_locationop1240 = new BitSet(new long[]{0x0000000400000000L});
    public static final BitSet FOLLOW_34_in_locationop1243 = new BitSet(new long[]{0x0000000000042000L});
    public static final BitSet FOLLOW_floatliteral_in_locationop1247 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_longliteral_in_locationop1249 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_containsproperty_in_containsop1263 = new BitSet(new long[]{0x0000000000000080L});
    public static final BitSet FOLLOW_CONTAINS_in_containsop1265 = new BitSet(new long[]{0x0000000002000000L});
    public static final BitSet FOLLOW_stringliteral_in_containsop1271 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_31_in_operation1281 = new BitSet(new long[]{0x0000000080220000L});
    public static final BitSet FOLLOW_expression_in_operation1284 = new BitSet(new long[]{0x0000000100000000L});
    public static final BitSet FOLLOW_32_in_operation1286 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_equalityop_in_operation1294 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_locationop_in_operation1302 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_containsop_in_operation1310 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_NOT_in_notexp1326 = new BitSet(new long[]{0x0000000080020000L});
    public static final BitSet FOLLOW_operation_in_notexp1332 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_operation_in_notexp1338 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_notexp_in_andexp1352 = new BitSet(new long[]{0x0000000000000012L});
    public static final BitSet FOLLOW_AND_in_andexp1355 = new BitSet(new long[]{0x0000000080220000L});
    public static final BitSet FOLLOW_notexp_in_andexp1361 = new BitSet(new long[]{0x0000000000000012L});
    public static final BitSet FOLLOW_andexp_in_expression1378 = new BitSet(new long[]{0x0000000001000002L});
    public static final BitSet FOLLOW_OR_in_expression1381 = new BitSet(new long[]{0x0000000080220000L});
    public static final BitSet FOLLOW_andexp_in_expression1387 = new BitSet(new long[]{0x0000000001000002L});
    public static final BitSet FOLLOW_property_in_order1424 = new BitSet(new long[]{0x0000000000000122L});
    public static final BitSet FOLLOW_direction_in_order1426 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_select_subject1445 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_select_assign1462 = new BitSet(new long[]{0x0000000800000000L});
    public static final BitSet FOLLOW_35_in_select_assign1464 = new BitSet(new long[]{0x0000000000020000L});
    public static final BitSet FOLLOW_ID_in_select_assign1468 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_33_in_select_expr1482 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_select_subject_in_select_expr1486 = new BitSet(new long[]{0x0000000400000002L});
    public static final BitSet FOLLOW_34_in_select_expr1489 = new BitSet(new long[]{0x0000000000020000L});
    public static final BitSet FOLLOW_select_subject_in_select_expr1491 = new BitSet(new long[]{0x0000000400000002L});
    public static final BitSet FOLLOW_39_in_select_expr1498 = new BitSet(new long[]{0x0000000000020000L});
    public static final BitSet FOLLOW_select_assign_in_select_expr1500 = new BitSet(new long[]{0x0000010400000000L});
    public static final BitSet FOLLOW_34_in_select_expr1503 = new BitSet(new long[]{0x0000000000020000L});
    public static final BitSet FOLLOW_select_assign_in_select_expr1505 = new BitSet(new long[]{0x0000010400000000L});
    public static final BitSet FOLLOW_40_in_select_expr1510 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_37_in_ql1533 = new BitSet(new long[]{0x0000008200020000L});
    public static final BitSet FOLLOW_select_expr_in_ql1536 = new BitSet(new long[]{0x0000005080220002L});
    public static final BitSet FOLLOW_38_in_ql1542 = new BitSet(new long[]{0x0000000080220000L});
    public static final BitSet FOLLOW_expression_in_ql1546 = new BitSet(new long[]{0x0000001000000002L});
    public static final BitSet FOLLOW_36_in_ql1551 = new BitSet(new long[]{0x0000000000020000L});
    public static final BitSet FOLLOW_order_in_ql1554 = new BitSet(new long[]{0x0000000400000002L});
    public static final BitSet FOLLOW_34_in_ql1558 = new BitSet(new long[]{0x0000000000020000L});
    public static final BitSet FOLLOW_order_in_ql1561 = new BitSet(new long[]{0x0000000400000002L});

}