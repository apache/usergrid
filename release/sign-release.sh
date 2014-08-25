export rcstring=""
export vstring="1.0.0"

gpg --armor --detach-sig target/usergrid-release-${vstring}${rcstring}-source.zip
gpg --armor --detach-sig target/usergrid-release-${vstring}${rcstring}-source.tar.gz

