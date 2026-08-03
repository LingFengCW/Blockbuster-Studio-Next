// 修复：删除之前错误插入的键块，重新以正确逗号插入
const fs = require("fs");
const path = require("path");

const dir = "D:/DEV/bbs_clean/src/client/resources/assets/bbs/assets/strings";

// 从 add_lang_keys.js 提取翻译数据（通过读取文件并 eval 出 T）
const src = fs.readFileSync("D:/DEV/bbs_clean/tools/add_lang_keys.js", "utf-8");
const TStart = src.indexOf("const T = {");
const TEnd = src.indexOf("const KEYS =");
const TBody = src.slice(TStart + "const T = ".length, TEnd).replace(/;\s*$/, "");
const T = eval("(" + TBody + ")");

const KEYS = [
  "bbs.ui.project.title", "bbs.ui.project.create", "bbs.ui.project.delete",
  "bbs.ui.project.open", "bbs.ui.project.name", "bbs.ui.project.empty",
  "bbs.ui.project.current", "bbs.ui.project.confirm_delete",
  "bbs.ui.main_menu.projects", "bbs.ui.backpack.title", "bbs.ui.backpack.export",
  "bbs.ui.backpack.import", "bbs.ui.backpack.delete", "bbs.ui.backpack.empty",
  "bbs.ui.backpack.exported", "bbs.ui.backpack.imported"
];

for (const [lang, dict] of Object.entries(T)) {
  const file = path.join(dir, lang + ".json");
  let content = fs.readFileSync(file, "utf-8");
  let lines = content.split("\n");

  // 删除旧的错误插入块（从 project.title 到 backpack.imported 的行）
  let start = -1, end = -1;
  for (let i = 0; i < lines.length; i++) {
    if (lines[i].includes('"bbs.ui.project.title"')) start = i;
    if (lines[i].includes('"bbs.ui.backpack.imported"')) end = i;
  }
  if (start >= 0 && end >= start) {
    // 上一行去掉尾部逗号
    lines[start - 1] = lines[start - 1].replace(/,$/, "");
    lines.splice(start, end - start + 1);
    content = lines.join("\n");
  }

  // 正确插入：每个新键行以逗号开头，值内引号做 JSON 转义
  const block = KEYS.map(k => `,\n    "${k}": "${String(dict[k]).replace(/"/g, '\\"')}"`).join("");
  const idx = content.trimEnd().lastIndexOf("}");
  content = content.slice(0, idx) + block + "\n" + content.slice(idx);

  fs.writeFileSync(file, content, "utf-8");
  console.log("FIXED", lang);
}

// 校验
let ok = 0, fail = 0;
for (const f of fs.readdirSync(dir)) {
  try { JSON.parse(fs.readFileSync(path.join(dir, f), "utf-8")); ok++; }
  catch (e) { fail++; console.log("STILL FAIL", f, e.message); }
}
console.log("VALID OK:", ok, "FAIL:", fail);
