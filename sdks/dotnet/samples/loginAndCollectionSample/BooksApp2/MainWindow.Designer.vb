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

<Global.Microsoft.VisualBasic.CompilerServices.DesignerGenerated()> _
Partial Class MainWindow
    Inherits System.Windows.Forms.Form

    'Form overrides dispose to clean up the component list.
    <System.Diagnostics.DebuggerNonUserCode()> _
    Protected Overrides Sub Dispose(ByVal disposing As Boolean)
        Try
            If disposing AndAlso components IsNot Nothing Then
                components.Dispose()
            End If
        Finally
            MyBase.Dispose(disposing)
        End Try
    End Sub

    'Required by the Windows Form Designer
    Private components As System.ComponentModel.IContainer

    'NOTE: The following procedure is required by the Windows Form Designer
    'It can be modified using the Windows Form Designer.  
    'Do not modify it using the code editor.
    <System.Diagnostics.DebuggerStepThrough()> _
    Private Sub InitializeComponent()
        Me.booksGrid = New System.Windows.Forms.DataGridView()
        Me.btnRetrieveBooks = New System.Windows.Forms.Button()
        Me.btnSettings = New System.Windows.Forms.Button()
        Me.btnAddABook = New System.Windows.Forms.Button()
        Me.btnDeleteABook = New System.Windows.Forms.Button()
        Me.title = New System.Windows.Forms.DataGridViewTextBoxColumn()
        Me.author = New System.Windows.Forms.DataGridViewTextBoxColumn()
        Me.uuid = New System.Windows.Forms.DataGridViewTextBoxColumn()
        CType(Me.booksGrid, System.ComponentModel.ISupportInitialize).BeginInit()
        Me.SuspendLayout()
        '
        'booksGrid
        '
        Me.booksGrid.AllowUserToAddRows = False
        Me.booksGrid.ColumnHeadersHeightSizeMode = System.Windows.Forms.DataGridViewColumnHeadersHeightSizeMode.AutoSize
        Me.booksGrid.Columns.AddRange(New System.Windows.Forms.DataGridViewColumn() {Me.title, Me.author, Me.uuid})
        Me.booksGrid.Location = New System.Drawing.Point(40, 32)
        Me.booksGrid.Margin = New System.Windows.Forms.Padding(4)
        Me.booksGrid.Name = "booksGrid"
        Me.booksGrid.Size = New System.Drawing.Size(467, 401)
        Me.booksGrid.TabIndex = 0
        '
        'btnRetrieveBooks
        '
        Me.btnRetrieveBooks.Location = New System.Drawing.Point(40, 471)
        Me.btnRetrieveBooks.Margin = New System.Windows.Forms.Padding(4)
        Me.btnRetrieveBooks.Name = "btnRetrieveBooks"
        Me.btnRetrieveBooks.Size = New System.Drawing.Size(129, 28)
        Me.btnRetrieveBooks.TabIndex = 1
        Me.btnRetrieveBooks.Text = "Retrieve Books"
        Me.btnRetrieveBooks.UseVisualStyleBackColor = True
        '
        'btnSettings
        '
        Me.btnSettings.Location = New System.Drawing.Point(407, 471)
        Me.btnSettings.Margin = New System.Windows.Forms.Padding(4)
        Me.btnSettings.Name = "btnSettings"
        Me.btnSettings.Size = New System.Drawing.Size(100, 28)
        Me.btnSettings.TabIndex = 2
        Me.btnSettings.Text = "Settings"
        Me.btnSettings.UseVisualStyleBackColor = True
        '
        'btnAddABook
        '
        Me.btnAddABook.Location = New System.Drawing.Point(191, 471)
        Me.btnAddABook.Margin = New System.Windows.Forms.Padding(4)
        Me.btnAddABook.Name = "btnAddABook"
        Me.btnAddABook.Size = New System.Drawing.Size(100, 28)
        Me.btnAddABook.TabIndex = 3
        Me.btnAddABook.Text = "Add A Book"
        Me.btnAddABook.UseVisualStyleBackColor = True
        '
        'btnDeleteABook
        '
        Me.btnDeleteABook.Location = New System.Drawing.Point(299, 471)
        Me.btnDeleteABook.Margin = New System.Windows.Forms.Padding(4)
        Me.btnDeleteABook.Name = "btnDeleteABook"
        Me.btnDeleteABook.Size = New System.Drawing.Size(100, 28)
        Me.btnDeleteABook.TabIndex = 4
        Me.btnDeleteABook.Text = "Delete Book"
        Me.btnDeleteABook.UseVisualStyleBackColor = True
        '
        'title
        '
        Me.title.HeaderText = "Title"
        Me.title.Name = "title"
        Me.title.Width = 200
        '
        'author
        '
        Me.author.AutoSizeMode = System.Windows.Forms.DataGridViewAutoSizeColumnMode.Fill
        Me.author.HeaderText = "Author"
        Me.author.Name = "author"
        '
        'uuid
        '
        Me.uuid.AutoSizeMode = System.Windows.Forms.DataGridViewAutoSizeColumnMode.Fill
        Me.uuid.HeaderText = "UUID"
        Me.uuid.Name = "uuid"
        Me.uuid.Visible = False
        '
        'Form1
        '
        Me.AutoScaleDimensions = New System.Drawing.SizeF(8.0!, 16.0!)
        Me.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font
        Me.ClientSize = New System.Drawing.Size(578, 535)
        Me.Controls.Add(Me.btnDeleteABook)
        Me.Controls.Add(Me.btnAddABook)
        Me.Controls.Add(Me.btnSettings)
        Me.Controls.Add(Me.btnRetrieveBooks)
        Me.Controls.Add(Me.booksGrid)
        Me.Margin = New System.Windows.Forms.Padding(4)
        Me.Name = "Form1"
        Me.Text = "Books Demo"
        CType(Me.booksGrid, System.ComponentModel.ISupportInitialize).EndInit()
        Me.ResumeLayout(False)

    End Sub
    Friend WithEvents booksGrid As System.Windows.Forms.DataGridView
    Friend WithEvents btnRetrieveBooks As System.Windows.Forms.Button
    Friend WithEvents btnSettings As System.Windows.Forms.Button
    Friend WithEvents btnAddABook As System.Windows.Forms.Button
    Friend WithEvents btnDeleteABook As System.Windows.Forms.Button
    Friend WithEvents title As System.Windows.Forms.DataGridViewTextBoxColumn
    Friend WithEvents author As System.Windows.Forms.DataGridViewTextBoxColumn
    Friend WithEvents uuid As System.Windows.Forms.DataGridViewTextBoxColumn

End Class
