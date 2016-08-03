#!/usr/bin/env bash

echo "Update setup.py with a new version.  Otherwise you will see an error like this:"
echo "Upload failed (400): A file named \"usergrid-tools-{VERSION}.tar.gz\" already exists for  usergrid-tools-{VERSION}. To fix problems with that file you should create a new release."
echo "error: Upload failed (400): A file named \"usergrid-tools-{VERSION}.tar.gz\" already exists for  usergrid-tools-{VERSION}. To fix problems with that file you should create a new release."

python setup.py sdist upload -r pypi

