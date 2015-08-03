/*
 * Generates API docs from Swagger.
 */

// Depdency management with Grape:
//    http://docs.groovy-lang.org/latest/html/documentation/grape.html

import io.swagger.models.parameters.RefParameter
@Grab(group = 'io.swagger', module = 'swagger-parser', version = '1.0.8')
@Grab(group = 'io.swagger', module = 'swagger-compat-spec-parser', version = '1.0.8')
@Grab(group = 'com.github.spullara.mustache.java', module = 'compiler', version = '0.8.18-SNAPSHOT')

import io.swagger.parser.*;
import io.swagger.models.*;
import com.github.mustachejava.*;


public class ApiDocGenerator {
    
    def parser = new SwaggerParser();
    def swagger = parser.read("src/main/resources/usergrid-swagger.yaml");
   
    // Mustache reference: http://mustache.github.io/mustache.5.html
    // mustache.java info: https://github.com/spullara/mustache.java
    def writer = new OutputStreamWriter(new FileOutputStream("rest-endpoints/api-docs.md"));
    def mf = new DefaultMustacheFactory();

    def mustacheBase = "src/main/resources";
    def operationTemplate = mf.compile(new FileReader("${mustacheBase}/operation.mustache"), "operation");
    def modelTemplate = mf.compile(new FileReader("${mustacheBase}/model.mustache"), "operation");
    

    public static void main( String[] args ) {
        def adg = new ApiDocGenerator();
        adg.generate();
    }
    
    def generate() {

        // organize methods by tag
        def tagsToUrlOps = [:]
        
        swagger.getPaths().each { pathEntry ->
            def url = pathEntry.key;
            def path = pathEntry.value;
           
            def tag; // assume each opeation has only one tag
            def method;
            def operation;
            
            if (path.get != null) {
                tag = path.get.tags[0];
                method = "GET";
                operation = path.get;
            }
            if (path.post != null) {
                tag = path.post.tags[0];
                method = "POST";
                operation = path.post;
            }
            if (path.delete != null) {
                tag = path.delete.tags[0];
                method = "DELETE";
                operation = path.delete;
            }
            if (path.put != null) {
                tag = path.put.tags[0];
                method = "PUT";
                operation = path.put;
            }

            def urlOps = tagsToUrlOps[tag];
            if ( urlOps == null ) {
                urlOps = [];
                tagsToUrlOps[tag] = urlOps;
            }
            def urlOp = [:];
            urlOp.url = url;
            urlOp.method = method;
            urlOp.operation = operation;
            urlOps.add(urlOp); 
        }

        writer.println "\n# Methods";
        
        tagsToUrlOps.each { entry -> 
            def tag = entry.key;
            def urlOps = entry.value;
//            writer.println "\n<div class='hr'/>";
            writer.println "\n## ${tag} Methods";
            urlOps.each { urlOp ->
                formatOperation( urlOp );
            };
        };

        writer.println "\n# Models";
        
        swagger.getDefinitions().each { definitionEntry -> 
            def name = definitionEntry.key;
            def model = definitionEntry.value;
            formatModel( name, model );
        };
    }

    def formatOperation( urlOp ) {

        def url = urlOp.url;
        def method = urlOp.method;
        def op = urlOp.operation;
        
        // put responses in array form, mustache doesn't play nice with associative arrays
        def responses = [];
        op.getResponses().each { responseEntry -> 
            def response = [:];
            response.status = responseEntry.key;
            response.description = responseEntry.value.description;
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
        modelTemplate.execute(writer, scope);
        writer.flush(); 
    }
}

