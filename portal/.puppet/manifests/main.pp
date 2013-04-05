$server_name = 'usergrid.dev'
$ssl_path = '/etc/apache2/ssl/'
$key_path = "${ssl_path}${server_name}.key"
$cert_path = "${ssl_path}${server_name}.cert"

Exec { path => '/usr/bin:/usr/sbin:/bin:/sbin:/usr/local/bin' }

package { 'openssl':
    ensure => installed
}

file { $ssl_path:
    ensure => directory,
    owner => "root",
    group => "root"
}

exec { 'openssl-genrsa':
    command => "openssl genrsa -out ${key_path} 2048",
    creates => $key_path,
    require => File[$ssl_path]
}

exec { 'openssl-req':
    command => "openssl req -new -x509 -key ${key_path} -out ${cert_path} -days 3650 -subj /CN=${server_name}",
    creates => $cert_path,
    require => Exec['openssl-genrsa']
}

class { 'apache': }

apache::mod { 'env': }

apache::vhost { $server_name:
    port     => '443',
    docroot  => '/vagrant',
    template => 'vhost-ssl.conf.erb',
    require  => Exec['openssl-req']
}

package { 'compass':
    ensure => present,
    provider => gem
}

package { 'bootstrap-sass':
    ensure => '2.0.0',
    provider => gem
}