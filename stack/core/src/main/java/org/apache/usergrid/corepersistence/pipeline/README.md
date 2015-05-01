#IO Framework
Based on the Pipes and Filters software pattern

![Top Level Pipeline Diagram](PipelineDiagram.jpg =800x800) 


##How does this all work?
Consider the following example flow:
 
 ```pipeline = 1. appId -> 2. filter1 -> filter2 -> 3. collector -> 4. PipelineResult```
 
 1. First we start with an application. You want to do a certain set of operations on this application. The application gets passed into the pipeline. We extract the application id and pass unto the first filter
 1. The filter represents some action being done unto the application. This could be a "getCollection" to a "deleteIndex" action. We can have as many as we want. These operations will be done in the order that they are represented in above. The results of each of the filters will be fed into the next one for processing. Thus filter 1 will be done before filter 2 can be applied.
 	a. An important note here is that a cursor may or may not be generated here. this cursor ( if it exists ) will be stored in what is known as a Pipeline Context. The context contains all approriate state cursor information in case we need resume a query after we have run it. 
 1. After we have applied all the filters, we take the results and feed them into a collector. The collector aggreates the results returned and pushes them into a ResultSet. 
 1. The result set ( along with a cursor is one was generated ) is fed into the PipelineResult class. The PipelineResult is returned as a observable that can be iterated on by the calling method.

Above is the uml diagram of how the seperate modules are connected. We see 7 core parts

###Pipeline Module

1. PipelineModule
	a. This handles all of the guice configuration for the Pipeline Module that we are working in. If you need to wire something do it here. 
1. PipelineBuilderFactory
	a. This factory is used to instantiate the ReadPipelineBuilder in CpEntityManager and CpRelationshipManager. This lets us use the pipeline outside of the pipeline module.
1. Pipeline
	a. The Pipeline class is where it all starts. Here we:
		1. Define the application scope to know what application we're working on
		1. What pipeline operations are going to be passed through 
		1. What type of Collector we are going to be using.
		1. The request and response cursor. 
		1. The limit on the number of entities we might want to get back from a query
		1. Where in the pipeline we are.  
	a. The ```execute``` method contains the execution of the pipeline. It retrieves the applications and pipes the operations from one to another until we run out of operations. At that point we aggregate the response into a collection and return an instance of PipelineResult with the results and a cursor.
	a. Things that depend on the read module
		1. The PipelineOperation
		1. The Collector
	a. Things that depend on the cursor module
		1. Request and Response Cursor
1. PipelineContext
	a. This is a stateful representation of the current step in the pipeline. It contains the cusor that we want to run (requestCursor) and the cursor that is returned from the filter (responseCursor). 
	a. Each filter contains a Pipeline Context due to the above reason. 
1. PipelineResult
	a. Gets created in the Pipeline after the results are collected from the filters.
	a. Depends on the ResponseCursor class. 
1. Cursor Module
	a. Contains the Request and ResponseCursor classes that are directly instantiated in Pipeline Module.
	a. Contains logic that represents the cursor logic
***

###Indepth Cursor Module Explanation
 ![Top Level Pipeline Diagram](cursor/CursorDiagram.jpg =800x800) 

The Cursor Module is made up of 7 classes.

1. ResponseCursor 
	a. This is the cursor that gets returned in the response after the filter has run. 
	b. The flow defined by the Response cursor is as follows
		1. Set cursor(s) that are made up of a Integer and a CursorEntry 
		1. Response Cursor gets initalized
		1. We go into the CursorEntry class that consists of the Cursor ( of a raw type ) and the serializer that we would use to parse the Cursor.
1. RequestCursor 
	a. Contains some information on the parsedCursor
	b. This gets populated by either the User ( using a cursor that we've given them), or by the pipeline feeding the cursor into the next stage. 
	c. Could be 	
		

 
***
###Indepth Read Module Explanation
 ![Top Level Pipeline Diagram](read/ReadDiagram.jpg =1300x1000) 

1. PipelineOperation
	a. Top class in the Pipeline because it defines what every pipeline operation needs to have and extend. Mandates that every class contains a PipelineContext
	b. Primary interface for Filtering commands.
1. CandidateResultsFilter
	a. Is an interface
	a. Extends PipelineOperation 
	b. Defines the types that will be requried in the filter. While not visible in the diagram the CandidateResultsFilters will consist of a <Id, CandidateResults>.
	c. Primary filter that will be used for interfacing with ES (Elasticsearch)
1. Filter
	a. Extends generic PipelineOperation
	b. Primary used to interact with Graph and Entity modules
	b. Why do we use the filter in the ReadPipeline when we could also interchange the Canadiate Results filter? Is it just the type that differentiates it. 
1. AbstractSeekingFilter
	a. This abstract filter is meant to be extended by any filter that requires a cursor and operations on that cursor. 
	b. Extends from the AbstractPipelineOperation because a filter is a pipeline operation. 
	c. Is used in the Graph and Elasticsearch submodules because those both use cursors. 
1. CursorSeek
	a. Protected internal class that lives in AbstractSeekingFilter
	b. Whats the deal with only seeking values on the first call? Is this not similar to pagination? 
	
1. Collector
	a. Extends generic PipelineOperation
	b. Primary used to interact with Entity and Elasticsearch Packages
	a. The stage of our filters that is used to reduce our stream of results into our final output.
1. CollectorState
	a. The state that holds a singleton collector instance and what type of collector the Collector filter should be using. 
	a. The collector state gets initialized with a CollectorFactory and then gets set with which collector it should use for the Collector object that it holds. 
	b. This is a private inner class within ReadPipelineBuilderImpl
1. Elasticsearch Module
	a. Contains the functions we use to actual perform filtered commands that contain elasticsearch components.
	b. These will typically return Canadiate Result sets that will be processed by the collector. 
1. Entity Module
	a. Contains a single filter that maps id's, and the collector that processes entity id's. 
1. Graph Module
	a. Contains the filters that are used to interact with the lower level Graph Module.
1. FilterFactory
	a. Defines all of the possible read filters that can be added to a pipeline. 
	b. Contain comments on what each type of filter should accomplish.  
1. ReadPipelineBuilder 
	a. Contains the builder interface that will assemble the underlying pipe along with updating and keeping track of its state. 
1. ReadPipelineBuilderImpl
	a. Contains the builder implementation of the ReadPipelineBuilder. 
	b. Adds on filters from FilterFactory depending on the type of action we take. 
	c. Contains execute method when the pipeline is finished building. This pushes results as a PipelineResult back up. 
	
***
###Indepth Entity Module Explanation
The entity module only contains two classes. So I won't attach the uml diagram as they aren't related to each other in any way.

1. EntityIdFilter
	a. A stopgap filter that helps migrating from the service tier and its entities. Just makes a list of entities. 
2. EntityLoadCollector
	a. The EntityLoadCollector loops through entity id's and then converts them to our old entity model so that they can go through the service and rest tier. 
***
###Indepth Graph Module Explanation
 ![Top Level Pipeline Diagram](read/graph/GraphDiagram.jpg =800x800) 
 
 1. EdgeCursorSerializer
 	a. The serializer we use to decode and make sense of the graph cursor that gets returned.
 1. AbstractReadGraph(EdgeById)Filter
 	a. An abstract class that defines how we should read graph edges from name(id).
 1. ReadGraphConnection(ById)Filter
 	a. Defines how to read Connections out of the graph using names(id).
 1. ReadGraphCollection(ById)Filter
 	a. Defines how to read Collections out of the graph using names(id).

 2. ReadGraphconnectionByTypeFilter
 	a. A filter that reads graph connections by type.
***
###Indepth Elasticsearch Module Explanation
 
 ![Top Level Pipeline Diagram](read/elasticsearch/ElasticsearchDiagram.jpg =800x800) 
 
 1. Impl Module 
 	a. contains all the implementations and verfiers and loaders for elasticsearch
 2. AbstractElasticSearchFilter
 	a. This extends into the same pattern as the Graph Module where we make a abstract filter so we can extend it to easily accomodate Collection or Connection searching.
 3. CandidateResultsEntityResultsCollector
 	a. Collects the results from Elasticsearch then retrieves them from cassandra and converts them from 2.0 to 1.0 entities that are suitable for return.
 4. CandidateResultsIdVerifyFilter
 	a. Filter that verifies that the canaidate results id's are correct???? What else does this do ? isn't that what the collector does?
 5. ElasticsearchCursorSerializer
 	a. The serializer we use to decode and make sense of the elasticsearch cursor.
 

 



 
