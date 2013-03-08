class {'apache': }

apache::vhost { 'usergrid.dev':
    port    => '80',
    docroot => '/vagrant'
}

package { 'compass':
    ensure => present,
    provider => gem
}

package { 'bootstrap-sass':
    ensure => '2.0.0',
    provider => gem
}