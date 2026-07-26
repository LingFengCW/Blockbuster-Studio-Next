package mchorse.bbs_mod.events;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class EventBus
{
    private final Map<Class<?>, CopyOnWriteArrayList<Subscription>> subscribers = new HashMap<>();

    /**
     * Programmatic (non-reflection) event handlers. Used by script plugins and
     * any code that wants to subscribe with a lambda instead of an annotated method.
     */
    private final Map<Class<?>, CopyOnWriteArrayList<Consumer<Object>>> handlers = new HashMap<>();

    /**
     * Registers the given subscriber to receive events.
     */
    public void register(Object subscriber)
    {
        for (Method method : subscriber.getClass().getDeclaredMethods())
        {
            this.subscribe(subscriber, method);
        }
    }

    private void subscribe(Object subscriber, Method method)
    {
        if (method.isAnnotationPresent(Subscribe.class))
        {
            if (method.getParameterCount() != 1)
            {
                return;
            }

            this.subscribers
                .computeIfAbsent(method.getParameterTypes()[0], (clazz) -> new CopyOnWriteArrayList<>())
                .add(new Subscription(subscriber, method));
        }
    }

    /**
     * Subscribes a handler to a given event type programmatically. This lets code
     * (including script plugins) listen for events without an annotated method.
     */
    @SuppressWarnings("unchecked")
    public <T> void subscribe(Class<T> type, Consumer<T> handler)
    {
        if (type == null || handler == null)
        {
            return;
        }

        this.handlers
            .computeIfAbsent(type, (clazz) -> new CopyOnWriteArrayList<>())
            .add((Consumer<Object>) handler);
    }

    /**
     * Posts the given event to the event bus.
     */
    public void post(Object event)
    {
        CopyOnWriteArrayList<Subscription> eventSubscribers = this.subscribers.get(event.getClass());

        if (eventSubscribers != null && !eventSubscribers.isEmpty())
        {
            for (Subscription subscription : eventSubscribers)
            {
                try
                {
                    subscription.method.invoke(subscription.target, event);
                }
                catch (Exception ignored)
                {}
            }
        }

        CopyOnWriteArrayList<Consumer<Object>> eventHandlers = this.handlers.get(event.getClass());

        if (eventHandlers != null && !eventHandlers.isEmpty())
        {
            for (Consumer<Object> handler : eventHandlers)
            {
                try
                {
                    handler.accept(event);
                }
                catch (Exception ignored)
                {}
            }
        }
    }
}
