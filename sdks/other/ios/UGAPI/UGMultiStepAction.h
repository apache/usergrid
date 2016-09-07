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

#import <Foundation/Foundation.h>

// an enumeration for multi-step processes. 
enum
{
    kMultiStepNone = 0,
    kMultiStepCreateActivity = 1, // create an activity
    kMultiStepPostActivity = 2,   // after creating an activity, post it to the user
    kMultiStepCreateGroupActivity = 3, // create an activity
    kMultiStepPostGroupActivity = 4,   // after creating an activity, post it to the user
    kMultiStepCleanup = 5         // the final step of all multi-step transactions
};

// UGMultiStepAction is used internally for client actions that
// require multiple transactions with the service. It is simple data
// storage, used by UGClient in abstracting out multi-step transactions
@interface UGMultiStepAction : NSObject

// the transaction ID that this multistep is associated
// with. When a transaction of this ID is complete, this is the
// UGMultiStepAction instance ot ask what to do next
@property int transactionID;

// the next action this transaction should take
@property int nextAction;

// data necessary for subsequent steps
@property NSString *userID; 
@property NSString *groupID; 
@property NSDictionary *activity; 

// the transactionID that will be sent to the user.
// This is distinct from normal transaction IDs,
// which we use internally at each step. 
@property int outwardTransactionID;

// YES if this action should be reported to the
// caller .No if not.
@property BOOL reportToClient;

@end
