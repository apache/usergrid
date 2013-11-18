Imports Usergrid.Sdk
Imports Usergrid.Sdk.Model

Public Class Settings

    Private Sub btnSave_Click(sender As Object, e As EventArgs) Handles btnSave.Click
        Dim client As Client = New Client(txtOrg.Text, txtApp.Text)
        client.Login(txtUserName.Text, txtPassword.Text, AuthType.User)
        Globals.client = client
        MessageeMainWindow.UpdateUsers()
        Me.Close()
    End Sub
End Class