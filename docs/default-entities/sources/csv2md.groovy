def dir = new File(".");
def files = dir.list();
files.each() { fileName ->
    if ( fileName.endsWith(".txt") ) {

        def baseName = fileName.substring(0, fileName.length() - 4);

        def outputFile = new File("../" + baseName + ".md");
        outputFile.withWriter('utf-8') { writer ->

            def inputFile = new File(fileName);
            def count = 0;
            writer.writeLine "# ${baseName}"
            writer.writeLine ""
            writer.writeLine "<!-- DO NOT EDIT THIS GENERATED FILE -->";
            writer.writeLine "<table class='usergrid-table rest-endpoints-table'>";

            inputFile.eachLine { line ->
                def parts = line.split("\\t")
                if ( count == 0 ) {
                    writer.writeLine  "  <tr>";
                    parts.each() { part ->
                        writer.writeLine  "    <th>" + part + "</th>";
                    }
                    writer.writeLine  "  </tr>";
                } else {
                    writer.writeLine  "  <tr>";
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

