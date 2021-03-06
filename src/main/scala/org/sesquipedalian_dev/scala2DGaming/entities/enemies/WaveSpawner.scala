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
package org.sesquipedalian_dev.scala2DGaming.entities.enemies

import org.sesquipedalian_dev.scala2DGaming.Main
import org.sesquipedalian_dev.scala2DGaming.entities.Location
import org.sesquipedalian_dev.scala2DGaming.game.{HasGameUpdate, TimeOfDay}
import org.sesquipedalian_dev.scala2DGaming.ui.Dialog

import scala.util.Random


// thing to make bad guys on some timer
class WaveSpawner(
  location: Location,
  initialWait: Float,
  secondsPerSpawn: Float,
  wiggle: Float,
  numUnitsWiggle: Int
) extends HasGameUpdate {
  var spawnTimer = initialWait * TimeOfDay.SLOW
  override def update(deltaTimeSeconds: Double): Unit = {
    spawnTimer -= deltaTimeSeconds.toFloat
    if(spawnTimer <= 0) {
      // wiggle the wave spawn timer up to +/- wiggle
      val thisWiggle = (Main.random.getOrElse(Random).nextFloat() * wiggle * 2) - wiggle
      spawnTimer = (secondsPerSpawn + thisWiggle) * TimeOfDay.SLOW

      // spawn a new wave
      // TODO add variations
      new BadGuySpawner(location, 4f, Main.random.getOrElse(Random).nextInt(numUnitsWiggle) + 10)

      if(!WaveSpawner.seenWaveStartTutorial) {
        WaveSpawner.seenWaveStartTutorial = true
        Dialog.open("Here they come!", "/textures/ui/sarge_dialog.bmp")
      }
      if(WaveSpawner.readyForWaveTwoTutorial && !WaveSpawner.seenWaveTwoTutorial) {
        WaveSpawner.seenWaveTwoTutorial = true
        Dialog.open("Here comes another wave! Get back to the front!", "/textures/ui/sarge_dialog.bmp")
      }
    }
  }
}

object WaveSpawner {
  var seenWaveStartTutorial = false
  var seenWaveOverTutorial = false
  var seenWaveTwoTutorial = false
  var readyForWaveTwoTutorial = false
  def onWaveOver(wave: BadGuySpawner): Unit = {
    if(!seenWaveOverTutorial) {
      seenWaveOverTutorial = true
      readyForWaveTwoTutorial = true
      Dialog.open("It's... it's over. FOR NOW. Maybe let your soldiers have a break.", "/textures/ui/sarge_dialog.bmp")
    }
  }
}