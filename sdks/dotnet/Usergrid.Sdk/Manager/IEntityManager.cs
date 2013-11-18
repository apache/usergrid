using Usergrid.Sdk.Model;

namespace Usergrid.Sdk.Manager
{
    public interface IEntityManager
    {
        T CreateEntity<T>(string collection, T entity);
        void DeleteEntity(string collection, string identifer /*name or uuid*/);
        void UpdateEntity<T>(string collection, string identifer /*name or uuid*/, T entity);
        T GetEntity<T>(string collectionName, string identifer /*name or uuid*/);
		UsergridCollection<T> GetEntities<T> (string collectionName, int limit = 10, string query = null);
		UsergridCollection<T> GetNextEntities<T>(string collectionName, string query = null);
		UsergridCollection<T> GetPreviousEntities<T>(string collectionName, string query = null);
    }
}