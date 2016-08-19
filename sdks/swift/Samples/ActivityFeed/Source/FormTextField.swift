//
//  FormTextField.swift
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
import UIKit

@IBDesignable class FormTextField: UITextField {

    @IBInspectable var inset: CGFloat = 0
    @IBOutlet weak var nextResponderField: UIResponder?

    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        setUp()
    }

    override init(frame: CGRect) {
        super.init(frame: frame)
        setUp()
    }

    func setUp() {
        addTarget(self, action: #selector(FormTextField.actionKeyboardButtonTapped(_:)), forControlEvents: .EditingDidEndOnExit)
    }

    func actionKeyboardButtonTapped(sender: UITextField) {
        switch nextResponderField {
        case let button as UIButton:
            if button.enabled {
                button.sendActionsForControlEvents(.TouchUpInside)
            } else {
                resignFirstResponder()
            }
        case .Some(let responder):
            responder.becomeFirstResponder()
        default:
            resignFirstResponder()
        }
    }

    override func textRectForBounds(bounds: CGRect) -> CGRect {
        return CGRectInset(bounds, inset, 0)
    }

    override func editingRectForBounds(bounds: CGRect) -> CGRect {
        return textRectForBounds(bounds)
    }
}
