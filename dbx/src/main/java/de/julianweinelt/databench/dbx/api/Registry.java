package de.julianweinelt.databench.dbx.api;

import de.julianweinelt.databench.dbx.api.events.*;
import de.julianweinelt.databench.dbx.api.events.EventListener;
import de.julianweinelt.databench.dbx.api.plugins.DbxPlugin;
import de.julianweinelt.databench.dbx.api.plugins.SystemPlugin;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
/**
 * The {@code Registry} class manages the core functionality of the DBX plugin framework,
 * including:
 * <ul>
 *     <li>Plugin registration and lifecycle</li>
 *     <li>Custom event handling and listener management</li>
 *     <li>Command registration</li>
 *     <li>Minecraft plugin endpoints</li>
 * </ul>
 *
 * <p>
 * It acts as a centralized manager and dispatcher, handling all dynamic components
 * within the plugin environment.
 * </p>
 */
public class Registry {
    private static final Logger log = LoggerFactory.getLogger(Registry.class);
    private final DbxAPI api;

    @Getter
    private final SystemPlugin systemPlugin;

    @Getter
    private final ConcurrentLinkedQueue<DbxPlugin> plugins = new ConcurrentLinkedQueue<>();

    private final Map<String, List<EventListener>> listeners = new HashMap<>();
    private final Map<DbxPlugin, List<String>> eventsRegisteredByPlugin = new HashMap<>();

    public Registry(DbxAPI api) {
        this.api = api;
        systemPlugin = new SystemPlugin();
    }

    public static Registry instance() {
        return DbxAPI.registry();
    }

    public int getEventAmount() {
        int amount = 0;
        for (DbxPlugin plugin : plugins) {
            amount += eventsRegisteredByPlugin.get(plugin).size();
        }
        return amount;
    }

    /**
     * Registers a single event name for a given plugin.
     *
     * @param plugin    the plugin registering the event
     * @param eventName the name of the event
     */
    public void registerEvent(DbxPlugin plugin, String eventName) {
        listeners.putIfAbsent(eventName, new ArrayList<>());

        if (eventsRegisteredByPlugin.containsKey(plugin)) eventsRegisteredByPlugin.get(plugin).add(eventName);
        else eventsRegisteredByPlugin.put(plugin, new ArrayList<>(Collections.singletonList(eventName)));
    }

    /**
     * Registers multiple event names for a given plugin.
     *
     * @param plugin     the plugin registering the events
     * @param eventNames the names of the events
     */
    public void registerEvents(DbxPlugin plugin, String... eventNames) {
        for (String eventName : eventNames) {
            listeners.putIfAbsent(eventName, new ArrayList<>());
        }

        if (eventsRegisteredByPlugin.containsKey(plugin)) eventsRegisteredByPlugin.get(plugin).addAll(Arrays.asList(eventNames));
        else eventsRegisteredByPlugin.put(plugin, new ArrayList<>(Arrays.asList(eventNames)));
    }

    /**
     * Registers multiple system event names (not bound to any plugin).
     * Should not be used to register events
     *
     * @param eventNames the event names to register
     */
    public void registerEvents(String... eventNames) {
        registerEvents(getSystemPlugin(), eventNames);
    }

    /**
     * Registers all listener methods annotated with {@link Subscribe} in the given listener object.
     *
     * @param listener the listener instance containing subscribed methods
     * @param plugin   the plugin registering the listener
     */
    public void registerListener(Object listener, DbxPlugin plugin) {
        for (Method method : listener.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(Subscribe.class)) {
                Subscribe annotation = method.getAnnotation(Subscribe.class);
                String eventName = annotation.value();
                Priority priority = annotation.priority();

                listeners.putIfAbsent(eventName, new ArrayList<>());
                listeners.get(eventName).add(new EventListener(listener, method, priority));
                listeners.get(eventName).sort(Comparator.comparing(EventListener::getPriority));
            }
        }
    }

    /**
     * Calls a specific event and dispatches it to all registered listeners
     * for the event's name, in priority order.
     *
     * @param event the event to dispatch
     */
    public void callEvent(Event event) {
        log.debug("Called event {}", event.getName());
        List<EventListener> eventListeners = listeners.get(event.getName());
        if (eventListeners != null) {
            for (EventListener listener : eventListeners) {
                try {
                    if (listener.getMethod().isAnnotationPresent(IgnoreCancelled.class) || !event.isCancelled()) {
                        listener.invoke(event);
                    }
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            }
        }
    }

    /**
     * Adds a plugin to the internal registry.
     *
     * @param name the plugin to register
     */
    public void addPlugin(DbxPlugin name) {
        plugins.add(name);
    }

    /**
     * Retrieves a plugin by its name.
     *
     * @param name the name of the plugin
     * @return the {@link DbxPlugin} instance, or {@code null} if not found
     */
    public DbxPlugin getPlugin(String name) {
        for (DbxPlugin m : plugins) if (m.getName().equals(name)) return m;
        return null;
    }

    /**
     * Removes a plugin and all its associated event registrations.
     *
     * @param name the name of the plugin to remove
     */
    public void removePlugin(String name) {
        DbxPlugin plugin = getPlugin(name);
        List<String> events = eventsRegisteredByPlugin.getOrDefault(plugin, new ArrayList<>());
        events.forEach(event -> listeners.getOrDefault(event, new ArrayList<>()).clear());
        events.forEach(listeners::remove);
        plugins.removeIf(m -> m.getName().equals(name));
    }
}