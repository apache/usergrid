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

Public Class MessageeMainWindow
    Public Sub UpdateUsers()
        Dim userList As UsergridCollection(Of UsergridUser)
        userList = Globals.client.GetEntities(Of UsergridUser)("users", 10)
        Dim i As Integer = 0
        While i < userList.Count
            Users.Items.Add(userList(i).UserName)
            i = i + 1
        End While
    End Sub
    Private Sub SettingsToolStripMenuItem_Click(sender As Object, e As EventArgs) Handles SettingsToolStripMenuItem.Click
        Settings.Show()
    End Sub

    Private Sub Users_SelectedIndexChanged(sender As Object, e As EventArgs) Handles Users.SelectedIndexChanged
        UpdateFollowersAndFollowing()
        UpdateYourFeed()
    End Sub
    Public Sub UpdateFollowersAndFollowing()
        Followers.Items.Clear()
        Following.Items.Clear()
        Dim followersList As IList(Of UsergridEntity)
        Dim followingList As IList(Of UsergridEntity)
        Dim conn As Connection = New Connection()
        Dim userName As String = Users.Items(Users.SelectedIndex).ToString
        followingList = Utils.GetFollowers(userName)
        followersList = Utils.GetFollowed(userName)

        Dim i As Integer = 0
        While i < followersList.Count
            Followers.Items.Add(followersList(i).Name)
            i = i + 1
        End While
        i = 0
        While i < followingList.Count
            Following.Items.Add(followingList(i).Name)
            i = i + 1
        End While
    End Sub

    Public Sub UpdateYourFeed()
        YourFeedBox.Text = ""
        Dim userFeed As UsergridCollection(Of UsergridActivity)
        userFeed = Globals.client.GetUserFeed(Of UsergridActivity)(Users.Items(Users.SelectedIndex).ToString)
        Dim i As Integer = 0
        While i < userFeed.Count
            YourFeedBox.Text = YourFeedBox.Text & "Posted By: " & userFeed(i).Actor.DisplayName & " on " & userFeed(i).PublishedDate & vbCrLf
            YourFeedBox.Text = YourFeedBox.Text & userFeed(i).Content & vbCrLf
            YourFeedBox.Text = YourFeedBox.Text & vbCrLf
            i = i + 1
        End While
    End Sub


    Private Sub btnPost_Click(sender As Object, e As EventArgs) Handles btnPost.Click
        Dim activity As UsergridActivity = New UsergridActivity()
        activity.Actor = New UsergridActor
        activity.Actor.DisplayName = Users.Items(Users.SelectedIndex).ToString
        activity.Actor.UserName = Users.Items(Users.SelectedIndex).ToString
        activity.Content = PostMessage.Text
        activity.Verb = "post"
        Globals.client.PostActivity(Of UsergridActivity)(Users.Items(Users.SelectedIndex).ToString, activity)
        UpdateYourFeed()
        PostMessage.Text = ""
    End Sub

    Private Sub UserDetailsToolStripMenuItem_Click(sender As Object, e As EventArgs) Handles UserDetailsToolStripMenuItem.Click
        UserSettings.Show()
       
    End Sub

    Private Sub MessageeMainWindow_Load(sender As Object, e As EventArgs) Handles MyBase.Load
        Globals.mainWindow = Me
    End Sub

    Private Sub MenuStrip2_ItemClicked(sender As Object, e As ToolStripItemClickedEventArgs) Handles MenuStrip2.ItemClicked

    End Sub
End Class
