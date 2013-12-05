package org.apache.usergrid.persistence.collection.mvcc.entity;


/** A dup of the Guava EventBus so we can easily mock and test
 *  @author tnine */
public interface CollectionEventBus {

    /**
     * Registers all handler methods on {@code object} to receive events.
     * Handler methods are selected and classified using this EventBus's
     * {@link com.google.common.eventbus.HandlerFindingStrategy}; the default strategy is the
     * {@link com.google.common.eventbus.AnnotatedHandlerFinder}.
     *
     * @param object  object whose handler methods should be registered.
     */
    void register(Object object);

    /**
     * Unregisters all handler methods on a registered {@code object}.
     *
     * @param object  object whose handler methods should be unregistered.
     * @throws IllegalArgumentException if the object was not previously registered.
     */
    void unregister(Object object);

    /**
      * Posts an event to all registered handlers.  This method will return
      * successfully after the event has been posted to all handlers, and
      * regardless of any exceptions thrown by handlers.
      *
      * <p>If no handlers have been subscribed for {@code event}'s class, and
      * {@code event} is not already a {@link com.google.common.eventbus.DeadEvent}, it will be wrapped in a
      * DeadEvent and reposted.
      *
      * @param event  event to post.
      */
     void post(Object event);
}
