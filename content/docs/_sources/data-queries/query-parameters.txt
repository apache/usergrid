# Query parameters & clauses

When querying your data, you can use your query string to get the data, then sort and manage it on the client. This topic describes a few techniques.

Query examples in this content are shown unencoded to make them easier to read. Keep in mind that you might need to encode query strings if you're sending them as part of URLs, such as when you're executing them with the cURL tool.

<div class="admonition note"> <p class="first admonition-title">Note</p> <p class="last"> 
Optimizing queries. As a best practice, you should include no more than 3 parameters in your queries. The API will not prevent you from submitting a query with more than 3 parameters; however, due to the nature of NoSQL, queries with many parameters can quickly become very inefficient.
</p></div>

For more information, see our [Usergrid DBMS overview](../data-store/data-storage-dbms.html) and [Data store best practices](../data-storage/optimizing-access).

### Contains

Your query can search the text of entity values of the string data type. For example, you can search a postal code field for values that start with a specific three numbers.

For example, the following query selects all restaurants with the word diner in the name:

    /restaurants?ql=select * where restaurants contains 'diner'
    
__Note__: Not all string properties of the default entities are indexed for searching. This includes the User entity's username property.

The following table lists a few examples of the kind of searches you can do in queries.

<table class="usergrid-table">
    <tr>
        <td>Goal</td>
        <td>Example</td>
        <td>Notes</td>
    </tr>
    <tr>
        <td>Find books whose 'title' property contains the full word "tale".</td>
        <td><pre>/books?ql=select * where title contains 'tale'</pre></td>
        <td>containslooks for the occurrence of a full word anywhere in the searched property. Note that contains will not work on the 'name' property, since it is not full-text indexed in the database.</td>
    </tr>
    <tr>
        <td>Find books whose 'title' property contains a word that starts with "ta".</td>
        <td><pre>/books?ql=select * where title contains 'ta*'</pre></td>
        <td>containswill look for partial words if you include a wildcard.</td>
    </tr>
    <tr>
        <td>Find books whose title property is exactly and only "A Tale of Two Cities".</td>
        <td><pre>/books?ql=select * where title = 'A Tale of Two Cities'</pre></td>
        <td>The = operator is looking for a match in the entire searched property value. Use a * wildcard to look for some set of the first characters only.</td>
    </tr>
</table>
    

### Location

If you've stored location data with your entities, you can query for the proximity of the geographical locations those entities represent. For more information on geolocation, see [Geolocation](../geolocation/geolocation.html).

<table class="usergrid-table">
    <tr>
        <td>Goal</td>
        <td>Example</td>
        <td>Notes</td>
    </tr>
    <tr>
        <td>Find stores whose locations are within the specified longitude and latitude.</td>
        <td><pre>/stores?ql=location within 500 of 40.042016, -86.900749</pre></td>
        <td>within will test for values within the value you specify. The within value is expressed as a number of meters.<br>The return results are sorted in order of nearest to furthest. If there are multiple entries at the same location, they're returned in the order they were added to the database.<br> For more on geolocation queries, see Geolocation.</td>
    </tr>
</table>

### Order by

You can return query results that are sorted in the order you specify. Use the order by clause to specify the property to sort by, along with the order in which results should be sorted. The syntax for the clause is as follows:

    order by <property_name> asc | desc
    
The following table includes a few examples:

<table class="usergrid-table">
    <tr>
        <td>Goal</td>
        <td>Example</td>
    </tr>
    <tr>
        <td>Sort by first name in ascending order</td>
        <td>/users?ql=select * where lastname = 'Smith' order by firstname asc</td>
    </tr>
    <tr>
        <td>Sort by first name in descending order</td>
        <td>/users?ql=select * where lastname = 'Smith' order by firstname desc</td>
    </tr>
    <tr>
        <td>Sort by last name, then first name in ascending orderl</td>
        <td>/users?ql=select * where lastname contains 'Sm*' order by lastname asc, firstname asc</td>
    </tr>
<table>
    

### Limit

When your query might return more results than you want to display to the user at once, you can use the limit parameter with cursors or API methods to manage the display of results. By default, query results are limited to 10 at a time. You can adjust this by setting the limit parameter to a value you prefer.

For example, you might execute a query that could potentially return hundreds of results, but you want to display 20 of those at a time to users. To do this, your code sets the limit parameter to 20 when querying for data, then provides a way for the user to request more of the results when they're ready.

You would use the following parameters in your query:

<table class="usergrid-table">
    <tr>
        <td>Parameter</td>
        <td>Type</td>
        <td>Description</td>
    </tr>
    <tr>
        <td>limit</td>
        <td>integer</td>
        <td><p>Number of results to return. The maximum number of results is 1,000. 
            Specifying a limit greater than 1,000 will result in a limit of 1,000.</p>
            <p>You can also use the limit parameter on a request without a query string. 
            The following example is shorthand for selecting all books and limiting by 100 at a time:</p>
            <pre>/books?limit=100</pre>
            <p>Using a limit on a DELETE can help you manage the amount of time it takes 
            to delete data. For example you can delete all of the books, 1000 at a time, 
            with the following:</p>
            <pre>DELETE /books?limit=1000</pre>
            <p> Keep in mind that DELETE operations can take longer to execute. 
            Yet even though the DELETE query call might time out (such as with a 
            very large limit), the operation will continue on the server even if 
            the client stops waiting for the result.</p>
        </td>
    </tr>
</table>

For example:

Select all users whose name starts with fred, and returns the first 50 results:

    /users?ql=select * where name = 'fred*'&limit=50
  
   
### Cursor

<table class="usergrid-table">
    <tr>
        <td>Parameter</td>
        <td>Type</td>
        <td>Description</td>
    </tr>
    <tr>
        <td>cursor</td>
        <td>string</td>
        <td>An encoded representation of the query position pointing to a set of results. To retrieve the next set of results, pass the cursor with your next call for most results./td>
    </tr>
</table>
        
Retrieve the next batch of users whose name is "fred", passing the cursor received from the last request to specify where the next set of results should begin:

    /users?ql=select * where name = 'fred*'&limit=50&cursor=LTIxNDg0NDUxNDpVdjb0JwREVlS1VCd0xvR3NWT0JRQQ
    