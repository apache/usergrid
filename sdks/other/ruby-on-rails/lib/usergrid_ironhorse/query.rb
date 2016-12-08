# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# http://guides.rubyonrails.org/active_record_querying.html

module Usergrid
  module Ironhorse

    class Query

      RecordNotFound = ActiveRecord::RecordNotFound

      def initialize(model_class)
        @model_class = model_class
        @options = {}
      end

      ## Initializes new record from relation while maintaining the current
      ## scope.
      ##
      ## Expects arguments in the same format as +Base.new+.
      ##
      ##   users = User.where(name: 'DHH')
      ##   user = users.new # => #<User id: nil, name: "DHH", created_at: nil, updated_at: nil>
      ##
      ## You can also pass a block to new with the new record as argument:
      ##
      ##   user = users.new { |user| user.name = 'Oscar' }
      ##   user.name # => Oscar
      #def new(*args, &block)
      #  scoping { @model_class.new(*args, &block) }
      #end

      # Find by uuid or name - This can either be a specific uuid or name (1), a list of uuids
      # or names (1, 5, 6), or an array of uuids or names ([5, 6, 10]).
      # If no record can be found for all of the listed ids, then RecordNotFound will be raised.
      #
      #   Person.find(1)       # returns the object for ID = 1
      #   Person.find("1")     # returns the object for ID = 1
      #   Person.find(1, 2, 6) # returns an array for objects with IDs in (1, 2, 6)
      #   Person.find([7, 17]) # returns an array for objects with IDs in (7, 17)
      #   Person.find([1])     # returns an array for the object with ID = 1
      #   Person.where("administrator = 1").order("created_on DESC").find(1)
      #
      def find(*ids)
        raise RecordNotFound unless ids
        ids = ids.first if ids.first.is_a? Array
        @records = ids.collect { |id| find_one! id } # todo: can this be optimized in one call?
        #entities = @model_class.resource[ids.join '&'].get.entities
        #raise RecordNotFound unless (entities.size == ids.size)
        #@records = entities.collect {|entity| @model_class.model_name.constantize.new(entity.data) }
        @records.size == 1 ? @records.first : @records
      end

      # Finds the first record matching the specified conditions. There
      # is no implied ordering so if order matters, you should specify it
      # yourself.
      #
      # If no record is found, returns <tt>nil</tt>.
      #
      #   Post.find_by name: 'Spartacus', rating: 4
      #   Post.find_by "published_at < ?", 2.weeks.ago
      def find_by(*conditions)
        where(*conditions).take
      end

      # Like <tt>find_by</tt>, except that if no record is found, raises
      # an <tt>ActiveRecord::RecordNotFound</tt> error.
      def find_by!(*conditions)
        where(*conditions).take!
      end

      # Gives a record (or N records if a parameter is supplied) without any implied
      # order.
      #
      #   Person.take # returns an object fetched by SELECT * FROM people
      #   Person.take(5) # returns 5 objects fetched by SELECT * FROM people LIMIT 5
      #   Person.where(["name LIKE '%?'", name]).take
      def take(limit=1)
        limit(limit).to_a
      end

      # Same as +take+ but raises <tt>ActiveRecord::RecordNotFound</tt> if no record
      # is found. Note that <tt>take!</tt> accepts no arguments.
      def take!
        take or raise RecordNotFound
      end

      # Find the first record (or first N records if a parameter is supplied).
      # If no order is defined it will order by primary key.
      #
      #   Person.first # returns the first object fetched by SELECT * FROM people
      #   Person.where(["user_name = ?", user_name]).first
      #   Person.where(["user_name = :u", { :u => user_name }]).first
      #   Person.order("created_on DESC").offset(5).first
      #   Person.first(3) # returns the first three objects fetched by SELECT * FROM people LIMIT 3
      def first(limit=1)
        limit(limit).load.first
      end

      # Same as +first+ but raises <tt>ActiveRecord::RecordNotFound</tt> if no record
      # is found. Note that <tt>first!</tt> accepts no arguments.
      def first!
        first or raise RecordNotFound
      end

      # Find the last record (or last N records if a parameter is supplied).
      # If no order is defined it will order by primary key.
      #
      #   Person.last # returns the last object fetched by SELECT * FROM people
      #   Person.where(["user_name = ?", user_name]).last
      #   Person.order("created_on DESC").offset(5).last
      #   Person.last(3) # returns the last three objects fetched by SELECT * FROM people.
      #
      # Take note that in that last case, the results are sorted in ascending order:
      #
      #   [#<Person id:2>, #<Person id:3>, #<Person id:4>]
      #
      # and not:
      #
      #   [#<Person id:4>, #<Person id:3>, #<Person id:2>]
      def last(limit=1)
        limit(limit).reverse_order.load.first
      end

      # Same as +last+ but raises <tt>ActiveRecord::RecordNotFound</tt> if no record
      # is found. Note that <tt>last!</tt> accepts no arguments.
      def last!
        last or raise RecordNotFound
      end

      def each
        to_a.each { |*block_args| yield(*block_args) }
        while @response.data['cursor'] && !limit_value
          next_page
          to_a.each { |*block_args| yield(*block_args) }
        end
      end

      def next_page
        @options[:cursor] = @response.data['cursor']
        @records = nil
        load
        self
      end

      # Returns +true+ if a record exists in the table that matches the +id+ or
      # conditions given, or +false+ otherwise. The argument can take six forms:
      #
      # * String - Finds the record with a primary key corresponding to this
      #   string (such as <tt>'5'</tt>).
      # * Array - Finds the record that matches these +find+-style conditions
      #   (such as <tt>['color = ?', 'red']</tt>).
      # * Hash - Finds the record that matches these +find+-style conditions
      #   (such as <tt>{color: 'red'}</tt>).
      # * +false+ - Returns always +false+.
      # * No args - Returns +false+ if the table is empty, +true+ otherwise.
      #
      # For more information about specifying conditions as a Hash or Array,
      # see the Conditions section in the introduction to ActiveRecord::Base.
      def exists?(conditions=nil)
        # todo: does not yet handle all conditions described above
        case conditions
          when Array, Hash
            pluck :uuid
            !where(conditions).take.empty?
          else
            !!find_one(conditions)
        end
      end
      alias_method :any?, :exists?
      alias_method :many?, :exists?

      def limit(limit=1)
        @options[:limit] = limit
        self
      end

      def offset(num)
        @options[:offset] = num
        self
      end

      # Removes from the query the condition(s) specified in +skips+.
      #
      # Example:
      #
      #   Post.order('id asc').except(:order)                  # discards the order condition
      #   Post.where('id > 10').order('id asc').except(:where) # discards the where condition but keeps the order
      #
      def except(*skips)
        skips.each {|option| @options.delete option}
      end

      # Removes any condition from the query other than the one(s) specified in +onlies+.
      #
      # Example:
      #
      #   Post.order('id asc').only(:where)         # discards the order condition
      #   Post.order('id asc').only(:where, :order) # uses the specified order
      #
      def only(*onlies)
        @options.keys do |k|
          unless onlines.include? k
            @options.delete k
          end
        end
      end

      # Allows to specify an order attribute:
      #
      #   User.order('name')
      #   => SELECT "users".* FROM "users" ORDER BY name
      #
      #   User.order('name DESC')
      #   => SELECT "users".* FROM "users" ORDER BY name DESC
      #
      #   User.order('name DESC, email')
      #   => SELECT "users".* FROM "users" ORDER BY name DESC, email
      def order(*args)
        @options[:order] << args
      end

      # Replaces any existing order defined on the relation with the specified order.
      #
      #   User.order('email DESC').reorder('id ASC') # generated SQL has 'ORDER BY id ASC'
      #
      # Subsequent calls to order on the same relation will be appended. For example:
      #
      #   User.order('email DESC').reorder('id ASC').order('name ASC')
      #
      # generates a query with 'ORDER BY name ASC, id ASC'.
      def reorder(*args)
        @options[:order] = args
      end

      def all
        @options[:conditions] = nil
        self
      end

      # Works in two unique ways.
      #
      # First: takes a block so it can be used just like Array#select.
      #
      #   Model.all.select { |m| m.field == value }
      #
      # This will build an array of objects from the database for the scope,
      # converting them into an array and iterating through them using Array#select.
      #
      # Second: Modifies the SELECT statement for the query so that only certain
      # fields are retrieved:
      #
      #   Model.select(:field)
      #   # => [#<Model field:value>]
      #
      # Although in the above example it looks as though this method returns an
      # array, it actually returns a relation object and can have other query
      # methods appended to it, such as the other methods in ActiveRecord::QueryMethods.
      #
      # The argument to the method can also be an array of fields.
      #
      #   Model.select(:field, :other_field, :and_one_more)
      #   # => [#<Model field: "value", other_field: "value", and_one_more: "value">]
      #
      def select(*fields)
        if block_given?
          to_a.select { |*block_args| yield(*block_args) }
        else
          raise ArgumentError, 'Call this with at least one field' if fields.empty?
          clone.select!(*fields)
        end
      end

      # Like #select, but modifies relation in place.
      def select!(*fields)
        @options[:select] ||= fields.join ','
        self
      end
      alias_method :pluck, :select!

      def reverse_order
        @options[:reversed] = true
        self
      end

      def readonly
        @options[:readonly] = true
        self
      end

      def to_a
        load
        @records
      end

      def as_json(options = nil) #:nodoc:
        to_a.as_json(options)
      end

      # Returns size of the results (not size of the stored collection)
      def size
        loaded? ? @records.length : count
      end

      # true if there are no records
      def empty?
        return @records.empty? if loaded?

        c = count
        c.respond_to?(:zero?) ? c.zero? : c.empty?
      end

      # true if there are any records
      def any?
        if block_given?
          to_a.any? { |*block_args| yield(*block_args) }
        else
          !empty?
        end
      end

      # true if there is more than one record
      def many?
        if block_given?
          to_a.many? { |*block_args| yield(*block_args) }
        else
          limit_value ? to_a.many? : size > 1
        end
      end

      # find_all_by_
      # find_by_
      # find_first_by_
      # find_last_by_
      def method_missing(method_name, *args)

        method = method_name.to_s

        if method.end_with? '!'
          method.chop!
          error_on_empty = true
        end

        if method.start_with? 'find_all_by_'
          attribs = method.gsub /^find_all_by_/, ''
        elsif method.start_with? 'find_by_'
          attribs = method.gsub /^find_by_/, ''
          limit(1)
        elsif method.start_with? 'find_first_by_'
          limit(1)
          find_first = true
          attribs = method.gsub /^find_first_by_/, ''
        elsif method.start_with? 'find_last_by_'
          limit(1)
          find_last = true
          attribs = method.gsub /^find_last_by_/, ''
        else
          super
        end

        attribs = attribs.split '_and_'
        conditions = {}
        attribs.each { |attr| conditions[attr] = args.shift }

        where(conditions, *args)
        load
        raise RecordNotFound if error_on_empty && @records.empty?
        return @records.first if limit_value == 1
        @records
      end

      # Tries to create a new record with the same scoped attributes
      # defined in the relation. Returns the initialized object if validation fails.
      #
      # Expects arguments in the same format as +Base.create+.
      #
      # ==== Examples
      #   users = User.where(name: 'Oscar')
      #   users.create # #<User id: 3, name: "oscar", ...>
      #
      #   users.create(name: 'fxn')
      #   users.create # #<User id: 4, name: "fxn", ...>
      #
      #   users.create { |user| user.name = 'tenderlove' }
      #   # #<User id: 5, name: "tenderlove", ...>
      #
      #   users.create(name: nil) # validation on name
      #   # #<User id: nil, name: nil, ...>
      def create(*args, &block)
        @model_class.create(*args, &block)
      end

      # Similar to #create, but calls +create!+ on the base class. Raises
      # an exception if a validation error occurs.
      #
      # Expects arguments in the same format as <tt>Base.create!</tt>.
      def create!(*args, &block)
        @model_class.create!(*args, &block)
      end

      # Tries to load the first record; if it fails, then <tt>create</tt> is called with the same arguments as this method.
      #
      # Expects arguments in the same format as +Base.create+.
      #
      # ==== Examples
      #   # Find the first user named Penélope or create a new one.
      #   User.where(:first_name => 'Penélope').first_or_create
      #   # => <User id: 1, first_name: 'Penélope', last_name: nil>
      #
      #   # Find the first user named Penélope or create a new one.
      #   # We already have one so the existing record will be returned.
      #   User.where(:first_name => 'Penélope').first_or_create
      #   # => <User id: 1, first_name: 'Penélope', last_name: nil>
      #
      #   # Find the first user named Scarlett or create a new one with a particular last name.
      #   User.where(:first_name => 'Scarlett').first_or_create(:last_name => 'Johansson')
      #   # => <User id: 2, first_name: 'Scarlett', last_name: 'Johansson'>
      #
      #   # Find the first user named Scarlett or create a new one with a different last name.
      #   # We already have one so the existing record will be returned.
      #   User.where(:first_name => 'Scarlett').first_or_create do |user|
      #     user.last_name = "O'Hara"
      #   end
      #   # => <User id: 2, first_name: 'Scarlett', last_name: 'Johansson'>
      def first_or_create(attributes={}, &block)
        result = first
        unless result
          attributes = @options[:hash].merge(attributes) if @options[:hash]
          result = create(attributes, &block)
        end
        result
      end

      # Like <tt>first_or_create</tt> but calls <tt>create!</tt> so an exception is raised if the created record is invalid.
      #
      # Expects arguments in the same format as <tt>Base.create!</tt>.
      def first_or_create!(attributes={}, &block)
        result = first
        unless result
          attributes = @options[:hash].merge(attributes) if @options[:hash]
          result = create!(attributes, &block)
        end
        result
      end

      # Like <tt>first_or_create</tt> but calls <tt>new</tt> instead of <tt>create</tt>.
      #
      # Expects arguments in the same format as <tt>Base.new</tt>.
      def first_or_initialize(attributes={}, &block)
        result = first
        unless result
          attributes = @options[:hash].merge(attributes) if @options[:hash]
          result = @model_class.new(attributes, &block)
        end
        result
      end

      # Destroys the records matching +conditions+ by instantiating each
      # record and calling its +destroy+ method. Each object's callbacks are
      # executed (including <tt>:dependent</tt> association options and
      # +before_destroy+/+after_destroy+ Observer methods). Returns the
      # collection of objects that were destroyed; each will be frozen, to
      # reflect that no changes should be made (since they can't be
      # persisted).
      #
      # Note: Instantiation, callback execution, and deletion of each
      # record can be time consuming when you're removing many records at
      # once. It generates at least one SQL +DELETE+ query per record (or
      # possibly more, to enforce your callbacks). If you want to delete many
      # rows quickly, without concern for their associations or callbacks, use
      # +delete_all+ instead.
      #
      # ==== Parameters
      #
      # * +conditions+ - A string, array, or hash that specifies which records
      #   to destroy. If omitted, all records are destroyed. See the
      #   Conditions section in the introduction to ActiveRecord::Base for
      #   more information.
      #
      # ==== Examples
      #
      #   Person.destroy_all("last_login < '2004-04-04'")
      #   Person.destroy_all(status: "inactive")
      #   Person.where(:age => 0..18).destroy_all
      def destroy_all(conditions=nil)
        if conditions
          where(conditions).destroy_all
        else
          to_a.each {|object| object.destroy}
          @records
        end
      end

      # Destroy an object (or multiple objects) that has the given id. The object is instantiated first,
      # therefore all callbacks and filters are fired off before the object is deleted. This method is
      # less efficient than ActiveRecord#delete but allows cleanup methods and other actions to be run.
      #
      # This essentially finds the object (or multiple objects) with the given id, creates a new object
      # from the attributes, and then calls destroy on it.
      #
      # ==== Parameters
      #
      # * +id+ - Can be either an Integer or an Array of Integers.
      #
      # ==== Examples
      #
      #   # Destroy a single object
      #   Foo.destroy(1)
      #
      #   # Destroy multiple objects
      #   foos = [1,2,3]
      #   Foo.destroy(foos)
      def destroy(id)
        if id.is_a?(Array)
          id.map {|one_id| destroy(one_id)}
        else
          find(id).destroy
        end
      end

      # Deletes the records matching +conditions+ without instantiating the records
      # first, and hence not calling the +destroy+ method nor invoking callbacks. This
      # is a single SQL DELETE statement that goes straight to the database, much more
      # efficient than +destroy_all+. Be careful with relations though, in particular
      # <tt>:dependent</tt> rules defined on associations are not honored. Returns the
      # number of rows affected.
      #
      #   Post.delete_all("person_id = 5 AND (category = 'Something' OR category = 'Else')")
      #   Post.delete_all(["person_id = ? AND (category = ? OR category = ?)", 5, 'Something', 'Else'])
      #   Post.where(:person_id => 5).where(:category => ['Something', 'Else']).delete_all
      #
      # Both calls delete the affected posts all at once with a single DELETE statement.
      # If you need to destroy dependent associations or call your <tt>before_*</tt> or
      # +after_destroy+ callbacks, use the +destroy_all+ method instead.
      #
      # If a limit scope is supplied, +delete_all+ raises an ActiveRecord error:
      #
      #   Post.limit(100).delete_all
      #   # => ActiveRecord::ActiveRecordError: delete_all doesn't support limit scope
      def delete_all(conditions=nil)
        raise ActiveRecordError.new("delete_all doesn't support limit scope") if self.limit_value

        if conditions
          where(conditions).delete_all
        else
          pluck :uuid
          response = load
          response.each {|entity| entity.delete} # todo: can this be optimized into one call?
          response.size
        end
      end

      # Deletes the row with a primary key matching the +id+ argument, using a
      # SQL +DELETE+ statement, and returns the number of rows deleted. Active
      # Record objects are not instantiated, so the object's callbacks are not
      # executed, including any <tt>:dependent</tt> association options or
      # Observer methods.
      #
      # You can delete multiple rows at once by passing an Array of <tt>id</tt>s.
      #
      # Note: Although it is often much faster than the alternative,
      # <tt>#destroy</tt>, skipping callbacks might bypass business logic in
      # your application that ensures referential integrity or performs other
      # essential jobs.
      #
      # ==== Examples
      #
      #   # Delete a single row
      #   Foo.delete(1)
      #
      #   # Delete multiple rows
      #   Foo.delete([2,3,4])
      def delete(id_or_array)
        if id_or_array.is_a? Array
          id_or_array.each {|id| @model_class.resource[id].delete} # todo: can this be optimized into one call?
        else
          @model_class.resource[id_or_array].delete
        end
      end

      # Updates all records with details given if they match a set of conditions supplied, limits and order can
      # also be supplied. This method sends a single update straight to the database. It does not instantiate
      # the involved models and it does not trigger Active Record callbacks or validations.
      #
      # ==== Parameters
      #
      # * +updates+ - hash of attribute updates
      #
      # ==== Examples
      #
      #   # Update all customers with the given attributes
      #   Customer.update_all wants_email: true
      #
      #   # Update all books with 'Rails' in their title
      #   Book.where('title LIKE ?', '%Rails%').update_all(author: 'David')
      #
      #   # Update all books that match conditions, but limit it to 5 ordered by date
      #   Book.where('title LIKE ?', '%Rails%').order(:created).limit(5).update_all(:author => 'David')
      def update_all(updates)
        raise ArgumentError, "Empty list of attributes to change" if updates.blank?
        raise ArgumentError, "updates must be a Hash" unless updates.is_a? Hash
        run_update(updates)
      end

      # Looping through a collection of records from the database
      # (using the +all+ method, for example) is very inefficient
      # since it will try to instantiate all the objects at once.
      #
      # In that case, batch processing methods allow you to work
      # with the records in batches, thereby greatly reducing memory consumption.
      #
      # The #find_each method uses #find_in_batches with a batch size of 1000 (or as
      # specified by the +:batch_size+ option).
      #
      #   Person.all.find_each do |person|
      #     person.do_awesome_stuff
      #   end
      #
      #   Person.where("age > 21").find_each do |person|
      #     person.party_all_night!
      #   end
      #
      #  You can also pass the +:start+ option to specify
      #  an offset to control the starting point.
      def find_each(options = {})
        find_in_batches(options) do |records|
          records.each { |record| yield record }
        end
      end

      # Yields each batch of records that was found by the find +options+ as
      # an array. The size of each batch is set by the +:batch_size+
      # option; the default is 1000.
      #
      # You can control the starting point for the batch processing by
      # supplying the +:start+ option. This is especially useful if you
      # want multiple workers dealing with the same processing queue. You can
      # make worker 1 handle all the records between id 0 and 10,000 and
      # worker 2 handle from 10,000 and beyond (by setting the +:start+
      # option on that worker).
      #
      # It's not possible to set the order. That is automatically set to
      # ascending on the primary key ("id ASC") to make the batch ordering
      # work. This also mean that this method only works with integer-based
      # primary keys. You can't set the limit either, that's used to control
      # the batch sizes.
      #
      #   Person.where("age > 21").find_in_batches do |group|
      #     sleep(50) # Make sure it doesn't get too crowded in there!
      #     group.each { |person| person.party_all_night! }
      #   end
      #
      #   # Let's process the next 2000 records
      #   Person.all.find_in_batches(start: 2000, batch_size: 2000) do |group|
      #     group.each { |person| person.party_all_night! }
      #   end
      def find_in_batches(options={})
        options.assert_valid_keys(:start, :batch_size)

        raise "Not yet implemented" # todo

        start = options.delete(:start) || 0
        batch_size = options.delete(:batch_size) || 1000

        while records.any?
          records_size = records.size
          primary_key_offset = records.last.id

          yield records

          break if records_size < batch_size

          if primary_key_offset
            records = relation.where(table[primary_key].gt(primary_key_offset)).to_a
          else
            raise "Primary key not included in the custom select clause"
          end
        end
      end

      # Updates an object (or multiple objects) and saves it to the database, if validations pass.
      # The resulting object is returned whether the object was saved successfully to the database or not.
      #
      # ==== Parameters
      #
      # * +id+ - This should be the id or an array of ids to be updated.
      # * +attributes+ - This should be a hash of attributes or an array of hashes.
      #
      # ==== Examples
      #
      #   # Updates one record
      #   Person.update(15, user_name: 'Samuel', group: 'expert')
      #
      #   # Updates multiple records
      #   people = { 1 => { "first_name" => "David" }, 2 => { "first_name" => "Jeremy" } }
      #   Person.update(people.keys, people.values)
      def update(id, attributes)
        if id.is_a?(Array)
          id.map.with_index { |one_id, idx| update(one_id, attributes[idx]) }
        else
          object = find(id)
          object.update_attributes(attributes)
          object
        end
      end

      ## todo: scoping
      ## Scope all queries to the current scope.
      ##
      ##   Comment.where(:post_id => 1).scoping do
      ##     Comment.first # SELECT * FROM comments WHERE post_id = 1
      ##   end
      ##
      ## Please check unscoped if you want to remove all previous scopes (including
      ## the default_scope) during the execution of a block.
      #def scoping
      #  previous, @model_class.current_scope = @model_class.current_scope, self
      #  yield
      #ensure
      #  klass.current_scope = previous
      #end


      # #where accepts conditions in one of several formats.
      #
      # === string
      #
      # A single string, without additional arguments, is used in the where clause of the query.
      #
      #    Client.where("orders_count = '2'")
      #    # SELECT * where orders_count = '2';
      #
      # Note that building your own string from user input may expose your application
      # to injection attacks if not done properly. As an alternative, it is recommended
      # to use one of the following methods.
      #
      # === array
      #
      # If an array is passed, then the first element of the array is treated as a template, and
      # the remaining elements are inserted into the template to generate the condition.
      # Active Record takes care of building the query to avoid injection attacks, and will
      # convert from the ruby type to the database type where needed. Elements are inserted
      # into the string in the order in which they appear.
      #
      #   User.where(["name = ? and email = ?", "Joe", "joe@example.com"])
      #   # SELECT * WHERE name = 'Joe' AND email = 'joe@example.com';
      #
      # Alternatively, you can use named placeholders in the template, and pass a hash as the
      # second element of the array. The names in the template are replaced with the corresponding
      # values from the hash.
      #
      #   User.where(["name = :name and email = :email", { name: "Joe", email: "joe@example.com" }])
      #   # SELECT * WHERE name = 'Joe' AND email = 'joe@example.com';
      #
      # This can make for more readable code in complex queries.
      #
      # Lastly, you can use sprintf-style % escapes in the template. This works slightly differently
      # than the previous methods; you are responsible for ensuring that the values in the template
      # are properly quoted. The values are passed to the connector for quoting, but the caller
      # is responsible for ensuring they are enclosed in quotes in the resulting SQL. After quoting,
      # the values are inserted using the same escapes as the Ruby core method <tt>Kernel::sprintf</tt>.
      #
      #   User.where(["name = '%s' and email = '%s'", "Joe", "joe@example.com"])
      #   # SELECT * WHERE name = 'Joe' AND email = 'joe@example.com';
      #
      # If #where is called with multiple arguments, these are treated as if they were passed as
      # the elements of a single array.
      #
      #   User.where("name = :name and email = :email", { name: "Joe", email: "joe@example.com" })
      #   # SELECT * WHERE name = 'Joe' AND email = 'joe@example.com';
      #
      # When using strings to specify conditions, you can use any operator available from
      # the database. While this provides the most flexibility, you can also unintentionally introduce
      # dependencies on the underlying database. If your code is intended for general consumption,
      # test with multiple database backends.
      #
      # === hash
      #
      # #where will also accept a hash condition, in which the keys are fields and the values
      # are values to be searched for.
      #
      # Fields can be symbols or strings. Values can be single values, arrays, or ranges.
      #
      #    User.where({ name: "Joe", email: "joe@example.com" })
      #    # SELECT * WHERE name = 'Joe' AND email = 'joe@example.com'
      #
      #    User.where({ name: ["Alice", "Bob"]})
      #    # SELECT * WHERE name IN ('Alice', 'Bob')
      #
      #    User.where({ created_at: (Time.now.midnight - 1.day)..Time.now.midnight })
      #    # SELECT * WHERE (created_at BETWEEN '2012-06-09 07:00:00.000000' AND '2012-06-10 07:00:00.000000')
      #
      # In the case of a belongs_to relationship, an association key can be used
      # to specify the model if an ActiveRecord object is used as the value.
      #
      #    author = Author.find(1)
      #
      #    # The following queries will be equivalent:
      #    Post.where(:author => author)
      #    Post.where(:author_id => author)
      #
      # === empty condition
      #
      # If the condition returns true for blank?, then where is a no-op and returns the current relation.
      #
      def where(opts, *rest)
        return self if opts.blank?
        case opts
          when Hash
            @options[:hash] = opts # keep around for first_or_create stuff...
            opts.each do |k,v|
              # todo: can we support IN and BETWEEN syntax as documented above?
              v = "'#{v}'" if v.is_a? String
              query_conditions << "#{k} = #{v}"
            end
          when String
            query_conditions << opts
          when Array
            query = opts.shift.gsub '?', "'%s'"
            query = query % opts
            query_conditions << query
        end
        self
      end


      protected


      def limit_value
        @options[:limit]
      end

      def query_conditions
        @options[:conditions] ||= []
      end

      def loaded?
        !!@records
      end

      def reversed?
        !!@options[:reversed]
      end

      def find_one(id_or_name=nil)
        begin
          entity = @model_class.resource[id_or_name].query(nil, limit: 1).entity
          @model_class.model_name.constantize.new(entity.data) if entity
        rescue RestClient::ResourceNotFound
          nil
        end
      end

      def find_one!(id_or_name=nil)
        find_one(id_or_name) or raise RecordNotFound
      end

      # Server-side options:
      # Xql	       string   Query in the query language
      # type	     string   Entity type to return
      # Xreversed   string   Return results in reverse order
      # connection string   Connection type (e.g., "likes")
      # start      string   First entity's UUID to return
      # cursor     string   Encoded representation of the query position for paging
      # Xlimit      integer  Number of results to return
      # permission string   Permission type
      # Xfilter     string   Condition on which to filter
      def query_options
        # todo: support more options?
        options = {}
        options.merge!({:limit => limit_value.to_json}) if limit_value
        options.merge!({:skip => @options[:skip].to_json}) if @options[:skip]
        options.merge!({:reversed => reversed?.to_json}) if reversed?
        options.merge!({:order => @options[:order]}) if @options[:order]
        options.merge!({:cursor => @options[:cursor]}) if @options[:cursor]
        options
      end

      def create_query
        select = @options[:select] || '*'
        where = ('where ' + query_conditions.join(' and ')) unless query_conditions.blank?
        "select #{select} #{where}"
      end

      def run_query
        @model_class.resource.query(create_query, query_options)
      end

      def run_update(attributes)
        @model_class.resource.update_query(attributes, create_query, query_options)
      end

      def load
        return if loaded?
        begin
          @response = run_query
          if (!@options[:select] or @options[:select] == '*')
            @records = @response.entities.collect {|r| @model_class.model_name.constantize.new(r.data)}
          else # handle list
            selects = @options[:select].split ','
            @records = @response.entities.collect do |r|
              data = {}
              (0..selects.size).each do |i|
                data[selects[i]] = r[i]
              end
              @model_class.model_name.constantize.new(data)
            end
          end
        rescue RestClient::ResourceNotFound
          @records = []
        end
      end
    end
  end
end
