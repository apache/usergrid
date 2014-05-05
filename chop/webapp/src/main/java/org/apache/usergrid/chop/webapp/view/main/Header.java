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

import com.vaadin.ui.*;
import com.vaadin.ui.themes.Reindeer;
import org.apache.usergrid.chop.api.Module;
import org.apache.usergrid.chop.webapp.dao.ModuleDao;
import org.apache.usergrid.chop.webapp.service.InjectorFactory;
import org.apache.usergrid.chop.webapp.view.runner.RunnersWindow;
import org.apache.usergrid.chop.webapp.view.user.UserSubwindow;
import org.apache.usergrid.chop.webapp.view.util.UIUtil;

public class Header extends AbsoluteLayout {

    private ModuleDao moduleDao = InjectorFactory.getInstance(ModuleDao.class);

    private Label moduleLabel = UIUtil.addLabel(this, "", "left: 10px; top: 10px;", "500px");

    public Header() {
        addRunnersButton();
        addManageButton();
    }

    private void addRunnersButton() {

        Button button = UIUtil.addButton(this, "Runners", "left: 840px; top: 10px;", "80px");
        button.setStyleName(Reindeer.BUTTON_LINK);

        button.addClickListener(new Button.ClickListener() {
            public void buttonClick(Button.ClickEvent event) {
                showWindow(new RunnersWindow());
            }
        });
    }

    private void addManageButton() {

        Button button = UIUtil.addButton(this, "Manage", "left: 940px; top: 10px;", "80px");
        button.setStyleName(Reindeer.BUTTON_LINK);

        button.addClickListener(new Button.ClickListener() {
            public void buttonClick(Button.ClickEvent event) {
                showWindow(new UserSubwindow());
            }
        });
    }

    private static void showWindow(Window window) {
        UI.getCurrent().addWindow(window);
    }

    void showModule(String moduleId) {

        Module module = moduleDao.get(moduleId);
        String caption = String.format(
                "<b>%s / %s / %s</b>",
                module.getGroupId(),
                module.getArtifactId(),
                module.getVersion()
        );

        moduleLabel.setValue(caption);
    }
}
