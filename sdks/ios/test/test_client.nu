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
    (assert_equal (object count:) (entities count))))