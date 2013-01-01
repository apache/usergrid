SKIP_ATTRS = %w(metadata uri type)

def format_result(result)
  if result.multiple_entities? && result.collection.size > 1
    format_collection(result.collection)
  else
    format_entity(result.entity)
  end
end

INDEX_COL_WIDTH = 2
COL_OVERHEAD = 3

def format_collection(collection, headers=nil)
  if collection && collection.size > 0
    table border: true do
      row header: true do
        headers ||= collection.first.keys.reject{|e| SKIP_ATTRS.include? e}
        column '#', width: INDEX_COL_WIDTH
        headers.each do |r|
          column r, width: equal_column_size(headers.size)
        end
      end
      collection.each_with_index do |entity, index|
        row do
          column index
          if entity.is_a? Array
            entity.each do |v|
              column v
            end
          else
            entity.reject{|k,v| SKIP_ATTRS.include? k}.each_value do |v|
              column v
            end
          end
        end
      end
    end
  else
    puts "0 results"
  end
end

def format_entity(entity)
  if entity
    table border: true do
      row header: true do
        column 'name', width: 20
        column 'value', width: (terminal_columns - 28)
      end
      entity.data.reject{|k,v| SKIP_ATTRS.include? k}.each do |k,v|
        row do
          column(k)
          column(v)
        end
      end
    end
  else
    puts "no data"
  end
end

def equal_column_size(num_cols)
  ((terminal_columns - COL_OVERHEAD - (INDEX_COL_WIDTH + COL_OVERHEAD)) / num_cols).to_i - COL_OVERHEAD
end

def terminal_columns
  size = detect_terminal_size
  size ? size[0] : 80
end

# Returns [width, height] of terminal when detected, nil if not detected.
# Think of this as a simpler version of Highline's Highline::SystemExtensions.terminal_size()
def detect_terminal_size
  if (ENV['COLUMNS'] =~ /^\d+$/) && (ENV['LINES'] =~ /^\d+$/)
    [ENV['COLUMNS'].to_i, ENV['LINES'].to_i]
  elsif (RUBY_PLATFORM =~ /java/ || (!STDIN.tty? && ENV['TERM'])) && command_exists?('tput')
    [`tput cols`.to_i, `tput lines`.to_i]
  elsif STDIN.tty? && command_exists?('stty')
    `stty size`.scan(/\d+/).map { |s| s.to_i }.reverse
  else
    nil
  end
rescue
  nil
end

# Determines if a shell command exists by searching for it in ENV['PATH'].
def command_exists?(command)
  ENV['PATH'].split(File::PATH_SEPARATOR).any? {|d| File.exists? File.join(d, command) }
end
