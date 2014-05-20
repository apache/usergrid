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

Public Class SettingsForm

    Private Sub btnSave_Click(sender As Object, e As EventArgs) Handles btnSave.Click
        Settings.userName = userName.Text
        Settings.password = password.Text
        Settings.org = org.Text
        Settings.app = app.Text
        Settings.client = New Client(Settings.org, Settings.app)
        Settings.client.Login(Settings.userName, Settings.password, AuthType.User)
        Me.Close()
    End Sub

    Private Sub SettingsForm_Load(sender As Object, e As EventArgs) Handles MyBase.Load
        If Settings.userName IsNot Nothing Then
            userName.Text = Settings.userName
        End If
        If Settings.password IsNot Nothing Then
            password.Text = Settings.password
        End If
        If Settings.org IsNot Nothing Then
            org.Text = Settings.org
        End If
        If Settings.app IsNot Nothing Then
            app.Text = Settings.app
        End If
    End Sub
End Class
