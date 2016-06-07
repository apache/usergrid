//
//  UsergridQuery.swift
//  UsergridSDK
//
//  Created by Robert Walsh on 7/22/15.
//
/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 *
 */

import Foundation

/**
 `UsergridQuery` is builder class used to construct filtered requests to Usergrid.
 
 `UsergridQuery` objects are then passed to `UsergridClient` or `Usergrid` methods which support `UsergridQuery` as a parameter are .GET(), .PUT(), and .DELETE().
 */
public class UsergridQuery : NSObject,NSCopying {
    
    // MARK: - Initialization -
    
    /**
    Desingated initializer for `UsergridQuery` objects.
    
    - parameter collectionName: The collection name or `type` of entities you want to query.
    
    - returns: A new instance of `UsergridQuery`.
    */
    public init(_ collectionName: String? = nil) {
        self.collectionName = collectionName
    }
    
    // MARK: - NSCopying -
    
    /**
    See the NSCopying protocol.
    
    - parameter zone: Ignored
    
    - returns: Returns a new instance that’s a copy of the receiver.
    */
    public func copyWithZone(zone: NSZone) -> AnyObject {
        let queryCopy = UsergridQuery(self.collectionName)
        queryCopy.requirementStrings = NSArray(array:self.requirementStrings, copyItems: true) as! [String]
        queryCopy.urlTerms = NSArray(array:self.urlTerms, copyItems: true) as! [String]
        for (key,value) in self.orderClauses {
            queryCopy.orderClauses[key] = value
        }
        queryCopy.limit = self.limit
        queryCopy.cursor = self.cursor
        return queryCopy
    }
    
    // MARK: - Building -
    
    /**
    Constructs the string that should be appeneded to the end of the URL as a query.
    
    - parameter autoURLEncode: Automatically encode the constructed string.
    
    - returns: The constructed URL query sting.
    */
    public func build(autoURLEncode: Bool = true) -> String {
        return self.constructURLAppend(autoURLEncode)
    }
    
    // MARK: - Builder Methods -

    /**
    Contains. Query: where term contains 'val%'.

    - parameter term:  The term.
    - parameter value: The value.

    - returns: `Self`
    */
    public func contains(term: String, value: String) -> Self { return self.containsWord(term, value: value) }


    /**
    Contains. Query: where term contains 'val%'.
    
    - parameter term:  The term.
    - parameter value: The value.
    
    - returns: `Self`
    */
    public func containsString(term: String, value: String) -> Self { return self.containsWord(term, value: value) }
    
    /**
     Contains. Query: where term contains 'val%'.
     
     - parameter term:  The term.
     - parameter value: The value.
     
     - returns: `Self`
     */
    public func containsWord(term: String, value: String) -> Self { return self.addRequirement(term + UsergridQuery.SPACE + UsergridQuery.CONTAINS + UsergridQuery.SPACE + ((value.isUuid()) ? UsergridQuery.EMPTY_STRING : UsergridQuery.APOSTROPHE) + value + ((value.isUuid()) ? UsergridQuery.EMPTY_STRING : UsergridQuery.APOSTROPHE)) }
    
    /**
     Sort ascending. Query:. order by term asc.
     
     - parameter term: The term.
     
     - returns: `Self`
     */
    public func ascending(term: String) -> Self { return self.asc(term) }
    
    /**
     Sort ascending. Query:. order by term asc.
     
     - parameter term: The term.
     
     - returns: `Self`
     */
    public func asc(term: String) -> Self { return self.sort(term, sortOrder: .Asc) }
    
    /**
     Sort descending. Query: order by term desc
     
     - parameter term: The term.
     
     - returns: `Self`
     */
    public func descending(term: String) -> Self { return self.desc(term) }
    
    /**
     Sort descending. Query: order by term desc
     
     - parameter term: The term.
     
     - returns: `Self`
     */
    public func desc(term: String) -> Self { return self.sort(term, sortOrder: .Desc) }
    
    /**
     Filter (or Equal-to). Query: where term = 'value'.
     
     - parameter term:  The term.
     - parameter value: The value.
     
     - returns: `Self`
     */
    public func filter(term: String, value: AnyObject) -> Self { return self.eq(term, value: value) }
    
    /**
     Equal-to. Query: where term = 'value'.
     
     - parameter term:  The term.
     - parameter value: The value.
     
     - returns: `Self`
     */
    public func equals(term: String, value: AnyObject) -> Self { return self.eq(term, value: value) }
    
    /**
     Equal-to. Query: where term = 'value'.
     
     - parameter term:  The term.
     - parameter value: The value.
     
     - returns: `Self`
     */
    public func eq(term: String, value: AnyObject) -> Self { return self.addOperationRequirement(term, operation:.Equal, value: value) }
    
    /**
     Greater-than. Query: where term > 'value'.
     
     - parameter term:  The term.
     - parameter value: The value.
     
     - returns: `Self`
     */
    public func greaterThan(term: String, value: AnyObject) -> Self { return self.gt(term, value: value) }
    
    /**
     Greater-than. Query: where term > 'value'.
     
     - parameter term:  The term.
     - parameter value: The value.
     
     - returns: `Self`
     */
    public func gt(term: String, value: AnyObject) -> Self { return self.addOperationRequirement(term, operation:.GreaterThan, value: value) }
    
    /**
     Greater-than-or-equal-to. Query: where term >= 'value'.
     
     - parameter term:  The term.
     - parameter value: The value.
     
     - returns: `Self`
     */
    public func greaterThanOrEqual(term: String, value: AnyObject) -> Self { return self.gte(term, value: value) }
    
    /**
     Greater-than-or-equal-to. Query: where term >= 'value'.
     
     - parameter term:  The term.
     - parameter value: The value.
     
     - returns: `Self`
     */
    public func gte(term: String, value: AnyObject) -> Self { return self.addOperationRequirement(term, operation:.GreaterThanEqualTo, value: value) }
    
    /**
     Less-than. Query: where term < 'value'.
     
     - parameter term:  The term.
     - parameter value: The value.
     
     - returns: `Self`
     */
    public func lessThan(term: String, value: AnyObject) -> Self { return self.lt(term, value: value) }
    
    /**
     Less-than. Query: where term < 'value'.
     
     - parameter term:  The term.
     - parameter value: The value.
     
     - returns: `Self`
     */
    public func lt(term: String, value: AnyObject) -> Self { return self.addOperationRequirement(term, operation:.LessThan, value: value) }
    
    /**
     Less-than-or-equal-to. Query: where term <= 'value'.
     
     - parameter term:  The term.
     - parameter value: The value.
     
     - returns: `Self`
     */
    public func lessThanOrEqual(term: String, value: AnyObject) -> Self { return self.lte(term, value: value) }
    
    /**
     Less-than-or-equal-to. Query: where term <= 'value'.
     
     - parameter term:  The term.
     - parameter value: The value.
     
     - returns: `Self`
     */
    public func lte(term: String, value: AnyObject) -> Self { return self.addOperationRequirement(term, operation:.LessThanEqualTo, value: value) }
    
    /**
     Contains. Query: location within val of lat, long.
     
     - parameter distance:  The distance from the latitude and longitude.
     - parameter latitude:  The latitude.
     - parameter longitude: The longitude.
     
     - returns: `Self`
     */
    public func locationWithin(distance: Float, latitude: Float, longitude: Float) -> Self {
        return self.addRequirement(UsergridQuery.LOCATION + UsergridQuery.SPACE + UsergridQuery.WITHIN + UsergridQuery.SPACE + distance.description + UsergridQuery.SPACE + UsergridQuery.OF + UsergridQuery.SPACE + latitude.description + UsergridQuery.COMMA + longitude.description )
    }
    
    /**
     Or operation for conditional queries.
     
     - returns: `Self`
     */
    public func or() -> Self {
        if !self.requirementStrings.first!.isEmpty {
            self.requirementStrings.insert(UsergridQuery.OR, atIndex: 0)
            self.requirementStrings.insert(UsergridQuery.EMPTY_STRING, atIndex: 0)
        }
        return self
    }

    /**
     And operation for conditional queries.

     - returns: `Self`
     */
    public func and() -> Self {
        if !self.requirementStrings.first!.isEmpty {
            self.requirementStrings.insert(UsergridQuery.AND, atIndex: 0)
            self.requirementStrings.insert(UsergridQuery.EMPTY_STRING, atIndex: 0)
        }
        return self
    }
    
    /**
     Not operation for conditional queries.
     
     - returns: `Self`
     */
    public func not() -> Self {
        if !self.requirementStrings.first!.isEmpty {
            self.requirementStrings.insert(UsergridQuery.NOT, atIndex: 0)
            self.requirementStrings.insert(UsergridQuery.EMPTY_STRING, atIndex: 0)
        }
        return self
    }
    
    /**
     Sort. Query: order by term `sortOrder`
     
     - parameter term:       The term.
     - parameter sortOrder:  The order.
     
     - returns: `Self`
     */
    public func sort(term: String, sortOrder: UsergridQuerySortOrder) -> Self {
        self.orderClauses[term] = sortOrder
        return self
    }
    
    /**
     Sets the collection name.
     
     - parameter collectionName: The new collection name.
     
     - returns: `Self`
     */
    public func collection(collectionName: String) -> Self {
        self.collectionName = collectionName
        return self
    }

    /**
     Sets the collection name.

     - parameter type: The new collection name.

     - returns: `Self`
     */
    public func type(type: String) -> Self {
        self.collectionName = type
        return self
    }
    
    /**
     Sets the limit on the query.  Default limit is 10.
     
     - parameter limit: The limit.
     
     - returns: `Self`
     */
    public func limit(limit: Int) -> Self {
        self.limit = limit
        return self
    }
    
    /**
     Adds a preconstructed query string as a requirement onto the query.
     
     - parameter value: The query string.
     
     - returns: `Self`
     */
    public func ql(value: String) -> Self {
        return self.addRequirement(value)
    }
    
    /**
     Sets the cursor of the query used internally by Usergrid's APIs.
     
     - parameter value: The cursor.
     
     - returns: `Self`
     */
    public func cursor(value: String?) -> Self {
        self.cursor = value
        return self
    }

    /**
     A special builder property that allows you to input a pre-defined query string. All builder properties will be ignored when this property is defined.

     - parameter value: The pre-defined query string.

     - returns: `Self`
     */
    public func fromString(value: String?) -> Self {
        self.fromStringValue = value
        return self
    }

    /**
     Adds a URL term that will be added next to the query string when constructing the URL append.
     
     - parameter term:        The term.
     - parameter equalsValue: The value.
     
     - returns: `Self`
     */
    public func urlTerm(term: String, equalsValue: String) -> Self {
        if (term as NSString).isEqualToString(UsergridQuery.QL) {
            self.ql(equalsValue)
        } else {
            self.urlTerms.append(term + UsergridQueryOperator.Equal.stringValue + equalsValue)
        }
        return self
    }
    
    /**
     Adds a string requirement to the query.
     
     - parameter term:        The term.
     - parameter operation:   The operation.
     - parameter stringValue: The string value.
     
     - returns: `Self`
     */
    public func addOperationRequirement(term: String, operation: UsergridQueryOperator, stringValue: String) -> Self {
        return self.addOperationRequirement(term,operation:operation,value:stringValue)
    }
    
    /**
     Adds a integer requirement to the query.
     
     - parameter term:      The term.
     - parameter operation: The operation.
     - parameter intValue:  The integer value.
     
     - returns: `Self`
     */
    public func addOperationRequirement(term: String, operation: UsergridQueryOperator, intValue: Int) -> Self {
        return self.addOperationRequirement(term,operation:operation,value:intValue)
    }
    
    private func addRequirement(requirement: String) -> Self {
        var requirementString: String = self.requirementStrings.removeAtIndex(0)
        if !requirementString.isEmpty {
            requirementString += UsergridQuery.SPACE + UsergridQuery.AND + UsergridQuery.SPACE
        }
        requirementString += requirement
        self.requirementStrings.insert(requirementString, atIndex: 0)
        return self
    }
    
    private func addOperationRequirement(term: String, operation: UsergridQueryOperator, value: AnyObject) -> Self {
        if value is String {
            return self.addRequirement(term + UsergridQuery.SPACE + operation.stringValue + UsergridQuery.SPACE + ((value.description.isUuid()) ? UsergridQuery.EMPTY_STRING : UsergridQuery.APOSTROPHE) + value.description + ((value.description.isUuid()) ? UsergridQuery.EMPTY_STRING : UsergridQuery.APOSTROPHE) )
        } else {
            return self.addRequirement(term + UsergridQuery.SPACE + operation.stringValue + UsergridQuery.SPACE + value.description)
        }
    }
    
    private func constructOrderByString() -> String {
        var orderByString = UsergridQuery.EMPTY_STRING
        if !self.orderClauses.isEmpty {
            var combinedClausesArray: [String] = []
            for (key,value) in self.orderClauses {
                combinedClausesArray.append(key + UsergridQuery.SPACE + value.stringValue)
            }
            for index in 0..<combinedClausesArray.count {
                if index > 0 {
                    orderByString += UsergridQuery.COMMA
                }
                orderByString += combinedClausesArray[index]
            }
            if !orderByString.isEmpty {
                orderByString = UsergridQuery.SPACE + UsergridQuery.ORDER_BY + UsergridQuery.SPACE + orderByString
            }
        }
        return orderByString
    }
    
    private func constructURLTermsString() -> String {
        return (self.urlTerms as NSArray).componentsJoinedByString(UsergridQuery.AMPERSAND)
    }
    
    private func constructRequirementString() -> String {
        var requirementsString = UsergridQuery.EMPTY_STRING
        var requirementStrings = self.requirementStrings
        
        // If the first requirement is empty lets remove it.
        if let firstRequirement = requirementStrings.first where firstRequirement.isEmpty {
            requirementStrings.removeFirst()
        }
        
        // If the first requirement now is a conditional separator then we should remove it so its not placed at the end of the constructed string.
        if let firstRequirement = requirementStrings.first where firstRequirement == UsergridQuery.OR || firstRequirement == UsergridQuery.NOT {
            requirementStrings.removeFirst()
        }
        
        requirementsString = (requirementStrings.reverse() as NSArray).componentsJoinedByString(UsergridQuery.SPACE)
        return requirementsString
    }
    
    private func constructURLAppend(autoURLEncode: Bool = true) -> String {

        if let fromString = self.fromStringValue {
            var requirementsString = fromString
            if autoURLEncode {
                if let encodedRequirementsString = fromString.stringByAddingPercentEncodingWithAllowedCharacters(NSCharacterSet.URLQueryAllowedCharacterSet()) {
                    requirementsString = encodedRequirementsString
                }
            }
            return "\(UsergridQuery.QUESTION_MARK)\(UsergridQuery.QL)=\(requirementsString)"
        }

        var urlAppend = UsergridQuery.EMPTY_STRING
        if self.limit != UsergridQuery.LIMIT_DEFAULT {
            urlAppend += "\(UsergridQuery.LIMIT)=\(self.limit.description)"
        }
        let urlTermsString = self.constructURLTermsString()
        if !urlTermsString.isEmpty {
            if !urlAppend.isEmpty {
                urlAppend += UsergridQuery.AMPERSAND
            }
            urlAppend += urlTermsString
        }
        if let cursorString = self.cursor where !cursorString.isEmpty {
            if !urlAppend.isEmpty {
                urlAppend += UsergridQuery.AMPERSAND
            }
            urlAppend += "\(UsergridQuery.CURSOR)=\(cursorString)"
        }
        
        var requirementsString = UsergridQuery.SELECT_ALL + UsergridQuery.SPACE + self.constructRequirementString()
        let orderByString = self.constructOrderByString()
        if !orderByString.isEmpty {
            requirementsString += orderByString
        }
        if !requirementsString.isEmpty {
            if autoURLEncode {
                if let encodedRequirementsString = requirementsString.stringByAddingPercentEncodingWithAllowedCharacters(NSCharacterSet.URLQueryAllowedCharacterSet()) {
                    requirementsString = encodedRequirementsString
                }
            }
            if !urlAppend.isEmpty {
                urlAppend += UsergridQuery.AMPERSAND
            }
            urlAppend += "\(UsergridQuery.QL)=\(requirementsString)"
        }
        
        if !urlAppend.isEmpty {
            urlAppend = "\(UsergridQuery.QUESTION_MARK)\(urlAppend)"
        }
        return urlAppend
    }
    
    private(set) var collectionName: String? = nil
    private(set) var cursor: String? = nil
    private(set) var limit: Int = UsergridQuery.LIMIT_DEFAULT

    private(set) var fromStringValue: String? = nil
    private(set) var requirementStrings: [String] = [UsergridQuery.EMPTY_STRING]
    private(set) var orderClauses: [String:UsergridQuerySortOrder] = [:]
    private(set) var urlTerms: [String] = []
    
    private static let LIMIT_DEFAULT = 10
    private static let AMPERSAND = "&"
    private static let AND = "and"
    private static let APOSTROPHE = "'"
    private static let COMMA = ","
    private static let CONTAINS = "contains"
    private static let CURSOR = "cursor"
    private static let EMPTY_STRING = ""
    private static let IN = "in"
    private static let LIMIT = "limit"
    private static let LOCATION = "location";
    private static let NOT = "not"
    private static let OF = "of"
    private static let OR = "or"
    private static let ORDER_BY = "order by"
    private static let QL = "ql"
    private static let QUESTION_MARK = "?"
    private static let SELECT_ALL = "select *"
    private static let SPACE = " "
    private static let WITHIN = "within"
    
    internal static let ASC = "asc"
    internal static let DESC = "desc"
    internal static let EQUAL = "="
    internal static let GREATER_THAN = ">"
    internal static let GREATER_THAN_EQUAL_TO = ">="
    internal static let LESS_THAN = "<"
    internal static let LESS_THAN_EQUAL_TO = "<="
}
