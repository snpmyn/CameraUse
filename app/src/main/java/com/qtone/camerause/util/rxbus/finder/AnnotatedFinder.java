package com.qtone.camerause.util.rxbus.finder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.qtone.camerause.util.rxbus.annotation.Produce;
import com.qtone.camerause.util.rxbus.annotation.Subscribe;
import com.qtone.camerause.util.rxbus.annotation.Tag;
import com.qtone.camerause.util.rxbus.entity.EventType;
import com.qtone.camerause.util.rxbus.entity.ProducerBaseEvent;
import com.qtone.camerause.util.rxbus.entity.SubscriberBaseEvent;
import com.qtone.camerause.util.rxbus.thread.EventThread;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @decs: AnnotatedFinder
 * <p>
 * Helper methods for finding methods annotated with {@link com.qtone.camerause.util.rxbus.annotation.Produce} and {@link com.qtone.camerause.util.rxbus.annotation.Subscribe}.
 * @author: 郑少鹏
 * @date: 2019/8/28 11:08
 */
public final class AnnotatedFinder {
    /**
     * Cache event bus producer methods for each class.
     */
    private static final ConcurrentMap<Class<?>, Map<EventType, SourceMethod>> PRODUCERS_CACHE = new ConcurrentHashMap<>();
    /**
     * Cache event bus subscriber methods for each class.
     */
    private static final ConcurrentMap<Class<?>, Map<EventType, Set<SourceMethod>>> SUBSCRIBERS_CACHE = new ConcurrentHashMap<>();

    private AnnotatedFinder() {
        // No instances.
    }

    private static void loadAnnotatedProducerMethods(Class<?> listenerClass, Map<EventType, SourceMethod> producerMethods) {
        Map<EventType, Set<SourceMethod>> subscriberMethods = new HashMap<>(9);
        loadAnnotatedMethods(listenerClass, producerMethods, subscriberMethods);
    }

    private static void loadAnnotatedSubscriberMethods(Class<?> listenerClass, Map<EventType, Set<SourceMethod>> subscriberMethods) {
        Map<EventType, SourceMethod> producerMethods = new HashMap<>(9);
        loadAnnotatedMethods(listenerClass, producerMethods, subscriberMethods);
    }

    /**
     * Load all methods annotated with {@link com.qtone.camerause.util.rxbus.annotation.Produce} or {@link com.qtone.camerause.util.rxbus.annotation.Subscribe} into their respective caches for the specified class.
     *
     * @param listenerClass     Class<?>
     * @param producerMethods   Map<EventType, SourceMethod>
     * @param subscriberMethods Map<EventType, Set<SourceMethod>>
     */
    private static void loadAnnotatedMethods(@NotNull Class<?> listenerClass, Map<EventType, SourceMethod> producerMethods, Map<EventType, Set<SourceMethod>> subscriberMethods) {
        for (Method method : listenerClass.getDeclaredMethods()) {
            // The compiler sometimes creates synthetic bridge methods as part of the type erasure process.
            // As of JDK8 these methods now include the same annotations as the original declarations.
            // They should be ignored for subscribe / produce.
            if (method.isBridge()) {
                continue;
            }
            if (method.isAnnotationPresent(Subscribe.class)) {
                Class<?> parameterClazz = getParameterClazz(method);
                Subscribe annotation = method.getAnnotation(Subscribe.class);
                EventThread thread = null;
                if (null != annotation) {
                    thread = annotation.thread();
                }
                Tag[] tags = new Tag[0];
                if (null != annotation) {
                    tags = annotation.tags();
                }
                int tagLength = tags.length;
                do {
                    String tag = Tag.DEFAULT;
                    if (tagLength > 0) {
                        tag = tags[tagLength - 1].value();
                    }
                    EventType type = new EventType(tag, parameterClazz);
                    Set<SourceMethod> methods = subscriberMethods.computeIfAbsent(type, k -> new HashSet<>());
                    methods.add(new SourceMethod(thread, method));
                    tagLength--;
                } while (tagLength > 0);
            } else if (method.isAnnotationPresent(Produce.class)) {
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length != 0) {
                    throw new IllegalArgumentException("Method " + method + "has @Produce annotation but requires " + parameterTypes.length + " arguments.  Methods must require zero arguments.");
                }
                if (method.getReturnType() == Void.class) {
                    throw new IllegalArgumentException("Method " + method + " has a return type of void.  Must declare a non-void type.");
                }
                Class<?> parameterClazz = getClass(method);
                Produce annotation = method.getAnnotation(Produce.class);
                EventThread eventThread = null;
                if (null != annotation) {
                    eventThread = annotation.thread();
                }
                Tag[] tags = new Tag[0];
                if (null != annotation) {
                    tags = annotation.tags();
                }
                int tagLength = tags.length;
                do {
                    String tag = Tag.DEFAULT;
                    if (tagLength > 0) {
                        tag = tags[tagLength - 1].value();
                    }
                    EventType type = new EventType(tag, parameterClazz);
                    if (producerMethods.containsKey(type)) {
                        throw new IllegalArgumentException("Producer for type " + type + " has already been registered.");
                    }
                    producerMethods.put(type, new SourceMethod(eventThread, method));
                    tagLength--;
                } while (tagLength > 0);
            }
        }
        PRODUCERS_CACHE.put(listenerClass, producerMethods);
        SUBSCRIBERS_CACHE.put(listenerClass, subscriberMethods);
    }

    @NonNull
    private static Class<?> getParameterClazz(@NonNull Method method) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length != 1) {
            throw new IllegalArgumentException("Method " + method + " has @Subscribe annotation but requires " + parameterTypes.length + " arguments.  Methods must require a single argument.");
        }
        return getClass(method, parameterTypes);
    }

    @NonNull
    private static Class<?> getClass(Method method, @NonNull Class<?>[] parameterTypes) {
        Class<?> parameterClazz = parameterTypes[0];
        if (parameterClazz.isInterface()) {
            throw new IllegalArgumentException("Method " + method + " has @Subscribe annotation on " + parameterClazz + " which is an interface.  Subscription must be on a concrete class type.");
        }
        if ((method.getModifiers() & Modifier.PUBLIC) == 0) {
            throw new IllegalArgumentException("Method " + method + " has @Subscribe annotation on " + parameterClazz + " but is not 'public'.");
        }
        return parameterClazz;
    }

    @NonNull
    private static Class<?> getClass(@NonNull Method method) {
        Class<?> parameterClazz = method.getReturnType();
        if (parameterClazz.isInterface()) {
            throw new IllegalArgumentException("Method " + method + " has @Produce annotation on " + parameterClazz + " which is an interface.  Producers must return a concrete class type.");
        }
        if (parameterClazz.equals(Void.TYPE)) {
            throw new IllegalArgumentException("Method " + method + " has @Produce annotation but has no return type.");
        }
        if ((method.getModifiers() & Modifier.PUBLIC) == 0) {
            throw new IllegalArgumentException("Method " + method + " has @Produce annotation on " + parameterClazz + " but is not 'public'.");
        }
        return parameterClazz;
    }

    /**
     * This implementation finds all methods marked with a {@link Produce} annotation.
     *
     * @param listener Object
     * @return Map<EventType, ProducerBaseEvent>
     */
    static @NotNull Map<EventType, ProducerBaseEvent> findAllProducers(@NotNull Object listener) {
        final Class<?> listenerClass = listener.getClass();
        Map<EventType, ProducerBaseEvent> producersInMethod = new HashMap<>(9);
        Map<EventType, SourceMethod> methods = PRODUCERS_CACHE.get(listenerClass);
        if (null == methods) {
            methods = new HashMap<>(9);
            loadAnnotatedProducerMethods(listenerClass, methods);
        }
        if (!methods.isEmpty()) {
            for (Map.Entry<EventType, SourceMethod> e : methods.entrySet()) {
                ProducerBaseEvent producer = new ProducerBaseEvent(listener, e.getValue().method, e.getValue().thread);
                producersInMethod.put(e.getKey(), producer);
            }
        }
        return producersInMethod;
    }

    /**
     * This implementation finds all methods marked with a {@link Subscribe} annotation.
     *
     * @param listener Object
     * @return Map<EventType, Set < SubscriberBaseEvent>>
     */
    static @NotNull Map<EventType, Set<SubscriberBaseEvent>> findAllSubscribers(@NotNull Object listener) {
        Class<?> listenerClass = listener.getClass();
        Map<EventType, Set<SubscriberBaseEvent>> subscribersInMethod = new HashMap<>(9);
        Map<EventType, Set<SourceMethod>> methods = SUBSCRIBERS_CACHE.get(listenerClass);
        if (null == methods) {
            methods = new HashMap<>(9);
            loadAnnotatedSubscriberMethods(listenerClass, methods);
        }
        if (!methods.isEmpty()) {
            for (Map.Entry<EventType, Set<SourceMethod>> entry : methods.entrySet()) {
                Set<SubscriberBaseEvent> subscribers = new HashSet<>();
                for (SourceMethod sourceMethod : entry.getValue()) {
                    subscribers.add(new SubscriberBaseEvent(listener, sourceMethod.method, sourceMethod.thread));
                }
                subscribersInMethod.put(entry.getKey(), subscribers);
            }
        }
        return subscribersInMethod;
    }

    private static class SourceMethod {
        private final EventThread thread;
        private final Method method;

        private SourceMethod(EventThread thread, Method method) {
            this.thread = thread;
            this.method = method;
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return super.equals(obj);
        }
    }
}