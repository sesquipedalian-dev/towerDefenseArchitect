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
package org.sesquipedalian_dev.util.registry

import javax.imageio.spi.RegisterableService

import org.sesquipedalian_dev.util.{Logging, Registry}

trait HasRegistryTag extends Logging {
  def tag: String = getClass.getSimpleName
  type ThisType <: AnyRef

  def register(instance: ThisType) = {
    trace"registering new instance for this type $tag $instance"
    Registry.register(instance, tag)
    if(logging.isTraceEnabled) {
      val newLst = Registry.objectsNoMF[ThisType](tag)
      trace"post register is it there? $newLst ${newLst.contains(instance)}"
    }
  }

  def unregister(instance: ThisType) = {
    Registry.unregister(instance, tag)
  }
}
