using System.Collections.Generic;
using System.Net;
using Newtonsoft.Json;
using RestSharp;
using Usergrid.Sdk.Model;
using Usergrid.Sdk.Payload;

namespace Usergrid.Sdk.Manager
{
    internal class ConnectionManager : ManagerBase, IConnectionManager
    {
        internal ConnectionManager(IUsergridRequest request) : base(request)
        {
        }

        public void CreateConnection(Connection connection) 
        {
            // e.g. /user/fred/following/user/barney
            IRestResponse response = Request.ExecuteJsonRequest(string.Format(
                "/{0}/{1}/{2}/{3}/{4}",
                connection.ConnectorCollectionName,
                connection.ConnectorIdentifier,
                connection.ConnectionName,
                connection.ConnecteeCollectionName,
                connection.ConnecteeIdentifier), Method.POST);

            ValidateResponse(response);
        }

        public IList<UsergridEntity> GetConnections(Connection connection) 
        {
            // e.g. /user/fred/following
            IRestResponse response = Request.ExecuteJsonRequest(string.Format("/{0}/{1}/{2}",
                                                                              connection.ConnectorCollectionName,
                                                                              connection.ConnectorIdentifier,
                                                                              connection.ConnectionName), Method.GET);

            if (response.StatusCode == HttpStatusCode.NotFound)
            {
                return default(List<UsergridEntity>);
            }

            ValidateResponse(response);

            var entity = JsonConvert.DeserializeObject<UsergridGetResponse<UsergridEntity>>(response.Content);

            return entity.Entities;
        }

        public IList<TConnectee> GetConnections<TConnectee>(Connection connection) 
        {
            // e.g. /user/fred/following/user
            IRestResponse response = Request.ExecuteJsonRequest(string.Format("/{0}/{1}/{2}/{3}",
                                                                              connection.ConnectorCollectionName,
                                                                              connection.ConnectorIdentifier, 
                                                                              connection.ConnectionName,
                                                                              connection.ConnecteeCollectionName), Method.GET);

            if (response.StatusCode == HttpStatusCode.NotFound)
            {
                return default(List<TConnectee>);
            }

            ValidateResponse(response);

            var entity = JsonConvert.DeserializeObject<UsergridGetResponse<TConnectee>>(response.Content);

            return entity.Entities;
        }

        public void DeleteConnection(Connection connection)
        {
            IRestResponse response = Request.ExecuteJsonRequest(string.Format(
                "/{0}/{1}/{2}/{3}/{4}",
                connection.ConnectorCollectionName,
                connection.ConnectorIdentifier,
                connection.ConnectionName,
                connection.ConnecteeCollectionName,
                connection.ConnecteeIdentifier), Method.DELETE);

            ValidateResponse(response);
        }
    }
}