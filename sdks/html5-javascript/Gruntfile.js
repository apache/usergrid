module.exports = function(grunt) {
	var files = [
        "lib/modules/util/Event.js",
        "lib/modules/util/Logger.js",
        "lib/modules/util/Promise.js",
        "lib/modules/util/Ajax.js",
        "lib/Usergrid.js",
		"lib/modules/Client.js",
		"lib/modules/Entity.js",
		"lib/modules/Collection.js",
		"lib/modules/Group.js",
		"lib/modules/Counter.js",
		"lib/modules/Folder.js",
		"lib/modules/Asset.js",
		"lib/modules/Error.js"
	];
	var tests = ["tests/mocha/index.html", "tests/mocha/test_*.html"];
	// Project configuration.
	grunt.initConfig({
        //pkg: grunt.file.readJSON('package.json'),
        "meta": {
            "package": grunt.file.readJSON("package.json")
        },
        "clean": ["usergrid.js", "usergrid.min.js"],
        "uglify": {
            "unminified": {
                "options": {
                    "banner": "/*! \n\
 *Licensed to the Apache Software Foundation (ASF) under one\n\
 *or more contributor license agreements.  See the NOTICE file\n\
 *distributed with this work for additional information\n\
 *regarding copyright ownership.  The ASF licenses this file\n\
 *to you under the Apache License, Version 2.0 (the\n\
 *\"License\"); you may not use this file except in compliance\n\
 *with the License.  You may obtain a copy of the License at\n\
 *\n\
 *  http://www.apache.org/licenses/LICENSE-2.0\n\
 * \n\
 *Unless required by applicable law or agreed to in writing,\n\
 *software distributed under the License is distributed on an\n\
 *\"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY\n\
 *KIND, either express or implied.  See the License for the\n\
 *specific language governing permissions and limitations\n\
 *under the License.\n\
 * \n\
 * \n\
 * <%= meta.package.name %>@<%= meta.package.version %> <%= grunt.template.today('yyyy-mm-dd') %> \n\
 */\n",
                    "mangle": false,
                    "compress": false,
                    "beautify": true,
                    "preserveComments": function(node, comment){
                        //console.log((node.parent_scope!==undefined&&comment.value.indexOf('*Licensed to the Apache Software Foundation')===-1)?"has parent":comment.value);
                        return comment.type==='comment2'&&comment.value.indexOf('*Licensed to the Apache Software Foundation')===-1;
                    }
                },
                "files": {
                    "usergrid.js": files
                }
            },
            "minified": {
                "options": {
                    "banner": "/*! \n\
 *Licensed to the Apache Software Foundation (ASF) under one\n\
 *or more contributor license agreements.  See the NOTICE file\n\
 *distributed with this work for additional information\n\
 *regarding copyright ownership.  The ASF licenses this file\n\
 *to you under the Apache License, Version 2.0 (the\n\
 *\"License\"); you may not use this file except in compliance\n\
 *with the License.  You may obtain a copy of the License at\n\
 *\n\
 *  http://www.apache.org/licenses/LICENSE-2.0\n\
 * \n\
 *Unless required by applicable law or agreed to in writing,\n\
 *software distributed under the License is distributed on an\n\
 *\"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY\n\
 *KIND, either express or implied.  See the License for the\n\
 *specific language governing permissions and limitations\n\
 *under the License.\n\
 * \n\
 * \n\
 * <%= meta.package.name %>@<%= meta.package.version %> <%= grunt.template.today('yyyy-mm-dd') %> \n\
 */\n",
                    "mangle": false,
                    "compress": true,
                    "beautify": false,
                    "preserveComments": "some"
                },
                "files": {
                    "usergrid.min.js": files
                }
            }
        },
        "connect": {
            "server": {
                "options": {
                    "port": 3000,
                    "base": "."
                }
            },
            "test": {
                "options": {
                    "port": 8000,
                    "base": "."
                }
            }
        },
        "watch": {
            "files": [files, 'Gruntfile.js'],
            "tasks": ["default"]
        },
        "blanket_mocha": {
            //"all": tests,
            urls: [ 'http://localhost:8000/tests/mocha/index.html' ],
            "options": {
                "dest": "report/coverage.html",
                "reporter": "Spec",
                "threshold": 70
            }
        }
    });
	grunt.loadNpmTasks("grunt-contrib-clean");
	grunt.loadNpmTasks("grunt-contrib-uglify");
	grunt.loadNpmTasks("grunt-contrib-watch");
	grunt.loadNpmTasks("grunt-contrib-connect");
	grunt.loadNpmTasks("grunt-blanket-mocha");
	grunt.registerTask("default", [
		"clean",
		"uglify"
	]);
	grunt.registerTask("dev", [
		"connect:server",
		"watch"
	]);
	grunt.registerTask("test", [
		"connect:test",
		"blanket_mocha"
	]);
};
