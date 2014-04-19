module.exports = function(grunt) {

    // Project configuration.
    grunt.initConfig({
        pkg: grunt.file.readJSON('package.json'),
        uglify: {
            options: {
                banner: '/*! <%= pkg.name %> <%= grunt.template.today("yyyy-mm-dd") %> */\n'
            },
            build: {
                src: 'src/angular-intro.js',
                dest: 'build/angular-intro.min.js'
            }
        },
        jshint: {
            lib: {
                options: {},
                src: ['lib/*.js']
            },
        },
        watch: {
            scripts: {
                files: 'lib/*.js',
                tasks: ['jshint', 'uglify'],
                options: {
                    interrupt: true
                },
            },
            gruntfile: {
                files: 'Gruntfile.js'
            }
        },
    });

    // Load all grunt tasks
    require('load-grunt-tasks')(grunt);

    // Default task(s).
    grunt.registerTask('default', ['jshint', 'uglify']);
};
