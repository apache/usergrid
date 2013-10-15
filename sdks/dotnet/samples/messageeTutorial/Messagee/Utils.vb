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
