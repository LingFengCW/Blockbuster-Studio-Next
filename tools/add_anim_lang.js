// 批量添加动画编辑器的语言键
const fs = require("fs");
const path = require("path");

const dir = "D:/DEV/bbs_clean/src/client/resources/assets/bbs/assets/strings";

const EN = {
  "bbs.ui.animation.title": "Animation",
  "bbs.ui.animation.new": "New",
  "bbs.ui.animation.save": "Save",
  "bbs.ui.animation.delete": "Delete",
  "bbs.ui.animation.back": "Back",
  "bbs.ui.animation.add_bone": "Add bone",
  "bbs.ui.animation.delete_bone": "Remove bone",
  "bbs.ui.animation.delete_key": "Delete keyframe",
  "bbs.ui.animation.play": "Play",
  "bbs.ui.animation.stop": "Stop",
  "bbs.ui.animation.name": "Animation name",
  "bbs.ui.animation.bone_name": "Bone name",
  "bbs.ui.animation.no_bones": "No bones available. Add one!"
};

const ZH = {
  "bbs.ui.animation.title": "动画",
  "bbs.ui.animation.new": "新建",
  "bbs.ui.animation.save": "保存",
  "bbs.ui.animation.delete": "删除",
  "bbs.ui.animation.back": "返回",
  "bbs.ui.animation.add_bone": "添加部位",
  "bbs.ui.animation.delete_bone": "移除部位",
  "bbs.ui.animation.delete_key": "删除关键帧",
  "bbs.ui.animation.play": "播放",
  "bbs.ui.animation.stop": "停止",
  "bbs.ui.animation.name": "动画名称",
  "bbs.ui.animation.bone_name": "部位名称",
  "bbs.ui.animation.no_bones": "没有可用部位，添加一个吧！"
};

const langs = fs.readdirSync(dir).filter(f => f.endsWith(".json"));
const KEYS = Object.keys(EN);

for (const f of langs) {
  const file = path.join(dir, f);
  let content = fs.readFileSync(file, "utf-8");
  const lang = f.replace(".json", "");
  const dict = lang === "zh_cn" ? ZH : EN;
  const block = KEYS.map(k => `,\n    "${k}": "${dict[k]}"`).join("");
  const idx = content.trimEnd().lastIndexOf("}");
  content = content.slice(0, idx) + block + "\n" + content.slice(idx);
  fs.writeFileSync(file, content, "utf-8");
}

let ok = 0, fail = 0;
for (const f of langs) {
  try { JSON.parse(fs.readFileSync(path.join(dir, f), "utf-8")); ok++; }
  catch (e) { fail++; console.log("FAIL", f, e.message); }
}
console.log("ANIM LANG OK:", ok, "FAIL:", fail);
