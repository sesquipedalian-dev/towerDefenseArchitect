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

import org.sesquipedalian_dev.scala2DGaming.HasGameUpdate
import org.sesquipedalian_dev.scala2DGaming.graphics.{HasSingleWorldSpriteRendering, HasWorldSpriteRendering, WorldSpritesRenderer}

case class RangeArc(
  minAngle: Float, // min anti-clockwise angle that is in our arc
  maxAngle: Float, // max anti-clockwise angle that is in our arc
  range: Float // distance out the arc goes
)

class GunTurret(
  var location: Location,
  secondsPerShot: Double,
  damagePerShot: Int,
  rangeArc: RangeArc
) extends HasSingleWorldSpriteRendering
  with HasGameUpdate
  with Equipment
{
  override val textureFile: String = "/textures/gun.bmp"

  var shotTimer = secondsPerShot

  override def update(deltaTimeSeconds: Double): Unit = {
    shotTimer -= deltaTimeSeconds
    if(shotTimer < 0) {
      user.foreach(g => { // can only shoot if we have a gunner
        shotTimer = secondsPerShot

        // look for a nearby BadGuy to shoot
        val target = HasWorldSpriteRendering.all.collectFirst({
          case badGuy: BadGuy if badGuyInRange(badGuy) => badGuy
        })

        target.foreach(badGuy => {
          new Projectile(location, badGuy, damagePerShot, speed = 5f)
        })
      })
    }
  }

  def badGuyInRange(badGuy: BadGuy): Boolean = {
    // get their location's angle relative to our location - another way of thinking it is
    // constructing their location relative to our location being the origin (0,0)
    val diffX = badGuy.location.x - location.x
    val diffY = badGuy.location.y - location.y

    // get the angle and convert to the range 0 < ANGLE < 2*PI
    val angle = Math.atan2(diffY, diffX)
    val totalAngle = if(angle < 0) {
      angle + Math.PI * 2
    } else {
      angle
    }

    if(rangeArc.minAngle <= totalAngle && totalAngle <= rangeArc.maxAngle) {
      // so their within our arc - now figure out their distance and make sure it's within range
      Math.pow(diffX, 2) + Math.pow(diffY, 2) <= Math.pow(rangeArc.range, 2)
    } else {
      false
    }
  }
}