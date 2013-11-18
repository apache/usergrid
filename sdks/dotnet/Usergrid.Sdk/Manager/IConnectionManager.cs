using System.Collections.Generic;
using Usergrid.Sdk.Model;

namespace Usergrid.Sdk.Manager {
    internal interface IConnectionManager {
        void CreateConnection(Connection connection);
        IList<UsergridEntity> GetConnections(Connection connection);
        IList<TConnectee> GetConnections<TConnectee>(Connection connection);
        void DeleteConnection(Connection connection);
    }
}