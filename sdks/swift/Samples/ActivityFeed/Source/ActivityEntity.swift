//
//  ActivityEntity.swift
//  ActivityFeed
//
//  Created by Robert Walsh on 1/20/16.
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
import UsergridSDK

public class ActivityEntity: UsergridEntity {

    public var actor: [String:AnyObject]? { return self["actor"] as? [String:AnyObject] }

    public var content: String? { return self["content"] as? String }

    public var displayName: String? { return self.actor?["displayName"] as? String }

    public var email: String? { return self.actor?["email"] as? String }

    public var imageInfo: [String:AnyObject]? { return self.actor?["image"] as? [String:AnyObject] }

    public var imageURL: String? { return self.imageInfo?["url"] as? String }

    static func registerSubclass() {
        UsergridEntity.mapCustomType("activity", toSubclass: ActivityEntity.self)
    }

    required public init(type: String, name: String?, propertyDict: [String : AnyObject]?) {
        super.init(type: type, name: name, propertyDict: propertyDict)
    }

    required public init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }

    public override func encodeWithCoder(aCoder: NSCoder) {
        super.encodeWithCoder(aCoder)
    }
    
}