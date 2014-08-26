

export rcstring="-rc2"
export vstring="1.0.0"

gpg --armor --detach-sig target/apache-usergrid-incubating-${vstring}${rcstring}-source.zip
gpg --armor --detach-sig target/apache-usergrid-incubating-${vstring}${rcstring}-source.tar.gz

gpg --print-md MD5 apache-usergrid-incubating-${vstring}${rcstring}-source.zip > apache-usergrid-incubating-${vstring}${rcstring}-source.zip.md5
gpg --print-md MD5 apache-usergrid-incubating-${vstring}${rcstring}-source.tar.gz > apache-usergrid-incubating-${vstring}${rcstring}-source.tar.gz.md5
