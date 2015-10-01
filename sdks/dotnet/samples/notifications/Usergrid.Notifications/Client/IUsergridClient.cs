/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  *  contributor license agreements.  The ASF licenses this file to You
 *  * under the Apache License, Version 2.0 (the "License"); you may not
 *  * use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.  For additional information regarding
 *  * copyright in this work, please see the NOTICE file in the top level
 *  * directory of this distribution.
 *
 */

using Newtonsoft.Json.Linq;
using System;
using System.Net.Http;
using System.Threading.Tasks;
using Windows.Networking.PushNotifications;
namespace Usergrid.Notifications.Client
{
    /// <summary>
    /// encapsulates usergrid calls, use UsergridFactory
    /// </summary>
    public interface IUsergridClient
    {
        /// <summary>
        /// Authenticate your calls
        /// </summary>
        /// <param name="user"></param>
        /// <param name="password"></param>
        /// <param name="isManagement"></param>
        /// <returns></returns>
        Task Authenticate(string user, string password, bool isManagement);
        /// <summary>
        /// Send a message 
        /// </summary>
        /// <param name="method"></param>
        /// <param name="url"></param>
        /// <param name="obj"></param>
        /// <returns></returns>
        Task<EntityResponse> SendAsync(HttpMethod method, string url, object obj);
        /// <summary>
        /// Send a message
        /// </summary>
        /// <param name="method"></param>
        /// <param name="url"></param>
        /// <param name="obj"></param>
        /// <param name="useManagementUrl"></param>
        /// <returns></returns>
        Task<EntityResponse> SendAsync(HttpMethod method, string url, object obj, bool useManagementUrl);
        /// <summary>
        /// Reference the push client, you should call register before using
        /// </summary>
        IPushClient Push { get; }

        Exception LastException { get; }

    }

    /// <summary>
    /// Only show http calls
    /// </summary>
    public interface IUsergridHttpClient
    {
        /// <summary>
        /// Send Http call async
        /// </summary>
        /// <param name="method"></param>
        /// <param name="url"></param>
        /// <param name="obj"></param>
        /// <param name="useManagementUrl">use management endpoint</param>
        /// <returns></returns>
        Task<EntityResponse> SendAsync(HttpMethod method, string url, object obj, bool useManagementUrl);
        /// <summary>
        /// send Http call async
        /// </summary>
        /// <param name="method"></param>
        /// <param name="url"></param>
        /// <param name="obj"></param>
        /// <returns></returns>
        Task<EntityResponse> SendAsync(HttpMethod method, string url, object obj);
    }

    /// <summary>
    /// Push client, call register
    /// </summary>
    public interface IPushClient
    {
        /// <summary>
        /// the notifier you are currently using
        /// </summary>
        string Notifier { get; set; }
        
        /// <summary>
        /// Device id in usergrid
        /// </summary>
        Guid DeviceId { get; set; }
        /// <summary>
        /// send a toast
        /// </summary>
        /// <param name="message"></param>
        /// <returns></returns>
        Task<bool> SendToast(string message);
        Task<bool> SendRaw(string message);
        /// <summary>
        /// Send a badge update
        /// </summary>
        /// <param name="message"></param>
        /// <returns></returns>
        Task<bool> SendBadge<T>(T message);

        Exception LastException { get; }

    }

}
