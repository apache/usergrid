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

;; test_connection.nu
;;  Simple tests for Usergrid Connection class.
;;
(load "Usergrid")

(set ORGANIZATION     "timburks")
(set APPLICATION      "unittest")
(set APPLICATION_UUID "6a87cc36-a328-11e2-836c-02e81af253c0")
(set CLIENT_ID        "YXA6aofMNqMoEeKDbALoGvJTwA")
(set CLIENT_SECRET    "YXA6oEdSD8zQyKxBKv_3xxZgvISM8C4")

(function perform (request)
          (set client ((UGHTTPClient alloc) initWithRequest:request))
          (set result (client connect))
          result)

(class TestConnection is NuTestCase
 ;; run this before every test
 (- setup is
    (set usergrid (UGConnection sharedConnection))
    (usergrid setOrganization:ORGANIZATION)
    (usergrid setApplication:APPLICATION))
 
 (- testClientSignin is
    (set usergrid (UGConnection sharedConnection))
    ;; log in
    (set request (usergrid getAccessTokenForApplicationWithClientID:CLIENT_ID
                                                       clientSecret:CLIENT_SECRET))
    (set result (perform request))
    (set object (result object))
    (assert_true (object access_token:))
    ;; capture token
    (usergrid authenticateWithResult:result)
    (assert_true (usergrid isAuthenticated)))
 
 (- testUserSignin is
    (set usergrid (UGConnection sharedConnection))
    ;; log in
    (set request (usergrid getAccessTokenForApplicationWithUsername:"test1"
                                                           password:"test1"))
    (set result (perform request))
    (set object (result object))
    (assert_true (object access_token:))
    ;; capture token
    (usergrid authenticateWithResult:result)
    (assert_true (usergrid isAuthenticated)))
 
 (- testApplicationDetail is
    (set usergrid (UGConnection sharedConnection))
    ;; log in
    (set request (usergrid getAccessTokenForApplicationWithUsername:"test2"
                                                           password:"test2"))
    (set result (perform request))
    (usergrid authenticateWithResult:result)
    ;; get application description
    (set request (usergrid getApplication:APPLICATION inOrganization:ORGANIZATION))
    (set result (perform request))
    (set object (result object))
    (assert_equal APPLICATION (object applicationName:))
    (assert_equal ORGANIZATION (object organization:))
    (assert_equal 1 ((object entities:) count))
    (set collections ((((object entities:) 0) metadata:) collections:))
    (assert_equal 8 (collections count)))
 
 (- testAssets is
    (set usergrid (UGConnection sharedConnection))
    ;; log in
    (usergrid authenticateWithResult:(perform (usergrid getAccessTokenForApplicationWithUsername:"test1" password:"test1")))
    
    ;; delete all assets
    (perform (usergrid deleteEntitiesInCollection:"assets"))
    (set results (perform (usergrid getEntitiesInCollection:"assets" limit:10)))
    (set object (results object))
    (assert_equal 0 (object count:))
    
    ;; add some assets
    (set N 2)
    (N times:
       (do (i)
           (set results (perform (usergrid createEntityInCollection:"assets" withValues:(dict name:"asset-#{i}"
                                                                                            number:i
                                                                                             owner:APPLICATION_UUID
                                                                                              path:"asset-#{i}"))))
           (set object (results object))
           (set uuid (((object entities:) 0) uuid:))
           (set results (perform (usergrid postData:("asset-#{i}" dataUsingEncoding:NSUTF8StringEncoding)
                                           forAsset:uuid)))))
    (set results (perform (usergrid getEntitiesInCollection:"assets" limit:10)))
    (set object (results object))
    (assert_equal N (object count:))
    ((object entities:) eachWithIndex:
     (do (asset i)
         (set uuid (asset uuid:))
         (set results (perform (usergrid getDataForAsset:uuid)))
         (set asset-string (NSString stringWithData:(results data) encoding:NSUTF8StringEncoding))
         (assert_equal i (asset number:))
         (assert_equal (asset owner:) APPLICATION_UUID)
         (assert_equal (asset path:) asset-string)
         (assert_equal (asset name:) asset-string)))))



