// $ANTLR 3.1.3 Mar 17, 2009 19:23:44 org/usergrid/persistence/query/tree/QueryFilter.g 2012-03-16 13:52:58

package org.usergrid.persistence.query.tree;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

public class QueryFilterLexer extends Lexer {
    public static final int EXPONENT=6;
    public static final int LT=16;
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
    public static final int INT=5;
    public static final int FLOAT=7;
    public static final int ID=4;
    public static final int EOF=-1;
    public static final int LTE=17;
    public static final int T__30=30;
    public static final int T__31=31;
    public static final int T__32=32;
    public static final int T__33=33;
    public static final int ESC_SEQ=8;
    public static final int BOOLEAN=10;
    public static final int WS=15;
    public static final int T__34=34;
    public static final int T__35=35;
    public static final int T__36=36;
    public static final int T__37=37;
    public static final int T__38=38;
    public static final int GT=19;
    public static final int EQ=18;
    public static final int STRING=9;


      private static final Logger logger = LoggerFactory
          .getLogger(QueryFilterLexer.class);

    	@Override
    	public void emitErrorMessage(String msg) {
    		logger.info(msg);
    	}


    // delegates
    // delegators

    public QueryFilterLexer() {;} 
    public QueryFilterLexer(CharStream input) {
        this(input, new RecognizerSharedState());
    }
    public QueryFilterLexer(CharStream input, RecognizerSharedState state) {
        super(input,state);

    }
    public String getGrammarFileName() { return "org/usergrid/persistence/query/tree/QueryFilter.g"; }

    // $ANTLR start "T__21"
    public final void mT__21() throws RecognitionException {
        try {
            int _type = T__21;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:21:7: ( 'within' )
            // org/usergrid/persistence/query/tree/QueryFilter.g:21:9: 'within'
            {
            match("within"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__21"

    // $ANTLR start "T__22"
    public final void mT__22() throws RecognitionException {
        try {
            int _type = T__22;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:22:7: ( 'of' )
            // org/usergrid/persistence/query/tree/QueryFilter.g:22:9: 'of'
            {
            match("of"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__22"

    // $ANTLR start "T__23"
    public final void mT__23() throws RecognitionException {
        try {
            int _type = T__23;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:23:7: ( ',' )
            // org/usergrid/persistence/query/tree/QueryFilter.g:23:9: ','
            {
            match(','); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__23"

    // $ANTLR start "T__24"
    public final void mT__24() throws RecognitionException {
        try {
            int _type = T__24;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:24:7: ( 'contains' )
            // org/usergrid/persistence/query/tree/QueryFilter.g:24:9: 'contains'
            {
            match("contains"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__24"

    // $ANTLR start "T__25"
    public final void mT__25() throws RecognitionException {
        try {
            int _type = T__25;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:25:7: ( '(' )
            // org/usergrid/persistence/query/tree/QueryFilter.g:25:9: '('
            {
            match('('); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__25"

    // $ANTLR start "T__26"
    public final void mT__26() throws RecognitionException {
        try {
            int _type = T__26;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:26:7: ( ')' )
            // org/usergrid/persistence/query/tree/QueryFilter.g:26:9: ')'
            {
            match(')'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__26"

    // $ANTLR start "T__27"
    public final void mT__27() throws RecognitionException {
        try {
            int _type = T__27;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:27:7: ( 'not' )
            // org/usergrid/persistence/query/tree/QueryFilter.g:27:9: 'not'
            {
            match("not"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__27"

    // $ANTLR start "T__28"
    public final void mT__28() throws RecognitionException {
        try {
            int _type = T__28;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:28:7: ( 'and' )
            // org/usergrid/persistence/query/tree/QueryFilter.g:28:9: 'and'
            {
            match("and"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__28"

    // $ANTLR start "T__29"
    public final void mT__29() throws RecognitionException {
        try {
            int _type = T__29;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:29:7: ( 'or' )
            // org/usergrid/persistence/query/tree/QueryFilter.g:29:9: 'or'
            {
            match("or"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__29"

    // $ANTLR start "T__30"
    public final void mT__30() throws RecognitionException {
        try {
            int _type = T__30;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:30:7: ( 'asc' )
            // org/usergrid/persistence/query/tree/QueryFilter.g:30:9: 'asc'
            {
            match("asc"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__30"

    // $ANTLR start "T__31"
    public final void mT__31() throws RecognitionException {
        try {
            int _type = T__31;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:31:7: ( 'desc' )
            // org/usergrid/persistence/query/tree/QueryFilter.g:31:9: 'desc'
            {
            match("desc"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__31"

    // $ANTLR start "T__32"
    public final void mT__32() throws RecognitionException {
        try {
            int _type = T__32;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:32:7: ( ':' )
            // org/usergrid/persistence/query/tree/QueryFilter.g:32:9: ':'
            {
            match(':'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__32"

    // $ANTLR start "T__33"
    public final void mT__33() throws RecognitionException {
        try {
            int _type = T__33;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:33:7: ( '*' )
            // org/usergrid/persistence/query/tree/QueryFilter.g:33:9: '*'
            {
            match('*'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__33"

    // $ANTLR start "T__34"
    public final void mT__34() throws RecognitionException {
        try {
            int _type = T__34;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:34:7: ( '{' )
            // org/usergrid/persistence/query/tree/QueryFilter.g:34:9: '{'
            {
            match('{'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__34"

    // $ANTLR start "T__35"
    public final void mT__35() throws RecognitionException {
        try {
            int _type = T__35;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:35:7: ( '}' )
            // org/usergrid/persistence/query/tree/QueryFilter.g:35:9: '}'
            {
            match('}'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__35"

    // $ANTLR start "T__36"
    public final void mT__36() throws RecognitionException {
        try {
            int _type = T__36;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:36:7: ( 'select' )
            // org/usergrid/persistence/query/tree/QueryFilter.g:36:9: 'select'
            {
            match("select"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__36"

    // $ANTLR start "T__37"
    public final void mT__37() throws RecognitionException {
        try {
            int _type = T__37;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:37:7: ( 'where' )
            // org/usergrid/persistence/query/tree/QueryFilter.g:37:9: 'where'
            {
            match("where"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__37"

    // $ANTLR start "T__38"
    public final void mT__38() throws RecognitionException {
        try {
            int _type = T__38;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:38:7: ( 'order by' )
            // org/usergrid/persistence/query/tree/QueryFilter.g:38:9: 'order by'
            {
            match("order by"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__38"

    // $ANTLR start "ID"
    public final void mID() throws RecognitionException {
        try {
            int _type = ID;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:52:5: ( ( 'a' .. 'z' | 'A' .. 'Z' | '_' ) ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '_' | '.' )* )
            // org/usergrid/persistence/query/tree/QueryFilter.g:52:7: ( 'a' .. 'z' | 'A' .. 'Z' | '_' ) ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '_' | '.' )*
            {
            if ( (input.LA(1)>='A' && input.LA(1)<='Z')||input.LA(1)=='_'||(input.LA(1)>='a' && input.LA(1)<='z') ) {
                input.consume();

            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}

            // org/usergrid/persistence/query/tree/QueryFilter.g:52:31: ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '_' | '.' )*
            loop1:
            do {
                int alt1=2;
                int LA1_0 = input.LA(1);

                if ( (LA1_0=='.'||(LA1_0>='0' && LA1_0<='9')||(LA1_0>='A' && LA1_0<='Z')||LA1_0=='_'||(LA1_0>='a' && LA1_0<='z')) ) {
                    alt1=1;
                }


                switch (alt1) {
            	case 1 :
            	    // org/usergrid/persistence/query/tree/QueryFilter.g:
            	    {
            	    if ( input.LA(1)=='.'||(input.LA(1)>='0' && input.LA(1)<='9')||(input.LA(1)>='A' && input.LA(1)<='Z')||input.LA(1)=='_'||(input.LA(1)>='a' && input.LA(1)<='z') ) {
            	        input.consume();

            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;}


            	    }
            	    break;

            	default :
            	    break loop1;
                }
            } while (true);


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "ID"

    // $ANTLR start "INT"
    public final void mINT() throws RecognitionException {
        try {
            int _type = INT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:55:5: ( ( '-' )? ( '0' .. '9' )+ )
            // org/usergrid/persistence/query/tree/QueryFilter.g:55:7: ( '-' )? ( '0' .. '9' )+
            {
            // org/usergrid/persistence/query/tree/QueryFilter.g:55:7: ( '-' )?
            int alt2=2;
            int LA2_0 = input.LA(1);

            if ( (LA2_0=='-') ) {
                alt2=1;
            }
            switch (alt2) {
                case 1 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:55:8: '-'
                    {
                    match('-'); 

                    }
                    break;

            }

            // org/usergrid/persistence/query/tree/QueryFilter.g:55:14: ( '0' .. '9' )+
            int cnt3=0;
            loop3:
            do {
                int alt3=2;
                int LA3_0 = input.LA(1);

                if ( ((LA3_0>='0' && LA3_0<='9')) ) {
                    alt3=1;
                }


                switch (alt3) {
            	case 1 :
            	    // org/usergrid/persistence/query/tree/QueryFilter.g:55:14: '0' .. '9'
            	    {
            	    matchRange('0','9'); 

            	    }
            	    break;

            	default :
            	    if ( cnt3 >= 1 ) break loop3;
                        EarlyExitException eee =
                            new EarlyExitException(3, input);
                        throw eee;
                }
                cnt3++;
            } while (true);


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "INT"

    // $ANTLR start "FLOAT"
    public final void mFLOAT() throws RecognitionException {
        try {
            int _type = FLOAT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:59:5: ( ( '-' )? ( ( '0' .. '9' )+ '.' ( '0' .. '9' )* ( EXPONENT )? | '.' ( '0' .. '9' )+ ( EXPONENT )? | ( '0' .. '9' )+ EXPONENT ) )
            // org/usergrid/persistence/query/tree/QueryFilter.g:59:8: ( '-' )? ( ( '0' .. '9' )+ '.' ( '0' .. '9' )* ( EXPONENT )? | '.' ( '0' .. '9' )+ ( EXPONENT )? | ( '0' .. '9' )+ EXPONENT )
            {
            // org/usergrid/persistence/query/tree/QueryFilter.g:59:8: ( '-' )?
            int alt4=2;
            int LA4_0 = input.LA(1);

            if ( (LA4_0=='-') ) {
                alt4=1;
            }
            switch (alt4) {
                case 1 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:59:9: '-'
                    {
                    match('-'); 

                    }
                    break;

            }

            // org/usergrid/persistence/query/tree/QueryFilter.g:59:15: ( ( '0' .. '9' )+ '.' ( '0' .. '9' )* ( EXPONENT )? | '.' ( '0' .. '9' )+ ( EXPONENT )? | ( '0' .. '9' )+ EXPONENT )
            int alt11=3;
            alt11 = dfa11.predict(input);
            switch (alt11) {
                case 1 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:59:17: ( '0' .. '9' )+ '.' ( '0' .. '9' )* ( EXPONENT )?
                    {
                    // org/usergrid/persistence/query/tree/QueryFilter.g:59:17: ( '0' .. '9' )+
                    int cnt5=0;
                    loop5:
                    do {
                        int alt5=2;
                        int LA5_0 = input.LA(1);

                        if ( ((LA5_0>='0' && LA5_0<='9')) ) {
                            alt5=1;
                        }


                        switch (alt5) {
                    	case 1 :
                    	    // org/usergrid/persistence/query/tree/QueryFilter.g:59:18: '0' .. '9'
                    	    {
                    	    matchRange('0','9'); 

                    	    }
                    	    break;

                    	default :
                    	    if ( cnt5 >= 1 ) break loop5;
                                EarlyExitException eee =
                                    new EarlyExitException(5, input);
                                throw eee;
                        }
                        cnt5++;
                    } while (true);

                    match('.'); 
                    // org/usergrid/persistence/query/tree/QueryFilter.g:59:33: ( '0' .. '9' )*
                    loop6:
                    do {
                        int alt6=2;
                        int LA6_0 = input.LA(1);

                        if ( ((LA6_0>='0' && LA6_0<='9')) ) {
                            alt6=1;
                        }


                        switch (alt6) {
                    	case 1 :
                    	    // org/usergrid/persistence/query/tree/QueryFilter.g:59:34: '0' .. '9'
                    	    {
                    	    matchRange('0','9'); 

                    	    }
                    	    break;

                    	default :
                    	    break loop6;
                        }
                    } while (true);

                    // org/usergrid/persistence/query/tree/QueryFilter.g:59:45: ( EXPONENT )?
                    int alt7=2;
                    int LA7_0 = input.LA(1);

                    if ( (LA7_0=='E'||LA7_0=='e') ) {
                        alt7=1;
                    }
                    switch (alt7) {
                        case 1 :
                            // org/usergrid/persistence/query/tree/QueryFilter.g:59:45: EXPONENT
                            {
                            mEXPONENT(); 

                            }
                            break;

                    }


                    }
                    break;
                case 2 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:60:9: '.' ( '0' .. '9' )+ ( EXPONENT )?
                    {
                    match('.'); 
                    // org/usergrid/persistence/query/tree/QueryFilter.g:60:13: ( '0' .. '9' )+
                    int cnt8=0;
                    loop8:
                    do {
                        int alt8=2;
                        int LA8_0 = input.LA(1);

                        if ( ((LA8_0>='0' && LA8_0<='9')) ) {
                            alt8=1;
                        }


                        switch (alt8) {
                    	case 1 :
                    	    // org/usergrid/persistence/query/tree/QueryFilter.g:60:14: '0' .. '9'
                    	    {
                    	    matchRange('0','9'); 

                    	    }
                    	    break;

                    	default :
                    	    if ( cnt8 >= 1 ) break loop8;
                                EarlyExitException eee =
                                    new EarlyExitException(8, input);
                                throw eee;
                        }
                        cnt8++;
                    } while (true);

                    // org/usergrid/persistence/query/tree/QueryFilter.g:60:25: ( EXPONENT )?
                    int alt9=2;
                    int LA9_0 = input.LA(1);

                    if ( (LA9_0=='E'||LA9_0=='e') ) {
                        alt9=1;
                    }
                    switch (alt9) {
                        case 1 :
                            // org/usergrid/persistence/query/tree/QueryFilter.g:60:25: EXPONENT
                            {
                            mEXPONENT(); 

                            }
                            break;

                    }


                    }
                    break;
                case 3 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:61:9: ( '0' .. '9' )+ EXPONENT
                    {
                    // org/usergrid/persistence/query/tree/QueryFilter.g:61:9: ( '0' .. '9' )+
                    int cnt10=0;
                    loop10:
                    do {
                        int alt10=2;
                        int LA10_0 = input.LA(1);

                        if ( ((LA10_0>='0' && LA10_0<='9')) ) {
                            alt10=1;
                        }


                        switch (alt10) {
                    	case 1 :
                    	    // org/usergrid/persistence/query/tree/QueryFilter.g:61:10: '0' .. '9'
                    	    {
                    	    matchRange('0','9'); 

                    	    }
                    	    break;

                    	default :
                    	    if ( cnt10 >= 1 ) break loop10;
                                EarlyExitException eee =
                                    new EarlyExitException(10, input);
                                throw eee;
                        }
                        cnt10++;
                    } while (true);

                    mEXPONENT(); 

                    }
                    break;

            }


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "FLOAT"

    // $ANTLR start "STRING"
    public final void mSTRING() throws RecognitionException {
        try {
            int _type = STRING;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:65:5: ( '\\'' ( ESC_SEQ | ~ ( '\\\\' | '\\'' ) )* '\\'' )
            // org/usergrid/persistence/query/tree/QueryFilter.g:65:8: '\\'' ( ESC_SEQ | ~ ( '\\\\' | '\\'' ) )* '\\''
            {
            match('\''); 
            // org/usergrid/persistence/query/tree/QueryFilter.g:65:13: ( ESC_SEQ | ~ ( '\\\\' | '\\'' ) )*
            loop12:
            do {
                int alt12=3;
                int LA12_0 = input.LA(1);

                if ( (LA12_0=='\\') ) {
                    alt12=1;
                }
                else if ( ((LA12_0>='\u0000' && LA12_0<='&')||(LA12_0>='(' && LA12_0<='[')||(LA12_0>=']' && LA12_0<='\uFFFF')) ) {
                    alt12=2;
                }


                switch (alt12) {
            	case 1 :
            	    // org/usergrid/persistence/query/tree/QueryFilter.g:65:15: ESC_SEQ
            	    {
            	    mESC_SEQ(); 

            	    }
            	    break;
            	case 2 :
            	    // org/usergrid/persistence/query/tree/QueryFilter.g:65:25: ~ ( '\\\\' | '\\'' )
            	    {
            	    if ( (input.LA(1)>='\u0000' && input.LA(1)<='&')||(input.LA(1)>='(' && input.LA(1)<='[')||(input.LA(1)>=']' && input.LA(1)<='\uFFFF') ) {
            	        input.consume();

            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;}


            	    }
            	    break;

            	default :
            	    break loop12;
                }
            } while (true);

            match('\''); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "STRING"

    // $ANTLR start "BOOLEAN"
    public final void mBOOLEAN() throws RecognitionException {
        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:69:9: ( ( 'true' | 'false' ) )
            // org/usergrid/persistence/query/tree/QueryFilter.g:69:11: ( 'true' | 'false' )
            {
            // org/usergrid/persistence/query/tree/QueryFilter.g:69:11: ( 'true' | 'false' )
            int alt13=2;
            int LA13_0 = input.LA(1);

            if ( (LA13_0=='t') ) {
                alt13=1;
            }
            else if ( (LA13_0=='f') ) {
                alt13=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 13, 0, input);

                throw nvae;
            }
            switch (alt13) {
                case 1 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:69:12: 'true'
                    {
                    match("true"); 


                    }
                    break;
                case 2 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:69:20: 'false'
                    {
                    match("false"); 


                    }
                    break;

            }


            }

        }
        finally {
        }
    }
    // $ANTLR end "BOOLEAN"

    // $ANTLR start "UUID"
    public final void mUUID() throws RecognitionException {
        try {
            int _type = UUID;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:71:6: ( HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT '-' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT '-' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT '-' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT '-' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT )
            // org/usergrid/persistence/query/tree/QueryFilter.g:71:8: HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT '-' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT '-' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT '-' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT '-' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT
            {
            mHEX_DIGIT(); 
            mHEX_DIGIT(); 
            mHEX_DIGIT(); 
            mHEX_DIGIT(); 
            mHEX_DIGIT(); 
            mHEX_DIGIT(); 
            mHEX_DIGIT(); 
            mHEX_DIGIT(); 
            match('-'); 
            mHEX_DIGIT(); 
            mHEX_DIGIT(); 
            mHEX_DIGIT(); 
            mHEX_DIGIT(); 
            match('-'); 
            mHEX_DIGIT(); 
            mHEX_DIGIT(); 
            mHEX_DIGIT(); 
            mHEX_DIGIT(); 
            match('-'); 
            mHEX_DIGIT(); 
            mHEX_DIGIT(); 
            mHEX_DIGIT(); 
            mHEX_DIGIT(); 
            match('-'); 
            mHEX_DIGIT(); 
            mHEX_DIGIT(); 
            mHEX_DIGIT(); 
            mHEX_DIGIT(); 
            mHEX_DIGIT(); 
            mHEX_DIGIT(); 
            mHEX_DIGIT(); 
            mHEX_DIGIT(); 
            mHEX_DIGIT(); 
            mHEX_DIGIT(); 
            mHEX_DIGIT(); 
            mHEX_DIGIT(); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "UUID"

    // $ANTLR start "EXPONENT"
    public final void mEXPONENT() throws RecognitionException {
        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:82:10: ( ( 'e' | 'E' ) ( '+' | '-' )? ( '0' .. '9' )+ )
            // org/usergrid/persistence/query/tree/QueryFilter.g:82:12: ( 'e' | 'E' ) ( '+' | '-' )? ( '0' .. '9' )+
            {
            if ( input.LA(1)=='E'||input.LA(1)=='e' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}

            // org/usergrid/persistence/query/tree/QueryFilter.g:82:22: ( '+' | '-' )?
            int alt14=2;
            int LA14_0 = input.LA(1);

            if ( (LA14_0=='+'||LA14_0=='-') ) {
                alt14=1;
            }
            switch (alt14) {
                case 1 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:
                    {
                    if ( input.LA(1)=='+'||input.LA(1)=='-' ) {
                        input.consume();

                    }
                    else {
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;}


                    }
                    break;

            }

            // org/usergrid/persistence/query/tree/QueryFilter.g:82:33: ( '0' .. '9' )+
            int cnt15=0;
            loop15:
            do {
                int alt15=2;
                int LA15_0 = input.LA(1);

                if ( ((LA15_0>='0' && LA15_0<='9')) ) {
                    alt15=1;
                }


                switch (alt15) {
            	case 1 :
            	    // org/usergrid/persistence/query/tree/QueryFilter.g:82:34: '0' .. '9'
            	    {
            	    matchRange('0','9'); 

            	    }
            	    break;

            	default :
            	    if ( cnt15 >= 1 ) break loop15;
                        EarlyExitException eee =
                            new EarlyExitException(15, input);
                        throw eee;
                }
                cnt15++;
            } while (true);


            }

        }
        finally {
        }
    }
    // $ANTLR end "EXPONENT"

    // $ANTLR start "HEX_DIGIT"
    public final void mHEX_DIGIT() throws RecognitionException {
        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:85:11: ( ( '0' .. '9' | 'a' .. 'f' | 'A' .. 'F' ) )
            // org/usergrid/persistence/query/tree/QueryFilter.g:85:13: ( '0' .. '9' | 'a' .. 'f' | 'A' .. 'F' )
            {
            if ( (input.LA(1)>='0' && input.LA(1)<='9')||(input.LA(1)>='A' && input.LA(1)<='F')||(input.LA(1)>='a' && input.LA(1)<='f') ) {
                input.consume();

            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}


            }

        }
        finally {
        }
    }
    // $ANTLR end "HEX_DIGIT"

    // $ANTLR start "ESC_SEQ"
    public final void mESC_SEQ() throws RecognitionException {
        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:89:5: ( '\\\\' ( 'b' | 't' | 'n' | 'f' | 'r' | '\\\"' | '\\'' | '\\\\' ) | UNICODE_ESC | OCTAL_ESC )
            int alt16=3;
            int LA16_0 = input.LA(1);

            if ( (LA16_0=='\\') ) {
                switch ( input.LA(2) ) {
                case '\"':
                case '\'':
                case '\\':
                case 'b':
                case 'f':
                case 'n':
                case 'r':
                case 't':
                    {
                    alt16=1;
                    }
                    break;
                case 'u':
                    {
                    alt16=2;
                    }
                    break;
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                    {
                    alt16=3;
                    }
                    break;
                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 16, 1, input);

                    throw nvae;
                }

            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 16, 0, input);

                throw nvae;
            }
            switch (alt16) {
                case 1 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:89:9: '\\\\' ( 'b' | 't' | 'n' | 'f' | 'r' | '\\\"' | '\\'' | '\\\\' )
                    {
                    match('\\'); 
                    if ( input.LA(1)=='\"'||input.LA(1)=='\''||input.LA(1)=='\\'||input.LA(1)=='b'||input.LA(1)=='f'||input.LA(1)=='n'||input.LA(1)=='r'||input.LA(1)=='t' ) {
                        input.consume();

                    }
                    else {
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;}


                    }
                    break;
                case 2 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:90:9: UNICODE_ESC
                    {
                    mUNICODE_ESC(); 

                    }
                    break;
                case 3 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:91:9: OCTAL_ESC
                    {
                    mOCTAL_ESC(); 

                    }
                    break;

            }
        }
        finally {
        }
    }
    // $ANTLR end "ESC_SEQ"

    // $ANTLR start "OCTAL_ESC"
    public final void mOCTAL_ESC() throws RecognitionException {
        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:96:5: ( '\\\\' ( '0' .. '3' ) ( '0' .. '7' ) ( '0' .. '7' ) | '\\\\' ( '0' .. '7' ) ( '0' .. '7' ) | '\\\\' ( '0' .. '7' ) )
            int alt17=3;
            int LA17_0 = input.LA(1);

            if ( (LA17_0=='\\') ) {
                int LA17_1 = input.LA(2);

                if ( ((LA17_1>='0' && LA17_1<='3')) ) {
                    int LA17_2 = input.LA(3);

                    if ( ((LA17_2>='0' && LA17_2<='7')) ) {
                        int LA17_4 = input.LA(4);

                        if ( ((LA17_4>='0' && LA17_4<='7')) ) {
                            alt17=1;
                        }
                        else {
                            alt17=2;}
                    }
                    else {
                        alt17=3;}
                }
                else if ( ((LA17_1>='4' && LA17_1<='7')) ) {
                    int LA17_3 = input.LA(3);

                    if ( ((LA17_3>='0' && LA17_3<='7')) ) {
                        alt17=2;
                    }
                    else {
                        alt17=3;}
                }
                else {
                    NoViableAltException nvae =
                        new NoViableAltException("", 17, 1, input);

                    throw nvae;
                }
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 17, 0, input);

                throw nvae;
            }
            switch (alt17) {
                case 1 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:96:9: '\\\\' ( '0' .. '3' ) ( '0' .. '7' ) ( '0' .. '7' )
                    {
                    match('\\'); 
                    // org/usergrid/persistence/query/tree/QueryFilter.g:96:14: ( '0' .. '3' )
                    // org/usergrid/persistence/query/tree/QueryFilter.g:96:15: '0' .. '3'
                    {
                    matchRange('0','3'); 

                    }

                    // org/usergrid/persistence/query/tree/QueryFilter.g:96:25: ( '0' .. '7' )
                    // org/usergrid/persistence/query/tree/QueryFilter.g:96:26: '0' .. '7'
                    {
                    matchRange('0','7'); 

                    }

                    // org/usergrid/persistence/query/tree/QueryFilter.g:96:36: ( '0' .. '7' )
                    // org/usergrid/persistence/query/tree/QueryFilter.g:96:37: '0' .. '7'
                    {
                    matchRange('0','7'); 

                    }


                    }
                    break;
                case 2 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:97:9: '\\\\' ( '0' .. '7' ) ( '0' .. '7' )
                    {
                    match('\\'); 
                    // org/usergrid/persistence/query/tree/QueryFilter.g:97:14: ( '0' .. '7' )
                    // org/usergrid/persistence/query/tree/QueryFilter.g:97:15: '0' .. '7'
                    {
                    matchRange('0','7'); 

                    }

                    // org/usergrid/persistence/query/tree/QueryFilter.g:97:25: ( '0' .. '7' )
                    // org/usergrid/persistence/query/tree/QueryFilter.g:97:26: '0' .. '7'
                    {
                    matchRange('0','7'); 

                    }


                    }
                    break;
                case 3 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:98:9: '\\\\' ( '0' .. '7' )
                    {
                    match('\\'); 
                    // org/usergrid/persistence/query/tree/QueryFilter.g:98:14: ( '0' .. '7' )
                    // org/usergrid/persistence/query/tree/QueryFilter.g:98:15: '0' .. '7'
                    {
                    matchRange('0','7'); 

                    }


                    }
                    break;

            }
        }
        finally {
        }
    }
    // $ANTLR end "OCTAL_ESC"

    // $ANTLR start "UNICODE_ESC"
    public final void mUNICODE_ESC() throws RecognitionException {
        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:103:5: ( '\\\\' 'u' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT )
            // org/usergrid/persistence/query/tree/QueryFilter.g:103:9: '\\\\' 'u' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT
            {
            match('\\'); 
            match('u'); 
            mHEX_DIGIT(); 
            mHEX_DIGIT(); 
            mHEX_DIGIT(); 
            mHEX_DIGIT(); 

            }

        }
        finally {
        }
    }
    // $ANTLR end "UNICODE_ESC"

    // $ANTLR start "WS"
    public final void mWS() throws RecognitionException {
        try {
            int _type = WS;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:106:4: ( ( ' ' | '\\t' | '\\n' | '\\r' | '\\f' )+ )
            // org/usergrid/persistence/query/tree/QueryFilter.g:106:6: ( ' ' | '\\t' | '\\n' | '\\r' | '\\f' )+
            {
            // org/usergrid/persistence/query/tree/QueryFilter.g:106:6: ( ' ' | '\\t' | '\\n' | '\\r' | '\\f' )+
            int cnt18=0;
            loop18:
            do {
                int alt18=2;
                int LA18_0 = input.LA(1);

                if ( ((LA18_0>='\t' && LA18_0<='\n')||(LA18_0>='\f' && LA18_0<='\r')||LA18_0==' ') ) {
                    alt18=1;
                }


                switch (alt18) {
            	case 1 :
            	    // org/usergrid/persistence/query/tree/QueryFilter.g:
            	    {
            	    if ( (input.LA(1)>='\t' && input.LA(1)<='\n')||(input.LA(1)>='\f' && input.LA(1)<='\r')||input.LA(1)==' ' ) {
            	        input.consume();

            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;}


            	    }
            	    break;

            	default :
            	    if ( cnt18 >= 1 ) break loop18;
                        EarlyExitException eee =
                            new EarlyExitException(18, input);
                        throw eee;
                }
                cnt18++;
            } while (true);

            _channel=HIDDEN;

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "WS"

    // $ANTLR start "LT"
    public final void mLT() throws RecognitionException {
        try {
            int _type = LT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:108:5: ( '<' | 'lt' )
            int alt19=2;
            int LA19_0 = input.LA(1);

            if ( (LA19_0=='<') ) {
                alt19=1;
            }
            else if ( (LA19_0=='l') ) {
                alt19=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 19, 0, input);

                throw nvae;
            }
            switch (alt19) {
                case 1 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:108:7: '<'
                    {
                    match('<'); 

                    }
                    break;
                case 2 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:108:13: 'lt'
                    {
                    match("lt"); 


                    }
                    break;

            }
            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "LT"

    // $ANTLR start "LTE"
    public final void mLTE() throws RecognitionException {
        try {
            int _type = LTE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:110:5: ( '<=' | 'lte' )
            int alt20=2;
            int LA20_0 = input.LA(1);

            if ( (LA20_0=='<') ) {
                alt20=1;
            }
            else if ( (LA20_0=='l') ) {
                alt20=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 20, 0, input);

                throw nvae;
            }
            switch (alt20) {
                case 1 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:110:7: '<='
                    {
                    match("<="); 


                    }
                    break;
                case 2 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:110:15: 'lte'
                    {
                    match("lte"); 


                    }
                    break;

            }
            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "LTE"

    // $ANTLR start "EQ"
    public final void mEQ() throws RecognitionException {
        try {
            int _type = EQ;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:112:5: ( '=' | 'eq' )
            int alt21=2;
            int LA21_0 = input.LA(1);

            if ( (LA21_0=='=') ) {
                alt21=1;
            }
            else if ( (LA21_0=='e') ) {
                alt21=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 21, 0, input);

                throw nvae;
            }
            switch (alt21) {
                case 1 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:112:7: '='
                    {
                    match('='); 

                    }
                    break;
                case 2 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:112:13: 'eq'
                    {
                    match("eq"); 


                    }
                    break;

            }
            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "EQ"

    // $ANTLR start "GT"
    public final void mGT() throws RecognitionException {
        try {
            int _type = GT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:114:5: ( '>' | 'gt' )
            int alt22=2;
            int LA22_0 = input.LA(1);

            if ( (LA22_0=='>') ) {
                alt22=1;
            }
            else if ( (LA22_0=='g') ) {
                alt22=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 22, 0, input);

                throw nvae;
            }
            switch (alt22) {
                case 1 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:114:7: '>'
                    {
                    match('>'); 

                    }
                    break;
                case 2 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:114:13: 'gt'
                    {
                    match("gt"); 


                    }
                    break;

            }
            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "GT"

    // $ANTLR start "GTE"
    public final void mGTE() throws RecognitionException {
        try {
            int _type = GTE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:116:5: ( '>=' | 'gte' )
            int alt23=2;
            int LA23_0 = input.LA(1);

            if ( (LA23_0=='>') ) {
                alt23=1;
            }
            else if ( (LA23_0=='g') ) {
                alt23=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 23, 0, input);

                throw nvae;
            }
            switch (alt23) {
                case 1 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:116:7: '>='
                    {
                    match(">="); 


                    }
                    break;
                case 2 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:116:15: 'gte'
                    {
                    match("gte"); 


                    }
                    break;

            }
            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "GTE"

    public void mTokens() throws RecognitionException {
        // org/usergrid/persistence/query/tree/QueryFilter.g:1:8: ( T__21 | T__22 | T__23 | T__24 | T__25 | T__26 | T__27 | T__28 | T__29 | T__30 | T__31 | T__32 | T__33 | T__34 | T__35 | T__36 | T__37 | T__38 | ID | INT | FLOAT | STRING | UUID | WS | LT | LTE | EQ | GT | GTE )
        int alt24=29;
        alt24 = dfa24.predict(input);
        switch (alt24) {
            case 1 :
                // org/usergrid/persistence/query/tree/QueryFilter.g:1:10: T__21
                {
                mT__21(); 

                }
                break;
            case 2 :
                // org/usergrid/persistence/query/tree/QueryFilter.g:1:16: T__22
                {
                mT__22(); 

                }
                break;
            case 3 :
                // org/usergrid/persistence/query/tree/QueryFilter.g:1:22: T__23
                {
                mT__23(); 

                }
                break;
            case 4 :
                // org/usergrid/persistence/query/tree/QueryFilter.g:1:28: T__24
                {
                mT__24(); 

                }
                break;
            case 5 :
                // org/usergrid/persistence/query/tree/QueryFilter.g:1:34: T__25
                {
                mT__25(); 

                }
                break;
            case 6 :
                // org/usergrid/persistence/query/tree/QueryFilter.g:1:40: T__26
                {
                mT__26(); 

                }
                break;
            case 7 :
                // org/usergrid/persistence/query/tree/QueryFilter.g:1:46: T__27
                {
                mT__27(); 

                }
                break;
            case 8 :
                // org/usergrid/persistence/query/tree/QueryFilter.g:1:52: T__28
                {
                mT__28(); 

                }
                break;
            case 9 :
                // org/usergrid/persistence/query/tree/QueryFilter.g:1:58: T__29
                {
                mT__29(); 

                }
                break;
            case 10 :
                // org/usergrid/persistence/query/tree/QueryFilter.g:1:64: T__30
                {
                mT__30(); 

                }
                break;
            case 11 :
                // org/usergrid/persistence/query/tree/QueryFilter.g:1:70: T__31
                {
                mT__31(); 

                }
                break;
            case 12 :
                // org/usergrid/persistence/query/tree/QueryFilter.g:1:76: T__32
                {
                mT__32(); 

                }
                break;
            case 13 :
                // org/usergrid/persistence/query/tree/QueryFilter.g:1:82: T__33
                {
                mT__33(); 

                }
                break;
            case 14 :
                // org/usergrid/persistence/query/tree/QueryFilter.g:1:88: T__34
                {
                mT__34(); 

                }
                break;
            case 15 :
                // org/usergrid/persistence/query/tree/QueryFilter.g:1:94: T__35
                {
                mT__35(); 

                }
                break;
            case 16 :
                // org/usergrid/persistence/query/tree/QueryFilter.g:1:100: T__36
                {
                mT__36(); 

                }
                break;
            case 17 :
                // org/usergrid/persistence/query/tree/QueryFilter.g:1:106: T__37
                {
                mT__37(); 

                }
                break;
            case 18 :
                // org/usergrid/persistence/query/tree/QueryFilter.g:1:112: T__38
                {
                mT__38(); 

                }
                break;
            case 19 :
                // org/usergrid/persistence/query/tree/QueryFilter.g:1:118: ID
                {
                mID(); 

                }
                break;
            case 20 :
                // org/usergrid/persistence/query/tree/QueryFilter.g:1:121: INT
                {
                mINT(); 

                }
                break;
            case 21 :
                // org/usergrid/persistence/query/tree/QueryFilter.g:1:125: FLOAT
                {
                mFLOAT(); 

                }
                break;
            case 22 :
                // org/usergrid/persistence/query/tree/QueryFilter.g:1:131: STRING
                {
                mSTRING(); 

                }
                break;
            case 23 :
                // org/usergrid/persistence/query/tree/QueryFilter.g:1:138: UUID
                {
                mUUID(); 

                }
                break;
            case 24 :
                // org/usergrid/persistence/query/tree/QueryFilter.g:1:143: WS
                {
                mWS(); 

                }
                break;
            case 25 :
                // org/usergrid/persistence/query/tree/QueryFilter.g:1:146: LT
                {
                mLT(); 

                }
                break;
            case 26 :
                // org/usergrid/persistence/query/tree/QueryFilter.g:1:149: LTE
                {
                mLTE(); 

                }
                break;
            case 27 :
                // org/usergrid/persistence/query/tree/QueryFilter.g:1:153: EQ
                {
                mEQ(); 

                }
                break;
            case 28 :
                // org/usergrid/persistence/query/tree/QueryFilter.g:1:156: GT
                {
                mGT(); 

                }
                break;
            case 29 :
                // org/usergrid/persistence/query/tree/QueryFilter.g:1:159: GTE
                {
                mGTE(); 

                }
                break;

        }

    }


    protected DFA11 dfa11 = new DFA11(this);
    protected DFA24 dfa24 = new DFA24(this);
    static final String DFA11_eotS =
        "\5\uffff";
    static final String DFA11_eofS =
        "\5\uffff";
    static final String DFA11_minS =
        "\2\56\3\uffff";
    static final String DFA11_maxS =
        "\1\71\1\145\3\uffff";
    static final String DFA11_acceptS =
        "\2\uffff\1\2\1\1\1\3";
    static final String DFA11_specialS =
        "\5\uffff}>";
    static final String[] DFA11_transitionS = {
            "\1\2\1\uffff\12\1",
            "\1\3\1\uffff\12\1\13\uffff\1\4\37\uffff\1\4",
            "",
            "",
            ""
    };

    static final short[] DFA11_eot = DFA.unpackEncodedString(DFA11_eotS);
    static final short[] DFA11_eof = DFA.unpackEncodedString(DFA11_eofS);
    static final char[] DFA11_min = DFA.unpackEncodedStringToUnsignedChars(DFA11_minS);
    static final char[] DFA11_max = DFA.unpackEncodedStringToUnsignedChars(DFA11_maxS);
    static final short[] DFA11_accept = DFA.unpackEncodedString(DFA11_acceptS);
    static final short[] DFA11_special = DFA.unpackEncodedString(DFA11_specialS);
    static final short[][] DFA11_transition;

    static {
        int numStates = DFA11_transitionS.length;
        DFA11_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA11_transition[i] = DFA.unpackEncodedString(DFA11_transitionS[i]);
        }
    }

    class DFA11 extends DFA {

        public DFA11(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 11;
            this.eot = DFA11_eot;
            this.eof = DFA11_eof;
            this.min = DFA11_min;
            this.max = DFA11_max;
            this.accept = DFA11_accept;
            this.special = DFA11_special;
            this.transition = DFA11_transition;
        }
        public String getDescription() {
            return "59:15: ( ( '0' .. '9' )+ '.' ( '0' .. '9' )* ( EXPONENT )? | '.' ( '0' .. '9' )+ ( EXPONENT )? | ( '0' .. '9' )+ EXPONENT )";
        }
    }
    static final String DFA24_eotS =
        "\1\uffff\2\33\1\uffff\1\33\2\uffff\3\33\4\uffff\2\33\1\uffff\1\51"+
        "\2\uffff\1\33\1\uffff\1\57\1\33\1\uffff\1\33\1\62\1\uffff\2\33\1"+
        "\65\1\67\7\33\1\uffff\1\51\1\uffff\1\51\2\uffff\1\33\2\uffff\1\33"+
        "\2\uffff\2\33\1\uffff\1\33\1\uffff\2\33\1\111\1\112\1\113\2\33\1"+
        "\51\1\uffff\1\22\2\uffff\5\33\3\uffff\1\126\1\33\1\51\1\uffff\1"+
        "\22\1\33\1\134\3\33\1\uffff\1\33\1\51\1\uffff\1\22\1\144\2\uffff"+
        "\2\33\1\147\1\51\1\uffff\1\22\1\uffff\2\33\1\uffff\1\51\1\uffff"+
        "\1\22\1\160\1\33\1\51\1\uffff\1\22\2\uffff\4\22";
    static final String DFA24_eofS =
        "\166\uffff";
    static final String DFA24_minS =
        "\1\11\1\150\1\146\1\uffff\1\60\2\uffff\1\157\2\60\4\uffff\1\145"+
        "\1\60\2\56\2\uffff\1\164\1\uffff\1\75\1\164\1\uffff\1\60\1\75\1"+
        "\uffff\1\164\1\145\2\56\1\156\1\60\1\164\1\144\1\143\1\60\1\154"+
        "\1\uffff\1\56\1\uffff\1\56\1\53\1\uffff\1\145\2\uffff\1\145\2\uffff"+
        "\1\150\1\162\1\uffff\1\145\1\uffff\1\164\1\60\3\56\1\143\1\145\1"+
        "\56\1\53\1\60\2\uffff\1\151\1\145\1\162\1\141\1\60\3\uffff\1\56"+
        "\1\143\1\56\1\53\1\60\1\156\1\56\1\40\1\151\1\60\1\uffff\1\164\1"+
        "\56\1\53\1\60\1\56\2\uffff\1\156\1\60\2\56\1\53\1\60\1\uffff\1\163"+
        "\1\60\1\uffff\1\56\1\53\1\60\1\56\2\55\1\53\1\55\1\uffff\4\60\1"+
        "\55";
    static final String DFA24_maxS =
        "\1\175\1\151\1\162\1\uffff\1\157\2\uffff\1\157\1\163\1\146\4\uffff"+
        "\1\145\1\161\1\71\1\146\2\uffff\1\164\1\uffff\1\75\1\164\1\uffff"+
        "\1\146\1\75\1\uffff\1\164\1\145\2\172\1\156\1\146\1\164\1\144\1"+
        "\143\1\163\1\154\1\uffff\1\145\1\uffff\2\146\1\uffff\1\145\2\uffff"+
        "\1\145\2\uffff\1\150\1\162\1\uffff\1\145\1\uffff\1\164\1\146\3\172"+
        "\1\143\1\145\3\146\2\uffff\1\151\1\145\1\162\1\141\1\146\3\uffff"+
        "\1\172\1\143\3\146\1\156\1\172\1\40\1\151\1\146\1\uffff\1\164\3"+
        "\146\1\172\2\uffff\1\156\1\146\1\172\3\146\1\uffff\1\163\1\146\1"+
        "\uffff\3\146\1\172\1\55\1\145\1\71\1\55\1\uffff\4\146\1\55";
    static final String DFA24_acceptS =
        "\3\uffff\1\3\1\uffff\1\5\1\6\3\uffff\1\14\1\15\1\16\1\17\4\uffff"+
        "\1\25\1\26\1\uffff\1\30\2\uffff\1\33\2\uffff\1\23\13\uffff\1\23"+
        "\1\uffff\1\24\2\uffff\1\27\1\uffff\1\32\1\31\1\uffff\1\35\1\34\2"+
        "\uffff\1\2\1\uffff\1\11\12\uffff\2\23\5\uffff\1\7\1\10\1\12\12\uffff"+
        "\1\13\5\uffff\1\21\1\22\6\uffff\1\1\2\uffff\1\20\10\uffff\1\4\5"+
        "\uffff";
    static final String DFA24_specialS =
        "\166\uffff}>";
    static final String[] DFA24_transitionS = {
            "\2\25\1\uffff\2\25\22\uffff\1\25\6\uffff\1\23\1\5\1\6\1\13\1"+
            "\uffff\1\3\1\20\1\22\1\uffff\12\21\1\12\1\uffff\1\26\1\30\1"+
            "\32\2\uffff\6\31\24\33\4\uffff\1\33\1\uffff\1\10\1\31\1\4\1"+
            "\11\1\17\1\31\1\27\4\33\1\24\1\33\1\7\1\2\3\33\1\16\3\33\1\1"+
            "\3\33\1\14\1\uffff\1\15",
            "\1\35\1\34",
            "\1\36\13\uffff\1\37",
            "",
            "\12\41\7\uffff\6\41\32\uffff\6\41\10\uffff\1\40",
            "",
            "",
            "\1\42",
            "\12\41\7\uffff\6\41\32\uffff\6\41\7\uffff\1\43\4\uffff\1\44",
            "\12\41\7\uffff\6\41\32\uffff\4\41\1\45\1\41",
            "",
            "",
            "",
            "",
            "\1\46",
            "\12\41\7\uffff\6\41\32\uffff\6\41\12\uffff\1\47",
            "\1\22\1\uffff\12\50",
            "\1\22\1\uffff\12\52\7\uffff\4\54\1\53\1\54\32\uffff\4\54\1"+
            "\53\1\54",
            "",
            "",
            "\1\55",
            "",
            "\1\56",
            "\1\60",
            "",
            "\12\41\7\uffff\6\41\32\uffff\6\41",
            "\1\61",
            "",
            "\1\63",
            "\1\64",
            "\1\33\1\uffff\12\33\7\uffff\32\33\4\uffff\1\33\1\uffff\32\33",
            "\1\33\1\uffff\12\33\7\uffff\32\33\4\uffff\1\33\1\uffff\3\33"+
            "\1\66\26\33",
            "\1\70",
            "\12\71\7\uffff\6\71\32\uffff\6\71",
            "\1\72",
            "\1\73",
            "\1\74",
            "\12\71\7\uffff\6\71\32\uffff\6\71\14\uffff\1\75",
            "\1\76",
            "",
            "\1\22\1\uffff\12\50\13\uffff\1\22\37\uffff\1\22",
            "",
            "\1\22\1\uffff\12\77\7\uffff\4\54\1\100\1\54\32\uffff\4\54\1"+
            "\100\1\54",
            "\1\22\1\uffff\1\22\2\uffff\12\101\7\uffff\6\54\32\uffff\6\54",
            "",
            "\1\102",
            "",
            "",
            "\1\103",
            "",
            "",
            "\1\104",
            "\1\105",
            "",
            "\1\106",
            "",
            "\1\107",
            "\12\110\7\uffff\6\110\32\uffff\6\110",
            "\1\33\1\uffff\12\33\7\uffff\32\33\4\uffff\1\33\1\uffff\32\33",
            "\1\33\1\uffff\12\33\7\uffff\32\33\4\uffff\1\33\1\uffff\32\33",
            "\1\33\1\uffff\12\33\7\uffff\32\33\4\uffff\1\33\1\uffff\32\33",
            "\1\114",
            "\1\115",
            "\1\22\1\uffff\12\116\7\uffff\4\54\1\117\1\54\32\uffff\4\54"+
            "\1\117\1\54",
            "\1\22\1\uffff\1\22\2\uffff\12\120\7\uffff\6\54\32\uffff\6\54",
            "\12\120\7\uffff\6\54\32\uffff\6\54",
            "",
            "",
            "\1\121",
            "\1\122",
            "\1\123",
            "\1\124",
            "\12\125\7\uffff\6\125\32\uffff\6\125",
            "",
            "",
            "",
            "\1\33\1\uffff\12\33\7\uffff\32\33\4\uffff\1\33\1\uffff\32\33",
            "\1\127",
            "\1\22\1\uffff\12\130\7\uffff\4\54\1\131\1\54\32\uffff\4\54"+
            "\1\131\1\54",
            "\1\22\1\uffff\1\22\2\uffff\12\132\7\uffff\6\54\32\uffff\6\54",
            "\12\132\7\uffff\6\54\32\uffff\6\54",
            "\1\133",
            "\1\33\1\uffff\12\33\7\uffff\32\33\4\uffff\1\33\1\uffff\32\33",
            "\1\135",
            "\1\136",
            "\12\137\7\uffff\6\137\32\uffff\6\137",
            "",
            "\1\140",
            "\1\22\1\uffff\12\141\7\uffff\4\54\1\142\1\54\32\uffff\4\54"+
            "\1\142\1\54",
            "\1\22\1\uffff\1\22\2\uffff\12\143\7\uffff\6\54\32\uffff\6\54",
            "\12\143\7\uffff\6\54\32\uffff\6\54",
            "\1\33\1\uffff\12\33\7\uffff\32\33\4\uffff\1\33\1\uffff\32\33",
            "",
            "",
            "\1\145",
            "\12\146\7\uffff\6\146\32\uffff\6\146",
            "\1\33\1\uffff\12\33\7\uffff\32\33\4\uffff\1\33\1\uffff\32\33",
            "\1\22\1\uffff\12\150\7\uffff\4\54\1\151\1\54\32\uffff\4\54"+
            "\1\151\1\54",
            "\1\22\1\uffff\1\22\2\uffff\12\152\7\uffff\6\54\32\uffff\6\54",
            "\12\152\7\uffff\6\54\32\uffff\6\54",
            "",
            "\1\153",
            "\12\154\7\uffff\6\154\32\uffff\6\154",
            "",
            "\1\22\1\uffff\12\155\7\uffff\4\54\1\156\1\54\32\uffff\4\54"+
            "\1\156\1\54",
            "\1\22\1\uffff\1\22\2\uffff\12\157\7\uffff\6\54\32\uffff\6\54",
            "\12\157\7\uffff\6\54\32\uffff\6\54",
            "\1\33\1\uffff\12\33\7\uffff\32\33\4\uffff\1\33\1\uffff\32\33",
            "\1\54",
            "\1\54\1\22\1\uffff\12\50\13\uffff\1\22\37\uffff\1\22",
            "\1\22\1\uffff\1\161\2\uffff\12\22",
            "\1\54",
            "",
            "\12\162\7\uffff\6\54\32\uffff\6\54",
            "\12\163\7\uffff\6\54\32\uffff\6\54",
            "\12\164\7\uffff\6\54\32\uffff\6\54",
            "\12\165\7\uffff\6\54\32\uffff\6\54",
            "\1\54"
    };

    static final short[] DFA24_eot = DFA.unpackEncodedString(DFA24_eotS);
    static final short[] DFA24_eof = DFA.unpackEncodedString(DFA24_eofS);
    static final char[] DFA24_min = DFA.unpackEncodedStringToUnsignedChars(DFA24_minS);
    static final char[] DFA24_max = DFA.unpackEncodedStringToUnsignedChars(DFA24_maxS);
    static final short[] DFA24_accept = DFA.unpackEncodedString(DFA24_acceptS);
    static final short[] DFA24_special = DFA.unpackEncodedString(DFA24_specialS);
    static final short[][] DFA24_transition;

    static {
        int numStates = DFA24_transitionS.length;
        DFA24_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA24_transition[i] = DFA.unpackEncodedString(DFA24_transitionS[i]);
        }
    }

    class DFA24 extends DFA {

        public DFA24(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 24;
            this.eot = DFA24_eot;
            this.eof = DFA24_eof;
            this.min = DFA24_min;
            this.max = DFA24_max;
            this.accept = DFA24_accept;
            this.special = DFA24_special;
            this.transition = DFA24_transition;
        }
        public String getDescription() {
            return "1:1: Tokens : ( T__21 | T__22 | T__23 | T__24 | T__25 | T__26 | T__27 | T__28 | T__29 | T__30 | T__31 | T__32 | T__33 | T__34 | T__35 | T__36 | T__37 | T__38 | ID | INT | FLOAT | STRING | UUID | WS | LT | LTE | EQ | GT | GTE );";
        }
    }
 

}