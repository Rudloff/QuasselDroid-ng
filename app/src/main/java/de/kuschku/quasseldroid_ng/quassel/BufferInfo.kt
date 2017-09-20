package de.kuschku.quasseldroid_ng.quassel

import de.kuschku.quasseldroid_ng.protocol.Buffer_Types
import de.kuschku.quasseldroid_ng.util.Flag
import de.kuschku.quasseldroid_ng.util.Flags
import de.kuschku.quasseldroid_ng.util.ShortFlag
import de.kuschku.quasseldroid_ng.util.ShortFlags

data class BufferInfo(var bufferId: Int, var networkId: Int, var type: Buffer_Types,
                      var groupId: Int,
                      var bufferName: String?) {
  enum class Type(override val bit: Short) : ShortFlag<Type> {
    InvalidBuffer(0x00),
    StatusBuffer(0x01),
    ChannelBuffer(0x02),
    QueryBuffer(0x04),
    GroupBuffer(0x08);

    companion object : ShortFlags.Factory<Type> {
      val validValues = values().filter { it.bit != 0.toShort() }.toTypedArray()
      override fun of(bit: Short) = ShortFlags.ofBitMask<Type>(bit)
      override fun of(vararg flags: Type) = ShortFlags.of(*flags)
    }
  }

  enum class Activity(override val bit: Int) : Flag<Activity> {
    NoActivity(0x00),
    OtherActivity(0x01),
    NewMessage(0x02),
    Highlight(0x40);

    companion object : Flags.Factory<Activity> {
      override val NONE = Activity.of()
      override fun of(bit: Int) = Flags.of<Activity>(bit)
      override fun of(vararg flags: Activity) = Flags.of(*flags)
    }
  }
}