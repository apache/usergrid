/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.persistence.collection.serialization.impl.util;


import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Transforms words to singular, plural, humanized (human readable), underscore, camel case, or ordinal form. This is
 * inspired by the <a href="http://api.rubyonrails.org/classes/Inflector.html">Inflector</a> class in <a
 * href="http://www.rubyonrails.org">Ruby on Rails</a>, which is distributed under the <a
 * href="http://wiki.rubyonrails.org/rails/pages/License">Rails license</a>.
 */
public class Inflector {

    protected static final Inflector INSTANCE = new Inflector();


    public static Inflector getInstance() {
        return INSTANCE;
    }


    protected class Rule {

        protected final String expression;
        protected final Pattern expressionPattern;
        protected final String replacement;


        protected Rule( String expression, String replacement ) {
            this.expression = expression;
            this.replacement = replacement != null ? replacement : "";
            expressionPattern = Pattern.compile( this.expression, Pattern.CASE_INSENSITIVE );
        }


        /**
         * Apply the rule against the input string, returning the modified string or null if the rule didn't apply (and
         * no modifications were made)
         *
         * @param input the input string
         *
         * @return the modified string if this rule applied, or null if the input was not modified by this rule
         */
        protected String apply( String input ) {
            Matcher matcher = expressionPattern.matcher( input );
            if ( !matcher.find() ) {
                return null;
            }
            return matcher.replaceAll( replacement );
        }


        @Override
        public int hashCode() {
            return expression.hashCode();
        }


        @Override
        public boolean equals( Object obj ) {
            if ( obj == this ) {
                return true;
            }
            if ( ( obj != null ) && ( obj.getClass() == this.getClass() ) ) {
                final Rule that = ( Rule ) obj;
                if ( expression.equalsIgnoreCase( that.expression ) ) {
                    return true;
                }
            }
            return false;
        }


        @Override
        public String toString() {
            return expression + ", " + replacement;
        }
    }


    private final LinkedList<Rule> plurals = new LinkedList<Rule>();
    private final LinkedList<Rule> singulars = new LinkedList<Rule>();
    /**
     * The lowercase words that are to be excluded and not processed. This map can be modified by the users via {@link
     * #getUncountables()}.
     */
    private final Set<String> uncountables = new HashSet<String>();


    public Inflector() {
        initialize();
    }


    protected Inflector( Inflector original ) {
        plurals.addAll( original.plurals );
        singulars.addAll( original.singulars );
        uncountables.addAll( original.uncountables );
    }


    @Override
    @SuppressWarnings("all")
    public Inflector clone() {
        return new Inflector( this );
    }

    // ------------------------------------------------------------------------------------------------
    // Usage functions
    // ------------------------------------------------------------------------------------------------


    /**
     * Returns the plural form of the word in the string. <p> Examples: <p/>
     * <pre>
     *   inflector.pluralize(&quot;post&quot;)               #=&gt; &quot;posts&quot;
     *   inflector.pluralize(&quot;octopus&quot;)            #=&gt; &quot;octopi&quot;
     *   inflector.pluralize(&quot;sheep&quot;)              #=&gt; &quot;sheep&quot;
     *   inflector.pluralize(&quot;words&quot;)              #=&gt; &quot;words&quot;
     *   inflector.pluralize(&quot;the blue mailman&quot;)   #=&gt; &quot;the blue mailmen&quot;
     *   inflector.pluralize(&quot;CamelOctopus&quot;)       #=&gt; &quot;CamelOctopi&quot;
     * </pre>
     * <p/> </p> <p> Note that if the {@link Object#toString()} is called on the supplied object, so this method works
     * for non-strings, too. </p>
     *
     * @param word the word that is to be pluralized.
     *
     * @return the pluralized form of the word, or the word itself if it could not be pluralized
     *
     * @see #singularize(Object)
     */
    public String pluralize( Object word ) {
        if ( word == null ) {
            return null;
        }
        String wordStr = word.toString().trim();
        if ( wordStr.length() == 0 ) {
            return wordStr;
        }
        if ( isUncountable( wordStr ) ) {
            return wordStr;
        }
        for ( Rule rule : plurals ) {
            String result = rule.apply( wordStr );
            if ( result != null ) {
                return result;
            }
        }
        return wordStr;
    }


    public String pluralize( Object word, int count ) {
        if ( word == null ) {
            return null;
        }
        if ( ( count == 1 ) || ( count == -1 ) ) {
            return word.toString();
        }
        return pluralize( word );
    }



    /**
     * Returns the singular form of the word in the string. <p> Examples: <p/>
     * <pre>
     *   inflector.singularize(&quot;posts&quot;)             #=&gt; &quot;post&quot;
     *   inflector.singularize(&quot;octopi&quot;)            #=&gt; &quot;octopus&quot;
     *   inflector.singularize(&quot;sheep&quot;)             #=&gt; &quot;sheep&quot;
     *   inflector.singularize(&quot;words&quot;)             #=&gt; &quot;word&quot;
     *   inflector.singularize(&quot;the blue mailmen&quot;)  #=&gt; &quot;the blue mailman&quot;
     *   inflector.singularize(&quot;CamelOctopi&quot;)       #=&gt; &quot;CamelOctopus&quot;
     * </pre>
     * <p/> </p> <p> Note that if the {@link Object#toString()} is called on the supplied object, so this method works
     * for non-strings, too. </p>
     *
     * @param word the word that is to be pluralized.
     *
     * @return the pluralized form of the word, or the word itself if it could not be pluralized
     *
     * @see #pluralize(Object)
     */
    public String singularize( Object word ) {
        if ( word == null ) {
            return null;
        }
        String wordStr = word.toString().trim();
        if ( wordStr.length() == 0 ) {
            return wordStr;
        }
        if ( isUncountable( wordStr ) ) {
            return wordStr;
        }
        for ( Rule rule : singulars ) {
            String result = rule.apply( wordStr );
            if ( result != null ) {
                return result;
            }
        }
        return wordStr;
    }



    // ------------------------------------------------------------------------------------------------
    // Management methods
    // ------------------------------------------------------------------------------------------------


    /**
     * Determine whether the supplied word is considered uncountable by the {@link #pluralize(Object) pluralize} and
     * {@link #singularize(Object) singularize} methods.
     *
     * @param word the word
     *
     * @return true if the plural and singular forms of the word are the same
     */
    public boolean isUncountable( String word ) {
        if ( word == null ) {
            return false;
        }
        String trimmedLower = word.trim().toLowerCase();
        return uncountables.contains( trimmedLower );
    }


    public void addPluralize( String rule, String replacement ) {
        final Rule pluralizeRule = new Rule( rule, replacement );
        plurals.addFirst( pluralizeRule );
    }


    public void addSingularize( String rule, String replacement ) {
        final Rule singularizeRule = new Rule( rule, replacement );
        singulars.addFirst( singularizeRule );
    }


    public void addIrregular( String singular, String plural ) {
        if ( org.apache.commons.lang.StringUtils.isEmpty( singular ) ) {
            throw new IllegalArgumentException( "singular rule may not be empty" );
        }
        if ( org.apache.commons.lang.StringUtils.isEmpty( plural ) ) {
            throw new IllegalArgumentException( "plural rule may not be empty" );
        }
        String singularRemainder = singular.length() > 1 ? singular.substring( 1 ) : "";
        String pluralRemainder = plural.length() > 1 ? plural.substring( 1 ) : "";
        addPluralize( "(" + singular.charAt( 0 ) + ")" + singularRemainder + "$", "$1" + pluralRemainder );
        addSingularize( "(" + plural.charAt( 0 ) + ")" + pluralRemainder + "$", "$1" + singularRemainder );
    }


    public void addUncountable( String... words ) {
        if ( ( words == null ) || ( words.length == 0 ) ) {
            return;
        }
        for ( String word : words ) {
            if ( word != null ) {
                uncountables.add( word.trim().toLowerCase() );
            }
        }
    }


    /** Completely remove all rules within this inflector. */
    public void clear() {
        uncountables.clear();
        plurals.clear();
        singulars.clear();
    }


    protected void initialize() {
        Inflector inflect = this;
        inflect.addPluralize( "$", "s" );
        inflect.addPluralize( "s$", "s" );
        inflect.addPluralize( "(ax|test)is$", "$1es" );
        inflect.addPluralize( "(octop|vir)us$", "$1i" );
        inflect.addPluralize( "(octop|vir)i$", "$1i" ); // already plural
        inflect.addPluralize( "(alias|status)$", "$1es" );
        inflect.addPluralize( "(bu)s$", "$1ses" );
        inflect.addPluralize( "(buffal|tomat)o$", "$1oes" );
        inflect.addPluralize( "([ti])um$", "$1a" );
        inflect.addPluralize( "([ti])a$", "$1a" ); // already plural
        inflect.addPluralize( "sis$", "ses" );
        inflect.addPluralize( "(?:([^f])fe|([lr])f)$", "$1$2ves" );
        inflect.addPluralize( "(hive)$", "$1s" );
        inflect.addPluralize( "([^aeiouy]|qu)y$", "$1ies" );
        inflect.addPluralize( "(x|ch|ss|sh)$", "$1es" );
        inflect.addPluralize( "(matr|vert|ind)ix|ex$", "$1ices" );
        inflect.addPluralize( "([m|l])ouse$", "$1ice" );
        inflect.addPluralize( "([m|l])ice$", "$1ice" );
        inflect.addPluralize( "^(ox)$", "$1en" );
        inflect.addPluralize( "(quiz)$", "$1zes" );
        // Need to check for the following words that are already pluralized:
        inflect.addPluralize( "(people|men|children|sexes|moves|stadiums)$", "$1" ); // irregulars
        inflect.addPluralize( "(oxen|octopi|viri|aliases|quizzes)$", "$1" ); // special
        // rules

        inflect.addSingularize( "s$", "" );
        inflect.addSingularize( "(s|si|u)s$", "$1s" ); // '-us' and '-ss' are
        // already singular
        inflect.addSingularize( "(n)ews$", "$1ews" );
        inflect.addSingularize( "([ti])a$", "$1um" );
        inflect.addSingularize( "((a)naly|(b)a|(d)iagno|(p)arenthe|(p)rogno|(s)ynop|(t)he)ses$", "$1$2sis" );
        inflect.addSingularize( "(^analy)ses$", "$1sis" );
        inflect.addSingularize( "(^analy)sis$", "$1sis" ); // already singular,
        // but ends in 's'
        inflect.addSingularize( "([^f])ves$", "$1fe" );
        inflect.addSingularize( "(hive)s$", "$1" );
        inflect.addSingularize( "(tive)s$", "$1" );
        inflect.addSingularize( "([lr])ves$", "$1f" );
        inflect.addSingularize( "([^aeiouy]|qu)ies$", "$1y" );
        inflect.addSingularize( "(s)eries$", "$1eries" );
        inflect.addSingularize( "(m)ovies$", "$1ovie" );
        inflect.addSingularize( "(x|ch|ss|sh)es$", "$1" );
        inflect.addSingularize( "([m|l])ice$", "$1ouse" );
        inflect.addSingularize( "(bus)es$", "$1" );
        inflect.addSingularize( "(o)es$", "$1" );
        inflect.addSingularize( "(shoe)s$", "$1" );
        inflect.addSingularize( "(cris|ax|test)is$", "$1is" ); // already
        // singular, but
        // ends in 's'
        inflect.addSingularize( "(cris|ax|test)es$", "$1is" );
        inflect.addSingularize( "(octop|vir)i$", "$1us" );
        inflect.addSingularize( "(octop|vir)us$", "$1us" ); // already singular,
        // but ends in 's'
        inflect.addSingularize( "(alias|status)es$", "$1" );
        inflect.addSingularize( "(alias|status)$", "$1" ); // already singular,
        // but ends in 's'
        inflect.addSingularize( "^(ox)en", "$1" );
        inflect.addSingularize( "(vert|ind)ices$", "$1ex" );
        inflect.addSingularize( "(matr)ices$", "$1ix" );
        inflect.addSingularize( "(quiz)zes$", "$1" );

        inflect.addIrregular( "person", "people" );
        inflect.addIrregular( "man", "men" );
        inflect.addIrregular( "child", "children" );
        inflect.addIrregular( "sex", "sexes" );
        inflect.addIrregular( "move", "moves" );
        inflect.addIrregular( "stadium", "stadiums" );

        inflect.addUncountable( "equipment", "information", "rice", "money", "species", "series", "fish", "sheep",
                "data", "analytics" );
    }
}
