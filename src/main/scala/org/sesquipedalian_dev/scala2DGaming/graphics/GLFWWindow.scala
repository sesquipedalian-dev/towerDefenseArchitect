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
package org.sesquipedalian_dev.scala2DGaming.graphics

import org.lwjgl.glfw.GLFW._
import org.lwjgl.glfw.{GLFWCursorPosCallback, GLFWErrorCallback, GLFWKeyCallback, GLFWMouseButtonCallback}
import org.lwjgl.opengl.GL
import org.lwjgl.system.MemoryUtil
import org.sesquipedalian_dev.scala2DGaming.game.TimeOfDay
import org.sesquipedalian_dev.scala2DGaming.input.{KeyInputHandler, MouseInputHandler}

// wrap GLFW window creation
class GLFWWindow(width: Int, height: Int, name: String) {
  var glfwWindow: Option[Long] = None
  var keyCallback: Option[GLFWKeyCallback] = None
  var mouseCursorCallback: Option[GLFWCursorPosCallback] = None
  var mouseActionCallback: Option[GLFWMouseButtonCallback] = None
  var errorCallback: Option[GLFWErrorCallback] = None

  def init(): Unit = {
    errorCallback = Some(GLFWErrorCallback.createThrow())
    errorCallback.foreach(glfwSetErrorCallback)

    // initi GLFW
    if(!glfwInit()) {
      throw new Exception("Couldn't init glfw!")
    }

    // give GLFW some hints about what kind of window we want
    glfwDefaultWindowHints()
    glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4) // OpenGL version 4.6
    glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 5)
    glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)
    glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE) // deactivate deprecated functionality

    // create GLFW window using width / height / name
    val window = glfwCreateWindow(width, height, name, MemoryUtil.NULL, MemoryUtil.NULL)
    if(window == MemoryUtil.NULL) {
      throw new Exception("Couldn't create glfw window!")
    }

    // Center the window on screen
    val vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
    glfwSetWindowPos(window,
      (vidMode.width() - width) / 2,
      (vidMode.height() - height) / 2
    );

    // create a keyboard input handler
    keyCallback = Some(new GLFWKeyCallback() {
      override def invoke(window: Long, key: Int, scancode: Int, action: Int, mods: Int): Unit = {
        if(action == GLFW_RELEASE) { // on key release, check handlers until we find one that consumes it
          KeyInputHandler.all.foldLeft(false)({
            case (false, handler) => handler.handleInput(window, key)
            case (true, _) => true
          })
        }
      }
    })
    keyCallback.foreach(kc => glfwSetKeyCallback(window, kc))

    mouseCursorCallback = Some(new GLFWCursorPosCallback() {
      override def invoke(window: Long, xpos: Double, ypos: Double) = {
        val lbState = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT)
        val rbState = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_RIGHT)
        MouseInputHandler.all.foldLeft(false)({
          case (false, handler) => handler.handleMove(window, xpos, ypos, lbState, rbState)
          case (true, _) => true
        })
      }
    })
    mouseCursorCallback.foreach(mc => glfwSetCursorPosCallback(window, mc))

    mouseActionCallback = Some(new GLFWMouseButtonCallback {
      override def invoke(window: Long, button: Int, action: Int, mods: Int) = {
        MouseInputHandler.all.foldLeft(false)({
          case (false, handler) => handler.handleAction(window, button, action)
          case (true, _) => true
        })
      }
    })
    mouseActionCallback.foreach(ma => glfwSetMouseButtonCallback(window, ma))

    // bind openGL to the window we made
    glfwMakeContextCurrent(window)

    // Enable VSYNC
    glfwSwapInterval(1)

    glfwWindow = Some(window)

    GL.createCapabilities()
  }

  def mainLoop(update: (Double) => Unit, render: () => Unit): Unit = {
    var lastLoop: Option[Double] = None

    while(glfwWindow.exists(!glfwWindowShouldClose(_))) {
      // get delta time for update
      val loopTime: Double = glfwGetTime()
      val deltaTime = lastLoop.map(ll => loopTime - ll)
      lastLoop = Some(loopTime)

      glfwPollEvents() // get input

      (deltaTime zip TimeOfDay.singleton).foreach(p => {
        val (dt, tod) = p
        // adjust game speed for time-of-day
        update(dt * tod.speed)
      }) // only update if this isn't the first loop

      render()
      glfwWindow.foreach(glfwSwapBuffers)
    }
  }

  def cleanup(): Unit = {
    glfwWindow.foreach(glfwDestroyWindow)
    keyCallback.foreach(_.free())
    mouseCursorCallback.foreach(_.free())
    mouseActionCallback.foreach(_.free())

    glfwTerminate()
    errorCallback.foreach(_.free())
  }
}
