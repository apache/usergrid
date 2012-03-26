// $ANTLR 3.4 org/usergrid/persistence/query/tree/QueryFilter.g 2012-03-26 00:24:46

package org.usergrid.persistence.query.tree;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked"})
public class QueryFilterLexer extends Lexer {
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


      private static final Logger logger = LoggerFactory
          .getLogger(QueryFilterLexer.class);

    	@Override
    	public void emitErrorMessage(String msg) {
    		logger.info(msg);
    	}


    // delegates
    // delegators
    public Lexer[] getDelegates() {
        return new Lexer[] {};
    }

    public QueryFilterLexer() {} 
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
            // org/usergrid/persistence/query/tree/QueryFilter.g:20:7: ( '(' )
            // org/usergrid/persistence/query/tree/QueryFilter.g:20:9: '('
            {
            match('('); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "T__21"

    // $ANTLR start "T__22"
    public final void mT__22() throws RecognitionException {
        try {
            int _type = T__22;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:21:7: ( ')' )
            // org/usergrid/persistence/query/tree/QueryFilter.g:21:9: ')'
            {
            match(')'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "T__22"

    // $ANTLR start "T__23"
    public final void mT__23() throws RecognitionException {
        try {
            int _type = T__23;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:22:7: ( '*' )
            // org/usergrid/persistence/query/tree/QueryFilter.g:22:9: '*'
            {
            match('*'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "T__23"

    // $ANTLR start "T__24"
    public final void mT__24() throws RecognitionException {
        try {
            int _type = T__24;
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
        	// do for sure before leaving
        }
    }
    // $ANTLR end "T__24"

    // $ANTLR start "T__25"
    public final void mT__25() throws RecognitionException {
        try {
            int _type = T__25;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:24:7: ( ':' )
            // org/usergrid/persistence/query/tree/QueryFilter.g:24:9: ':'
            {
            match(':'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "T__25"

    // $ANTLR start "T__26"
    public final void mT__26() throws RecognitionException {
        try {
            int _type = T__26;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:25:7: ( 'and' )
            // org/usergrid/persistence/query/tree/QueryFilter.g:25:9: 'and'
            {
            match("and"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "T__26"

    // $ANTLR start "T__27"
    public final void mT__27() throws RecognitionException {
        try {
            int _type = T__27;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:26:7: ( 'asc' )
            // org/usergrid/persistence/query/tree/QueryFilter.g:26:9: 'asc'
            {
            match("asc"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "T__27"

    // $ANTLR start "T__28"
    public final void mT__28() throws RecognitionException {
        try {
            int _type = T__28;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:27:7: ( 'contains' )
            // org/usergrid/persistence/query/tree/QueryFilter.g:27:9: 'contains'
            {
            match("contains"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "T__28"

    // $ANTLR start "T__29"
    public final void mT__29() throws RecognitionException {
        try {
            int _type = T__29;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:28:7: ( 'desc' )
            // org/usergrid/persistence/query/tree/QueryFilter.g:28:9: 'desc'
            {
            match("desc"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "T__29"

    // $ANTLR start "T__30"
    public final void mT__30() throws RecognitionException {
        try {
            int _type = T__30;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:29:7: ( 'not' )
            // org/usergrid/persistence/query/tree/QueryFilter.g:29:9: 'not'
            {
            match("not"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "T__30"

    // $ANTLR start "T__31"
    public final void mT__31() throws RecognitionException {
        try {
            int _type = T__31;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:30:7: ( 'of' )
            // org/usergrid/persistence/query/tree/QueryFilter.g:30:9: 'of'
            {
            match("of"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "T__31"

    // $ANTLR start "T__32"
    public final void mT__32() throws RecognitionException {
        try {
            int _type = T__32;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:31:7: ( 'or' )
            // org/usergrid/persistence/query/tree/QueryFilter.g:31:9: 'or'
            {
            match("or"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "T__32"

    // $ANTLR start "T__33"
    public final void mT__33() throws RecognitionException {
        try {
            int _type = T__33;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:32:7: ( 'order by' )
            // org/usergrid/persistence/query/tree/QueryFilter.g:32:9: 'order by'
            {
            match("order by"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "T__33"

    // $ANTLR start "T__34"
    public final void mT__34() throws RecognitionException {
        try {
            int _type = T__34;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:33:7: ( 'select' )
            // org/usergrid/persistence/query/tree/QueryFilter.g:33:9: 'select'
            {
            match("select"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "T__34"

    // $ANTLR start "T__35"
    public final void mT__35() throws RecognitionException {
        try {
            int _type = T__35;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:34:7: ( 'where' )
            // org/usergrid/persistence/query/tree/QueryFilter.g:34:9: 'where'
            {
            match("where"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "T__35"

    // $ANTLR start "T__36"
    public final void mT__36() throws RecognitionException {
        try {
            int _type = T__36;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:35:7: ( 'within' )
            // org/usergrid/persistence/query/tree/QueryFilter.g:35:9: 'within'
            {
            match("within"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "T__36"

    // $ANTLR start "T__37"
    public final void mT__37() throws RecognitionException {
        try {
            int _type = T__37;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:36:7: ( '{' )
            // org/usergrid/persistence/query/tree/QueryFilter.g:36:9: '{'
            {
            match('{'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "T__37"

    // $ANTLR start "T__38"
    public final void mT__38() throws RecognitionException {
        try {
            int _type = T__38;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:37:7: ( '}' )
            // org/usergrid/persistence/query/tree/QueryFilter.g:37:9: '}'
            {
            match('}'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "T__38"

    // $ANTLR start "LT"
    public final void mLT() throws RecognitionException {
        try {
            int _type = LT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:53:5: ( '<' | 'lt' )
            int alt1=2;
            switch ( input.LA(1) ) {
            case '<':
                {
                alt1=1;
                }
                break;
            case 'l':
                {
                alt1=2;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 1, 0, input);

                throw nvae;

            }

            switch (alt1) {
                case 1 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:53:7: '<'
                    {
                    match('<'); 

                    }
                    break;
                case 2 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:53:13: 'lt'
                    {
                    match("lt"); 



                    }
                    break;

            }
            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "LT"

    // $ANTLR start "LTE"
    public final void mLTE() throws RecognitionException {
        try {
            int _type = LTE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:55:5: ( '<=' | 'lte' )
            int alt2=2;
            switch ( input.LA(1) ) {
            case '<':
                {
                alt2=1;
                }
                break;
            case 'l':
                {
                alt2=2;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 2, 0, input);

                throw nvae;

            }

            switch (alt2) {
                case 1 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:55:7: '<='
                    {
                    match("<="); 



                    }
                    break;
                case 2 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:55:15: 'lte'
                    {
                    match("lte"); 



                    }
                    break;

            }
            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "LTE"

    // $ANTLR start "EQ"
    public final void mEQ() throws RecognitionException {
        try {
            int _type = EQ;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:57:5: ( '=' | 'eq' )
            int alt3=2;
            switch ( input.LA(1) ) {
            case '=':
                {
                alt3=1;
                }
                break;
            case 'e':
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
                    // org/usergrid/persistence/query/tree/QueryFilter.g:57:7: '='
                    {
                    match('='); 

                    }
                    break;
                case 2 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:57:13: 'eq'
                    {
                    match("eq"); 



                    }
                    break;

            }
            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "EQ"

    // $ANTLR start "GT"
    public final void mGT() throws RecognitionException {
        try {
            int _type = GT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:59:5: ( '>' | 'gt' )
            int alt4=2;
            switch ( input.LA(1) ) {
            case '>':
                {
                alt4=1;
                }
                break;
            case 'g':
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
                    // org/usergrid/persistence/query/tree/QueryFilter.g:59:7: '>'
                    {
                    match('>'); 

                    }
                    break;
                case 2 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:59:13: 'gt'
                    {
                    match("gt"); 



                    }
                    break;

            }
            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "GT"

    // $ANTLR start "GTE"
    public final void mGTE() throws RecognitionException {
        try {
            int _type = GTE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:61:5: ( '>=' | 'gte' )
            int alt5=2;
            switch ( input.LA(1) ) {
            case '>':
                {
                alt5=1;
                }
                break;
            case 'g':
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
                    // org/usergrid/persistence/query/tree/QueryFilter.g:61:7: '>='
                    {
                    match(">="); 



                    }
                    break;
                case 2 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:61:15: 'gte'
                    {
                    match("gte"); 



                    }
                    break;

            }
            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "GTE"

    // $ANTLR start "ID"
    public final void mID() throws RecognitionException {
        try {
            int _type = ID;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:64:5: ( ( 'a' .. 'z' | 'A' .. 'Z' | '_' ) ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '_' | '.' )* )
            // org/usergrid/persistence/query/tree/QueryFilter.g:64:7: ( 'a' .. 'z' | 'A' .. 'Z' | '_' ) ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '_' | '.' )*
            {
            if ( (input.LA(1) >= 'A' && input.LA(1) <= 'Z')||input.LA(1)=='_'||(input.LA(1) >= 'a' && input.LA(1) <= 'z') ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            // org/usergrid/persistence/query/tree/QueryFilter.g:64:31: ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '_' | '.' )*
            loop6:
            do {
                int alt6=2;
                switch ( input.LA(1) ) {
                case '.':
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                case 'A':
                case 'B':
                case 'C':
                case 'D':
                case 'E':
                case 'F':
                case 'G':
                case 'H':
                case 'I':
                case 'J':
                case 'K':
                case 'L':
                case 'M':
                case 'N':
                case 'O':
                case 'P':
                case 'Q':
                case 'R':
                case 'S':
                case 'T':
                case 'U':
                case 'V':
                case 'W':
                case 'X':
                case 'Y':
                case 'Z':
                case '_':
                case 'a':
                case 'b':
                case 'c':
                case 'd':
                case 'e':
                case 'f':
                case 'g':
                case 'h':
                case 'i':
                case 'j':
                case 'k':
                case 'l':
                case 'm':
                case 'n':
                case 'o':
                case 'p':
                case 'q':
                case 'r':
                case 's':
                case 't':
                case 'u':
                case 'v':
                case 'w':
                case 'x':
                case 'y':
                case 'z':
                    {
                    alt6=1;
                    }
                    break;

                }

                switch (alt6) {
            	case 1 :
            	    // org/usergrid/persistence/query/tree/QueryFilter.g:
            	    {
            	    if ( input.LA(1)=='.'||(input.LA(1) >= '0' && input.LA(1) <= '9')||(input.LA(1) >= 'A' && input.LA(1) <= 'Z')||input.LA(1)=='_'||(input.LA(1) >= 'a' && input.LA(1) <= 'z') ) {
            	        input.consume();
            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;
            	    }


            	    }
            	    break;

            	default :
            	    break loop6;
                }
            } while (true);


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "ID"

    // $ANTLR start "INT"
    public final void mINT() throws RecognitionException {
        try {
            int _type = INT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:67:5: ( ( '-' )? ( '0' .. '9' )+ )
            // org/usergrid/persistence/query/tree/QueryFilter.g:67:7: ( '-' )? ( '0' .. '9' )+
            {
            // org/usergrid/persistence/query/tree/QueryFilter.g:67:7: ( '-' )?
            int alt7=2;
            switch ( input.LA(1) ) {
                case '-':
                    {
                    alt7=1;
                    }
                    break;
            }

            switch (alt7) {
                case 1 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:67:8: '-'
                    {
                    match('-'); 

                    }
                    break;

            }


            // org/usergrid/persistence/query/tree/QueryFilter.g:67:14: ( '0' .. '9' )+
            int cnt8=0;
            loop8:
            do {
                int alt8=2;
                switch ( input.LA(1) ) {
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    {
                    alt8=1;
                    }
                    break;

                }

                switch (alt8) {
            	case 1 :
            	    // org/usergrid/persistence/query/tree/QueryFilter.g:
            	    {
            	    if ( (input.LA(1) >= '0' && input.LA(1) <= '9') ) {
            	        input.consume();
            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;
            	    }


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


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "INT"

    // $ANTLR start "FLOAT"
    public final void mFLOAT() throws RecognitionException {
        try {
            int _type = FLOAT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:71:5: ( ( '-' )? ( ( '0' .. '9' )+ '.' ( '0' .. '9' )* ( EXPONENT )? | '.' ( '0' .. '9' )+ ( EXPONENT )? | ( '0' .. '9' )+ EXPONENT ) )
            // org/usergrid/persistence/query/tree/QueryFilter.g:71:8: ( '-' )? ( ( '0' .. '9' )+ '.' ( '0' .. '9' )* ( EXPONENT )? | '.' ( '0' .. '9' )+ ( EXPONENT )? | ( '0' .. '9' )+ EXPONENT )
            {
            // org/usergrid/persistence/query/tree/QueryFilter.g:71:8: ( '-' )?
            int alt9=2;
            switch ( input.LA(1) ) {
                case '-':
                    {
                    alt9=1;
                    }
                    break;
            }

            switch (alt9) {
                case 1 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:71:9: '-'
                    {
                    match('-'); 

                    }
                    break;

            }


            // org/usergrid/persistence/query/tree/QueryFilter.g:71:15: ( ( '0' .. '9' )+ '.' ( '0' .. '9' )* ( EXPONENT )? | '.' ( '0' .. '9' )+ ( EXPONENT )? | ( '0' .. '9' )+ EXPONENT )
            int alt16=3;
            alt16 = dfa16.predict(input);
            switch (alt16) {
                case 1 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:71:17: ( '0' .. '9' )+ '.' ( '0' .. '9' )* ( EXPONENT )?
                    {
                    // org/usergrid/persistence/query/tree/QueryFilter.g:71:17: ( '0' .. '9' )+
                    int cnt10=0;
                    loop10:
                    do {
                        int alt10=2;
                        switch ( input.LA(1) ) {
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            {
                            alt10=1;
                            }
                            break;

                        }

                        switch (alt10) {
                    	case 1 :
                    	    // org/usergrid/persistence/query/tree/QueryFilter.g:
                    	    {
                    	    if ( (input.LA(1) >= '0' && input.LA(1) <= '9') ) {
                    	        input.consume();
                    	    }
                    	    else {
                    	        MismatchedSetException mse = new MismatchedSetException(null,input);
                    	        recover(mse);
                    	        throw mse;
                    	    }


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


                    match('.'); 

                    // org/usergrid/persistence/query/tree/QueryFilter.g:71:33: ( '0' .. '9' )*
                    loop11:
                    do {
                        int alt11=2;
                        switch ( input.LA(1) ) {
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            {
                            alt11=1;
                            }
                            break;

                        }

                        switch (alt11) {
                    	case 1 :
                    	    // org/usergrid/persistence/query/tree/QueryFilter.g:
                    	    {
                    	    if ( (input.LA(1) >= '0' && input.LA(1) <= '9') ) {
                    	        input.consume();
                    	    }
                    	    else {
                    	        MismatchedSetException mse = new MismatchedSetException(null,input);
                    	        recover(mse);
                    	        throw mse;
                    	    }


                    	    }
                    	    break;

                    	default :
                    	    break loop11;
                        }
                    } while (true);


                    // org/usergrid/persistence/query/tree/QueryFilter.g:71:45: ( EXPONENT )?
                    int alt12=2;
                    switch ( input.LA(1) ) {
                        case 'E':
                        case 'e':
                            {
                            alt12=1;
                            }
                            break;
                    }

                    switch (alt12) {
                        case 1 :
                            // org/usergrid/persistence/query/tree/QueryFilter.g:71:45: EXPONENT
                            {
                            mEXPONENT(); 


                            }
                            break;

                    }


                    }
                    break;
                case 2 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:72:9: '.' ( '0' .. '9' )+ ( EXPONENT )?
                    {
                    match('.'); 

                    // org/usergrid/persistence/query/tree/QueryFilter.g:72:13: ( '0' .. '9' )+
                    int cnt13=0;
                    loop13:
                    do {
                        int alt13=2;
                        switch ( input.LA(1) ) {
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            {
                            alt13=1;
                            }
                            break;

                        }

                        switch (alt13) {
                    	case 1 :
                    	    // org/usergrid/persistence/query/tree/QueryFilter.g:
                    	    {
                    	    if ( (input.LA(1) >= '0' && input.LA(1) <= '9') ) {
                    	        input.consume();
                    	    }
                    	    else {
                    	        MismatchedSetException mse = new MismatchedSetException(null,input);
                    	        recover(mse);
                    	        throw mse;
                    	    }


                    	    }
                    	    break;

                    	default :
                    	    if ( cnt13 >= 1 ) break loop13;
                                EarlyExitException eee =
                                    new EarlyExitException(13, input);
                                throw eee;
                        }
                        cnt13++;
                    } while (true);


                    // org/usergrid/persistence/query/tree/QueryFilter.g:72:25: ( EXPONENT )?
                    int alt14=2;
                    switch ( input.LA(1) ) {
                        case 'E':
                        case 'e':
                            {
                            alt14=1;
                            }
                            break;
                    }

                    switch (alt14) {
                        case 1 :
                            // org/usergrid/persistence/query/tree/QueryFilter.g:72:25: EXPONENT
                            {
                            mEXPONENT(); 


                            }
                            break;

                    }


                    }
                    break;
                case 3 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:73:9: ( '0' .. '9' )+ EXPONENT
                    {
                    // org/usergrid/persistence/query/tree/QueryFilter.g:73:9: ( '0' .. '9' )+
                    int cnt15=0;
                    loop15:
                    do {
                        int alt15=2;
                        switch ( input.LA(1) ) {
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            {
                            alt15=1;
                            }
                            break;

                        }

                        switch (alt15) {
                    	case 1 :
                    	    // org/usergrid/persistence/query/tree/QueryFilter.g:
                    	    {
                    	    if ( (input.LA(1) >= '0' && input.LA(1) <= '9') ) {
                    	        input.consume();
                    	    }
                    	    else {
                    	        MismatchedSetException mse = new MismatchedSetException(null,input);
                    	        recover(mse);
                    	        throw mse;
                    	    }


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


                    mEXPONENT(); 


                    }
                    break;

            }


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "FLOAT"

    // $ANTLR start "STRING"
    public final void mSTRING() throws RecognitionException {
        try {
            int _type = STRING;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:77:5: ( '\\'' ( ESC_SEQ |~ ( '\\\\' | '\\'' ) )* '\\'' )
            // org/usergrid/persistence/query/tree/QueryFilter.g:77:8: '\\'' ( ESC_SEQ |~ ( '\\\\' | '\\'' ) )* '\\''
            {
            match('\''); 

            // org/usergrid/persistence/query/tree/QueryFilter.g:77:13: ( ESC_SEQ |~ ( '\\\\' | '\\'' ) )*
            loop17:
            do {
                int alt17=3;
                int LA17_0 = input.LA(1);

                if ( (LA17_0=='\\') ) {
                    alt17=1;
                }
                else if ( ((LA17_0 >= '\u0000' && LA17_0 <= '&')||(LA17_0 >= '(' && LA17_0 <= '[')||(LA17_0 >= ']' && LA17_0 <= '\uFFFF')) ) {
                    alt17=2;
                }


                switch (alt17) {
            	case 1 :
            	    // org/usergrid/persistence/query/tree/QueryFilter.g:77:15: ESC_SEQ
            	    {
            	    mESC_SEQ(); 


            	    }
            	    break;
            	case 2 :
            	    // org/usergrid/persistence/query/tree/QueryFilter.g:77:25: ~ ( '\\\\' | '\\'' )
            	    {
            	    if ( (input.LA(1) >= '\u0000' && input.LA(1) <= '&')||(input.LA(1) >= '(' && input.LA(1) <= '[')||(input.LA(1) >= ']' && input.LA(1) <= '\uFFFF') ) {
            	        input.consume();
            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;
            	    }


            	    }
            	    break;

            	default :
            	    break loop17;
                }
            } while (true);


            match('\''); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "STRING"

    // $ANTLR start "BOOLEAN"
    public final void mBOOLEAN() throws RecognitionException {
        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:82:9: ( ( 'true' | 'false' ) )
            // org/usergrid/persistence/query/tree/QueryFilter.g:82:11: ( 'true' | 'false' )
            {
            // org/usergrid/persistence/query/tree/QueryFilter.g:82:11: ( 'true' | 'false' )
            int alt18=2;
            switch ( input.LA(1) ) {
            case 't':
                {
                alt18=1;
                }
                break;
            case 'f':
                {
                alt18=2;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 18, 0, input);

                throw nvae;

            }

            switch (alt18) {
                case 1 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:82:12: 'true'
                    {
                    match("true"); 



                    }
                    break;
                case 2 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:82:20: 'false'
                    {
                    match("false"); 



                    }
                    break;

            }


            }


        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "BOOLEAN"

    // $ANTLR start "UUID"
    public final void mUUID() throws RecognitionException {
        try {
            int _type = UUID;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:83:6: ( HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT '-' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT '-' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT '-' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT '-' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT )
            // org/usergrid/persistence/query/tree/QueryFilter.g:83:8: HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT '-' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT '-' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT '-' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT '-' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT
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
        	// do for sure before leaving
        }
    }
    // $ANTLR end "UUID"

    // $ANTLR start "EXPONENT"
    public final void mEXPONENT() throws RecognitionException {
        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:95:10: ( ( 'e' | 'E' ) ( '+' | '-' )? ( '0' .. '9' )+ )
            // org/usergrid/persistence/query/tree/QueryFilter.g:95:12: ( 'e' | 'E' ) ( '+' | '-' )? ( '0' .. '9' )+
            {
            if ( input.LA(1)=='E'||input.LA(1)=='e' ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            // org/usergrid/persistence/query/tree/QueryFilter.g:95:22: ( '+' | '-' )?
            int alt19=2;
            switch ( input.LA(1) ) {
                case '+':
                case '-':
                    {
                    alt19=1;
                    }
                    break;
            }

            switch (alt19) {
                case 1 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:
                    {
                    if ( input.LA(1)=='+'||input.LA(1)=='-' ) {
                        input.consume();
                    }
                    else {
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;
                    }


                    }
                    break;

            }


            // org/usergrid/persistence/query/tree/QueryFilter.g:95:33: ( '0' .. '9' )+
            int cnt20=0;
            loop20:
            do {
                int alt20=2;
                switch ( input.LA(1) ) {
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    {
                    alt20=1;
                    }
                    break;

                }

                switch (alt20) {
            	case 1 :
            	    // org/usergrid/persistence/query/tree/QueryFilter.g:
            	    {
            	    if ( (input.LA(1) >= '0' && input.LA(1) <= '9') ) {
            	        input.consume();
            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;
            	    }


            	    }
            	    break;

            	default :
            	    if ( cnt20 >= 1 ) break loop20;
                        EarlyExitException eee =
                            new EarlyExitException(20, input);
                        throw eee;
                }
                cnt20++;
            } while (true);


            }


        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "EXPONENT"

    // $ANTLR start "HEX_DIGIT"
    public final void mHEX_DIGIT() throws RecognitionException {
        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:98:11: ( ( '0' .. '9' | 'a' .. 'f' | 'A' .. 'F' ) )
            // org/usergrid/persistence/query/tree/QueryFilter.g:
            {
            if ( (input.LA(1) >= '0' && input.LA(1) <= '9')||(input.LA(1) >= 'A' && input.LA(1) <= 'F')||(input.LA(1) >= 'a' && input.LA(1) <= 'f') ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            }


        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "HEX_DIGIT"

    // $ANTLR start "ESC_SEQ"
    public final void mESC_SEQ() throws RecognitionException {
        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:102:5: ( '\\\\' ( 'b' | 't' | 'n' | 'f' | 'r' | '\\\"' | '\\'' | '\\\\' ) | UNICODE_ESC | OCTAL_ESC )
            int alt21=3;
            switch ( input.LA(1) ) {
            case '\\':
                {
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
                    alt21=1;
                    }
                    break;
                case 'u':
                    {
                    alt21=2;
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
                    alt21=3;
                    }
                    break;
                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 21, 1, input);

                    throw nvae;

                }

                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 21, 0, input);

                throw nvae;

            }

            switch (alt21) {
                case 1 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:102:9: '\\\\' ( 'b' | 't' | 'n' | 'f' | 'r' | '\\\"' | '\\'' | '\\\\' )
                    {
                    match('\\'); 

                    if ( input.LA(1)=='\"'||input.LA(1)=='\''||input.LA(1)=='\\'||input.LA(1)=='b'||input.LA(1)=='f'||input.LA(1)=='n'||input.LA(1)=='r'||input.LA(1)=='t' ) {
                        input.consume();
                    }
                    else {
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;
                    }


                    }
                    break;
                case 2 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:103:9: UNICODE_ESC
                    {
                    mUNICODE_ESC(); 


                    }
                    break;
                case 3 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:104:9: OCTAL_ESC
                    {
                    mOCTAL_ESC(); 


                    }
                    break;

            }

        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "ESC_SEQ"

    // $ANTLR start "OCTAL_ESC"
    public final void mOCTAL_ESC() throws RecognitionException {
        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:109:5: ( '\\\\' ( '0' .. '3' ) ( '0' .. '7' ) ( '0' .. '7' ) | '\\\\' ( '0' .. '7' ) ( '0' .. '7' ) | '\\\\' ( '0' .. '7' ) )
            int alt22=3;
            switch ( input.LA(1) ) {
            case '\\':
                {
                switch ( input.LA(2) ) {
                case '0':
                case '1':
                case '2':
                case '3':
                    {
                    switch ( input.LA(3) ) {
                    case '0':
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                        {
                        switch ( input.LA(4) ) {
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                            {
                            alt22=1;
                            }
                            break;
                        default:
                            alt22=2;
                        }

                        }
                        break;
                    default:
                        alt22=3;
                    }

                    }
                    break;
                case '4':
                case '5':
                case '6':
                case '7':
                    {
                    switch ( input.LA(3) ) {
                    case '0':
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                        {
                        alt22=2;
                        }
                        break;
                    default:
                        alt22=3;
                    }

                    }
                    break;
                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 22, 1, input);

                    throw nvae;

                }

                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 22, 0, input);

                throw nvae;

            }

            switch (alt22) {
                case 1 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:109:9: '\\\\' ( '0' .. '3' ) ( '0' .. '7' ) ( '0' .. '7' )
                    {
                    match('\\'); 

                    if ( (input.LA(1) >= '0' && input.LA(1) <= '3') ) {
                        input.consume();
                    }
                    else {
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;
                    }


                    if ( (input.LA(1) >= '0' && input.LA(1) <= '7') ) {
                        input.consume();
                    }
                    else {
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;
                    }


                    if ( (input.LA(1) >= '0' && input.LA(1) <= '7') ) {
                        input.consume();
                    }
                    else {
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;
                    }


                    }
                    break;
                case 2 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:110:9: '\\\\' ( '0' .. '7' ) ( '0' .. '7' )
                    {
                    match('\\'); 

                    if ( (input.LA(1) >= '0' && input.LA(1) <= '7') ) {
                        input.consume();
                    }
                    else {
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;
                    }


                    if ( (input.LA(1) >= '0' && input.LA(1) <= '7') ) {
                        input.consume();
                    }
                    else {
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;
                    }


                    }
                    break;
                case 3 :
                    // org/usergrid/persistence/query/tree/QueryFilter.g:111:9: '\\\\' ( '0' .. '7' )
                    {
                    match('\\'); 

                    if ( (input.LA(1) >= '0' && input.LA(1) <= '7') ) {
                        input.consume();
                    }
                    else {
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;
                    }


                    }
                    break;

            }

        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "OCTAL_ESC"

    // $ANTLR start "UNICODE_ESC"
    public final void mUNICODE_ESC() throws RecognitionException {
        try {
            // org/usergrid/persistence/query/tree/QueryFilter.g:116:5: ( '\\\\' 'u' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT )
            // org/usergrid/persistence/query/tree/QueryFilter.g:116:9: '\\\\' 'u' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT
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
        	// do for sure before leaving
        }
    }
    // $ANTLR end "UNICODE_ESC"

    // $ANTLR start "WS"
    public final void mWS() throws RecognitionException {
        try {
            int _type = WS;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/usergrid/persistence/query/tree/QueryFilter.g:118:4: ( ( ' ' | '\\t' | '\\n' | '\\r' | '\\f' )+ )
            // org/usergrid/persistence/query/tree/QueryFilter.g:118:6: ( ' ' | '\\t' | '\\n' | '\\r' | '\\f' )+
            {
            // org/usergrid/persistence/query/tree/QueryFilter.g:118:6: ( ' ' | '\\t' | '\\n' | '\\r' | '\\f' )+
            int cnt23=0;
            loop23:
            do {
                int alt23=2;
                switch ( input.LA(1) ) {
                case '\t':
                case '\n':
                case '\f':
                case '\r':
                case ' ':
                    {
                    alt23=1;
                    }
                    break;

                }

                switch (alt23) {
            	case 1 :
            	    // org/usergrid/persistence/query/tree/QueryFilter.g:
            	    {
            	    if ( (input.LA(1) >= '\t' && input.LA(1) <= '\n')||(input.LA(1) >= '\f' && input.LA(1) <= '\r')||input.LA(1)==' ' ) {
            	        input.consume();
            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;
            	    }


            	    }
            	    break;

            	default :
            	    if ( cnt23 >= 1 ) break loop23;
                        EarlyExitException eee =
                            new EarlyExitException(23, input);
                        throw eee;
                }
                cnt23++;
            } while (true);


            _channel=HIDDEN;

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "WS"

    public void mTokens() throws RecognitionException {
        // org/usergrid/persistence/query/tree/QueryFilter.g:1:8: ( T__21 | T__22 | T__23 | T__24 | T__25 | T__26 | T__27 | T__28 | T__29 | T__30 | T__31 | T__32 | T__33 | T__34 | T__35 | T__36 | T__37 | T__38 | LT | LTE | EQ | GT | GTE | ID | INT | FLOAT | STRING | UUID | WS )
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
                // org/usergrid/persistence/query/tree/QueryFilter.g:1:118: LT
                {
                mLT(); 


                }
                break;
            case 20 :
                // org/usergrid/persistence/query/tree/QueryFilter.g:1:121: LTE
                {
                mLTE(); 


                }
                break;
            case 21 :
                // org/usergrid/persistence/query/tree/QueryFilter.g:1:125: EQ
                {
                mEQ(); 


                }
                break;
            case 22 :
                // org/usergrid/persistence/query/tree/QueryFilter.g:1:128: GT
                {
                mGT(); 


                }
                break;
            case 23 :
                // org/usergrid/persistence/query/tree/QueryFilter.g:1:131: GTE
                {
                mGTE(); 


                }
                break;
            case 24 :
                // org/usergrid/persistence/query/tree/QueryFilter.g:1:135: ID
                {
                mID(); 


                }
                break;
            case 25 :
                // org/usergrid/persistence/query/tree/QueryFilter.g:1:138: INT
                {
                mINT(); 


                }
                break;
            case 26 :
                // org/usergrid/persistence/query/tree/QueryFilter.g:1:142: FLOAT
                {
                mFLOAT(); 


                }
                break;
            case 27 :
                // org/usergrid/persistence/query/tree/QueryFilter.g:1:148: STRING
                {
                mSTRING(); 


                }
                break;
            case 28 :
                // org/usergrid/persistence/query/tree/QueryFilter.g:1:155: UUID
                {
                mUUID(); 


                }
                break;
            case 29 :
                // org/usergrid/persistence/query/tree/QueryFilter.g:1:160: WS
                {
                mWS(); 


                }
                break;

        }

    }


    protected DFA16 dfa16 = new DFA16(this);
    protected DFA24 dfa24 = new DFA24(this);
    static final String DFA16_eotS =
        "\5\uffff";
    static final String DFA16_eofS =
        "\5\uffff";
    static final String DFA16_minS =
        "\2\56\3\uffff";
    static final String DFA16_maxS =
        "\1\71\1\145\3\uffff";
    static final String DFA16_acceptS =
        "\2\uffff\1\2\1\1\1\3";
    static final String DFA16_specialS =
        "\5\uffff}>";
    static final String[] DFA16_transitionS = {
            "\1\2\1\uffff\12\1",
            "\1\3\1\uffff\12\1\13\uffff\1\4\37\uffff\1\4",
            "",
            "",
            ""
    };

    static final short[] DFA16_eot = DFA.unpackEncodedString(DFA16_eotS);
    static final short[] DFA16_eof = DFA.unpackEncodedString(DFA16_eofS);
    static final char[] DFA16_min = DFA.unpackEncodedStringToUnsignedChars(DFA16_minS);
    static final char[] DFA16_max = DFA.unpackEncodedStringToUnsignedChars(DFA16_maxS);
    static final short[] DFA16_accept = DFA.unpackEncodedString(DFA16_acceptS);
    static final short[] DFA16_special = DFA.unpackEncodedString(DFA16_specialS);
    static final short[][] DFA16_transition;

    static {
        int numStates = DFA16_transitionS.length;
        DFA16_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA16_transition[i] = DFA.unpackEncodedString(DFA16_transitionS[i]);
        }
    }

    class DFA16 extends DFA {

        public DFA16(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 16;
            this.eot = DFA16_eot;
            this.eof = DFA16_eof;
            this.min = DFA16_min;
            this.max = DFA16_max;
            this.accept = DFA16_accept;
            this.special = DFA16_special;
            this.transition = DFA16_transition;
        }
        public String getDescription() {
            return "71:15: ( ( '0' .. '9' )+ '.' ( '0' .. '9' )* ( EXPONENT )? | '.' ( '0' .. '9' )+ ( EXPONENT )? | ( '0' .. '9' )+ EXPONENT )";
        }
    }
    static final String DFA24_eotS =
        "\6\uffff\7\32\2\uffff\1\50\1\32\1\uffff\1\32\1\54\2\32\1\uffff\1"+
        "\57\4\uffff\6\32\1\71\1\73\3\32\2\uffff\1\50\1\21\2\uffff\1\54\1"+
        "\57\1\uffff\1\57\2\uffff\1\104\1\105\3\32\1\111\1\uffff\1\32\1\uffff"+
        "\3\32\1\47\1\53\1\57\1\uffff\1\30\2\uffff\2\32\1\123\1\uffff\4\32"+
        "\1\57\1\uffff\1\30\2\32\1\uffff\2\32\1\137\1\32\1\57\1\uffff\1\30"+
        "\2\32\1\uffff\1\146\1\uffff\1\147\1\57\1\uffff\1\30\2\32\2\uffff"+
        "\1\57\1\uffff\1\30\1\32\1\160\1\57\1\uffff\1\30\2\uffff\4\30";
    static final String DFA24_eofS =
        "\166\uffff";
    static final String DFA24_minS =
        "\1\11\5\uffff\3\60\1\157\1\146\1\145\1\150\2\uffff\1\75\1\164\1"+
        "\uffff\1\60\1\75\1\164\1\60\2\56\4\uffff\1\144\1\143\1\60\1\156"+
        "\1\60\1\164\2\56\1\154\1\145\1\164\2\uffff\2\56\2\uffff\2\56\1\uffff"+
        "\1\56\1\53\1\uffff\2\56\1\60\1\164\1\143\1\56\1\uffff\1\145\1\uffff"+
        "\1\145\1\162\1\150\3\56\1\53\1\60\2\uffff\1\60\1\141\1\56\1\uffff"+
        "\1\162\1\143\1\145\1\151\1\56\1\53\2\60\1\151\1\uffff\1\40\1\164"+
        "\1\56\1\156\1\56\1\53\2\60\1\156\1\uffff\1\56\1\uffff\2\56\1\53"+
        "\2\60\1\163\2\uffff\1\56\1\53\1\60\1\55\1\56\1\55\1\53\1\55\1\uffff"+
        "\4\60\1\55";
    static final String DFA24_maxS =
        "\1\175\5\uffff\1\163\1\157\1\146\1\157\1\162\1\145\1\151\2\uffff"+
        "\1\75\1\164\1\uffff\1\161\1\75\1\164\1\146\1\71\1\146\4\uffff\1"+
        "\144\1\143\1\146\1\156\1\163\1\164\2\172\1\154\1\145\1\164\2\uffff"+
        "\2\172\2\uffff\1\172\1\145\1\uffff\2\146\1\uffff\2\172\1\146\1\164"+
        "\1\143\1\172\1\uffff\1\145\1\uffff\1\145\1\162\1\150\2\172\3\146"+
        "\2\uffff\1\146\1\141\1\172\1\uffff\1\162\1\143\1\145\1\151\4\146"+
        "\1\151\1\uffff\1\40\1\164\1\172\1\156\4\146\1\156\1\uffff\1\172"+
        "\1\uffff\1\172\4\146\1\163\2\uffff\3\146\1\55\1\172\1\145\1\71\1"+
        "\55\1\uffff\4\146\1\55";
    static final String DFA24_acceptS =
        "\1\uffff\1\1\1\2\1\3\1\4\1\5\7\uffff\1\21\1\22\2\uffff\1\25\6\uffff"+
        "\1\32\1\33\1\30\1\35\13\uffff\1\24\1\23\2\uffff\1\27\1\26\2\uffff"+
        "\1\31\2\uffff\1\34\6\uffff\1\13\1\uffff\1\14\10\uffff\1\6\1\7\3"+
        "\uffff\1\12\11\uffff\1\11\11\uffff\1\15\1\uffff\1\17\6\uffff\1\16"+
        "\1\20\10\uffff\1\10\5\uffff";
    static final String DFA24_specialS =
        "\166\uffff}>";
    static final String[] DFA24_transitionS = {
            "\2\33\1\uffff\2\33\22\uffff\1\33\6\uffff\1\31\1\1\1\2\1\3\1"+
            "\uffff\1\4\1\26\1\30\1\uffff\12\27\1\5\1\uffff\1\17\1\21\1\23"+
            "\2\uffff\6\25\24\32\4\uffff\1\32\1\uffff\1\6\1\25\1\7\1\10\1"+
            "\22\1\25\1\24\4\32\1\20\1\32\1\11\1\12\3\32\1\13\3\32\1\14\3"+
            "\32\1\15\1\uffff\1\16",
            "",
            "",
            "",
            "",
            "",
            "\12\36\7\uffff\6\36\32\uffff\6\36\7\uffff\1\34\4\uffff\1\35",
            "\12\36\7\uffff\6\36\32\uffff\6\36\10\uffff\1\37",
            "\12\36\7\uffff\6\36\32\uffff\4\36\1\40\1\36",
            "\1\41",
            "\1\42\13\uffff\1\43",
            "\1\44",
            "\1\45\1\46",
            "",
            "",
            "\1\47",
            "\1\51",
            "",
            "\12\36\7\uffff\6\36\32\uffff\6\36\12\uffff\1\52",
            "\1\53",
            "\1\55",
            "\12\36\7\uffff\6\36\32\uffff\6\36",
            "\1\30\1\uffff\12\56",
            "\1\30\1\uffff\12\60\7\uffff\4\62\1\61\1\62\32\uffff\4\62\1"+
            "\61\1\62",
            "",
            "",
            "",
            "",
            "\1\63",
            "\1\64",
            "\12\65\7\uffff\6\65\32\uffff\6\65",
            "\1\66",
            "\12\65\7\uffff\6\65\32\uffff\6\65\14\uffff\1\67",
            "\1\70",
            "\1\32\1\uffff\12\32\7\uffff\32\32\4\uffff\1\32\1\uffff\32\32",
            "\1\32\1\uffff\12\32\7\uffff\32\32\4\uffff\1\32\1\uffff\3\32"+
            "\1\72\26\32",
            "\1\74",
            "\1\75",
            "\1\76",
            "",
            "",
            "\1\32\1\uffff\12\32\7\uffff\32\32\4\uffff\1\32\1\uffff\4\32"+
            "\1\77\25\32",
            "\1\32\1\uffff\12\32\7\uffff\32\32\4\uffff\1\32\1\uffff\32\32",
            "",
            "",
            "\1\32\1\uffff\12\32\7\uffff\32\32\4\uffff\1\32\1\uffff\4\32"+
            "\1\100\25\32",
            "\1\30\1\uffff\12\56\13\uffff\1\30\37\uffff\1\30",
            "",
            "\1\30\1\uffff\12\101\7\uffff\4\62\1\102\1\62\32\uffff\4\62"+
            "\1\102\1\62",
            "\1\30\1\uffff\1\30\2\uffff\12\103\7\uffff\6\62\32\uffff\6\62",
            "",
            "\1\32\1\uffff\12\32\7\uffff\32\32\4\uffff\1\32\1\uffff\32\32",
            "\1\32\1\uffff\12\32\7\uffff\32\32\4\uffff\1\32\1\uffff\32\32",
            "\12\106\7\uffff\6\106\32\uffff\6\106",
            "\1\107",
            "\1\110",
            "\1\32\1\uffff\12\32\7\uffff\32\32\4\uffff\1\32\1\uffff\32\32",
            "",
            "\1\112",
            "",
            "\1\113",
            "\1\114",
            "\1\115",
            "\1\32\1\uffff\12\32\7\uffff\32\32\4\uffff\1\32\1\uffff\32\32",
            "\1\32\1\uffff\12\32\7\uffff\32\32\4\uffff\1\32\1\uffff\32\32",
            "\1\30\1\uffff\12\116\7\uffff\4\62\1\117\1\62\32\uffff\4\62"+
            "\1\117\1\62",
            "\1\30\1\uffff\1\30\2\uffff\12\120\7\uffff\6\62\32\uffff\6\62",
            "\12\120\7\uffff\6\62\32\uffff\6\62",
            "",
            "",
            "\12\121\7\uffff\6\121\32\uffff\6\121",
            "\1\122",
            "\1\32\1\uffff\12\32\7\uffff\32\32\4\uffff\1\32\1\uffff\32\32",
            "",
            "\1\124",
            "\1\125",
            "\1\126",
            "\1\127",
            "\1\30\1\uffff\12\130\7\uffff\4\62\1\131\1\62\32\uffff\4\62"+
            "\1\131\1\62",
            "\1\30\1\uffff\1\30\2\uffff\12\132\7\uffff\6\62\32\uffff\6\62",
            "\12\132\7\uffff\6\62\32\uffff\6\62",
            "\12\133\7\uffff\6\133\32\uffff\6\133",
            "\1\134",
            "",
            "\1\135",
            "\1\136",
            "\1\32\1\uffff\12\32\7\uffff\32\32\4\uffff\1\32\1\uffff\32\32",
            "\1\140",
            "\1\30\1\uffff\12\141\7\uffff\4\62\1\142\1\62\32\uffff\4\62"+
            "\1\142\1\62",
            "\1\30\1\uffff\1\30\2\uffff\12\143\7\uffff\6\62\32\uffff\6\62",
            "\12\143\7\uffff\6\62\32\uffff\6\62",
            "\12\144\7\uffff\6\144\32\uffff\6\144",
            "\1\145",
            "",
            "\1\32\1\uffff\12\32\7\uffff\32\32\4\uffff\1\32\1\uffff\32\32",
            "",
            "\1\32\1\uffff\12\32\7\uffff\32\32\4\uffff\1\32\1\uffff\32\32",
            "\1\30\1\uffff\12\150\7\uffff\4\62\1\151\1\62\32\uffff\4\62"+
            "\1\151\1\62",
            "\1\30\1\uffff\1\30\2\uffff\12\152\7\uffff\6\62\32\uffff\6\62",
            "\12\152\7\uffff\6\62\32\uffff\6\62",
            "\12\153\7\uffff\6\153\32\uffff\6\153",
            "\1\154",
            "",
            "",
            "\1\30\1\uffff\12\155\7\uffff\4\62\1\156\1\62\32\uffff\4\62"+
            "\1\156\1\62",
            "\1\30\1\uffff\1\30\2\uffff\12\157\7\uffff\6\62\32\uffff\6\62",
            "\12\157\7\uffff\6\62\32\uffff\6\62",
            "\1\62",
            "\1\32\1\uffff\12\32\7\uffff\32\32\4\uffff\1\32\1\uffff\32\32",
            "\1\62\1\30\1\uffff\12\56\13\uffff\1\30\37\uffff\1\30",
            "\1\30\1\uffff\1\161\2\uffff\12\30",
            "\1\62",
            "",
            "\12\162\7\uffff\6\62\32\uffff\6\62",
            "\12\163\7\uffff\6\62\32\uffff\6\62",
            "\12\164\7\uffff\6\62\32\uffff\6\62",
            "\12\165\7\uffff\6\62\32\uffff\6\62",
            "\1\62"
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
            return "1:1: Tokens : ( T__21 | T__22 | T__23 | T__24 | T__25 | T__26 | T__27 | T__28 | T__29 | T__30 | T__31 | T__32 | T__33 | T__34 | T__35 | T__36 | T__37 | T__38 | LT | LTE | EQ | GT | GTE | ID | INT | FLOAT | STRING | UUID | WS );";
        }
    }
 

}