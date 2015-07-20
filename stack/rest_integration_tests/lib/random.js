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
module.exports.randomString = function randomString(length) {
    var text = "";
    var possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    for (var i = 0; i < length; i++) {
        text += possible.charAt(Math.floor(Math.random() * possible.length));
    }
    return text;
}

module.exports.abc = function abc() {
    letters = ["aaa bbb ccc", "ddd eee fff", "ggg hhh iii", "jjj kkk lll"];
    return letters[Math.floor(Math.random() * letters.length)];
}

module.exports.randomNumber = function randomNumber(length) {
    var text = "";
    var possible = "0123456789";

    for (var i = 0; i < length; i++) {
        text += possible.charAt(Math.floor(Math.random() * possible.length));
    }
    return parseInt(text);
}

module.exports.randomEntity = function randomEntity(entitiesArray) {
    return entitiesArray[Math.floor(Math.random()*entitiesArray.length)];
}

module.exports.geo = function geo(center, radius, count) {
    var points = [];
    for (var i = 0; i < count; i++) {
        points.push(randomGeo(center, radius));
    }
    return points;
}

function randomGeo(center, radius) {
    var y0 = center.latitude;
    var x0 = center.longitude;
    var rd = radius / 111300;

    var u = Math.random();
    var v = Math.random();

    var w = rd * Math.sqrt(u);
    var t = 2 * Math.PI * v;
    var x = w * Math.cos(t);
    var y = w * Math.sin(t);

    // var xp = x / Math.cos(y0);

    return {
        'latitude': y + y0,
        'longitude': x + x0
    };
}
