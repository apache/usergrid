var packageJson = require('./package.json');
var userGrid = require('./config.js');

var menu = '<ul class="nav nav-list" menu="sideMenu">';
userGrid.options.menuItems.forEach(function(menuItem){
  menu += '<li class="option '+ (menuItem.active ? 'active' : '') + '" ng-cloak>';
  menu += '<a data-ng-href="'+menuItem.path+'"><i class="pictogram">'+menuItem.pic+'</i>'+menuItem.title+'</a>';
  menuItem.items && menuItem.items.forEach(function(subItem){
    menu += '<ul class="nav nav-list">';
    menu += '<li>';
    menu += '<a data-ng-href="'+subItem.path+'"><i class="pictogram sub">'+subItem.pic+'</i>'+subItem.title+'</a>'
    menu += '</li>';
    menu += '</ul>';
  });
  menu += '</li>';
});
menu += '</ul>';

var mainRefs = "",
    devRefs = ""
    ;
userGrid.options.scriptReferences.main.forEach(function (current) {
  mainRefs += "<script src='" + current + "'></script>";
});
userGrid.options.scriptReferences.dev.forEach(function (current) {
  devRefs += "<script src='" + current + "'></script>";
});

var cssRefs = "";
userGrid.options.cssRefs.forEach(function(css){
  cssRefs += '<link href="'+css.src+'" rel="stylesheet" id="'+css.id+'"/>';
});

module.exports = function (grunt) {

  var distPath = 'dist/'+packageJson.packageName,
      libsFile = 'js/libs/usergrid-libs.min.js',
      devFile = 'js/usergrid-dev.min.js',
      devFileIncludes= ['js/**/*.js','!js/libs/**/*.js', '!js/**/*.min.js'],
      mainFile = 'js/usergrid.min.js',
      templateFile = 'js/templates.js',
      distName = packageJson.packageName
      ;
  // Project configuration.
  grunt.initConfig({
    pkg: grunt.file.readJSON('package.json'),

    uglify: {
      options: {
        banner: '/*! <%= pkg.name %>@<%= pkg.version %> <%= grunt.template.today("yyyy-mm-dd") %> */\n'
      },
      'usergrid-libs':{
        options: {
          mangle: false,
          compress: {warnings:false},
          beautify: false
        },
        files:{
          'js/libs/usergrid-libs.min.js':[
            'js/libs/jquery/jquery-1.9.1.min.js',
            'js/libs/jquery/jquery-migrate-1.1.1.min.js',
            'js/libs/jquery/jquery.sparkline.min.js',
            'js/libs/Highcharts-2.3.5/js/highcharts.js',
            'js/libs/angular-1.2.5/angular.min.js',
            'js/libs/angular-1.2.5/angular-route.min.js',
            'js/libs/angular-1.2.5/angular-resource.min.js',
            'js/libs/angular-1.2.5/angular-sanitize.min.js',
            'js/libs/usergrid.sdk.js',
            'js/libs/MD5.min.js',
            'bower_components/angularitics/dist/angulartics.min.js',
            'bower_components/angularitics/dist/angulartics-google-analytics.min.js',
            'js/libs/ui-bootstrap/ui-bootstrap-custom-tpls-0.3.0.min.js',
            'js/libs/jqueryui/jquery-ui-1.8.18.min.js',
            'js/libs/jqueryui/date.min.js'
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
          'js/usergrid-dev.min.js': [
            'js/app.js',
            'js/**/*.js',
            '!js/config.js',
            '!js/libs/**/*.js',
            '!'+mainFile,
            '!'+devFile
          ]
        }
      },
      'usergrid': {
        options: {
          mangle: false,
          compress: {warnings:false},
          beautify: false
        },
        files: {
          'js/usergrid.min.js': [
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
        src: ['js/**/*.html','!**/index*'],
        dest: templateFile,
        options:  {
          url:    function(url) { return url.replace('js/', ''); }
        }
      }
    },

    cssmin: {
      combine: {
        files: {
          'css/dash.min.css': ['css/apigeeGlobalNavigation.css', 'css/main.css']
        }
      }
    },
    watch: {
      files: [
        'index-template.html',
        'css/**/*.css',
        'js/**/*.js',
        'js/**/*.html',
        '!tests/',
        '!archive/',
        '!css/dash.min.css',
        '!js/libs/',
        '!js/*.min.js',
        '!'+templateFile,
        '!'+libsFile
      ],
      tasks: ['build-dev']
    },
    connect: {
      server: {
        options: {
          target: 'http://localhost:3000/index-debug.html', // target url to open
          open: 'http://localhost:3000/index-debug.html',
          port: 3000,
          base: ''
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
      }
    },
    karma: {
      unit: {
        configFile: 'tests/karma.conf.js',
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
          baseUrl:'http://localhost:3005'
        }
      },
      phantom: {
        options: {
          args: {
            // Arguments passed to the command
            capabilities: {
              baseUrl:'http://localhost:3005',
              'browserName': 'phantomjs'
            }
          }
        }
      },
      chrome: {
        options: {
          args: {
            // Arguments passed to the command
            capabilities: {
              baseUrl:'http://localhost:3006',
              'browserName': 'chrome'
            }
          }
        }
      }
    },
    copy:{
      main:{
        files:[
          // includes files within path
          {expand: true, src: ['*.html','config.js', '*.ico','js/*.min.js'], dest: distPath, filter: 'isFile'},
          {expand: true, src: ['sdk/**','css/**','bower_components/**','img/**','js/app-overview/doc-includes/images/**','archive/**','js/libs/**','js/charts/*.json'], dest: distPath}

        ]
      }
    },
    compress: {
      main: {
        options: {
          archive: 'dist/'+distName+'.'+packageJson.version+'.zip'
        },
        expand: true,
        cwd: distPath+'/',
        src: ['**/*'],
        dest: distName+'.'+packageJson.version
      }
    },
    clean: {
        build: ['dist/','js/*.min.js',templateFile]
    },
    dom_munger: {
      main: {
        options: {
          append:{selector:'body',html:mainRefs},
          update: {selector:'#main-script',attribute:'src',value:mainFile}

        },
        src: 'index-template.html',  //update the dist/index.html (the src index.html is copied there)
        dest: 'index.html'  //update the dist/index.html (the src index.html is copied there)

      },
      dev: {
        options: {
          append:{selector:'body',html:devRefs},
          update: {selector:'#main-script',attribute:'src',value:devFile}
        },
        src: 'index-template.html',  //update the dist/index.html (the src index.html is copied there)
        dest: 'index-debug.html'  //update the dist/index.html (the src index.html is copied there)
      },
      menu: {
        options: {
          append:{selector:'#sideMenu',html:menu}
        },
        src: ['index.html','index-debug.html']  //update the dist/index.html (the src index.html is copied there)
      },
      css: {
        options: {
          append:{selector:'head',html:cssRefs}
        },
        src: ['index.html','index-debug.html']  //update the dist/index.html (the src index.html is copied there)
      }
    },
    bower: {
      install: {
        options:{
          cleanup:false,
          copy:false
        }
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

  // Default task(s).
  grunt.registerTask('dev', ['connect:server', 'watch']);

  grunt.registerTask('validate', ['jshint', 'complexity']);

  grunt.registerTask('build-dev', [ 'ngtemplates','uglify:usergrid-dev','uglify:usergrid', 'cssmin','dom_munger','karma:unit']);

  grunt.registerTask('default', ['build','karma:unit']);

  grunt.registerTask('e2e', ['karma:unit','connect:e2e-phantom','protractor:phantom']);
  grunt.registerTask('e2e-chrome', ['karma:unit','connect:e2e-chrome','protractor:chrome']);


  grunt.registerTask('no-monitoring', ['build','clean:perf','karma:unit','compress']);

  grunt.registerTask('build', ['clean:build','bower:install','ngtemplates', 'uglify','cssmin','dom_munger','copy','compress']);
};
