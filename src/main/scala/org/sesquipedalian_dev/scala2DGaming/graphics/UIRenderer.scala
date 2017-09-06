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

import java.awt.Color
import java.nio.{FloatBuffer, IntBuffer}

import org.joml.Matrix4f
import org.lwjgl.glfw.GLFW.{glfwGetCurrentContext, glfwGetFramebufferSize}
import org.lwjgl.opengl.GL11.{GL_FLOAT, GL_TEXTURE_2D, glBindTexture, glViewport}
import org.lwjgl.opengl.GL15._
import org.lwjgl.opengl.GL20._
import org.lwjgl.opengl.GL30.{GL_TEXTURE_2D_ARRAY, glBindVertexArray, glGenVertexArrays}
import org.lwjgl.system.MemoryStack
import org.sesquipedalian_dev.scala2DGaming.util.cleanly

class UIRenderer extends Renderer {
  final val UI_WIDTH = 2560
  final val UI_HEIGHT = 1440
//  final val UI_WIDTH = 1600
//  final val UI_HEIGHT = 900
//  final val UI_WIDTH = 1024
//  final val UI_HEIGHT = 576
  final val FLOAT_PER_VERTEX = 7
  final val VERTEX_PER_CHAR = 4
  final val EL_PER_CHAR = 6
  final val TEXT_SIZE = 32
  def MAX_CHARS_PER_DRAW = 1024 / (VERTEX_PER_CHAR * FLOAT_PER_VERTEX )
  def vertexBufferSize: Int = MAX_CHARS_PER_DRAW * (VERTEX_PER_CHAR * FLOAT_PER_VERTEX )
  def elementBufferSize: Int = MAX_CHARS_PER_DRAW * EL_PER_CHAR
  def vertexShaderRscName: String = "/shaders/text.vert"
  def fragmentShaderRscName: String = "/shaders/text.frag"

  var fontTexture: Option[FontTexture] = None
  var camera: Option[Camera2D] = None

  override def init(): Unit = {
    super.init()
    checkError()

    programHandle.foreach(glUseProgram)

    // tell shader program where to bind the shader attributes to the buffer data we're going to pass in
    val stride = 7 * java.lang.Float.BYTES
    val posAttrib = programHandle.map(glGetAttribLocation(_, "position")).filter(_ != -1)
    posAttrib.foreach(pos => {
      glEnableVertexAttribArray(pos)
      glVertexAttribPointer(pos, 2, GL_FLOAT, false, stride, 0)
    })
    checkError()

    val texAttrib = programHandle.map(glGetAttribLocation(_, "textColor")).filter(_ != -1)
    texAttrib.foreach(tex => {
      glEnableVertexAttribArray(tex)
      glVertexAttribPointer(tex, 3, GL_FLOAT, false, stride, 2 * java.lang.Float.BYTES)
    })
    checkError()

    val texIAttrib = programHandle.map(glGetAttribLocation(_, "texCoordinate")).filter(_ != -1)
    texIAttrib.foreach(tex => {
      glEnableVertexAttribArray(tex)
      glVertexAttribPointer(tex, 2, GL_FLOAT, false, stride, 5 * java.lang.Float.BYTES)
    })
    checkError()

    // set the texture image uniform
    val texIUniform = programHandle.map(glGetUniformLocation(_, "texImage")).filter(_ != -1)
    cleanly(MemoryStack.stackPush())(stack => {
      texIUniform.foreach(uniform => {
        glUniform1i(uniform, 0)
      })
    })
    checkError()

    fontTexture = Some(new FontTexture("/fonts/Consolas.ttf", 16))
    fontTexture.foreach(_.init)

    // create our camera
    camera = Some(new UICamera(UI_WIDTH, UI_HEIGHT, 1))
    camera.foreach(camera => {
      camera.register()
      programHandle.foreach(camera.init)
    })
    register()
  }

  override def render(): Unit = {
    programHandle.foreach(glUseProgram)
    camera.foreach(_.updateScreenSize("projection"))

    drawTextOnWorld(0, 0, "TL", Color.RED)
    drawTextOnWorld(UI_WIDTH - (2 * TEXT_SIZE), 0, "TR", Color.PINK)
    drawTextOnWorld(UI_WIDTH - (2 * TEXT_SIZE), UI_HEIGHT - TEXT_SIZE, "BR", Color.CYAN)
    drawTextOnWorld(0, UI_HEIGHT - TEXT_SIZE, "BL", Color.YELLOW)

//    drawTextOnWorld(0, 0, "A", Color.RED)
//    drawTextOnWorld(UI_WIDTH - TEXT_SIZE, 0, "B", Color.PINK)
//    drawTextOnWorld(UI_WIDTH - TEXT_SIZE, UI_HEIGHT - TEXT_SIZE, "C", Color.CYAN)
//    drawTextOnWorld(0, UI_HEIGHT - TEXT_SIZE, "D", Color.YELLOW)

    super.render()
  }

  def drawTextOnWorld(x: Float, y: Float, text: String, color: Color): Unit = {
    for {
      (c, index) <- text.zipWithIndex
      glyph <- fontTexture.flatMap(_.glyphs.get(c))
    } {
      if (vertexBuffer.remaining < (VERTEX_PER_CHAR * FLOAT_PER_VERTEX) || elBuffer.remaining < EL_PER_CHAR) {
        flushVertexData()
      }

      vertexBuffer.put(x + (TEXT_SIZE * index)).put(y)
        .put(color.getRed).put(color.getGreen).put(color.getBlue)
        .put(glyph.x).put(glyph.height)
//        .put(0).put(1)
      vertexBuffer.put(x + (TEXT_SIZE * (index + 1))).put(y)
        .put(color.getRed).put(color.getGreen).put(color.getBlue)
        .put(glyph.width).put(glyph.height)
//          .put(1).put(1)
      vertexBuffer.put(x + (TEXT_SIZE * (index + 1))).put(y + TEXT_SIZE)
        .put(color.getRed).put(color.getGreen).put(color.getBlue)
        .put(glyph.width).put(glyph.y)
//        .put(1).put(0)
      vertexBuffer.put(x + (TEXT_SIZE * (index))).put(y + TEXT_SIZE)
        .put(color.getRed).put(color.getGreen).put(color.getBlue)
        .put(glyph.x).put(glyph.y)
//        .put(0).put(0)


      val currentVertIndex = numObjectsThisDraw / EL_PER_CHAR * VERTEX_PER_CHAR
      elBuffer.put(currentVertIndex).put(currentVertIndex + 1).put(currentVertIndex + 2)
      elBuffer.put(currentVertIndex + 2).put(currentVertIndex + 3).put(currentVertIndex)

      numObjectsThisDraw += EL_PER_CHAR
    }
  }

  override def flushVertexData(): Unit = {
    fontTexture.flatMap(_.textureHandle).foreach(th => {
      glBindTexture(GL_TEXTURE_2D, th)
      super.flushVertexData()
    })
  }

  override def cleanup(): Unit = {
    super.cleanup()
    fontTexture.foreach(_.cleanup)
    camera.foreach(_.cleanup)
  }

  UIRenderer.singleton = Some(this)
}

object UIRenderer {
  var singleton: Option[UIRenderer] = None
}
