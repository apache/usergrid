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
package org.apache.usergrid.mongo.utils;


import org.bson.BSONDecoder;
import org.bson.BSONEncoder;
import org.bson.BasicBSONDecoder;
import org.bson.BasicBSONEncoder;


public class BSONUtils {

    public static BSONEncoder encoder() {
        return _staticEncoder.get();
    }


    public static BSONDecoder decoder() {
        return _staticDecoder.get();
    }


    static ThreadLocal<BSONEncoder> _staticEncoder = new ThreadLocal<BSONEncoder>() {
        @Override
        protected BSONEncoder initialValue() {
            return new BasicBSONEncoder();
        }
    };

    static ThreadLocal<BSONDecoder> _staticDecoder = new ThreadLocal<BSONDecoder>() {
        @Override
        protected BSONDecoder initialValue() {
            return new BasicBSONDecoder();
        }
    };
}
