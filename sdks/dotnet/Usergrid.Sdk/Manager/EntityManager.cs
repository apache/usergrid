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
using System.Collections.Generic;
using System.Linq;
using System.Net;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using RestSharp;
using Usergrid.Sdk.Model;
using Usergrid.Sdk.Payload;

namespace Usergrid.Sdk.Manager {
    internal class EntityManager : ManagerBase, IEntityManager {
        private readonly IDictionary<Type, Stack<string>> _cursorStates = new Dictionary<Type, Stack<string>>();
        private readonly IDictionary<Type, int> _pageSizes = new Dictionary<Type, int>();

        internal EntityManager(IUsergridRequest request) : base(request) {}

        public T CreateEntity<T>(string collection, T entity) {
            IRestResponse response = Request.ExecuteJsonRequest("/" + collection, Method.POST, entity);
            ValidateResponse(response);
            var returnedEntity = JsonConvert.DeserializeObject<UsergridGetResponse<T>>(response.Content);

            return returnedEntity.Entities.FirstOrDefault();
        }

        public void DeleteEntity(string collection, string identifer) {
            IRestResponse response = Request.ExecuteJsonRequest(string.Format("/{0}/{1}", collection, identifer), Method.DELETE);
            ValidateResponse(response);
        }

        public void UpdateEntity<T>(string collection, string identifer, T entity) {
            IRestResponse response = Request.ExecuteJsonRequest(string.Format("/{0}/{1}", collection, identifer), Method.PUT, entity);
            ValidateResponse(response);
        }

        public T GetEntity<T>(string collectionName, string identifer) {
            IRestResponse response = Request.ExecuteJsonRequest(string.Format("/{0}/{1}", collectionName, identifer), Method.GET);

            if (response.StatusCode == HttpStatusCode.NotFound)
                return default(T);
            ValidateResponse(response);

            var entity = JsonConvert.DeserializeObject<UsergridGetResponse<T>>(response.Content);

            return entity.Entities.FirstOrDefault();
        }

        public UsergridCollection<T> GetEntities<T>(string collectionName, int limit = 10, string query = null) {
            _pageSizes.Remove(typeof (T));
            _pageSizes.Add(typeof (T), limit);

            string url = string.Format("/{0}?limit={1}", collectionName, limit);
            if (query != null)
                url += "&query=" + query;

            IRestResponse response = Request.ExecuteJsonRequest(url, Method.GET);

            if (response.StatusCode == HttpStatusCode.NotFound)
                return new UsergridCollection<T>();

            ValidateResponse(response);

            var getResponse = JsonConvert.DeserializeObject<UsergridGetResponse<T>>(response.Content);
            var collection = new UsergridCollection<T>(getResponse.Entities);

            _cursorStates.Remove(typeof (T));

            if (getResponse.Cursor != null) {
                collection.HasNext = true;
                collection.HasPrevious = false;
                _cursorStates.Add(typeof (T), new Stack<string>(new[] {null, null, getResponse.Cursor}));
            }

            return collection;
        }

        public UsergridCollection<T> GetNextEntities<T>(string collectionName, string query = null) {
            if (!_cursorStates.ContainsKey(typeof (T))) {
                return new UsergridCollection<T>();
            }

            Stack<string> stack = _cursorStates[typeof (T)];
            string cursor = stack.Peek();

            if (cursor == null) {
                return new UsergridCollection<T> {HasNext = false, HasPrevious = true};
            }

            int limit = _pageSizes[typeof (T)];

            string url = string.Format("/{0}?cursor={1}&limit={2}", collectionName, cursor, limit);
            if (query != null)
                url += "&query=" + query;

            IRestResponse response = Request.ExecuteJsonRequest(url, Method.GET);

            if (response.StatusCode == HttpStatusCode.NotFound)
                return new UsergridCollection<T>();

            ValidateResponse(response);

            var getResponse = JsonConvert.DeserializeObject<UsergridGetResponse<T>>(response.Content);
            var collection = new UsergridCollection<T>(getResponse.Entities);

            if (getResponse.Cursor != null) {
                collection.HasNext = true;
                stack.Push(getResponse.Cursor);
            }
            else {
                stack.Push(null);
            }

            collection.HasPrevious = true;

            return collection;
        }

        public UsergridCollection<T> GetPreviousEntities<T>(string collectionName, string query = null) {
            if (!_cursorStates.ContainsKey(typeof (T))) {
                var error = new UsergridError
                    {
                        Error = "cursor_not_initialized",
                        Description = "Call GetEntities method to initialize the cursor"
                    };
                throw new UsergridException(error);
            }

            Stack<string> stack = _cursorStates[typeof (T)];
            stack.Pop();
            stack.Pop();
            string cursor = stack.Peek();

            int limit = _pageSizes[typeof (T)];

            if (cursor == null) {
                return GetEntities<T>(collectionName, limit);
            }

            string url = string.Format("/{0}?cursor={1}&limit={2}", collectionName, cursor, limit);
            if (query != null)
                url += "&query=" + query;

            IRestResponse response = Request.ExecuteJsonRequest(url, Method.GET);

            if (response.StatusCode == HttpStatusCode.NotFound)
                return new UsergridCollection<T>();

            ValidateResponse(response);

            var getResponse = JsonConvert.DeserializeObject<UsergridGetResponse<T>>(response.Content);
            var collection = new UsergridCollection<T>(getResponse.Entities)
                {
                    HasNext = true, 
                    HasPrevious = true
                };

            stack.Push(getResponse.Cursor);

            return collection;
        }
    }
}
