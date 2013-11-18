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

