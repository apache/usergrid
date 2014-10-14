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
package org.apache.usergrid.chop.webapp.view.main;


import com.vaadin.ui.AbsoluteLayout;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.VerticalLayout;


public class TabSheetManager {

    private final TabSheet tabSheet;

    TabSheetManager( TabSheet tabSheet ) {
        this.tabSheet = tabSheet;
    }

    public void addTab( AbsoluteLayout layout, String caption ) {
        removeAll();
        tabSheet.addTab( layout, caption );
    }

    public void addTabWithVerticalLayout( VerticalLayout layout, String caption ) {
        removeAll();
        tabSheet.setSizeFull();
        tabSheet.addTab( layout, caption );
    }

    public void removeAll() {
        // BUG: Showing two charts doesn't work, thus we have to close others to display a new one.
        tabSheet.removeAllComponents();
    }
}
