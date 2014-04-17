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
Partial Class UserSettings
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
        Me.Label1 = New System.Windows.Forms.Label()
        Me.userNameLabel = New System.Windows.Forms.Label()
        Me.Label2 = New System.Windows.Forms.Label()
        Me.uuidLabel = New System.Windows.Forms.Label()
        Me.unconnectedUsers = New System.Windows.Forms.ListBox()
        Me.Label3 = New System.Windows.Forms.Label()
        Me.followingList = New System.Windows.Forms.ListBox()
        Me.Label4 = New System.Windows.Forms.Label()
        Me.btnAddFollowing = New System.Windows.Forms.Button()
        Me.btnDeleteFollowing = New System.Windows.Forms.Button()
        Me.btnClose = New System.Windows.Forms.Button()
        Me.SuspendLayout()
        '
        'Label1
        '
        Me.Label1.AutoSize = True
        Me.Label1.Font = New System.Drawing.Font("Microsoft Sans Serif", 10.2!, System.Drawing.FontStyle.Bold, System.Drawing.GraphicsUnit.Point, CType(0, Byte))
        Me.Label1.Location = New System.Drawing.Point(30, 22)
        Me.Label1.Name = "Label1"
        Me.Label1.Size = New System.Drawing.Size(109, 20)
        Me.Label1.TabIndex = 0
        Me.Label1.Text = "User Name:"
        '
        'userNameLabel
        '
        Me.userNameLabel.AutoSize = True
        Me.userNameLabel.Font = New System.Drawing.Font("Microsoft Sans Serif", 10.2!, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, CType(0, Byte))
        Me.userNameLabel.Location = New System.Drawing.Point(145, 22)
        Me.userNameLabel.Name = "userNameLabel"
        Me.userNameLabel.Size = New System.Drawing.Size(59, 20)
        Me.userNameLabel.TabIndex = 1
        Me.userNameLabel.Text = "Label2"
        '
        'Label2
        '
        Me.Label2.AutoSize = True
        Me.Label2.Font = New System.Drawing.Font("Microsoft Sans Serif", 10.2!, System.Drawing.FontStyle.Bold, System.Drawing.GraphicsUnit.Point, CType(0, Byte))
        Me.Label2.Location = New System.Drawing.Point(34, 60)
        Me.Label2.Name = "Label2"
        Me.Label2.Size = New System.Drawing.Size(60, 20)
        Me.Label2.TabIndex = 2
        Me.Label2.Text = "UUID:"
        '
        'uuidLabel
        '
        Me.uuidLabel.AutoSize = True
        Me.uuidLabel.Font = New System.Drawing.Font("Microsoft Sans Serif", 10.2!, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, CType(0, Byte))
        Me.uuidLabel.Location = New System.Drawing.Point(149, 60)
        Me.uuidLabel.Name = "uuidLabel"
        Me.uuidLabel.Size = New System.Drawing.Size(59, 20)
        Me.uuidLabel.TabIndex = 3
        Me.uuidLabel.Text = "Label3"
        '
        'unconnectedUsers
        '
        Me.unconnectedUsers.FormattingEnabled = True
        Me.unconnectedUsers.ItemHeight = 16
        Me.unconnectedUsers.Location = New System.Drawing.Point(38, 131)
        Me.unconnectedUsers.Name = "unconnectedUsers"
        Me.unconnectedUsers.Size = New System.Drawing.Size(120, 84)
        Me.unconnectedUsers.TabIndex = 4
        '
        'Label3
        '
        Me.Label3.AutoSize = True
        Me.Label3.Font = New System.Drawing.Font("Microsoft Sans Serif", 10.2!, System.Drawing.FontStyle.Bold, System.Drawing.GraphicsUnit.Point, CType(0, Byte))
        Me.Label3.Location = New System.Drawing.Point(38, 108)
        Me.Label3.Name = "Label3"
        Me.Label3.Size = New System.Drawing.Size(59, 20)
        Me.Label3.TabIndex = 5
        Me.Label3.Text = "Users"
        '
        'followingList
        '
        Me.followingList.FormattingEnabled = True
        Me.followingList.ItemHeight = 16
        Me.followingList.Location = New System.Drawing.Point(341, 131)
        Me.followingList.Name = "followingList"
        Me.followingList.Size = New System.Drawing.Size(120, 84)
        Me.followingList.TabIndex = 6
        '
        'Label4
        '
        Me.Label4.AutoSize = True
        Me.Label4.Font = New System.Drawing.Font("Microsoft Sans Serif", 10.2!, System.Drawing.FontStyle.Bold, System.Drawing.GraphicsUnit.Point, CType(0, Byte))
        Me.Label4.Location = New System.Drawing.Point(337, 108)
        Me.Label4.Name = "Label4"
        Me.Label4.Size = New System.Drawing.Size(88, 20)
        Me.Label4.TabIndex = 7
        Me.Label4.Text = "Following"
        '
        'btnAddFollowing
        '
        Me.btnAddFollowing.Location = New System.Drawing.Point(198, 131)
        Me.btnAddFollowing.Name = "btnAddFollowing"
        Me.btnAddFollowing.Size = New System.Drawing.Size(75, 23)
        Me.btnAddFollowing.TabIndex = 8
        Me.btnAddFollowing.Text = "Add ->"
        Me.btnAddFollowing.UseVisualStyleBackColor = True
        '
        'btnDeleteFollowing
        '
        Me.btnDeleteFollowing.Location = New System.Drawing.Point(198, 177)
        Me.btnDeleteFollowing.Name = "btnDeleteFollowing"
        Me.btnDeleteFollowing.Size = New System.Drawing.Size(75, 23)
        Me.btnDeleteFollowing.TabIndex = 9
        Me.btnDeleteFollowing.Text = "<- Delete"
        Me.btnDeleteFollowing.UseVisualStyleBackColor = True
        '
        'btnClose
        '
        Me.btnClose.Location = New System.Drawing.Point(341, 325)
        Me.btnClose.Name = "btnClose"
        Me.btnClose.Size = New System.Drawing.Size(75, 23)
        Me.btnClose.TabIndex = 10
        Me.btnClose.Text = "Close"
        Me.btnClose.UseVisualStyleBackColor = True
        '
        'UserSettings
        '
        Me.AutoScaleDimensions = New System.Drawing.SizeF(8.0!, 16.0!)
        Me.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font
        Me.ClientSize = New System.Drawing.Size(537, 395)
        Me.Controls.Add(Me.btnClose)
        Me.Controls.Add(Me.btnDeleteFollowing)
        Me.Controls.Add(Me.btnAddFollowing)
        Me.Controls.Add(Me.Label4)
        Me.Controls.Add(Me.followingList)
        Me.Controls.Add(Me.Label3)
        Me.Controls.Add(Me.unconnectedUsers)
        Me.Controls.Add(Me.uuidLabel)
        Me.Controls.Add(Me.Label2)
        Me.Controls.Add(Me.userNameLabel)
        Me.Controls.Add(Me.Label1)
        Me.Name = "UserSettings"
        Me.Text = "UserSettings"
        Me.ResumeLayout(False)
        Me.PerformLayout()

    End Sub
    Friend WithEvents Label1 As System.Windows.Forms.Label
    Friend WithEvents userNameLabel As System.Windows.Forms.Label
    Friend WithEvents Label2 As System.Windows.Forms.Label
    Friend WithEvents uuidLabel As System.Windows.Forms.Label
    Friend WithEvents unconnectedUsers As System.Windows.Forms.ListBox
    Friend WithEvents Label3 As System.Windows.Forms.Label
    Friend WithEvents followingList As System.Windows.Forms.ListBox
    Friend WithEvents Label4 As System.Windows.Forms.Label
    Friend WithEvents btnAddFollowing As System.Windows.Forms.Button
    Friend WithEvents btnDeleteFollowing As System.Windows.Forms.Button
    Friend WithEvents btnClose As System.Windows.Forms.Button
End Class
