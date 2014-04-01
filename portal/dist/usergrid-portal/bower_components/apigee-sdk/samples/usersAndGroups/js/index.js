/**
 * This JavaScript file contains the app's logic. It uses Apigee JavaScript
 * SDK functions to access an Apigee app services database.
 */

// Be sure to set your org name and app name!
var apigeeClient = new Apigee.Client({
    orgName: '', // Your organization name. You'll find this in the admin portal.
    appName: 'sandbox', // Your App Services app name. It's in the admin portal.
    logging: true, //optional - turn on logging, off by default
    buildCurl: true //optional - log network calls in the console, off by default	
});

// Variables we can use from multiple functions in the code.
var users, allGroups, currentUser, groupsForUser;

// Each of these initializes a separate page of the UI.
$(document).on("pagecreate", "#page_home", function (event) {
    $('#frm_display_add_user_to_group').on('click', 
        '#btn_display_add_user_to_group',  displayAddUserToGroup);
});
$(document).on("pageinit", "#page_view_users_list", function (event) {
    getUsers();
});
$(document).on("pageinit", "#page_view_group_list", function (event) {
    getGroups();
});
$(document).on("pageinit", "#page_add_user", function (event) {
    $('#frm_add_user').on('click', '#btn_add_user', addUser);
});
$(document).on("pageinit", "#page_add_group", function (event) {
    $('#frm_add_group').on('click', '#btn_add_group', addGroup);
});
$(document).on("pageinit", "#page_add_user_to_group", function (event) {
        buildAllGroupsList('#select_groups');
        buildGroupsForUserList(currentUser, '#list_groups_with_this_user');
        $('#frm_add_user_to_group').on('click', '#btn_add_user_to_group', addUserToGroup);
});

/**
 * Gets the full list of users in the application.
 * updating a ul in the UI that lists the users.
 */
function getUsers() {
    // Create a collection instance to keep the 
    // users in.
    var users = new Apigee.Collection({
        "client": apigeeClient,
        "type": "users"
    });
    // Fetch the users from the database.
    users.fetch(
        // Called if the fetch succeeded.
        function () {
        // Clear the list of old stuff.
        $('#users_list').empty();
        if (users.hasNextEntity()){
            // Loop through the returned users,
            // adding a new list item for each.
            while (users.hasNextEntity()) {
                var user = users.getNextEntity();
                $('#users_list').append(
                '<li data-theme="c">' +
                '<h3>' + user.get("username") + '</h3>' +
                '</li>');
            }   
        // If there aren't any users, add a message.
        } else {
            $('#users_list').append(
            '<li data-theme="c">' +
            '<h3>No users to display.</h3>' +
            '</li>');
        }
        // Refresh the list so that it's styled correctly.
        $('#users_list').listview('refresh');
    },
    // Called if the attempt to get users failed.
    function () {
        client.logError({tag:"getUsers", 
            logMessage:"Unable to retrieve users."})
    });
}

/**
 * Gets the full list of groups from the database,
 * updating a ul in the UI with the returned values.
 */
function getGroups() {
    // A local collection variable to hold the group data.
    var groups = new Apigee.Collection({
        "client": apigeeClient,
        "type": "groups"
    });
    // Attempt to get the group data.
    groups.fetch(
    // Called if the fetch attempt succeeded.
    function () {
        // Empty the HTML list of groups.
        $('#groups_list').empty();
        // Loop through users in the collection, wrapping values from
        // each in HTML for inclusion in the UI.
        if (groups.hasNextEntity()) {
            while (groups.hasNextEntity()) {
                var group = groups.getNextEntity();
                // Build out the list with jQuery.
                $('#groups_list').append(
                '<li data-theme="c">' +
                '<h3>' + group.get("path") + '</h3>' +
                '</li>');
            }
        // If there weren't any groups, display a message.
        } else {
            $('#groups_list').append(
            '<li data-theme="c">' +
            '<h3>No groups to display.</h3>' +
            '</li>');
        }
        // Refresh the list using jQuery Mobile.
        $('#groups_list').listview('refresh');
    },
    // Called if the fetch request failed.
    function () {
        apigeeClient.logError({tag:"getGroups", 
            logMessage:"Unable to retrieve groups."})
    });
}

/**
 * Adds a new user to the database.
 */
function addUser() {
    // Variable to collect data to send with the request.
    var userName = $("#fld_user_name").val(); // Must be unique.
    var name = $("#fld_name").val();
    var email = $("#fld_email").val();
    var password = $("#fld_password").val();

    // Call an SDK method to create a new user with
    // data collected from the form.
    apigeeClient.signup(userName, password, email, 
        name, function (error, entity, data) {
        if (error) {
            var message = "Unable to add a user. " + data;
            apigeeClient.logError({tag:"addUser", logMessage:message})
        } else {
            // Refresh the user list to include the new user.
            getUsers();
        }
    });
}

/**
 * Add a new group to the database.
 */
function addGroup() {

    // Collect values to send with the request.
    var path = $("#fld_group_path").val();
    var title = $("#fld_display_name").val(); // Must be unique.
    
    // Bundle values in a JSON object.
    var options = {
        "path" : path,
        "title" : title
    }

    // Call an SDK method to create the group with the collected
    // data.
    apigeeClient.createGroup(options, function (error, response) {
        // If the attempt fails, display an error message.
        if (error) {
            var message = "Unable to add a group. " + data;
            apigeeClient.logError({tag:"addGroup", logMessage:message})
        } else {
            //Reload the list of groups from the database.
            getGroups();
        }
    });    
}

/**
 * Build the list of all groups into a dropdown
 * for selecting the group to which a user should
 * be added.
 */
function buildAllGroupsList(listId){
    // Create a local collection object that points
    // at groups in the datbase.
    groups = new Apigee.Collection({
        "client": apigeeClient,
        "type": "groups"
    });
    // Attempt to get the data.
    groups.fetch(
        // If the attempt succeeds, loop through
        // the results, building options that the 
        // dropdown still display.
        function () {
            $(listId).empty();
            while (groups.hasNextEntity()) {
                var group = groups.getNextEntity();
                $(listId).append(
                '<option value = \"' + group.get("path") + '\">' +
                group.get("path") + '</option>');
            }
        },
        // If the attempt fails log a message.
        function () {
            var message = "Unable to create the list of groups. " + data;
            apigeeClient.logError({tag:"buildAllGroupsList", logMessage:message})
    });    
}

/**
 * Builds a list of the groups in which userName
 * belongs.
 */
function buildGroupsForUserList(userName, listId) {
    var options = {
        "username" : userName,
        "type": "users"
    }
    // Use an SDK method to get an entity object 
    // representing the user whose groups should be 
    // displayed.
    apigeeClient.getEntity(options, function(error, entity, data){
        if (error) {
            var message = "Error getting user entity. " + data;
            apigeeClient.logError({tag:"buildGroupsForUserList", logMessage:message})
        } else {
            // Call an SDK method to get the groups to which 
            // the user belongs.
            entity.getGroups(function(error, data, groups){
                if (error){
                    var message = "Couldn't get a user's groups. " + data;
                    apigeeClient.logError({tag:"buildGroupsForUserList", logMessage:message})
               } else {
                    // Clear the list into which group data will go.
                    $(listId).empty();
                    // Loop through the list of groups, adding 
                    // each to the list
                    for (var i = 0; i < groups.length; i++) {
                        var group = groups[i];
                        $(listId).append(
                        '<li data-theme="c">' +
                        '<h3>' + group.path + '</h3>' +
                        '</li>');
                    }
                    // Refresh the list to make sure it's styled.
                    $(listId).listview('refresh');
               }
            });
        }
    });
}

/**
 * Add a user to a group.
 */
function addUserToGroup() {
    // Current user set from an input on 
    // the home page.
    var userName = currentUser;
    // Group selected from the dropdown.
    var selectedGroupPath = $("#select_groups").val();

    var groupOptions = {
            client:apigeeClient, 
            path:selectedGroupPath
    }
    // Create a local variable that points
    // at the selected group in the database.
    var group = new Apigee.Group(groupOptions);
    // Get the group data.
    group.fetch();
    
    // Options for the 
    var options = {
        'username':currentUser,
        'type':'users'
    }
    // Call an SDK method to get an entity representing
    // the user so it can be used when adding the 
    // user to the group.
    apigeeClient.getEntity(options, function(error, entity, data){
        if (error) {
            var message = "Error getting user entity. " + data;
            apigeeClient.logError({tag:"addUserToGroup", logMessage:message})
        } else {
            var addUserOptions = {
                user : entity
            }
            // Call an SDK method to add the user.
            group.add(addUserOptions, function(error, data, entities){
                if (error){
                    var message = "Error adding user to group. " + data;
                    displayPopup(message);
                    apigeeClient.logError({tag:"addUserToGroup", logMessage:message})
                } else {
                    // Refresh the list of groups the user is in.
                    buildGroupsForUserList(currentUser, '#list_groups_with_this_user');
                }
            });
        }        
    });
}

/**
 * Displays a message in a popup.
 */
function displayAddUserToGroup(){
    currentUser = $("#fld_home_username").val();
    if (currentUser.isEmpty()){
        displayPopup("Please enter the username for a user in your " +
            "application.");    
    } else {
        $("#link_display_add_user_to_group").click();
    }
}


/**
 * Displays a popup.
 */
function displayPopup(message){
    var options = {
        "transition" : "slideup"
    }
    $('#messagePopup').empty();
    $('#messagePopup').append(
        '<p>' + message + '</p>'
    );
    $('#messagePopup').popup('open', options );
}

/**
 * Adds an empty string check to strings.
 */
String.prototype.isEmpty = function() {
    if (!this.match(/\S/)) {
        return true;
    } else {
        return false;
    }
}
