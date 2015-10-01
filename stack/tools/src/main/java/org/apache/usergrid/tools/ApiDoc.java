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
import java.util.Map;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.persistence.Schema;
import org.apache.usergrid.persistence.entities.Application;
import org.apache.usergrid.persistence.schema.CollectionInfo;
import org.apache.usergrid.tools.apidoc.swagger.ApiListing;
import org.apache.usergrid.utils.JsonUtils;
import org.w3c.dom.Document;
import org.yaml.snakeyaml.Loader;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.BaseConstructor;
import org.yaml.snakeyaml.constructor.Constructor;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;

import static org.apache.usergrid.utils.StringUtils.readClasspathFileAsString;


public class ApiDoc extends ToolBase {

    private static final Logger logger = LoggerFactory.getLogger( ApiDoc.class );


    @Override
    public Options createOptions() {

        Option generateWadl = OptionBuilder.create( "wadl" );

        Options options = new Options();
        options.addOption( generateWadl );

        return options;
    }


    @Override
    public void runTool( CommandLine line ) throws Exception {
        logger.info( "Generating applications docs..." );

        ApiListing listing = loadListing( "applications" );
        output( listing, "applications" );

        logger.info( "Generating management docs..." );

        listing = loadListing( "management" );
        output( listing, "management" );

        logger.info( "Done!" );
    }


    public ApiListing loadListing( String section ) {
        Yaml yaml = new Yaml( new Loader());
        //TODO: fix line above
        String yamlString = readClasspathFileAsString( "/apidoc/" + section + ".yaml" );
        ApiListing listing = ( ApiListing ) yaml.load( yamlString );
        return listing;
    }


    public void output( ApiListing listing, String section ) throws IOException, TransformerException {
        Document doc = listing.createWADLApplication();

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setAttribute( "indent-number", 4 );
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty( OutputKeys.INDENT, "yes" );
        transformer.setOutputProperty( "{http://xml.apache.org/xslt}indent-amount", "4" );

        DOMSource source = new DOMSource( doc );
        StreamResult result = new StreamResult( new File( section + ".wadl" ) );
        transformer.transform( source, result );

        File file = new File( section + ".json" );
        listing.setBasePath( "${basePath}" );
        FileUtils.write( file, JsonUtils.mapToFormattedJsonString( listing ) );
    }


    public void addCollections( ApiListing listing ) {
        Map<String, CollectionInfo> collections = Schema.getDefaultSchema().getCollections( Application.ENTITY_TYPE );
        collections.clear();
    }
}
