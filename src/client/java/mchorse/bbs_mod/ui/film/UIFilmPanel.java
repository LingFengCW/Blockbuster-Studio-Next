package mchorse.bbs_mod.ui.film;

import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.actions.ActionState;
import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.camera.clips.modifiers.TranslateClip;
import mchorse.bbs_mod.camera.clips.overwrite.IdleClip;
import mchorse.bbs_mod.camera.controller.CameraController;
import mchorse.bbs_mod.camera.controller.RunnerCameraController;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.renderer.MorphRenderer;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.Recorder;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.network.ClientNetwork;
import mchorse.bbs_mod.projects.ProjectManager;
import mchorse.bbs_mod.settings.values.IValueListener;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.settings.values.ui.ValueEditorLayout;
import mchorse.bbs_mod.ui.ContentType;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.dashboard.panels.IFlightSupported;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanels;
import mchorse.bbs_mod.ui.dashboard.panels.UIDataDashboardPanel;
import mchorse.bbs_mod.ui.dashboard.panels.overlay.UICRUDOverlayPanel;
import mchorse.bbs_mod.ui.dashboard.utils.IUIOrbitKeysHandler;
import mchorse.bbs_mod.ui.film.audio.UIAudioRecorder;
import mchorse.bbs_mod.ui.film.controller.UIFilmController;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditor;
import mchorse.bbs_mod.ui.film.utils.UIFilmUndoHandler;
import mchorse.bbs_mod.ui.film.utils.undo.UIUndoHistoryOverlay;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.context.UISimpleContextMenu;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIMessageOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UINumberOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIPromptOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.utils.UIDraggable;
import mchorse.bbs_mod.ui.framework.elements.utils.UIRenderable;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.context.ContextMenuManager;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.CollectionUtils;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.PlayerUtils;
import mchorse.bbs_mod.utils.Timer;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.Clips;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.joml.Vectors;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.KeyframeSegment;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector2i;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class UIFilmPanel extends UIDataDashboardPanel<Film> implements IFlightSupported, IUIOrbitKeysHandler, ICursor
{
    private RunnerCameraController runner;
    private boolean lastRunning;
    private final Position position = new Position(0, 0, 0, 0, 0);
    private final Position lastPosition = new Position(0, 0, 0, 0, 0);

    public UIElement main;
    public UIElement editArea;
    public UIDraggable draggableMain;
    public UIFilmRecorder recorder;
    public UIFilmPreview preview;
    public UIAssetBin assetBin;

    /* HTML (Ultralight) editor overlay. */
    public lingfeng.bbsnext.mcef.UIOverlay ultralightOverlay;

    public UIIcon duplicateFilm;

    /* Main editors */
    public UIClipsPanel cameraEditor;
    public UIReplaysEditor replayEditor;
    public UIClipsPanel actionEditor;

    /* Icon bar buttons */
    public UIIcon openHistory;
    public UIIcon toggleHorizontal;
    public UIIcon openCameraEditor;
    public UIIcon openReplayEditor;
    public UIIcon openActionEditor;

    private Camera camera = new Camera();
    private boolean entered;
    public boolean playerToCamera;

    /* Entity control */
    private UIFilmController controller = new UIFilmController(this);
    private UIFilmUndoHandler undoHandler;

    /* PR-style top bar (design doc 5 / 11): title bar + main menu bar. */
    private UIElement titleBar;
    private UIElement menuBar;
    private final List<UIButton> menuButtons = new ArrayList<>();
    private int savedUndoIndex = -1;

    public final Matrix4f lastView = new Matrix4f();
    public final Matrix4f lastProjection = new Matrix4f();

    private Timer flightEditTime = new Timer(100);

    private List<UIElement> panels = new ArrayList<>();
    private UIElement secretPlay;

    private boolean newFilm;

    /**
     * Initialize the camera editor with a camera profile.
     */
    public UIFilmPanel(UIDashboard dashboard)
    {
        super(dashboard);

        this.runner = new RunnerCameraController(this, (playing) ->
        {
            this.notifyServer(playing ? ActionState.PLAY : ActionState.PAUSE);
        });
        this.runner.getContext().captureSnapshots();

        this.recorder = new UIFilmRecorder(this);

        this.main = new UIElement();
        this.editArea = new UIElement();
        this.preview = new UIFilmPreview(this);

        this.draggableMain = new UIDraggable((context) ->
        {
            ValueEditorLayout layout = BBSSettings.editorLayoutSettings;

            if (layout.isHorizontal())
            {
                layout.setMainSizeH(1F - (context.mouseY - this.editor.area.y) / (float) this.editor.area.h);
                layout.setEditorSizeH(1F - (context.mouseX - this.editor.area.x) / (float) this.editor.area.w);
            }
            else
            {
                layout.setMainSizeV((context.mouseX - this.editor.area.x) / (float) this.editor.area.w);
                layout.setEditorSizeV((context.mouseY - this.editor.area.y) / (float) this.editor.area.h);
            }

            this.setupEditorFlex(true);
        });

        this.draggableMain.reference(() ->
        {
            return BBSSettings.editorLayoutSettings.isHorizontal()
                ? new Vector2i(this.editArea.area.x, this.editArea.area.ey())
                : new Vector2i(this.editArea.area.x, this.editArea.area.y);
        });
        this.draggableMain.rendering((context) ->
        {
            int size = 5;

            if (BBSSettings.editorLayoutSettings.isHorizontal())
            {
                int x = this.editArea.area.x + 3;
                int y = this.editArea.area.ey() - 3;

                context.batcher.box(x, y - size, x + 1, y, Colors.WHITE);
                context.batcher.box(x, y - 1, x + size, y, Colors.WHITE);

                x = this.editArea.area.x - 3;
                y = this.editArea.area.ey() - 3;

                context.batcher.box(x - 1, y - size, x, y, Colors.WHITE);
                context.batcher.box(x - size, y - 1, x, y, Colors.WHITE);
            }
            else
            {
                int x = this.editArea.area.x + 3;
                int y = this.editArea.area.y - 3;

                context.batcher.box(x, y - size, x + 1, y, Colors.WHITE);
                context.batcher.box(x, y - 1, x + size, y, Colors.WHITE);

                x = this.editArea.area.x + 3;
                y = this.editArea.area.y + 3;

                context.batcher.box(x, y, x + 1, y + size, Colors.WHITE);
                context.batcher.box(x, y, x + size, y + 1, Colors.WHITE);
            }
        });

        /* Editors */
        this.cameraEditor = new UIClipsPanel(this, BBSMod.getFactoryCameraClips()).target(this.editArea);
        this.cameraEditor.full(this.main);

        this.cameraEditor.clips.context((menu) ->
        {
            UIAudioRecorder.addOption(this, menu);
        });

        this.replayEditor = new UIReplaysEditor(this);
        this.replayEditor.full(this.main).setVisible(false);
        this.actionEditor = new UIClipsPanel(this, BBSMod.getFactoryActionClips()).target(this.editArea);
        this.actionEditor.full(this.main).setVisible(false);

        /* Icon bar buttons */
        this.openHistory = new UIIcon(Icons.LIST, (b) ->
        {
            UIOverlay.addOverlay(this.getContext(), new UIUndoHistoryOverlay(this), 200, 0.6F);
        });
        this.openHistory.tooltip(UIKeys.FILM_OPEN_HISTORY, Direction.LEFT);
        this.toggleHorizontal = new UIIcon(() -> BBSSettings.editorLayoutSettings.isHorizontal() ? Icons.EXCHANGE : Icons.CONVERT, (b) ->
        {
            BBSSettings.editorLayoutSettings.setHorizontal(!BBSSettings.editorLayoutSettings.isHorizontal());

            this.setupEditorFlex(true);
        });
        this.toggleHorizontal.tooltip(UIKeys.FILM_TOGGLE_LAYOUT, Direction.LEFT);
        this.openCameraEditor = new UIIcon(Icons.FRUSTUM, (b) -> this.showPanel(this.cameraEditor));
        this.openCameraEditor.tooltip(UIKeys.FILM_OPEN_CAMERA_EDITOR, Direction.LEFT);
        this.openReplayEditor = new UIIcon(Icons.SCENE, (b) -> this.showPanel(this.replayEditor));
        this.openReplayEditor.tooltip(UIKeys.FILM_OPEN_REPLAY_EDITOR, Direction.LEFT);
        this.openActionEditor = new UIIcon(Icons.ACTION, (b) -> this.showPanel(this.actionEditor));
        this.openActionEditor.tooltip(UIKeys.FILM_OPEN_ACTION_EDITOR, Direction.LEFT);

        /* Setup elements */
        this.iconBar.add(this.openHistory, this.toggleHorizontal.marginTop(9), this.openCameraEditor.marginTop(9), this.openReplayEditor, this.openActionEditor);

        this.editor.add(this.main, new UIRenderable(this::renderIcons));
        this.main.add(this.cameraEditor, this.replayEditor, this.actionEditor, this.editArea, this.preview, this.draggableMain);
        this.add(this.controller, new UIRenderable(this::renderDividers));
        this.overlay.namesList.setFileIcon(Icons.FILM);

        /* PR-style asset bin pinned to the left 25% of the editor. */
        this.assetBin = new UIAssetBin(this);
        this.assetBin.relative(this.editor).x(0).y(0).w(0.25F).h(1F);
        this.editor.add(this.assetBin);

        /* Right click on the editor's empty space: create scene/character
         * or play with the replay guard. */
        this.context(m ->
        {
            m.action(Icons.SCENE, UIKeys.ASSETS_NEW_SCENE, this::newScene);
            m.action(Icons.PLAYER, UIKeys.ASSETS_NEW_CHARACTER, this::newCharacter);
            m.action(Icons.PLAY, UIKeys.CAMERA_EDITOR_KEYS_EDITOR_PLAY_FILM, this::playWithGuard);
        });

        /* Register keybinds */
        IKey modes = UIKeys.CAMERA_EDITOR_KEYS_MODES_TITLE;
        IKey editor = UIKeys.CAMERA_EDITOR_KEYS_EDITOR_TITLE;
        IKey looping = UIKeys.CAMERA_EDITOR_KEYS_LOOPING_TITLE;
        Supplier<Boolean> active = () -> !this.isFlying();

        this.keys().register(Keys.PLAUSE, () -> this.preview.plause.clickItself()).active(active).category(editor);
        this.keys().register(Keys.NEXT_CLIP, () -> this.setCursor(this.data.camera.findNextTick(this.getCursor()))).active(active).category(editor);
        this.keys().register(Keys.PREV_CLIP, () -> this.setCursor(this.data.camera.findPreviousTick(this.getCursor()))).active(active).category(editor);
        this.keys().register(Keys.NEXT, () -> this.setCursor(this.getCursor() + 1)).active(active).category(editor);
        this.keys().register(Keys.PREV, () -> this.setCursor(this.getCursor() - 1)).active(active).category(editor);
        this.keys().register(Keys.UNDO, this::undo).category(editor);
        this.keys().register(Keys.REDO, this::redo).category(editor);
        this.keys().register(Keys.FLIGHT, this::toggleFlight).active(() -> this.data != null).category(modes);
        this.keys().register(Keys.LOOPING, () ->
        {
            BBSSettings.editorLoop.set(!BBSSettings.editorLoop.get());
            this.getContext().notifyInfo(UIKeys.CAMERA_EDITOR_KEYS_LOOPING_TOGGLE_NOTIFICATION);
        }).active(active).category(looping);
        this.keys().register(Keys.LOOPING_SET_MIN, () -> this.cameraEditor.clips.setLoopMin()).active(active).category(looping);
        this.keys().register(Keys.LOOPING_SET_MAX, () -> this.cameraEditor.clips.setLoopMax()).active(active).category(looping);
        this.keys().register(Keys.JUMP_FORWARD, () -> this.setCursor(this.getCursor() + BBSSettings.editorJump.get())).active(active).category(editor);
        this.keys().register(Keys.JUMP_BACKWARD, () -> this.setCursor(this.getCursor() - BBSSettings.editorJump.get())).active(active).category(editor);
        this.keys().register(Keys.FILM_CONTROLLER_CYCLE_EDITORS, () ->
        {
            this.showPanel(MathUtils.cycler(this.getPanelIndex() + (Window.isShiftPressed() ? -1 : 1), this.panels));
            UIUtils.playClick();
        }).category(editor);

        this.openOverlay.context((menu) ->
        {
            if (this.data == null)
            {
                return;
            }

            menu.action(Icons.ARROW_RIGHT, UIKeys.FILM_MOVE_TITLE, () ->
            {
                UIFilmMoveOverlayPanel panel = new UIFilmMoveOverlayPanel((vector) ->
                {
                    int topLayer = this.data.camera.getTopLayer() + 1;
                    int duration = this.data.camera.calculateDuration();
                    double dx = vector.x;
                    double dy = vector.y;
                    double dz = vector.z;

                    BaseValue.edit(this.data, (__) ->
                    {
                        TranslateClip clip = new TranslateClip();

                        clip.layer.set(topLayer);
                        clip.duration.set(duration);
                        clip.translate.get().set(dx, dy, dz);
                        __.camera.addClip(clip);

                        for (Replay replay : __.replays.getList())
                        {
                            for (Keyframe<Double> keyframe : replay.keyframes.x.getKeyframes()) keyframe.setValue(keyframe.getValue() + dx);
                            for (Keyframe<Double> keyframe : replay.keyframes.y.getKeyframes()) keyframe.setValue(keyframe.getValue() + dy);
                            for (Keyframe<Double> keyframe : replay.keyframes.z.getKeyframes()) keyframe.setValue(keyframe.getValue() + dz);

                            replay.actions.shift(dx, dy, dz);
                        }
                    });
                });

                UIOverlay.addOverlay(this.getContext(), panel, 200, 0.9F);
            });

            menu.action(Icons.TIME, UIKeys.FILM_INSERT_SPACE_TITLE, () ->
            {
                UINumberOverlayPanel panel = new UINumberOverlayPanel(UIKeys.FILM_INSERT_SPACE_TITLE, UIKeys.FILM_INSERT_SPACE_DESCRIPTION, (d) ->
                {
                    if (d.intValue() <= 0)
                    {
                        return;
                    }

                    for (Replay replay : this.data.replays.getList())
                    {
                        for (KeyframeChannel<?> channel : replay.keyframes.getChannels())
                        {
                            channel.insertSpace(this.getCursor(), d.intValue());
                        }

                        for (KeyframeChannel channel : replay.properties.properties.values())
                        {
                            channel.insertSpace(this.getCursor(), d.intValue());
                        }
                    }
                });

                panel.value.limit(1).integer().setValue(1D);

                UIOverlay.addOverlay(this.getContext(), panel);
            });

            menu.action(Icons.LINE, UIKeys.FILM_REPLACE_INVENTORY, () ->
            {
                BaseValue.edit(this.getData().inventory, (inv) -> inv.fromPlayer(Minecraft.getInstance().player));
            });
        });

        this.fill(null);

        this.setupEditorFlex(false);
        this.flightEditTime.mark();

        this.panels.add(this.cameraEditor);
        this.panels.add(this.replayEditor);
        this.panels.add(this.actionEditor);

        this.secretPlay = new UIElement();
        this.secretPlay.keys().register(Keys.PLAUSE, () -> this.preview.plause.clickItself()).active(() -> !this.isFlying() && !this.canBeSeen() && this.data != null).category(editor);

        this.setUndoId("film_panel");
        this.cameraEditor.setUndoId("camera_editor");
        this.replayEditor.setUndoId("replay_editor");
        this.actionEditor.setUndoId("action_editor");

        UIElement element = new UIElement()
        {
            @Override
            protected boolean subMouseScrolled(UIContext context)
            {
                if (Window.isCtrlPressed() && !UIFilmPanel.this.isFlying())
                {
                    int magnitude = Window.isShiftPressed() ? BBSSettings.editorJump.get() : 1;
                    int newCursor = UIFilmPanel.this.getCursor() + (int) Math.copySign(magnitude, context.mouseWheel);

                    UIFilmPanel.this.setCursor(newCursor);

                    return true;
                }

                return super.subMouseScrolled(context);
            }
        };

        this.add(element);

        /* PR-style top bar: title bar (with dirty *) + main menu bar. */
        this.setupTopBar();

        /* Shift the whole editor area down below the top bar. Simple pixel
         * flex (panel width - 20px right icon bar) - no cross-resizer
         * dependencies that could leave the area at 0. */
        this.editor.resetFlex().relative(this).x(0).y(58).w(1F, -20).h(1F, -58);

        /* HTML (MCEF) editor overlay - full screen, replaces the entire
         * native editor UI while visible (the HTML page background is
         * opaque, except for the transparent viewport area that shows the
         * world preview underneath). */
        this.ultralightOverlay = new lingfeng.bbsnext.mcef.UIOverlay(this);
        this.ultralightOverlay.relative(this).x(0).y(0).w(1F).h(1F);
        this.ultralightOverlay.setVisible(false);
        this.add(this.ultralightOverlay);
    }

    /** Toggle the HTML (Ultralight) editor overlay. */
    public void toggleUltralight()
    {
        boolean show = !this.ultralightOverlay.isVisible();

        this.ultralightOverlay.setVisible(show);

        if (show && this.ultralightOverlay.area.w > 0 && this.ultralightOverlay.area.h > 0)
        {
            lingfeng.bbsnext.mcef.MCEFUI.createBrowser(this.ultralightOverlay.area.w, this.ultralightOverlay.area.h, new lingfeng.bbsnext.mcef.EditorBridge(this));
        }
    }

    /* ======== PR-style top bar: title bar + main menu bar ======== */

    private void setupTopBar()
    {
        /* ---- Title bar (project name - *work name) ---- */
        this.titleBar = new UIElement();
        this.titleBar.relative(this).x(0).y(0).w(1F, -20).h(30);

        this.titleBar.add(new UIRenderable(this::renderTitleBar));

        /* ---- Main menu bar (file / edit / view / ... ) ---- */
        this.menuBar = new UIElement();
        this.menuBar.relative(this).x(0).y(30).w(1F, -20).h(28);

        this.menuBar.add(new UIRenderable((context) ->
        {
            context.batcher.box(0, 0, this.menuBar.area.w, this.menuBar.area.h, Colors.A75);
        }));

        String[] menus = { "文件", "编辑", "视图", "轨道", "关键帧", "录制", "渲染", "窗口", "设置" };
        int x = 0;

        for (String name : menus)
        {
            UIButton btn = new UIButton(IKey.raw(name), (b) -> this.openMenu(b));

            btn.relative(this.menuBar).x(x).y(0).w(64).h(28);
            btn.textColor(Colors.LIGHTER_GRAY, true);
            this.menuBar.add(btn);
            this.menuButtons.add(btn);
            x += 64;
        }

        this.add(this.titleBar, this.menuBar);
    }

    private void renderTitleBar(UIContext context)
    {
        int w = this.titleBar.area.w;

        context.batcher.box(0, 0, w, this.titleBar.area.h, Colors.CONTROL_BAR);

        String projectName = "未命名工程";
        mchorse.bbs_mod.projects.ProjectManager projects = mchorse.bbs_mod.projects.ProjectManager.get();

        if (projects != null && projects.getCurrent() != null)
        {
            projectName = projects.getCurrent().name;
        }

        String workName = this.data == null ? "未打开作品" : String.valueOf(this.data.getId());
        String title = projectName + " - " + (this.isDirty() ? "*" : "") + workName;

        context.batcher.text(title, 12, 7, this.isDirty() ? Colors.ORANGE : Colors.LIGHTER_GRAY, true);
    }

    /** Whether the current film has unsaved edits since the last save/load. */
    public boolean isDirty()
    {
        return this.data != null && this.undoHandler != null
            && this.undoHandler.getUndoManager().getCurrentUndoIndex() != this.savedUndoIndex;
    }

    /** Open the main menu of the clicked menu-bar button. */
    private void openMenu(UIButton button)
    {
        ContextMenuManager manager = new ContextMenuManager();
        String name = button.label.get();

        this.buildMenu(manager, name);

        UISimpleContextMenu menu = manager.create();

        if (menu != null)
        {
            UIContext ctx = button.getContext();

            if (ctx != null)
            {
                menu.setMouse(ctx);
                ctx.menu.overlay.add(menu);
            }
        }
    }

    /** Fill the 9 top menus with real actions (design doc 11). */
    private void buildMenu(ContextMenuManager m, String name)
    {
        if (this.data != null)
        {
            if (name.equals("文件"))
            {
                m.action(Icons.ADD, IKey.raw("新建场景"), this::newScene);
                m.action(Icons.SAVED, IKey.raw("保存"), this::forceSave);
                m.action(Icons.FILM, IKey.raw("导出当前场景"), () ->
                {
                    mchorse.bbs_mod.projects.SceneManager scenes = mchorse.bbs_mod.projects.SceneManager.get();
                    mchorse.bbs_mod.projects.Scene scene = scenes == null ? null : scenes.getCurrent();

                    if (scene != null && scenes.exportScene(scene) != null)
                    {
                        this.getContext().notifySuccess(IKey.raw("已导出: " + scene.name));
                    }
                    else
                    {
                        this.getContext().notifyError(IKey.raw("导出失败"));
                    }
                });
                m.action(Icons.TRASH, IKey.raw("关闭当前作品"), this::disableContext);
            }
            else if (name.equals("编辑"))
            {
                m.action(Icons.EDIT, IKey.raw("撤销"), this::undo);
                m.action(Icons.DUPE, IKey.raw("重做"), this::redo);
            }
            else if (name.equals("视图"))
            {
                m.action(Icons.SQUARE, IKey.raw("中心线"), BBSSettings.editorCenterLines.get(), () ->
                {
                    BBSSettings.editorCenterLines.set(!BBSSettings.editorCenterLines.get());
                });
                m.action(Icons.LINE, IKey.raw("三分构图线"), BBSSettings.editorRuleOfThirds.get(), () ->
                {
                    BBSSettings.editorRuleOfThirds.set(!BBSSettings.editorRuleOfThirds.get());
                });
                m.action(Icons.CURSOR, IKey.raw("准星"), BBSSettings.editorCrosshair.get(), () ->
                {
                    BBSSettings.editorCrosshair.set(!BBSSettings.editorCrosshair.get());
                });
                m.action(Icons.REFRESH, IKey.raw("重置视角"), () -> this.dashboard.orbit.setup(this.getCamera()));
            }
            else if (name.equals("轨道"))
            {
                m.action(Icons.FRUSTUM, IKey.raw("打开相机时间轴"), () -> this.showPanel(this.cameraEditor));
                m.action(Icons.SCENE, IKey.raw("打开回放编辑器"), () -> this.showPanel(this.replayEditor));
            }
            else if (name.equals("关键帧"))
            {
                m.action(Icons.ARROW_LEFT, IKey.raw("上一帧"), () -> this.setCursor(this.getCursor() - 1));
                m.action(Icons.ARROW_RIGHT, IKey.raw("下一帧"), () -> this.setCursor(this.getCursor() + 1));
                m.action(Icons.CURSOR, IKey.raw("跳转到开头"), () -> this.setCursor(0));
            }
            else if (name.equals("录制"))
            {
                m.action(Icons.PLANE, IKey.raw("切换飞行/录制模式"), this::toggleFlight);
                m.action(Icons.SPHERE, IKey.raw("录制回放"), () -> this.preview.recordReplay.clickItself());
                m.action(Icons.VIDEO_CAMERA, IKey.raw("录制视频"), () -> this.preview.recordVideo.clickItself());
            }
            else if (name.equals("渲染"))
            {
                m.action(Icons.CAMERA, IKey.raw("截图导出"), () -> this.preview.recordVideo.clickItself());
            }
            else if (name.equals("窗口"))
            {
                m.action(Icons.FRUSTUM, IKey.raw("相机时间轴"), () -> this.showPanel(this.cameraEditor));
                m.action(Icons.SCENE, IKey.raw("回放编辑器"), () -> this.showPanel(this.replayEditor));
                m.action(Icons.ACTION, IKey.raw("动作编辑器"), () -> this.showPanel(this.actionEditor));
                m.action(Icons.EDITOR, IKey.raw("HTML 界面 (Ultralight)"), this::toggleUltralight);
            }
            else if (name.equals("设置"))
            {
                m.action(Icons.SETTINGS, IKey.raw("打开设置"), () -> this.dashboard.settings.clickItself());
            }
        }
        else
        {
            m.action(Icons.ADD, IKey.raw("新建场景"), this::newScene);
        }
    }

    /** Height of the bottom timeline dock (design doc 5: bottom timeline). */
    private static final int TIMELINE_H = 240;

    private void setupEditorFlex(boolean resize)
    {
        this.main.resetFlex();
        this.editArea.resetFlex();
        this.preview.resetFlex();
        this.draggableMain.resetFlex();

        /* Fixed PR-style layout: left 25% asset bin (already anchored in
         * the constructor), right 75% = preview viewport on top with the
         * timeline docked at the bottom spanning the whole right side. */
        this.main.relative(this.editor).x(0.25F).y(1F, -TIMELINE_H).w(0.75F).h(TIMELINE_H);
        this.preview.relative(this.editor).x(0.25F).y(0).w(0.75F).h(1F, -TIMELINE_H);
        this.editArea.relative(this.preview).x(1F).y(0).w(0).h(0);
        this.draggableMain.hoverOnly().relative(this.main).x(0).y(-3).w(1F).h(6);

        if (resize)
        {
            this.resize();
            this.resize();
        }
    }

    public void pickClip(Clip clip, UIClipsPanel panel)
    {
        if (panel == this.cameraEditor)
        {
            this.setFlight(false);
        }
    }

    public int getPanelIndex()
    {
        for (int i = 0; i < this.panels.size(); i++)
        {
            if (this.panels.get(i).isVisible())
            {
                return i;
            }
        }

        return -1;
    }

    public void showPanel(int index)
    {
        this.showPanel(this.panels.get(index));
    }

    public void showPanel(UIElement element)
    {
        this.cameraEditor.setVisible(false);
        this.replayEditor.setVisible(false);
        this.actionEditor.setVisible(false);

        element.setVisible(true);

        if (this.isFlying())
        {
            this.toggleFlight();
        }
    }

    public UIFilmController getController()
    {
        return this.controller;
    }

    public UIFilmUndoHandler getUndoHandler()
    {
        return this.undoHandler;
    }

    public RunnerCameraController getRunner()
    {
        return this.runner;
    }

    @Override
    protected UICRUDOverlayPanel createOverlayPanel()
    {
        UICRUDOverlayPanel crudPanel = super.createOverlayPanel();

        this.duplicateFilm = new UIIcon(Icons.SCENE, (b) ->
        {
            UIPromptOverlayPanel panel = new UIPromptOverlayPanel(
                UIKeys.GENERAL_DUPE,
                UIKeys.PANELS_MODALS_DUPE,
                (str) -> this.dupeData(crudPanel.namesList.getPath(str).toString())
            );

            panel.text.setText(crudPanel.namesList.getCurrentFirst().getLast());
            panel.text.filename();

            UIOverlay.addOverlay(this.getContext(), panel);
        });

        crudPanel.icons.add(this.duplicateFilm);

        return crudPanel;
    }

    private void dupeData(String name)
    {
        if (this.getData() != null && !this.overlay.namesList.hasInHierarchy(name))
        {
            this.save();
            this.overlay.namesList.addFile(name);

            Film data = new Film();
            Position position = new Position();
            IdleClip idle = new IdleClip();
            int tick = this.getCursor();

            position.set(this.getCamera());
            idle.duration.set(BBSSettings.getDefaultDuration());
            idle.position.set(position);
            data.camera.addClip(idle);
            data.setId(name);

            for (Replay replay : this.data.replays.getList())
            {
                Replay copy = new Replay(replay.getId());

                copy.form.set(FormUtils.copy(replay.form.get()));

                for (KeyframeChannel<?> channel : replay.keyframes.getChannels())
                {
                    if (!channel.isEmpty())
                    {
                        KeyframeChannel newChannel = (KeyframeChannel) copy.keyframes.get(channel.getId());

                        newChannel.insert(0, channel.interpolate(tick));
                    }
                }

                for (Map.Entry<String, KeyframeChannel> entry : replay.properties.properties.entrySet())
                {
                    KeyframeChannel channel = entry.getValue();

                    if (channel.isEmpty())
                    {
                        continue;
                    }

                    KeyframeChannel newChannel = new KeyframeChannel(channel.getId(), channel.getFactory());
                    KeyframeSegment segment = channel.find(tick);

                    if (segment != null)
                    {
                        newChannel.insert(0, segment.createInterpolated());
                    }

                    if (!newChannel.isEmpty())
                    {
                        copy.properties.properties.put(newChannel.getId(), newChannel);
                        copy.properties.add(newChannel);
                    }
                }

                data.replays.add(copy);
            }

            this.fill(data);
            this.save();
        }
    }

    @Override
    public void open()
    {
        super.open();

        Recorder recorder = BBSModClient.getFilms().stopRecording();

        if (recorder == null || recorder.hasNotStarted())
        {
            this.notifyServer(ActionState.RESTART);

            return;
        }

        this.applyRecordedKeyframes(recorder, this.data);
    }

    public void receiveActions(String filmId, int replayId, int tick, BaseType clips)
    {
        Film film = this.data;

        if (film != null && film.getId().equals(filmId) && CollectionUtils.inRange(film.replays.getList(), replayId))
        {
            BaseValue.edit(film.replays.getList().get(replayId), IValueListener.FLAG_UNMERGEABLE, (replay) ->
            {
                Clips newClips = new Clips("", BBSMod.getFactoryActionClips());

                newClips.fromData(clips);
                replay.actions.copyOver(newClips, tick);
            });
        }

        this.save();
    }

    public void applyRecordedKeyframes(Recorder recorder, Film film)
    {
        int replayId = recorder.exception;
        Replay rp = CollectionUtils.getSafe(film.replays.getList(), replayId);

        if (rp != null)
        {
            BaseValue.edit(film, (f) ->
            {
                rp.keyframes.copyOver(recorder.keyframes, 0);

                Form form = rp.form.get();

                if (form != null)
                {
                    for (Map.Entry<String, KeyframeChannel> entry : recorder.properties.properties.entrySet())
                    {
                        KeyframeChannel channel = rp.properties.getOrCreate(form, entry.getKey());

                        if (channel != null && entry.getValue() != null)
                        {
                            channel.copyOver(entry.getValue(), 0);
                        }
                    }
                }

                f.inventory.fromData(recorder.inventory.toData());
                f.hp.set(recorder.hp);
                f.hunger.set(recorder.hunger);
                f.xpLevel.set(recorder.xpLevel);
                f.xpProgress.set(recorder.xpProgress);
            });
        }
    }

    @Override
    public void appear()
    {
        super.appear();

        BBSRendering.setCustomSize(true);
        MorphRenderer.hidePlayer = true;

        CameraController cameraController = this.getCameraController();

        this.fillData();
        this.setFlight(false);
        cameraController.add(this.runner);

        this.getContext().menu.getRoot().add(this.secretPlay);
    }

    @Override
    public void close()
    {
        super.close();

        BBSRendering.setCustomSize(false);
        MorphRenderer.hidePlayer = false;

        CameraController cameraController = this.getCameraController();

        this.cameraEditor.embedView(null);
        this.setFlight(false);
        cameraController.remove(this.runner);

        this.disableContext();
        this.replayEditor.close();

        this.notifyServer(ActionState.STOP);
    }

    @Override
    public void disappear()
    {
        super.disappear();

        BBSRendering.setCustomSize(false);
        MorphRenderer.hidePlayer = false;

        this.setFlight(false);
        this.getCameraController().remove(this.runner);

        this.disableContext();
        this.secretPlay.removeFromParent();

        if (this.ultralightOverlay != null)
        {
            this.ultralightOverlay.setVisible(false);
        }
    }

    private void disableContext()
    {
        this.runner.getContext().shutdown();
    }

    @Override
    public boolean needsBackground()
    {
        return false;
    }

    @Override
    public boolean canPause()
    {
        return false;
    }

    @Override
    public boolean canRefresh()
    {
        return false;
    }

    @Override
    public ContentType getType()
    {
        return ContentType.FILMS;
    }

    @Override
    public IKey getTitle()
    {
        return UIKeys.FILM_TITLE;
    }

    @Override
    public void fillDefaultData(Film data)
    {
        super.fillDefaultData(data);

        IdleClip clip = new IdleClip();
        Camera camera = new Camera();
        Minecraft mc = Minecraft.getInstance();

        camera.set(mc.player, MathUtils.toRad(mc.options.fov().get()));

        clip.layer.set(8);
        clip.duration.set(BBSSettings.getDefaultDuration());
        clip.fromCamera(camera);
        data.camera.addClip(clip);

        this.newFilm = true;
    }

    @Override
    public void fill(Film data)
    {
        this.notifyServer(ActionState.STOP);
        super.fill(data);
        this.notifyServer(ActionState.RESTART);

        /* Keep the asset bin's project/backpack lists in sync with the
         * scene/sequence that was just opened. */
        this.assetBin.refresh();

        /* The HTML editor replaces the native UI whenever a work is open.
         * (The overlay is created at the end of the constructor, so it may
         * be null while fill() runs during construction.) */
        if (this.ultralightOverlay != null)
        {
            this.ultralightOverlay.setVisible(data != null);
        }
    }

    @Override
    protected void fillData(Film data)
    {
        if (this.data != null)
        {
            this.disableContext();
        }

        if (data != null)
        {
            this.undoHandler = new UIFilmUndoHandler(this);

            data.preCallback(this.undoHandler::handlePreValues);
        }
        else
        {
            this.undoHandler = null;
        }

        /* Fresh load = clean state for the title-bar dirty marker. */
        this.savedUndoIndex = this.undoHandler == null ? -1 : this.undoHandler.getUndoManager().getCurrentUndoIndex();

        this.preview.replays.setEnabled(data != null);        this.openHistory.setEnabled(data != null);
        this.toggleHorizontal.setEnabled(data != null);
        this.openCameraEditor.setEnabled(data != null);
        this.openReplayEditor.setEnabled(data != null);
        this.openActionEditor.setEnabled(data != null);
        this.duplicateFilm.setEnabled(data != null);

        this.actionEditor.setClips(null);
        this.runner.setWork(data == null ? null : data.camera);
        this.cameraEditor.setClips(data == null ? null : data.camera);
        this.replayEditor.setFilm(data);
        this.cameraEditor.pickClip(null);

        this.fillData();
        this.controller.createEntities();

        if (this.newFilm)
        {
            Clip main = this.data.camera.get(0);

            this.cameraEditor.clips.setSelected(main);
            this.cameraEditor.pickClip(main);
        }

        this.entered = data != null;
        this.newFilm = false;
    }

    @Override
    public void save()
    {
        super.save();

        this.markClean();
    }

    @Override
    public void forceSave()
    {
        super.forceSave();

        this.markClean();
    }

    /** Record the current undo index as the clean baseline (title-bar *). */
    private void markClean()
    {
        this.savedUndoIndex = this.undoHandler == null ? -1 : this.undoHandler.getUndoManager().getCurrentUndoIndex();
    }

    public void undo()
    {
        if (this.data != null && this.undoHandler.getUndoManager().undo(this.data)) UIUtils.playClick();
    }

    public void redo()
    {
        if (this.data != null && this.undoHandler.getUndoManager().redo(this.data)) UIUtils.playClick();
    }

    public boolean isFlying()
    {
        return this.dashboard.orbitUI.canControl();
    }

    public void toggleFlight()
    {
        this.setFlight(!this.isFlying());
    }

    /**
     * Set flight mode
     */
    public void setFlight(boolean flight)
    {
        if (!this.isRunning() || !flight)
        {
            this.runner.setManual(flight ? this.position : null);
            this.dashboard.orbitUI.setControl(flight);

            /* Marking the latest undo as unmergeable */
            if (this.undoHandler != null && !flight)
            {
                this.undoHandler.getUndoManager().markLastUndoNoMerging();
            }
            else
            {
                this.lastPosition.set(Position.ZERO);
            }
        }
    }

    public Vector2i getLoopingRange()
    {
        Clip clip = this.cameraEditor.getClip();

        int min = -1;
        int max = -1;

        if (clip != null)
        {
            min = clip.tick.get();
            max = min + clip.duration.get();
        }

        UIClips clips = this.cameraEditor.clips;

        if (clips.loopMin != clips.loopMax && clips.loopMin >= 0 && clips.loopMin < clips.loopMax)
        {
            min = clips.loopMin;
            max = clips.loopMax;
        }

        max = Math.min(max, this.data.camera.calculateDuration());

        return new Vector2i(min, max);
    }

    @Override
    public void update()
    {
        this.controller.update();

        if (this.playerToCamera && this.data != null)
        {
            this.teleportToCamera();
        }

        super.update();
    }

    /* Rendering code */

    @Override
    public void renderPanelBackground(UIContext context)
    {
        super.renderPanelBackground(context);

        Texture texture = BBSRendering.getTexture();

        if (texture != null)
        {
            context.batcher.box(0, 0, context.menu.width, context.menu.height, Colors.A100);

            int w = context.menu.width;
            int h = context.menu.height;
            Vector2i resize = Vectors.resize(texture.width / (float) texture.height, w, h);
            Area area = new Area();

            area.setSize(resize.x, resize.y);
            area.setPos((w - area.w) / 2, (h - area.h) / 2);

            context.batcher.texturedBox(texture.id, Colors.WHITE, area.x, area.y, area.w, area.h, 0, texture.height, texture.width, 0, texture.width, texture.height);
        }

        this.updateLogic(context);
    }

    @Override
    protected void renderBackground(UIContext context)
    {
        super.renderBackground(context);

        if (this.cameraEditor.isVisible()) UIDashboardPanels.renderHighlightHorizontal(context.batcher, this.openCameraEditor.area);
        if (this.replayEditor.isVisible()) UIDashboardPanels.renderHighlightHorizontal(context.batcher, this.openReplayEditor.area);
        if (this.actionEditor.isVisible()) UIDashboardPanels.renderHighlightHorizontal(context.batcher, this.openActionEditor.area);
    }

    /**
     * Draw everything on the screen
     */
    @Override
    public void render(UIContext context)
    {
        if (this.controller.isControlling())
        {
            context.mouseX = context.mouseY = -1;
        }

        this.controller.orbit.update(context);

        if (this.undoHandler != null)
        {
            this.undoHandler.submitUndo();
        }

        this.updateLogic(context);

        int color = BBSSettings.primaryColor.get();

        this.area.render(context.batcher, Colors.mulRGB(color | Colors.A100, 0.2F));

        if (this.editor.isVisible())
        {
            this.preview.area.render(context.batcher, Colors.A75);
        }

        super.render(context);

        if (this.entered)
        {
            LocalPlayer player = Minecraft.getInstance().player;
            Vec3 pos = player.position();
            Vector3d cameraPos = this.camera.position;
            double distance = cameraPos.distance(pos.x, pos.y, pos.z);
            int value = Minecraft.getInstance().options.renderDistance().get();

            if (distance > value * 12)
            {
                this.getContext().notifyError(UIKeys.FILM_TELEPORT_DESCRIPTION);
            }

            this.entered = false;
        }
    }

    /**
     * Update logic for such components as repeat fixture, minema recording,
     * sync mode, flight mode, etc.
     */
    private void updateLogic(UIContext context)
    {
        Clip clip = this.cameraEditor.getClip();

        /* Loop fixture */
        if (BBSSettings.editorLoop.get() && this.isRunning())
        {
            Vector2i loop = this.getLoopingRange();
            int min = loop.x;
            int max = loop.y;
            int ticks = this.getCursor();

            if (!this.recorder.isRecording() && !this.controller.isRecording() && min >= 0 && max >= 0 && min < max && (ticks >= max - 1 || ticks < min))
            {
                this.setCursor(min);
            }
        }

        /* Animate flight mode */
        if (this.dashboard.orbitUI.canControl())
        {
            this.dashboard.orbit.apply(this.position);

            Position current = new Position(this.getCamera());
            boolean check = this.flightEditTime.check();

            if (this.cameraEditor.getClip() != null && this.cameraEditor.isVisible() && this.controller.getPovMode() != UIFilmController.CAMERA_MODE_FREE)
            {
                if (!this.lastPosition.equals(current) && check)
                {
                    this.cameraEditor.editClip(current);
                }
            }

            if (check)
            {
                this.lastPosition.set(current);
            }
        }
        else
        {
            this.dashboard.orbit.setup(this.getCamera());
        }

        /* Rewind playback back to 0 */
        if (this.lastRunning && !this.isRunning())
        {
            this.lastRunning = this.runner.isRunning();

            if (BBSSettings.editorRewind.get())
            {
                this.setCursor(0);
                this.notifyServer(ActionState.RESTART);
            }
        }
    }

    /**
     * Draw icons for indicating different active states (like syncing
     * or flight mode)
     */
    private void renderIcons(UIContext context)
    {
        int x = this.iconBar.area.ex() - 18;
        int y = this.iconBar.area.ey() - 18;

        if (BBSSettings.editorLoop.get())
        {
            context.batcher.icon(Icons.REFRESH, x, y);
        }
    }

    private void renderDividers(UIContext context)
    {
        Area a1 = this.openHistory.area;
        Area a2 = this.toggleHorizontal.area;

        context.batcher.box(a1.x + 3, a1.ey() + 4, a1.ex() - 3, a1.ey() + 5, 0x22ffffff);
        context.batcher.box(a2.x + 3, a2.ey() + 4, a2.ex() - 3, a2.ey() + 5, 0x22ffffff);
    }

    @Override
    public void startRenderFrame(float tickDelta)
    {
        super.startRenderFrame(tickDelta);

        this.controller.startRenderFrame(tickDelta);
    }

    @Override
    public void renderInWorld(LevelRenderContext context)
    {
        super.renderInWorld(context);

        if (!BBSRendering.isIrisShadowPass())
        {
            this.lastProjection.set(new org.joml.Matrix4f());
            this.lastView.set(context.poseStack().last().pose());
        }

        this.controller.renderFrame(context);
    }

    /* IUICameraWorkDelegate implementation */

    public void notifyServer(ActionState state)
    {
        if (this.data == null || !ClientNetwork.isIsBBSModOnServer())
        {
            return;
        }

        String id = this.data.getId();
        int tick = this.getCursor();

        ClientNetwork.sendActionState(id, state, tick);
    }

    public Camera getCamera()
    {
        return this.camera;
    }

    public Camera getWorldCamera()
    {
        return BBSModClient.getCameraController().camera;
    }

    public CameraController getCameraController()
    {
        return BBSModClient.getCameraController();
    }

    @Override
    public int getCursor()
    {
        return this.runner.ticks;
    }

    @Override
    public void setCursor(int value)
    {
        this.flightEditTime.mark();
        this.lastPosition.set(Position.ZERO);

        this.runner.ticks = Math.max(0, value);

        this.notifyServer(ActionState.SEEK);
    }

    public boolean isRunning()
    {
        return this.runner.isRunning();
    }

    /**
     * Playback guard: every replay marked as an actor must have at least
     * one configured action clip, otherwise playback is refused with a
     * warning toast.
     */
    public boolean checkReplaysReady()
    {
        mchorse.bbs_mod.film.Film film = this.getData();

        if (film == null)
        {
            return true;
        }

        for (mchorse.bbs_mod.film.replays.Replay replay : film.replays.getList())
        {
            if (replay.actor.get() && replay.actions.getClips(mchorse.bbs_mod.actions.types.ActionClip.class).isEmpty())
            {
                this.getContext().notifyError(UIKeys.REPLAYS_NEED_ACTIONS.format(replay.label.get()));
                this.replayEditor.setReplay(replay);

                return false;
            }
        }

        return true;
    }

    public void togglePlayback()
    {
        this.setFlight(false);

        this.runner.toggle(this.getCursor());
        this.lastRunning = this.runner.isRunning();

        if (this.runner.isRunning())
        {
            this.cameraEditor.clips.scale.shiftIntoMiddle(this.getCursor());

            if (this.replayEditor.keyframeEditor != null)
            {
                this.replayEditor.keyframeEditor.view.getXAxis().shiftIntoMiddle(this.getCursor());
            }
        }
    }

    public boolean canUseKeybinds()
    {
        return !this.isFlying();
    }

    public void fillData()
    {
        this.cameraEditor.fillData();
        this.actionEditor.fillData();

        if (this.replayEditor.keyframeEditor != null && this.replayEditor.keyframeEditor.editor != null)
        {
            this.replayEditor.keyframeEditor.editor.update();
        }
    }

    public void teleportToCamera()
    {
        Camera camera = this.getCamera();
        Vector3d cameraPos = camera.position;
        double x = cameraPos.x;
        double y = cameraPos.y;
        double z = cameraPos.z;

        PlayerUtils.teleport(x, y, z, MathUtils.toDeg(camera.rotation.y) - 180F, MathUtils.toDeg(camera.rotation.x));
    }

    public boolean checkShowNoCamera()
    {
        boolean noCamera = this.getData().camera.calculateDuration() <= 0;

        if (noCamera)
        {
            UIOverlay.addOverlay(this.getContext(), new UIMessageOverlayPanel(
                UIKeys.FILM_NO_CAMERA_TITLE,
                UIKeys.FILM_NO_CAMERA_DESCRIPTION
            ));
        }

        return noCamera;
    }

    public void updateActors(String filmId, Map<String, Integer> actors)
    {
        if (this.data != null && this.data.getId().equals(filmId))
        {
            this.controller.updateActors(actors);
        }
    }

    @Override
    public boolean handleKeyPressed(UIContext context)
    {
        return this.controller.orbit.keyPressed(context, this.preview.area);
    }

    @Override
    public void applyUndoData(MapType data)
    {
        super.applyUndoData(data);

        this.showPanel(data.getInt("panel"));
        this.setCursor(data.getInt("tick"));
        this.controller.createEntities();
    }

    @Override
    public void collectUndoData(MapType data)
    {
        super.collectUndoData(data);

        data.putInt("panel", this.getPanelIndex());
        data.putInt("tick", this.getCursor());
    }

    @Override
    protected boolean canSave(UIContext context)
    {
        return !this.recorder.isRecording();
    }

    /**
     * Create a brand new scene (with background world picker) and open it
     * in the editor. Shared by the asset bin and the editor context menu.
     */
    public void newScene()
    {
        mchorse.bbs_mod.projects.SceneManager scenes = mchorse.bbs_mod.projects.SceneManager.get();

        if (scenes == null)
        {
            return;
        }

        UIOverlay.addOverlay(this.getContext(), new mchorse.bbs_mod.ui.projects.UINewSceneOverlayPanel((name, background) ->
        {
            String sceneName = name == null ? "" : name.trim();

            if (sceneName.isEmpty())
            {
                sceneName = UIKeys.SCENES_DEFAULT_NAME.format(scenes.getScenes().size() + 1).get();
            }

            mchorse.bbs_mod.projects.Scene scene = scenes.create(sceneName, background);
            this.assetBin.refresh();
            this.openScene(scene);
        }));
    }

    /**
     * Create a new character (replay) in the currently open scene, with
     * the type/options dialog. Shared by the asset bin and the editor
     * context menu.
     */
    public void newCharacter()
    {
        mchorse.bbs_mod.film.Film film = this.getData();

        if (film == null)
        {
            this.getContext().notifyError(UIKeys.ASSETS_NEED_SCENE);

            return;
        }

        UIOverlay.addOverlay(this.getContext(), new mchorse.bbs_mod.ui.projects.UINewEntityOverlayPanel((result) ->
        {
            mchorse.bbs_mod.film.replays.Replay replay = film.replays.addReplay();

            replay.form.set(result.type.factory.get());

            if (result.name != null && !result.name.isEmpty())
            {
                replay.label.set(result.name);
            }

            replay.actor.set(result.actor);
            replay.shadow.set(result.shadow);
            replay.looping.set(result.looping ? 1 : 0);

            this.replayEditor.setReplay(replay);
            this.showPanel(1);
            this.fillData();
        }));
    }

    private void playWithGuard()
    {
        if (this.checkReplaysReady())
        {
            this.togglePlayback();
        }
    }

    /**
     * Open a scene in the editor: make it current, load its film payload
     * and switch to the camera editor so the user can start cutting.
     */
    public void openScene(mchorse.bbs_mod.projects.Scene scene)
    {
        mchorse.bbs_mod.projects.SceneManager scenes = mchorse.bbs_mod.projects.SceneManager.get();

        if (scenes == null || scene == null)
        {
            return;
        }

        scenes.setCurrent(scene);

        mchorse.bbs_mod.film.Film film = scenes.loadFilm(scene);

        film.setId(scene.id);
        this.fill(film);
        this.showPanel(0);
    }

    /**
     * Linked-reference editing of a sequence: load the film of the first
     * scene the sequence references into this editor. Edits made here
     * propagate to every parent sequence that references it.
     */
    public void openSequence(mchorse.bbs_mod.projects.Sequence sequence)
    {
        if (sequence == null)
        {
            return;
        }

        mchorse.bbs_mod.projects.SceneManager scenes = mchorse.bbs_mod.projects.SceneManager.get();

        if (scenes == null)
        {
            return;
        }

        for (mchorse.bbs_mod.projects.Sequence.SequenceRef ref : sequence.refs)
        {
            if (mchorse.bbs_mod.projects.Sequence.SequenceRef.SCENE.equals(ref.type))
            {
                mchorse.bbs_mod.projects.Scene scene = scenes.getById(ref.id);

                if (scene != null)
                {
                    scenes.setCurrent(scene);

                    mchorse.bbs_mod.film.Film film = scenes.loadFilm(scene);

                    film.setId(scene.id);
                    this.fill(film);
                }

                return;
            }
        }

        this.getContext().notifyError(IKey.raw("Sequence has no scene reference yet"));
    }
}


