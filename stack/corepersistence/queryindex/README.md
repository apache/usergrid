Core Persistence Query Index Module
===
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
    * No, but how does it work. Does timeout reset on each query?

* We need to set a Refresh Frequency, how do we design around that?
    * To be determined...

* Better to have index for all, or one per organization?

* For each index, how many shards? The default five is good enough?
    * The number of shards = the maximum number of nodes possible
	
__Work remaining:__

- Figure out why some tests are running out of memory and hanging the build

- Create CHOP-style test cases to stress system

- Bring in tests from org.apache.usergrid.persistence.query

- Get rid of Load Level stuff in Results, allow caller to get anything

- Use Entity Validation utils from Collection instead of my own checks

- Use mutli-get for fetching entities for results
