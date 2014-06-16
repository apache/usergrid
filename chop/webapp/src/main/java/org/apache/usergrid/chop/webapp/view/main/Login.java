package org.apache.usergrid.chop.webapp.view.main;

import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;
import org.apache.commons.lang.StringUtils;
import org.apache.usergrid.chop.webapp.service.shiro.ShiroRealm;
import org.apache.usergrid.chop.webapp.view.util.JavaScriptUtil;

public class Login extends UI {

    private final Label title = new Label ("<h3>Login</h3>", ContentMode.HTML);
    private final TextField usernameField = new TextField( "Username:" );
    private final PasswordField passwordField = new PasswordField( "Password:" );
    private final Button loginButton = new Button( "Login" );

    AbsoluteLayout mainLayout;
    MainView mainView = new MainView();

    @Override
    protected void init(VaadinRequest vaadinRequest) {

        mainLayout  = addMainLayout();

        addItems();
        //addButtons(mainLayout);
        loadScripts();
    }


    private void loadScripts() {
        JavaScriptUtil.loadFile("js/jquery.min.js");
        JavaScriptUtil.loadFile( "js/jquery.flot.min.js" );
    }

    private AbsoluteLayout addMainLayout() {

        AbsoluteLayout absoluteLayout = new AbsoluteLayout();
        absoluteLayout.setWidth( "1300px" );
        absoluteLayout.setHeight( "700px" );

        VerticalLayout verticalLayout = new VerticalLayout();
        verticalLayout.setSizeFull();
        verticalLayout.addComponent( absoluteLayout );
        verticalLayout.setComponentAlignment( absoluteLayout, Alignment.MIDDLE_CENTER );

        setContent( verticalLayout );

        return absoluteLayout;
    }

    private void addButtons( AbsoluteLayout mainLayout ) {

        TextField username = new TextField();
        PasswordField pass = new PasswordField();
        mainLayout.addComponent( username, String.format( "left: %spx; top: %spx;", 200, 300 ) );
        mainLayout.addComponent( pass, String.format( "left: %spx; top: %spx;", 200, 400 ) );
    }

    private static void addButton( AbsoluteLayout mainLayout, int left, String caption, Button.ClickListener listener ) {

        Button button = new Button( caption );
        button.setWidth( "100px" );
        button.addClickListener( listener );

        mainLayout.addComponent( button, String.format( "left: %spx; top: 0px;", left ) );
    }

    private void addItems() {

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
        mainLayout.addComponent(formLayout, "left: 350px; top: 50px;");

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
                Notification.show( "Yep", "You successfully logged in", Notification.Type.HUMANIZED_MESSAGE );
                goToMainView();
            }
            else{
                Notification.show( "Error", "Check your password and username", Notification.Type.HUMANIZED_MESSAGE );
            }


        } catch ( Exception e ) {
            Notification.show( "Error", "Check your password and username: " + e.getMessage(), Notification.Type.ERROR_MESSAGE );
        }
    }

    private boolean authUser( String username, String password ){
        return ShiroRealm.authenticateUser(username, password);
    }


    private void goToMainView(){
        setContent( mainView );
    }
}