Imports Usergrid.Sdk

Public Class Form1

    Private Sub Form1_Load(sender As Object, e As EventArgs) Handles MyBase.Load

    End Sub
    Public Sub clearBooks()
        booksGrid.Rows.Clear()
    End Sub
    Public Sub updateBooks()
        'Look for the books
        Dim books As UsergridCollection(Of Book)
        Dim book As Book
        books = Settings.client.GetEntities(Of Book)("books", 10)
        Dim i As Integer = 0
        While i < books.Count
            book = books(i)
            booksGrid.Rows.Add(book.title, book.author, book.uuid)
            i = i + 1
        End While
    End Sub

    Private Sub Button1_Click(sender As Object, e As EventArgs) Handles btnRetrieveBooks.Click
        booksGrid.Rows.Clear()
        updateBooks()
    End Sub


    Private Sub booksGrid_UserDeletedRow(sender As Object, e As DataGridViewRowEventArgs) _
     Handles booksGrid.UserDeletedRow

        Dim messageBoxVB As New System.Text.StringBuilder()
        messageBoxVB.AppendFormat("{0} = {1}", "Row", e.Row)
        messageBoxVB.AppendLine()
        MessageBox.Show(messageBoxVB.ToString(), "UserDeletedRow Event")

    End Sub


    Private Sub Button2_Click(sender As Object, e As EventArgs) Handles btnSettings.Click
        SettingsForm.Show()
    End Sub

    
    Private Sub Button3_Click(sender As Object, e As EventArgs) Handles btnAddABook.Click
        AddBookForm.Show()
    End Sub

    Private Sub Button4_Click(sender As Object, e As EventArgs) Handles btnDeleteABook.Click
        Dim i As Integer
        Dim uuid As String
        i = booksGrid.CurrentRow.Index
        uuid = booksGrid.Item(2, i).Value
        Settings.client.DeleteEntity("books", uuid)
        booksGrid.Rows.RemoveAt(i)
        clearBooks()
        updateBooks()
    End Sub

    Private Sub booksGrid_CellContentClick(sender As Object, e As DataGridViewCellEventArgs) Handles booksGrid.CellContentClick

    End Sub
End Class
