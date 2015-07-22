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

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.impl.DefaultPrettyPrinter;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.utils.ConversionUtils;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;


/**
 * Base class for ToolBase implementations that write output to an output directory and file.
 *
 * @author zznate
 */
public abstract class ExportingToolBase extends ToolBase {

    protected Logger logger = LoggerFactory.getLogger( ExportingToolBase.class );

    protected static File outputDir;

    /** Output dir option: -outputDir */
    protected static final String OUTPUT_DIR = "outputDir";

    protected String baseOutputDirName = "export";
    protected UUID orgId;
    JsonFactory jsonFactory = new JsonFactory();
    protected long startTime = System.currentTimeMillis();


    @Override
    @SuppressWarnings("static-access")
    public Options createOptions() {

        Options options = super.createOptions();

        Option outputDir = OptionBuilder.hasArg().withDescription( "output file name -outputDir" ).create( OUTPUT_DIR );
        Option orgId = OptionBuilder.hasArg().withDescription( "Use a specific organization -orgId" ).create( "orgId" );

        options.addOption( outputDir );
        options.addOption( orgId );

        return options;
    }


    protected void prepareBaseOutputFileName( CommandLine line ) {

        boolean hasOutputDir = line.hasOption( OUTPUT_DIR );

        if ( hasOutputDir ) {
            baseOutputDirName = line.getOptionValue( OUTPUT_DIR );
        }
    }


    protected void applyOrgId( CommandLine line ) {
        if ( line.hasOption( "orgId" ) ) {
            orgId = ConversionUtils.uuid( line.getOptionValue( "orgId" ) );
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


    protected File createCollectionsDir( String applicationName ) {
        return createDir( String.format( "%s/%s.applicationName.collections", outputDir, applicationName ) );
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


    protected JsonGenerator getJsonGenerator( String outFile ) throws IOException {
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
