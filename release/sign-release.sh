export rcstring="rc1"
export vstring="0.0.29"

gpg --armor --detach-sig target/usergrid-release-${vstring}${rcstring}-source.zip
gpg --armor --detach-sig target/usergrid-release-${vstring}${rcstring}-source.tar.gz

