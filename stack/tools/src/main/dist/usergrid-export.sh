#!/bin/bash

# make this this is set correctly
export USERGRID_TOOLS_HOME=/usr/share/usergrid-tools-X.Y.Z

pushd $USERGRID_TOOLS_HOME
java -jar usergrid-tools.jar WarehouseExport -upload
java -jar usergrid-tools.jar WarehouseUpsert
popd