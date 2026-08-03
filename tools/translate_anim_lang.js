// 为动画编辑器键提供全部语言的真实翻译（替换之前的英文占位）
const fs = require("fs");
const path = require("path");

const dir = "D:/DEV/bbs_clean/src/client/resources/assets/bbs/assets/strings";

// 键名 -> 各语言翻译
const KEYS = [
  "bbs.ui.animation.title", "bbs.ui.animation.new", "bbs.ui.animation.save",
  "bbs.ui.animation.delete", "bbs.ui.animation.back", "bbs.ui.animation.add_bone",
  "bbs.ui.animation.delete_bone", "bbs.ui.animation.delete_key", "bbs.ui.animation.play",
  "bbs.ui.animation.stop", "bbs.ui.animation.name", "bbs.ui.animation.bone_name",
  "bbs.ui.animation.no_bones"
];

const T = {
  ar_ar: ["الرسوم المتحركة", "جديد", "حفظ", "حذف", "رجوع", "إضافة عظم", "إزالة عظم", "حذف الإطار الرئيسي", "تشغيل", "إيقاف", "اسم الحركة", "اسم العظم", "لا توجد عظام متاحة. أضف واحداً!"],
  de_de: ["Animation", "Neu", "Speichern", "Löschen", "Zurück", "Knochen hinzufügen", "Knochen entfernen", "Keyframe löschen", "Abspielen", "Stopp", "Animationsname", "Knochenname", "Keine Knochen verfügbar. Füge einen hinzu!"],
  es_es: ["Animación", "Nuevo", "Guardar", "Eliminar", "Volver", "Añadir hueso", "Quitar hueso", "Eliminar fotograma clave", "Reproducir", "Detener", "Nombre de la animación", "Nombre del hueso", "No hay huesos disponibles. ¡Añade uno!"],
  fr_fr: ["Animation", "Nouveau", "Enregistrer", "Supprimer", "Retour", "Ajouter un os", "Retirer un os", "Supprimer la clé d'animation", "Lire", "Arrêter", "Nom de l'animation", "Nom de l'os", "Aucun os disponible. Ajoutez-en un !"],
  hu_hu: ["Animáció", "Új", "Mentés", "Törlés", "Vissza", "Csont hozzáadása", "Csont eltávolítása", "Kulcskocka törlése", "Lejátszás", "Leállítás", "Animáció neve", "Csont neve", "Nincs elérhető csont. Adj hozzá egyet!"],
  id_id: ["Animasi", "Baru", "Simpan", "Hapus", "Kembali", "Tambah tulang", "Hapus tulang", "Hapus keyframe", "Putar", "Berhenti", "Nama animasi", "Nama tulang", "Tidak ada tulang tersedia. Tambahkan satu!"],
  ko_kr: ["애니메이션", "새로 만들기", "저장", "삭제", "뒤로", "뼈 추가", "뼈 제거", "키프레임 삭제", "재생", "정지", "애니메이션 이름", "뼈 이름", "사용 가능한 뼈가 없습니다. 하나 추가하세요!"],
  pl_pl: ["Animacja", "Nowa", "Zapisz", "Usuń", "Wstecz", "Dodaj kość", "Usuń kość", "Usuń klatkę kluczową", "Odtwórz", "Zatrzymaj", "Nazwa animacji", "Nazwa kości", "Brak dostępnych kości. Dodaj jedną!"],
  pt_br: ["Animação", "Novo", "Salvar", "Excluir", "Voltar", "Adicionar osso", "Remover osso", "Excluir quadro-chave", "Reproduzir", "Parar", "Nome da animação", "Nome do osso", "Nenhum osso disponível. Adicione um!"],
  pt_pt: ["Animação", "Novo", "Guardar", "Eliminar", "Voltar", "Adicionar osso", "Remover osso", "Eliminar quadro-chave", "Reproduzir", "Parar", "Nome da animação", "Nome do osso", "Nenhum osso disponível. Adicione um!"],
  ru_ru: ["Анимация", "Создать", "Сохранить", "Удалить", "Назад", "Добавить кость", "Удалить кость", "Удалить ключевой кадр", "Воспроизвести", "Стоп", "Название анимации", "Название кости", "Нет доступных костей. Добавьте одну!"],
  th_th: ["แอนิเมชัน", "ใหม่", "บันทึก", "ลบ", "กลับ", "เพิ่มกระดูก", "ลบกระดูก", "ลบคีย์เฟรม", "เล่น", "หยุด", "ชื่อแอนิเมชัน", "ชื่อกระดูก", "ไม่มีกระดูกที่พร้อมใช้ เพิ่มหนึ่งรายการ!"],
  tr_tr: ["Animasyon", "Yeni", "Kaydet", "Sil", "Geri", "Kemik ekle", "Kemiği kaldır", "Anahtar kareyi sil", "Oynat", "Durdur", "Animasyon adı", "Kemik adı", "Kullanılabilir kemik yok. Bir tane ekleyin!"],
  uk_ua: ["Анімація", "Створити", "Зберегти", "Видалити", "Назад", "Додати кістку", "Видалити кістку", "Видалити ключовий кадр", "Відтворити", "Стоп", "Назва анімації", "Назва кістки", "Немає доступних кісток. Додайте одну!"],
  ur_pk: ["حرکت پذیری", "نیا", "محفوظ کریں", "حذف کریں", "واپس", "ہڈی شامل کریں", "ہڈی ہٹائیں", "کلیدی فریم حذف کریں", "چلائیں", "روکیں", "حرکت کا نام", "ہڈی کا نام", "کوئی ہڈی دستیاب نہیں۔ ایک شامل کریں!"],
  vi_vn: ["Hoạt ảnh", "Mới", "Lưu", "Xóa", "Quay lại", "Thêm xương", "Gỡ xương", "Xóa khung hình chính", "Phát", "Dừng", "Tên hoạt ảnh", "Tên xương", "Không có xương khả dụng. Thêm một cái!"],
  zh_tw: ["動畫", "新建", "儲存", "刪除", "返回", "新增骨骼", "移除骨骼", "刪除關鍵影格", "播放", "停止", "動畫名稱", "骨骼名稱", "沒有可用骨骼，新增一個吧！"]
};

function esc(s) {
  return String(s).replace(/\\/g, "\\\\").replace(/"/g, '\\"');
}

for (const [lang, values] of Object.entries(T)) {
  const file = path.join(dir, lang + ".json");
  let content = fs.readFileSync(file, "utf-8");
  let lines = content.split("\n");

  for (let i = 0; i < KEYS.length; i++) {
    const key = KEYS[i];
    const val = esc(values[i]);
    // 逐行查找该键并整行替换
    for (let j = 0; j < lines.length; j++) {
      if (lines[j].includes('"' + key + '"')) {
        lines[j] = lines[j].replace(/:\s*".*"(\s*,?\s*)$/, ': "' + val + '"$1');
        break;
      }
    }
  }

  fs.writeFileSync(file, lines.join("\n"), "utf-8");
  console.log("OK", lang);
}

// 校验
let ok = 0, fail = 0;
for (const f of fs.readdirSync(dir)) {
  try { JSON.parse(fs.readFileSync(path.join(dir, f), "utf-8")); ok++; }
  catch (e) { fail++; console.log("FAIL", f, e.message); }
}
console.log("VALID OK:", ok, "FAIL:", fail);
