#!/bin/bash

# make this this is set correctly
export USERGRID_TOOLS_HOME=/usr/share/usergrid-tools-X.Y.Z

pushd $USERGRID_TOOLS_HOME
java -jar ugtools.jar WarehouseExport -upload
java -jar ugtools.jar WarehouseUpsert
popd