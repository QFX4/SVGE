package org.nxn.vulkan

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.{VK10, VkBufferImageCopy, VkDescriptorImageInfo, VkExtent3D, VkImageCreateInfo, VkImageFormatProperties, VkImageSubresourceLayers, VkMemoryRequirements, VkOffset3D, VkWriteDescriptorSet}
import org.nxn.utils.Dimension
import org.nxn.utils.Using.*

import java.util.function.Consumer

class Image(device: Device, val size:Dimension, format:Int = VK10.VK_FORMAT_R8G8B8A8_SRGB) extends Memory(device){

  protected def initInage(size:Dimension, format:Int):Long = MemoryStack.stackPush() | { stack =>
    val dev = device.physicalDevice

    val props = VkImageFormatProperties.calloc(stack)
    vkCheck(VK10.vkGetPhysicalDeviceImageFormatProperties(dev.vkPhysicalDevice, format, VK10.VK_IMAGE_TYPE_2D, VK10.VK_IMAGE_TILING_OPTIMAL, VK10.VK_IMAGE_USAGE_SAMPLED_BIT, 0, props))

    val info = VkImageCreateInfo.calloc(stack)
      .sType$Default()
      .imageType(VK10.VK_IMAGE_TYPE_2D)
      .tiling(VK10.VK_IMAGE_TILING_OPTIMAL)
      .initialLayout(VK10.VK_IMAGE_LAYOUT_UNDEFINED)
      .usage(VK10.VK_IMAGE_USAGE_SAMPLED_BIT | VK10.VK_IMAGE_USAGE_TRANSFER_DST_BIT)
      .sharingMode(if(dev.graphicsQueueIndex == dev.presentQueueIndex) VK10.VK_SHARING_MODE_EXCLUSIVE else VK10.VK_SHARING_MODE_CONCURRENT)
      .extent(((e: VkExtent3D) => {
          e.width(size.width).height(size.height).depth(1)
        }):Consumer[VkExtent3D])
      .format(format)
      .arrayLayers(1)
      .mipLevels(1)
      .samples(VK10.VK_SAMPLE_COUNT_1_BIT)

    val lp = stack.callocLong(1)
    vkCheck(VK10.vkCreateImage(device.vkDevice, info, null, lp))
    lp.get(0)
  }

  val vkImage:Long = initInage(size, format)

  override val vkMemory:Long = initMemory(VK10.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT)(new MemoryCallback{
    override def requirements(memReq: VkMemoryRequirements): Unit = {
      VK10.vkGetImageMemoryRequirements(device.vkDevice, vkImage, memReq)
    }
    override def bind(mem: Long): Unit = {
      vkCheck(VK10.vkBindImageMemory(device.vkDevice, vkImage, mem, 0))
    }
  })

  protected def initImageView(format:Int):ImageView = {
    new ImageView(device, vkImage, format)
  }

  val imageView: ImageView = initImageView(format)

  def copyBufferToImage(buffer: Buffer, commandPool: CommandPool, fence: Fence, graphicsQueue:Queue = device.graphicsQueue):Fence = MemoryStack.stackPush() | { stack =>
    new CommandBuffer(commandPool) | { buff =>
      buff.record(stack, true)({ rec =>

        val region = VkBufferImageCopy.calloc(1, stack)
          .bufferOffset(0)
          .bufferRowLength(0)
          .bufferImageHeight(0)
          .imageSubresource(((t: VkImageSubresourceLayers) => {
            t.aspectMask(VK10.VK_IMAGE_ASPECT_COLOR_BIT)
              .mipLevel(0)
              .baseArrayLayer(0)
              .layerCount(1)
          }): Consumer[VkImageSubresourceLayers])
          .imageOffset(((t: VkOffset3D) => {
            t.x(0).y(0).z(0)
          }): Consumer[VkOffset3D])
          .imageExtent(((t: VkExtent3D) => {
            t.width(size.width).height(size.height).depth(1)
          }): Consumer[VkExtent3D])

        VK10.vkCmdCopyBufferToImage(rec, buffer.vkBuffer, vkImage, VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, region)

      })

      graphicsQueue.submit(buff, fence)
    }
    fence
  }

  def updateDescriptorSet(descriptorSet: DescriptorSet, binding:Int, sampler: Sampler):this.type = MemoryStack.stackPush() | { stack =>
    val imgInfo = VkDescriptorImageInfo.calloc(1, stack)
    imgInfo.get(0)
      .imageLayout(VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
      .imageView(imageView.vkImageView)
      .sampler(sampler.vkSampler)

    val write = VkWriteDescriptorSet.calloc(1, stack)
    write.get(0)
      .sType$Default()
      .dstSet(descriptorSet.vkDescriptorSet)
      .dstBinding(binding)
      .descriptorType(VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
      .descriptorCount(1)
      .pImageInfo(imgInfo)

    VK10.vkUpdateDescriptorSets(device.vkDevice, write, null)

    this
  }

  override def close(): Unit = {
    imageView.close()
    VK10.vkDestroyImage(device.vkDevice, vkImage, null)
    super.close()
  }

}
