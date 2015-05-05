#IO Framework
Based on the Pipes and Filters software pattern

##How does this all work?
Consider the following example flow:
 
 ```pipeline = 1. appId -> 2. filter1 -> filter2 -> 3. collector -> 4. Observable```
 
 1. First we start with an application. You want to do a certain set of operations on this application. The application gets passed into the pipeline. We extract the application id and pass unto the first filter
 1. The filter represents some action being done unto the application. This could be a "getCollection" to a "deleteIndex" action. We can have as many as we want. These operations will be done in the order that they are represented in above. The results of each of the filters will be fed into the next one for processing. Thus filter 1 will be done before filter 2 can be applied.
	* An important note here is that a cursor may or may not be generated here. this cursor ( if it exists ) will be stored in what is known as a Pipeline Context. The context contains all approriate state cursor information in case we need resume a query after we have run it. 
 1. After we have applied all the filters, we take the results and feed them into a collector. The collector aggreates the results returned and pushes them into a ResultSet. 
 1. The result set ( along with a cursor is one was generated ) is fed into a observable that can be iterated on.
###Pipeline Module

![Top Level Pipeline Diagram](https://github.com/apache/incubator-usergrid/blob/c3897d3abac7226d9a93a831c020567abd00536c/stack/core/src/main/java/org/apache/usergrid/corepersistence/pipeline/PipelineDiagram.jpg?raw=true =800x800) 

1. PipelineModule
	* This handles all of the guice configuration for the Pipeline Module that we are working in. If you need to wire something, do it here. 
1. PipelineBuilderFactory
	* This factory is used to instantiate the ReadPipelineBuilder in CpEntityManager and CpRelationshipManager. 
1. Pipeline
	* The Pipeline class is where it all starts. Here we:
		1. Define the application scope to know what application we're working on
		1. What pipeline operations are going to be passed through 
		1. What type of Collector we are going to be using.
		1. The request and response cursor. 
		1. The limit on the number of entities we might want to get back from a query
		1. Where in the pipeline we are.  
	* The ```execute``` method contains the execution of the pipeline. It retrieves the applications and pipes the operations from one to another until we run out of operations. At that point we aggregate the response into a collection and return an observable with the elements.
1. PipelineContext
	* This is a stateful representation of the current step in the pipeline. It contains the cusor that we want to run (requestCursor) and the cursor that is returned from the filter (responseCursor). 
	* Each filter contains a Pipeline Context due to the above reason. 
1. PipelineOperation
	* Top class in the Pipeline because it defines what every pipeline operation needs to have and extend. Mandates that every class in the Pipeline extend this class.
	* Primary interface for Filtering and Collection commands in the Read module. 
1. Cursor Module
	* Contains the Request and ResponseCursor classes that are instantiated in Pipeline Module.
	* Contains logic that represents the cursor logic.
1. Read Module
	* Contains the logic behind reading from graph and elasticsearch.

***

###Indepth Cursor Module Explanation
 ![Top Level Pipeline Diagram](https://github.com/apache/incubator-usergrid/blob/c3897d3abac7226d9a93a831c020567abd00536c/stack/core/src/main/java/org/apache/usergrid/corepersistence/pipeline/cursor/CursorDiagram.jpg?raw=true =800x800 ) 

1. ResponseCursor 
	* This is the cursor that gets returned in the response after the filter has run. 
	* The flow defined by the Response cursor is as follows
		1. Set cursor(s) that are made up of a Integer and a CursorEntry 
		1. Response Cursor gets initalized
		1. We go into the CursorEntry class that consists of the Cursor ( of a raw type ) and the serializer that we would use to parse the Cursor.
1. RequestCursor 
	* Contains some information on the parsedCursor
	* This gets populated by either the User ( using a cursor that we've given them), or by the pipeline feeding the cursor into the next stage. 
1. AbstractCursorSerializer
	* Used exclusivly in the read module and should probably be refactored there
	* Is a CursorSerializer that implements the the base cursor methods.
1. CursorSerializerUtil
	* Defines the type of serialization we encode the cursors with. In this case Smile Jackson Serialization.
		
***
###Indepth Read Module Explanation
 ![Top Level Pipeline Diagram](https://github.com/apache/incubator-usergrid/blob/c3897d3abac7226d9a93a831c020567abd00536c/stack/core/src/main/java/org/apache/usergrid/corepersistence/pipeline/read/ReadDiagram.jpg?raw=true =1300x1000) 

1. Filter
	* Extends generic PipelineOperation
	* Interacts with anything that classifies itself as a filter.
	* Defines output as a element T and a FilterResult. 
1. AbstractPathFilter
	* This abstract filter is meant to be extended by any filter that requires a cursor and operations on that cursor. 
	* Extends from the AbstractPipelineOperation because a filter is a pipeline operation. 
	* Is used in all the submodules as a way to deal with cursors. 
1. CursorSeek
	* Protected internal class that lives in AbstractPathFilter
	* When resuming we use the RequestCursor to page for values. After use the cursor is no longer valid, and we only need to seek on the values that were returned from the cursor call. Any calls on the RequestCursor will be empty afterwards.
1. Collector
	* Extends generic PipelineOperation
	* Primary used to interact with the collect module
	* Used to reduce our stream of results into our final output.
1. Elasticsearch Module
	* Contains the functions we use to actual perform filtered commands that contain elasticsearch components.
	* These will typically return Canadiate Result sets that will be processed by the collector. 
1. Collect Module
	* Contains a single filter that maps id's, and the collector that processes entity id's. 
1. Graph Module
	* Contains the filters that are used to interact with the lower level Graph Module.
1. FilterFactory
	* Defines all of the possible read filters that can be added to a pipeline. 
	* Contain comments on what each type of filter should accomplish.  
1. ReadPipelineBuilder 
	* Contains the builder interface that will assemble the underlying pipe along with updating and keeping track of its state. 
1. ReadPipelineBuilderImpl
	* Contains the builder implementation of the ReadPipelineBuilder. 
	* Adds on filters from FilterFactory depending on the type of action we take. 
	* Contains execute method when the pipeline is finished building. This pushes results as an observable back up. 
1. CollectorState
	* The state that holds a singleton collector instance and what type of collector the Collector filter should be using. 
	* The collector state gets initialized with a CollectorFactory and then gets set with which collector it should use for the Collector object that it holds. 
	* This is a private inner class within ReadPipelineBuilderImpl
1. Results Page
	* Contains the encapsulation of entities as a group of responses.
	* Holds the list of entities along with the limit of the entities we want for a response and the cursor that gets returned.
1. EdgePath
	* Represents the path from the intial entity to the emitted element. 
	* A list of these represnt a path through the graph to a specific element.    
	
***

###Indepth Collect Module Explanation

![Top Level Pipeline Diagram](https://github.com/apache/incubator-usergrid/blob/c3897d3abac7226d9a93a831c020567abd00536c/stack/core/src/main/java/org/apache/usergrid/corepersistence/pipeline/read/collect/CollectDiagram.jpg?raw=true =1300x1000) 


1. EntityFilter
	*  This filter is our intermediate resume filter. So if we're returning in consistent results around the end of the limit query then it is possible for the last result on the last page and first result on the resume page to have the same entity. This filter detects is that is the case and will filter the first result if the id's of the last entity of the last page and the first entity of the new page match.
1. IdCursorSerializer
	* The serializer for Id's.
1. AbstractCollector
	* Abstract class that derives from Collector class
	* Adds a pipelineContext for the collector to work with when looking at cursors.
1. ResultsPageCollector
	* Takes the entities and collects them into results so we can return them through the service and rest tier. Exists for 1.0 compatibility. 
1. ResultsPageWithCursorCollector
	*  This collector aggregates our results together using an arrayList.

	
***
###Indepth Graph Module Explanation
 ![Top Level Pipeline Diagram](https://github.com/apache/incubator-usergrid/blob/c3897d3abac7226d9a93a831c020567abd00536c/stack/core/src/main/java/org/apache/usergrid/corepersistence/pipeline/read/graph/GraphDiagram.jpg?raw=true =800x800) 
 
 1. EdgeCursorSerializer
 	* The serializer we use to decode and make sense of the graph cursor that gets returned.

 The Main difference between ReadGraph and ReadGraph by id is that the Id won't ever bother itself with cursors because it doesn't need to worry about cursor generation. Hence the distinction but very similar patterns. 
 
 1. AbstractReadGraph(EdgeById)Filter
 	* An abstract class that defines how we should read graph edges from name(id).
 1. ReadGraphConnection(ById)Filter
 	* Defines how to read Connections out of the graph using names(id).
 1. ReadGraphCollection(ById)Filter
 	* Defines how to read Collections out of the graph using names(id).
 1. ReadGraphconnectionByTypeFilter
 	* A filter that reads graph connections by type.
 1. EntityIdFilter
	* A stopgap filter that helps migrating from the service tier and its entities. Just makes a list of entities. 
 1. EdgeLoadFilter
 	* Loads entities from a set of Ids.
 1. EntityVerifier
 	* Verifies that the id's in the filter results exist and can be added to the results. 
 	* Functions as a collector. 	 
 1. EdgeState
 	* A wrapper class that addresses the problem with skipping a value if a concurrent change has been made on the data set. In some cases we would be skipping a value. Now the cursor will always try to seek to the same position that we ended instead of the new position created by the change in data.   	

***
###Indepth Elasticsearch Module Explanation
 
 ![Top Level Pipeline Diagram](https://github.com/apache/incubator-usergrid/blob/c3897d3abac7226d9a93a831c020567abd00536c/stack/core/src/main/java/org/apache/usergrid/corepersistence/pipeline/read/elasticsearch/Elasticsearchdiagram.jpg?raw=true =800x800) 
 	
 1. ElasticsearchCursorSerializer
 	* The serializer we use to decode and make sense of the elasticsearch cursor.
 1. Candidate
 	* Contains the candidate result and the search edge that was searched for that result.
	* Since we needed the search edge along with the CandidateResults we packaged them together into a single class.
 1. CandidateIdFilter
 	* Takes in candidate results and outputs a stream of validated Ids.
 	* Uses the EntityCollector to map a fresh new cp entity to an old 1.0 version of the entity. Then we return those results to the upper tiers.
 1. EntityCollector
 	* I'm not entirely clear how the collector actually does the mapping. Seems like it just does the elasticsearch repair and checks entity versions. Then collects the entities into a result set 
 1. AbstractElasticSearchFilter
 	* This extends into the same pattern as the Graph Module where we make a abstract filter so we can extend it to easily accomodate Collection or Connection searching.
 1. ElasticSearchConnectionFilter
  	* Creates the filter that will go and search for connections in elasticsearch. 
 1. ElasticSearchCollectionFilter
  	* Creates the filter that will go and search for collections in elasticsearch.
 1. CandidateEntityFilter
  	* Searches on incoming Candidate entity and returns an entity instead of an Id like the CandidateIdFilter.
  	* Does a similar repair using the EntityVerifier. 
 1. EntityVerifier
  	* Collects the entities emitted and returns them as a result set. Also verifies that the entities exist or if they need to be repaired in elasticsearch.

 



 
