package com.qtone.camerause.util.rxbus.annotation;

import com.qtone.camerause.util.rxbus.thread.EventThread;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @decs: Produce
 * Marks a method as an instance producer, as used by {@link com.qtone.camerause.util.rxbus.finder.AnnotatedFinder} and {@link com.qtone.camerause.util.rxbus.Bus}.
 * <p>
 * Bus infers the instance type from the annotated method's return type.
 * Producer methods may return null when there is no appropriate value to share.
 * The calling {@link com.qtone.camerause.util.rxbus.Bus} ignores such returns and posts nothing.
 * @author: 郑少鹏
 * @date: 2019/8/28 10:55
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Produce {
    Tag[] tags() default {};

    EventThread thread() default EventThread.MAIN_THREAD;
}