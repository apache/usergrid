require 'json'

def parse_sql(query)
  result = {}
  keywords = %w(select from where limit)
  current = nil
  query.downcase.split(/[\s,*]/).each do |ea|
    next if ea == '' || (current == 'select' && ea == 'distinct')
    if keywords.include? ea
      current = ea
    elsif current
      if current == 'select' and ea.start_with? "{"
        current = nil
        next
      end
      if result[current]
        if result[current].is_a? Array
          result[current] << ea
        else
          result[current] = [result[current]] << ea
        end
      else
        result[current] = (current == 'select') ? [ea] : ea
      end
    end
  end
  result
end

# returns a json string
def parse_data(input)
  return unless input
  # must be wrapped in {}
  input = "{#{input}}" unless input.start_with? '{' or input.start_with? '['
  # must be a json string or 1.9 hash format
  begin
    MultiJson.dump(eval(input))
  rescue SyntaxError
    input
  end
end
