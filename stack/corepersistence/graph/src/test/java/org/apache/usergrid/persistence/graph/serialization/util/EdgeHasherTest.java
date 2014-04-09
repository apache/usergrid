/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.persistence.graph.serialization.util;


import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;

import static java.lang.Character.UnicodeBlock.AEGEAN_NUMBERS;
import static java.lang.Character.UnicodeBlock.ALCHEMICAL_SYMBOLS;
import static java.lang.Character.UnicodeBlock.ALPHABETIC_PRESENTATION_FORMS;
import static java.lang.Character.UnicodeBlock.ANCIENT_GREEK_MUSICAL_NOTATION;
import static java.lang.Character.UnicodeBlock.ANCIENT_GREEK_NUMBERS;
import static java.lang.Character.UnicodeBlock.ANCIENT_SYMBOLS;
import static java.lang.Character.UnicodeBlock.ARABIC;
import static java.lang.Character.UnicodeBlock.ARABIC_PRESENTATION_FORMS_A;
import static java.lang.Character.UnicodeBlock.ARABIC_PRESENTATION_FORMS_B;
import static java.lang.Character.UnicodeBlock.ARABIC_SUPPLEMENT;
import static java.lang.Character.UnicodeBlock.ARMENIAN;
import static java.lang.Character.UnicodeBlock.ARROWS;
import static java.lang.Character.UnicodeBlock.AVESTAN;
import static java.lang.Character.UnicodeBlock.BALINESE;
import static java.lang.Character.UnicodeBlock.BAMUM;
import static java.lang.Character.UnicodeBlock.BAMUM_SUPPLEMENT;
import static java.lang.Character.UnicodeBlock.BASIC_LATIN;
import static java.lang.Character.UnicodeBlock.BATAK;
import static java.lang.Character.UnicodeBlock.BENGALI;
import static java.lang.Character.UnicodeBlock.BLOCK_ELEMENTS;
import static java.lang.Character.UnicodeBlock.BOPOMOFO;
import static java.lang.Character.UnicodeBlock.BOPOMOFO_EXTENDED;
import static java.lang.Character.UnicodeBlock.BOX_DRAWING;
import static java.lang.Character.UnicodeBlock.BRAHMI;
import static java.lang.Character.UnicodeBlock.BRAILLE_PATTERNS;
import static java.lang.Character.UnicodeBlock.BUGINESE;
import static java.lang.Character.UnicodeBlock.BUHID;
import static java.lang.Character.UnicodeBlock.BYZANTINE_MUSICAL_SYMBOLS;
import static java.lang.Character.UnicodeBlock.CARIAN;
import static java.lang.Character.UnicodeBlock.CHAM;
import static java.lang.Character.UnicodeBlock.CHEROKEE;
import static java.lang.Character.UnicodeBlock.CJK_COMPATIBILITY;
import static java.lang.Character.UnicodeBlock.CJK_COMPATIBILITY_FORMS;
import static java.lang.Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS;
import static java.lang.Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT;
import static java.lang.Character.UnicodeBlock.CJK_RADICALS_SUPPLEMENT;
import static java.lang.Character.UnicodeBlock.CJK_STROKES;
import static java.lang.Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION;
import static java.lang.Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS;
import static java.lang.Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A;
import static java.lang.Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B;
import static java.lang.Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_C;
import static java.lang.Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_D;
import static java.lang.Character.UnicodeBlock.COMBINING_DIACRITICAL_MARKS;
import static java.lang.Character.UnicodeBlock.COMBINING_DIACRITICAL_MARKS_SUPPLEMENT;
import static java.lang.Character.UnicodeBlock.COMBINING_HALF_MARKS;
import static java.lang.Character.UnicodeBlock.COMBINING_MARKS_FOR_SYMBOLS;
import static java.lang.Character.UnicodeBlock.COMMON_INDIC_NUMBER_FORMS;
import static java.lang.Character.UnicodeBlock.CONTROL_PICTURES;
import static java.lang.Character.UnicodeBlock.COPTIC;
import static java.lang.Character.UnicodeBlock.COUNTING_ROD_NUMERALS;
import static java.lang.Character.UnicodeBlock.CUNEIFORM;
import static java.lang.Character.UnicodeBlock.CUNEIFORM_NUMBERS_AND_PUNCTUATION;
import static java.lang.Character.UnicodeBlock.CURRENCY_SYMBOLS;
import static java.lang.Character.UnicodeBlock.CYPRIOT_SYLLABARY;
import static java.lang.Character.UnicodeBlock.CYRILLIC;
import static java.lang.Character.UnicodeBlock.CYRILLIC_EXTENDED_A;
import static java.lang.Character.UnicodeBlock.CYRILLIC_EXTENDED_B;
import static java.lang.Character.UnicodeBlock.CYRILLIC_SUPPLEMENTARY;
import static java.lang.Character.UnicodeBlock.DESERET;
import static java.lang.Character.UnicodeBlock.DEVANAGARI;
import static java.lang.Character.UnicodeBlock.DEVANAGARI_EXTENDED;
import static java.lang.Character.UnicodeBlock.DINGBATS;
import static java.lang.Character.UnicodeBlock.DOMINO_TILES;
import static java.lang.Character.UnicodeBlock.EGYPTIAN_HIEROGLYPHS;
import static java.lang.Character.UnicodeBlock.EMOTICONS;
import static java.lang.Character.UnicodeBlock.ENCLOSED_ALPHANUMERICS;
import static java.lang.Character.UnicodeBlock.ENCLOSED_ALPHANUMERIC_SUPPLEMENT;
import static java.lang.Character.UnicodeBlock.ENCLOSED_CJK_LETTERS_AND_MONTHS;
import static java.lang.Character.UnicodeBlock.ENCLOSED_IDEOGRAPHIC_SUPPLEMENT;
import static java.lang.Character.UnicodeBlock.ETHIOPIC;
import static java.lang.Character.UnicodeBlock.ETHIOPIC_EXTENDED;
import static java.lang.Character.UnicodeBlock.ETHIOPIC_EXTENDED_A;
import static java.lang.Character.UnicodeBlock.ETHIOPIC_SUPPLEMENT;
import static java.lang.Character.UnicodeBlock.GENERAL_PUNCTUATION;
import static java.lang.Character.UnicodeBlock.GEOMETRIC_SHAPES;
import static java.lang.Character.UnicodeBlock.GEORGIAN;
import static java.lang.Character.UnicodeBlock.GEORGIAN_SUPPLEMENT;
import static java.lang.Character.UnicodeBlock.GLAGOLITIC;
import static java.lang.Character.UnicodeBlock.GOTHIC;
import static java.lang.Character.UnicodeBlock.GREEK;
import static java.lang.Character.UnicodeBlock.GREEK_EXTENDED;
import static java.lang.Character.UnicodeBlock.GUJARATI;
import static java.lang.Character.UnicodeBlock.GURMUKHI;
import static java.lang.Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS;
import static java.lang.Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO;
import static java.lang.Character.UnicodeBlock.HANGUL_JAMO;
import static java.lang.Character.UnicodeBlock.HANGUL_JAMO_EXTENDED_A;
import static java.lang.Character.UnicodeBlock.HANGUL_JAMO_EXTENDED_B;
import static java.lang.Character.UnicodeBlock.HANGUL_SYLLABLES;
import static java.lang.Character.UnicodeBlock.HANUNOO;
import static java.lang.Character.UnicodeBlock.HEBREW;
import static java.lang.Character.UnicodeBlock.HIRAGANA;
import static java.lang.Character.UnicodeBlock.IDEOGRAPHIC_DESCRIPTION_CHARACTERS;
import static java.lang.Character.UnicodeBlock.IMPERIAL_ARAMAIC;
import static java.lang.Character.UnicodeBlock.INSCRIPTIONAL_PAHLAVI;
import static java.lang.Character.UnicodeBlock.INSCRIPTIONAL_PARTHIAN;
import static java.lang.Character.UnicodeBlock.IPA_EXTENSIONS;
import static java.lang.Character.UnicodeBlock.JAVANESE;
import static java.lang.Character.UnicodeBlock.KAITHI;
import static java.lang.Character.UnicodeBlock.KANA_SUPPLEMENT;
import static java.lang.Character.UnicodeBlock.KANBUN;
import static java.lang.Character.UnicodeBlock.KANGXI_RADICALS;
import static java.lang.Character.UnicodeBlock.KANNADA;
import static java.lang.Character.UnicodeBlock.KATAKANA;
import static java.lang.Character.UnicodeBlock.KATAKANA_PHONETIC_EXTENSIONS;
import static java.lang.Character.UnicodeBlock.KAYAH_LI;
import static java.lang.Character.UnicodeBlock.KHAROSHTHI;
import static java.lang.Character.UnicodeBlock.KHMER;
import static java.lang.Character.UnicodeBlock.KHMER_SYMBOLS;
import static java.lang.Character.UnicodeBlock.LAO;
import static java.lang.Character.UnicodeBlock.LATIN_1_SUPPLEMENT;
import static java.lang.Character.UnicodeBlock.LATIN_EXTENDED_A;
import static java.lang.Character.UnicodeBlock.LATIN_EXTENDED_ADDITIONAL;
import static java.lang.Character.UnicodeBlock.LATIN_EXTENDED_B;
import static java.lang.Character.UnicodeBlock.LATIN_EXTENDED_C;
import static java.lang.Character.UnicodeBlock.LATIN_EXTENDED_D;
import static java.lang.Character.UnicodeBlock.LEPCHA;
import static java.lang.Character.UnicodeBlock.LETTERLIKE_SYMBOLS;
import static java.lang.Character.UnicodeBlock.LIMBU;
import static java.lang.Character.UnicodeBlock.LINEAR_B_IDEOGRAMS;
import static java.lang.Character.UnicodeBlock.LINEAR_B_SYLLABARY;
import static java.lang.Character.UnicodeBlock.LISU;
import static java.lang.Character.UnicodeBlock.LYCIAN;
import static java.lang.Character.UnicodeBlock.LYDIAN;
import static java.lang.Character.UnicodeBlock.MAHJONG_TILES;
import static java.lang.Character.UnicodeBlock.MALAYALAM;
import static java.lang.Character.UnicodeBlock.MANDAIC;
import static java.lang.Character.UnicodeBlock.MATHEMATICAL_ALPHANUMERIC_SYMBOLS;
import static java.lang.Character.UnicodeBlock.MATHEMATICAL_OPERATORS;
import static java.lang.Character.UnicodeBlock.MEETEI_MAYEK;
import static java.lang.Character.UnicodeBlock.MISCELLANEOUS_MATHEMATICAL_SYMBOLS_A;
import static java.lang.Character.UnicodeBlock.MISCELLANEOUS_MATHEMATICAL_SYMBOLS_B;
import static java.lang.Character.UnicodeBlock.MISCELLANEOUS_SYMBOLS;
import static java.lang.Character.UnicodeBlock.MISCELLANEOUS_SYMBOLS_AND_ARROWS;
import static java.lang.Character.UnicodeBlock.MISCELLANEOUS_SYMBOLS_AND_PICTOGRAPHS;
import static java.lang.Character.UnicodeBlock.MISCELLANEOUS_TECHNICAL;
import static java.lang.Character.UnicodeBlock.MODIFIER_TONE_LETTERS;
import static java.lang.Character.UnicodeBlock.MONGOLIAN;
import static java.lang.Character.UnicodeBlock.MUSICAL_SYMBOLS;
import static java.lang.Character.UnicodeBlock.MYANMAR;
import static java.lang.Character.UnicodeBlock.MYANMAR_EXTENDED_A;
import static java.lang.Character.UnicodeBlock.NEW_TAI_LUE;
import static java.lang.Character.UnicodeBlock.NKO;
import static java.lang.Character.UnicodeBlock.NUMBER_FORMS;
import static java.lang.Character.UnicodeBlock.OGHAM;
import static java.lang.Character.UnicodeBlock.OLD_ITALIC;
import static java.lang.Character.UnicodeBlock.OLD_PERSIAN;
import static java.lang.Character.UnicodeBlock.OLD_SOUTH_ARABIAN;
import static java.lang.Character.UnicodeBlock.OLD_TURKIC;
import static java.lang.Character.UnicodeBlock.OL_CHIKI;
import static java.lang.Character.UnicodeBlock.OPTICAL_CHARACTER_RECOGNITION;
import static java.lang.Character.UnicodeBlock.ORIYA;
import static java.lang.Character.UnicodeBlock.OSMANYA;
import static java.lang.Character.UnicodeBlock.PHAGS_PA;
import static java.lang.Character.UnicodeBlock.PHAISTOS_DISC;
import static java.lang.Character.UnicodeBlock.PHOENICIAN;
import static java.lang.Character.UnicodeBlock.PHONETIC_EXTENSIONS;
import static java.lang.Character.UnicodeBlock.PHONETIC_EXTENSIONS_SUPPLEMENT;
import static java.lang.Character.UnicodeBlock.PLAYING_CARDS;
import static java.lang.Character.UnicodeBlock.PRIVATE_USE_AREA;
import static java.lang.Character.UnicodeBlock.REJANG;
import static java.lang.Character.UnicodeBlock.RUMI_NUMERAL_SYMBOLS;
import static java.lang.Character.UnicodeBlock.RUNIC;
import static java.lang.Character.UnicodeBlock.SAMARITAN;
import static java.lang.Character.UnicodeBlock.SAURASHTRA;
import static java.lang.Character.UnicodeBlock.SHAVIAN;
import static java.lang.Character.UnicodeBlock.SINHALA;
import static java.lang.Character.UnicodeBlock.SMALL_FORM_VARIANTS;
import static java.lang.Character.UnicodeBlock.SPACING_MODIFIER_LETTERS;
import static java.lang.Character.UnicodeBlock.SPECIALS;
import static java.lang.Character.UnicodeBlock.SUNDANESE;
import static java.lang.Character.UnicodeBlock.SUPERSCRIPTS_AND_SUBSCRIPTS;
import static java.lang.Character.UnicodeBlock.SUPPLEMENTAL_ARROWS_A;
import static java.lang.Character.UnicodeBlock.SUPPLEMENTAL_ARROWS_B;
import static java.lang.Character.UnicodeBlock.SUPPLEMENTAL_MATHEMATICAL_OPERATORS;
import static java.lang.Character.UnicodeBlock.SUPPLEMENTAL_PUNCTUATION;
import static java.lang.Character.UnicodeBlock.SYLOTI_NAGRI;
import static java.lang.Character.UnicodeBlock.SYRIAC;
import static java.lang.Character.UnicodeBlock.TAGALOG;
import static java.lang.Character.UnicodeBlock.TAGBANWA;
import static java.lang.Character.UnicodeBlock.TAGS;
import static java.lang.Character.UnicodeBlock.TAI_LE;
import static java.lang.Character.UnicodeBlock.TAI_THAM;
import static java.lang.Character.UnicodeBlock.TAI_VIET;
import static java.lang.Character.UnicodeBlock.TAI_XUAN_JING_SYMBOLS;
import static java.lang.Character.UnicodeBlock.TAMIL;
import static java.lang.Character.UnicodeBlock.TELUGU;
import static java.lang.Character.UnicodeBlock.THAANA;
import static java.lang.Character.UnicodeBlock.THAI;
import static java.lang.Character.UnicodeBlock.TIBETAN;
import static java.lang.Character.UnicodeBlock.TIFINAGH;
import static java.lang.Character.UnicodeBlock.TRANSPORT_AND_MAP_SYMBOLS;
import static java.lang.Character.UnicodeBlock.UGARITIC;
import static java.lang.Character.UnicodeBlock.UNIFIED_CANADIAN_ABORIGINAL_SYLLABICS;
import static java.lang.Character.UnicodeBlock.UNIFIED_CANADIAN_ABORIGINAL_SYLLABICS_EXTENDED;
import static java.lang.Character.UnicodeBlock.VAI;
import static java.lang.Character.UnicodeBlock.VARIATION_SELECTORS;
import static java.lang.Character.UnicodeBlock.VARIATION_SELECTORS_SUPPLEMENT;
import static java.lang.Character.UnicodeBlock.VEDIC_EXTENSIONS;
import static java.lang.Character.UnicodeBlock.VERTICAL_FORMS;
import static java.lang.Character.UnicodeBlock.YIJING_HEXAGRAM_SYMBOLS;
import static java.lang.Character.UnicodeBlock.YI_RADICALS;
import static java.lang.Character.UnicodeBlock.YI_SYLLABLES;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


/**
 *
 *
 */
public class EdgeHasherTest {

    private static final Logger logger = LoggerFactory.getLogger( EdgeHasherTest.class );


    @Test
    public void consistentOutput() {

        final String simpleEdge = "simpleEdge";

        long[] hashed = EdgeHasher.createEdgeHash( simpleEdge );

        long[] otherHash = EdgeHasher.createEdgeHash( simpleEdge );

        assertArrayEquals( "hashMatches", hashed, otherHash );
    }


    @Test
    public void consistentOutputType() {

        final String simpleEdge = "simpleEdge";
        final Id simpleId = new SimpleId( "timpleType" );

        long[] hashed = EdgeHasher.createEdgeHash( simpleEdge, simpleId );

        long[] otherHash = EdgeHasher.createEdgeHash( simpleEdge, simpleId );

        assertArrayEquals( "hashMatches", hashed, otherHash );
    }


    @Test
    public void testLanguageCollisions() {

        // this is a very long running test and so it should be in a special "slowtest" group that
        // only gets run on a build server. As a stop gap, I've added this limit mechanism:
        int count = 0;
        int limit = 3; // Integer.MAX_VALUE;

        for ( Character.UnicodeBlock unicodeBlock : blocks ) {
            if ( count++ < limit ) {
                testCollisions( unicodeBlock );
            }
            else {
                break;
            }
        }
    }


    public void testCollisions( Character.UnicodeBlock unicodeBlock ) {

        Set<Character> block = findCharactersInUnicodeBlock( unicodeBlock );


        //total number of hashes to compare per unicode bock
        final int totalCount = 100000;
        final int delta = block.size();
        final int lengthToTest = totalCount / delta;

        final Set<HashEdge> hashed = new HashSet( totalCount );


        logger.info( "Testing hash collision on unicode block {}", unicodeBlock );


        final Character first = block.iterator().next();
        HashEdge edgeHash;

        int count = 0;

        StringBuilder builder = new StringBuilder();

        for ( int index = 0; index < lengthToTest; index++ ) {

            builder.append( first );

            for ( Character currentChar : block ) {

                builder.setCharAt( index, currentChar );

                final String sourceString = builder.toString();

                //now hash it
                edgeHash = new HashEdge( EdgeHasher.createEdgeHash( sourceString ) );


                count++;


                if ( hashed.contains( edgeHash ) ) {

                    fail( String.format(
                            "Expected hash of '%s' to be unique, but hash of '%s' already exists in unicode block "
                                    + "'%s'.", sourceString, edgeHash, unicodeBlock ) );
                }

                hashed.add( edgeHash );
            }
        }


        assertEquals( "Check the sizes are equal", count, hashed.size() );

        //force a mark
        hashed.clear();
    }


    private static Set<Character> findCharactersInUnicodeBlock( final Character.UnicodeBlock block ) {
        final Set<Character> chars = new HashSet<Character>();
        for ( int codePoint = Character.MIN_CODE_POINT; codePoint <= Character.MAX_CODE_POINT; codePoint++ ) {
            if ( block == Character.UnicodeBlock.of( codePoint ) ) {
                chars.add( ( char ) codePoint );
            }
        }
        return chars;
    }


    /**
     * Taken from the character source.  Note this purposefully eliminates Surrogate sets from the blocks since there
     * are invalid, and do cause hash conflicts due to multiple characters mapping to a single byte value by the JVM in
     * both UTF-8 and UTF-16
     */
    public static Character.UnicodeBlock[] blocks = {
            BASIC_LATIN, LATIN_1_SUPPLEMENT, LATIN_EXTENDED_A, LATIN_EXTENDED_B, IPA_EXTENSIONS,
            SPACING_MODIFIER_LETTERS, COMBINING_DIACRITICAL_MARKS, GREEK, CYRILLIC, CYRILLIC_SUPPLEMENTARY, ARMENIAN,
            HEBREW, ARABIC, SYRIAC, ARABIC_SUPPLEMENT, THAANA, NKO, SAMARITAN, MANDAIC,

            DEVANAGARI, BENGALI, GURMUKHI, GUJARATI, ORIYA, TAMIL, TELUGU, KANNADA, MALAYALAM, SINHALA, THAI, LAO,
            TIBETAN, MYANMAR, GEORGIAN, HANGUL_JAMO, ETHIOPIC, ETHIOPIC_SUPPLEMENT, CHEROKEE,
            UNIFIED_CANADIAN_ABORIGINAL_SYLLABICS, OGHAM, RUNIC, TAGALOG, HANUNOO, BUHID, TAGBANWA, KHMER, MONGOLIAN,
            UNIFIED_CANADIAN_ABORIGINAL_SYLLABICS_EXTENDED, LIMBU, TAI_LE, NEW_TAI_LUE, KHMER_SYMBOLS, BUGINESE,
            TAI_THAM,

            BALINESE, SUNDANESE, BATAK, LEPCHA, OL_CHIKI,

            VEDIC_EXTENSIONS, PHONETIC_EXTENSIONS, PHONETIC_EXTENSIONS_SUPPLEMENT,
            COMBINING_DIACRITICAL_MARKS_SUPPLEMENT, LATIN_EXTENDED_ADDITIONAL, GREEK_EXTENDED, GENERAL_PUNCTUATION,
            SUPERSCRIPTS_AND_SUBSCRIPTS, CURRENCY_SYMBOLS, COMBINING_MARKS_FOR_SYMBOLS, LETTERLIKE_SYMBOLS,
            NUMBER_FORMS, ARROWS, MATHEMATICAL_OPERATORS, MISCELLANEOUS_TECHNICAL, CONTROL_PICTURES,
            OPTICAL_CHARACTER_RECOGNITION, ENCLOSED_ALPHANUMERICS, BOX_DRAWING, BLOCK_ELEMENTS, GEOMETRIC_SHAPES,
            MISCELLANEOUS_SYMBOLS, DINGBATS, MISCELLANEOUS_MATHEMATICAL_SYMBOLS_A, SUPPLEMENTAL_ARROWS_A,
            BRAILLE_PATTERNS, SUPPLEMENTAL_ARROWS_B, MISCELLANEOUS_MATHEMATICAL_SYMBOLS_B,
            SUPPLEMENTAL_MATHEMATICAL_OPERATORS, MISCELLANEOUS_SYMBOLS_AND_ARROWS, GLAGOLITIC, LATIN_EXTENDED_C, COPTIC,
            GEORGIAN_SUPPLEMENT, TIFINAGH, ETHIOPIC_EXTENDED, CYRILLIC_EXTENDED_A, SUPPLEMENTAL_PUNCTUATION,
            CJK_RADICALS_SUPPLEMENT, KANGXI_RADICALS,

            IDEOGRAPHIC_DESCRIPTION_CHARACTERS, CJK_SYMBOLS_AND_PUNCTUATION, HIRAGANA, KATAKANA, BOPOMOFO,
            HANGUL_COMPATIBILITY_JAMO, KANBUN, BOPOMOFO_EXTENDED, CJK_STROKES, KATAKANA_PHONETIC_EXTENSIONS,
            ENCLOSED_CJK_LETTERS_AND_MONTHS, CJK_COMPATIBILITY, CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A,
            YIJING_HEXAGRAM_SYMBOLS, CJK_UNIFIED_IDEOGRAPHS, YI_SYLLABLES, YI_RADICALS, LISU, VAI, CYRILLIC_EXTENDED_B,
            BAMUM, MODIFIER_TONE_LETTERS, LATIN_EXTENDED_D, SYLOTI_NAGRI, COMMON_INDIC_NUMBER_FORMS, PHAGS_PA,
            SAURASHTRA, DEVANAGARI_EXTENDED, KAYAH_LI, REJANG, HANGUL_JAMO_EXTENDED_A, JAVANESE,

            CHAM, MYANMAR_EXTENDED_A, TAI_VIET,

            ETHIOPIC_EXTENDED_A,

            MEETEI_MAYEK, HANGUL_SYLLABLES, HANGUL_JAMO_EXTENDED_B, PRIVATE_USE_AREA, CJK_COMPATIBILITY_IDEOGRAPHS,
            ALPHABETIC_PRESENTATION_FORMS, ARABIC_PRESENTATION_FORMS_A, VARIATION_SELECTORS, VERTICAL_FORMS,
            COMBINING_HALF_MARKS, CJK_COMPATIBILITY_FORMS, SMALL_FORM_VARIANTS, ARABIC_PRESENTATION_FORMS_B,
            HALFWIDTH_AND_FULLWIDTH_FORMS, SPECIALS, LINEAR_B_SYLLABARY, LINEAR_B_IDEOGRAMS, AEGEAN_NUMBERS,
            ANCIENT_GREEK_NUMBERS, ANCIENT_SYMBOLS, PHAISTOS_DISC,

            LYCIAN, CARIAN,

            OLD_ITALIC, GOTHIC,

            UGARITIC, OLD_PERSIAN,

            DESERET, SHAVIAN, OSMANYA,

            CYPRIOT_SYLLABARY, IMPERIAL_ARAMAIC,

            PHOENICIAN, LYDIAN,

            KHAROSHTHI, OLD_SOUTH_ARABIAN,

            AVESTAN, INSCRIPTIONAL_PARTHIAN, INSCRIPTIONAL_PAHLAVI,

            OLD_TURKIC,

            RUMI_NUMERAL_SYMBOLS,

            BRAHMI, KAITHI,

            CUNEIFORM, CUNEIFORM_NUMBERS_AND_PUNCTUATION,

            EGYPTIAN_HIEROGLYPHS,

            BAMUM_SUPPLEMENT,

            KANA_SUPPLEMENT,

            BYZANTINE_MUSICAL_SYMBOLS, MUSICAL_SYMBOLS, ANCIENT_GREEK_MUSICAL_NOTATION,

            TAI_XUAN_JING_SYMBOLS, COUNTING_ROD_NUMERALS,

            MATHEMATICAL_ALPHANUMERIC_SYMBOLS,

            MAHJONG_TILES, DOMINO_TILES, PLAYING_CARDS, ENCLOSED_ALPHANUMERIC_SUPPLEMENT,
            ENCLOSED_IDEOGRAPHIC_SUPPLEMENT, MISCELLANEOUS_SYMBOLS_AND_PICTOGRAPHS, EMOTICONS,

            TRANSPORT_AND_MAP_SYMBOLS, ALCHEMICAL_SYMBOLS,

            CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B,

            CJK_UNIFIED_IDEOGRAPHS_EXTENSION_C, CJK_UNIFIED_IDEOGRAPHS_EXTENSION_D,

            CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT,

            TAGS,

            VARIATION_SELECTORS_SUPPLEMENT,
    };


    public static class HashEdge {
        private final long[] hashed;


        public HashEdge( final long[] hashed ) {
            this.hashed = hashed;
        }


        @Override
        public boolean equals( final Object o ) {
            if ( this == o ) {
                return true;
            }
            if ( !( o instanceof HashEdge ) ) {
                return false;
            }

            final HashEdge hashEdge = ( HashEdge ) o;

            if ( !Arrays.equals( hashed, hashEdge.hashed ) ) {
                return false;
            }

            return true;
        }


        @Override
        public int hashCode() {
            return Arrays.hashCode( hashed );
        }
    }
}
