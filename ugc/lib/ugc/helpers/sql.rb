def parse_sql(query)
  result = {}
  keywords = %w(select from where)
  current = nil
  query.downcase.split(/[\s,*]/).each do |ea|
    next if ea == ''
    if keywords.include? ea
      current = ea
    elsif current
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
