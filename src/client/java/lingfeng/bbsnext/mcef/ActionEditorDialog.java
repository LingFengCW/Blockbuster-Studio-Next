package lingfeng.bbsnext.mcef;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.actions.types.ActionClip;
import mchorse.bbs_mod.actions.types.LocomotionActionClip;
import mchorse.bbs_mod.actions.types.ScriptActionClip;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.film.replays.ReplayKeyframes;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.TransferHandler;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Standalone "动作编辑器" window for a single character (Replay).
 *
 * <p>Opened from the editor toolbar, it pops a real OS window (always on top,
 * non-modal) so it does NOT depend on the Minecraft in-game window.</p>
 *
 * <p>Two character kinds:
 * <ul>
 *   <li><b>关键帧角色 (keyframe)</b> - body movement is driven by
 *       {@link Replay#keyframes} keyframe channels; the left list shows each
 *       body channel and the right panel edits its keyframes.</li>
 *   <li><b>纯动作角色 (action)</b> - driven by {@link Replay#actions}
 *       (ActionClips). The left list shows the actions (and, in "路径" mode,
 *       the walking-path keyframe channels x/y/z/yaw). Actions can be dragged
 *       from the list straight onto the timeline to set their start tick.</li>
 * </ul>
 *
 * <p>The top character selector pulls any character into this window (the
 * "拖进来" step); the ▼ next to the name opens armor info + the action list.</p>
 */
public class ActionEditorDialog
{
    /* ---- dark theme palette (mirrors NativeDialog) ---- */
    private static final Color BG = new Color(0x1b1d23);
    private static final Color PANEL = new Color(0x24272f);
    private static final Color FIELD = new Color(0x1f2229);
    private static final Color TEXT = new Color(0xe8eaed);
    private static final Color MUTED = new Color(0x9aa0aa);
    private static final Color BORDER = new Color(0x353a45);
    private static final Color ACCENT = new Color(0x4c8dff);

    private final List<Replay> replays;
    private int replayIndex;
    private Replay replay;

    /* action-mode sub mode: "action" (list actions) | "path" (list path channels) */
    private String leftMode = "action";

    private JDialog dlg;
    private JLabel nameLabel;
    private JButton[] typeBtns = new JButton[2];
    private JList<String> list;
    private DefaultListModel<String> listModel = new DefaultListModel<>();
    private List<Object> backing = new ArrayList<>();
    private JPanel rightPanel;
    private TrackCanvas track;
    private JPanel modeRow;

    /** Currently selected item (ActionClip / ScriptActionClip / KeyframeChannel<Double>). */
    private Object selected = null;

    public ActionEditorDialog(List<Replay> replays, int index)
    {
        this.replays = replays;
        this.replayIndex = index;
        this.replay = replays.get(index);
    }

    public static void open(List<Replay> replays, int index, int actionIndex)
    {
        if (replays == null || replays.isEmpty())
        {
            return;
        }

        if (index < 0 || index >= replays.size())
        {
            index = 0;
        }

        final int idx = index;
        final int act = actionIndex;
        SwingUtilities.invokeLater(() -> new ActionEditorDialog(replays, idx).build(act));
    }

    private void build(int actionIndex)
    {
        dlg = new JDialog((Frame) null, "动作编辑器", false);
        dlg.setAlwaysOnTop(true);
        dlg.setResizable(true);
        dlg.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dlg.setSize(960, 600);
        dlg.getContentPane().setBackground(BG);
        dlg.setLayout(new BorderLayout(0, 0));

        dlg.add(buildHeader(), BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerSize(4);
        split.setBackground(BORDER);
        split.setLeftComponent(buildLeft());
        split.setRightComponent(buildRight());
        split.setDividerLocation(320);
        dlg.add(split, BorderLayout.CENTER);

        refresh();

        if (actionIndex >= 0 && actionIndex < replay.actions.get().size())
        {
            selectObject(replay.actions.get().get(actionIndex));
        }

        dlg.setLocationRelativeTo(null);
        dlg.toFront();
        dlg.setVisible(true);
    }

    /* ---------------- header ---------------- */

    private JPanel buildHeader()
    {
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setBackground(PANEL);
        header.setBorder(new EmptyBorder(8, 12, 8, 12));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);

        /* Character selector: pull any character into this window. */
        String[] names = new String[replays.size()];
        for (int i = 0; i < replays.size(); i++)
        {
            names[i] = (i + 1) + ". " + replays.get(i).getName();
        }
        JComboBox<String> charSel = new JComboBox<>(names);
        charSel.setSelectedIndex(replayIndex);
        charSel.setBackground(FIELD);
        charSel.setForeground(TEXT);
        charSel.setMaximumSize(new Dimension(220, 30));
        charSel.addActionListener(e ->
        {
            replayIndex = charSel.getSelectedIndex();
            replay = replays.get(replayIndex);
            refresh();
        });
        left.add(charSel);

        JButton drop = new JButton("▼");
        drop.setFocusPainted(false);
        drop.setBackground(PANEL);
        drop.setForeground(TEXT);
        drop.setBorder(new LineBorder(BORDER, 1, true));
        drop.addActionListener(e -> showDropMenu(drop));
        left.add(drop);

        nameLabel = new JLabel(replay.getName());
        nameLabel.setForeground(TEXT);
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));
        left.add(nameLabel);

        header.add(left, BorderLayout.WEST);

        JPanel typePanel = new JPanel(new GridLayout(1, 2, 6, 0));
        typePanel.setOpaque(false);
        typePanel.setMaximumSize(new Dimension(260, 32));
        typeBtns[0] = segBtn("关键帧角色", "keyframe");
        typeBtns[1] = segBtn("纯动作角色", "action");
        typePanel.add(typeBtns[0]);
        typePanel.add(typeBtns[1]);
        header.add(typePanel, BorderLayout.CENTER);

        JButton close = new JButton("关闭");
        close.setFocusPainted(false);
        close.setBackground(PANEL);
        close.setForeground(TEXT);
        close.setBorder(new LineBorder(BORDER, 1, true));
        close.addActionListener(e -> dlg.dispose());
        header.add(close, BorderLayout.EAST);

        return header;
    }

    private JButton segBtn(String label, String value)
    {
        JButton b = new JButton(label);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setBorder(new LineBorder(BORDER, 1, true));
        b.addActionListener(e ->
        {
            mc(() -> replay.characterType.set(value));
            leftMode = "action";
            refresh();
        });
        return b;
    }

    private void showDropMenu(Component anchor)
    {
        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(PANEL);
        menu.setBorder(new LineBorder(BORDER, 1, true));

        menu.add(labelItem("盔甲信息"));
        menu.add(sep());
        addArmorRow(menu, "头盔", replay.keyframes.armorHead.interpolate(0, ItemStack.EMPTY));
        addArmorRow(menu, "胸甲", replay.keyframes.armorChest.interpolate(0, ItemStack.EMPTY));
        addArmorRow(menu, "护腿", replay.keyframes.armorLegs.interpolate(0, ItemStack.EMPTY));
        addArmorRow(menu, "靴子", replay.keyframes.armorFeet.interpolate(0, ItemStack.EMPTY));
        addArmorRow(menu, "主手", replay.keyframes.mainHand.interpolate(0, ItemStack.EMPTY));
        addArmorRow(menu, "副手", replay.keyframes.offHand.interpolate(0, ItemStack.EMPTY));

        menu.add(sep());
        menu.add(labelItem("动作"));
        for (Clip clip : replay.actions.get())
        {
            JMenuItem it = new JMenuItem(clip.title.get().isEmpty() ? "(未命名动作)" : clip.title.get());
            it.setBackground(PANEL);
            it.setForeground(TEXT);
            it.addActionListener(e -> selectObject(clip));
            menu.add(it);
        }

        menu.show(anchor, 0, anchor.getHeight());
    }

    private void addArmorRow(JPopupMenu menu, String slot, ItemStack stack)
    {
        String txt = stack.isEmpty() ? "空" : slot + "：" + stack.getItem().toString();
        JMenuItem it = new JMenuItem(txt);
        it.setBackground(PANEL);
        it.setForeground(MUTED);
        menu.add(it);
    }

    /* ---------------- left ---------------- */

    private JPanel buildLeft()
    {
        JPanel left = new JPanel(new BorderLayout(0, 0));
        left.setBackground(PANEL);
        left.setBorder(new LineBorder(BORDER, 0, true));

        JPanel north = new JPanel(new BorderLayout(0, 6));
        north.setOpaque(false);
        north.setBorder(new EmptyBorder(8, 10, 6, 10));

        JLabel title = new JLabel("动作");
        title.setForeground(MUTED);
        north.add(title, BorderLayout.NORTH);

        /* action/路径 toggle (only meaningful for action characters) */
        modeRow = new JPanel(new GridLayout(1, 2, 6, 0));
        modeRow.setOpaque(false);
        JButton mAct = modeBtn("动作", "action");
        JButton mPath = modeBtn("路径", "path");
        modeRow.add(mAct);
        modeRow.add(mPath);
        north.add(modeRow, BorderLayout.SOUTH);

        left.add(north, BorderLayout.NORTH);

        list = new JList<>(listModel);
        list.setBackground(FIELD);
        list.setForeground(TEXT);
        list.setSelectionBackground(ACCENT);
        list.setFixedCellHeight(26);
        list.setDragEnabled(true);
        list.setTransferHandler(new ActionListTransferHandler());
        list.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                int i = list.getSelectedIndex();
                if (i >= 0 && i < backing.size())
                {
                    selectObject(backing.get(i));
                }
            }
        });
        left.add(new JScrollPane(list), BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        btns.setOpaque(false);
        JButton add = new JButton("+ 新建");
        JButton del = new JButton("删除");
        for (JButton b : new JButton[]{add, del})
        {
            b.setFocusPainted(false);
            b.setBackground(PANEL);
            b.setForeground(TEXT);
            b.setBorder(new LineBorder(BORDER, 1, true));
        }
        add.addActionListener(e -> onNew());
        del.addActionListener(e -> onDelete());
        btns.add(add);
        btns.add(del);
        left.add(btns, BorderLayout.SOUTH);

        return left;
    }

    private JButton modeBtn(String label, String value)
    {
        JButton b = new JButton(label);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setBorder(new LineBorder(BORDER, 1, true));
        b.addActionListener(e ->
        {
            leftMode = value;
            refresh();
        });
        return b;
    }

    private void onNew()
    {
        if (!"action".equals(replay.characterType.get()))
        {
            JOptionPane.showMessageDialog(dlg, "关键帧角色请直接选中左侧身体通道，在右侧添加关键帧。", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if ("path".equals(leftMode))
        {
            JOptionPane.showMessageDialog(dlg, "路径关键帧请在右侧直接添加帧（x / z / yaw 等）。", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(PANEL);
        String[][] opts = {
            {"走路", "locomotion", "walk"},
            {"奔跑", "locomotion", "run"},
            {"空闲", "locomotion", "idle"},
            {"攻击", "attack", null},
            {"挥击", "swipe", null},
            {"破坏方块", "break_block", null},
            {"放置方块", "place_block", null},
            {"聊天", "chat", null},
            {"命令", "command", null},
            {"丢物品", "drop_item", null},
            {"脚本动作", "script", null}
        };
        for (String[] o : opts)
        {
            JMenuItem it = new JMenuItem(o[0]);
            it.setBackground(PANEL);
            it.setForeground(TEXT);
            it.addActionListener(e -> createAction(o[1], o[2], o[0]));
            menu.add(it);
        }
        menu.show(list, 0, list.getHeight());
    }

    private void createAction(String factoryKey, String mode, String label)
    {
        mc(() ->
        {
            Clip c = BBSMod.getFactoryActionClips().create(Link.bbs(factoryKey));

            if (c instanceof ActionClip ac)
            {
                ac.title.set(label);
                ac.duration.set(20);

                if (c instanceof LocomotionActionClip loc && mode != null)
                {
                    loc.mode.set(mode);
                }

                replay.actions.addClip(ac);
            }
        });
        refresh();
    }

    private void onDelete()
    {
        if (selected instanceof ActionClip ac)
        {
            mc(() -> replay.actions.remove(ac));
            selected = null;
            refresh();
        }
        else if (selected instanceof ScriptActionClip)
        {
            mc(() -> replay.actions.remove((Clip) selected));
            selected = null;
            refresh();
        }
        else if (selected instanceof KeyframeChannel)
        {
            mc(() -> ((KeyframeChannel<?>) selected).getKeyframes().clear());
            refresh();
        }
    }

    /* ---------------- right ---------------- */

    private JComponent buildRight()
    {
        rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBackground(PANEL);
        rightPanel.setBorder(new EmptyBorder(12, 12, 12, 12));

        track = new TrackCanvas();
        track.setPreferredSize(new Dimension(Integer.MAX_VALUE, 90));
        track.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        track.setTransferHandler(new TrackDropHandler());

        JScrollPane scroller = new JScrollPane(rightPanel);
        scroller.setBackground(PANEL);
        scroller.setBorder(null);
        return scroller;
    }

    private void selectObject(Object obj)
    {
        selected = obj;
        list.setSelectedIndex(backing.indexOf(obj));
        refreshRight();
    }

    private void refreshRight()
    {
        rightPanel.removeAll();

        if (selected instanceof ScriptActionClip sac)
        {
            rightPanel.add(headerLabel("脚本动作 · 自定义参数"));
            rightPanel.add(muted("每个数字可被外部工具/社区读取，可单独导出为文件分享。"));
            rightPanel.add(Box.createVerticalStrut(8));

            JPanel addRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
            addRow.setOpaque(false);
            JButton addP = new JButton("+ 添加数字");
            styleSmallBtn(addP);
            addP.addActionListener(e -> mc(() ->
            {
                sac.params.add(new ValueFloat(String.valueOf(sac.params.getAllTyped().size()), 0F));
                refreshRight();
            }));
            addRow.add(addP);
            rightPanel.add(addRow);
            rightPanel.add(Box.createVerticalStrut(6));

            int pi = 0;
            for (ValueFloat v : sac.params.getList())
            {
                final int idx = pi++;
                JPanel row = new JPanel(new BorderLayout(8, 0));
                row.setOpaque(false);
                row.add(muted("参数 " + idx), BorderLayout.WEST);
                JSpinner sp = new JSpinner(new SpinnerNumberModel(v.get().doubleValue(), -1e9, 1e9, 0.1));
                styleSpinner(sp);
                sp.addChangeListener(e -> mc(() -> v.set(((Double) sp.getValue()).floatValue())));
                row.add(sp, BorderLayout.CENTER);
                JButton del = new JButton("删除");
                styleSmallBtn(del);
                del.addActionListener(e -> mc(() ->
                {
                    sac.params.getAllTyped().remove(idx);
                    refreshRight();
                }));
                row.add(del, BorderLayout.EAST);
                rightPanel.add(row);
            }

            rightPanel.add(Box.createVerticalStrut(10));
            JButton export = new JButton("导出为文件 (.json)");
            styleSmallBtn(export);
            export.addActionListener(e ->
            {
                File out = sac.exportScript(Minecraft.getInstance().gameDirectory);
                if (out != null)
                {
                    JOptionPane.showMessageDialog(dlg, "已导出：\n" + out.getAbsolutePath(), "导出成功", JOptionPane.INFORMATION_MESSAGE);
                }
                else
                {
                    JOptionPane.showMessageDialog(dlg, "导出失败（无法写入文件）。", "导出失败", JOptionPane.ERROR_MESSAGE);
                }
            });
            rightPanel.add(export);

            track.setData(List.of(new double[]{0, 0.5}, new double[]{1, 0.5}));
        }
        else if (selected instanceof ActionClip ac)
        {
            rightPanel.add(headerLabel("动作时间轴"));
            rightPanel.add(track);
            rightPanel.add(Box.createVerticalStrut(10));

            rightPanel.add(numberRow("每组帧数 (frequency)", ac.frequency.get(), v -> mc(() -> ac.frequency.set(v))));
            rightPanel.add(numberRow("时长 (duration)", ac.duration.get(), v -> mc(() -> ac.duration.set(v))));

            if (ac instanceof LocomotionActionClip loc)
            {
                JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
                row.setOpaque(false);
                row.add(muted("模式"));
                JComboBox<String> mode = new JComboBox<>(new String[]{"walk", "run", "idle"});
                mode.setSelectedItem(loc.mode.get());
                styleCombo(mode);
                mode.addActionListener(e -> mc(() -> loc.mode.set((String) mode.getSelectedItem())));
                row.add(mode);
                row.add(muted("(走路/奔跑/空闲)"));
                rightPanel.add(row);
                rightPanel.add(numberRow("步长 (每步方块)", (int) (loc.step.get() * 10), v -> mc(() -> loc.step.set(v / 10F))));
            }

            if (!(ac instanceof ScriptActionClip))
            {
                rightPanel.add(Box.createVerticalStrut(6));
                rightPanel.add(muted("提示：从左侧列表把动作拖到上方时间轴，即可设置它的起始帧。"));
            }

            track.setData(List.of(new double[]{0, 0.5}, new double[]{1, 0.5}));
        }
        else if (selected instanceof KeyframeChannel)
        {
            @SuppressWarnings("unchecked")
            KeyframeChannel<Double> ch = (KeyframeChannel<Double>) selected;
            rightPanel.add(headerLabel("关键帧通道：" + ch.getId()));
            rightPanel.add(track);
            rightPanel.add(Box.createVerticalStrut(10));

            JPanel addRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
            addRow.setOpaque(false);
            JSpinner tick = new JSpinner(new SpinnerNumberModel(0, 0, 100000, 1));
            JSpinner val = new JSpinner(new SpinnerNumberModel(0.0, -1000.0, 1000.0, 0.1));
            styleSpinner(tick);
            styleSpinner(val);
            addRow.add(muted("帧"));
            addRow.add(tick);
            addRow.add(muted("值"));
            addRow.add(val);
            JButton add = new JButton("添加关键帧");
            styleSmallBtn(add);
            add.addActionListener(e -> mc(() ->
            {
                ch.insert((float) (int) tick.getValue(), (Double) val.getValue());
            }));
            addRow.add(add);
            rightPanel.add(addRow);

            rightPanel.add(Box.createVerticalStrut(8));
            rightPanel.add(muted("已有关键帧（点击删除）："));
            List<Keyframe<Double>> kfs = ch.getKeyframes();
            List<double[]> marks = new ArrayList<>();
            int maxTick = 1;
            for (Keyframe<Double> kf : kfs)
            {
                maxTick = Math.max(maxTick, (int) kf.getTick());
            }
            for (Keyframe<Double> kf : kfs)
            {
                final Keyframe<Double> fk = kf;
                JPanel kr = new JPanel(new BorderLayout(8, 0));
                kr.setOpaque(false);
                kr.add(muted("帧 " + (int) fk.getTick() + " = " + fk.getValue()), BorderLayout.WEST);
                JButton del = new JButton("删除");
                styleSmallBtn(del);
                del.addActionListener(e -> mc(() -> ch.getKeyframes().remove(fk)));
                kr.add(del, BorderLayout.EAST);
                rightPanel.add(kr);
                marks.add(new double[]{(int) fk.getTick() / (double) maxTick, 0.5});
            }
            track.setData(marks);
        }
        else
        {
            rightPanel.add(headerLabel("请从左侧选择一个动作或身体通道"));
            rightPanel.add(muted("动作角色：选「动作」模式拖动作进时间轴，或切「路径」模式编辑行走路径关键帧。"));
        }

        rightPanel.revalidate();
        rightPanel.repaint();
    }

    /* ---------------- refresh ---------------- */

    private void refresh()
    {
        boolean action = "action".equals(replay.characterType.get());
        for (int i = 0; i < 2; i++)
        {
            boolean on = (i == 0) == !action; /* 0=keyframe, 1=action */
            typeBtns[i].setBackground(on ? ACCENT : PANEL);
            typeBtns[i].setForeground(on ? Color.WHITE : TEXT);
            typeBtns[i].setBorder(new LineBorder(on ? ACCENT.darker() : BORDER, 1, true));
        }
        nameLabel.setText(replay.getName());
        modeRow.setVisible(action);

        listModel.clear();
        backing.clear();

        if (action)
        {
            if ("path".equals(leftMode))
            {
                /* Walking-path keyframe channels. */
                List<KeyframeChannel<Double>> path = java.util.Arrays.asList(
                    replay.keyframes.x, replay.keyframes.y, replay.keyframes.z, replay.keyframes.yaw);
                for (KeyframeChannel<Double> ch : path)
                {
                    listModel.addElement(ch.getId() + "  (" + ch.getKeyframes().size() + " 帧)");
                    backing.add(ch);
                }
            }
            else
            {
                for (Clip clip : replay.actions.get())
                {
                    String t = clip.title.get();
                    listModel.addElement((t.isEmpty() ? "(未命名动作)" : t) + "  [" + clip.tick.get() + "→" + (clip.tick.get() + clip.duration.get()) + "]");
                    backing.add(clip);
                }
            }
        }
        else
        {
            for (KeyframeChannel<?> ch : replay.keyframes.getChannels())
            {
                if (ch instanceof KeyframeChannel && isDoubleChannel(ch))
                {
                    @SuppressWarnings("unchecked")
                    KeyframeChannel<Double> dch = (KeyframeChannel<Double>) ch;
                    listModel.addElement(ch.getId() + "  (" + dch.getKeyframes().size() + " 帧)");
                    backing.add(dch);
                }
            }
        }

        refreshRight();
    }

    private boolean isDoubleChannel(KeyframeChannel<?> ch)
    {
        return ch instanceof KeyframeChannel && Double.class.equals(typeArg(ch));
    }

    private static Class<?> typeArg(KeyframeChannel<?> ch)
    {
        try
        {
            return ch.getKeyframes().isEmpty() ? Double.class : ch.getKeyframes().get(0).getValue().getClass();
        }
        catch (Throwable t)
        {
            return Double.class;
        }
    }

    /* ---------------- drag & drop (list -> timeline) ---------------- */

    private class ActionListTransferHandler extends TransferHandler
    {
        @Override
        protected Transferable createTransferable(JComponent c)
        {
            int i = list.getSelectedIndex();
            if (i < 0 || i >= backing.size() || !(backing.get(i) instanceof ActionClip))
            {
                return null;
            }
            return new StringSelection("act:" + i);
        }

        @Override
        public int getSourceActions(JComponent c)
        {
            return COPY;
        }
    }

    private class TrackDropHandler extends TransferHandler
    {
        @Override
        public boolean canImport(TransferSupport support)
        {
            return support.isDataFlavorSupported(DataFlavor.stringFlavor)
                && "action".equals(replay.characterType.get())
                && !"path".equals(leftMode);
        }

        @Override
        public boolean importData(TransferSupport support)
        {
            if (!canImport(support))
            {
                return false;
            }

            try
            {
                String data = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
                if (data == null || !data.startsWith("act:"))
                {
                    return false;
                }

                int i = Integer.parseInt(data.substring(4));
                if (i < 0 || i >= backing.size() || !(backing.get(i) instanceof ActionClip ac))
                {
                    return false;
                }

                int maxTick = track.getMaxTick();
                int x = support.getDropLocation().getDropPoint().x;
                int tick = (int) Math.round(((double) x / Math.max(1, track.getWidth())) * maxTick);
                tick = Math.max(0, Math.min(tick, maxTick));

                int finalTick = tick;
                mc(() -> ac.tick.set(finalTick));
                selectObject(ac);
                return true;
            }
            catch (UnsupportedFlavorException | IOException ex)
            {
                return false;
            }
        }
    }

    /* ---------------- tiny swing helpers ---------------- */

    private JLabel muted(String t)
    {
        JLabel l = new JLabel(t);
        l.setForeground(MUTED);
        l.setBorder(new EmptyBorder(2, 4, 2, 4));
        return l;
    }

    private JLabel headerLabel(String t)
    {
        JLabel l = new JLabel(t);
        l.setForeground(TEXT);
        l.setFont(l.getFont().deriveFont(Font.BOLD));
        l.setBorder(new EmptyBorder(2, 0, 8, 0));
        return l;
    }

    private JMenuItem labelItem(String t)
    {
        JMenuItem it = new JMenuItem(t);
        it.setBackground(PANEL);
        it.setForeground(ACCENT);
        it.setEnabled(false);
        return it;
    }

    private JMenuItem sep()
    {
        JMenuItem it = new JMenuItem("");
        it.setEnabled(false);
        return it;
    }

    private JPanel numberRow(String label, int value, java.util.function.IntConsumer onChange)
    {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        row.setOpaque(false);
        row.add(muted(label));
        JSpinner sp = new JSpinner(new SpinnerNumberModel(value, 0, 100000, 1));
        styleSpinner(sp);
        sp.addChangeListener(e -> onChange.accept((Integer) sp.getValue()));
        row.add(sp);
        return row;
    }

    private void styleSpinner(JSpinner sp)
    {
        sp.setBackground(FIELD);
        sp.setForeground(TEXT);
        ((JComponent) sp.getEditor()).setBackground(FIELD);
        ((JTextField) sp.getEditor().getComponent(0)).setForeground(TEXT);
        ((JTextField) sp.getEditor().getComponent(0)).setCaretColor(TEXT);
    }

    private void styleCombo(JComboBox<?> cb)
    {
        cb.setBackground(FIELD);
        cb.setForeground(TEXT);
    }

    private void styleSmallBtn(JButton b)
    {
        b.setFocusPainted(false);
        b.setBackground(PANEL);
        b.setForeground(TEXT);
        b.setBorder(new LineBorder(BORDER, 1, true));
        b.setPreferredSize(new Dimension(120, 28));
    }

    private void mc(Runnable r)
    {
        Minecraft.getInstance().execute(r);
    }

    /* ---------------- track canvas ---------------- */

    private static class TrackCanvas extends JPanel
    {
        private List<double[]> marks = new ArrayList<>();
        private int maxTick = 300;

        void setData(List<double[]> m)
        {
            this.marks = m == null ? new ArrayList<>() : m;
            repaint();
        }

        int getMaxTick()
        {
            return maxTick;
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(FIELD);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(BORDER);
            g2.drawLine(0, getHeight() / 2, getWidth(), getHeight() / 2);
            g2.setColor(ACCENT);
            for (double[] m : marks)
            {
                int x = (int) (m[0] * getWidth());
                int y = (int) (m[1] * getHeight());
                g2.fillOval(x - 5, y - 5, 10, 10);
            }
        }
    }
}
