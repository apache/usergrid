Core Persistence Query Index Module
===

---
This module defines an __EntityCollectionIndex__ interface for indexing, de-indexing and querying Entities within a Collection. Queries are expressed in Usergrid's SQL-like query syntax. 

Implementation
---
This module also provides an implementation of the EntityCollectionIndex using the open source ElasticSearch as index and query engine. 

Here are the important parts of the QueryIndex module:

* __EntityCollectionIndex__: the interface that defines methods for indexing, deindexing and querying an index. 

* __EntityCollectionIndexFactory__: factory for obtaining an index for an Entity Collection.

* __IndexFig__: defines configuration needed for this module to operate.

* __org.apache.usergrid.persistence.index.impl__: provides an implementation using ElasticSearch via its Java API. 

* __Query, Results and EntityRefs__: these classes were "ported" from Usergrid 1.0 to support Usergrid query syntax. We define a grammar and use ANTLR to generate a parser and a lexer.

Legacy Tests
---
These tests help us ensure that Usergrid 1.0 query syntax is fully supported by this module. To enable re-use of tests from Usergrid 1.0 this module's tests include some "legacy" test infrastructure classes, e.g. Application, Core Application. It also includes a partial implementation of the old Entity Manager interface.

These are legacy tests that are now (mostly) running against the index implementaiton.

* GeoIT
* IndexIT
* CollectionIT
* QueryTest
* GrammarTreeTest
* LongLiteralTest
* StringLiteralTest

Stress Tests
---
Coming soon...


Issues and work remaining
---

* We have to set a Query Cursor Timeout, is that a problem?

* We need to set a Refresh Frequency, how do we design around that?

* What additional tests that should bring in from 1.0?

* What is a good Chop stress test, just index & query lots of stuff?

* Is it OK to use scope.getName() as ElasticSearch type name? 
	* Are scope names guaranteed to be unique? 
	* Does a type name need to be a composite like "appId|orgId|scope"?
	
* Should we add support for connections aka "edges" via Graph module?

* What things can be done asyncrhonously here? Is there anything we should parallelize?



__Work remaining:__

- Figure out the above issues

- Figure out why some tests are running out of memory and hanging the build

- Create CHOP-style test cases to stress system










