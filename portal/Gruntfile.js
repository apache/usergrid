var packageJson = require('./package.json');
var userGrid = require('./config.js');
var  versionPath = packageJson.version;
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
  mainRefs += "<script src='" + versionPath+'/'+ current + "'></script>";
});
userGrid.options.scriptReferences.dev.forEach(function (current) {
  devRefs += "<script src='" + versionPath+'/'+ current + "'></script>";
});

var cssRefs = "";
userGrid.options.cssRefs.forEach(function(css){
  cssRefs += '<link href="'+versionPath+'/'+css.src+'" rel="stylesheet" id="'+css.id+'"/>';
});

console.warn('to run e2e tests you need to have a running instance of webdriver, 1) npm install protractor -g -> 2) webdriver-manager start --standalone');
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
       //   open: 'http://localhost:3000/index-debug.html',
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
            baseUrl:'http://localhost:3005',
            // Arguments passed to the command
            'browser': 'phantomjs'
          }
        }
      },
      chrome: {
        options: {
          args: {
            baseUrl:'http://localhost:3006',
            // Arguments passed to the command
            'browser': 'chrome'
          }
        }
      },
      prod: {
        options: {
          args: {
            baseUrl:'http://apigee.com/usergrid',
            // Arguments passed to the command
            browser: 'chrome',
            params:{
              useSso:true
            }
          }
        }
      },
      mars:{
        options:{
          args:{
            baseUrl:'http://appservices.apigee.com/mars/',
            browser: 'chrome',
            params:{
              useSso:true,
              orgName:'apijeep'
            }
          }
        }
      },
      firefox: {
        options: {
          args: {
            baseUrl:'http://localhost:3007',
            // Arguments passed to the command
            'browser': 'firefox'
          }
        }
      }
    },
    copy:{
      versioned:{
        files:[
          {src:['js/*.min.js','js/libs/**','css/**','img/**','bower_components/**'],dest:versionPath,expand:true}
        ]
      },
      main:{
        files:[
          // includes files within path
          {expand: true, src: ['*.html','config.js', '*.ico'], dest: distPath, filter: 'isFile'},
          {expand: true, src: [versionPath+'/**','sdk/**','css/**','img/**' ,'archive/**','js/charts/*.json'], dest: distPath}

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
        build: ['dist/','js/*.min.js',templateFile,versionPath+'/']//'bower_components/',
    },
    dom_munger: {
      main: {
        options: {
          append:{selector:'body',html:mainRefs},
          update: {selector:'#main-script',attribute:'src',value:versionPath+'/'+mainFile}

        },
        src: 'index-template.html',  //update the dist/index.html (the src index.html is copied there)
        dest: 'index.html'  //update the dist/index.html (the src index.html is copied there)

      },
      dev: {
        options: {
          append:{selector:'body',html:devRefs},
          update: {selector:'#main-script',attribute:'src',value:versionPath+'/'+devFile}
        },
        src: 'index-template.html',  //update the dist/index.html (the src index.html is copied there)
        dest: 'index-debug.html'  //update the dist/index.html (the src index.html is copied there)
      },
      menu: {
        options: {
          callback:function($){
            $('#sideMenu').append(menu);
            $('head').append(cssRefs);
            var libs = $('#libScript');
            for(var key in libs){
              var elem = libs[key];
              if(elem.attribs){
                if (elem.attribs.src) {
                  elem.attribs.src = versionPath + '/' + elem.attribs.src;
                }
                if (elem.attribs.href) {
                  elem.attribs.href = versionPath + '/' + elem.attribs.href;
                }
              }
            };
          }
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

  grunt.registerTask('build-dev', [ 'ngtemplates','uglify:usergrid-dev','uglify:usergrid', 'cssmin','dom_munger','copy:versioned','karma:unit']);

  grunt.registerTask('default', ['build','karma:unit']);

  grunt.registerTask('e2e', ['connect:e2e-phantom','protractor:phantom']);
  grunt.registerTask('e2e-chrome', ['connect:e2e-chrome','protractor:chrome']);
  grunt.registerTask('e2e-firefox', ['connect:e2e-firefox','protractor:firefox']);
  grunt.registerTask('e2e-prod', ['protractor:prod']);
  grunt.registerTask('e2e-mars', ['protractor:mars']);


  grunt.registerTask('no-monitoring', ['build','clean:perf','karma:unit','compress']);

  grunt.registerTask('build', ['clean:build','bower:install','ngtemplates', 'uglify','cssmin','dom_munger','copy','compress']);
};
