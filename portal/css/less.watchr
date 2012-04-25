# Run me with:
#
#   $ watchr less.watchr

# --------------------------------------------------
# Helpers
# --------------------------------------------------
def lessc(file)
  print "compiling #{file.inspect}... "
  system "lessc #{file}"
  puts 'done'
end

# --------------------------------------------------
# Watchr Rules
# --------------------------------------------------
watch ( '.*\.less$' ) {lessc 'usergrid.less > usergrid.css' }

# --------------------------------------------------
# Signal Handling
# --------------------------------------------------
# Ctrl-\
Signal.trap('QUIT') do
  puts " --- Compiling all .less files ---\n\n"
  Dir['**/*.less'].each {|file| lessc file }
  puts 'all compiled'
end

# Ctrl-C
Signal.trap('INT')  { abort("\n") }