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
using Newtonsoft.Json;

namespace Usergrid.Sdk.Model
{
	public class UsergridActivity
	{
		public UsergridActivity(){}

		public UsergridActivity(UsergridUser user, UsergridImage image = null){
			Actor = new UsergridActor {
				DisplayName = user.Name,
				Email = user.Email,
				Image = image,
				UserName = user.UserName,
				Uuid = user.Uuid
			};
		}

		public UsergridActor Actor {get;set;}

		public string Verb {get;set;}

		public string Content { get; set;}

		[JsonProperty("published")]
		private long PublishedLong {  get;  set;}

		[JsonIgnore]
		public DateTime PublishedDate {
			get { return PublishedLong.FromUnixTime(); }
		}
	}
}

