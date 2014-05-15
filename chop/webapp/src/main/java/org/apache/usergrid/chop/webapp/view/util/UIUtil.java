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

import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;

public class UIUtil {

    public static ComboBox createCombo( String caption, Object values[] ) {

        ComboBox combo = new ComboBox( caption );
        combo.setTextInputAllowed( false );
        combo.setNullSelectionAllowed( false );

        populateCombo( combo, values );

        return combo;
    }





    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static ComboBox addCombo(AbsoluteLayout layout, String caption, String position, String width, Object values[]) {

        ComboBox combo = new ComboBox(caption);
        combo.setTextInputAllowed(false);
        combo.setNullSelectionAllowed(false);
        combo.setWidth(width);

        layout.addComponent(combo, position);
        populateCombo(combo, values);

        return combo;
    }


    public static void populateCombo(ComboBox combo, Object values[]) {

        if (values == null || values.length == 0) {
            return;
        }

        for (Object value : values) {
            combo.addItem(value);
        }

        combo.select(values[0]);
    }

    public static void select(ComboBox combo, Object value) {
        if (value != null) {
            combo.select(value);
        }
    }

    public static Button addButton(AbsoluteLayout layout, String caption, String position, String width) {

        Button button = new Button(caption);
        button.setWidth( width );
        layout.addComponent(button, position);

        return button;
    }

    public static AbsoluteLayout addLayout(AbsoluteLayout parent, String id, String position, String width, String height) {

        AbsoluteLayout layout = new AbsoluteLayout();
        layout.setId(id);
        layout.setWidth(width);
        layout.setHeight(height);

        parent.addComponent(layout, position);

        return layout;
    }

    public static Label addLabel(AbsoluteLayout parent, String text, String position, String width) {

        Label label = new Label(text, ContentMode.HTML);
        label.setWidth(width);

        parent.addComponent(label, position);

        return label;
    }

    public static ListSelect addListSelect(AbsoluteLayout parent, String caption, String position, String width) {

        ListSelect list = new ListSelect(caption);
        list.setWidth(width);
        list.setNullSelectionAllowed(false);
        list.setImmediate(true);

        parent.addComponent(list, position);

        return list;
    }

    public static TextArea addTextArea(AbsoluteLayout parent, String caption, String position, String width, String height, boolean readOnly) {

        TextArea textArea = new TextArea(caption);
        textArea.setWidth(width);
        textArea.setHeight(height);
        textArea.setWordwrap(false);
        textArea.setReadOnly(readOnly);

        parent.addComponent(textArea, position);

        return textArea;
    }

    public static TextArea addTextArea(AbsoluteLayout parent, String caption, String position, String width, String height) {
        return addTextArea(parent, caption, position, width, height, false);
    }

}
