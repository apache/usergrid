package org.apache.usergrid.chop.webapp.view.main;

import com.vaadin.annotations.PreserveOnRefresh;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.AbsoluteLayout;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import org.apache.commons.lang.StringUtils;
import org.apache.usergrid.chop.webapp.service.shiro.ShiroRealm;
import org.apache.usergrid.chop.webapp.view.util.JavaScriptUtil;


@PreserveOnRefresh
public class Login extends UI {

    private final Label title = new Label ( "<h3>Login</h3>", ContentMode.HTML );
    private final TextField usernameField = new TextField( "Username:" );
    private final PasswordField passwordField = new PasswordField( "Password:" );
    private final Button loginButton = new Button( "Login" );

    VerticalLayout mainLayout;
    MainView mainView = new MainView();

    @Override
    protected void init(VaadinRequest vaadinRequest) {
        mainLayout  = new VerticalLayout();
        mainLayout.setSizeFull();
        setContent( mainLayout );

        addItems();
        loadScripts();
    }


    private void loadScripts() {
        JavaScriptUtil.loadFile("js/jquery.min.js");
        JavaScriptUtil.loadFile( "js/jquery.flot.min.js" );
    }

    private void addItems() {
        // Set default values
        FormLayout formLayout = addFormLayout();
        formLayout.addComponent( title );
        formLayout.addComponent( usernameField );
        formLayout.addComponent( passwordField );
        formLayout.addComponent( loginButton );
        formLayout.addComponent( addButtonLayout() );
    }

    private FormLayout addFormLayout() {

        FormLayout formLayout = new FormLayout();
        formLayout.setWidth( "300px" );
        formLayout.setHeight( "200px" );
        formLayout.addStyleName( "outlined" );
        formLayout.setSpacing( true );
        mainLayout.addComponent( formLayout );
        mainLayout.setComponentAlignment( formLayout, Alignment.MIDDLE_CENTER );
        return formLayout;
    }


    private AbsoluteLayout addButtonLayout() {

        AbsoluteLayout layout = new AbsoluteLayout();
        layout.setWidth( "100%" );
        layout.setHeight( "50px" );

        layout.addComponent( loginButton, "left: 0px; top: 20px;" );
        loginButton.addClickListener( new Button.ClickListener() {
            public void buttonClick( Button.ClickEvent event ) {
                loginButtonClicked();
            }
        } );
        return layout;
    }

    private void loginButtonClicked() {

        String username = usernameField.getValue();
        String password = passwordField.getValue();

        if ( StringUtils.isEmpty(username) || StringUtils.isEmpty( password ) ) {
            Notification.show( "Error", "Please enter username and password", Notification.Type.ERROR_MESSAGE );
            return;
        }
        try {
            if ( authUser(username, password) ){
                redirectToMainView();
            }
            else{
                Notification.show( "Error", "Check your password and username", Notification.Type.HUMANIZED_MESSAGE );
            }
        } catch ( Exception e ) {
            Notification.show( "Error", "Check your password and username: " + e.getMessage(), Notification.Type.ERROR_MESSAGE );
        }
    }

    private boolean authUser( String username, String password ){
        return ShiroRealm.authenticateUser( username, password );
    }

    public void redirectToMainView(){
        setContent( mainView );
    }
}