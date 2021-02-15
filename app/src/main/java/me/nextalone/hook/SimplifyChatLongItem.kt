/* QNotified - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2021 xenonhydride@gmail.com
 * https://github.com/ferredoxin/QNotified
 *
 * This software is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software.  If not, see
 * <https://www.gnu.org/licenses/>.
 */
package me.nextalone.hook

import de.robv.android.xposed.XC_MethodHook
import me.nextalone.hook.base.MultiItemDelayableHook
import me.nextalone.util.clazz
import me.nextalone.util.hookBefore
import me.nextalone.util.method
import me.singleneuron.qn_kernel.data.requireMinQQVersion
import me.singleneuron.util.QQVersion
import nil.nadph.qnotified.util.Utils

object SimplifyChatLongItem : MultiItemDelayableHook("na_simplify_chat_long_item", "屏蔽") {
    override val allItems = "复制|转发|收藏|回复|多选|撤回|删除|一起写|设为精华|待办|私聊|截图|存表情|相关表情|复制链接|存微云|发给电脑".split("|").toMutableList()
    override val defaultItems = "一起写|私聊|相关表情|待办"

    override fun initOnce() = try {
        val callback: (XC_MethodHook.MethodHookParam) -> Unit = callback@{
            if (!isEnabled) return@callback
            val str = it.args[1] as String
            if (activeItems.contains(str))
                it.result = null
        }
        "com.tencent.mobileqq.utils.dialogutils.QQCustomMenuImageLayout".clazz.declaredMethods.run {
            this.forEach { method ->
                if (method.name == "setMenu") {
                    val customMenu = method.parameterTypes[0].name.replace(".", "/")
                    "L$customMenu;->a(ILjava/lang/String;II)V"
                        .method
                        .hookBefore(this@SimplifyChatLongItem, callback)
                    "L$customMenu;->a(ILjava/lang/String;I)V"
                        .method
                        .hookBefore(this@SimplifyChatLongItem, callback)
                }
            }
        }
        true
    } catch (t: Throwable) {
        Utils.log(t)
        false
    }

    override fun isValid(): Boolean = requireMinQQVersion(QQVersion.QQ_8_0_0)
}
