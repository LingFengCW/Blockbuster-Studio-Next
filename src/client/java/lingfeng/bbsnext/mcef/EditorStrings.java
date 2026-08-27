package lingfeng.bbsnext.mcef;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.HashMap;
import java.util.Map;

/**
 * Localized string dictionary for the HTML editor chrome.
 *
 * <p>The editor page reads its visible labels from {@code window.bbsState.strings}
 * (injected by {@link EditorBridge#getStateJson(java.lang.String)} via
 * {@link #stringsFor(String)}), so the whole UI can switch language at runtime
 * without reloading the page. Each key maps to a per-language value; {@link #stringsFor}
 * falls back to {@code zh_cn} and finally to the key itself if a language is
 * missing an entry.</p>
 */
public final class EditorStrings
{
    public static final Map<String, Map<String, String>> TABLE = new HashMap<>();

    static
    {
        /* ----- top bar ----- */
        put("app.title", "BBS 编辑器", "BBS Editor");
        put("menu.save", "保存", "Save");
        put("preview.enter", "进入预览世界", "Enter Preview World");
        put("preview.exit", "退出预览世界", "Exit Preview World");
        put("menu.close", "关闭", "Close");

        /* ----- top menu ----- */
        put("menu.file", "文件", "File");
        put("menu.edit", "编辑", "Edit");
        put("menu.view", "视图", "View");
        put("menu.track", "轨道", "Track");
        put("menu.record", "录制", "Record");
        put("menu.window", "窗口", "Window");
        put("menu.newScene", "新建场景", "New Scene");
        put("menu.newCharacter", "新建角色", "New Character");
        put("menu.newEntity", "新建实体", "New Entity");
        put("menu.exitEditor", "退出编辑器", "Exit Editor");
        put("menu.undo", "撤销", "Undo");
        put("menu.redo", "重做", "Redo");
        put("menu.camFollow", "跟随相机", "Follow Camera");
        put("menu.camFree", "自由相机", "Free Camera");
        put("menu.camOrbit", "轨道相机", "Orbit Camera");
        put("menu.camThird", "第三人称", "Third Person");
        put("menu.actorControl", "演员控制", "Actor Control");
        put("menu.delSelectedClip", "删除选中剪辑", "Delete Selected Clip");
        put("menu.toggleRecord", "开始/停止录制", "Start/Stop Recording");
        put("menu.previewPlay", "预览播放", "Preview Playback");

        /* ----- slim toolbar ----- */
        put("tb.play", "播放", "Play");
        put("tb.playTitle", "播放/暂停 (空格)", "Play/Pause (Space)");
        put("tb.actionEditor", "动作编辑器", "Action Editor");
        put("tb.hint", "新建功能在「右键素材箱空白处 / 角色右键」里", "Use right-click on empty asset-box space or on a character to create new items");
        put("tb.screenshot", "截图", "Screenshot");

        /* ----- preview toolbar ----- */
        put("tl.toStart", "回到开头", "Back to Start");
        put("tl.toEnd", "跳到结尾", "Jump to End");
        put("vp.play", "播放", "Play");
        put("vp.playTitle", "播放/暂停", "Play/Pause");
        put("clip.none", "无剪切", "No Clip");
        put("clip.noneTitle", "剪切蒙版：无", "Clip mask: None");
        put("clip.rect", "矩形", "Rectangle");
        put("clip.rectTitle", "剪切蒙版：矩形", "Clip mask: Rectangle");
        put("clip.circle", "圆形", "Circle");
        put("clip.circleTitle", "剪切蒙版：圆形", "Clip mask: Circle");

        /* ----- loading / empty ----- */
        put("preview.loading", "正在加载预览世界…", "Loading preview world…");
        put("preview.empty", "实时预览：进入或加载一个世界后，这里会显示 Minecraft 的 3D 画面。", "Live preview: after entering or loading a world, Minecraft's 3D view will appear here.");

        /* ----- timeline toolbar ----- */
        put("tl.newSeq", "＋序列", "+ Sequence");
        put("tl.newSeqTitle", "新建序列", "New Sequence");
        put("tl.rename", "重命名", "Rename");
        put("tl.renameTitle", "重命名当前序列", "Rename Current Sequence");
        put("tl.delete", "删除", "Delete");
        put("tl.deleteTitle", "删除当前序列", "Delete Current Sequence");
        put("tl.zoomIn", "放大", "Zoom In");
        put("tl.zoomInTitle", "放大时间轴", "Zoom In Timeline");
        put("tl.zoomOut", "缩小", "Zoom Out");
        put("tl.zoomOutTitle", "缩小时间轴", "Zoom Out Timeline");
        put("tl.snap", "吸附", "Snap");
        put("tl.snapTitle", "吸附开关", "Snap Toggle");
        put("tl.allKeyframes", "整体关键帧", "All Keyframes");
        put("tl.shiftLeft", "所有关键帧左移 1 帧", "Shift all keyframes 1 frame left");
        put("tl.shiftRight", "所有关键帧右移 1 帧", "Shift all keyframes 1 frame right");
        put("tl.offsetFrames", "偏移帧数", "Offset Frames");
        put("tl.offset", "偏移", "Offset");
        put("tl.offsetTitle", "按输入帧数偏移所有关键帧", "Offset all keyframes by entered frames");
        put("tl.tabMain", "主序列", "Main Sequence");
        put("tl.tabNew", "+ 序列", "+ Sequence");

        /* ----- update toast ----- */
        put("up.title", "新版本可用", "Update Available");
        put("up.found", "发现新版本 ", "New version found: ");
        put("up.staged", "（已下载，待安装）", "(downloaded, ready to install)");
        put("up.download", "（点击下载并安装）", "(click to download & install)");
        put("up.btnStaged", "安装更新", "Install Update");
        put("up.btnDownload", "下载并更新", "Download & Update");
        put("up.ignore", "忽略", "Dismiss");

        /* ----- asset-bin context menu (new item) ----- */
        put("ctx.newItem", "新建项", "New Item");
        put("ctx.scene", "场景", "Scene");
        put("ctx.character", "角色", "Character");
        put("ctx.entity", "实体", "Entity");
        put("ctx.particle", "粒子", "Particle");
        put("ctx.item", "物品", "Item");
        put("ctx.camera", "相机", "Camera");
        put("ctx.saveWork", "保存作品", "Save Project");
        put("ctx.closeEditor", "关闭编辑器", "Close Editor");
        put("ctx.splitAtPlayhead", "在播放头拆分", "Split at Playhead");
        put("ctx.deleteClip", "删除剪辑块", "Delete Clip Block");
        put("ctx.actorControl", "演员控制", "Actor Control");
        put("ctx.charType", "角色类型", "Character Type");
        put("ctx.keyframeChar", "关键帧角色", "Keyframe Character");
        put("ctx.actionChar", "动作角色", "Action Character");
        put("ctx.playbackChar", "回放角色", "Playback Character");
        put("ctx.moveMode", "移动方式", "Movement");
        put("ctx.keyframePath", "关键帧路径（手动调）", "Keyframe Path (manual)");
        put("ctx.recordPath", "录制路径（世界内，写回角色）", "Record Path (in-world, write back to character)");
        put("ctx.action", "动作", "Action");
        put("ctx.openActionEditor", "打开动作编辑器", "Open Action Editor");
        put("ctx.newAction", "新建动作（MC 预设）", "New Action (MC preset)");
        put("ctx.recordToAction", "用录制创建动作", "Create Action from Recording");
        put("ctx.equip", "装配装备", "Equip Gear");
        put("ctx.toBackpack", "放入背包", "Add to Backpack");
        put("ctx.delete", "删除", "Delete");
        put("ctx.setKeyframeChar", "设为关键帧角色", "Set as Keyframe Character");
        put("ctx.setActionChar", "设为动作角色", "Set as Action Character");
        put("ctx.selectOnTrack", "在轨道上选中", "Select on Track");
        put("ctx.renameCamera", "重命名相机", "Rename Camera");
        put("ctx.recordCoord", "录制坐标（进世界走）", "Record Coordinates (enter world)");
        put("ctx.exportCamera", "导出为文件（.cambbs）", "Export to File (.cambbs)");
        put("ctx.toBackpackCross", "放入背包（跨作品）", "Add to Backpack (cross-project)");
        put("ctx.deleteCamera", "删除相机", "Delete Camera");
        put("ctx.renameCameraGroup", "重命名相机组", "Rename Camera Group");
        put("ctx.exportCameraGroup", "导出为文件（.camgrp）", "Export to File (.camgrp)");
        put("ctx.deleteCameraGroup", "删除相机组", "Delete Camera Group");
        put("ctx.packCameraGroup", "打包成相机组", "Pack into Camera Group");
        put("ctx.warnNotCamera", "所选含非相机，无法建相机组", "Selection contains non-camera, cannot create group");
        put("ctx.removeTrack", "移除轨道（保留角色）", "Remove Track (keep character)");
        put("ctx.deleteReplay", "删除角色", "Delete Character");
        put("ctx.importBackpack", "导入到本作品", "Import to Project");
        put("ctx.deleteBackpackItem", "从背包删除", "Delete from Backpack");
        put("ctx.openScene", "打开场景", "Open Scene");
        put("ctx.enterSequence", "进入序列", "Enter Sequence");

        /* ----- dialog texts ----- */
        put("dlg.new", "新建", "New");
        put("dlg.cancel", "取消", "Cancel");
        put("dlg.ok", "确定", "OK");
        put("dlg.close", "关闭", "Close");
        put("dlg.rename", "重命名", "Rename");
        put("dlg.confirm", "确认", "Confirm");
        put("dlg.renameSeq", "重命名序列", "Rename Sequence");
        put("dlg.renameCameraGroup", "重命名相机组", "Rename Camera Group");
        put("dlg.renameCamera", "重命名相机", "Rename Camera");
        put("dlg.deleteChar", "删除角色", "Delete Character");
        put("dlg.deleteCharMsg1", "确定删除角色「", "Delete character '");
        put("dlg.deleteCharMsg2", "」？该操作可通过 Ctrl+Z 撤销。", "'? This can be undone with Ctrl+Z.");
        put("dlg.unsaved", "未保存的更改", "Unsaved Changes");
        put("dlg.unsavedMsg", "当前作品有未保存的更改，确定退出编辑器？", "The current project has unsaved changes. Exit the editor?");

        /* ----- action editor chrome ----- */
        put("ae.title", "动作编辑器", "Action Editor");
        put("ae.compose", "组合动作组", "Compose Action Group");
        put("ae.group", "▾ 动作组", "▾ Action Groups");
        put("ae.timelineTitle", "时间轴（动作轨道 + 材质轨道：拖动块改起始帧，拖材质块右端 ▮ 改时长）", "Timeline (action track + material track: drag blocks to change start frame, drag the right edge ▮ to change duration)");
        put("ae.newActionBtn", "+ 新建动作", "+ New Action");
        put("ae.modePath", "路径", "Path");
        put("ae.laneAction", "动作", "Action");
        put("ae.laneMaterial", "材质", "Material");
        put("ae.pathChannels", "行走路径关键帧通道（x / y / z / yaw）", "Walk-path keyframe channels (x / y / z / yaw)");
        put("ae.actionList", "动作列表（拖到下方时间轴设置起始帧）", "Action list (drag to the timeline below to set the start frame)");
        put("ae.bodyChannels", "关键帧身体通道（选中后编辑关键帧）", "Keyframe body channels (select to edit keyframes)");
        put("ae.rightHintAction", "从左侧选择一个动作进行编辑，或者从下方「材质」轨道点选一个材质片段进行编辑。", "Select an action on the left to edit, or pick a material clip on the 'Material' track below.");
        put("ae.rightHintKeyframe", "从左侧选择一个身体通道，添加 / 删除关键帧；或点选下方「材质」轨道的片段。", "Select a body channel on the left to add / remove keyframes, or pick a clip on the 'Material' track below.");
        put("ae.composeConfirm1", "将把当前时间轴上的 <b>", "Will combine <b>");
        put("ae.composeConfirm2", "</b> 个动作组合成一个可复用动作组：<br>", "</b> actions on the current timeline into a reusable action group:<br>");
        put("ae.composeConfirm3", "<br><br>确定组合？", "<br><br>Combine?");
        put("ae.noclientBadge", "⚠ 需进入预览世界才播放", "⚠ Plays only in preview world");

        /* ----- action editor form fields ----- */
        put("ae.f.name", "名称", "Name");
        put("ae.f.duration", "时长 (duration)", "Duration");
        put("ae.f.frequency", "每组帧数 (frequency)", "Frames per group (frequency)");
        put("ae.f.tick", "起始帧 (tick)", "Start frame (tick)");
        put("ae.f.layer", "图层 (layer)", "Layer");
        put("ae.f.enabled", "启用", "Enabled");
        put("ae.f.blendIn", "淡入 (blendIn)", "Fade in (blendIn)");
        put("ae.f.blendOut", "淡出 (blendOut)", "Fade out (blendOut)");
        put("ae.f.mode", "移动模式 (mode)", "Move mode");
        put("ae.f.step", "步长 (每步方块)", "Step (blocks per step)");
        put("ae.f.deleteAction", "删除此动作", "Delete this action");
        put("ae.f.type", "类型", "Type");
        put("ae.f.deleteMaterial", "删除此材质", "Delete this material");
        put("ae.loc.walk", "走路", "Walk");
        put("ae.loc.run", "奔跑", "Run");
        put("ae.loc.idle", "空闲", "Idle");
        put("ae.scriptHint", "脚本在播放时运行，通过 bbs 接口操控这个角色。", "The script runs during playback and controls this character via the bbs API.");
        put("ae.scriptSrc", "脚本源码", "Script Source");
        put("ae.saveScript", "保存脚本", "Save Script");
        put("ae.exportJson", "导出为文件 (.json)", "Export to File (.json)");
        put("ae.paramsHint", "自定义参数（数字，可被外部工具读取 / 导出分享）", "Custom parameters (numbers, readable / shareable by external tools)");
        put("ae.addNumber", "+ 添加数字", "+ Add Number");
        put("ae.mat.model", "模型 / Morph", "Model / Morph");
        put("ae.mat.skin", "皮肤 / 贴图", "Skin / Texture");
        put("ae.mat.equip", "装备 / 物品", "Equip / Item");
        put("ae.mat.itemPh", "minecraft:diamond_sword（留空=清除）", "minecraft:diamond_sword (leave blank to clear)");
        put("ae.mat.itemId", "物品 ID (item)", "Item ID (item)");
        put("ae.mat.slot", "装备槽 (slot)", "Equip slot (slot)");
        put("ae.mat.modelTarget", "模型目标 (target)", "Model target (target)");
        put("ae.mat.skinTarget", "贴图目标 (target)", "Texture target (target)");
        put("ae.mat.targetPh", "模型/Morph id，支持 MODEL:/PARTICLE:/group: 前缀", "Model/Morph id, supports MODEL:/PARTICLE:/group: prefixes");
        put("ae.channelHead", "关键帧通道：", "Keyframe channel: ");
        put("ae.frames", "帧", "frames");
        put("ae.addKeyframe", "◆ 添加关键帧", "◆ Add Keyframe");
        put("ae.punchKeyframe", "◆ 播放头打帧", "◆ Keyframe at Playhead");
        put("ae.punchKeyframeTitle", "在当前播放位置打一个关键帧", "Add a keyframe at the current playhead");
        put("ae.frame", "帧", "Frame");
        put("ae.value", "值", "Value");
        put("ae.noKeyframes", "暂无关键帧", "No keyframes");
        put("ae.preset.walk", "走路", "Walk");
        put("ae.preset.run", "奔跑", "Run");
        put("ae.preset.idle", "空闲", "Idle");
        put("ae.preset.attack", "攻击", "Attack");
        put("ae.preset.swipe", "挥击", "Swipe");
        put("ae.preset.break_block", "破坏方块", "Break Block");
        put("ae.preset.place_block", "放置方块", "Place Block");
        put("ae.preset.fishing", "钓鱼", "Fishing");
        put("ae.preset.chat", "聊天", "Chat");
        put("ae.preset.command", "命令", "Command");
        put("ae.preset.drop_item", "丢物品", "Drop Item");
        put("ae.preset.script", "脚本动作", "Script Action");
        put("ae.blendMode", "图层混合模式 (Blend Mode)", "Layer Blend Mode");
        put("ae.opacity", "不透明度", "Opacity");
        put("ae.matteSource", "蒙版源轨道 (Track Matte)", "Matte Source Track");
        put("ae.matteMode", "蒙版模式 (Matte Mode)", "Matte Mode");
        put("ae.none", "(无)", "(none)");
        put("ae.editBig", "在大窗中编辑（上方面板）", "Edit in large window (panel above)");
        put("ae.closeDetail", "关闭详情", "Close Details");
        put("ae.mask", "蒙版", "Mask");
        put("ae.firstPerson", "第一人称", "First Person");
        put("ae.shadow", "阴影", "Shadow");
        put("ae.dockTitle", "动作编辑器（大窗模式）", "Action Editor (large window)");
        put("ae.max", "最大化", "Maximize");
        put("rename.ph", "输入新名称", "Enter new name");

        /* ----- create-modal form fields ----- */
        put("form.name", "名称（留空自动命名）", "Name (leave blank for auto-naming)");
        put("form.namePh", "可选", "Optional");
        put("form.world", "背景世界（角色在此世界中活动）", "Background World (where the character acts)");
        put("form.noWorld", "（无 / 空白世界）", "(none / blank world)");
        put("form.itemId", "物品 ID（如 minecraft:stone）", "Item ID (e.g. minecraft:stone)");
        put("form.type", "角色形态（生物/模型/粒子/方块）", "Character Form (mob/model/particle/block)");
        put("form.model", "角色模型（游戏内注册的角色 / 玩家）", "Character Model (in-game registered character / player)");
        put("form.modeSingle", "单选", "Single");
        put("form.modeExpr", "自定义表达式", "Custom Expression");
        put("form.modeGroup", "模型组", "Model Group");
        put("form.noModel", "（无可用模型）", "(no models available)");
        put("form.modelPh", "如 minecraft:zombie 或自定义模型标识", "e.g. minecraft:zombie or custom model id");
        put("form.selected", "已选", "Selected:");
        put("form.nameLabel", "名称", "Name");

        /* ----- create-modal titles (dialog headers) ----- */
        put("create.camera", "新建相机", "New Camera");
        put("create.sequence", "新建序列", "New Sequence");
        put("create.particle", "新建粒子", "New Particle");
        put("create.item", "新建物品", "New Item");

        /* ----- asset-bin section labels ----- */
        put("bin.work", "作品：", "Project: ");
        put("bin.sequence", "序列", "Sequence");
        put("bin.cameraGroup", "相机组", "Camera Group");
        put("bin.charActions", "角色动作", "Character Actions");
        put("bin.backpack", "全局背包", "Global Backpack");
        put("bin.title", "素材资源箱", "Asset Library");
        put("bin.createHint", "右键空白新建", "Right-click empty to create");
        put("bin.equip", "装备", "Equip");
        put("bin.collapse", "折叠/展开素材箱", "Collapse/Expand asset box");
        put("bin.worldOf", "世界：", "World: ");
        put("bin.ref", "引用", "Ref");
        put("bin.hintCreateCamera", "先用「右键素材箱空白处 › 新建项 › 相机」创建相机", "Right-click empty asset-box space › New Item › Camera to create a camera first");
        put("bin.hintSelectChar", "请先在「角色」分组里点选一个动作角色", "Select an action character in the Character group first");

        /* ----- timeline track labels ----- */
        put("track.replay", "轨道", "Track");
        put("track.audio", "音频", "Audio");
        put("track.audioEmpty", "音轨(暂无数据)", "Audio track (no data)");
        put("track.clip", "剪辑 ", "Clip ");

        /* ----- camera status warnings ----- */
        put("cam.warnLostNow", "⚠ 相机丢失：当前帧没有相机覆盖，无法播放", "⚠ Camera missing: no camera covers the current frame, cannot play");
        put("cam.warnOverlapNow", "⚠ 相机重叠：当前帧被多个相机覆盖", "⚠ Camera overlap: current frame covered by multiple cameras");
        put("cam.warnLost", "⚠ 相机丢失：时间轴存在无相机的区段", "⚠ Camera missing: timeline has segments with no camera");
        put("cam.warnOverlap", "⚠ 相机重叠：时间轴存在多相机重叠的区段", "⚠ Camera overlap: timeline has segments covered by multiple cameras");

        /* ----- toasts (static / composed) ----- */
        put("toast.charUnavailable", "该角色不可用", "This character is unavailable");
        put("toast.noActionToCompose", "没有可组合的动作，请先添加动作到时间轴", "No actions to combine; add actions to the timeline first");
        put("toast.nonClientGroup", "此动作组需进入预览世界才会播放", "This action group only plays in the preview world");
        put("toast.exitedPreview", "已退出预览世界", "Exited preview world");
        put("toast.noActionGroup", "暂无动作组，先用「组合动作组」生成", "No action groups yet; use 'Compose Action Group' first");
        put("toast.addedGroup", "已加入动作组 @ tick ", "Added to action group @ tick ");
        put("toast.setStartAction", "已设置起始帧：动作 #", "Set start frame: action #");
        put("toast.setStartMaterial", "已设置起始帧：材质 #", "Set start frame: material #");
        put("toast.atTick", " @ tick ", " @ tick ");

        /* ----- misc ----- */
        put("film.prefix", "场景：", "Scene: ");
    }

    private static void put(String key, String zh, String en)
    {
        Map<String, String> langs = new HashMap<String, String>();
        langs.put("zh_cn", zh);
        langs.put("en_gb", en);
        TABLE.put(key, langs);
    }

    /**
     * Build a JSON object mapping every string key to its value for the given
     * language. Falls back to {@code zh_cn}, then to the key itself, so a missing
     * translation never yields a blank label.
     */
    public static JsonObject stringsFor(String lang)
    {
        JsonObject out = new JsonObject();

        for (Map.Entry<String, Map<String, String>> e : TABLE.entrySet())
        {
            String key = e.getKey();
            Map<String, String> langs = e.getValue();
            String val = langs.get(lang);

            if (val == null)
            {
                val = langs.get("zh_cn");
            }

            if (val == null)
            {
                val = key;
            }

            out.add(key, new JsonPrimitive(val));
        }

        return out;
    }
}
