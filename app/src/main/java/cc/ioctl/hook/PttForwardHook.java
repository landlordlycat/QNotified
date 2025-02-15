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
package cc.ioctl.hook;

import static android.widget.LinearLayout.LayoutParams.MATCH_PARENT;
import static android.widget.LinearLayout.LayoutParams.WRAP_CONTENT;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static nil.nadph.qnotified.ui.ViewBuilder.newLinearLayoutParams;
import static nil.nadph.qnotified.util.Initiator.load;
import static nil.nadph.qnotified.util.ReflexUtil.findField;
import static nil.nadph.qnotified.util.ReflexUtil.getFirstByType;
import static nil.nadph.qnotified.util.ReflexUtil.getFirstNSFFieldByType;
import static nil.nadph.qnotified.util.ReflexUtil.iget_object_or_null;
import static nil.nadph.qnotified.util.ReflexUtil.invoke_virtual;
import static nil.nadph.qnotified.util.Utils.dip2px;
import static nil.nadph.qnotified.util.Utils.dip2sp;
import static nil.nadph.qnotified.util.Utils.getQQAppInterface;
import static nil.nadph.qnotified.util.Utils.log;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;

import org.ferredoxin.ferredoxin_ui.base.UiSwitchPreference;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import me.singleneuron.qn_kernel.annotation.UiItem;
import me.singleneuron.qn_kernel.base.CommonDelayAbleHookBridge;
import nil.nadph.qnotified.SyncUtils;
import nil.nadph.qnotified.base.annotation.FunctionEntry;
import nil.nadph.qnotified.bridge.ChatActivityFacade;
import nil.nadph.qnotified.bridge.SessionInfoImpl;
import nil.nadph.qnotified.config.ConfigManager;
import nil.nadph.qnotified.step.DexDeobfStep;
import nil.nadph.qnotified.ui.CustomDialog;
import nil.nadph.qnotified.ui.ResUtils;
import nil.nadph.qnotified.ui.drawable.HighContrastBorder;
import nil.nadph.qnotified.util.CustomMenu;
import nil.nadph.qnotified.util.DexKit;
import nil.nadph.qnotified.util.FaceImpl;
import nil.nadph.qnotified.util.LicenseStatus;
import nil.nadph.qnotified.util.Toasts;
import nil.nadph.qnotified.util.Utils;

@FunctionEntry
@UiItem
public class PttForwardHook extends CommonDelayAbleHookBridge {

    public static final int R_ID_PTT_FORWARD = 0x30EE77CB;
    public static final int R_ID_PTT_SAVE = 0x30EE77CC;
    public static final String qn_enable_ptt_save = "qn_enable_ptt_save";
    public static final String qn_cache_ptt_save_last_parent_dir = "qn_cache_ptt_save_last_parent_dir";
    public static final PttForwardHook INSTANCE = new PttForwardHook();

    private final UiSwitchPreference mUiSwitchPreference = this.new UiSwitchPreferenceItemFactory("语音转发", "长按语音消息");

    @NonNull
    @Override
    public UiSwitchPreference getPreference() {
        return mUiSwitchPreference;
    }

    @Nullable
    @Override
    public String[] getPreferenceLocate() {
        return new String[]{"增强功能"};
    }

    private PttForwardHook() {
        super(SyncUtils.PROC_MAIN, new DexDeobfStep(DexKit.C_FACADE));
    }

    private static void showSavePttFileDialog(Activity context, final File ptt) {
        CustomDialog dialog = CustomDialog.createFailsafe(context);
        final Context ctx = dialog.getContext();
        final EditText editText = new EditText(ctx);
        TextView tv = new TextView(ctx);
        tv.setText("格式为.slk/.amr 一般无法直接打开slk格式 而且大多数语音均为slk格式(转发语音可以看到格式) 请自行寻找软件进行转码");
        tv.setPadding(20, 10, 20, 10);
        String lastSaveDir = ConfigManager.getCache().getString(qn_cache_ptt_save_last_parent_dir);
        if (TextUtils.isEmpty(lastSaveDir)) {
            File f = ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if (f == null) {
                f = Environment.getExternalStorageDirectory();
            }
            lastSaveDir = f.getPath();
        }
        editText.setText(new File(lastSaveDir, Utils.getPathTail(ptt)).getPath());
        editText.setTextSize(16);
        int _5 = dip2px(ctx, 5);
        editText.setPadding(_5, _5, _5, _5);
        //editText.setBackgroundDrawable(new HighContrastBorder());
        ViewCompat.setBackground(editText, new HighContrastBorder());
        LinearLayout linearLayout = new LinearLayout(ctx);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.addView(tv, MATCH_PARENT, WRAP_CONTENT);
        linearLayout.addView(editText, newLinearLayoutParams(MATCH_PARENT, WRAP_CONTENT, _5 * 2));
        final AlertDialog alertDialog = (AlertDialog) dialog
            .setTitle("输入保存路径(请自行转码)")
            .setView(linearLayout)
            .setPositiveButton("保存", null)
            .setNegativeButton("取消", null)
            .create();
        alertDialog.show();
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            .setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String path = editText.getText().toString();
                    if (path.equals("")) {
                        Toasts.error(ctx, "请输入路径");
                        return;
                    }
                    if (!path.startsWith("/")) {
                        Toasts.error(ctx, "请输入完整路径(以\"/\"开头)");
                        return;
                    }
                    File f = new File(path);
                    File dir = f.getParentFile();
                    if (dir == null || !dir.exists() || !dir.isDirectory()) {
                        Toasts.error(ctx, "文件夹不存在");
                        return;
                    }
                    if (!dir.canWrite()) {
                        Toasts.error(ctx, "文件夹无访问权限");
                        return;
                    }
                    FileOutputStream fout = null;
                    FileInputStream fin = null;
                    try {
                        if (!f.exists()) {
                            f.createNewFile();
                        }
                        fin = new FileInputStream(ptt);
                        fout = new FileOutputStream(f);
                        byte[] buf = new byte[1024];
                        int i;
                        while ((i = fin.read(buf)) > 0) {
                            fout.write(buf, 0, i);
                        }
                        fout.flush();
                        alertDialog.dismiss();
                        ConfigManager cache = ConfigManager.getCache();
                        String pdir = f.getParent();
                        if (pdir != null) {
                            cache.putString(qn_cache_ptt_save_last_parent_dir, pdir);
                            cache.save();
                        }
                    } catch (IOException e) {
                        Toasts.error(ctx, "失败:" + e.toString().replace("java.io.", ""));
                    } finally {
                        if (fin != null) {
                            try {
                                fin.close();
                            } catch (IOException ignored) {
                            }
                        }
                        if (fout != null) {
                            try {
                                fout.close();
                            } catch (IOException ignored) {
                            }
                        }
                    }
                }
            });
    }

    @Override
    public boolean initOnce() {
        try {
            Class clz_ForwardBaseOption = load("com/tencent/mobileqq/forward/ForwardBaseOption");
            if (clz_ForwardBaseOption == null) {
                Class clz_DirectForwardActivity = load(
                    "com/tencent/mobileqq/activity/DirectForwardActivity");
                for (Field f : clz_DirectForwardActivity.getDeclaredFields()) {
                    if (Modifier.isStatic(f.getModifiers())) {
                        continue;
                    }
                    Class clz = f.getType();
                    if (Modifier.isAbstract(clz.getModifiers()) && !clz.getName()
                        .contains("android")) {
                        clz_ForwardBaseOption = clz;
                        break;
                    }
                }
            }
            Method buildConfirmDialog = null;
            for (Method m : clz_ForwardBaseOption.getDeclaredMethods()) {
                if (!m.getReturnType().equals(void.class)) {
                    continue;
                }
                if (!Modifier.isFinal(m.getModifiers())) {
                    continue;
                }
                if (m.getParameterTypes().length != 0) {
                    continue;
                }
                buildConfirmDialog = m;
                break;
            }
            XposedBridge.hookMethod(buildConfirmDialog, new XC_MethodHook(51) {
                @SuppressLint("SetTextI18n")
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Field f = findField(param.thisObject.getClass(), Bundle.class, "a");
                    if (f == null) {
                        f = getFirstNSFFieldByType(param.thisObject.getClass(), Bundle.class);
                    }
                    f.setAccessible(true);
                    Bundle data = (Bundle) f.get(param.thisObject);
                    if (!data.containsKey("ptt_forward_path")) {
                        return;
                    }
                    param.setResult(null);
                    final String path = data.getString("ptt_forward_path");
                    Activity ctx = getFirstByType(param.thisObject,  Activity.class);
                    if (path == null || !new File(path).exists()) {
                        Toasts.error(ctx, "Error: Invalid ptt file!");
                        return;
                    }
                    ResUtils.initTheme(ctx);
                    boolean multi;
                    final ArrayList<Utils.ContactDescriptor> mTargets = new ArrayList<>();
                    boolean unsupport = false;
                    if (data.containsKey("forward_multi_target")) {
                        ArrayList targets = data.getParcelableArrayList("forward_multi_target");
                        if (targets.size() > 1) {
                            multi = true;
                            for (Object rr : targets) {
                                Utils.ContactDescriptor c = Utils.parseResultRec(rr);
                                mTargets.add(c);
                            }
                        } else {
                            multi = false;
                            Utils.ContactDescriptor c = Utils.parseResultRec(targets.get(0));
                            mTargets.add(c);
                        }
                    } else {
                        multi = false;
                        Utils.ContactDescriptor cd = new Utils.ContactDescriptor();
                        cd.uin = data.getString("uin");
                        cd.uinType = data.getInt("uintype", -1);
                        cd.nick = data.getString("uinname");
                        if (cd.nick == null) {
                            cd.nick = data.getString("uin");
                        }
                        mTargets.add(cd);
                    }
                    if (unsupport) {
                        Toasts.info(ctx, "暂不支持我的设备/临时聊天/讨论组");
                    }
                    LinearLayout main = new LinearLayout(ctx);
                    main.setOrientation(LinearLayout.VERTICAL);
                    LinearLayout heads = new LinearLayout(ctx);
                    heads.setGravity(Gravity.CENTER_VERTICAL);
                    heads.setOrientation(LinearLayout.HORIZONTAL);
                    View div = new View(ctx);
                    div.setBackgroundColor(ResUtils.skin_gray3.getDefaultColor());
                    TextView tv = new TextView(ctx);
                    tv.setText("[语音转发]" + path);
                    tv.setTextColor(ResUtils.skin_gray3);
                    tv.setTextSize(dip2sp(ctx, 16));
                    int pd = dip2px(ctx, 8);
                    tv.setPadding(pd, pd, pd, pd);
                    main.addView(heads, MATCH_PARENT, WRAP_CONTENT);
                    main.addView(div, MATCH_PARENT, 1);
                    main.addView(tv, MATCH_PARENT, WRAP_CONTENT);
                    int w = dip2px(ctx, 40);
                    LinearLayout.LayoutParams imglp = new LinearLayout.LayoutParams(w, w);
                    imglp.setMargins(pd, pd, pd, pd);
                    FaceImpl face = FaceImpl.getInstance();
                    if (multi) {
                        if (mTargets != null) {
                            for (Utils.ContactDescriptor cd : mTargets) {
                                ImageView imgview = new ImageView(ctx);
                                Bitmap bm = face.getBitmapFromCache(
                                    cd.uinType == 1 ? FaceImpl.TYPE_TROOP : FaceImpl.TYPE_USER,
                                    cd.uin);
                                if (bm == null) {
                                    imgview.setImageDrawable(
                                        ResUtils.loadDrawableFromAsset("face.png", ctx));
                                    face.registerView(
                                        cd.uinType == 1 ? FaceImpl.TYPE_TROOP : FaceImpl.TYPE_USER,
                                        cd.uin, imgview);
                                } else {
                                    imgview.setImageBitmap(bm);
                                }
                                heads.addView(imgview, imglp);
                            }
                        }
                    } else {
                        Utils.ContactDescriptor cd = mTargets.get(0);
                        ImageView imgview = new ImageView(ctx);
                        Bitmap bm = face.getBitmapFromCache(
                            cd.uinType == 1 ? FaceImpl.TYPE_TROOP : FaceImpl.TYPE_USER, cd.uin);
                        if (bm == null) {
                            imgview
                                .setImageDrawable(ResUtils.loadDrawableFromAsset("face.png", ctx));
                            face.registerView(
                                cd.uinType == 1 ? FaceImpl.TYPE_TROOP : FaceImpl.TYPE_USER, cd.uin,
                                imgview);
                        } else {
                            imgview.setImageBitmap(bm);
                        }
                        heads.setPadding(pd / 2, pd / 2, pd / 2, pd / 2);
                        TextView ni = new TextView(ctx);
                        ni.setText(cd.nick);
                        ni.setTextColor(0xFF000000);
                        ni.setPadding(pd, 0, 0, 0);
                        ni.setTextSize(dip2sp(ctx, 18));
                        heads.addView(imgview, imglp);
                        heads.addView(ni);
                    }
                    CustomDialog dialog = CustomDialog.create(ctx);
                    final Activity finalCtx = ctx;
                    dialog.setPositiveButton("确认", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                for (Utils.ContactDescriptor cd : mTargets) {
                                    Parcelable sesssion = SessionInfoImpl
                                        .createSessionInfo(cd.uin, cd.uinType);
                                    ChatActivityFacade
                                        .sendPttMessage(getQQAppInterface(), sesssion, path);
                                }
                                Toasts.success(finalCtx, "已发送");
                            } catch (Throwable e) {
                                log(e);
                                try {
                                    Toasts.error(finalCtx, "失败: " + e);
                                } catch (Throwable ignored) {
                                    Toast.makeText(finalCtx, "失败: " + e, Toast.LENGTH_SHORT).show();
                                }
                            }
                            finalCtx.finish();
                        }
                    });
                    dialog.setNegativeButton("取消", new Utils.DummyCallback());
                    dialog.setCancelable(true);
                    dialog.setView(main);
                    dialog.setTitle("发送给");
                    dialog.show();
                }
            });
            Class cl_PttItemBuilder = load("com/tencent/mobileqq/activity/aio/item/PttItemBuilder");
            if (cl_PttItemBuilder == null) {
                Class cref = load("com/tencent/mobileqq/activity/aio/item/PttItemBuilder$2");
                try {
                    cl_PttItemBuilder = cref.getDeclaredField("this$0").getType();
                } catch (NoSuchFieldException e) {
                }
            }
            findAndHookMethod(cl_PttItemBuilder, "a", int.class, Context.class,
                load("com/tencent/mobileqq/data/ChatMessage"), new XC_MethodHook(60) {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        int id = (int) param.args[0];
                        Activity context = (Activity) param.args[1];
                        Object chatMessage = param.args[2];
                        if (id == R_ID_PTT_FORWARD) {
                            param.setResult(null);
                            String url = (String) invoke_virtual(chatMessage, "getLocalFilePath");
                            File file = new File(url);
                            if (!file.exists()) {
                                Toasts.error(context, "未找到语音文件");
                                return;
                            }
                            Intent intent = new Intent(context,
                                load("com/tencent/mobileqq/activity/ForwardRecentActivity"));
                            intent.putExtra("selection_mode", 0);
                            intent.putExtra("direct_send_if_dataline_forward", false);
                            intent.putExtra("forward_text", "null");
                            intent.putExtra("ptt_forward_path", file.getPath());
                            intent.putExtra("forward_type", -1);
                            intent.putExtra("caller_name", "ChatActivity");
                            intent.putExtra("k_smartdevice", false);
                            intent.putExtra("k_dataline", false);
                            intent.putExtra("k_forward_title", "语音转发");
                            context.startActivity(intent);
                        } else if (id == R_ID_PTT_SAVE) {
                            param.setResult(null);
                            String url = (String) invoke_virtual(chatMessage, "getLocalFilePath");
                            File file = new File(url);
                            if (!file.exists()) {
                                Toasts.error(context, "未找到语音文件");
                                return;
                            }
                            showSavePttFileDialog(context, file);
                        }
                    }
                });
            for (Method m : cl_PttItemBuilder.getDeclaredMethods()) {
                if (!m.getReturnType().isArray()) {
                    continue;
                }
                Class<?>[] ps = m.getParameterTypes();
                if (ps.length == 1 && ps[0].equals(View.class)) {
                    XposedBridge.hookMethod(m, new XC_MethodHook(60) {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            if (LicenseStatus.sDisableCommonHooks) {
                                return;
                            }
                            if (!isEnabled()) {
                                return;
                            }
                            Object arr = param.getResult();
                            Class<?> clQQCustomMenuItem = arr.getClass().getComponentType();
                            Object ret;
                            if (isSavePttEnabled()) {
                                Object item_forward = CustomMenu
                                    .createItem(clQQCustomMenuItem, R_ID_PTT_FORWARD, "转发");
                                Object item_save = CustomMenu
                                    .createItem(clQQCustomMenuItem, R_ID_PTT_SAVE, "保存");
                                ret = Array
                                    .newInstance(clQQCustomMenuItem, Array.getLength(arr) + 2);
                                Array.set(ret, 0, Array.get(arr, 0));
                                //noinspection SuspiciousSystemArraycopy
                                System.arraycopy(arr, 1, ret, 2, Array.getLength(arr) - 1);
                                Array.set(ret, 1, item_forward);
                                Array.set(ret, Array.getLength(ret) - 1, item_save);
                            } else {
                                Object item_forward = CustomMenu
                                    .createItem(clQQCustomMenuItem, R_ID_PTT_FORWARD, "转发");
                                ret = Array
                                    .newInstance(clQQCustomMenuItem, Array.getLength(arr) + 1);
                                Array.set(ret, 0, Array.get(arr, 0));
                                //noinspection SuspiciousSystemArraycopy
                                System.arraycopy(arr, 1, ret, 2, Array.getLength(arr) - 1);
                                Array.set(ret, 1, item_forward);
                            }
                            param.setResult(ret);
                        }
                    });
                }
            }
            return true;
        } catch (Throwable e) {
            log(e);
            return false;
        }
    }

    public boolean isSavePttEnabled() {
        try {
            return ConfigManager.getDefaultConfig().getBooleanOrFalse(qn_enable_ptt_save);
        } catch (Exception e) {
            log(e);
            return false;
        }
    }
}
