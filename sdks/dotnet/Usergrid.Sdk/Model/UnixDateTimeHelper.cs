// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements.  See the NOTICE file distributed with
// this work for additional information regarding copyright ownership.
// The ASF licenses this file to You under the Apache License, Version 2.0
// (the "License"); you may not use this file except in compliance with
// the License.  You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

using System;

namespace Usergrid.Sdk
{
	public static class UnixDateTimeHelper
	{
		private const string InvalidUnixEpochErrorMessage = "Unix epoc starts January 1st, 1970";

		/// <summary>
		///   Convert a long into a DateTime
		/// </summary>
		public static DateTime FromUnixTime(this Int64 self)
		{
			var ret = new DateTime(1970, 1, 1);
			return ret.AddSeconds(self/1000);
		}

		/// <summary>
		///   Convert a DateTime into a long
		/// </summary>
		public static Int64 ToUnixTime(this DateTime self)
		{

			if (self == DateTime.MinValue)
			{
				return 0;
			}

			var epoc = new DateTime(1970, 1, 1);
			var delta = self - epoc;

			if (delta.TotalSeconds < 0) throw new ArgumentOutOfRangeException(InvalidUnixEpochErrorMessage);

			return (long) delta.TotalSeconds * 1000;
		}
	}
}

