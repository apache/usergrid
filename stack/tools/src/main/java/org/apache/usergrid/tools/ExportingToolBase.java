/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.tools;


import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.persistence.graph.impl.SimpleEdge;
import org.apache.usergrid.services.assets.BinaryStoreFactory.Provider;
import org.apache.usergrid.utils.ConversionUtils;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;


/**
 * Base class for ToolBase implementations that write output to an output directory and file.
 *
 */
public abstract class ExportingToolBase extends ToolBase {

    protected Logger logger = LoggerFactory.getLogger( ExportingToolBase.class );

    protected static File outputDir;

    /** Output dir option: -outputDir */
    protected static final String OUTPUT_DIR = "outputDir";
    protected static final String ORG_ID = "orgid";
    protected static final String ORG_NAME = "orgName";
    protected static final String APP_ID = "appId";
    protected static final String APP_NAME = "appName";
    protected static final String COLL_NAMES = "collNames";
    protected static final String APPEND_TIMESTAMP = "appendTimestamp";
    protected static final String SKIP_CONN = "skipConnections";
    protected static final String SKIP_DICT = "skipDictionaries";
    protected static final String LAST_EDGE = "lastEdge";
    protected static final String SKIP_ASSETS = "skipAssets";
    protected static final String FIELD_TYPE = "fieldType";
    protected static final String COLLECTION_NAME = "collectionName";

    protected String baseOutputDirName = "export";
    protected UUID orgId;
    protected String orgName;
    protected UUID applicationId;
    protected String applicationName;
    protected String[] collNames;
    protected String fieldType;
    protected boolean skipConnections = false;
    protected boolean skipDictionaries = false;
    protected boolean skipAssets = false;
    protected String lastEdgeJson = null;
    
    JsonFactory jsonFactory = new JsonFactory();
    protected long startTime = System.currentTimeMillis();


    @Override
    @SuppressWarnings("static-access")
    public Options createOptions() {

        Options options = super.createOptions();

        Option outputDir = OptionBuilder.hasArg().withDescription( "Output file name -outputDir" ).create( OUTPUT_DIR );
        Option orgId = OptionBuilder.hasArg().withDescription( "Use a specific organization -orgId" ).create( ORG_ID );
        Option appId = OptionBuilder.hasArg().withDescription( "Use a specific application -appId (Needs -orgId or -orgName)" ).create( APP_ID );
        Option orgName = OptionBuilder.hasArg().withDescription( "Use a specific organization name -orgName" ).create( ORG_NAME );
        Option appName = OptionBuilder.hasArg().withDescription( "Use a specific application name -appName (Needs -orgId or -orgName)" ).create( APP_NAME );
        Option collNames = OptionBuilder.hasArg().withDescription( "Export list of comma separated collections -collNames (Needs -orgId or -orgName and -appId or -appName)" ).create( COLL_NAMES );
        Option appendTimestamp = OptionBuilder.withDescription( "Attach timestamp to output directory -appendTimestamp" ).create( APPEND_TIMESTAMP );
        Option skipConns = OptionBuilder.withDescription( "Skip exporting connections for entities -skipConnections" ).create( SKIP_CONN );
        Option skipDicts = OptionBuilder.withDescription( "Skip exporting dictionaries for entities -skipDictionaries" ).create( SKIP_DICT );
        Option lastEdge = OptionBuilder.hasArg().withDescription( "Last Edge from previous run to resume export -lastEdge" ).create( LAST_EDGE );
        Option skipAssets = OptionBuilder.withDescription( "Skip exporting assets for entities -skipAssets" ).create( SKIP_ASSETS );
        Option awsKey =  OptionBuilder.hasArg().withDescription( "AWS access key -awsKey" ).create( AWS_KEY );
        Option storeType = OptionBuilder.hasArg().withDescription( "Binary store type -storeType (aws, google, local)" ).create( STORE_TYPE );
        Option awsId = OptionBuilder.hasArg().withDescription( "AWS access id -awsId" ).create( AWS_ID );
        Option bucketName = OptionBuilder.hasArg().withDescription( "Binary storage bucket name -bucketName" ).create( BINARY_BUCKET_NAME );
        Option fieldType = OptionBuilder.hasArg().withDescription( "Field type for unique value check -fieldType" ).create( FIELD_TYPE );
        Option collectionName = OptionBuilder.hasArg().withDescription( "Collection name for unique value check -collectionName" ).create( COLLECTION_NAME );
        
        options.addOption( outputDir );
        options.addOption( orgId );
        options.addOption( appId );
        options.addOption( collNames );
        options.addOption( appName );
        options.addOption( orgName );
        options.addOption( appendTimestamp );
        options.addOption( skipConns );
        options.addOption( skipDicts );
        options.addOption( lastEdge );
        options.addOption( skipAssets );
        options.addOption( awsKey );
        options.addOption( awsId );
        options.addOption( bucketName );
        options.addOption( storeType );
        options.addOption(fieldType);
        options.addOption(collectionName);

        return options;
    }


    protected void prepareBaseOutputFileName( CommandLine line ) {

        boolean hasOutputDir = line.hasOption( OUTPUT_DIR );
        boolean appendTimestamp = line.hasOption( APPEND_TIMESTAMP );

        if ( hasOutputDir ) {
            baseOutputDirName = line.getOptionValue( OUTPUT_DIR );
        }
        
        if(appendTimestamp) {
        	baseOutputDirName = baseOutputDirName + "_"+startTime;
        }
    }


    protected void applyOrgId( CommandLine line ) {
        if ( line.hasOption( "orgId" ) ) {
            orgId = ConversionUtils.uuid( line.getOptionValue( "orgId" ) );
        }
    }
    
    
    protected void applyExportParams( CommandLine line ) {
    	
        if ( line.hasOption( ORG_ID ) ) {
            orgId = ConversionUtils.uuid( line.getOptionValue( ORG_ID ) );
        }
        else if ( line.hasOption( ORG_NAME ) ) {
            orgName = line.getOptionValue( ORG_NAME ) ;
        }
        
        if ( line.hasOption( APP_ID ) ) {
            applicationId = ConversionUtils.uuid( line.getOptionValue( APP_ID ) );
        }
        else if ( line.hasOption( APP_NAME ) ) {
            applicationName = line.getOptionValue( APP_NAME ) ;
        }
        if ( line.hasOption( COLL_NAMES ) ) {
            collNames = line.getOptionValue( COLL_NAMES ).split(",");
        }
        if(line.hasOption( COLLECTION_NAME )) {
        	collNames = new String[] {line.getOptionValue( COLLECTION_NAME )};
        }
        skipConnections = line.hasOption( SKIP_CONN );
        skipDictionaries = line.hasOption( SKIP_DICT );
        
        if(line.hasOption(LAST_EDGE)) {
        	lastEdgeJson = line.getOptionValue(LAST_EDGE);
        }
        
        skipAssets = line.hasOption( SKIP_ASSETS );
        
        if(line.hasOption( FIELD_TYPE )) {
        	fieldType = line.getOptionValue( FIELD_TYPE );
        }
    }
    
	protected void validateOptions(CommandLine line) throws MissingOptionException {
		if ((line.hasOption(APP_ID) || line.hasOption(APP_NAME))
				&& !(line.hasOption(ORG_ID) || line.hasOption(ORG_NAME))) {
			throw new MissingOptionException("-orgId or -orgName is required if you pass -appId or -appName");
		}
		if (line.hasOption(COLL_NAMES) && !(line.hasOption(APP_ID) || line.hasOption(APP_NAME))) {
			throw new MissingOptionException(
					"[-appId or -appName] and [-orgId or -orgName] are required if you pass -collNames");
		}

		if (!line.hasOption(SKIP_ASSETS)) {
			if (line.hasOption(STORE_TYPE)) {
				String storeType = line.getOptionValue(STORE_TYPE);
				if (storeType.equals(Provider.aws.toString())) {
					if (!line.hasOption(AWS_ID) || !line.hasOption(AWS_KEY) || !line.hasOption(BINARY_BUCKET_NAME)) {
						throw new MissingOptionException(
								"[-awsId and -awsKey and -bucketName] are required if you pass -storeType as aws");
					}
				} else if (storeType.equals(Provider.google.toString())) {
					if (!line.hasOption(BINARY_BUCKET_NAME)) {
						throw new MissingOptionException("[-bucketName] is required if you pass -storeType as google");
					}
				}
			} else {
				throw new MissingOptionException("[-storeType] is required if you do not pass -skipAssets");
			}
		}
	}


    /**
     * Write the string onto the writer and check if verbose is enabled to log also an echo of what is being written to
     * the writer.
     *
     * @param out PrintWriter
     * @param content string to be written
     */
    @SuppressWarnings("unused")
    protected void writeOutput( PrintWriter out, String content ) {
        echo( content );
        out.print( content );
    }


    protected File createOutputParentDir() {
        return createDir( baseOutputDirName );
    }


    protected File createOutputFile( String type, String name ) {
        return new File( outputDir, prepareOutputFileName( type, name ) );
    }


    protected File createOutputFile( File parent, String type, String name ) {
        return new File( parent, prepareOutputFileName( type, name ) );
    }
    
    protected File createOrgDir( String orgName ) {
        return createDir( outputDir.getAbsolutePath(), orgName);
    }
    
    protected File createApplicationDir( File orgDir, String applicationName ) {
        return createDir( orgDir.getAbsolutePath(), applicationName);
    }


    protected File createCollectionDir( File appDir, String collectionName ) {
        return createDir( appDir.getAbsolutePath(), collectionName) ;
    }
    
    
    protected File createDir( String baseDir, String dirName ) {
        
        return createDir((baseDir!=null && !baseDir.trim().equals(""))?(baseDir + File.separator +dirName): dirName);
    }


    protected File createDir( String dirName ) {
        File file = new File( dirName );

        if ( file.exists() ) {
            if ( file.isDirectory() ) {
                return file;
            }
            else {
                throw new RuntimeException(
                        String.format( "Unable to create directory %s.  It already exists as a file", dirName ) );
            }
        }

        if ( !file.mkdirs() ) {

            throw new RuntimeException( String.format( "Unable to create directory %s", dirName ) );
        }

        return file;
    }


    /**
     * @param type just a label such us: organization, application.
     *
     * @return the file name concatenated with the type and the name of the collection
     */
    protected String prepareOutputFileName( String type, String name ) {
        name = name.replace( "/", PATH_REPLACEMENT );
        // Add application and timestamp
        StringBuilder str = new StringBuilder();
        // str.append(baseOutputFileName);
        // str.append(".");
        str.append( type );
        str.append( "." );
        str.append( name );
        str.append( "." );
        str.append( startTime );
        str.append( ".json" );

        String outputFileName = str.toString();

        logger.info( "Creating output filename:" + outputFileName );

        return outputFileName;
    }


    protected JsonGenerator getJsonGenerator(String outFile ) throws IOException {
        return getJsonGenerator( new File( outputDir, outFile ) );
    }


    protected JsonGenerator getJsonGenerator( File outFile ) throws IOException {
        PrintWriter out = new PrintWriter( outFile, "UTF-8" );
        JsonGenerator jg = jsonFactory.createJsonGenerator( out );
        jg.setPrettyPrinter( new DefaultPrettyPrinter() );
        jg.setCodec( new ObjectMapper() );
        return jg;
    }
}
