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
import org.lwjgl.opengl.GL20._
import org.lwjgl.opengl.GL30._
import org.lwjgl.system.MemoryUtil
import org.sesquipedalian_dev.util._
import org.sesquipedalian_dev.util.registry.{HasRegistryCollection, HasRegistrySingleton}

class WorldSpritesRenderer(
  textureSize: Int,
  worldWidth: Int,
  worldHeight: Int
) extends Renderer
  with ThrowsExceptionOnGLError
{
  final val VERTICES_PER_THING = 4
  final val MAX_THINGS_PER_DRAW = 1024 / (VERTICES_PER_THING + 1);
  final val ELEMENTS_PER_THING = 6

  def vertexBufferSize: Int = MAX_THINGS_PER_DRAW
  def elementBufferSize: Int = MAX_THINGS_PER_DRAW * ELEMENTS_PER_THING
  def vertexShaderRscName: String = "/shaders/textureArray.vert"
  def fragmentShaderRscName: String = "/shaders/textureArray.frag"

  var textureArray: Option[TextureArray] = None

  var camera: Option[Camera2D] = None

  override def init(): Unit = {
    super.init()

    programHandle.foreach(glUseProgram)

    // tell shader program where to bind the shader attributes to the buffer data we're going to pass in
    val stride = 5 * java.lang.Float.BYTES
    setUpVertexArrayAttrib("position", GL_FLOAT, 2, stride, 0)
    setUpVertexArrayAttrib("texCoord", GL_FLOAT, 2, stride, 2 * java.lang.Float.BYTES)
    setUpVertexArrayAttrib("texIndex", GL_FLOAT, 1, stride, 4 * java.lang.Float.BYTES)

    // set up uniform variables
    setUpShaderUniform("texImage", 0)
    setUpShaderUniform("forceAlpha", 1f)

    drawCalls = Map(
      "" -> DrawCallInfo(
        MemoryUtil.memAllocInt(elementBufferSize),
        MemoryUtil.memAllocFloat(vertexBufferSize),
        0,
        () => {}
      )
    )

    val testTextureNames = List("/textures/world/testTex.bmp", "/textures/world/testTex2.bmp", "/textures/world/terraPortrait.bmp")
    textureArray = Some(new TextureArray(textureSize))
    testTextureNames.foreach(fn => textureArray.foreach(_.addTextureResource(fn)))

    // create our camera
    camera = Some(new Camera2D(worldWidth, worldHeight, textureSize))
    camera.foreach(camera => {
      camera.register()
      programHandle.foreach(camera.init)
    })
    register()
  }

  override def render(): Unit = {
    programHandle.foreach(glUseProgram)
    camera.foreach(_.updateScreenSize(programHandle, "projection"))

    HasWorldSpriteRendering.render(this)

    super.render()
  }

  override def flushVertexData(key: String = ""): Unit = {
    textureArray.flatMap(_.textureHandle).foreach(th => {
      glBindTexture(GL_TEXTURE_2D_ARRAY, th)
      super.flushVertexData(key)
    })
  }

  def drawAGuyWorld(x: Float, y: Float, texIndex: Int): Unit = {
    drawCalls.get("").foreach(drawCall => {
      val vertexBuffer = drawCall.vertexBuffer
      val elBuffer = drawCall.elBuffer
      if(vertexBuffer.remaining < (4 * 5) || elBuffer.remaining < (2 * 3)) {
        flushVertexData()
      }

      vertexBuffer.put(x * textureSize).put(y * textureSize)
        .put(0f).put(1f).put(texIndex.toFloat)
      vertexBuffer.put((x + 1) * textureSize).put(y * textureSize)
        .put(1f).put(1f).put(texIndex.toFloat)
      vertexBuffer.put((x + 1) * textureSize).put((y + 1) * textureSize)
        .put(1f).put(0f).put(texIndex.toFloat)
      vertexBuffer.put(x * textureSize).put((y + 1) * textureSize)
        .put(0f).put(0f).put(texIndex.toFloat)

      val currentVertIndex = drawCall.numObjectsThisDraw / 6 * VERTICES_PER_THING
      elBuffer.put(currentVertIndex).put(currentVertIndex + 1).put(currentVertIndex + 2)
      elBuffer.put(currentVertIndex + 2).put(currentVertIndex + 3).put(currentVertIndex)

      drawCall.numObjectsThisDraw += 6
    })
  }

  def drawAGuy(x: Float, y: Float, texIndex: Int): Unit = {
    drawCalls.get("").foreach(drawCall => {
      val vertexBuffer = drawCall.vertexBuffer
      val elBuffer = drawCall.elBuffer
      if(vertexBuffer.remaining < (4 * 5) || elBuffer.remaining < (2 * 3)) {
        flushVertexData()
      }

      vertexBuffer.put(0f + x).put(0f + y).put(0f).put(1f).put(texIndex.toFloat)
      vertexBuffer.put(textureSize + x).put(0f + y).put(1f).put(1f).put(texIndex.toFloat)
      vertexBuffer.put(textureSize + x).put(textureSize + y).put(1f).put(0f).put(texIndex.toFloat)
      vertexBuffer.put(0f + x).put(textureSize + y).put(0f).put(0f).put(texIndex.toFloat)

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

  WorldSpritesRenderer.register(this)
}

object WorldSpritesRenderer extends HasRegistrySingleton {
  override type ThisType = WorldSpritesRenderer
  def drawAGuyWorld(x: Float, y: Float, texIndex: Int): Unit = singleton.foreach(_.drawAGuyWorld(x, y, texIndex))
  def drawAGuy(x: Float, y: Float, texIndex: Int): Unit = singleton.foreach(_.drawAGuy(x, y, texIndex))
}
