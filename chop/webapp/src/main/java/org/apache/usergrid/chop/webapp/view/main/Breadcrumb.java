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
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.themes.Reindeer;
import org.apache.usergrid.chop.webapp.view.chart.layout.ChartLayout;
import org.apache.usergrid.chop.webapp.view.util.UIUtil;

public class Breadcrumb extends AbsoluteLayout {

    private AbstractComponent items[] = new AbstractComponent[3];
    private ChartLayout chartLayouts[] = new ChartLayout[3];
    private MainView mainView;

    public Breadcrumb(MainView mainView) {
        this.mainView = mainView;
        initItems();
    }

    private void initItems() {
        items[0] = addButton(0, 10, "80px");
        items[1] = addButton(1, 100, "110px");
        items[2] = addLabel(240, "80px");
    }

    private Label addLabel(int left, String width) {
        String position = String.format("left: %spx; top: 10px;", left);
        return UIUtil.addLabel(this, "", position, width);
    }

    private Button addButton(final int pos, int left, String width) {

        String position = String.format("left: %spx; top: 10px;", left);

        Button button = UIUtil.addButton(this, "", position, width);
        button.setStyleName(Reindeer.BUTTON_LINK);

        button.addClickListener(new Button.ClickListener() {
            public void buttonClick(Button.ClickEvent event) {
                buttonClicked(pos);
            }
        });

        return button;
    }

    private void buttonClicked(int pos) {
        mainView.show(chartLayouts[pos]);
        hideItems(pos);
    }

    public void setItem(ChartLayout chartLayout, String caption, int pos) {

        setCaption(caption, pos);
        items[pos].setVisible(true);
        chartLayouts[pos] = chartLayout;

        hideItems(pos);
    }

    private void setCaption(String caption, int pos) {
        if (pos == 2) {
            ((Label) items[pos]).setValue(String.format("<b>%s</b>", caption));
        } else {
            items[pos].setCaption(caption);
        }
    }

    private void hideItems(int pos) {
        for (int i = pos + 1; i < items.length; i++) {
            items[i].setVisible(false);
        }
    }
}
