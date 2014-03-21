sudo mkdir __tmpupdate__
pushd __tmpupdate__

    sudo s3cmd --config=/etc/s3cfg get s3://${RELEASE_BUCKET}/usergrid-cloudformation-1.0-SNAPSHOT-any.tar.gz
    sudo tar xzvf usergrid-cloudformation-1.0-SNAPSHOT-any.tar.gz 
    sudo /etc/init.d/tomcat7 stop
    sudo cp -r webapps/* /var/lib/tomcat7/webapps

    pushd /usr/share/usergrid/scripts
        sudo groovy configure_portal_new.groovy > /var/lib/tomcat7/webapps/portal/config.js 
    popd

    sudo /etc/init.d/tomcat7 start

popd 
sudo rm -rf __tmpupdate__
