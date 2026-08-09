package lingfeng.bbsnext.mcef;

import net.minecraft.client.Minecraft;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.ColorUIResource;
import java.awt.*;
import java.io.File;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Native (OS-level) Swing dialogs that float ABOVE the Minecraft window.
 *
 * <p>The HTML editor lives inside an MCEF off-screen browser that is blitted
 * onto the game's GUI. Because of that, any in-page popup (and BBS's own
 * {@code UIOverlay.addOverlay} panels, which render *under* the MCEF texture)
 * gets visually buried and cannot be interacted with. To get a real,
 * clickable input dialog we pop a separate AWT/Swing window with
 * {@link JDialog#setAlwaysOnTop(boolean)}.</p>
 *
 * <p>The dialogs are styled with a clean dark "modern" theme so they read as
 * a native part of the editor rather than the dated Windows look-and-feel.</p>
 *
 * <p>Every dialog is built on the EDT via {@link SwingUtilities#invokeLater}
 * and reports its result through a callback. Callers must hop back onto the
 * Minecraft main thread (via {@link Minecraft#execute}) before touching BBS
 * data, since the callback fires on the EDT, not the render thread.</p>
 *
 * <p>Note: this requires Minecraft to run in windowed or borderless mode.
 * Under exclusive fullscreen the OS will not let a separate window appear on
 * top of the grabbed display.</p>
 */
public class NativeDialog
{
    /* ---- Modern dark theme palette ---- */
    private static final Color BG     = new Color(0x1b1d23);
    private static final Color PANEL  = new Color(0x24272f);
    private static final Color FIELD  = new Color(0x1f2229);
    private static final Color TEXT   = new Color(0xe8eaed);
    private static final Color MUTED  = new Color(0x9aa0aa);
    private static final Color BORDER = new Color(0x353a45);
    private static final Color ACCENT = new Color(0x4c8dff);

    static
    {
        try
        {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        }
        catch (Exception ignored)
        {
        }

        /* Broad dark-theme overrides so every Swing control (including the
         * combo-box popup list) matches the dark palette. */
        UIManager.put("Panel.background", new ColorUIResource(PANEL));
        UIManager.put("Panel.foreground", new ColorUIResource(TEXT));
        UIManager.put("Label.foreground", new ColorUIResource(TEXT));
        UIManager.put("TextField.background", new ColorUIResource(FIELD));
        UIManager.put("TextField.foreground", new ColorUIResource(TEXT));
        UIManager.put("TextField.caretForeground", new ColorUIResource(TEXT));
        UIManager.put("TextField.inactiveForeground", new ColorUIResource(MUTED));
        UIManager.put("TextField.border", new LineBorder(BORDER, 1, true));
        UIManager.put("Button.background", new ColorUIResource(PANEL));
        UIManager.put("Button.foreground", new ColorUIResource(TEXT));
        UIManager.put("Button.select", new ColorUIResource(ACCENT));
        UIManager.put("Button.focus", new ColorUIResource(ACCENT));
        UIManager.put("ComboBox.background", new ColorUIResource(FIELD));
        UIManager.put("ComboBox.foreground", new ColorUIResource(TEXT));
        UIManager.put("ComboBox.buttonBackground", new ColorUIResource(BORDER));
        UIManager.put("ComboBox.buttonShadow", new ColorUIResource(BORDER));
        UIManager.put("ComboBox.selectionBackground", new ColorUIResource(ACCENT));
        UIManager.put("ComboBox.selectionForeground", new ColorUIResource(Color.WHITE));
        UIManager.put("ComboBox.border", new LineBorder(BORDER, 1, true));
        UIManager.put("List.background", new ColorUIResource(FIELD));
        UIManager.put("List.foreground", new ColorUIResource(TEXT));
        UIManager.put("List.selectionBackground", new ColorUIResource(ACCENT));
        UIManager.put("List.selectionForeground", new ColorUIResource(Color.WHITE));
        UIManager.put("CheckBox.background", new ColorUIResource(PANEL));
        UIManager.put("CheckBox.foreground", new ColorUIResource(TEXT));
        UIManager.put("OptionPane.background", new ColorUIResource(BG));
        UIManager.put("PopupMenu.background", new ColorUIResource(PANEL));
        UIManager.put("ToolTip.background", new ColorUIResource(PANEL));
        UIManager.put("ToolTip.foreground", new ColorUIResource(TEXT));
    }

    private static JDialog dialog(String title)
    {
        JDialog dlg = new JDialog((Frame) null, title, true);
        dlg.setAlwaysOnTop(true);
        dlg.setModal(true);
        dlg.setResizable(false);
        dlg.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dlg.getContentPane().setBackground(BG);
        return dlg;
    }

    private static void open(JDialog dlg)
    {
        dlg.pack();
        dlg.setLocationRelativeTo(null);
        dlg.toFront();
        dlg.requestFocusInWindow();
        dlg.setVisible(true);
    }

    /** Modern button: accent (filled) for primary, ghost (outlined) for
     *  secondary. Flat, focus ring removed, comfortable height. */
    private static JButton button(String text, boolean accent)
    {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setFocusable(false);
        b.setOpaque(true);
        b.setBorder(new LineBorder(accent ? ACCENT.darker() : BORDER, 1, true));
        b.setBackground(accent ? ACCENT : PANEL);
        b.setForeground(accent ? Color.WHITE : TEXT);
        b.setPreferredSize(new Dimension(b.getPreferredSize().width + 28, 34));
        return b;
    }

    private static JPanel buttonRow(JButton... buttons)
    {
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btns.setOpaque(false);
        for (JButton b : buttons)
        {
            btns.add(b);
        }
        return btns;
    }

    private static JTextField textField(String def, int cols)
    {
        JTextField tf = new JTextField(def == null ? "" : def, cols);
        tf.setOpaque(true);
        tf.setBackground(FIELD);
        tf.setForeground(TEXT);
        tf.setCaretColor(TEXT);
        tf.setBorder(new LineBorder(BORDER, 1, true));
        return tf;
    }

    /** Plain single-line text input. {@code onResult} receives the trimmed
     *  text, or {@code null} when cancelled/closed. */
    public static void textInput(String title, String message, String def, Consumer<String> onResult)
    {
        SwingUtilities.invokeLater(() ->
        {
            JDialog dlg = dialog(title);
            JPanel root = new JPanel(new BorderLayout(10, 12));
            root.setBorder(new EmptyBorder(16, 16, 16, 16));
            root.setBackground(PANEL);

            JLabel msg = new JLabel(message);
            msg.setForeground(TEXT);
            root.add(msg, BorderLayout.NORTH);

            JTextField tf = textField(def, 26);
            root.add(tf, BorderLayout.CENTER);

            JButton cancel = button("取消", false);
            JButton ok = button("确定", true);
            root.add(buttonRow(cancel, ok), BorderLayout.SOUTH);

            final boolean[] closed = {false};
            Consumer<String> finish = (value) ->
            {
                if (closed[0])
                {
                    return;
                }

                closed[0] = true;
                dlg.dispose();
                onResult.accept(value);
            };

            ok.addActionListener(e -> finish.accept(tf.getText().trim()));
            cancel.addActionListener(e -> finish.accept(null));
            dlg.addWindowListener(new java.awt.event.WindowAdapter()
            {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e)
                {
                    finish.accept(null);
                }
            });
            tf.addActionListener(e -> ok.doClick());

            dlg.add(root);
            open(dlg);
        });
    }

    /** New-scene dialog: scene name + background world picker. */
    public static void sceneDialog(BiConsumer<String, String> onResult)
    {
        SwingUtilities.invokeLater(() ->
        {
            JDialog dlg = dialog("新建场景");
            JPanel root = new JPanel(new BorderLayout(12, 12));
            root.setBorder(new EmptyBorder(16, 16, 16, 16));
            root.setBackground(PANEL);

            JPanel form = new JPanel();
            form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
            form.setOpaque(false);

            JLabel nameLbl = new JLabel("场景名称");
            nameLbl.setForeground(MUTED);
            nameLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            form.add(nameLbl);

            JTextField name = textField("", 26);
            name.setAlignmentX(Component.LEFT_ALIGNMENT);
            name.setMaximumSize(new Dimension(Integer.MAX_VALUE, name.getPreferredSize().height));
            form.add(name);
            form.add(Box.createVerticalStrut(12));

            JLabel bgLbl = new JLabel("背景世界（留空为空白世界）");
            bgLbl.setForeground(MUTED);
            bgLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            form.add(bgLbl);

            DefaultComboBoxModel<String> bgModel = new DefaultComboBoxModel<>();
            bgModel.addElement("");

            File saves = new File(Minecraft.getInstance().gameDirectory, "saves");

            if (saves.isDirectory())
            {
                File[] dirs = saves.listFiles(File::isDirectory);

                if (dirs != null)
                {
                    for (File d : dirs)
                    {
                        if (new File(d, "level.dat").exists())
                        {
                            bgModel.addElement(d.getName());
                        }
                    }
                }
            }

            JComboBox<String> bg = new JComboBox<>(bgModel);
            bg.setAlignmentX(Component.LEFT_ALIGNMENT);
            bg.setMaximumSize(new Dimension(Integer.MAX_VALUE, bg.getPreferredSize().height));
            bg.setBackground(FIELD);
            bg.setForeground(TEXT);
            form.add(bg);

            root.add(form, BorderLayout.CENTER);

            JButton cancel = button("取消", false);
            JButton ok = button("确定", true);
            root.add(buttonRow(cancel, ok), BorderLayout.SOUTH);

            final boolean[] closed = {false};
            Runnable finish = () ->
            {
                if (closed[0])
                {
                    return;
                }

                closed[0] = true;
                String n = name.getText().trim();
                String b = (String) bg.getSelectedItem();
                dlg.dispose();
                onResult.accept(n, b == null ? "" : b);
            };

            ok.addActionListener(e -> finish.run());
            cancel.addActionListener(e -> { if (!closed[0]) { closed[0] = true; dlg.dispose(); onResult.accept(null, null); } });
            dlg.addWindowListener(new java.awt.event.WindowAdapter()
            {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e)
                {
                    if (!closed[0]) { closed[0] = true; dlg.dispose(); onResult.accept(null, null); }
                }
            });

            dlg.add(root);
            open(dlg);
        });
    }

    /** New-character / entity dialog result. */
    public static class CharResult
    {
        public final String name;
        public final String type; /* MOB, MODEL, PARTICLE, BLOCK */
        public final String charType; /* keyframe | action */
        public final boolean actor;
        public final boolean shadow;
        public final boolean looping;

        public CharResult(String name, String type, String charType, boolean actor, boolean shadow, boolean looping)
        {
            this.name = name;
            this.type = type;
            this.charType = charType;
            this.actor = actor;
            this.shadow = shadow;
            this.looping = looping;
        }
    }

    /** New-character/entity dialog: name + form type + actor/shadow/looping
     *  options, with a segmented type picker for a modern feel. */
    public static void characterDialog(Consumer<CharResult> onResult)
    {
        SwingUtilities.invokeLater(() ->
        {
            JDialog dlg = dialog("新建角色 / 实体");
            JPanel root = new JPanel(new BorderLayout(12, 12));
            root.setBorder(new EmptyBorder(16, 16, 16, 16));
            root.setBackground(PANEL);

            JPanel form = new JPanel();
            form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
            form.setOpaque(false);

            JLabel nameLbl = new JLabel("名称（可留空）");
            nameLbl.setForeground(MUTED);
            nameLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            form.add(nameLbl);

            JTextField name = textField("", 26);
            name.setAlignmentX(Component.LEFT_ALIGNMENT);
            name.setMaximumSize(new Dimension(Integer.MAX_VALUE, name.getPreferredSize().height));
            form.add(name);
            form.add(Box.createVerticalStrut(12));

            JLabel typeLbl = new JLabel("类型");
            typeLbl.setForeground(MUTED);
            typeLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            form.add(typeLbl);

            String[] types = {"MOB", "MODEL", "PARTICLE", "BLOCK"};
            String[] typeLabels = {"生物 (MOB)", "模型 (MODEL)", "粒子 (PARTICLE)", "方块 (BLOCK)"};
            final String[] selected = {types[0]};

            JPanel seg = new JPanel(new GridLayout(1, types.length, 6, 0));
            seg.setOpaque(false);
            seg.setAlignmentX(Component.LEFT_ALIGNMENT);
            seg.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            JButton[] segBtns = new JButton[types.length];

            for (int i = 0; i < types.length; i++)
            {
                final int idx = i;
                JButton b = new JButton(typeLabels[i]);
                b.setFocusPainted(false);
                b.setOpaque(true);
                b.setBackground(PANEL);
                b.setForeground(TEXT);
                b.setBorder(new LineBorder(BORDER, 1, true));
                b.addActionListener(e ->
                {
                    selected[0] = types[idx];
                    for (int j = 0; j < segBtns.length; j++)
                    {
                        boolean on = j == idx;
                        segBtns[j].setBackground(on ? ACCENT : PANEL);
                        segBtns[j].setForeground(on ? Color.WHITE : TEXT);
                        segBtns[j].setBorder(new LineBorder(on ? ACCENT.darker() : BORDER, 1, true));
                    }
                });
                segBtns[i] = b;
                seg.add(b);
            }
            /* Highlight the default (MOB). */
            segBtns[0].setBackground(ACCENT);
            segBtns[0].setForeground(Color.WHITE);
            segBtns[0].setBorder(new LineBorder(ACCENT.darker(), 1, true));
            form.add(seg);
            form.add(Box.createVerticalStrut(12));

            JLabel ctLbl = new JLabel("角色类型");
            ctLbl.setForeground(MUTED);
            ctLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            form.add(ctLbl);

            String[] cts = {"keyframe", "action"};
            String[] ctLabels = {"关键帧角色", "纯动作角色"};
            final String[] selectedCT = {"action"};
            JPanel ctSeg = new JPanel(new GridLayout(1, cts.length, 6, 0));
            ctSeg.setOpaque(false);
            ctSeg.setAlignmentX(Component.LEFT_ALIGNMENT);
            ctSeg.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            JButton[] ctBtns = new JButton[cts.length];
            for (int i = 0; i < cts.length; i++)
            {
                final int idx = i;
                JButton b = new JButton(ctLabels[i]);
                b.setFocusPainted(false);
                b.setOpaque(true);
                b.setBackground(PANEL);
                b.setForeground(TEXT);
                b.setBorder(new LineBorder(BORDER, 1, true));
                b.addActionListener(e ->
                {
                    selectedCT[0] = cts[idx];
                    for (int j = 0; j < ctBtns.length; j++)
                    {
                        boolean on = j == idx;
                        ctBtns[j].setBackground(on ? ACCENT : PANEL);
                        ctBtns[j].setForeground(on ? Color.WHITE : TEXT);
                        ctBtns[j].setBorder(new LineBorder(on ? ACCENT.darker() : BORDER, 1, true));
                    }
                });
                ctBtns[i] = b;
                ctSeg.add(b);
            }
            ctBtns[1].setBackground(ACCENT);
            ctBtns[1].setForeground(Color.WHITE);
            ctBtns[1].setBorder(new LineBorder(ACCENT.darker(), 1, true));
            form.add(ctSeg);
            form.add(Box.createVerticalStrut(12));

            JCheckBox actor = new JCheckBox("作为演员 (actor)", false);
            JCheckBox shadow = new JCheckBox("投射阴影 (shadow)", true);
            JCheckBox looping = new JCheckBox("循环 (looping)", false);
            for (JCheckBox cb : new JCheckBox[]{actor, shadow, looping})
            {
                cb.setOpaque(false);
                cb.setForeground(TEXT);
                cb.setAlignmentX(Component.LEFT_ALIGNMENT);
                form.add(cb);
            }

            root.add(form, BorderLayout.CENTER);

            JButton cancel = button("取消", false);
            JButton ok = button("确定", true);
            root.add(buttonRow(cancel, ok), BorderLayout.SOUTH);

            final boolean[] closed = {false};
            Runnable finish = () ->
            {
                if (closed[0])
                {
                    return;
                }

                closed[0] = true;
                dlg.dispose();
                onResult.accept(new CharResult(name.getText().trim(),
                    selected[0], selectedCT[0], actor.isSelected(), shadow.isSelected(), looping.isSelected()));
            };

            ok.addActionListener(e -> finish.run());
            cancel.addActionListener(e -> { if (!closed[0]) { closed[0] = true; dlg.dispose(); onResult.accept(null); } });
            dlg.addWindowListener(new java.awt.event.WindowAdapter()
            {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e)
                {
                    if (!closed[0]) { closed[0] = true; dlg.dispose(); onResult.accept(null); }
                }
            });

            dlg.add(root);
            open(dlg);
        });
    }

    /** New-item dialog: ask for an item id (e.g. minecraft:diamond_sword). */
    public static void itemDialog(Consumer<String> onResult)
    {
        textInput("新建物品", "物品 ID（例如 minecraft:diamond_sword）：", "minecraft:", onResult);
    }

    /** Equip-entry result. {@code itemId == null} means "clear this slot". */
    public static class EquipEntry
    {
        public final String slot;
        public final String itemId;

        public EquipEntry(String slot, String itemId)
        {
            this.slot = slot;
            this.itemId = itemId;
        }
    }

    /** Equipment dialog: one row per armor/hand slot, each with an item id
     *  field and 装配/清除 buttons. {@code onEquip} fires immediately per
     *  action so the caller can apply it live; the window stays open until
     *  关闭. */
    public static void equipDialog(String title, Consumer<EquipEntry> onEquip)
    {
        SwingUtilities.invokeLater(() ->
        {
            JDialog dlg = dialog(title == null ? "装配装备" : title);
            JPanel root = new JPanel(new BorderLayout(12, 12));
            root.setBorder(new EmptyBorder(16, 16, 16, 16));
            root.setBackground(PANEL);

            JPanel form = new JPanel(new GridLayout(0, 1, 8, 8));
            form.setOpaque(false);
            String[][] slots = {
                {"head", "头盔"}, {"chest", "胸甲"}, {"legs", "护腿"},
                {"feet", "靴子"}, {"mainhand", "主手"}, {"offhand", "副手"}
            };

            for (String[] slot : slots)
            {
                JPanel row = new JPanel(new BorderLayout(8, 8));
                row.setOpaque(false);
                JLabel lbl = new JLabel(slot[1]);
                lbl.setForeground(TEXT);
                row.add(lbl, BorderLayout.WEST);

                JTextField tf = textField("", 22);
                row.add(tf, BorderLayout.CENTER);

                JPanel ab = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
                ab.setOpaque(false);
                JButton set = button("装配", true);
                JButton clr = button("清除", false);
                ab.add(set);
                ab.add(clr);
                row.add(ab, BorderLayout.EAST);

                final String slotKey = slot[0];

                set.addActionListener(e -> onEquip.accept(new EquipEntry(slotKey, tf.getText().trim())));
                clr.addActionListener(e ->
                {
                    tf.setText("");
                    onEquip.accept(new EquipEntry(slotKey, null));
                });

                form.add(row);
            }

            root.add(form, BorderLayout.CENTER);

            JButton close = button("关闭", false);
            JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            btns.setOpaque(false);
            btns.add(close);
            root.add(btns, BorderLayout.SOUTH);
            close.addActionListener(e -> dlg.dispose());

            dlg.add(root);
            open(dlg);
        });
    }
}
