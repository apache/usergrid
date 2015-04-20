/*
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
*/
var bower = require('./bower.json');

var distPath = 'dist/' + bower.name,
  coveragePath = 'dist-cov/' + bower.name,
  libsFile = 'js/generated/usergrid-libs.min.js',
  devFile = 'js/generated/usergrid-dev.js',
  mainFile = 'js/generated/usergrid.min.js',
  templateFile = 'js/generated-templates/templates.js',
  coverageDir = 'test/coverage/instrument/',
  coverageFile = 'test/coverage/instrument/js/usergrid-coverage.min.js',

  distName = bower.name,
  licenseHeader =
  ' /**\n \
 Licensed to the Apache Software Foundation (ASF) under one\n \
 or more contributor license agreements.  See the NOTICE file\n \
 distributed with this work for additional information \n \
 regarding copyright ownership.  The ASF licenses this file \n\
 to you under the Apache License, Version 2.0 (the \n \
 "License"); you may not use this file except in compliance \n \
 with the License.  You may obtain a copy of the License at \n \
  \n \
 http://www.apache.org/licenses/LICENSE-2.0 \n \
  \n \
 Unless required by applicable law or agreed to in writing,\n \
 software distributed under the License is distributed on an\n \
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY\n \
 KIND, either express or implied.  See the License for the\n \
 specific language governing permissions and limitations\n \
 under the License.\n \
 */\n';
console.warn(
  'to run e2e tests you need to have a running instance of webdriver, 1) npm install protractor -g -> 2) webdriver-manager start --standalone'
);
module.exports = function(grunt) {


  // Project configuration.
  grunt.initConfig({
    pkg: grunt.file.readJSON('package.json'),

    uglify: {
      options: {
        banner: licenseHeader +
          '\n /*! <%= pkg.name %>@<%= pkg.version %>  */\n'
      },
      'usergrid-libs': {
        options: {
          mangle: false,
          compress: {
            warnings: false
          },
          beautify: false
        },
        files: {
          'js/generated/usergrid-libs.min.js': [
            'js/libs/jquery/jquery-1.9.1.min.js',
            'js/libs/jquery/jquery-migrate-1.1.1.min.js',
            'js/libs/jquery/jquery.sparkline.min.js',
            'js/libs/angular-1.2.5/angular.min.js',
            'js/libs/angular-1.2.5/angular-route.min.js',
            'js/libs/angular-1.2.5/angular-resource.min.js',
            'js/libs/angular-1.2.5/angular-sanitize.min.js',
            'js/libs/usergrid.sdk.js',
            'js/libs/MD5.min.js',
            'js/libs/ui-bootstrap/ui-bootstrap-custom-tpls-0.3.0.min.js',
            'js/libs/jqueryui/jquery-ui-1.8.18.min.js',
            'js/libs/jqueryui/date.min.js',
            'bower_components/intro.js/minified/intro.min.js',
            'bower_components/angular-intro.js/src/angular-intro.js',
          ]
        }
      },
      'usergrid-dev': {
        options: {
          mangle: false,
          compress: false,
          beautify: true,
          wrap: true
        },
        files: {
          'js/generated/usergrid-dev.js': [
            'js/app.js',
            'js/**/*.js',
            '!js/config.js',
            '!js/libs/**/*.js',
            '!js/generated/*.js'
          ]
        }
      },
      'usergrid-coverage': {
        options: {
          mangle: false,
          compress: false,
          beautify: false,
          wrap: false
        },
        files: {
          'test/coverage/instrument/js/usergrid-coverage.min.js': [
            coverageDir + 'js/app.js',
            coverageDir + 'js/**/*.js',
            'js/generated-templates/templates.js',
            '!' + coverageDir + 'js/config.js',
            '!' + coverageDir + 'js/libs/**/*.js',
            '!' + coverageDir + 'js/generated/*.js',
            '!' + coverageDir + 'js/usergrid-coverage.min.js'
          ]
        }
      },
      'usergrid-coverage-min': {
        options: {
          mangle: false,
          compress: {
            warnings: false
          },
          beautify: false
        },
        files: {
          'test/coverage/instrument/js/usergrid.min.js': [
            coverageFile
          ]
        }
      },
      'usergrid': {
        options: {
          mangle: false,
          compress: {
            warnings: false
          },
          beautify: false
        },
        files: {
          'js/generated/usergrid.min.js': [
            devFile
          ]
        }
      }

    },
    ngtemplates: {
      "appservices": {
        options: {
          base: 'js/'
        },
        src: ['js/**/*.html', '!**/index*'],
        dest: templateFile,
        options: {
          url: function(url) {
            return url.replace('js/', '');
          }
        }
      }
    },

    cssmin: {
      combine: {
        files: {
          'css/main.min.css': ['css/main.css']
        }
      }
    },
    watch: {
      options: {
        livereload: true
      },
      files: [
        'index-template.html',
        'css/**/*.css',
        'js/**/*.js',
        'js/**/*.html',
        '!tests/',
        '!css/main.min.css',
        '!js/libs/',
        '!js/generated/*.js',
        '!js/generated-templates/*.js'
      ],
      tasks: ['build-dev']
    },
    connect: {
      server: {
        options: {
          target: 'http://localhost:3000/index-debug.html', // target url to open
          open: 'http://localhost:3000/index-debug.html',
          hostname: '*',
          livereload: true,
          port: 3000,
          base: ''
        }
      },
      'e2e-coverage-chrome': {
        options: {
          port: 3006,
          base: coveragePath
        }
      },
      'e2e-phantom': {
        options: {
          port: 3005,
          base: distPath
        }
      },
      'e2e-chrome': {
        options: {
          port: 3006,
          base: distPath
        }
      },
      'e2e-firefox': {
        options: {
          port: 3007,
          base: distPath
        }
      }
    },
    karma: {
      unit: {
        configFile: 'tests/karma.conf.js',
        runnerPort: 9999,
        singleRun: true,
        browsers: ['PhantomJS']
      },
      coverage: {
        configFile: 'tests/karma-coverage.conf.js',
        runnerPort: 9999,
        singleRun: true,
        browsers: ['PhantomJS']
      }
    },
    protractor: {
      options: {
        configFile: "tests/protractorConf.js", // Default config file
        keepAlive: true, // If false, the grunt process stops when the test fails.
        noColor: false, // If true, protractor will not use colors in its output.
        args: {
          baseUrl: 'http://localhost:3005/'
        }
      },
      phantom: {
        options: {
          args: {
            baseUrl: 'http://localhost:3005/',
            // Arguments passed to the command
            'browser': 'phantomjs'
          }
        }
      },
      chrome: {
        options: {
          args: {
            baseUrl: 'http://localhost:3006/?noHelp=true',
            // Arguments passed to the command
            'browser': 'chrome'
          }
        }
      },
      firefox: {
        options: {
          args: {
            baseUrl: 'http://localhost:3007/',
            // Arguments passed to the command
            'browser': 'firefox'
          }
        }
      }
    },
    copy: {
      coverage: {
        files: [{
          src: ['js/*.min.js', 'js/libs/**', 'sdk/**',
            'js/charts/*.json', 'css/**', 'img/**', 'js/libs/**',
            'config.js', 'bower_components/**'
          ],
          dest: coveragePath,
          expand: true
        }, {
          src: ['js/*.min.js'],
          dest: coveragePath,
          cwd: coverageDir,
          expand: true
        }]
      },
      main: {
        files: [
          // includes files within path
          {
            expand: true,
            src: ['*.html', 'config.js', '*.ico', 'helpJson.json'],
            dest: distPath,
            filter: 'isFile'
          }, {
            expand: true,
            src: ['sdk/**', 'css/**', 'img/**', 'js/charts/*.json'],
            dest: distPath
          }, {
            expand: true,
            src: ['js/generated/**', 'js/libs/**', 'css/**', 'img/**',
              'bower_components/**'
            ],
            dest: distPath
          }

        ]
      }
    },
    clean: {
      build: ['dist/', 'dist-cov/', 'test/', 'js/generated/',
        'js/*.min.js', templateFile, 'index.html', 'index-debug.html'
      ],
      coverage: ['reports/']
    },
    dom_munger: {
      main: {
        options: {
          update: {
            selector: '#main-script',
            attribute: 'src',
            value: mainFile
          }

        },
        src: 'index-template.html', //update the dist/index.html (the src index.html is copied there)
        dest: 'index.html' //update the dist/index.html (the src index.html is copied there)

      },
      dev: {
        options: {
          update: {
            selector: '#main-script',
            attribute: 'src',
            value: devFile
          }
        },
        src: 'index-template.html', //update the dist/index.html (the src index.html is copied there)
        dest: 'index-debug.html' //update the dist/index.html (the src index.html is copied there)
      },
      coverage: {
        options: {
          update: {
            selector: '#main-script',
            attribute: 'src',
            value: 'js/usergrid-coverage.min.js'
          }
        },
        src: 'index-template.html', //update the dist/index.html (the src index.html is copied there)
        dest: coveragePath + '/index.html' //update the dist/index.html (the src index.html is copied there)
      }
    },
    bower: {
      install: {
        options: {
          cleanup: false,

          copy: false
        }
      }
    },
    compress: {
      main: {
        options: {
          mode: 'tar',
          archive: 'dist/' + distName + '.tar'
        },
        expand: true,
        cwd: distPath + '/',
        src: ['**/*'],
        dest: distName + '.' + bower.version
      }
    },

    instrument: {
      files: 'js/**/*.js',
      options: {
        lazy: true,
        basePath: coverageDir
      }
    },
    makeReport: {
      src: 'reports/**/*.json',
      options: {
        type: 'lcov',
        dir: 'reports',
        print: 'detail'
      }
    }
  });

  grunt.loadNpmTasks('grunt-contrib-uglify');
  grunt.loadNpmTasks('grunt-contrib-cssmin');
  grunt.loadNpmTasks('grunt-contrib-copy');
  grunt.loadNpmTasks('grunt-contrib-watch');
  grunt.loadNpmTasks('grunt-contrib-connect');
  grunt.loadNpmTasks('grunt-contrib-compress');
  grunt.loadNpmTasks('grunt-contrib-clean');
  grunt.loadNpmTasks('grunt-angular-templates');
  grunt.loadNpmTasks('grunt-bower-task');
  grunt.loadNpmTasks('grunt-contrib-connect');
  grunt.loadNpmTasks('grunt-protractor-runner');
  grunt.loadNpmTasks('grunt-karma');
  grunt.loadNpmTasks('grunt-dom-munger');

  grunt.loadNpmTasks('grunt-istanbul');

  // Default task(s).
  grunt.registerTask('dev', ['connect:server', 'watch']);

  grunt.registerTask('validate', ['jshint', 'complexity']);
  grunt.registerTask('report', ['build', 'coverage']);

  grunt.registerTask('build', ['clean:build', 'bower:install', 'ngtemplates',
    'uglify', 'cssmin', 'dom_munger', 'karma:unit', 'copy'
  ]);
  grunt.registerTask('build-dev', ['build']);
  grunt.registerTask('build-coverage', ['ngtemplates', 'instrument',
    'uglify:usergrid-coverage', 'uglify:usergrid-coverage-min', 'cssmin',
    'dom_munger', 'copy:coverage'
  ]);

  grunt.registerTask('default', ['build']);

  grunt.registerTask('e2e', ['connect:e2e-phantom', 'protractor:phantom']);
  grunt.registerTask('e2e-chrome', ['connect:e2e-chrome', 'protractor:chrome']);
  grunt.registerTask('e2e-coverage', ['clean:coverage',
    'connect:e2e-coverage', 'protractor:coverage'
  ]);
  grunt.registerTask('e2e-coverage-chrome', ['clean:coverage',
    'connect:e2e-coverage-chrome', 'protractor:chrome', 'makeReport'
  ]);
  grunt.registerTask('e2e-firefox', ['connect:e2e-firefox',
    'protractor:firefox'
  ]);
  grunt.registerTask('no-monitoring', ['build', 'clean:perf', 'karma:unit']);

};
