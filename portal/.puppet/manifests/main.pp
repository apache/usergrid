class {'apache': }

apache::vhost { 'usergrid.dev':
    port    => '80',
    docroot => '/vagrant'
}