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
Partial Class MessageeMainWindow
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
        Me.UsersLabel = New System.Windows.Forms.Label()
        Me.PostMessage = New System.Windows.Forms.RichTextBox()
        Me.MessageLabel = New System.Windows.Forms.Label()
        Me.MenuStrip1 = New System.Windows.Forms.MenuStrip()
        Me.MenuStrip2 = New System.Windows.Forms.MenuStrip()
        Me.UserDetailsToolStripMenuItem = New System.Windows.Forms.ToolStripMenuItem()
        Me.SettingsToolStripMenuItem = New System.Windows.Forms.ToolStripMenuItem()
        Me.Followers = New System.Windows.Forms.ListBox()
        Me.Label1 = New System.Windows.Forms.Label()
        Me.Users = New System.Windows.Forms.ListBox()
        Me.Label2 = New System.Windows.Forms.Label()
        Me.Following = New System.Windows.Forms.ListBox()
        Me.btnPost = New System.Windows.Forms.Button()
        Me.YourFeed = New System.Windows.Forms.TabPage()
        Me.YourFeedBox = New System.Windows.Forms.RichTextBox()
        Me.RichTextBox1 = New System.Windows.Forms.RichTextBox()
        Me.Feeds = New System.Windows.Forms.TabControl()
        Me.MenuStrip2.SuspendLayout()
        Me.YourFeed.SuspendLayout()
        Me.Feeds.SuspendLayout()
        Me.SuspendLayout()
        '
        'UsersLabel
        '
        Me.UsersLabel.AutoSize = True
        Me.UsersLabel.Font = New System.Drawing.Font("Microsoft Sans Serif", 10.2!, System.Drawing.FontStyle.Bold, System.Drawing.GraphicsUnit.Point, CType(0, Byte))
        Me.UsersLabel.Location = New System.Drawing.Point(12, 397)
        Me.UsersLabel.Name = "UsersLabel"
        Me.UsersLabel.Size = New System.Drawing.Size(118, 20)
        Me.UsersLabel.TabIndex = 2
        Me.UsersLabel.Text = "Choose User"
        '
        'PostMessage
        '
        Me.PostMessage.Location = New System.Drawing.Point(250, 424)
        Me.PostMessage.Name = "PostMessage"
        Me.PostMessage.Size = New System.Drawing.Size(342, 96)
        Me.PostMessage.TabIndex = 3
        Me.PostMessage.Text = ""
        '
        'MessageLabel
        '
        Me.MessageLabel.AutoSize = True
        Me.MessageLabel.Font = New System.Drawing.Font("Microsoft Sans Serif", 10.2!, System.Drawing.FontStyle.Bold, System.Drawing.GraphicsUnit.Point, CType(0, Byte))
        Me.MessageLabel.Location = New System.Drawing.Point(246, 397)
        Me.MessageLabel.Name = "MessageLabel"
        Me.MessageLabel.Size = New System.Drawing.Size(146, 20)
        Me.MessageLabel.TabIndex = 4
        Me.MessageLabel.Text = "Post A Message"
        '
        'MenuStrip1
        '
        Me.MenuStrip1.Location = New System.Drawing.Point(0, 28)
        Me.MenuStrip1.Name = "MenuStrip1"
        Me.MenuStrip1.Size = New System.Drawing.Size(763, 24)
        Me.MenuStrip1.TabIndex = 5
        Me.MenuStrip1.Text = "MenuStrip1"
        '
        'MenuStrip2
        '
        Me.MenuStrip2.Items.AddRange(New System.Windows.Forms.ToolStripItem() {Me.UserDetailsToolStripMenuItem, Me.SettingsToolStripMenuItem})
        Me.MenuStrip2.Location = New System.Drawing.Point(0, 0)
        Me.MenuStrip2.Name = "MenuStrip2"
        Me.MenuStrip2.Size = New System.Drawing.Size(763, 28)
        Me.MenuStrip2.TabIndex = 6
        Me.MenuStrip2.Text = "MenuStrip2"
        '
        'UserDetailsToolStripMenuItem
        '
        Me.UserDetailsToolStripMenuItem.Name = "UserDetailsToolStripMenuItem"
        Me.UserDetailsToolStripMenuItem.Size = New System.Drawing.Size(100, 24)
        Me.UserDetailsToolStripMenuItem.Text = "User Details"
        '
        'SettingsToolStripMenuItem
        '
        Me.SettingsToolStripMenuItem.Name = "SettingsToolStripMenuItem"
        Me.SettingsToolStripMenuItem.Size = New System.Drawing.Size(74, 24)
        Me.SettingsToolStripMenuItem.Text = "Settings"
        '
        'Followers
        '
        Me.Followers.FormattingEnabled = True
        Me.Followers.ItemHeight = 16
        Me.Followers.Location = New System.Drawing.Point(16, 531)
        Me.Followers.Name = "Followers"
        Me.Followers.Size = New System.Drawing.Size(148, 68)
        Me.Followers.TabIndex = 7
        '
        'Label1
        '
        Me.Label1.AutoSize = True
        Me.Label1.Font = New System.Drawing.Font("Microsoft Sans Serif", 10.2!, System.Drawing.FontStyle.Bold, System.Drawing.GraphicsUnit.Point, CType(0, Byte))
        Me.Label1.Location = New System.Drawing.Point(12, 508)
        Me.Label1.Name = "Label1"
        Me.Label1.Size = New System.Drawing.Size(90, 20)
        Me.Label1.TabIndex = 8
        Me.Label1.Text = "Followers"
        '
        'Users
        '
        Me.Users.FormattingEnabled = True
        Me.Users.ItemHeight = 16
        Me.Users.Location = New System.Drawing.Point(16, 421)
        Me.Users.Name = "Users"
        Me.Users.Size = New System.Drawing.Size(148, 84)
        Me.Users.TabIndex = 9
        '
        'Label2
        '
        Me.Label2.AutoSize = True
        Me.Label2.Font = New System.Drawing.Font("Microsoft Sans Serif", 10.2!, System.Drawing.FontStyle.Bold, System.Drawing.GraphicsUnit.Point, CType(0, Byte))
        Me.Label2.Location = New System.Drawing.Point(14, 602)
        Me.Label2.Name = "Label2"
        Me.Label2.Size = New System.Drawing.Size(88, 20)
        Me.Label2.TabIndex = 10
        Me.Label2.Text = "Following"
        '
        'Following
        '
        Me.Following.FormattingEnabled = True
        Me.Following.ItemHeight = 16
        Me.Following.Location = New System.Drawing.Point(16, 624)
        Me.Following.Name = "Following"
        Me.Following.Size = New System.Drawing.Size(148, 68)
        Me.Following.TabIndex = 11
        '
        'btnPost
        '
        Me.btnPost.Font = New System.Drawing.Font("Microsoft Sans Serif", 10.2!, System.Drawing.FontStyle.Bold, System.Drawing.GraphicsUnit.Point, CType(0, Byte))
        Me.btnPost.Location = New System.Drawing.Point(250, 531)
        Me.btnPost.Name = "btnPost"
        Me.btnPost.Size = New System.Drawing.Size(75, 28)
        Me.btnPost.TabIndex = 12
        Me.btnPost.Text = "Post"
        Me.btnPost.UseVisualStyleBackColor = True
        '
        'YourFeed
        '
        Me.YourFeed.Controls.Add(Me.YourFeedBox)
        Me.YourFeed.Controls.Add(Me.RichTextBox1)
        Me.YourFeed.Location = New System.Drawing.Point(4, 25)
        Me.YourFeed.Name = "YourFeed"
        Me.YourFeed.Padding = New System.Windows.Forms.Padding(3)
        Me.YourFeed.Size = New System.Drawing.Size(700, 319)
        Me.YourFeed.TabIndex = 2
        Me.YourFeed.Text = "Your Feed"
        Me.YourFeed.UseVisualStyleBackColor = True
        '
        'YourFeedBox
        '
        Me.YourFeedBox.Location = New System.Drawing.Point(3, 3)
        Me.YourFeedBox.Name = "YourFeedBox"
        Me.YourFeedBox.Size = New System.Drawing.Size(698, 313)
        Me.YourFeedBox.TabIndex = 1
        Me.YourFeedBox.Text = ""
        '
        'RichTextBox1
        '
        Me.RichTextBox1.Location = New System.Drawing.Point(14, 193)
        Me.RichTextBox1.Name = "RichTextBox1"
        Me.RichTextBox1.Size = New System.Drawing.Size(8, 8)
        Me.RichTextBox1.TabIndex = 0
        Me.RichTextBox1.Text = ""
        '
        'Feeds
        '
        Me.Feeds.Controls.Add(Me.YourFeed)
        Me.Feeds.Location = New System.Drawing.Point(12, 31)
        Me.Feeds.Name = "Feeds"
        Me.Feeds.SelectedIndex = 0
        Me.Feeds.Size = New System.Drawing.Size(708, 348)
        Me.Feeds.TabIndex = 0
        '
        'MessageeMainWindow
        '
        Me.AutoScaleDimensions = New System.Drawing.SizeF(8!, 16!)
        Me.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font
        Me.ClientSize = New System.Drawing.Size(763, 704)
        Me.Controls.Add(Me.btnPost)
        Me.Controls.Add(Me.Following)
        Me.Controls.Add(Me.Label2)
        Me.Controls.Add(Me.Users)
        Me.Controls.Add(Me.Label1)
        Me.Controls.Add(Me.Followers)
        Me.Controls.Add(Me.MessageLabel)
        Me.Controls.Add(Me.PostMessage)
        Me.Controls.Add(Me.UsersLabel)
        Me.Controls.Add(Me.Feeds)
        Me.Controls.Add(Me.MenuStrip1)
        Me.Controls.Add(Me.MenuStrip2)
        Me.MainMenuStrip = Me.MenuStrip1
        Me.Name = "MessageeMainWindow"
        Me.Text = "Messagee"
        Me.MenuStrip2.ResumeLayout(false)
        Me.MenuStrip2.PerformLayout
        Me.YourFeed.ResumeLayout(false)
        Me.Feeds.ResumeLayout(false)
        Me.ResumeLayout(false)
        Me.PerformLayout

End Sub
    Friend WithEvents UsersLabel As System.Windows.Forms.Label
    Friend WithEvents PostMessage As System.Windows.Forms.RichTextBox
    Friend WithEvents MessageLabel As System.Windows.Forms.Label
    Friend WithEvents MenuStrip1 As System.Windows.Forms.MenuStrip
    Friend WithEvents MenuStrip2 As System.Windows.Forms.MenuStrip
    Friend WithEvents UserDetailsToolStripMenuItem As System.Windows.Forms.ToolStripMenuItem
    Friend WithEvents SettingsToolStripMenuItem As System.Windows.Forms.ToolStripMenuItem
    Friend WithEvents Followers As System.Windows.Forms.ListBox
    Friend WithEvents Label1 As System.Windows.Forms.Label
    Friend WithEvents Users As System.Windows.Forms.ListBox
    Friend WithEvents Label2 As System.Windows.Forms.Label
    Friend WithEvents Following As System.Windows.Forms.ListBox
    Friend WithEvents btnPost As System.Windows.Forms.Button
    Friend WithEvents YourFeed As System.Windows.Forms.TabPage
    Friend WithEvents YourFeedBox As System.Windows.Forms.RichTextBox
    Friend WithEvents RichTextBox1 As System.Windows.Forms.RichTextBox
    Friend WithEvents Feeds As System.Windows.Forms.TabControl

End Class
