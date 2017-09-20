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
package org.sesquipedalian_dev.scala2DGaming.entities

import org.sesquipedalian_dev.scala2DGaming.graphics.{HasSingleWorldSpriteRendering, HasWorldSpriteRendering, WorldSpritesRenderer}
import org.sesquipedalian_dev.scala2DGaming.{HasGameUpdate, TimeOfDay}

class BadGuy(
  var location: Location,
  _direction: Option[Location], // expected to be +/- 1 (or 0) in each axis
  worldSize: Location,
  var health: Int
) extends HasGameUpdate
  with HasSingleWorldSpriteRendering
  with HasMovingToward
{
  val name: String = "BadGuy"
  direction = _direction
  override val textureFile: String = "/textures/badguy.bmp"
  final val speed: Float = 1f / TimeOfDay.SLOW.toFloat // blocks / sec

  override def update(deltaTimeSeconds: Double): Unit = {
    if(health <= 0) {
      // killed!
      HasGameUpdate.unregister(this)
      HasWorldSpriteRendering.unregister(this)

      // any other projectiles that may have been chasing us can go away
      HasGameUpdate.all.foreach({
        case x: Projectile if x.target == this => {
          HasGameUpdate.unregister(x)
          HasWorldSpriteRendering.unregister(x)
        }
        case _ =>
      })
    }

    super.update(deltaTimeSeconds)
  }

  override def render(worldSpritesRenderer: WorldSpritesRenderer): Unit = {
    if(textureIndex.isEmpty) {
      worldSpritesRenderer.textureArray.foreach(ta => {
        val currentLoc = ta.textureFiles.indexOf(textureFile)
        if(currentLoc == -1) {
          textureIndex = ta.addTextureResource(textureFile)
        } else {
          textureIndex = Some(currentLoc)
        }
      })
    }
    textureIndex.foreach(ti => worldSpritesRenderer.drawAGuyWorld(location.x, location.y, ti))
  }
}
