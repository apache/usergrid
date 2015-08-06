# Query operators & data types

The following operators and data types are supported by the SQL-like query language in Usergrid.

## Operators

<table class="usergrid-table">
    <tr>
        <td>Operator</td>
        <td>Purpose</td>
        <td>Example</td>
    </tr>
    <tr>
        <td>'<' or 'lt'</td>
        <td>Less than</td>
        <td>select * where quantity > '1000'</td>
    </tr>
    <tr>
        <td>'<=' or 'lte'</td>
        <td>Less than or equal to</td>
        <td>Example</td>
    </tr>
    <tr>
        <td>'=' or 'eq'</td>
        <td>Equals</td>
        <td>select * where price = '20.00'</td>
    </tr>
    <tr>
        <td>'>=' or 'gte'</td>
        <td>Greater than or equal to </td>
        <td>select * where quantity >= '1000'</td>
    </tr>
    <tr>
        <td>'>' or 'gt'</td>
        <td>Greater than</td>
        <td>select * where quantity > '1000'</td>
    </tr>
    <tr>
        <td>not <some_expression></td>
        <td>Subtraction of results</td>
        <td>select * where quantity < '4000' and not quantity = '2000'</td>
    </tr>
    <tr>
        <td>and</td>
        <td>Union of results</td>
        <td>select * where quantity > '1000' and quantity < '4000'</td>
    </tr>
    <tr>
        <td>or</td>
        <td>Intersection of results</td>
        <td>select * where quantity = '1000' or quantity = '4000'</td>
    </tr>
    <tr>
        <td>contains</td>
        <td>Narrow by contained text</td>
        <td>select * where title contains 'tale'</td>
    </tr>
</table>


## Data types

As you develop queries, remember that entity properties each conform to a particular data type. For example, in the default entity User, the name property is stored as a string, the created date as a long, and metadata is stored as a JSON object. Your queries must be data type-aware to ensure that query results are as you expect them to be.

For example, if you create an entity with a price property with a value of 100.00, querying for 100 will return no results, since the API expected a decimal-delimited float value in your query.

For a list of property data types for each default entities, see [Default Data Entities](../rest-endpoints/api-doc.html#models)s.

<table class="usergrid-table">
    <tr>
        <td>string</td>
        <td><pre>'value', unicode '\uFFFF', octal '\0707'</pre></td>
    </tr>
    <tr>
        <td>long</td>
        <td><pre>1357412326021</pre> <br> Timestamps are typically stored as long values.</td>
    </tr>
    <tr>
        <td>float</td>
        <td><pre>10.1, -10.1, 10e10, 10e-10, 10E10, 10E-10</pre> <br>
        Your query must be specific about the value you're looking for, down to the value 
        (if any) after the decimal point.</td>
    </tr>
    <tr>
        <td>boolean</td>
        <td><pre>true | false</pre></td>
    </tr>
    <tr>
        <td>UUID</td>
        <td><pre>ee912c4b-5769-11e2-924d-02e81ac5a17b</pre></td>
    </tr>
    <tr>
        <td>Array</td>
        <td><pre>["boat", "car", "bike"]</pre></td>
    </tr>
    <tr>
        <td>object</td>
        <td><p>For a JSON object like this one:</p>
            <pre>
                {
                 "items": [
                  {
                   "name": "rocks"
                  },
                  {
                   "name": "boats"
                  }
                 ]
                }
            </pre>
            <p>you can use dot notation to reach property values in the object:</p>
            <pre>
                 /mycollection/thing?ql="select * where items.name = 'rocks'"
            </pre>
            <p>Objects are often used to contain entity metadata, such as the activities 
            associated with a user, the users associated with a role, and so on.</p>
            <p>Please note that object properties are not indexed. This means queries 
            using dot-notation will be much slower than queries on indexed entity properties.</p></td>
    </tr>
</table>
