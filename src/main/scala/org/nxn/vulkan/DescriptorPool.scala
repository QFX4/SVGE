package org.nxn.vulkan

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.{VK10, VkDescriptorPoolCreateInfo, VkDescriptorPoolSize}
import org.nxn.utils.Using.*

/** poolTypes : Map(VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER -> 1) */
class DescriptorPool(val device: Device, poolSizes:Map[Int,Int], maxSets:Int = 1) extends AutoCloseable{

  protected def initDescriptorPool(sizes:Map[Int,Int], sets:Int):Long = MemoryStack.stackPush() | { stack =>
    val pools = VkDescriptorPoolSize.calloc(sizes.size, stack)
    for(i <- sizes.zipWithIndex) {
      val e = i._1
      pools.get(i._2)
        .`type`(e._1)
        .descriptorCount(e._2)
    }

    val info = VkDescriptorPoolCreateInfo.calloc(stack)
      .sType$Default()
      .pPoolSizes(pools)
      .maxSets(sets)

    val lb = stack.callocLong(1)
    vkCheck(VK10.vkCreateDescriptorPool(device.vkDevice, info, null, lb))
    lb.get(0)
  }

  val vkDescriptorPool:Long = initDescriptorPool(poolSizes, maxSets)

  override def close(): Unit = {
    VK10.vkDestroyDescriptorPool(device.vkDevice, vkDescriptorPool, null)
  }

}
