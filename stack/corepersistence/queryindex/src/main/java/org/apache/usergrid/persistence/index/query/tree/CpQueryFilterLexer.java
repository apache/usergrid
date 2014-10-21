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
import org.apache.usergrid.persistence.index.exceptions.QueryTokenException;



import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked"})
public class CpQueryFilterLexer extends Lexer {
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




    // delegates
    // delegators
    public Lexer[] getDelegates() {
        return new Lexer[] {};
    }

    public CpQueryFilterLexer() {} 
    public CpQueryFilterLexer(CharStream input) {
        this(input, new RecognizerSharedState());
    }
    public CpQueryFilterLexer(CharStream input, RecognizerSharedState state) {
        super(input,state);
    }
    public String getGrammarFileName() { return "org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g"; }

    // $ANTLR start "T__31"
    public final void mT__31() throws RecognitionException {
        try {
            int _type = T__31;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:52:7: ( '(' )
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:52:9: '('
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
    // $ANTLR end "T__31"

    // $ANTLR start "T__32"
    public final void mT__32() throws RecognitionException {
        try {
            int _type = T__32;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:53:7: ( ')' )
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:53:9: ')'
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
    // $ANTLR end "T__32"

    // $ANTLR start "T__33"
    public final void mT__33() throws RecognitionException {
        try {
            int _type = T__33;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:54:7: ( '*' )
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:54:9: '*'
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
    // $ANTLR end "T__33"

    // $ANTLR start "T__34"
    public final void mT__34() throws RecognitionException {
        try {
            int _type = T__34;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:55:7: ( ',' )
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:55:9: ','
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
    // $ANTLR end "T__34"

    // $ANTLR start "T__35"
    public final void mT__35() throws RecognitionException {
        try {
            int _type = T__35;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:56:7: ( ':' )
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:56:9: ':'
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
    // $ANTLR end "T__35"

    // $ANTLR start "T__36"
    public final void mT__36() throws RecognitionException {
        try {
            int _type = T__36;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:57:7: ( 'order by' )
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:57:9: 'order by'
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
    // $ANTLR end "T__36"

    // $ANTLR start "T__37"
    public final void mT__37() throws RecognitionException {
        try {
            int _type = T__37;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:58:7: ( 'select' )
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:58:9: 'select'
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
    // $ANTLR end "T__37"

    // $ANTLR start "T__38"
    public final void mT__38() throws RecognitionException {
        try {
            int _type = T__38;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:59:7: ( 'where' )
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:59:9: 'where'
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
    // $ANTLR end "T__38"

    // $ANTLR start "T__39"
    public final void mT__39() throws RecognitionException {
        try {
            int _type = T__39;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:60:7: ( '{' )
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:60:9: '{'
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
    // $ANTLR end "T__39"

    // $ANTLR start "T__40"
    public final void mT__40() throws RecognitionException {
        try {
            int _type = T__40;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:61:7: ( '}' )
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:61:9: '}'
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
    // $ANTLR end "T__40"

    // $ANTLR start "LT"
    public final void mLT() throws RecognitionException {
        try {
            int _type = LT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:105:5: ( '<' | 'lt' )
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
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:105:7: '<'
                    {
                    match('<'); 

                    }
                    break;
                case 2 :
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:105:13: 'lt'
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
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:107:5: ( '<=' | 'lte' )
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
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:107:7: '<='
                    {
                    match("<="); 



                    }
                    break;
                case 2 :
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:107:15: 'lte'
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
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:109:5: ( '=' | 'eq' )
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
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:109:7: '='
                    {
                    match('='); 

                    }
                    break;
                case 2 :
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:109:13: 'eq'
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
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:111:5: ( '>' | 'gt' )
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
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:111:7: '>'
                    {
                    match('>'); 

                    }
                    break;
                case 2 :
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:111:13: 'gt'
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
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:113:5: ( '>=' | 'gte' )
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
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:113:7: '>='
                    {
                    match(">="); 



                    }
                    break;
                case 2 :
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:113:15: 'gte'
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

    // $ANTLR start "BOOLEAN"
    public final void mBOOLEAN() throws RecognitionException {
        try {
            int _type = BOOLEAN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:117:9: ( ( TRUE | FALSE ) )
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:117:11: ( TRUE | FALSE )
            {
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:117:11: ( TRUE | FALSE )
            int alt6=2;
            switch ( input.LA(1) ) {
            case 'T':
            case 't':
                {
                alt6=1;
                }
                break;
            case 'F':
            case 'f':
                {
                alt6=2;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 6, 0, input);

                throw nvae;

            }

            switch (alt6) {
                case 1 :
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:117:12: TRUE
                    {
                    mTRUE(); 


                    }
                    break;
                case 2 :
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:117:17: FALSE
                    {
                    mFALSE(); 


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
    // $ANTLR end "BOOLEAN"

    // $ANTLR start "AND"
    public final void mAND() throws RecognitionException {
        try {
            int _type = AND;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:119:5: ( ( 'A' | 'a' ) ( 'N' | 'n' ) ( 'D' | 'd' ) | '&&' )
            int alt7=2;
            switch ( input.LA(1) ) {
            case 'A':
            case 'a':
                {
                alt7=1;
                }
                break;
            case '&':
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
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:119:7: ( 'A' | 'a' ) ( 'N' | 'n' ) ( 'D' | 'd' )
                    {
                    if ( input.LA(1)=='A'||input.LA(1)=='a' ) {
                        input.consume();
                    }
                    else {
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;
                    }


                    if ( input.LA(1)=='N'||input.LA(1)=='n' ) {
                        input.consume();
                    }
                    else {
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;
                    }


                    if ( input.LA(1)=='D'||input.LA(1)=='d' ) {
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
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:119:37: '&&'
                    {
                    match("&&"); 



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
    // $ANTLR end "AND"

    // $ANTLR start "OR"
    public final void mOR() throws RecognitionException {
        try {
            int _type = OR;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:121:5: ( ( 'O' | 'o' ) ( 'R' | 'r' ) | '||' )
            int alt8=2;
            switch ( input.LA(1) ) {
            case 'O':
            case 'o':
                {
                alt8=1;
                }
                break;
            case '|':
                {
                alt8=2;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 8, 0, input);

                throw nvae;

            }

            switch (alt8) {
                case 1 :
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:121:7: ( 'O' | 'o' ) ( 'R' | 'r' )
                    {
                    if ( input.LA(1)=='O'||input.LA(1)=='o' ) {
                        input.consume();
                    }
                    else {
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;
                    }


                    if ( input.LA(1)=='R'||input.LA(1)=='r' ) {
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
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:121:28: '||'
                    {
                    match("||"); 



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
    // $ANTLR end "OR"

    // $ANTLR start "NOT"
    public final void mNOT() throws RecognitionException {
        try {
            int _type = NOT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:123:5: ( ( 'N' | 'n' ) ( 'O' | 'o' ) ( 'T' | 't' ) )
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:123:7: ( 'N' | 'n' ) ( 'O' | 'o' ) ( 'T' | 't' )
            {
            if ( input.LA(1)=='N'||input.LA(1)=='n' ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            if ( input.LA(1)=='O'||input.LA(1)=='o' ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            if ( input.LA(1)=='T'||input.LA(1)=='t' ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "NOT"

    // $ANTLR start "ASC"
    public final void mASC() throws RecognitionException {
        try {
            int _type = ASC;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:125:5: ( ( 'A' | 'a' ) ( 'S' | 's' ) ( 'C' | 'c' ) )
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:125:7: ( 'A' | 'a' ) ( 'S' | 's' ) ( 'C' | 'c' )
            {
            if ( input.LA(1)=='A'||input.LA(1)=='a' ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            if ( input.LA(1)=='S'||input.LA(1)=='s' ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            if ( input.LA(1)=='C'||input.LA(1)=='c' ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "ASC"

    // $ANTLR start "DESC"
    public final void mDESC() throws RecognitionException {
        try {
            int _type = DESC;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:127:6: ( ( 'D' | 'd' ) ( 'E' | 'e' ) ( 'S' | 's' ) ( 'C' | 'c' ) )
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:127:8: ( 'D' | 'd' ) ( 'E' | 'e' ) ( 'S' | 's' ) ( 'C' | 'c' )
            {
            if ( input.LA(1)=='D'||input.LA(1)=='d' ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            if ( input.LA(1)=='E'||input.LA(1)=='e' ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            if ( input.LA(1)=='S'||input.LA(1)=='s' ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            if ( input.LA(1)=='C'||input.LA(1)=='c' ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "DESC"

    // $ANTLR start "CONTAINS"
    public final void mCONTAINS() throws RecognitionException {
        try {
            int _type = CONTAINS;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:129:10: ( ( 'C' | 'c' ) ( 'O' | 'o' ) ( 'N' | 'n' ) ( 'T' | 't' ) ( 'A' | 'a' ) ( 'I' | 'i' ) ( 'N' | 'n' ) ( 'S' | 's' ) )
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:129:12: ( 'C' | 'c' ) ( 'O' | 'o' ) ( 'N' | 'n' ) ( 'T' | 't' ) ( 'A' | 'a' ) ( 'I' | 'i' ) ( 'N' | 'n' ) ( 'S' | 's' )
            {
            if ( input.LA(1)=='C'||input.LA(1)=='c' ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            if ( input.LA(1)=='O'||input.LA(1)=='o' ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            if ( input.LA(1)=='N'||input.LA(1)=='n' ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            if ( input.LA(1)=='T'||input.LA(1)=='t' ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            if ( input.LA(1)=='A'||input.LA(1)=='a' ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            if ( input.LA(1)=='I'||input.LA(1)=='i' ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            if ( input.LA(1)=='N'||input.LA(1)=='n' ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            if ( input.LA(1)=='S'||input.LA(1)=='s' ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "CONTAINS"

    // $ANTLR start "WITHIN"
    public final void mWITHIN() throws RecognitionException {
        try {
            int _type = WITHIN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:131:8: ( ( 'W' | 'w' ) ( 'I' | 'i' ) ( 'T' | 't' ) ( 'H' | 'h' ) ( 'I' | 'i' ) ( 'N' | 'n' ) )
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:131:10: ( 'W' | 'w' ) ( 'I' | 'i' ) ( 'T' | 't' ) ( 'H' | 'h' ) ( 'I' | 'i' ) ( 'N' | 'n' )
            {
            if ( input.LA(1)=='W'||input.LA(1)=='w' ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            if ( input.LA(1)=='I'||input.LA(1)=='i' ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            if ( input.LA(1)=='T'||input.LA(1)=='t' ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            if ( input.LA(1)=='H'||input.LA(1)=='h' ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            if ( input.LA(1)=='I'||input.LA(1)=='i' ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            if ( input.LA(1)=='N'||input.LA(1)=='n' ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "WITHIN"

    // $ANTLR start "OF"
    public final void mOF() throws RecognitionException {
        try {
            int _type = OF;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:133:4: ( ( 'O' | 'o' ) ( 'F' | 'f' ) )
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:133:6: ( 'O' | 'o' ) ( 'F' | 'f' )
            {
            if ( input.LA(1)=='O'||input.LA(1)=='o' ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            if ( input.LA(1)=='F'||input.LA(1)=='f' ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "OF"

    // $ANTLR start "UUID"
    public final void mUUID() throws RecognitionException {
        try {
            int _type = UUID;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:135:6: ( HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT '-' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT '-' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT '-' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT '-' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT )
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:135:9: HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT '-' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT '-' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT '-' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT '-' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT
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

    // $ANTLR start "ID"
    public final void mID() throws RecognitionException {
        try {
            int _type = ID;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:146:5: ( ( 'a' .. 'z' | 'A' .. 'Z' | '_' ) ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '_' | '.' | '-' )* )
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:146:7: ( 'a' .. 'z' | 'A' .. 'Z' | '_' ) ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '_' | '.' | '-' )*
            {
            if ( (input.LA(1) >= 'A' && input.LA(1) <= 'Z')||input.LA(1)=='_'||(input.LA(1) >= 'a' && input.LA(1) <= 'z') ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:146:31: ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '_' | '.' | '-' )*
            loop9:
            do {
                int alt9=2;
                switch ( input.LA(1) ) {
                case '-':
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
                    alt9=1;
                    }
                    break;

                }

                switch (alt9) {
            	case 1 :
            	    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:
            	    {
            	    if ( (input.LA(1) >= '-' && input.LA(1) <= '.')||(input.LA(1) >= '0' && input.LA(1) <= '9')||(input.LA(1) >= 'A' && input.LA(1) <= 'Z')||input.LA(1)=='_'||(input.LA(1) >= 'a' && input.LA(1) <= 'z') ) {
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
            	    break loop9;
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

    // $ANTLR start "LONG"
    public final void mLONG() throws RecognitionException {
        try {
            int _type = LONG;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:149:6: ( ( '-' )? ( '0' .. '9' )+ )
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:149:8: ( '-' )? ( '0' .. '9' )+
            {
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:149:8: ( '-' )?
            int alt10=2;
            switch ( input.LA(1) ) {
                case '-':
                    {
                    alt10=1;
                    }
                    break;
            }

            switch (alt10) {
                case 1 :
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:149:9: '-'
                    {
                    match('-'); 

                    }
                    break;

            }


            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:149:15: ( '0' .. '9' )+
            int cnt11=0;
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
            	    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:
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
            	    if ( cnt11 >= 1 ) break loop11;
                        EarlyExitException eee =
                            new EarlyExitException(11, input);
                        throw eee;
                }
                cnt11++;
            } while (true);


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "LONG"

    // $ANTLR start "FLOAT"
    public final void mFLOAT() throws RecognitionException {
        try {
            int _type = FLOAT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:153:5: ( ( '-' )? ( ( '0' .. '9' )+ '.' ( '0' .. '9' )* ( EXPONENT )? | '.' ( '0' .. '9' )+ ( EXPONENT )? | ( '0' .. '9' )+ EXPONENT ) )
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:153:8: ( '-' )? ( ( '0' .. '9' )+ '.' ( '0' .. '9' )* ( EXPONENT )? | '.' ( '0' .. '9' )+ ( EXPONENT )? | ( '0' .. '9' )+ EXPONENT )
            {
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:153:8: ( '-' )?
            int alt12=2;
            switch ( input.LA(1) ) {
                case '-':
                    {
                    alt12=1;
                    }
                    break;
            }

            switch (alt12) {
                case 1 :
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:153:9: '-'
                    {
                    match('-'); 

                    }
                    break;

            }


            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:153:15: ( ( '0' .. '9' )+ '.' ( '0' .. '9' )* ( EXPONENT )? | '.' ( '0' .. '9' )+ ( EXPONENT )? | ( '0' .. '9' )+ EXPONENT )
            int alt19=3;
            alt19 = dfa19.predict(input);
            switch (alt19) {
                case 1 :
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:153:17: ( '0' .. '9' )+ '.' ( '0' .. '9' )* ( EXPONENT )?
                    {
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:153:17: ( '0' .. '9' )+
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
                    	    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:
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


                    match('.'); 

                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:153:33: ( '0' .. '9' )*
                    loop14:
                    do {
                        int alt14=2;
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
                            alt14=1;
                            }
                            break;

                        }

                        switch (alt14) {
                    	case 1 :
                    	    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:
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
                    	    break loop14;
                        }
                    } while (true);


                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:153:45: ( EXPONENT )?
                    int alt15=2;
                    switch ( input.LA(1) ) {
                        case 'E':
                        case 'e':
                            {
                            alt15=1;
                            }
                            break;
                    }

                    switch (alt15) {
                        case 1 :
                            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:153:45: EXPONENT
                            {
                            mEXPONENT(); 


                            }
                            break;

                    }


                    }
                    break;
                case 2 :
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:154:9: '.' ( '0' .. '9' )+ ( EXPONENT )?
                    {
                    match('.'); 

                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:154:13: ( '0' .. '9' )+
                    int cnt16=0;
                    loop16:
                    do {
                        int alt16=2;
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
                            alt16=1;
                            }
                            break;

                        }

                        switch (alt16) {
                    	case 1 :
                    	    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:
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
                    	    if ( cnt16 >= 1 ) break loop16;
                                EarlyExitException eee =
                                    new EarlyExitException(16, input);
                                throw eee;
                        }
                        cnt16++;
                    } while (true);


                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:154:25: ( EXPONENT )?
                    int alt17=2;
                    switch ( input.LA(1) ) {
                        case 'E':
                        case 'e':
                            {
                            alt17=1;
                            }
                            break;
                    }

                    switch (alt17) {
                        case 1 :
                            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:154:25: EXPONENT
                            {
                            mEXPONENT(); 


                            }
                            break;

                    }


                    }
                    break;
                case 3 :
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:155:9: ( '0' .. '9' )+ EXPONENT
                    {
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:155:9: ( '0' .. '9' )+
                    int cnt18=0;
                    loop18:
                    do {
                        int alt18=2;
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
                            alt18=1;
                            }
                            break;

                        }

                        switch (alt18) {
                    	case 1 :
                    	    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:
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
                    	    if ( cnt18 >= 1 ) break loop18;
                                EarlyExitException eee =
                                    new EarlyExitException(18, input);
                                throw eee;
                        }
                        cnt18++;
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
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:159:5: ( '\\'' ( ESC_SEQ |~ ( '\\\\' | '\\'' ) )* '\\'' )
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:159:8: '\\'' ( ESC_SEQ |~ ( '\\\\' | '\\'' ) )* '\\''
            {
            match('\''); 

            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:159:13: ( ESC_SEQ |~ ( '\\\\' | '\\'' ) )*
            loop20:
            do {
                int alt20=3;
                int LA20_0 = input.LA(1);

                if ( (LA20_0=='\\') ) {
                    alt20=1;
                }
                else if ( ((LA20_0 >= '\u0000' && LA20_0 <= '&')||(LA20_0 >= '(' && LA20_0 <= '[')||(LA20_0 >= ']' && LA20_0 <= '\uFFFF')) ) {
                    alt20=2;
                }


                switch (alt20) {
            	case 1 :
            	    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:159:15: ESC_SEQ
            	    {
            	    mESC_SEQ(); 


            	    }
            	    break;
            	case 2 :
            	    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:159:25: ~ ( '\\\\' | '\\'' )
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
            	    break loop20;
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

    // $ANTLR start "WS"
    public final void mWS() throws RecognitionException {
        try {
            int _type = WS;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:164:4: ( ( ' ' | '\\t' | '\\n' | '\\r' | '\\f' )+ )
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:164:6: ( ' ' | '\\t' | '\\n' | '\\r' | '\\f' )+
            {
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:164:6: ( ' ' | '\\t' | '\\n' | '\\r' | '\\f' )+
            int cnt21=0;
            loop21:
            do {
                int alt21=2;
                switch ( input.LA(1) ) {
                case '\t':
                case '\n':
                case '\f':
                case '\r':
                case ' ':
                    {
                    alt21=1;
                    }
                    break;

                }

                switch (alt21) {
            	case 1 :
            	    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:
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
            	    if ( cnt21 >= 1 ) break loop21;
                        EarlyExitException eee =
                            new EarlyExitException(21, input);
                        throw eee;
                }
                cnt21++;
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

    // $ANTLR start "TRUE"
    public final void mTRUE() throws RecognitionException {
        try {
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:172:15: ( ( 'T' | 't' ) ( 'R' | 'r' ) ( 'U' | 'u' ) ( 'E' | 'e' ) )
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:172:17: ( 'T' | 't' ) ( 'R' | 'r' ) ( 'U' | 'u' ) ( 'E' | 'e' )
            {
            if ( input.LA(1)=='T'||input.LA(1)=='t' ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            if ( input.LA(1)=='R'||input.LA(1)=='r' ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            if ( input.LA(1)=='U'||input.LA(1)=='u' ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            if ( input.LA(1)=='E'||input.LA(1)=='e' ) {
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
    // $ANTLR end "TRUE"

    // $ANTLR start "FALSE"
    public final void mFALSE() throws RecognitionException {
        try {
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:174:16: ( ( 'F' | 'f' ) ( 'A' | 'a' ) ( 'L' | 'l' ) ( 'S' | 's' ) ( 'E' | 'e' ) )
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:174:18: ( 'F' | 'f' ) ( 'A' | 'a' ) ( 'L' | 'l' ) ( 'S' | 's' ) ( 'E' | 'e' )
            {
            if ( input.LA(1)=='F'||input.LA(1)=='f' ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            if ( input.LA(1)=='A'||input.LA(1)=='a' ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            if ( input.LA(1)=='L'||input.LA(1)=='l' ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            if ( input.LA(1)=='S'||input.LA(1)=='s' ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            if ( input.LA(1)=='E'||input.LA(1)=='e' ) {
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
    // $ANTLR end "FALSE"

    // $ANTLR start "EXPONENT"
    public final void mEXPONENT() throws RecognitionException {
        try {
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:179:10: ( ( 'e' | 'E' ) ( '+' | '-' )? ( '0' .. '9' )+ )
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:179:12: ( 'e' | 'E' ) ( '+' | '-' )? ( '0' .. '9' )+
            {
            if ( input.LA(1)=='E'||input.LA(1)=='e' ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:179:22: ( '+' | '-' )?
            int alt22=2;
            switch ( input.LA(1) ) {
                case '+':
                case '-':
                    {
                    alt22=1;
                    }
                    break;
            }

            switch (alt22) {
                case 1 :
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:
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


            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:179:33: ( '0' .. '9' )+
            int cnt23=0;
            loop23:
            do {
                int alt23=2;
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
                    alt23=1;
                    }
                    break;

                }

                switch (alt23) {
            	case 1 :
            	    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:
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
            	    if ( cnt23 >= 1 ) break loop23;
                        EarlyExitException eee =
                            new EarlyExitException(23, input);
                        throw eee;
                }
                cnt23++;
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
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:182:11: ( ( '0' .. '9' | 'a' .. 'f' | 'A' .. 'F' ) )
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:
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
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:186:5: ( '\\\\' ( 'b' | 't' | 'n' | 'f' | 'r' | '\\\"' | '\\'' | '\\\\' ) | UNICODE_ESC | OCTAL_ESC )
            int alt24=3;
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
                    alt24=1;
                    }
                    break;
                case 'u':
                    {
                    alt24=2;
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
                    alt24=3;
                    }
                    break;
                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 24, 1, input);

                    throw nvae;

                }

                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 24, 0, input);

                throw nvae;

            }

            switch (alt24) {
                case 1 :
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:186:9: '\\\\' ( 'b' | 't' | 'n' | 'f' | 'r' | '\\\"' | '\\'' | '\\\\' )
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
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:187:9: UNICODE_ESC
                    {
                    mUNICODE_ESC(); 


                    }
                    break;
                case 3 :
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:188:9: OCTAL_ESC
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
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:193:5: ( '\\\\' ( '0' .. '3' ) ( '0' .. '7' ) ( '0' .. '7' ) | '\\\\' ( '0' .. '7' ) ( '0' .. '7' ) | '\\\\' ( '0' .. '7' ) )
            int alt25=3;
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
                            alt25=1;
                            }
                            break;
                        default:
                            alt25=2;
                        }

                        }
                        break;
                    default:
                        alt25=3;
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
                        alt25=2;
                        }
                        break;
                    default:
                        alt25=3;
                    }

                    }
                    break;
                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 25, 1, input);

                    throw nvae;

                }

                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 25, 0, input);

                throw nvae;

            }

            switch (alt25) {
                case 1 :
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:193:9: '\\\\' ( '0' .. '3' ) ( '0' .. '7' ) ( '0' .. '7' )
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
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:194:9: '\\\\' ( '0' .. '7' ) ( '0' .. '7' )
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
                    // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:195:9: '\\\\' ( '0' .. '7' )
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
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:200:5: ( '\\\\' 'u' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT )
            // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:200:9: '\\\\' 'u' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT
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

    public void mTokens() throws RecognitionException {
        // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:1:8: ( T__31 | T__32 | T__33 | T__34 | T__35 | T__36 | T__37 | T__38 | T__39 | T__40 | LT | LTE | EQ | GT | GTE | BOOLEAN | AND | OR | NOT | ASC | DESC | CONTAINS | WITHIN | OF | UUID | ID | LONG | FLOAT | STRING | WS )
        int alt26=30;
        alt26 = dfa26.predict(input);
        switch (alt26) {
            case 1 :
                // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:1:10: T__31
                {
                mT__31(); 


                }
                break;
            case 2 :
                // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:1:16: T__32
                {
                mT__32(); 


                }
                break;
            case 3 :
                // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:1:22: T__33
                {
                mT__33(); 


                }
                break;
            case 4 :
                // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:1:28: T__34
                {
                mT__34(); 


                }
                break;
            case 5 :
                // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:1:34: T__35
                {
                mT__35(); 


                }
                break;
            case 6 :
                // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:1:40: T__36
                {
                mT__36(); 


                }
                break;
            case 7 :
                // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:1:46: T__37
                {
                mT__37(); 


                }
                break;
            case 8 :
                // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:1:52: T__38
                {
                mT__38(); 


                }
                break;
            case 9 :
                // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:1:58: T__39
                {
                mT__39(); 


                }
                break;
            case 10 :
                // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:1:64: T__40
                {
                mT__40(); 


                }
                break;
            case 11 :
                // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:1:70: LT
                {
                mLT(); 


                }
                break;
            case 12 :
                // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:1:73: LTE
                {
                mLTE(); 


                }
                break;
            case 13 :
                // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:1:77: EQ
                {
                mEQ(); 


                }
                break;
            case 14 :
                // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:1:80: GT
                {
                mGT(); 


                }
                break;
            case 15 :
                // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:1:83: GTE
                {
                mGTE(); 


                }
                break;
            case 16 :
                // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:1:87: BOOLEAN
                {
                mBOOLEAN(); 


                }
                break;
            case 17 :
                // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:1:95: AND
                {
                mAND(); 


                }
                break;
            case 18 :
                // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:1:99: OR
                {
                mOR(); 


                }
                break;
            case 19 :
                // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:1:102: NOT
                {
                mNOT(); 


                }
                break;
            case 20 :
                // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:1:106: ASC
                {
                mASC(); 


                }
                break;
            case 21 :
                // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:1:110: DESC
                {
                mDESC(); 


                }
                break;
            case 22 :
                // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:1:115: CONTAINS
                {
                mCONTAINS(); 


                }
                break;
            case 23 :
                // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:1:124: WITHIN
                {
                mWITHIN(); 


                }
                break;
            case 24 :
                // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:1:131: OF
                {
                mOF(); 


                }
                break;
            case 25 :
                // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:1:134: UUID
                {
                mUUID(); 


                }
                break;
            case 26 :
                // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:1:139: ID
                {
                mID(); 


                }
                break;
            case 27 :
                // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:1:142: LONG
                {
                mLONG(); 


                }
                break;
            case 28 :
                // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:1:147: FLOAT
                {
                mFLOAT(); 


                }
                break;
            case 29 :
                // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:1:153: STRING
                {
                mSTRING(); 


                }
                break;
            case 30 :
                // org/apache/usergrid/persistence/index/query/tree/CpQueryFilter.g:1:160: WS
                {
                mWS(); 


                }
                break;

        }

    }


    protected DFA19 dfa19 = new DFA19(this);
    protected DFA26 dfa26 = new DFA26(this);
    static final String DFA19_eotS =
        "\5\uffff";
    static final String DFA19_eofS =
        "\5\uffff";
    static final String DFA19_minS =
        "\2\56\3\uffff";
    static final String DFA19_maxS =
        "\1\71\1\145\3\uffff";
    static final String DFA19_acceptS =
        "\2\uffff\1\2\1\1\1\3";
    static final String DFA19_specialS =
        "\5\uffff}>";
    static final String[] DFA19_transitionS = {
            "\1\2\1\uffff\12\1",
            "\1\3\1\uffff\12\1\13\uffff\1\4\37\uffff\1\4",
            "",
            "",
            ""
    };

    static final short[] DFA19_eot = DFA.unpackEncodedString(DFA19_eotS);
    static final short[] DFA19_eof = DFA.unpackEncodedString(DFA19_eofS);
    static final char[] DFA19_min = DFA.unpackEncodedStringToUnsignedChars(DFA19_minS);
    static final char[] DFA19_max = DFA.unpackEncodedStringToUnsignedChars(DFA19_maxS);
    static final short[] DFA19_accept = DFA.unpackEncodedString(DFA19_acceptS);
    static final short[] DFA19_special = DFA.unpackEncodedString(DFA19_specialS);
    static final short[][] DFA19_transition;

    static {
        int numStates = DFA19_transitionS.length;
        DFA19_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA19_transition[i] = DFA.unpackEncodedString(DFA19_transitionS[i]);
        }
    }

    class DFA19 extends DFA {

        public DFA19(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 19;
            this.eot = DFA19_eot;
            this.eof = DFA19_eof;
            this.min = DFA19_min;
            this.max = DFA19_max;
            this.accept = DFA19_accept;
            this.special = DFA19_special;
            this.transition = DFA19_transition;
        }
        public String getDescription() {
            return "153:15: ( ( '0' .. '9' )+ '.' ( '0' .. '9' )* ( EXPONENT )? | '.' ( '0' .. '9' )+ ( EXPONENT )? | ( '0' .. '9' )+ EXPONENT )";
        }
    }
    static final String DFA26_eotS =
        "\6\uffff\3\35\2\uffff\1\51\1\35\1\uffff\1\35\1\56\4\35\1\uffff\1"+
        "\35\1\uffff\5\35\1\67\5\uffff\2\26\1\75\3\35\2\uffff\1\51\1\15\1"+
        "\35\2\uffff\1\56\7\35\1\uffff\1\67\2\uffff\1\67\1\35\1\uffff\3\35"+
        "\1\50\1\35\1\55\2\35\1\24\1\125\1\126\2\35\1\67\1\uffff\1\37\5\35"+
        "\1\141\1\35\2\uffff\1\143\1\35\1\67\1\uffff\1\37\2\35\1\152\2\35"+
        "\1\uffff\1\141\1\uffff\1\35\1\67\1\uffff\1\37\1\uffff\1\161\1\uffff"+
        "\1\162\2\35\1\67\1\uffff\1\37\2\uffff\2\35\1\67\1\uffff\1\37\1\35"+
        "\1\176\1\67\1\uffff\1\37\1\35\2\uffff\1\35\1\37\1\35\1\37\1\35\1"+
        "\37\1\35\1\37\26\35\1\72";
    static final String DFA26_eofS =
        "\u009f\uffff";
    static final String DFA26_minS =
        "\1\11\5\uffff\1\106\1\145\1\111\2\uffff\1\75\1\164\1\uffff\1\60"+
        "\1\75\1\164\1\122\2\60\1\uffff\1\106\1\uffff\1\117\2\60\1\111\1"+
        "\60\1\56\1\uffff\1\56\3\uffff\3\55\1\154\1\145\1\124\2\uffff\2\55"+
        "\1\60\2\uffff\1\55\1\125\1\60\1\104\1\103\1\124\1\60\1\116\1\uffff"+
        "\1\56\1\53\1\uffff\1\56\1\145\1\uffff\1\145\1\162\1\110\1\55\1\60"+
        "\1\55\1\105\1\123\3\55\1\103\1\124\1\56\1\53\1\60\1\162\1\143\1"+
        "\145\1\111\1\60\1\55\1\105\2\uffff\1\55\1\101\1\56\1\53\1\60\1\40"+
        "\1\164\1\55\1\116\1\60\1\uffff\1\55\1\uffff\1\111\1\56\1\53\1\60"+
        "\1\uffff\1\55\1\uffff\1\55\1\60\1\116\1\56\1\53\1\60\2\uffff\1\60"+
        "\1\123\1\56\1\53\1\60\3\55\1\53\1\55\1\60\1\uffff\7\60\2\55\4\60"+
        "\1\55\4\60\1\55\14\60\1\55";
    static final String DFA26_maxS =
        "\1\175\5\uffff\1\162\1\145\1\151\2\uffff\1\75\1\164\1\uffff\1\161"+
        "\1\75\1\164\1\162\1\146\1\163\1\uffff\1\162\1\uffff\1\157\1\146"+
        "\1\157\1\151\2\146\1\uffff\1\71\3\uffff\3\172\1\154\1\145\1\164"+
        "\2\uffff\2\172\1\146\2\uffff\1\172\1\165\1\154\1\144\1\143\1\164"+
        "\1\163\1\156\1\uffff\2\146\1\uffff\2\145\1\uffff\1\145\1\162\1\150"+
        "\1\172\1\146\1\172\1\145\1\163\3\172\1\143\1\164\3\146\1\162\1\143"+
        "\1\145\1\151\1\146\1\172\1\145\2\uffff\1\172\1\141\3\146\1\40\1"+
        "\164\1\172\1\156\1\146\1\uffff\1\172\1\uffff\1\151\3\146\1\uffff"+
        "\1\172\1\uffff\1\172\1\146\1\156\3\146\2\uffff\1\146\1\163\3\146"+
        "\1\55\1\172\1\145\1\71\1\55\1\146\1\uffff\7\146\2\55\4\146\1\55"+
        "\4\146\1\55\14\146\1\172";
    static final String DFA26_acceptS =
        "\1\uffff\1\1\1\2\1\3\1\4\1\5\3\uffff\1\11\1\12\2\uffff\1\15\6\uffff"+
        "\1\21\1\uffff\1\22\6\uffff\1\32\1\uffff\1\34\1\35\1\36\6\uffff\1"+
        "\14\1\13\3\uffff\1\17\1\16\10\uffff\1\33\2\uffff\1\31\2\uffff\1"+
        "\30\27\uffff\1\24\1\23\12\uffff\1\20\1\uffff\1\25\4\uffff\1\6\1"+
        "\uffff\1\10\6\uffff\1\7\1\27\13\uffff\1\26\40\uffff";
    static final String DFA26_specialS =
        "\u009f\uffff}>";
    static final String[] DFA26_transitionS = {
            "\2\41\1\uffff\2\41\22\uffff\1\41\5\uffff\1\24\1\40\1\1\1\2\1"+
            "\3\1\uffff\1\4\1\36\1\37\1\uffff\12\34\1\5\1\uffff\1\13\1\15"+
            "\1\17\2\uffff\1\23\1\33\1\31\1\30\1\33\1\22\7\35\1\27\1\25\4"+
            "\35\1\21\2\35\1\32\3\35\4\uffff\1\35\1\uffff\1\23\1\33\1\31"+
            "\1\30\1\16\1\22\1\20\4\35\1\14\1\35\1\27\1\6\3\35\1\7\1\21\2"+
            "\35\1\10\3\35\1\11\1\26\1\12",
            "",
            "",
            "",
            "",
            "",
            "\1\44\13\uffff\1\43\23\uffff\1\44\13\uffff\1\42",
            "\1\45",
            "\1\47\36\uffff\1\46\1\47",
            "",
            "",
            "\1\50",
            "\1\52",
            "",
            "\12\54\7\uffff\6\54\32\uffff\6\54\12\uffff\1\53",
            "\1\55",
            "\1\57",
            "\1\60\37\uffff\1\60",
            "\12\54\7\uffff\1\61\5\54\32\uffff\1\61\5\54",
            "\12\54\7\uffff\6\54\7\uffff\1\62\4\uffff\1\63\15\uffff\6\54"+
            "\7\uffff\1\62\4\uffff\1\63",
            "",
            "\1\44\13\uffff\1\43\23\uffff\1\44\13\uffff\1\43",
            "",
            "\1\64\37\uffff\1\64",
            "\12\54\7\uffff\4\54\1\65\1\54\32\uffff\4\54\1\65\1\54",
            "\12\54\7\uffff\6\54\10\uffff\1\66\21\uffff\6\54\10\uffff\1"+
            "\66",
            "\1\47\37\uffff\1\47",
            "\12\54\7\uffff\6\54\32\uffff\6\54",
            "\1\37\1\uffff\12\70\7\uffff\4\72\1\71\1\72\32\uffff\4\72\1"+
            "\71\1\72",
            "",
            "\1\37\1\uffff\12\73",
            "",
            "",
            "",
            "\2\35\1\uffff\12\35\7\uffff\32\35\4\uffff\1\35\1\uffff\3\35"+
            "\1\74\26\35",
            "\2\35\1\uffff\12\35\7\uffff\32\35\4\uffff\1\35\1\uffff\32\35",
            "\2\35\1\uffff\12\35\7\uffff\32\35\4\uffff\1\35\1\uffff\32\35",
            "\1\76",
            "\1\77",
            "\1\100\37\uffff\1\100",
            "",
            "",
            "\2\35\1\uffff\12\35\7\uffff\32\35\4\uffff\1\35\1\uffff\4\35"+
            "\1\101\25\35",
            "\2\35\1\uffff\12\35\7\uffff\32\35\4\uffff\1\35\1\uffff\32\35",
            "\12\102\7\uffff\6\102\32\uffff\6\102",
            "",
            "",
            "\2\35\1\uffff\12\35\7\uffff\32\35\4\uffff\1\35\1\uffff\4\35"+
            "\1\103\25\35",
            "\1\104\37\uffff\1\104",
            "\12\102\7\uffff\6\102\5\uffff\1\105\24\uffff\6\102\5\uffff"+
            "\1\105",
            "\1\106\37\uffff\1\106",
            "\1\107\37\uffff\1\107",
            "\1\110\37\uffff\1\110",
            "\12\102\7\uffff\6\102\14\uffff\1\111\15\uffff\6\102\14\uffff"+
            "\1\111",
            "\1\112\37\uffff\1\112",
            "",
            "\1\37\1\uffff\12\113\7\uffff\4\72\1\114\1\72\32\uffff\4\72"+
            "\1\114\1\72",
            "\1\37\1\uffff\1\37\2\uffff\12\115\7\uffff\6\72\32\uffff\6\72",
            "",
            "\1\37\1\uffff\12\73\13\uffff\1\37\37\uffff\1\37",
            "\1\116",
            "",
            "\1\117",
            "\1\120",
            "\1\121\37\uffff\1\121",
            "\2\35\1\uffff\12\35\7\uffff\32\35\4\uffff\1\35\1\uffff\32\35",
            "\12\122\7\uffff\6\122\32\uffff\6\122",
            "\2\35\1\uffff\12\35\7\uffff\32\35\4\uffff\1\35\1\uffff\32\35",
            "\1\123\37\uffff\1\123",
            "\1\124\37\uffff\1\124",
            "\2\35\1\uffff\12\35\7\uffff\32\35\4\uffff\1\35\1\uffff\32\35",
            "\2\35\1\uffff\12\35\7\uffff\32\35\4\uffff\1\35\1\uffff\32\35",
            "\2\35\1\uffff\12\35\7\uffff\32\35\4\uffff\1\35\1\uffff\32\35",
            "\1\127\37\uffff\1\127",
            "\1\130\37\uffff\1\130",
            "\1\37\1\uffff\12\131\7\uffff\4\72\1\132\1\72\32\uffff\4\72"+
            "\1\132\1\72",
            "\1\37\1\uffff\1\37\2\uffff\12\133\7\uffff\6\72\32\uffff\6\72",
            "\12\133\7\uffff\6\72\32\uffff\6\72",
            "\1\134",
            "\1\135",
            "\1\136",
            "\1\137\37\uffff\1\137",
            "\12\140\7\uffff\6\140\32\uffff\6\140",
            "\2\35\1\uffff\12\35\7\uffff\32\35\4\uffff\1\35\1\uffff\32\35",
            "\1\142\37\uffff\1\142",
            "",
            "",
            "\2\35\1\uffff\12\35\7\uffff\32\35\4\uffff\1\35\1\uffff\32\35",
            "\1\144\37\uffff\1\144",
            "\1\37\1\uffff\12\145\7\uffff\4\72\1\146\1\72\32\uffff\4\72"+
            "\1\146\1\72",
            "\1\37\1\uffff\1\37\2\uffff\12\147\7\uffff\6\72\32\uffff\6\72",
            "\12\147\7\uffff\6\72\32\uffff\6\72",
            "\1\150",
            "\1\151",
            "\2\35\1\uffff\12\35\7\uffff\32\35\4\uffff\1\35\1\uffff\32\35",
            "\1\153\37\uffff\1\153",
            "\12\154\7\uffff\6\154\32\uffff\6\154",
            "",
            "\2\35\1\uffff\12\35\7\uffff\32\35\4\uffff\1\35\1\uffff\32\35",
            "",
            "\1\155\37\uffff\1\155",
            "\1\37\1\uffff\12\156\7\uffff\4\72\1\157\1\72\32\uffff\4\72"+
            "\1\157\1\72",
            "\1\37\1\uffff\1\37\2\uffff\12\160\7\uffff\6\72\32\uffff\6\72",
            "\12\160\7\uffff\6\72\32\uffff\6\72",
            "",
            "\2\35\1\uffff\12\35\7\uffff\32\35\4\uffff\1\35\1\uffff\32\35",
            "",
            "\2\35\1\uffff\12\35\7\uffff\32\35\4\uffff\1\35\1\uffff\32\35",
            "\12\163\7\uffff\6\163\32\uffff\6\163",
            "\1\164\37\uffff\1\164",
            "\1\37\1\uffff\12\165\7\uffff\4\72\1\166\1\72\32\uffff\4\72"+
            "\1\166\1\72",
            "\1\37\1\uffff\1\37\2\uffff\12\167\7\uffff\6\72\32\uffff\6\72",
            "\12\167\7\uffff\6\72\32\uffff\6\72",
            "",
            "",
            "\12\170\7\uffff\6\170\32\uffff\6\170",
            "\1\171\37\uffff\1\171",
            "\1\37\1\uffff\12\172\7\uffff\4\72\1\173\1\72\32\uffff\4\72"+
            "\1\173\1\72",
            "\1\37\1\uffff\1\37\2\uffff\12\174\7\uffff\6\72\32\uffff\6\72",
            "\12\174\7\uffff\6\72\32\uffff\6\72",
            "\1\175",
            "\2\35\1\uffff\12\35\7\uffff\32\35\4\uffff\1\35\1\uffff\32\35",
            "\1\72\1\37\1\uffff\12\73\13\uffff\1\37\37\uffff\1\37",
            "\1\37\1\uffff\1\177\2\uffff\12\37",
            "\1\72",
            "\12\u0080\7\uffff\6\u0080\32\uffff\6\u0080",
            "",
            "\12\u0081\7\uffff\6\72\32\uffff\6\72",
            "\12\u0082\7\uffff\6\u0082\32\uffff\6\u0082",
            "\12\u0083\7\uffff\6\72\32\uffff\6\72",
            "\12\u0084\7\uffff\6\u0084\32\uffff\6\u0084",
            "\12\u0085\7\uffff\6\72\32\uffff\6\72",
            "\12\u0086\7\uffff\6\u0086\32\uffff\6\u0086",
            "\12\u0087\7\uffff\6\72\32\uffff\6\72",
            "\1\u0088",
            "\1\72",
            "\12\u0089\7\uffff\6\u0089\32\uffff\6\u0089",
            "\12\u008a\7\uffff\6\u008a\32\uffff\6\u008a",
            "\12\u008b\7\uffff\6\u008b\32\uffff\6\u008b",
            "\12\u008c\7\uffff\6\u008c\32\uffff\6\u008c",
            "\1\u008d",
            "\12\u008e\7\uffff\6\u008e\32\uffff\6\u008e",
            "\12\u008f\7\uffff\6\u008f\32\uffff\6\u008f",
            "\12\u0090\7\uffff\6\u0090\32\uffff\6\u0090",
            "\12\u0091\7\uffff\6\u0091\32\uffff\6\u0091",
            "\1\u0092",
            "\12\u0093\7\uffff\6\u0093\32\uffff\6\u0093",
            "\12\u0094\7\uffff\6\u0094\32\uffff\6\u0094",
            "\12\u0095\7\uffff\6\u0095\32\uffff\6\u0095",
            "\12\u0096\7\uffff\6\u0096\32\uffff\6\u0096",
            "\12\u0097\7\uffff\6\u0097\32\uffff\6\u0097",
            "\12\u0098\7\uffff\6\u0098\32\uffff\6\u0098",
            "\12\u0099\7\uffff\6\u0099\32\uffff\6\u0099",
            "\12\u009a\7\uffff\6\u009a\32\uffff\6\u009a",
            "\12\u009b\7\uffff\6\u009b\32\uffff\6\u009b",
            "\12\u009c\7\uffff\6\u009c\32\uffff\6\u009c",
            "\12\u009d\7\uffff\6\u009d\32\uffff\6\u009d",
            "\12\u009e\7\uffff\6\u009e\32\uffff\6\u009e",
            "\2\35\1\uffff\12\35\7\uffff\32\35\4\uffff\1\35\1\uffff\32\35"
    };

    static final short[] DFA26_eot = DFA.unpackEncodedString(DFA26_eotS);
    static final short[] DFA26_eof = DFA.unpackEncodedString(DFA26_eofS);
    static final char[] DFA26_min = DFA.unpackEncodedStringToUnsignedChars(DFA26_minS);
    static final char[] DFA26_max = DFA.unpackEncodedStringToUnsignedChars(DFA26_maxS);
    static final short[] DFA26_accept = DFA.unpackEncodedString(DFA26_acceptS);
    static final short[] DFA26_special = DFA.unpackEncodedString(DFA26_specialS);
    static final short[][] DFA26_transition;

    static {
        int numStates = DFA26_transitionS.length;
        DFA26_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA26_transition[i] = DFA.unpackEncodedString(DFA26_transitionS[i]);
        }
    }

    class DFA26 extends DFA {

        public DFA26(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 26;
            this.eot = DFA26_eot;
            this.eof = DFA26_eof;
            this.min = DFA26_min;
            this.max = DFA26_max;
            this.accept = DFA26_accept;
            this.special = DFA26_special;
            this.transition = DFA26_transition;
        }
        public String getDescription() {
            return "1:1: Tokens : ( T__31 | T__32 | T__33 | T__34 | T__35 | T__36 | T__37 | T__38 | T__39 | T__40 | LT | LTE | EQ | GT | GTE | BOOLEAN | AND | OR | NOT | ASC | DESC | CONTAINS | WITHIN | OF | UUID | ID | LONG | FLOAT | STRING | WS );";
        }
    }
 

}