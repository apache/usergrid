module.exports = function(grunt) {
  var files = ['lib/Usergrid.js', 'lib/Client.js', 'lib/Entity.js', 'lib/Collection.js', 'lib/Group.js', 'lib/Event.js'];
  var tests = [ 'tests/mocha/index.html','tests/mocha/test_*.html' ];
   // Project configuration.
  grunt.initConfig({
    //pkg: grunt.file.readJSON('package.json'),
    meta: {
      package: grunt.file.readJSON('package.json'),
    },
    clean: ['usergrid.js', 'usergrid.min.js'],
    uglify: {
      build: {
        options: {
          banner: '/*! <%= meta.package.name %>@<%= meta.package.version %> <%= grunt.template.today("yyyy-mm-dd") %> */\n',
            mangle: false,
            compress: false,
            beautify: true,
            preserveComments: "all"
        },
        files: {
          'usergrid.js': files
        }
      },
      buildmin: {
        options: {
          banner: '/*! <%= meta.package.name %>@<%= meta.package.version %> <%= grunt.template.today("yyyy-mm-dd") %> */\n',
            mangle: false,
            compress: true,
            beautify: false,
            preserveComments: "some"
        },
        files: {
          'usergrid.min.js': files
        }
      }
    },
    connect: {
      server: {
        options: {
          port: 3000,
          base: '.'
        }
      },
      test: {
        options: {
          port: 8000,
          base: '.'
        }
      }
    },
    watch : {
      files : files,
      tasks : ['default']
    },
    blanket_mocha: {
      all: tests,
      //urls: [ 'http://localhost:8000/tests/mocha/index.html' ],
      options: {
          dest: 'report/coverage.html',
          reporter: 'Spec',
          threshold: 70
      }
    },
  });
  grunt.loadNpmTasks('grunt-contrib-clean');
  grunt.loadNpmTasks('grunt-contrib-uglify');
  grunt.loadNpmTasks('grunt-contrib-watch'); 
  grunt.loadNpmTasks('grunt-contrib-connect');
  grunt.loadNpmTasks('grunt-blanket-mocha');
grunt.registerTask('default', [
    'clean',
    'uglify'
  ]);
  grunt.registerTask('dev', [
  	'connect:server',
    'watch'
  ]);
  grunt.registerTask('test', [
    'connect:test',
    'blanket_mocha',
  ]);
};

