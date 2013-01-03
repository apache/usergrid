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
    metadata = collection_metadata collection, headers
    table border: true do
      row header: true do
        headers ||= metadata.keys
        column '#', width: INDEX_COL_WIDTH
        headers.each {|header| column header, width: metadata[header][:size] }
      end
      collection.each_with_index do |entity, index|
        row do
          column index+1
          if entity.is_a? Array
            entity.each {|v| column v }
          else
            headers.each do |header|
              column entity[header]
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

# return hash { column_name: { max_size: 12, values: [] }  }
def collection_metadata(collection, headers=nil)
  result = {}
  collection.each do |entity|
    if entity.is_a? Array
      headers.each_with_index do |header, index|
        col = result[header] ||= {}
        size = entity[index].to_s.size
        col[:max_size] = col[:max_size] ? [col[:max_size], size].max : size
      end
    else
      entity.reject{|k,v| headers ? !headers.include?(k) : SKIP_ATTRS.include?(k)}.each do |k,v|
        col = result[k] ||= {}
        size = v.to_s.size
        col[:max_size] = col[:max_size] ? [col[:max_size], size].max : size
      end
    end
  end
  total_size = result.inject(0) do |total, (col,meta)|
    meta[:max_size] = [col.size, meta[:max_size]].max
    total += meta[:max_size]
  end
  terminal_columns = HighLine.new.output_cols
  overhead = (result.keys.size + 2) * COL_OVERHEAD + INDEX_COL_WIDTH
  if total_size + overhead < terminal_columns
    result.each {|col,meta| meta[:size] = meta[:max_size]}
  else
    col_size = (terminal_columns - overhead) / result.keys.size
    result.each {|col,meta| meta[:size] = col_size}
  end
  result
end
