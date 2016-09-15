;;
;; Licensed to the Apache Software Foundation (ASF) under one or more
;; contributor license agreements.  See the NOTICE file distributed with
;; this work for additional information regarding copyright ownership.
;; The ASF licenses this file to You under the Apache License, Version 2.0
;; (the "License"); you may not use this file except in compliance with
;; the License.  You may obtain a copy of the License at
;;
;;     http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.
;;

;; test_client.nu
;;  Simple tests for Usergrid Client class.
;;
(load "Usergrid")

(class TestClient is NuTestCase
 
 (- testClientSigninAndGetUsers is
    ;; create a client
    (set client ((UGClient alloc) initWithOrganizationId:"1hotrod" withApplicationID:"fred"))
    ;; sign in a test user
    (set response (client logInUser:"alice" password:"test1test"))
    (assert_equal 0 (response transactionState))
    (set object ((response rawResponse) JSONValue))
    (assert_true (object access_token:))
    (assert_true (object expires_in:))
    (set user (object user:))
    (assert_true (user activated:))
    (assert_equal "alice" (user name:))
    (assert_equal "test1test" (user validate-password:))
    ;; query for users
    (set query (UGQuery new))
    (set response (client getUsers:query))
    (assert_equal 0 (response transactionState))
    (set object ((response rawResponse) JSONValue))
    (assert_equal "fred" (object applicationName:))
    (assert_equal "get" (object action:))
    (set entities (object entities:))
    (assert_equal (object count:) (entities count)))

 (- testSemVerOfClient is
    ;; create a client
    (assert_equal "0.1.1" (UGClient version))))
