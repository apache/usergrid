# */
# * Licensed to the Apache Software Foundation (ASF) under one
# * or more contributor license agreements.  See the NOTICE file
# * distributed with this work for additional information
# * regarding copyright ownership.  The ASF licenses this file
# * to you under the Apache License, Version 2.0 (the
# * "License"); you may not use this file except in compliance
# * with the License.  You may obtain a copy of the License at
# *
# *   http://www.apache.org/licenses/LICENSE-2.0
# *
# * Unless required by applicable law or agreed to in writing,
# * software distributed under the License is distributed on an
# * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# * KIND, either express or implied.  See the License for the
#    * specific language governing permissions and limitations
# * under the License.
# */

__author__ = 'Jeff West @ ApigeeCorporation'

from setuptools import setup, find_packages

VERSION = '0.1.13.1'

with open('README.rst') as file:
    long_description = file.read()

setup(
    name='usergrid',
    version=VERSION,
    description='Usergrid SDK for Python',
    url='http://usergrid.apache.org',
    download_url="https://codeload.github.com/jwest-apigee/usergrid-python/zip/v" + VERSION,
    author='Jeff West',
    author_email='jwest@apigee.com',
    packages=find_packages(),
    long_description=long_description,
    install_requires=[
        'requests',
        'urllib3'
    ],
    entry_points={
    },
    classifiers=[
        'Development Status :: 4 - Beta',
        'Intended Audience :: Developers',
        'Operating System :: OS Independent',
        'Topic :: Software Development',
    ]
)
