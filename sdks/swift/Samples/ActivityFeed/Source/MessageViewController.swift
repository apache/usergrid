//
//  MessageViewController.swift
//  ActivityFeed
//
//  Created by Robert Walsh on 1/21/16.
//
/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 *
 */

import Foundation
import UsergridSDK
import SlackTextViewController
import WatchConnectivity

class MessageViewController : SLKTextViewController {

    static let MESSAGE_CELL_IDENTIFIER = "MessengerCell"

    private var messageEntities: [ActivityEntity] = []

    init() {
        super.init(tableViewStyle:.Plain)
        commonInit()
    }

    required init(coder decoder: NSCoder) {
        super.init(coder: decoder)
        commonInit()
    }

    override static func tableViewStyleForCoder(decoder: NSCoder) -> UITableViewStyle {
        return .Plain
    }

    override func viewWillAppear(animated: Bool) {
        self.reloadMessages()
        if let username = Usergrid.currentUser?.name {
            self.navigationItem.title = "\(username)'s Feed"
        }
        super.viewWillAppear(animated)
    }

    func commonInit() {
        self.bounces = true
        self.shakeToClearEnabled = true
        self.keyboardPanningEnabled = true
        self.shouldScrollToBottomAfterKeyboardShows = true
        self.inverted = true

        self.registerClassForTextView(MessageTextView)
        self.activateWCSession()
    }

    func reloadMessages() {
        UsergridManager.getFeedMessages { (response) -> Void in
            self.messageEntities = response.entities as? [ActivityEntity] ?? []
            self.tableView!.reloadData()
        }
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        self.rightButton.setTitle("Send", forState: .Normal)

        self.textInputbar.autoHideRightButton = true
        self.textInputbar.maxCharCount = 256
        self.textInputbar.editorTitle.textColor = UIColor.darkGrayColor()

        self.tableView!.separatorStyle = .None
        self.tableView!.registerClass(MessageTableViewCell.self, forCellReuseIdentifier:MessageViewController.MESSAGE_CELL_IDENTIFIER)
    }

    override func didPressRightButton(sender: AnyObject!) {
        self.textView.refreshFirstResponder()

        UsergridManager.postFeedMessage(self.textView.text) { (response) -> Void in
            if let messageEntity = response.entity as? ActivityEntity {
                let indexPath = NSIndexPath(forRow: 0, inSection: 0)
                let rowAnimation: UITableViewRowAnimation = self.inverted ? .Bottom : .Top
                let scrollPosition: UITableViewScrollPosition = self.inverted ? .Bottom : .Top

                self.tableView!.beginUpdates()
                self.messageEntities.insert(messageEntity, atIndex: 0)
                self.tableView!.insertRowsAtIndexPaths([indexPath], withRowAnimation: rowAnimation)
                self.tableView!.endUpdates()

                self.tableView!.scrollToRowAtIndexPath(indexPath, atScrollPosition: scrollPosition, animated: true)
                self.tableView!.reloadRowsAtIndexPaths([indexPath], withRowAnimation: .Automatic)

                self.sendEntitiesToWatch(self.messageEntities)
            }
        }
        super.didPressRightButton(sender)
    }

    override func keyForTextCaching() -> String? {
        return NSBundle.mainBundle().bundleIdentifier
    }

    override func numberOfSectionsInTableView(tableView: UITableView) -> Int {
        return 1
    }

    override func tableView(tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return self.messageEntities.count
    }

    override func tableView(tableView: UITableView, cellForRowAtIndexPath indexPath: NSIndexPath) -> UITableViewCell {
        return self.messageCellForRowAtIndexPath(indexPath)
    }

    @IBAction func unwindToChat(segue: UIStoryboardSegue) {

    }

    func populateCell(cell:MessageTableViewCell,feedEntity:ActivityEntity) {

        cell.titleLabel.text = feedEntity.displayName
        cell.bodyLabel.text = feedEntity.content
        cell.thumbnailView.image = nil

        if let imageURLString = feedEntity.imageURL, imageURL = NSURL(string: imageURLString) {
            NSURLSession.sharedSession().dataTaskWithURL(imageURL) { (data, response, error) in
                if let imageData = data, image = UIImage(data: imageData) {
                    dispatch_async(dispatch_get_main_queue(), { () -> Void in
                        cell.thumbnailView.image = image
                    })
                }
            }.resume()
        }
    }

    func messageCellForRowAtIndexPath(indexPath:NSIndexPath) -> MessageTableViewCell {
        let cell = self.tableView!.dequeueReusableCellWithIdentifier(MessageViewController.MESSAGE_CELL_IDENTIFIER) as! MessageTableViewCell
        self.populateCell(cell, feedEntity: self.messageEntities[indexPath.row])

        cell.indexPath = indexPath
        cell.transform = self.tableView!.transform

        return cell
    }

    override func tableView(tableView: UITableView, heightForRowAtIndexPath indexPath: NSIndexPath) -> CGFloat {

        let feedEntity = messageEntities[indexPath.row]

        guard let messageText = feedEntity.content where !messageText.isEmpty
        else {
                return 0
        }

        let messageUsername : NSString = feedEntity.displayName ?? ""

        let paragraphStyle = NSMutableParagraphStyle()
        paragraphStyle.lineBreakMode = .ByWordWrapping
        paragraphStyle.alignment = .Left

        let pointSize = MessageTableViewCell.defaultFontSize
        let attributes = [NSFontAttributeName:UIFont.boldSystemFontOfSize(pointSize),NSParagraphStyleAttributeName:paragraphStyle]

        let width: CGFloat = CGRectGetWidth(self.tableView!.frame) - MessageTableViewCell.kMessageTableViewCellAvatarHeight - 25

        let titleBounds = messageUsername.boundingRectWithSize(CGSize(width: width, height: CGFloat.max), options: .UsesLineFragmentOrigin, attributes: attributes, context: nil)
        let bodyBounds = messageText.boundingRectWithSize(CGSize(width: width, height: CGFloat.max), options: .UsesLineFragmentOrigin, attributes: attributes, context: nil)

        var height = CGRectGetHeight(titleBounds) + CGRectGetHeight(bodyBounds) + 40
        if height < MessageTableViewCell.kMessageTableViewCellMinimumHeight {
            height = MessageTableViewCell.kMessageTableViewCellMinimumHeight
        }

        return height
    }
}

extension MessageViewController : WCSessionDelegate {

    func activateWCSession() {
        if (WCSession.isSupported()) {
            let session = WCSession.defaultSession()
            session.delegate = self
            session.activateSession()
        }
    }

    func sendEntitiesToWatch(messages:[UsergridEntity]) {
        if WCSession.defaultSession().reachable {
            NSKeyedArchiver.setClassName("ActivityEntity", forClass: ActivityEntity.self)
            let data = NSKeyedArchiver.archivedDataWithRootObject(messages)
            WCSession.defaultSession().sendMessageData(data, replyHandler: nil, errorHandler: { (error) -> Void in
                self.showAlert(title: "WCSession Unreachable.", message: "\(error)")
            })
        }
    }

    func session(session: WCSession, didReceiveMessage message: [String : AnyObject]) {
        if let action = message["action"] as? String where action == "getMessages" {
            UsergridManager.getFeedMessages { (response) -> Void in
                if let entities = response.entities {
                    self.sendEntitiesToWatch(entities)
                }
            }
        }
    }

}

