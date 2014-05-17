use 5.006;
use strict;
use warnings FATAL => 'all';
use Module::Build;

my $builder = Module::Build->new(
    module_name         => 'Usergrid::Client',
    license             => 'Apache_2_0',
    dist_author         => q{Anuradha Weeraman <anuradha@cpan.org>},
    dist_version_from   => 'lib/Usergrid/Client.pm',
    release_status      => 'stable',
    configure_requires => {
        'Module::Build' => 0,
    },
    build_requires => {
        'Test::More' => 0,
    },
    requires => {
        'Moose' => 0,
        'JSON' => 0,
        'REST::Client' => 0
    },
    add_to_cleanup     => [ 'Usergrid-Client-*' ]
);

$builder->create_build_script();
