
# Licensed to the Apache Software Foundation (ASF) under one or more contributor
# license agreements.  See the NOTICE.txt file distributed with this work for
# additional information regarding copyright ownership.  The ASF licenses this
# file to you under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License.  You may obtain a copy of
# the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
# License for the specific language governing permissions and limitations under
# the License.

SKIP_ATTRS = %w(metadata uri type)
INDEX_COL_WIDTH = 2

def format_response(response)
  if response.multiple_entities? && response.collection.size > 1
    format_collection response.collection
  else
    format_entity response.entity
  end
end

def col_overhead
  $settings.table_border? ? 3 : 1
end

def format_collection(collection, headers=nil)
  if collection && collection.size > 0
    save_headers = headers ? headers.clone : nil
    save_response collection.response
    metadata = collection_metadata collection, headers
    table border: $settings.table_border? do
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
            headers.each {|header| column entity[header]}
          end
        end
      end
    end
    if collection.cursor && agree('Next Page? (Y/N)') {|q| q.default = 'Y'}
      format_collection(collection.next_page, save_headers)
    end
  else
    puts "0 results"
  end
end

def format_entity(entity)
  if entity
    name_cols = value_cols = 0
    entity.data.reject{|k,v| SKIP_ATTRS.include? k}.each do |k,v|
      name_cols = [name_cols, k.size].max
      value_cols = [value_cols, v.to_s.size].max
    end
    table border: $settings.table_border? do
      row header: true do
        name_width = [name_cols, 20].min
        column 'name', width: name_width
        column 'value', width: [value_cols, HighLine.new.output_cols - name_width - (3 * col_overhead)].min
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

# return hash { column_name: { max_size: 12, size: 12 }  }
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
    total + meta[:max_size]
  end
  terminal_columns = HighLine.new.output_cols
  overhead = (result.keys.size + 2) * col_overhead + INDEX_COL_WIDTH
  if total_size + overhead < terminal_columns
    result.each {|col,meta| meta[:size] = meta[:max_size]}
  else
    col_size = (terminal_columns - overhead) / result.keys.size
    result.each {|col,meta| meta[:size] = col_size}
  end
  result
end
