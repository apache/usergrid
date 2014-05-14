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
