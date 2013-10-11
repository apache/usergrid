Imports Usergrid.Sdk

Public Class MainWindow

    Private Sub MainWindow_Load(sender As Object, e As EventArgs) Handles MyBase.Load

    End Sub
    Public Sub clearBooks()
        booksGrid.Rows.Clear()
    End Sub
    Public Sub updateBooks()
        'Clear the current list of books
        clearBooks()

        'Look for the books
        Dim books As UsergridCollection(Of Book)
        Dim book As Book
        books = Settings.client.GetEntities(Of Book)("books", 10)
        Do
            Dim i As Integer = 0
            While i < books.Count
                book = books(i)
                booksGrid.Rows.Add(book.title, book.author, book.uuid)
                i = i + 1
            End While
            books = Settings.client.GetNextEntities(Of Book)("books")
        Loop While books.Count > 0
    End Sub

    Private Sub btnRetrieveBooks_Click(sender As Object, e As EventArgs) Handles btnRetrieveBooks.Click
        booksGrid.Rows.Clear()
        updateBooks()
    End Sub

    Private Sub btnSettings_Click(sender As Object, e As EventArgs) Handles btnSettings.Click
        SettingsForm.Show()
    End Sub


    Private Sub btnAddBook_Click(sender As Object, e As EventArgs) Handles btnAddABook.Click
        AddBookForm.Show()
    End Sub

    Private Sub btnDeleteABook_Click(sender As Object, e As EventArgs) Handles btnDeleteABook.Click
        Dim i As Integer
        Dim uuid As String
        i = booksGrid.CurrentRow.Index
        uuid = booksGrid.Item(2, i).Value
        Settings.client.DeleteEntity("books", uuid)
        booksGrid.Rows.RemoveAt(i)
        clearBooks()
        updateBooks()
    End Sub

End Class
