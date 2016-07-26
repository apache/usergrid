from setuptools import setup, find_packages

__author__ = 'Jeff West @ ApigeeCorporation'

VERSION = '0.5.13'

setup(
        name='usergrid-tools',
        version=VERSION,
        description='Tools for working with Apache Usergrid',
        url='http://usergrid.apache.org',
        download_url="https://codeload.github.com/jwest-apigee/usergrid-util-python/zip/%s" % VERSION,
        author='Jeff West',
        author_email='jwest@apigee.com',

        # packages=['usergrid_tools', 'es_tools'],
        packages=find_packages(exclude=["*.tests", "*.tests.*", "tests.*", "tests", "sandbox"]),

        install_requires=[
            'requests',
            'usergrid>=0.1.3',
            'time_uuid',
            'argparse',
            'redis',
            'ConcurrentLogHandler',
        ],

        entry_points={
            'console_scripts': [
                'usergrid_iterator = usergrid_tools.iterators.simple_iterator:main',
                'usergrid_data_migrator = usergrid_tools.migration.usergrid_data_migrator:main',
                'usergrid_data_exporter = usergrid_tools.migration.usergrid_data_exporter:main',
                'usergrid_entity_index_test = usergrid_tools.indexing.entity_index_test:main',
                'usergrid_batch_index_test = usergrid_tools.indexing.batch_index_test:main',
                'usergrid_parse_importer = usergrid_tools.parse_importer.parse_importer:main',
                'usergrid_deleter = usergrid_tools.parse_importer.parse_importer:main',
                'usergrid_library_check = usergrid_tools.library_check:main',
            ]
        }
)
