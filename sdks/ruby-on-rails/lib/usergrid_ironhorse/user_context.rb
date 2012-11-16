module Usergrid
  module Ironhorse
    module UserContext

      # returns user if logged in, nil otherwise
      # if session is passed, stores user_id and auth_token in the session
      # and sets User as current_user
      def authenticate(username, password, session=nil)
        application = Usergrid::Application.new Usergrid::Ironhorse::Base.settings[:application_url]
        begin
          application.login username, password
          user = new application.current_user.data
          if session
            session[:usergrid_user_id] = user.id
            session[:usergrid_auth_token] = user.current_auth_token
            set_thread_context(session)
            Thread.current[:usergrid_current_user] = user
          end
          user
        rescue
          Rails.logger.info $!
          raise $!
        end
      end

      # clears auth from session and thread
      def clear_authentication(session)
        session[:usergrid_user_id] = nil
        session[:usergrid_auth_token] = nil
        clear_thread_context(session)
      end

      # allows admin actions to be done in a block
      def as_admin(&block)
        save_auth_token = Thread.current[:usergrid_auth_token]
        begin
          Thread.current[:usergrid_auth_token] = Base.settings[:auth_token]
          yield block
        ensure
          Thread.current[:usergrid_auth_token] = save_auth_token
        end
      end

      # sets auth for current thread
      def set_thread_context(session)
        Thread.current[:usergrid_user_id] = session[:usergrid_user_id]
        Thread.current[:usergrid_auth_token] = session[:usergrid_auth_token]
        Thread.current[:usergrid_current_user] = nil
      end
      alias_method :set_context, :set_thread_context

      # clears auth from current thread
      def clear_thread_context(session)
        Thread.current[:usergrid_user_id] = nil
        Thread.current[:usergrid_auth_token] = nil
        Thread.current[:usergrid_current_user] = nil
      end
      alias_method :clear_context, :clear_thread_context

      # returns the auth token for the current thread
      def current_auth_token
        Thread.current[:usergrid_auth_token]
      end

      # does a find and return
      def current_user
        unless Thread.current[:usergrid_current_user]
          Thread.current[:usergrid_current_user] = find(Thread.current[:usergrid_user_id]) if Thread.current[:usergrid_user_id]
        end
        Thread.current[:usergrid_current_user]
      end

    end
  end
end