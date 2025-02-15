/*
 * QNotified - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2021 dmca@ioctl.cc
 * https://github.com/ferredoxin/QNotified
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as published
 * by ferredoxin.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and eula along with this software.  If not, see
 * <https://www.gnu.org/licenses/>
 * <https://github.com/ferredoxin/QNotified/blob/master/LICENSE.md>.
 */
package me.kyuubiran.hook

import android.view.View
import ltd.nextalone.util.method
import ltd.nextalone.util.replace
import ltd.nextalone.util.tryOrFalse
import nil.nadph.qnotified.base.annotation.FunctionEntry
import nil.nadph.qnotified.hook.CommonDelayableHook
import nil.nadph.qnotified.step.DexDeobfStep
import nil.nadph.qnotified.util.DexKit

//移除傻屌右划群应用
@FunctionEntry
object RemoveGroupApp :
    CommonDelayableHook("kr_remove_group_app", DexDeobfStep(DexKit.C_GroupAppActivity)) {

    override fun initOnce(): Boolean {
        return tryOrFalse {
            DexKit.doFindClass(DexKit.C_GroupAppActivity)?.method {
                it.returnType == View::class.java && it.parameterTypes.isEmpty()
            }?.replace(this, null)
        }
    }
}
