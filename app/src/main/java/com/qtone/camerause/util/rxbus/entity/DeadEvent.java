package com.qtone.camerause.util.rxbus.entity;

import androidx.annotation.NonNull;

import timber.log.Timber;

/**
 * @decs: DeadEvent
 * Wraps an event that was posted, but which had no subscribers and thus could not be delivered.
 * <p>
 * Subscribing a DeadEvent is useful for debugging or logging, as it can detect misconfigurations in a system's event distribution.
 * @author: 郑少鹏
 * @date: 2019/8/28 10:59
 */
public class DeadEvent {
    /**
     * Creates a new DeadEvent.
     *
     * @param source Object broadcasting the DeadEvent (generally the {@link com.qtone.camerause.util.rxbus.Bus}).
     * @param event  The event that could not be delivered.
     */
    public DeadEvent(@NonNull Object source, @NonNull Object event) {
        Timber.d(this.getClass().getSimpleName(), source.getClass().getSimpleName() + " " + event.getClass().getSimpleName());
    }
}