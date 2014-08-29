/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.chop.webapp.view.chart.layout.item;


import com.vaadin.ui.Table;
import org.apache.usergrid.chop.webapp.service.util.JsonUtil;
import org.json.JSONObject;
import java.text.DecimalFormat;


public class DetailsTable extends Table {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat( "#" );


    public DetailsTable() {
        setWidth( "250px" );
        setHeight( "350px" );
        addContainerProperty( "Details", String.class, null );
        addContainerProperty( "Value", String.class, null );
    }


    public void setContent( JSONObject json ) {
        removeAllItems();
        addValues( json );
    }


    private void addValues( JSONObject json ) {
        for ( String key : JsonUtil.getKeys( json ) ) {
            if ( !"id".equals( key ) ) {
                addItem( new Object[] { key, getValue( json, key ) }, key );
            }
        }
    }


    private String getValue( JSONObject json, String key ) {

        Object value = json.opt( key );

        return value instanceof Double ? DECIMAL_FORMAT.format( value ) : json.optString( key );
    }
}
