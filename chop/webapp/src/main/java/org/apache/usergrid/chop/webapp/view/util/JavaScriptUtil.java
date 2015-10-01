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
package org.apache.usergrid.chop.webapp.view.util;

import com.vaadin.ui.JavaScript;
import com.vaadin.ui.JavaScriptFunction;
import org.apache.usergrid.chop.webapp.service.util.FileUtil;

public class JavaScriptUtil {

    private static void execute(String script) {
        JavaScript.getCurrent().execute(script);
    }

    private static void addCallback(String jsCallbackName, JavaScriptFunction jsCallback) {
        JavaScript.getCurrent().addFunction(jsCallbackName, jsCallback);
    }

    public static void loadFile(String fileName) {
        execute(FileUtil.getContent(fileName));
    }

    public static void loadChart(String chart, String jsCallbackName, JavaScriptFunction jsCallback) {
        execute(chart);
        addCallback(jsCallbackName, jsCallback);
    }
}
