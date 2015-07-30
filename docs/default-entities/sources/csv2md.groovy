def dir = new File(".");
def files = dir.list();
files.each() { fileName ->
    if ( fileName.endsWith(".txt") ) {

        def baseName = fileName.substring(0, fileName.length() - 4);

        def outputFile = new File("../" + baseName + ".md");
        outputFile.withWriter('utf-8') { writer ->

            def inputFile = new File(fileName);
            def count = 0;
            writer.writeLine "# ${baseName.capitalize()}"
            writer.writeLine ""
            writer.writeLine "<!-- DO NOT EDIT THIS GENERATED FILE -->";
            writer.writeLine ""
            writer.writeLine "<table class='usergrid-table entities-table'>";

            inputFile.eachLine { line ->
                def parts = line.split("\\t")
                def evenodd = count % 2 ? "even" : "odd";
                if ( count == 0 ) {
                    writer.writeLine  "  <tr>"
                    parts.each() { part ->
                        writer.writeLine  "    <th>" + part + "</th>";
                    }
                    writer.writeLine  "  </tr>";
                } else {
                    writer.writeLine  "  <tr class='ug-${evenodd} usergrid-table'>";
                    parts.each() { part ->
                        writer.writeLine  "    <td>" + part + "</td>";
                    }
                    writer.writeLine  "  </tr>";
                }
                count++;
            }
            writer.writeLine  "</table>";

        }
    }
}

