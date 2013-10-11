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