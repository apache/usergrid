' Licensed to the Apache Software Foundation (ASF) under one or more
' contributor license agreements.  See the NOTICE file distributed with
' this work for additional information regarding copyright ownership.
' The ASF licenses this file to You under the Apache License, Version 2.0
' (the "License"); you may not use this file except in compliance with
' the License.  You may obtain a copy of the License at
'
'     http://www.apache.org/licenses/LICENSE-2.0
'
' Unless required by applicable law or agreed to in writing, software
' distributed under the License is distributed on an "AS IS" BASIS,
' WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
' See the License for the specific language governing permissions and
' limitations under the License.

Imports Usergrid.Sdk
Imports Usergrid.Sdk.Model

Public Class Utils

    Public Shared Function GetFollowers(userName As String) As IList
        Dim conn As Connection = New Connection()
        conn.ConnectorIdentifier = userName
        conn.ConnectorCollectionName = "users"
        conn.ConnectionName = "following"
        Return Globals.client.GetConnections(conn)
    End Function

    Public Shared Function GetFollowed(userName As String) As IList
        Dim conn As Connection = New Connection()
        conn.ConnectorIdentifier = userName
        conn.ConnectorCollectionName = "users"
        conn.ConnectionName = "followed"
        Return Globals.client.GetConnections(conn)
    End Function

    Public Shared Sub FollowUser(follower As String, followed As String)
        Dim conn As Connection = New Connection()
        conn.ConnecteeCollectionName = "users"
        conn.ConnectorCollectionName = "users"

        conn.ConnectorIdentifier = follower
        conn.ConnecteeIdentifier = followed
        conn.ConnectionName = "following"
        Globals.client.CreateConnection(conn)
    End Sub

    Public Shared Sub AddFollower(follower As String, followed As String)
        Dim conn As Connection = New Connection()
        conn.ConnecteeCollectionName = "users"
        conn.ConnectorCollectionName = "users"

        conn.ConnectorIdentifier = followed
        conn.ConnecteeIdentifier = follower
        conn.ConnectionName = "followed"
        Globals.client.CreateConnection(conn)
    End Sub

    Public Shared Sub DeleteFollowUser(follower As String, followed As String)
        Dim conn As Connection = New Connection()
        conn.ConnecteeCollectionName = "users"
        conn.ConnectorCollectionName = "users"

        conn.ConnectorIdentifier = follower
        conn.ConnecteeIdentifier = followed
        conn.ConnectionName = "following"
        Dim clist As IList = Globals.client.GetConnections(conn)
        Globals.client.DeleteConnection(conn)
    End Sub

    Public Shared Sub DeleteFollower(follower As String, followed As String)
        Dim conn As Connection = New Connection()
        conn.ConnecteeCollectionName = "users"
        conn.ConnectorCollectionName = "users"

        conn.ConnectorIdentifier = followed
        conn.ConnecteeIdentifier = follower
        conn.ConnectionName = "followed"
        Globals.client.DeleteConnection(conn)
    End Sub

End Class
