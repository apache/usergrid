package org.apache.usergrid.persistence.collection.mvcc.stage.impl;


import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.google.inject.BindingAnnotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;


/**
 * Marks the create pipeline
 *
 * @author tnine
 */
@BindingAnnotation
@Target( { FIELD, PARAMETER, METHOD } )
@Retention( RUNTIME )
public @interface UpdatePipeline {}
