/*
 * Generates Markdown representation of 
 */

// Depdency management with Grape:
//    http://docs.groovy-lang.org/latest/html/documentation/grape.html

package usergrid

@Grab(group = 'io.swagger', module = 'swagger-parser', version = '1.0.8')
@Grab(group = 'io.swagger', module = 'swagger-compat-spec-parser', version = '1.0.8')
@Grab(group = 'com.github.spullara.mustache.java', module = 'compiler', version = '0.8.18-SNAPSHOT')
@Grab(group = 'org.slf4j', module = 'slf4j-simple', version = '1.7.12')

import com.github.mustachejava.DefaultMustacheFactory
import io.swagger.models.Model
import io.swagger.models.Operation
import io.swagger.models.parameters.BodyParameter
import io.swagger.models.parameters.RefParameter
import io.swagger.models.properties.RefProperty
import io.swagger.parser.*
import org.apache.commons.lang3.RandomStringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory


/**
 * Generates Usergrid API docs from Swagger in Markdown format.
 */
public class ApiDocGenerator {
   
    static Logger logger = LoggerFactory.getLogger(ApiDocGenerator.class);
    
    def parser = new SwaggerParser();
    def swagger = parser.read("src/main/resources/usergrid-swagger.yaml");
   
    // Mustache reference: http://mustache.github.io/mustache.5.html
    // mustache.java info: https://github.com/spullara/mustache.java
    def writer;
    def mf = new DefaultMustacheFactory();

    def mustacheBase = "src/main/resources";
    
    def operationTemplate;
    def modelTemplate;
    def fileStartTemplate;
    def fileEndTemplate;
    
    def allTags = [];
    def allModels = [];
    def urlOpsBySchema = [:];
    def modelsBySchema = [:];
    
    def tagsToUrlOps = new TreeMap();
    
    def definitions = new TreeMap();
    def subTypes = new TreeMap();
    
    public ApiDocGenerator() {
        writer = new OutputStreamWriter(new FileOutputStream("rest-endpoints/api-docs.md"));
        operationTemplate = mf.compile(
                new FileReader("${mustacheBase}/markdown/operation.mustache"), "operation");
        modelTemplate = mf.compile(
                new FileReader("${mustacheBase}/markdown/model.mustache"), "model");
        fileStartTemplate = mf.compile(
                new FileReader("${mustacheBase}/markdown/file-start.mustache"), "file-start");
    }
    
    public static void main( String[] args ) {
        
        def htmlgen = new HtmlApiDocGenerator();
        htmlgen.generate();
        
        def mdgen = new ApiDocGenerator();
        mdgen.generate();
    }
    
    def generate() {
      
        // build various hashmaps and lists to help organize and cross-reference content
        
        swagger.getPaths().each { pathEntry ->
            def url = pathEntry.key;
            def path = pathEntry.value;
           
            if (path.get != null) {
                addOperation("GET", url, path.get);
            }
            if (path.post != null) {
                addOperation("POST", url, path.post);
            }
            if (path.put != null) {
                addOperation("PUT", url, path.put);
            }
            if (path.delete != null) {
                addOperation("DELETE", url, path.delete);
            }
        };
        tagsToUrlOps.each { entry -> allTags.add(entry.key) };

        swagger.getDefinitions().each { entry ->
            addModel( entry.key, entry.value );
        };
        definitions.each { entry -> allModels.add(entry.key); };
        
        // generate the doc
        
        generateFileStart();
        
        generateMethodsSectionTitle();
        
        tagsToUrlOps.each { entry -> 
            def tag = entry.key;
            def urlOps = entry.value;
            generateMethodsTitle(tag);
            urlOps.each { urlOp -> formatOperation( urlOp ); };
        };

        generateModelsTitle();
        
        definitions.each { entry ->
            def name = entry.key;
            def model = entry.value;
            formatModel( name, model );
        };
        
        generateSubTypesTitle();
        
        subTypes.each { entry ->
            def name = entry.key;
            def model = entry.value;
            formatModel( name, model );
        };
        
        generateFileEnd();
    }
    
    def addOperation( String method, String url, Operation operation ) {
       
        // assume each operation has one tag
        def tag = operation.tags[0];
        
        def urlOps = tagsToUrlOps[tag];
        if ( urlOps == null ) {
            urlOps = [];
            tagsToUrlOps[tag] = urlOps;
        }
        
        def urlOp = new HashMap();
        urlOp.url = url;
        urlOp.method = method;
        urlOp.operation = operation;
        urlOp.opId = RandomStringUtils.randomAlphanumeric(10).toLowerCase();
        urlOps.add(urlOp);

        operation.getResponses().each { responseEntry ->

            if (responseEntry.value.schema != null) {

                if (responseEntry.key.equals("200")) {
                    def list = urlOpsBySchema[responseEntry.value.schema.ref];
                    if (list == null) {
                        list = new HashSet();
                        urlOpsBySchema[responseEntry.value.schema.ref] = list;
                    }
                    list.add(urlOp);
                }
            }
        };

        operation.getParameters().each { param ->
            
            if ( param instanceof BodyParameter && param.schema != null ) {
                def list = urlOpsBySchema[param.schema.ref];
                if (list == null) {
                    list = new HashSet();
                    urlOpsBySchema[param.schema.ref] = list;
                }
                list.add(urlOp);
            }
        };
        
    }
    
    def addModel(name, model) {
       
        if (urlOpsBySchema[name] != null || name.equals("Error")) {
            definitions.put(name, model);
        } else if (modelsBySchema[name] != null ) {
            subTypes.put(name, model);
        } else {
            logger.error("${name} omitted because it is not referenced by any API path or definition.");
        }
        
        model.properties.each { propertyEntity ->
            
            if (propertyEntity.value instanceof RefProperty ) {
                def list = modelsBySchema[ propertyEntity.value.ref ];
                if ( list == null ) {
                    list = [];
                    modelsBySchema[ propertyEntity.value.ref ] = list;
                }
                def modelNames = [:];
                modelNames.refName = name;
                modelNames.refName_lc = name.toLowerCase();
                list.add(modelNames);
            }
        };
    }

    def formatOperation( urlOp ) {
        
        // build up a scope and then call a mustache template
        // makes some assumptions based on the Usergrid Swagger file

        def url = urlOp.url;
        def method = urlOp.method;
        def op = urlOp.operation;
        
        // put responses in array form, mustache doesn't play nice with associative arrays
        def responses = [];
        op.getResponses().each { responseEntry -> 
            def response = [:];
            response.status = responseEntry.key;
            response.description = responseEntry.value.description;
            
            // if parameter has a schema, assume that it is a reference
            if ( responseEntry.value.schema != null) {
                response.schema = responseEntry.value.schema.ref;
                response.schemaAnchor = responseEntry.value.schema.ref.toLowerCase();
               
                // keep track of paths that use each schema definition
                if ( response.status.equals("200") ) {
                    def list = urlOpsBySchema[response.schema];
                    if ( list == null ) {
                        list = [];
                        urlOpsBySchema[response.schema] = list;
                    }
                    list.add(urlOp);
                }
            }
            responses.add(response);
        }

        def params = [];
        op.getParameters().each { parameter -> 
            def param = [:];
            param.name = parameter.name;
            param.name_lc = parameter.name.toLowerCase();
            param.required = parameter.required;
            param.description = parameter.description;
            param.in = parameter.in;
            
            // assume that body parameters have a schema that is a reference
            if (parameter.in == "body") {
                
                param.schemaRef = parameter.schema.ref;
                param.schemaAnchor = parameter.schema.ref.toLowerCase();

                // keep track of paths that use each schema definition
                def list = urlOpsBySchema[parameter.schema.ref];
                if ( list == null ) {
                    list = [];
                    urlOpsBySchema[parameter.schema.ref] = list;
                }
                list.add(urlOp);

            } else if ( !(parameter instanceof RefParameter) ) {
                param.type = parameter.type;
            }
            params.add(param);
        }
        
        def scope = [:];
        scope.url = url;
        scope.method = method;
        scope.description = op.getDescription();
        scope.summary = op.getSummary();
        scope.tags = op.getTags();
        scope.responses = responses;
        scope.parameters = params;
        scope.opId = urlOp.opId;
        
        operationTemplate.execute(writer, scope);
        writer.flush();
    }
    
    def formatModel(String name, Model model) {

        def scope = [:];
        scope.referrers = urlOpsBySchema[name];
        scope.modelRefs = modelsBySchema[name];
        scope.hasReferrers = scope.referrers != null && !scope.referrers.isEmpty();
        scope.hasParamRefs = scope.paramRefs != null && !scope.paramRefs.isEmpty();
        scope.hasModelRefs = scope.modelRefs != null && !scope.modelRefs.isEmpty();
        
        // put properties in array form, mustache doesn't play nice with associative arrays
        def props = [];
        model.getProperties().each { property -> 
            def prop = [:];
            prop.name = property.key;
            prop.type = property.value.type;
            prop.title = property.value.title;
            prop.description = property.value.description;
            prop.access = property.value.access;
            prop.readOnly = property.value.readOnly;
            prop.required = property.value.required;
            prop.position = property.value.position;
            if ( property.value instanceof RefProperty ) {
                prop.ref = property.value.ref;
                prop.ref_lc = property.value.ref.toLowerCase();
            }
            props.add(prop);   
        };
        scope.name = name;
        scope.name_lc = name.toLowerCase();
        scope.description = model.description;
        scope.properties = props;
       
        modelTemplate.execute(writer, scope);
        writer.flush(); 
    }
    
    def generateFileStart() {
        def scope = [:];
        def tags = [];
        allTags.each{ tag -> 
            def atag = [:];
            atag.name = tag;
            atag.link = tag.toLowerCase();
            tags.add(atag);
        };
        scope.tags = tags;
        fileStartTemplate.execute(writer, scope);
        writer.flush();
    }
    
    def generateMethodsSectionTitle() {
        writer.println "";
        writer.println "## Methods";
        writer.println "";
    }
    
    def generateMethodsTitle(String tag) {
        writer.println "";
        writer.println "### ${tag}";
        writer.println "";
    }
    
    def generateModelsTitle() {
        writer.println "\n## Models";
        writer.println "This section lists the properties for the Usergrid Default Entities:";
    }

    def generateSubTypesTitle() {
        writer.println "\n## Sub-Types";
        writer.println "This section lists the properties for sub-types used in Usergrid Default Entities.";
    }
    
    def generateFileEnd() {
        writer.println "";
        writer.println "Generated from the Usergrid Swagger definition.";
        writer.println ""; 
    }
}

/**
 * Generates Usergrid API docs from Swagger in HTML format.
 */
class HtmlApiDocGenerator extends ApiDocGenerator {
    
    public HtmlApiDocGenerator() {
        writer = new OutputStreamWriter(
                new FileOutputStream("rest-endpoints/api-docs.html"));
        operationTemplate = mf.compile(
                new FileReader("${mustacheBase}/html/operation.mustache"), "operation");
        modelTemplate = mf.compile(
                new FileReader("${mustacheBase}/html/model.mustache"), "operation");
        fileStartTemplate = mf.compile(
                new FileReader("${mustacheBase}/html/file-start.mustache"), "file-start");
        fileEndTemplate = mf.compile(
                new FileReader("${mustacheBase}/html/file-end.mustache"), "file-end");
    }

    def generateFileStart() {
        def scope = [:];
        scope.title = "Usergrid API Reference";
        fileStartTemplate.execute(writer, scope);
        writer.flush();
    }

    def generateMethodsSectionTitle() {
        writer.println "<h2>Methods</h2>" +
                "<p>API methods are organized by the tags.</p>";
        writer.println "<p>Following the methods is a listing of all " +
                "<a href='#models'>Default Entity Models.</p>"
    }

    def generateMethodsTitle(String tag) {
        writer.println "<a name='${tag}-method'></a>";
        writer.println "<h2>${tag}</h2>";
    }

    def generateModelsTitle() {
        writer.println "<a name='models'></a>";
        writer.println "<br><h1>Default Entity Models</h1>" +
                "<p>This section lists the properties for the following Usergrid Default Entities.</p>";
    }

    def generateSubTypesTitle() {
        writer.println "<a name='subtypes'></a>";
        writer.println "<br><h1>Sub-types Referenced By Models</h1>" +
                "<p>This section lists the properties for sub-types used in Usergrid Default Entities.</p>";
    }
    
    def generateFileEnd() {
        def scope = [:];
        fileEndTemplate.execute(writer, scope);
        writer.flush();
    }
}

