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
package org.usergrid.persistence.cassandra;

import static org.usergrid.utils.ConversionUtils.*;
import static org.apache.commons.codec.digest.DigestUtils.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;
import org.usergrid.persistence.IndexBucketLocator;

/**
 * Simple implementation that does static hashing across 100 rows. Future
 * implementations should be "smart" and create new tokens as required as
 * buckets become too large for an entity property within an application
 * 
 * @author tnine
 * 
 */
public class SimpleIndexBucketLocatorImpl implements IndexBucketLocator {

    public static final BigInteger MINIMUM = BigInteger.ZERO;
    public static final BigInteger MAXIMUM = new BigInteger("" + 2).pow(127);

    private final List<BigInteger> buckets = new ArrayList<BigInteger>(100);
    private final List<String> bucketsString = new ArrayList<String>(100);
    private final int size;

    /**
     * Create a bucket locator with the specified size
     * 
     * @param size
     */
    public SimpleIndexBucketLocatorImpl(int size) {
        for (int i = 0; i < size; i++) {
            BigInteger integer = initialToken(size, i);
            buckets.add(integer);
            bucketsString.add(String.format("%039d", integer));

        }

        this.size = size;
    }

    /**
     * Base constructor that creates a ring of 100 tokens
     */
    public SimpleIndexBucketLocatorImpl() {
        this(100);
    }

    /**
     * Get a token
     * 
     * @param size
     * @param position
     * @return
     */
    private static BigInteger initialToken(int size, int position) {
        BigInteger decValue = MINIMUM;
        if (position != 0)
            decValue = MAXIMUM.divide(new BigInteger("" + size))
                    .multiply(new BigInteger("" + position))
                    .subtract(BigInteger.ONE);
        return decValue;
    }

    /**
     * Get the next token in the ring for this big int.
     * 
     * @param entityId
     * @return
     */
    private String getClosestToken(UUID entityId) {
        BigInteger location = new BigInteger(md5(bytes(entityId)));
        location = location.abs();

        int index = Collections.binarySearch(buckets, location);

        if (index < 0) {
            index = (index + 1) * -1;
        }

        // mod if we need to wrap
        index = index % size;

        return bucketsString.get(index);

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.usergrid.persistence.IndexBucketLocator#getBucket(java.util.UUID,
     * java.lang.String, java.util.UUID, java.lang.String)
     */
    @Override
    public String getBucket(UUID applicationId, String entityType,
            UUID entityId, String propertyName) {
        return getClosestToken(entityId);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.usergrid.persistence.IndexBucketLocator#getBuckets(java.util.UUID,
     * java.lang.String, java.lang.String)
     */
    @Override
    public List<String> getBuckets(UUID applicationId, String entityType,
            String propertyName) {
        return bucketsString;
    }

}
