//
//  HybridNitroShaders.swift
//  Pods
//
//  Created by Giulio Amato on 03/07/2026.
//

import Foundation
import UIKit

class HybridNitroShaders : HybridNitroShadersSpec {
  // UIView
  var view: UIView = UIView()

  // Props
  var isRed: Bool = false {
    didSet {
      view.backgroundColor = isRed ? .red : .black
    }
  }
}
