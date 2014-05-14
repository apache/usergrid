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
using Newtonsoft.Json.Linq;

namespace Usergrid.Sdk.Model
{
	public class EntitySerializer : JsonConverter
	{
		public override object ReadJson (JsonReader reader, Type objectType, object existingValue, JsonSerializer serializer)
		{
			var jsonSerializer = new JsonSerializer ();

			var obj = JObject.Load(reader); 
			var r1 = obj.CreateReader();
			var r2 = obj.CreateReader ();

			var usergridEntity = jsonSerializer.Deserialize (r1, objectType);
			var entityProperty = usergridEntity.GetType ().GetProperty ("Entity");
			var entity = jsonSerializer.Deserialize(r2, entityProperty.PropertyType);
			entityProperty.SetValue (usergridEntity, entity, null);

			return usergridEntity;
		}

		public override void WriteJson (JsonWriter writer, object value, JsonSerializer serializer)
		{
			throw new NotImplementedException ();
		}

		public override bool CanConvert (Type objectType)
		{
			return typeof(UsergridEntity).IsAssignableFrom (objectType);
		}
	}
}

