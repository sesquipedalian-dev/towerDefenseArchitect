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

import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL20.{glGetUniformLocation, _}
import org.lwjgl.opengl.GL30._
import org.lwjgl.system.{MemoryStack, MemoryUtil}
import org.sesquipedalian_dev.util._
import org.sesquipedalian_dev.util.registry.HasRegistrySingleton

class UIButtonsRenderer(
  worldWidth: Int,
  worldHeight: Int
) extends Renderer
  with ThrowsExceptionOnGLError
  with Logging
{
  val textureSize: Int = 64
  final val VERTICES_PER_THING = 4
  final val MAX_THINGS_PER_DRAW = 1024 / (VERTICES_PER_THING + 1);
  final val ELEMENTS_PER_THING = 6

  def vertexBufferSize: Int = MAX_THINGS_PER_DRAW
  def elementBufferSize: Int = MAX_THINGS_PER_DRAW * ELEMENTS_PER_THING
  def vertexShaderRscName: String = "/shaders/textureArray.vert"
  def fragmentShaderRscName: String = "/shaders/textureArray.frag"

  var textureArray: Option[TextureArray] = None

  var camera: Option[UICamera] = None

  override def init(): Unit = {
    super.init()

    programHandle.foreach(glUseProgram)

    // tell shader program where to bind the shader attributes to the buffer data we're going to pass in
    val stride = 5 * java.lang.Float.BYTES
    setUpVertexArrayAttrib("position", GL_FLOAT, 2, stride, 0)
    setUpVertexArrayAttrib("texCoord", GL_FLOAT, 2, stride, 2 * java.lang.Float.BYTES)
    setUpVertexArrayAttrib("texIndex", GL_FLOAT, 2, stride, 4 * java.lang.Float.BYTES)
    setUpShaderUniform("texImage", 0)

    def setForceAlpha(targetAlpha: Float): Unit = {
      // set the texture image uniform
      val alphaUniform = programHandle.map(glGetUniformLocation(_, "forceAlpha"))
      cleanly(MemoryStack.stackPush())(stack => {
        alphaUniform.foreach(uniform => {
          glUniform1f(uniform, targetAlpha)
        })
      })
    }

    drawCalls = Map(
      "" -> DrawCallInfo(
        MemoryUtil.memAllocInt(elementBufferSize),
        MemoryUtil.memAllocFloat(vertexBufferSize),
        0,
        () => setForceAlpha(1f)
      ),
      "TextBacking" -> DrawCallInfo(
        MemoryUtil.memAllocInt(elementBufferSize),
        MemoryUtil.memAllocFloat(vertexBufferSize),
        0,
        () => setForceAlpha(.5f)
      )
    )

    val testTextureNames = List()
    textureArray = Some(new TextureArray(textureSize))
    testTextureNames.foreach(fn => textureArray.foreach(_.addTextureResource(fn)))

    // create our camera
    camera = Some(new UICamera(worldWidth, worldHeight, 1))
    camera.foreach(camera => {
      camera.register()
      programHandle.foreach(camera.init)
    })
    register()
  }

  override def render(): Unit = {
    programHandle.foreach(glUseProgram)
    camera.foreach(_.updateScreenSize(programHandle, "projection"))

    HasUiSpriteRendering.render(this)

    //    // top left
    //    drawAGuy(0, 0, 0)
    //    // top right
    //    drawAGuy((worldWidth - 1) * textureSize, 0, 1)
    //    // bottom right
    //    drawAGuy((worldWidth - 1) * textureSize, (worldHeight - 1) * textureSize, 0)
    //    // bottom left
    //    drawAGuy(0, (worldHeight - 1) * textureSize, 1)
    //
    //    // do all the terra
    //    for {
    //      _x <- 0 until worldWidth
    //      _y <- 0 until worldHeight
    //      (x, y) <- Some(_x, _y) if !List(
    //        (0, 0),
    //        (0, worldHeight - 1),
    //        (worldWidth - 1, 0),
    //        (worldWidth - 1, worldHeight - 1)
    //      ).contains((x, y))
    //    } {
    //      drawAGuyWorld(x, y, 2)
    //    }

    super.render()
  }

  override def flushVertexData(key: String): Unit = {
    textureArray.flatMap(_.textureHandle).foreach(th => {
      glBindTexture(GL_TEXTURE_2D_ARRAY, th)
      super.flushVertexData(key)
    })
  }

  def drawAButton(x: Float, y: Float, texIndex: Int, width: Int = textureSize, height: Int = textureSize): Unit = {
    drawCalls.get("").foreach(drawCall => {
      val vertexBuffer = drawCall.vertexBuffer
      val elBuffer = drawCall.elBuffer

      if (vertexBuffer.remaining < (4 * 5) || elBuffer.remaining < (2 * 3)) {
        flushVertexData("")
      }

      vertexBuffer.put(x).put(y)
        .put(0f).put(1f).put(texIndex.toFloat)
      vertexBuffer.put(x + width).put(y)
        .put(1f).put(1f).put(texIndex.toFloat)
      vertexBuffer.put(x + width).put(y + (height / camera.flatMap(_.aspectRatio).getOrElse(1f)))
        .put(1f).put(0f).put(texIndex.toFloat)
      vertexBuffer.put(x).put(y + (height / camera.flatMap(_.aspectRatio).getOrElse(1f)))
        .put(0f).put(0f).put(texIndex.toFloat)

      trace"""putting vertices:
             $x, $y, 0, 1
             ${x + width}, $y, 1, 1
             ${x + width}, ${y + (height / camera.flatMap(_.aspectRatio).getOrElse(1f))}, 1, 0
             $x, ${y + (height / camera.flatMap(_.aspectRatio).getOrElse(1f))}, 0, 0
           """

      val currentVertIndex = drawCall.numObjectsThisDraw / 6 * VERTICES_PER_THING
      elBuffer.put(currentVertIndex).put(currentVertIndex + 1).put(currentVertIndex + 2)
      elBuffer.put(currentVertIndex + 2).put(currentVertIndex + 3).put(currentVertIndex)

      drawCall.numObjectsThisDraw += 6
    })
  }

  override def cleanup(): Unit = {
    // free all the memory we used
    super.cleanup()
    textureArray.foreach(_.cleanup)
    camera.foreach(_.cleanup)
  }

  val textBackingTexture = "/textures/ui/black6464.bmp"
  var textureIndex: Option[Int] = None
  def drawTextBacking(x: Float, y: Float, numChars: Int, textSize: String): Unit = {
    if(textureIndex.isEmpty) {
      textureArray.foreach(ta => {
        val currentLoc = ta.textureFiles.indexOf(textBackingTexture)
        if(currentLoc == -1) {
          textureIndex = ta.addTextureResource(textBackingTexture)
        } else {
          textureIndex = Some(currentLoc)
        }
      })
    }
    textureIndex.foreach(texIndex => UIButtonsRenderer.draw("TextBacking", (drawCall) => {
      val vertexBuffer = drawCall.vertexBuffer
      val elBuffer = drawCall.elBuffer

      if (vertexBuffer.remaining < (4 * 5) || elBuffer.remaining < (2 * 3)) {
        true
      } else {

        val xTextSize = UITextRenderer.singleton.flatMap(_.textSizes.find(_.name == textSize).map(_.size)).getOrElse(0).toFloat

        trace"drawing text backing $xTextSize $x/${x + (numChars * xTextSize)} $y/${y + xTextSize}"

        vertexBuffer.put(x).put(y)
          .put(0f).put(1f).put(texIndex.toFloat)
        vertexBuffer.put(x + (numChars * xTextSize)).put(y)
          .put(1f).put(1f).put(texIndex.toFloat)
        vertexBuffer.put(x + (numChars * xTextSize)).put(y + xTextSize)
          .put(1f).put(0f).put(texIndex.toFloat)
        vertexBuffer.put(x).put(y + xTextSize)
          .put(0f).put(0f).put(texIndex.toFloat)

        val currentVertIndex = drawCall.numObjectsThisDraw / 6 * 4
        elBuffer.put(currentVertIndex).put(currentVertIndex + 1).put(currentVertIndex + 2)
        elBuffer.put(currentVertIndex + 2).put(currentVertIndex + 3).put(currentVertIndex)

        drawCall.numObjectsThisDraw += 6

        false
      }
    }))
  }

  UIButtonsRenderer.register(this)
}

object UIButtonsRenderer extends HasRegistrySingleton {
  override type ThisType = UIButtonsRenderer

  // func returns true if it should be retried after flushing the buffer
  def draw(drawCallKey: String, drawFunc: (DrawCallInfo) => Boolean): Unit = {
    singleton.foreach(instance => {
      instance.drawCalls.get(drawCallKey).foreach(drawCall => {
        val retry = drawFunc(drawCall)
        if (retry) {
          instance.flushVertexData(drawCallKey)
        }
      })
    })
  }
}
