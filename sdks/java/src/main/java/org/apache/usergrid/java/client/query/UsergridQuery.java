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
package org.apache.usergrid.java.client.query;

import org.apache.usergrid.java.client.UsergridEnums.UsergridQueryOperator;
import org.apache.usergrid.java.client.UsergridEnums.UsergridQuerySortOrder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URLEncoder;
import java.util.*;

@SuppressWarnings("unused")
public final class UsergridQuery {

    @NotNull private final ArrayList<String> requirementStrings = new ArrayList<>();
    @NotNull private final ArrayList<String> urlTerms = new ArrayList<>();
    @NotNull private final HashMap<String, UsergridQuerySortOrder> orderClauses = new HashMap<>();
    @NotNull private Integer limit = UsergridQuery.LIMIT_DEFAULT;
    @Nullable private String cursor = null;
    @Nullable private String fromStringValue = null;
    @Nullable private String collectionName = null;

    public UsergridQuery() {
        this(null);
    }

    public UsergridQuery(@Nullable final String collectionName) {
        this.collectionName = collectionName;
        this.requirementStrings.add(UsergridQuery.EMPTY_STRING);
    }

    private static boolean isUUID(@NotNull final String string) {
        try {
            UUID uuid = UUID.fromString(string);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    @NotNull
    private static String encode(@NotNull final String stringValue) {
        String escapedString;
        try {
            escapedString = URLEncoder.encode(stringValue, UTF8);
        } catch (Exception e) {
            escapedString = stringValue;
        }
        return escapedString;
    }

    @NotNull
    public static String strJoin(@NotNull final List<String> array, @NotNull final String separator) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0, il = array.size(); i < il; i++) {
            if (i > 0) {
                stringBuilder.append(separator);
            }
            stringBuilder.append(array.get(i));
        }
        return stringBuilder.toString();
    }

    @NotNull
    public UsergridQuery fromString(@NotNull final String stringValue) {
        this.fromStringValue = stringValue;
        return this;
    }

    @Nullable
    public String getType() {
        return this.collectionName;
    }

    @Nullable
    public String getCollectionName() {
        return this.collectionName;
    }

    @Nullable
    public String getCollection() {
        return this.collectionName;
    }

    @NotNull
    public UsergridQuery type(@Nullable final String type) {
        this.collectionName = type;
        return this;
    }

    @NotNull
    public UsergridQuery collection(@Nullable final String collectionName) {
        return this.type(collectionName);
    }

    @NotNull
    public UsergridQuery collectionName(@Nullable final String collectionName) {
        return this.type(collectionName);
    }

    @NotNull
    public UsergridQuery cursor(@Nullable final String value) {
        this.cursor = value;
        return this;
    }

    @NotNull
    public UsergridQuery limit(@NotNull final Integer limit) {
        this.limit = limit;
        return this;
    }

    @NotNull
    private UsergridQuery addConditionalSeparator(@NotNull final String separator) {
        if (!this.requirementStrings.get(0).isEmpty()) {
            this.requirementStrings.add(0, separator);
            this.requirementStrings.add(0, UsergridQuery.EMPTY_STRING);
        }
        return this;
    }

    @NotNull
    public UsergridQuery and() {
        return this.addConditionalSeparator(UsergridQuery.AND);
    }

    @NotNull
    public UsergridQuery or() {
        return this.addConditionalSeparator(UsergridQuery.OR);
    }

    @NotNull
    public UsergridQuery not() {
        return this.addConditionalSeparator(UsergridQuery.NOT);
    }

    @NotNull
    public UsergridQuery sort(@NotNull final String term, @NotNull final UsergridQuerySortOrder sortOrder) {
        this.orderClauses.put(term, sortOrder);
        return this;
    }

    @NotNull
    public UsergridQuery ascending(@NotNull final String term) {
        return this.asc(term);
    }

    @NotNull
    public UsergridQuery asc(@NotNull final String term) {
        return this.sort(term, UsergridQuerySortOrder.ASC);
    }

    @NotNull
    public UsergridQuery descending(@NotNull final String term) {
        return this.desc(term);
    }

    @NotNull
    public UsergridQuery desc(@NotNull final String term) {
        return this.sort(term, UsergridQuerySortOrder.DESC);
    }

    @NotNull
    public UsergridQuery contains(@NotNull final String term, @NotNull final String value) {
        return this.containsWord(term, value);
    }

    @NotNull
    public UsergridQuery containsString(@NotNull final String term, @NotNull final String value) {
        return this.containsWord(term, value);
    }

    @NotNull
    public UsergridQuery containsWord(@NotNull final String term, @NotNull final String value) {
        return this.addRequirement(term + SPACE + CONTAINS + SPACE + ((UsergridQuery.isUUID(value)) ? EMPTY_STRING : APOSTROPHE) + value + ((UsergridQuery.isUUID(value)) ? EMPTY_STRING : APOSTROPHE));
    }

    @NotNull
    public UsergridQuery filter(@NotNull final String term, @NotNull final Object value) {
        return this.eq(term, value);
    }

    @NotNull
    public UsergridQuery equals(@NotNull final String term, @NotNull final Object value) {
        return this.eq(term, value);
    }

    @NotNull
    public UsergridQuery eq(@NotNull final String term, @NotNull final Object value) {
        return this.addOperationRequirement(term, UsergridQueryOperator.EQUAL, value);
    }

    @NotNull
    public UsergridQuery greaterThan(@NotNull final String term, @NotNull final Object value) {
        return this.gt(term, value);
    }

    @NotNull
    public UsergridQuery gt(@NotNull final String term, @NotNull final Object value) {
        return this.addOperationRequirement(term, UsergridQueryOperator.GREATER_THAN, value);
    }

    @NotNull
    public UsergridQuery greaterThanOrEqual(@NotNull final String term, @NotNull final Object value) {
        return this.gte(term, value);
    }

    @NotNull
    public UsergridQuery gte(@NotNull final String term, @NotNull final Object value) {
        return this.addOperationRequirement(term, UsergridQueryOperator.GREATER_THAN_EQUAL_TO, value);
    }

    @NotNull
    public UsergridQuery lessThan(@NotNull final String term, @NotNull final Object value) {
        return this.lt(term, value);
    }

    @NotNull
    public UsergridQuery lt(@NotNull final String term, @NotNull final Object value) {
        return this.addOperationRequirement(term, UsergridQueryOperator.LESS_THAN, value);
    }

    @NotNull
    public UsergridQuery lessThanOrEqual(@NotNull final String term, @NotNull final Object value) {
        return this.lte(term, value);
    }

    @NotNull
    public UsergridQuery lte(@NotNull final String term, @NotNull final Object value) {
        return this.addOperationRequirement(term, UsergridQueryOperator.LESS_THAN_EQUAL_TO, value);
    }

    @NotNull
    public UsergridQuery locationWithin(final double distance, final double latitude, final double longitude) {
        return this.addRequirement(LOCATION + SPACE + WITHIN + SPACE + distance + SPACE + OF + SPACE + latitude + SPACE + COMMA + longitude);
    }

    @NotNull
    public UsergridQuery urlTerm(@NotNull final String term, @NotNull final String equalsValue) {
        if (term.equalsIgnoreCase(QL)) {
            this.ql(equalsValue);
        } else {
            this.urlTerms.add(UsergridQuery.encode(term) + EQUALS + UsergridQuery.encode(equalsValue));
        }
        return this;
    }

    @NotNull
    public UsergridQuery ql(final String value) {

        return value != null && !value.isEmpty() ? this.addRequirement(value) : this;

    }

    @NotNull
    public UsergridQuery addRequirement(@NotNull final String requirement) {
        String requirementString = this.requirementStrings.remove(0);
        if (!requirement.isEmpty() && !requirementString.isEmpty()) {
            requirementString += SPACE + AND + SPACE;
        }
        requirementString += requirement;
        this.requirementStrings.add(0, requirementString);
        return this;
    }

    @NotNull
    public UsergridQuery addOperationRequirement(@NotNull final String term, @NotNull final UsergridQueryOperator operation, final int intValue) {
        return this.addOperationRequirement(term, operation, Integer.valueOf(intValue));
    }

    @NotNull
    public UsergridQuery addOperationRequirement(@NotNull final String term, @NotNull final UsergridQueryOperator operation, @NotNull final Object value) {
        if (value instanceof String) {
            String valueString = value.toString();
            if (!UsergridQuery.isUUID(valueString)) {
                valueString = APOSTROPHE + value + APOSTROPHE;
            }
            return addRequirement(term + SPACE + operation.operatorValue() + SPACE + valueString);
        } else {
            return addRequirement(term + SPACE + operation.operatorValue() + SPACE + value.toString());
        }
    }

    @NotNull
    private String constructOrderByString() {
        String orderByString = EMPTY_STRING;
        if (!this.orderClauses.isEmpty()) {
            for (Map.Entry<String, UsergridQuerySortOrder> orderClause : this.orderClauses.entrySet()) {
                if (!orderByString.isEmpty()) {
                    orderByString += COMMA;
                }
                orderByString += orderClause.getKey() + SPACE + orderClause.getValue().toString();
            }
            if (!orderByString.isEmpty()) {
                orderByString = SPACE + ORDER_BY + SPACE + orderByString;
            }
        }
        return orderByString;
    }

    @NotNull
    private String constructURLTermsString() {
        String urlTermsString = EMPTY_STRING;
        if (!this.urlTerms.isEmpty()) {
            urlTermsString = UsergridQuery.strJoin(this.urlTerms, AMPERSAND);
        }
        return urlTermsString;
    }

    @NotNull
    private String constructRequirementString() {
        ArrayList<String> requirementStrings = new ArrayList<>(this.requirementStrings);
        String firstString = requirementStrings.get(0);
        if (firstString.isEmpty()) {
            requirementStrings.remove(0);
        }
        String requirementsString = EMPTY_STRING;
        if (!requirementStrings.isEmpty()) {
            firstString = requirementStrings.get(0);
            if (firstString.equalsIgnoreCase(OR) || firstString.equalsIgnoreCase(AND) || firstString.equalsIgnoreCase(NOT)) {
                requirementStrings.remove(0);
            }
            if (!requirementStrings.isEmpty()) {
                Collections.reverse(requirementStrings);
                requirementsString = UsergridQuery.strJoin(requirementStrings, SPACE);
            }
        }
        return requirementsString;
    }

    @NotNull
    private String constructURLAppend() {
        return this.constructURLAppend(true);
    }

    @NotNull
    private String constructURLAppend(final boolean autoURLEncode) {
        if (this.fromStringValue != null) {
            String requirementsString = this.fromStringValue;
            if (autoURLEncode) {
                requirementsString = UsergridQuery.encode(requirementsString);
            }
            return QUESTION_MARK + QL + EQUALS + requirementsString;
        }
        String urlAppend = EMPTY_STRING;
        if (this.limit != LIMIT_DEFAULT) {
            urlAppend += LIMIT + EQUALS + this.limit.toString();
        }
        String urlTermsString = this.constructURLTermsString();
        if (!urlTermsString.isEmpty()) {
            if (!urlAppend.isEmpty()) {
                urlAppend += AMPERSAND;
            }
            urlAppend += urlTermsString;
        }
        if (this.cursor != null && !this.cursor.isEmpty()) {
            if (!urlAppend.isEmpty()) {
                urlAppend += AMPERSAND;
            }
            urlAppend += CURSOR + EQUALS + this.cursor;
        }

        String requirementsString = this.constructRequirementString();

        String orderByString = this.constructOrderByString();
        if (!orderByString.isEmpty()) {
            requirementsString += orderByString;
        }
        if (!requirementsString.isEmpty()) {
            if (autoURLEncode) {
                requirementsString = UsergridQuery.encode(requirementsString);
            }
            if (!urlAppend.isEmpty()) {
                urlAppend += AMPERSAND;
            }
            urlAppend += QL + EQUALS + requirementsString;
        }
        if (!urlAppend.isEmpty()) {
            urlAppend = QUESTION_MARK + urlAppend;
        }
        return urlAppend;
    }

    @NotNull
    public String build() {
        return this.build(true);
    }

    @NotNull
    public String build(final boolean autoURLEncode) {
        return this.constructURLAppend(autoURLEncode);
    }

    private static final int LIMIT_DEFAULT = 10;
    @NotNull private static final String AMPERSAND = "&";
    @NotNull private static final String AND = "and";
    @NotNull private static final String APOSTROPHE = "'";
    @NotNull private static final String COMMA = ",";
    @NotNull private static final String CONTAINS = "contains";
    @NotNull private static final String CURSOR = "cursor";
    @NotNull private static final String EMPTY_STRING = "";
    @NotNull private static final String EQUALS = "=";
    @NotNull private static final String LIMIT = "limit";
    @NotNull private static final String LOCATION = "location";
    @NotNull private static final String NOT = "not";
    @NotNull private static final String OF = "of";
    @NotNull private static final String OR = "or";
    @NotNull private static final String ORDER_BY = "order by";
    @NotNull private static final String QL = "ql";
    @NotNull private static final String QUESTION_MARK = "?";
    @NotNull private static final String SELECT_ALL = "select *";
    @NotNull private static final String SPACE = " ";
    @NotNull private static final String UTF8 = "UTF-8";
    @NotNull private static final String WHERE = "where";
    @NotNull private static final String WITHIN = "within";
}
