# Bosh Release + PCF Tile for Apache Usergrid

This is a bosh release for the apache usergrid. The scripts provided can help create the bosh release and tile for Apache Usergrid.
A version of the tile is available here: https://s3.amazonaws.com/usergrid-v2-public/usergrid_v2.pivotal

# Components Dependency
* Apache usergrid warfile
  Grab the code from: https://github.com/apache/usergrid
  Build the war file and save it as ROOT.war 
* CF CLI Linux binary
  Download the CF CLI linux binary (64 bit) from: https://github.com/cloudfoundry/cli/releases
* ElasticSearch and Cassandra. 
  Apache Usergrid requires ElasticSearch and Cassandra to search and manage data.
  The Bosh release uses dockerized images to run both ElasticSearch and Cassandra.
  Create docker images for both elastic search (v1.7) and cassandra (v2.1) and save them locally as tarballs
* Docker Bosh Release
  To run docker images within Bosh, we need the docker bosh release.
  Download v23 from: http://bosh.io/releases/github.com/cf-platform-eng/docker-boshrelease?all=1

# Building Bosh release
* Ensure following files are available at the root of the apache-usergrid-release directory
```
cf-linux-amd64.tgz          # downloaded from CF cli github repo
ROOT.war                    # built from usergrid repo
cassandra-2.1.tgz           # Saved Cassandra 2.1 docker image 
elasticsearch-1.7.tgz       # Saved ElasticSearch 1.7 docker image 
docker-boshrelease-23.tgz   # Docker Bosh release v23
```
* Run addBlobs.sh
  Important to ensure the above blobs filenames match the entries inside the addBlobs.sh (& each of the packages/*/packaging file)
* Run ./createRelease.sh
  Edit the version as required inside the script
# Building Tile
* Edit the apache-usergrid-tile-1.6.yml to refer to the correct version of release tarball (for docker bosh release and usergrid)
* Run ./createTile.sh
  Edit the file names or versions as needed.
  The docker-boshrelease-23.tgz file should be present in the directory to create a valid working tile
  The script should create the usergrid.pivotal tile file.

# Notes
* Ensure the usergrid war file is named ROOT.war (or rename all references of ROOT.war with different file name) before running addBlobs.sh
* If newer versions are being used, please check and replace the associated versions inside packages/<package-name>/spec & packages/<packagge-name>/packaging file to deal with correct files.
* Update the tile metadata file if newer release versions are used
* Update the content_migrations.yml if new tile version is being published
