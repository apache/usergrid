<?xml version="1.0" encoding="UTF-8"?><html xmlns="http://www.w3.org/1999/xhtml" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:wadl="http://research.sun.com/wadl/2006/10" xmlns:html="http://www.w3.org/1999/xhtml">
   <head>
      <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" /><style type="text/css">
        body {
            font-family: sans-serif;
            font-size: 0.85em;
            margin: 2em 2em;
        }
        .methods {
            margin-left: 2em; 
            margin-bottom: 2em;
        }
        .method {
            background-color: #eef;
            border: 1px solid #DDDDE6;
            padding: .5em;
            margin-bottom: 1em;
            width: 50em
        }
        .methodNameTable {
            width: 100%;
            border: 0px;
            border-bottom: 2px solid white;
            font-size: 1.4em;
        }
        .methodNameTd {
            background-color: #eef;
        }
        h1 {
            font-size: 2m;
            margin-bottom: 0em;
        }
        h2 {
            border-bottom: 1px solid black;
            margin-top: 1.5em;
            margin-bottom: 0.5em;
            font-size: 1.5em;
           }
        h3 {
            color: #FF6633;
            font-size: 1.35em;
            margin-top: .5em;
            margin-bottom: 0em;
        }
        h5 {
            font-size: 1.2em;
            color: #99a;
            margin: 0.5em 0em 0.25em 0em;
        }
        h6 {
            color: #700000;
            font-size: 1em;
            margin: 1em 0em 0em 0em;
        }
        .h7 {
            margin-top: .75em;
            font-size: 1em;
            font-weight: bold;
            font-style: italic;
            color: blue;
        }
        .h8 {
            margin-top: .75em;
            font-size: 1em;
            font-weight: bold;
            font-style: italic;
            color: black;
        }
        tt {
            font-size: 1em;
        }
        table {
            margin-bottom: 0.5em;
            border: 1px solid #E0E0E0;
        }
        th {
            text-align: left;
            font-weight: normal;
            font-size: 1em;
            color: black;
            background-color: #DDDDE6;
            padding: 3px 6px;
            border: 1px solid #B1B1B8;
        }
        td {
            padding: 3px 6px;
            vertical-align: top;
            background-color: #F6F6FF;
            font-size: 0.85em;
        }
        p {
            margin-top: 0em;
            margin-bottom: 0em;
        }
        td.summary {
            background-color: white;
        }
        td.summarySeparator {
            padding: 1px;
        }
    </style><title>
         Web Application
         
      </title>
   </head>
   <body>
      <h1>
         Web Application
         
      </h1>
      <h2>Summary</h2>
      <table>
         <tr>
            <th>Resource</th>
            <th>Method</th>
            <th>Description</th>
         </tr>
         <tr>
            <td class="summary"><a href="#d1e12">http://localhost:8080/ROOT/{applicationId}</a></td>
            <td class="summary"><a href="#executeGet">GET</a><br /><a href="#executePost">POST</a><br /><a href="#executePut">PUT</a><br /><a href="#executeDelete">DELETE</a><br /></td>
            <td class="summary"><br /><br /><br /><br /><br /><br /><br /></td>
         </tr>
         <tr>
            <td class="summarySeparator"></td>
            <td class="summarySeparator"></td>
            <td class="summarySeparator"></td>
         </tr>
      </table>
      <p></p>
      <h2>Resources</h2><br /><h3><a name="d1e4">http://localhost:8080/ROOT/</a></h3>

      <p></p>
      <h5>Methods</h5>
      <div class="methods">
         <div class="method">
            <table class="methodNameTable">
               <tr>
                  <td class="methodNameTd" style="font-weight: bold"><a name="getRoot">GET</a></td>
                  <td class="methodNameTd" style="text-align: right">getRoot()</td>
               </tr>
            </table>
            <p></p>
            <h6>request</h6>
            <div style="margin-left: 2em">
               unspecified              
            </div>
            <h6>responses</h6>
            <div style="margin-left: 2em">
               <div class="h8">status: </div>
               200 - OK
               
               <div style="margin-left: 2em">
                  <div class="h7">representations</div>
                  <table>
                     <tr>
                        <td>application/json</td>

                     </tr>
                  </table>
               </div>
            </div>
         </div>
      </div>

      <h3><a name="d1e194">http://localhost:8080/ROOT/management/accounts</a></h3>
      <p></p>
      <h5>Methods</h5>
      <div class="methods">
         <div class="method">
            <table class="methodNameTable">
               <tr>
                  <td class="methodNameTd" style="font-weight: bold"><a name="newAccountFromForm">POST</a></td>
                  <td class="methodNameTd" style="text-align: right">newAccountFromForm() 
                     
                  </td>

               </tr>
            </table>
            <p></p>
            <h6>request</h6>
            <div style="margin-left: 2em">
               <div class="h7">representations</div>
               <table>
                  <tr>

                     <td>application/x-www-form-urlencoded</td>
                  </tr>
                  <tr>
                     <td style="padding: 0em, 0em, 0em, 2em">
                        <div class="h7">query params</div>
                        <table>
                           <tr>
                              <td><strong>account</strong></td>

                              <td><a href="http://www.w3.org/TR/xmlschema-2/#string">string</a></td>
                           </tr>
                           <tr>
                              <td><strong>username</strong></td>
                              <td><a href="http://www.w3.org/TR/xmlschema-2/#string">string</a></td>
                           </tr>
                           <tr>

                              <td><strong>name</strong></td>
                              <td><a href="http://www.w3.org/TR/xmlschema-2/#string">string</a></td>
                           </tr>
                           <tr>
                              <td><strong>email</strong></td>
                              <td><a href="http://www.w3.org/TR/xmlschema-2/#string">string</a></td>
                           </tr>

                           <tr>
                              <td><strong>password</strong></td>
                              <td><a href="http://www.w3.org/TR/xmlschema-2/#string">string</a></td>
                           </tr>
                        </table>
                        <p></p>
                     </td>
                  </tr>

               </table>
            </div>
            <h6>responses</h6>
            <div style="margin-left: 2em">
               <div class="h8">status: </div>
               200 - OK
               
               <div style="margin-left: 2em">
                  <div class="h7">representations</div>

                  <table>
                     <tr>
                        <td>application/json</td>
                     </tr>
                  </table>
               </div>
            </div>
         </div>

      </div>
   </body>
</html>