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
package com.usergrid.count;

/**
 * @author zznate
 */
public class CounterProcessingUnavailableException extends RuntimeException {

    private static final String ERR_MSG = "Counter was not processed. Reason: ";

    public CounterProcessingUnavailableException() {
        super(ERR_MSG);
    }

    public CounterProcessingUnavailableException(String errMsg) {
        super(ERR_MSG + errMsg);
    }

    public CounterProcessingUnavailableException(String errMsg, Throwable t) {
        super(ERR_MSG + errMsg, t);
    }
}
