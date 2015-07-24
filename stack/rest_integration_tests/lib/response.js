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
module.exports = {};
module.exports.getError = function(err, response) {
    return err || (response.statusCode >= 400 ? response.body : null)
};

module.exports.distanceInMeters = function(location1, location2) {
    var R = 6371000;
    var a = 0.5 - Math.cos((location2.latitude - location1.latitude) * Math.PI / 180) / 2 +
        Math.cos(location1.latitude * Math.PI / 180) * Math.cos(location2.latitude * Math.PI / 180) *
        (1 - Math.cos((location2.longitude - location1.longitude) * Math.PI / 180)) / 2;

    var distance = R * 2 * Math.asin(Math.sqrt(a));
    return distance;
}
