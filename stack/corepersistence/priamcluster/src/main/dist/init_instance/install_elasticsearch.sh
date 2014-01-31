#!/bin/bash

# Install and stop ElasticSearch
pushd /etc/apt/sources.list.d

cat >> elasticsearch.sources.list << EOF
deb http://packages.elasticsearch.org/elasticsearch/1.0/debian stable main
EOF
apt-get update
apt-get --force-yes -y install elasticsearch
/etc/init.d/elasticsearch stop

# Configure and restart ElasticSearch
update-rc.d elasticsearch defaults 95 10
cd /usr/share/usergrid/scripts
groovy ./configure_elasticsearch.groovy > /etc/elasticsearch/elasticsearch.yml

popd