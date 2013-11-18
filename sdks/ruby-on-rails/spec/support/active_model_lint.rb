# spec/support/active_model_lint.rb
# adapted from rspec-rails: http://github.com/rspec/rspec-rails/blob/master/spec/rspec/rails/mocks/mock_model_spec.rb

shared_examples_for "ActiveModel" do
  include ActiveModel::Lint::Tests

  # to_s is to support ruby-1.9
  ActiveModel::Lint::Tests.public_instance_methods.map{|m| m.to_s}.grep(/^test/).each do |m|
    example m.sub('_',' ') do
      send m
    end
  end

  def model
    subject
  end
end
