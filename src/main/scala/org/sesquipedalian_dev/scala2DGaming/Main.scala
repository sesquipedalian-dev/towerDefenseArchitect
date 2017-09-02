/**
  * Copyright 2017 sesquipedalian.dev@gmail.com
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
package org.sesquipedalian_dev.scala2DGaming

import org.sesquipedalian_dev.scala2DGaming.graphics.{Camera2D, GLFWWindow, Renderable}
import org.sesquipedalian_dev.scala2DGaming.input.CloseHandler

object Main {
//  final val SCREEN_WIDTH: Int = 640
//  final val SCREEN_HEIGHT: Int = 480
//  final val SCREEN_WIDTH: Int = 1920
//  final val SCREEN_HEIGHT: Int = 1080
  final val SCREEN_WIDTH: Int = 600
  final val SCREEN_HEIGHT: Int = 600
  final val TEXTURE_SIZE: Int = 512

  final val WORLD_WIDTH: Int = 50
  final val WORLD_HEIGHT: Int = 50

  var window: GLFWWindow = null

  def main(args: Array[String]): Unit = {
    println("Hello World")

    window = new GLFWWindow(SCREEN_WIDTH, SCREEN_HEIGHT, "tut")
    window.init()

    // make input handlers
    window.glfwWindow.foreach(w => new CloseHandler(w))

    // make renderables - order matters for initialization
    val renderer = new TestMesh(TEXTURE_SIZE, WORLD_WIDTH, WORLD_HEIGHT)
    val camera = new Camera2D(SCREEN_WIDTH, SCREEN_HEIGHT, WORLD_WIDTH, WORLD_HEIGHT, TEXTURE_SIZE)
    Renderable.init()

    window.mainLoop(update, Renderable.render)

    window.cleanup()
    Renderable.cleanup()
  }

  def update(deltaTimeSeconds: Double): Unit = {

  }
}