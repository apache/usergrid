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
package org.usergrid.utils;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;

public class TimeUtils {

	public static long millisToMinutes(long millis) {
		return millis / 1000 / 60;
	}

	public static long minutesToMillis(long minutes) {
		return minutes * 60 * 100;
	}

	public static long millisToHours(long millis) {
		return millis / 1000 / 60 / 60;
	}

	public static long hoursToMillis(long hours) {
		return hours * 60 * 60 * 100;
	}

	public static long millisToDays(long millis) {
		return millis / 1000 / 60 / 60 / 24;
	}

	public static long daysToMillis(long days) {
		return days * 24 * 60 * 60 * 100;
	}

	public static long millisToWeeks(long millis) {
		return millis / 1000 / 60 / 60 / 24 / 7;
	}

	public static long weeksToMillis(long weeks) {
		return weeks * 7 * 24 * 60 * 60 * 100;
	}

  public static long millisFromDuration(String durationStr) {
    long total = 0;
    MultiplierToken mt;
    long dur;
    for (String val : Splitter.on(',')
           .trimResults()
           .omitEmptyStrings()
           .split(durationStr)) {
      dur = Long.parseLong(CharMatcher.DIGIT.retainFrom(val));
      mt = MultiplierToken.from(val.charAt(val.length() - 1));
      total += (mt.multiplier * dur);
    }
    return total;
  }

  private enum MultiplierToken {
    SEC_TOKEN('s',1000L),
    MIN_TOKEN('m',60000L),
    HOUR_TOKEN('h',3600000L),
    DAY_TOKEN('d',86400000L);

    final char token;
    final long multiplier;

    MultiplierToken(char token, long multiplier) {
      this.token = token;
      this.multiplier = multiplier;
    }

    static MultiplierToken from(char c) {
      switch(c) {
        case 's': return SEC_TOKEN;
        case 'm': return MIN_TOKEN;
        case 'h': return HOUR_TOKEN;
        case 'd': return DAY_TOKEN;
      }
      throw new IllegalArgumentException("Duration token was not on of [s,m,h,d] but was " + c);
    }
  }


}
