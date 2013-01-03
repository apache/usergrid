SKIP_ATTRS = %w(metadata uri type)

def format_result(response)
  if response.multiple_entities? && response.collection.size > 1
    format_collection(response.collection)
  else
    format_entity(response.entity)
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
          column index+1
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
    if collection.cursor && agree('Next Page? (Y/N)') {|q| q.default = 'Y'}
      format_collection(collection.next_page, headers)
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
  HighLine.new.output_cols
end