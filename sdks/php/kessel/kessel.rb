# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

################################################################################
#
#  Kessel - the fastest way to accurate docs!
#
#
################################################################################

config_filename = 'config.ini'

directories = {}
directories ['tests_dir'] = 'tests2'
directories ['docs_dir']  = 'docs3'
directories ['output_dir']  = 'output4'

docs_dir_signifier = '@docs-dir'
tests_dir_signifier = '@tests-dir'
output_dir_signifier = '@output-dir'

start_tag = '@han'
end_tag = '@solo'
hole = '@yoda'

excludes = ["$tester->success", "$tester->error(", "$tester->notice("]
codeblocks = {}

def excludeLine(line, excludeList)
  #check to see the line matches anything in the exclude array
  result = false
  excludeList.each { |index|
    if line.include? index
      result = true
    end
  }
  return result;
end

def loadCodeBlocks(filename, start_tag, end_tag, codeblocks, excludes)
  tag = ''
  output = false
  puts "parsing #{filename}";
  IO.foreach(filename) do |line|
    if line.include? start_tag
      #check to make sure we are not already in a tag
      if output
        puts "Error - new start tag (@han) found before end tag (@solo)!"
      end
      output = true
      #get code descriptor from line
      tag = line[/\{.*?\}/]
      if  codeblocks[tag]
        puts "Error - duplicate tag detected #{tag}"
      else
        codeblocks[tag] = ''
      end
    elsif line.include? end_tag
      #check to make sure we are in a tag
      if !output
        puts "Error - new end tag (@solo) found before start tag (@han)!"
      end
      output = false
    elsif output == true
      #grab each line inside the tag and add it to the code block
      if !excludeLine(line, excludes)
	 #puts "code block found: #{line}"
	 #add a \t tab character to the beginning of each line for markdown files
        codeblocks[tag] = codeblocks[tag] +  "\t #{line}"
      end
    end
  end
  return codeblocks
end

def writeOutput(outfilename, infilename, hole, codeblocks)
  out_file = File.open(outfilename, 'w')
  IO.foreach(infilename) do |line|
    if line.include? hole
      output = true
      #get code descriptor from line
      tag = line[/\{.*?\}/]
      codeblock =  codeblocks[tag]
      out_file.puts codeblock
    else
      out_file.puts line
    end
  end
  out_file.close
end

def loadCodeFilesList(filename)
  testFilenames = {}
  IO.foreach(filename) do |line|
  	next if filename == '.DS_Store'
    #make sure line is not empty (2 or less chars), and not a comment (starts with a #)
    if line.length > 2 && line[0].chr != "#"
      testFilenames[line] = line
    end
  end
  return testFilenames
end


def parseCodeBlocksInDir(dir, start_tag, end_tag, codeblocks, excludes)
  Dir.foreach(dir) do
    |f|
    if f != '.' && f != '..'
      puts "dir=" +f
      path = dir + '/' +f
#      puts "getting code blocks from #{path}"
      codeblocks = loadCodeBlocks(path, start_tag, end_tag, codeblocks, excludes)
    end
  end
  return codeblocks
end


def parseDocsInDir(outdir, indir, hole, codeblocks)
  Dir.foreach(indir) do
    |f|
    if f != '.' && f != '..'
      outfilename = outdir + '/' + f
      infilename = indir +'/'+ f
      puts "reading #{infilename}, writing #{outfilename}"
      writeOutput(outfilename, infilename, hole, codeblocks)
    end
  end
  return codeblocks
end


def parseConfig(filename, docs_dir_signifier, tests_dir_signifier, output_dir_signifier, directories)
  IO.foreach(filename) do |line|
    line.strip!
    puts "read config line:" + line
    if line.include? docs_dir_signifier
      puts "found " + line
      line.slice! docs_dir_signifier;
      line.strip!
      directories['docs_dir'] = line
    elsif line.include? tests_dir_signifier
      line.slice! tests_dir_signifier;
      line.strip!
      directories['tests_dir'] = line
    elsif line.include? output_dir_signifier
      line.slice! output_dir_signifier;
      line.strip!
      directories['output_dir'] = line
    end
  end
  return directories
end


directories = parseConfig(config_filename, docs_dir_signifier, tests_dir_signifier, output_dir_signifier, directories)
codeblocks = parseCodeBlocksInDir(directories['tests_dir'], start_tag, end_tag, codeblocks, excludes)
parseDocsInDir(directories['output_dir'], directories['docs_dir'], hole, codeblocks)


def parseCodeBlocksInFilesByConfig(config_filename, start_tag, end_tag, codeblocks, excludes)
  filename.each { |key, value|
    value.strip!
    path = 'tests/' + value
    codeblocks = loadCodeBlocks(path, start_tag, end_tag, codeblocks, excludes)
  }
end
#parseCodeBlocksInFiles(test_file_names, start_tag, end_tag, codeblocks, excludes)







