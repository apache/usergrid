Core Persistence
===============

A Framework to provide basic component services for persistence frameworks


Data Templates
==============

Below are the basic data templates this system should support


Collections
-----------

A scope storage and indexing framework.  Properties should be secondary indexed, and should be able to be queried efficiently.


*MVCC Semantics*

Transaction/Checkpoint logging on indexing.
Consistent data view.  Can potentially be for long running jobs.
Optimistic Locking (maybe)
Atomic updates (maybe)

*Operation Chaining* (maybe)

Possible ability to define an operation context where a set of all writes must either succeed or fail as a group
(can probably be done with MVCC)




Graphs
-----------

A system for creating relationships between scope entities.  The directed edges can be named (a type) and
an index query can be executed on those edges.



Maps
-----------

A map that can store hierarchical keys.  Shorter keys are better.  This should allow for range "scanning".  I.E.

key1: => org1/app1/env1/version1

key2: => org1/app1/env2/version1

Operations:

 Put by key
 Get by key
 Iterate by scan
 Delete by key


Get me all keys present in org1/app1.

Start => org1/app1

End => org1/app1 inclusive

-----------
===========

A write through distributed cache backed by the cassandra map implementation for persistence





