/*
 * Copyright (c) 2015, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package org.glassfish.soteria.cdi;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Optional.empty;
import static org.glassfish.soteria.Utils.isEmpty;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

import javax.el.ELProcessor;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

public class CdiUtils {
	
    public static <A extends Annotation> Optional<A> getAnnotation(BeanManager beanManager, Annotated annotated, Class<A> annotationType) {

        annotated.getAnnotation(annotationType);

        if (annotated.getAnnotations().isEmpty()) {
            return empty();
        }

        if (annotated.isAnnotationPresent(annotationType)) {
            return Optional.of(annotated.getAnnotation(annotationType));
        }

        Queue<Annotation> annotations = new LinkedList<>(annotated.getAnnotations());

        while (!annotations.isEmpty()) {
            Annotation annotation = annotations.remove();

            if (annotation.annotationType().equals(annotationType)) {
                return Optional.of(annotationType.cast(annotation));
            }

            if (beanManager.isStereotype(annotation.annotationType())) {
                annotations.addAll(
                    beanManager.getStereotypeDefinition(
                        annotation.annotationType()
                    )
                );
            }
        }

        return empty();
    }
    
    public static void addAnnotatedTypes(BeforeBeanDiscovery beforeBean, BeanManager beanManager, Class<?>... types) {
        for (Class<?> type : types) {
            beforeBean.addAnnotatedType(beanManager.createAnnotatedType(type), "Soteria " + type.getName());
        }
    }
    
    public static <A extends Annotation> Optional<A> getAnnotation(BeanManager beanManager, Class<?> annotatedClass, Class<A> annotationType) {

        if (annotatedClass.isAnnotationPresent(annotationType)) {
            return Optional.of(annotatedClass.getAnnotation(annotationType));
        }

        Queue<Annotation> annotations = new LinkedList<>(asList(annotatedClass.getAnnotations()));

        while (!annotations.isEmpty()) {
            Annotation annotation = annotations.remove();

            if (annotation.annotationType().equals(annotationType)) {
                return Optional.of(annotationType.cast(annotation));
            }

            if (beanManager.isStereotype(annotation.annotationType())) {
                annotations.addAll(
                    beanManager.getStereotypeDefinition(
                        annotation.annotationType()
                    )
                );
            }
        }

        return empty();
    }
    
    public static BeanManager getBeanManager() {
        BeanManager beanManager = jndiLookup("java:comp/BeanManager");
        
        if (beanManager == null) {
            // Tomcat and Jetty
            beanManager = jndiLookup("java:comp/env/BeanManager");
        }
        
        return beanManager;
    }
    
    // 
    public static <T> T getBeanReference(Class<T> type, Annotation... qualifiers) {
        return type.cast(getBeanReferenceByType(getBeanManager(), type, qualifiers));
    }
    
    /**
     * @param beanManager the bean manager
     * @param type the required bean type the reference must have
     * @param qualifiers the required qualifiers the reference must have
     * @return a bean reference adhering to the required type and qualifiers
     */
    public static <T> T getBeanReference(BeanManager beanManager, Class<T> type, Annotation... qualifiers) {
        return type.cast(getBeanReferenceByType(beanManager, type, qualifiers));
    }

    public static Object getBeanReferenceByType(BeanManager beanManager, Type type, Annotation... qualifiers) {

        Object beanReference = null;

        Bean<?> bean = beanManager.resolve(beanManager.getBeans(type, qualifiers));
        if (bean != null) {
            beanReference = beanManager.getReference(bean, type, beanManager.createCreationalContext(bean));
        }

        return beanReference;
    }
    
    @SuppressWarnings("unchecked")
    private static <T> T getContextualReference(Class<T> type, BeanManager beanManager, Set<Bean<?>> beans) {
        
        Object beanReference = null;
        
        Bean<?> bean = beanManager.resolve(beans);
        if (bean != null) {
            beanReference = beanManager.getReference(bean, type, beanManager.createCreationalContext(bean));
        }
        
        return (T) beanReference;
    }

    public static <T> List<T> getBeanReferencesByType(Class<T> type, boolean optional) {
        BeanManager beanManager =  getBeanManager();

        Set<Bean<?>> beans = getBeanDefinitions(type, optional, beanManager);

        List<T> result = new ArrayList<>(beans.size());

        for (Bean<?> bean : beans) {
            result.add(getContextualReference(type, beanManager, Collections.singleton(bean)));
        }

        return result;
    }
    
    public static ELProcessor getELProcessor(ELProcessor elProcessor) {
        if (elProcessor != null) {
            return elProcessor;
        }
        
        return getELProcessor();
    }
    
    public static ELProcessor getELProcessor() {
        ELProcessor elProcessor = new ELProcessor();
        elProcessor.getELManager().addELResolver(getBeanManager().getELResolver());
        
        return elProcessor;
    }

    private static <T> Set<Bean<?>> getBeanDefinitions(Class<T> type, boolean optional, BeanManager beanManager) {
        Set<Bean<?>> beans = beanManager.getBeans(type, new AnyAnnotationLiteral());
        if (!isEmpty(beans)) {
            return beans;
        } 
        
        if (optional) {
            return emptySet();
        } 
        
        throw new IllegalStateException("Could not find beans for Type=" + type);
    }

    @SuppressWarnings("unchecked")
    public static <T> T jndiLookup(String name) {
        InitialContext context = null;
        try {
            context = new InitialContext();
            return (T) context.lookup(name);
        } catch (NamingException e) {
            if (is(e, NameNotFoundException.class)) {
                return null;
            } else {
                throw new IllegalStateException(e);
            }
        } finally {
            close(context);
        }
    }

    private static void close(InitialContext context) {
        try {
            if (context != null) {
                context.close();
            }
        } catch (NamingException e) {
            throw new IllegalStateException(e);
        }
    }
    
    public static <T extends Throwable> boolean is(Throwable exception, Class<T> type) {
        Throwable unwrappedException = exception;

        while (unwrappedException != null) {
            if (type.isInstance(unwrappedException)) {
                return true;
            }

            unwrappedException = unwrappedException.getCause();
        }

        return false;
    }

}
