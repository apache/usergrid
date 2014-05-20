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

require 'active_record/validations'
require 'active_record/errors'
require 'active_record/callbacks'

module Usergrid
  module Ironhorse

    class Base
      include ActiveModel::AttributeMethods
      include ActiveModel::Conversion
      include ActiveModel::Validations
      include ActiveModel::Dirty
      include ActiveModel::Serialization
      include ActiveModel::MassAssignmentSecurity
      extend  ActiveModel::Naming
      extend  ActiveModel::Callbacks

      RESERVED_ATTRIBUTES = %w(metadata created modified uuid type uri)

      define_model_callbacks :create, :destroy, :save, :update

      # todo: determine the subset to support...
      # unsupported: :group, :joins, :preload, :eager_load, :includes, :from, :lock,
      #              :having, :create_with, :uniq, :references, :none, :count,
      #              :average, :minimum, :maximum, :sum, :calculate, :ids
      #              :find_each, :find_in_batches, :offset, :readonly

      #delegate :find, :take, :take!, :first, :first!, :last, :last!, :exists?, :any?, :many?, :to => :all
      #delegate :first_or_create, :first_or_create!, :first_or_initialize, :to => :all
      #delegate :find_by, :find_by!, :to => :all
      #delegate :destroy, :destroy_all, :delete, :delete_all, :update, :update_all, :to => :all
      #delegate :find_each, :find_in_batches, :to => :all
      #delegate :select, :group, :order, :except, :reorder, :limit, :offset,
      #         :where, :preload, :eager_load, :includes, :from, :lock, :readonly,
      #         :having, :create_with, :uniq, :references, :none, :to => :all
      #delegate :count, :average, :minimum, :maximum, :sum, :calculate, :pluck, :ids, :to => :all

      @@settings ||= nil

      attr_accessor :attributes

      HashWithIndifferentAccess = ActiveSupport::HashWithIndifferentAccess
      RecordNotSaved = ActiveRecord::RecordNotSaved

      def initialize(attrs=nil)
        @attributes = HashWithIndifferentAccess.new
        assign_attributes attrs if attrs
      end

      def self.configure!(application_url, auth_token)
        @@settings = HashWithIndifferentAccess.new application_url: application_url, auth_token: auth_token
      end

      def self.settings
        return @@settings if @@settings
        path = "config/usergrid.yml"
        environment = defined?(Rails) && Rails.respond_to?(:env) ? Rails.env : ENV['RACK_ENV']
        @@settings = HashWithIndifferentAccess.new YAML.load(ERB.new(File.new(path).read).result)[environment]
      end

      # forward to all
      def self.method_missing(method, *args, &block)
        all.send method, *args, &block
      end

      # forward to all
      def method_missing(method, *args, &block)
        if args.size == 0
          attributes[method]
        elsif args.size == 1 && method[-1] == '='
          attr = method[0..-2]
          if attributes[attr] != args[0]
            attribute_will_change!(attr)
            attributes[attr] = args[0]
          end
        else
          all.send method, *args, &block
        end
      end

      # todo: scopes
      def self.all
        unscoped
      end

      #def self.scope(symbol, scope)
      #  @scopes[symbol] = scope
      #end

      #def self.current_scope
      #  @current_scope ||= default_scope
      #end
      #
      #def self.current_scope=(scope)
      #  @current_scope = scope
      #end
      #
      #def self.default_scope
      #  @default_scope ||= unscoped
      #end

      def self.unscoped
        Query.new(self)
      end

      def self.create(attributes=nil, options=nil, &block)
        if attributes.is_a?(Array)
          attributes.collect { |attr| create(attr, options, &block) }
        else
          object = new(attributes, &block)
          object.save
          object
        end
      end

      def self.create!(attributes=nil, options=nil, &block)
        if attributes.is_a?(Array)
          attributes.collect {|attr| create!(attr, options, &block)}
        else
          object = new(attributes)
          yield(object) if block_given?
          object.save!
          object
        end
      end

      def self.class_attributes
        @class_attributes ||= {}
      end

      # Returns true if the record is persisted, i.e. it's not a new record and it was
      # not destroyed, otherwise returns false.
      def persisted?
        !(new_record? || destroyed?)
      end

      def new_record?
        !self.uuid
      end

      def self.group
        model_name.plural.downcase
      end

      # Creates a Usergrid::Resource
      def self.resource
        app = Usergrid::Application.new settings[:application_url]
        #app.auth_token = Thread.current[:usergrid_auth_token]
        app[group]
      end

      # Saves the model.
      #
      # If the model is new a record gets created in the database, otherwise
      # the existing record gets updated.
      #
      # By default, save always run validations. If any of them fail the action
      # is cancelled and +save+ returns +false+. However, if you supply
      # :validate => false, validations are bypassed altogether. See
      # ActiveRecord::Validations for more information.
      #
      # There's a series of callbacks associated with +save+. If any of the
      # <tt>before_*</tt> callbacks return +false+ the action is cancelled and
      # +save+ returns +false+. See ActiveRecord::Callbacks for further
      # details.
      def save
        begin
          create_or_update
        rescue ActiveRecord::RecordInvalid
          false
        end
      end

      # Saves the model.
      #
      # If the model is new a record gets created in the database, otherwise
      # the existing record gets updated.
      #
      # With <tt>save!</tt> validations always run. If any of them fail
      # ActiveRecord::RecordInvalid gets raised. See ActiveRecord::Validations
      # for more information.
      #
      # There's a series of callbacks associated with <tt>save!</tt>. If any of
      # the <tt>before_*</tt> callbacks return +false+ the action is cancelled
      # and <tt>save!</tt> raises ActiveRecord::RecordNotSaved. See
      # ActiveRecord::Callbacks for further details.
      def save!
        create_or_update or raise RecordNotSaved
      end

      # Deletes the record in the database and freezes this instance to
      # reflect that no changes should be made (since they can't be
      # persisted). Returns the frozen instance.
      #
      # The row is simply removed with a +DELETE+ statement on the
      # record's primary key, and no callbacks are executed.
      #
      # To enforce the object's +before_destroy+ and +after_destroy+
      # callbacks, Observer methods, or any <tt>:dependent</tt> association
      # options, use <tt>#destroy</tt>.
      def delete
        self.class.delete(id) if persisted?
        @destroyed = true
        freeze
      end

      # Deletes the record in the database and freezes this instance to reflect
      # that no changes should be made (since they can't be persisted).
      #
      # There's a series of callbacks associated with <tt>destroy</tt>. If
      # the <tt>before_destroy</tt> callback return +false+ the action is cancelled
      # and <tt>destroy</tt> returns +false+. See
      # ActiveRecord::Callbacks for further details.
      def destroy
        raise ReadOnlyRecord if readonly?
        # todo: callbacks?
        instance_resource.delete if persisted?
        @destroyed = true
        freeze
      end

      # Deletes the record in the database and freezes this instance to reflect
      # that no changes should be made (since they can't be persisted).
      #
      # There's a series of callbacks associated with <tt>destroy!</tt>. If
      # the <tt>before_destroy</tt> callback return +false+ the action is cancelled
      # and <tt>destroy!</tt> raises ActiveRecord::RecordNotDestroyed. See
      # ActiveRecord::Callbacks for further details.
      def destroy!
        destroy || raise(ActiveRecord::RecordNotDestroyed)
      end

      # Returns true if this object has been destroyed, otherwise returns false.
      def destroyed?
        !!@destroyed
      end

      # Reloads the attributes of this object from the database.
      def reload
        return false if !persisted?
        fresh_object = self.class.find(id)
        refresh_data fresh_object.instance_variable_get('@attributes')
        self
      end

      # Updates the attributes of the model from the passed-in hash and saves the
      # record, all wrapped in a transaction. If the object is invalid, the saving
      # will fail and false will be returned.
      def update_attributes(attributes)
        assign_attributes attributes
        save
      end

      # Updates its receiver just like +update_attributes+ but calls <tt>save!</tt> instead
      # of +save+, so an exception is raised if the record is invalid.
      def update_attributes!(attributes)
        assign_attributes attributes
        save!
      end

      # Note that whenever you include ActiveModel::AttributeMethods in your class,
      # it requires you to implement an +attributes+ method which returns a hash
      # with each attribute name in your model as hash key and the attribute value as
      # hash value.
      #
      # Hash keys must be strings.
      def attributes
        @attributes ||= self.class.class_attributes.clone
      end

      def id; self.uuid end
      def created_at; self.created end
      def updated_at; self.modified end


      protected


      def assign_attributes(attrs)
        attrs = sanitize_for_mass_assignment(attrs)
        attrs.each do |attr,value|
          attr = attr.to_s
          unless attributes[attr] == value
            attribute_will_change!(attr) unless RESERVED_ATTRIBUTES.include? attr
            attributes[attr] = value
          end
        end
      end

      def create_or_update
        raise ReadOnlyRecord if readonly?
        if valid?
          run_callbacks :save do
            return new_record? ? do_create : do_update
          end
        end
        false
      end

      def do_create
        group_resource.post(unsaved_attributes) do |resp, req, res, &block|
          if resp.code.to_s == "200" || resp.code.to_s == "201"
            refresh_data resp.entity_data
            return true
          else
            errors.add(resp.code.to_s, resp)
            return false
          end
        end
      end

      def do_update
        return false unless changed?

        instance_resource.put(unsaved_attributes) do |resp, req, res, &block|
          if resp.code.to_s == "200" || resp.code.to_s == "201"
            refresh_data resp.entity_data
            return true
          else
            errors.add(resp.code, resp)
            return false
          end
        end
      end

      def unsaved_attributes
        HashWithIndifferentAccess[changed.collect {|k| [k, attributes[k]]}]
      end

      def group_resource
        self.class.resource
      end

      def instance_resource
        self.class.resource["#{self.id}"]
      end

      def refresh_data(entity_data)
        @previously_changed = changes
        @changed_attributes.clear
        @attributes = HashWithIndifferentAccess.new entity_data
      end

      def attribute_will_change!(attr)
        begin
          value = __send__(attr)
          value = value.duplicable? ? value.clone : value
        rescue TypeError, NoMethodError
        end

        changed_attributes[attr] = value unless changed_attributes.include?(attr)
      end
    end
  end
end
