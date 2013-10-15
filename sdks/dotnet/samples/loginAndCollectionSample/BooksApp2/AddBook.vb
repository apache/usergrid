Imports Usergrid.Sdk

Public Class AddBookForm

    Private Sub btnSave_Click(sender As Object, e As EventArgs) Handles btnSave.Click
        Dim b As Book
        b = New Book
        b.title = title.Text
        b.author = author.Text
        Settings.client.CreateEntity(Of Book)("books", b)
        MainWindow.updateBooks()
        Me.Close()
    End Sub

    Private Sub AddBookForm_Load(sender As Object, e As EventArgs) Handles MyBase.Load

    End Sub
End Class