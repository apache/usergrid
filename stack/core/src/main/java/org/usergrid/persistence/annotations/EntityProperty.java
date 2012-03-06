/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.usergrid.persistence.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value = { ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface EntityProperty {

	String name() default "";

	boolean indexed() default false;

	boolean basic() default false;

	boolean required() default false;

	boolean indexedInConnections() default false;

	boolean mutable() default true;

	boolean unique() default false;

	boolean aliasProperty() default false;

	boolean pathBasedName() default false;

	boolean fulltextIndexed() default false;

	boolean publicVisible() default false;

	boolean includedInExport() default true;

	boolean timestamp() default false;

}
