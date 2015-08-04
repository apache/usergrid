/*
 * Generates Markdown representation of 
 */

// Depdency management with Grape:
//    http://docs.groovy-lang.org/latest/html/documentation/grape.html

package usergrid;

@Grab(group = 'io.swagger', module = 'swagger-parser', version = '1.0.8')
@Grab(group = 'io.swagger', module = 'swagger-compat-spec-parser', version = '1.0.8')
@Grab(group = 'com.github.spullara.mustache.java', module = 'compiler', version = '0.8.18-SNAPSHOT')

import io.swagger.parser.*;
import io.swagger.models.*;
import com.github.mustachejava.*
import org.apache.commons.lang3.RandomStringUtils;


public class ApiDocGenerator {
    
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


    public ApiDocGenerator() {
        writer = new OutputStreamWriter(new FileOutputStream("rest-endpoints/api-docs.md"));
        operationTemplate = mf.compile(
                new FileReader("${mustacheBase}/operation.mustache"), "operation");
        modelTemplate = mf.compile(
                new FileReader("${mustacheBase}/model.mustache"), "model");
    }
    
    public static void main( String[] args ) {
        
        def htmlgen = new HtmlApiDocGenerator();
        htmlgen.generate();
        
        def mdgen = new ApiDocGenerator();
        mdgen.generate();
    }
    
    def generate() {
       
        // build up scope and generate via Mustache template
        
        // organize methods by tag
        def tagsToUrlOps = new TreeMap();
        
        swagger.getPaths().each { pathEntry ->
            def url = pathEntry.key;
            def path = pathEntry.value;
           
            if (path.get != null) {
                addOperation(tagsToUrlOps, "GET", url, path.get);
            }
            if (path.post != null) {
                addOperation(tagsToUrlOps, "POST", url, path.post);
            }
            if (path.put != null) {
                addOperation(tagsToUrlOps, "PUT", url, path.put);
            }
            if (path.delete != null) {
                addOperation(tagsToUrlOps, "DELETE", url, path.delete);
            }
        };

        tagsToUrlOps.each { entry -> allTags.add(entry.key) };

        def definitions = new TreeMap();
        swagger.getDefinitions().each { entry ->
            definitions.put( entry.key, entry.value );
        };
        definitions.each { entry -> allModels.add(entry.key); };
       
        // generate
        
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
        generateFileEnd();
    }
    
    def addOperation( Map tagsToUrlOps, String method, String url, Operation operation ) {
       
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
        urlOps.add(urlOp);
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
            }
            responses.add(response);
        }

        def params = [];
        op.getParameters().each { parameter -> 
            def param = [:];
            param.name = parameter.name;
            param.required = parameter.required;
            param.description = parameter.description;
            param.in = parameter.in;
            
            // assume that body parameters have a schema that is a reference
            if (parameter.in == "body" && parameter.schema != null) {
                param.schemaRef = parameter.schema.ref;
                param.schemaAnchor = parameter.schema.ref.toLowerCase();
            } else if (parameter.in == "path") {
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
        scope.opid = RandomStringUtils.randomAlphanumeric(10);
       
        operationTemplate.execute(writer, scope);
        writer.flush();
    }
    
    def formatModel(String name, Model model) {

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
            props.add(prop);   
        };
        def scope = [:];
        scope.name = name;
        scope.properties = props;
        scope.modelid = RandomStringUtils.randomAlphanumeric(10);
        modelTemplate.execute(writer, scope);
        writer.flush(); 
    }
    
    def generateFileStart() {
        // no op
    }
    
    def generateMethodsSectionTitle() {
        writer.println "## Methods";
    }
    
    def generateMethodsTitle(String tag) {
        writer.println "## ${tag} Methods>";
    }
    
    def generateModelsTitle() {
        writer.println "\n## Models";
        writer.println "Properties for Usergrid default entities.";
    }
    
    def generateFileEnd() {
        // no op
    }
}


class HtmlApiDocGenerator extends ApiDocGenerator {
    
    public HtmlApiDocGenerator() {
        writer = new OutputStreamWriter(
                new FileOutputStream("rest-endpoints/api-docs.html"));
        operationTemplate = mf.compile(
                new FileReader("${mustacheBase}/operation-html.mustache"), "operation");
        modelTemplate = mf.compile(
                new FileReader("${mustacheBase}/model-html.mustache"), "operation");
        fileStartTemplate = mf.compile(
                new FileReader("${mustacheBase}/file-start-html.mustache"), "file-start");
        fileEndTemplate = mf.compile(
                new FileReader("${mustacheBase}/file-end-html.mustache"), "file-end");
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
        writer.println "<h2>${tag} Methods</h2>";
    }

    def generateModelsTitle() {
        writer.println "<a name='models'></a>";
        writer.println "<h2>Default Entity Models</h2>" +
                "<p>This section lists the properties for the following Usergrid Default Entities:</p>";
    }

    def generateFileEnd() {
        def scope = [:];
        fileEndTemplate.execute(writer, scope);
        writer.flush();
    }
}

