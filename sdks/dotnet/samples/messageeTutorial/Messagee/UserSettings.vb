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

Public Class UserSettings
    Private userName As String

   
    Private Sub UserSettings_Load(sender As Object, e As EventArgs) Handles MyBase.Load
        Me.userName = MessageeMainWindow.Users.Items(MessageeMainWindow.Users.SelectedIndex).ToString
        userNameLabel.Text = Me.userName
        Dim user As UsergridUser = Globals.client.GetUser(Of UsergridUser)(userName)
        uuidLabel.Text = user.Uuid
        PopulateUsersAndFollowingLists(userName)
        btnAddFollowing.Enabled = False
        btnDeleteFollowing.Enabled = False
    End Sub

    Private Sub PopulateUsersAndFollowingLists(userName As String)
        unconnectedUsers.Items.Clear()
        followingList.Items.Clear()

        Dim following As IList(Of UsergridEntity) = Utils.GetFollowers(userName)
        Dim followersList As IList(Of UsergridEntity) = Utils.GetFollowed(userName)

        Dim allUsers As UsergridCollection(Of UsergridUser) = Globals.client.GetEntities(Of UsergridUser)("users")
        Dim i As Integer = 0
        While i < allUsers.Count 
            If Not EntityInIList(allUsers(i), following) And allUsers(i).UserName <> userName Then
                unconnectedUsers.Items.Add(allUsers(i).UserName)
            End If
            i = i + 1
        End While
        i = 0

        While i < following.Count
            Dim dc As UsergridUser = Globals.client.GetUser(Of UsergridUser)(following(i).Uuid)
            followingList.Items.Add(dc.UserName)
            i = i + 1
        End While
    End Sub
    Private Function EntityInIList(entity As UsergridEntity, ilist As IList) As Boolean
        Dim i As Integer = 0
        While i < ilist.Count
            Dim ilistEntity As UsergridEntity = ilist(i)
            If ilistEntity.Name = entity.Name Then
                Return True
            End If
            i = i + 1
        End While
        Return False
    End Function

    Private Sub btnClose_Click(sender As Object, e As EventArgs) Handles btnClose.Click
        Me.Close()
        Globals.mainWindow.UpdateFollowersAndFollowing()
        Globals.mainWindow.UpdateYourFeed()
    End Sub

    Private Sub btnAddFollowing_Click(sender As Object, e As EventArgs) Handles btnAddFollowing.Click
        Utils.FollowUser(Me.userName, unconnectedUsers.Items(unconnectedUsers.SelectedIndex).ToString)
        Utils.AddFollower(Me.userName, unconnectedUsers.Items(unconnectedUsers.SelectedIndex).ToString)
        btnAddFollowing.Enabled = False
        PopulateUsersAndFollowingLists(Me.userName)
    End Sub

    Private Sub unconnectedUsers_SelectedIndexChanged(sender As Object, e As EventArgs) Handles unconnectedUsers.SelectedIndexChanged
        btnAddFollowing.Enabled = True
    End Sub

    Private Sub followingList_SelectedIndexChanged(sender As Object, e As EventArgs) Handles followingList.SelectedIndexChanged
        btnDeleteFollowing.Enabled = True
    End Sub

    Private Sub btnDeleteFollowing_Click(sender As Object, e As EventArgs) Handles btnDeleteFollowing.Click
        Dim delUserName = followingList.Items(followingList.SelectedIndex).ToString()
        Utils.DeleteFollowUser(Me.userName, delUserName)
        Utils.DeleteFollower(Me.userName, delUserName)
        btnDeleteFollowing.Enabled = False
        PopulateUsersAndFollowingLists(Me.userName)
    End Sub
End Class
