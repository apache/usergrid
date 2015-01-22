using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using System.Net;
using System.IO;
using System.Runtime.Serialization;
using System.Runtime.Serialization.Json;
using System.Text;
using Newtonsoft.Json.Linq;
using Newtonsoft.Json;
using System.Net.Http;
using System.Net.Http.Headers;
using Windows.Storage;
using Windows.Networking.PushNotifications;

namespace Usergrid.Notifications
{
    public class Usergrid : UsergridHttpClient
    {
        private string url;
        private string token;
        private HttpClient client;
        private PushClient push;
        private string managementUrl;

        public Usergrid(string url, string org, string app)
        {
            string urlWithSlash = url.EndsWith("/",StringComparison.CurrentCulture) ? url : url+"/";
            this.url = String.Format("{0}{1}/{2}/", urlWithSlash, org, app);
            this.managementUrl = urlWithSlash + "management/";
            this.client = new HttpClient();
            push = new PushClient(this);
        }

        public async Task Authenticate(string user, string password, bool isManagement)
        {
            var url = "token";
            var jsonObject = new JObject();
            jsonObject.Add("username", user);
            jsonObject.Add("password", password);
            jsonObject.Add("grant_type", "password");
            HttpResponseMessage response = await SendAsync(HttpMethod.Post,url, jsonObject, isManagement);

            if (response.StatusCode == HttpStatusCode.OK)
            {
                JObject jsonResponse = await GetJsonResponse(response);
                this.token = jsonResponse.GetValue("access_token").Value<String>();
                client.DefaultRequestHeaders.Add("X-Authorization", token);
            }
            else
            {
                throw new Exception("Authentication failed");
            }
        }

        public async Task<JObject> GetJsonResponse(HttpResponseMessage response)
        {
            JObject jsonResponse = null;
            using (var stream = await response.Content.ReadAsStreamAsync())
            {
                using (var reader = new StreamReader(stream, Encoding.UTF8))
                {
                    jsonResponse = JObject.Parse(reader.ReadToEnd());
                }
            }
            return jsonResponse;
        }

        private HttpContent getJsonBody(Object jsonObject)
        {
            var content =  new StringContent(JsonConvert.SerializeObject(jsonObject));
            return content;
        }
        public  Task<HttpResponseMessage> SendAsync(HttpMethod method, string url, object obj)
        {
            return SendAsync(method, url, obj, false);
        }
        public Task<HttpResponseMessage> SendAsync(HttpMethod method, string url, object obj, bool useManagementUrl)
        {
            HttpRequestMessage message = new HttpRequestMessage(HttpMethod.Post, (useManagementUrl ? this.managementUrl : this.url) + url);
            if(obj!=null)
                message.Content = getJsonBody(obj);
            message.Headers.Accept.Add(new MediaTypeWithQualityHeaderValue( "application/json"));
            return this.client.SendAsync(message);

        }
   
        public PushClient Push
        {
            get { return push; }
        }


    }

    public interface UsergridHttpClient
    {
        Task<HttpResponseMessage>SendAsync(HttpMethod method,string url, object obj);
        Task<JObject> GetJsonResponse(HttpResponseMessage response);

    }
    public class PushClient
    {
        private UsergridHttpClient usergrid;
        
        public PushClient(UsergridHttpClient usergrid)
        { 
            this.usergrid = usergrid; 
        }

        public async Task<bool> SendNotification(string message)
        {
            if (DeviceId == null)
            {
                throw new Exception("Please call PushClient.RegisterDevice first.");
            }
            var jsonObject = new JObject();
            var payloads = new JObject();
            var payload = new JObject();
            payload.Add("toast", new JValue(message));
            payloads.Add("winphone", payload);
            jsonObject.Add("payloads",payloads);
            jsonObject.Add("debug", true);
            var response = await usergrid.SendAsync(HttpMethod.Post,String.Format("devices/{0}/notifications",deviceId), jsonObject);
            var status = response.StatusCode;
            JObject jsonResponse = await usergrid.GetJsonResponse(response);
            return (status == HttpStatusCode.OK);
        }

        public async Task RegisterDevice(string notifier)
        {
            this.Notifier = notifier;
            var channel = await PushNotificationChannelManager.CreatePushNotificationChannelForApplicationAsync();
            var settings = ApplicationData.Current.LocalSettings;
            if (!settings.Values.ContainsKey("currentDeviceId"))
            {
                Guid uuid = await registerDevice(notifier, channel.Uri);
                settings.Values.Add("currentDeviceId", uuid);
                this.DeviceId = uuid;
            }
            else
            {
                object tempId;
                settings.Values.TryGetValue("currentDeviceId", out tempId);
                this.DeviceId = Guid.Parse(tempId.ToString());
                if (!await DeviceExists(DeviceId))
                {
                    Guid uuid = await registerDevice(notifier, channel.Uri);
                    settings.Values["currentDeviceId"] = uuid;
                    this.DeviceId = uuid;
                }

            }

           
        }
      
        public async Task AddNotifier(string name, string sid, string apiKey)
        {
            JObject obj = new JObject();
            obj.Add("name", new JValue(name));
            obj.Add("sid", new JValue(sid));
            obj.Add("provider", new JValue("windows"));
            obj.Add("apiKey", new JValue(apiKey));
            obj.Add("logging", new JValue(true));
            HttpResponseMessage response = await usergrid.SendAsync(HttpMethod.Post, "notifiers", obj);

            JObject jsonResponse = await usergrid.GetJsonResponse(response);
            if (jsonResponse != null)
            {
                var body = jsonResponse.GetValue("entities").ToString();
            }
        }

        public async Task<bool> DeviceExists(Guid deviceId)
        {
            HttpResponseMessage response = await usergrid.SendAsync(HttpMethod.Get, "devices/"+deviceId, null);
            JObject jsonResponse = await usergrid.GetJsonResponse(response);
            if (jsonResponse != null)
            {
                    var body = jsonResponse.GetValue("entities");
                    return body!=null &&  body.Value<JArray>().Count > 0;
            }
            else { return false; }
        }

        private async Task<Guid> registerDevice(string notifier, string uri)
        {
            JObject obj = new JObject();
            obj.Add(notifier + ".notifier.id", new JValue(uri));
            HttpResponseMessage response = await usergrid.SendAsync(HttpMethod.Post, "devices/", obj);

            JObject jsonResponse = await usergrid.GetJsonResponse(response);

            if (response.StatusCode == HttpStatusCode.OK && jsonResponse != null)
            {
                var entity = jsonResponse.GetValue("entities").Value<JArray>()[0];
                var uuid = Guid.Parse(entity.Value<String>("uuid"));
                return uuid;
            }
            else { return Guid.Empty; }
        }

        public string Notifier { get; set; }
        private Guid DeviceId { get; set; }

    }
}
