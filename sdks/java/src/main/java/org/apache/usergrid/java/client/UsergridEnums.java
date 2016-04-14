/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.java.client;

import org.apache.usergrid.java.client.model.UsergridEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class UsergridEnums {
    public enum UsergridAuthMode {
        NONE,
        USER,
        APP
    }

    public enum UsergridDirection {
        IN("connecting"),
        OUT("connections");

        @NotNull private final String connectionValue;

        UsergridDirection(@NotNull final String connectionValue) {
            this.connectionValue = connectionValue;
        }

        @NotNull
        public String connectionValue() {
            return this.connectionValue;
        }
    }

    public enum UsergridHttpMethod {
        GET,
        PUT,
        POST,
        DELETE;

        @Nullable
        public static UsergridHttpMethod fromString(@NotNull final String stringValue) {
            try {
                return UsergridHttpMethod.valueOf(stringValue.toUpperCase());
            } catch(Exception e) {
                return null;
            }
        }

        @Override @NotNull
        public String toString() {
            return super.toString().toUpperCase();
        }
    }

    public enum UsergridQueryOperator {
        EQUAL("="),
        GREATER_THAN(">"),
        GREATER_THAN_EQUAL_TO(">="),
        LESS_THAN("<"),
        LESS_THAN_EQUAL_TO("<=");

        @NotNull private final String operatorValue;

        UsergridQueryOperator(@NotNull final String operatorValue) {
            this.operatorValue = operatorValue;
        }

        @NotNull
        public String operatorValue() {
            return this.operatorValue;
        }
    }

    public enum UsergridQuerySortOrder {
        ASC,
        DESC;

        @Nullable
        public static UsergridQuerySortOrder fromString(@NotNull final String stringValue) {
            try {
                return UsergridQuerySortOrder.valueOf(stringValue.toUpperCase());
            } catch(Exception e) {
                return null;
            }
        }

        @Override @NotNull
        public String toString() {
            return super.toString().toLowerCase();
        }
    }

    public enum UsergridEntityProperties {
        TYPE,
        UUID,
        NAME,
        CREATED,
        MODIFIED,
        LOCATION;

        @Nullable
        public static UsergridEntityProperties fromString(@NotNull final String stringValue) {
            try {
                return UsergridEntityProperties.valueOf(stringValue.toUpperCase());
            } catch(Exception e) {
                return null;
            }
        }

        @Override @NotNull
        public String toString() {
            return super.toString().toLowerCase();
        }

        public boolean isMutableForEntity(@NotNull final UsergridEntity entity) {
            switch(this) {
                case LOCATION: {
                    return true;
                }
                case NAME: {
                    return entity.isUser();
                }
                case TYPE:
                case UUID:
                case CREATED:
                case MODIFIED:
                default: {
                    return false;
                }
            }
        }
    }

    public enum UsergridUserProperties {
        NAME,
        USERNAME,
        PASSWORD,
        EMAIL,
        ACTIVATED,
        DISABLED,
        PICTURE;

        @Nullable
        public static UsergridUserProperties fromString(@NotNull final String stringValue) {
            try {
                return UsergridUserProperties.valueOf(stringValue.toUpperCase());
            } catch(Exception e) {
                return null;
            }
        }

        @Override @NotNull
        public String toString() {
            return super.toString().toLowerCase();
        }
    }
}
