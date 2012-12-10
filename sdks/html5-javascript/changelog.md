##Change log

###0.9.10

- Refactored directory structure.  SDK file now at root, extensions in their own directory, examples in their own directory.

- Moved cURL command generator into a separate file (extensions/usergrid.curl.js).  Include this file after the the SDK if you want cURL command generation.

- Moved Validation functionality into a separate file (extensions/usergrid.validation.js). Include this file after the the SDK if you want to use this functionality.

- Moved Session file into a separate file (extensions/usergrid.session.js). Include this file after the the SDK if you want to use this functionality.

- Removed deprecated get function from Collection object.

- Added timeout callback.

- Added beginnings of a qUnit test suite - only a few functions tested so far, but we will add to the suite as we progress.

- Removed minified files.  We hope to host these files on a CDN soon.
