package lingfeng.bbsnext.mcef;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.Films;
import mchorse.bbs_mod.film.CameraGroup;
import mchorse.bbs_mod.film.CameraGroups;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.projects.Backpack;
import mchorse.bbs_mod.projects.BackpackService;
import mchorse.bbs_mod.projects.BBSProject;
import mchorse.bbs_mod.projects.ProjectManager;
import mchorse.bbs_mod.projects.Scene;
import mchorse.bbs_mod.projects.SceneManager;
import mchorse.bbs_mod.projects.Sequence;
import mchorse.bbs_mod.projects.SequenceManager;
import mchorse.bbs_mod.forms.forms.BlockForm;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ItemForm;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.forms.MobForm;
import mchorse.bbs_mod.forms.forms.ParticleForm;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.entity.ActorEntity;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.utils.undo.IUndo;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.UIScreen;
import lingfeng.bbsnext.update.UpdateChecker;
import lingfeng.bbsnext.update.UpdateConfig;
import lingfeng.bbsnext.update.LiveUi;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.actions.types.RecordedPathActionClip;
import mchorse.bbs_mod.camera.clips.overwrite.IdleClip;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.StringType;
import mchorse.bbs_mod.data.DataStorageUtils;
import mchorse.bbs_mod.camera.clips.overwrite.PathClip;
import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.camera.clips.CameraClip;
import mchorse.bbs_mod.camera.clips.CameraClipContext;
import mchorse.bbs_mod.camera.clips.misc.AudioClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.actions.types.ActionClip;
import mchorse.bbs_mod.actions.types.LocomotionActionClip;
import mchorse.bbs_mod.actions.types.ScriptActionClip;
import mchorse.bbs_mod.actions.types.item.ItemActionClip;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import lingfeng.bbsnext.camera.CameraPathRecorder;
import lingfeng.bbsnext.client.GlTextureBridge;
import lingfeng.bbsnext.film.replays.MaterialClip;
import lingfeng.bbsnext.film.replays.ActionGroup;
import lingfeng.bbsnext.film.replays.ActionGroupLibrary;
import lingfeng.bbsnext.film.replays.TrackPropStore;
import lingfeng.bbsnext.film.replays.TrackProp;
import lingfeng.bbsnext.film.replays.TrackOrderStore;
import lingfeng.bbsnext.film.replays.CameraTrackStore;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.item.Items;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelStorageSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/**
 * Java <-> JavaScript bridge for the HTML editor.
 *
 * The page reads its state from `window.bbsState` (pushed by
 * {@link MCEFUI#pushState()}, which calls {@link #getStateJson(EditorBridge)})
 * and sends actions back through the CEF JSQuery channel
 * ({@link #handle(String, EditorBridge)}).
 *
 * Action request format (JSON string): {"a":"openScene","id":"..."}
 */
public class EditorBridge implements IHtmlBridge
{
    private final UIFilmPanel panel;
    /** ID of the sequence that click-to-add drops assets into. Set when the
     *  user selects a sequence in the asset tree (enterSequence). */
    private static String activeSequenceId = null;
    /** Index of the timeline clip the user last selected (via clipOp select or
     *  by clicking a clip/track block). Used as the fallback target for
     *  clipOp delete/split when the request carries no explicit index — e.g.
     *  the "轨道 > 删除选中剪辑" menu item and the clip right-click split/delete
     *  items, which all send index -1. Without this the selection is never
     *  tracked on the Java side and those operations silently no-op. */
    private static int selectedClipIndex = -1;
    /** Index of the character (Replay) whose action editor is currently open in
     *  the HTML page (-1 = closed). The editor is rendered as an in-page modal
     *  so it stays visible in exclusive fullscreen (a Swing JDialog would be
     *  buried by the OS). */
    private static int actionEditorReplay = -1;
    /** Left-list mode for an action character: "action" (list ActionClips) or
     *  "path" (list the x/y/z/yaw walking-path keyframe channels). */
    private static String actionEditorLeftMode = "action";
    /** Index of the ActionClip selected in the right editor (-1 = none). */
    private static int actionEditorSelected = -1;
    /** Id of the keyframe channel selected in the right editor (null = none). */
    private static String actionEditorSelectedChannel = null;
    /** Index of the material clip selected in the material timeline (-1 = none). */
    private static int actionEditorSelectedMaterial = -1;
    /** Index of the character (Replay) currently focused in the asset-detail
     *  panel (-1 = none). The panel shares the action editor's state, so both
     *  the panel and the modal read from the same focused/open replay. */
    private static int focusedCharacterIndex = -1;
    /** Stable replay id of the currently focused character (D1: index-free focus
     *  tracking so deleting a replay cannot shift the focused target). */
    private static String focusedCharacterId = null;
    /** Stable replay id backing {@code actionEditorReplay} so undo/redo and
     *  deletions keep the action editor pinned to the right replay. */
    private static String actionEditorReplayId = null;
    /** Index of the character (Replay) whose equipment editor is currently open
     *  in the HTML page (-1 = closed). The editor is rendered as an in-page
     *  modal so it stays visible in exclusive fullscreen (a Swing JDialog would
     *  be buried by the OS and the button would appear dead). */
    private static int equipReplay = -1;
    private static JsonObject renameReq = null; /* {kind:"camera"|"cameragroup", target, current} -> HTML rename modal */
    private static final Gson GSON = new Gson();

    /**
     * B1: coalescing flag for editor refreshes. Every {@link #refreshHtml()}
     * (and {@link #scheduleRefresh()}) now only sets this flag and returns;
     * the real {@code pushState()} is consumed at most once per client tick by
     * the {@link ClientTickEvents#END_CLIENT_TICK} hook below. This bounds the
     * number of full HTML rebuilds to one per frame no matter how many UI
     * interactions fire in that frame (previously each call rebuilt the whole
     * editor synchronously, which made the editor feel extremely sluggish).
     */
    private static boolean pendingRefresh = false;
    private static boolean tickHookRegistered = false;
    private static EditorBridge instance;
    /** Whether the editor has opened a preview/world (true after a successful
     *  openWorld; cleared on exitPreviewWorld / closeEditor). Drives the
     *  inWorld flag the HTML page reads to toggle the preview-world buttons. */
    private static boolean inWorld = false;
    /** True only while this editor owns the live preview world (bbs_preview
     *  entered via enterPreviewWorld, or a scene world via enterSceneWorld).
     *  Drives the "owningPreviewWorld" flag the HTML page reads to decide
     *  whether the exit-preview-world button is shown. False the moment the
     *  world is torn down so the button can never outlive the world. */
    private static boolean owningPreviewWorld = false;
    /** Re-entrancy guard for exitPreviewWorld: a double fire (e.g. button
     *  + shortcut, or rapid toggles) must not re-enter the teardown path. */
    private static boolean exitingPreviewWorld = false;

    public EditorBridge(UIFilmPanel panel)
    {
        this.panel = panel;
        instance = this;
    }

    /* -------- IHtmlBridge -------- */

    @Override
    public String getStateJson()
    {
        return getStateJson(this);
    }

    @Override
    public String handle(String request)
    {
        return handle(request, this);
    }

    @Override
    public String pageUrl()
    {
        return MCEFUI.pageFileUrl("editor_ui.html");
    }

    /* -------- state (JSON) -------- */

    /** Full editor state as a JSON string, pushed to the page as window.bbsState. */
    public static String getStateJson(EditorBridge bridge)
    {
        JsonObject root = new JsonObject();

        UIFilmPanel panel = bridge.panel;

        if (Minecraft.getInstance().level != null)
        {
            clearEnteringWorld();
        }

        BBSProject project = ProjectManager.get() == null ? null : ProjectManager.get().getCurrent();
        SceneManager scenes = SceneManager.get();
        Film film = panel.getData();

        root.addProperty("project", project == null ? "" : project.name);
        root.addProperty("film", film == null ? "" : film.getId());

        Scene current = scenes == null ? null : scenes.getCurrent();
        root.addProperty("scene", current == null ? "" : current.name);
        root.addProperty("sceneWorld", current == null || current.background == null ? "" : current.background);
        root.addProperty("inWorld", inWorld || Minecraft.getInstance().level != null);
        root.addProperty("owningPreviewWorld", owningPreviewWorld);

        root.addProperty("cursor", panel.getCursor());
        root.addProperty("running", panel.isRunning());
        root.addProperty("dirty", panel.isDirty());

        /* Off-world 3D preview (canvas-rendered in the page; no world needed) */
        JsonObject preview3d = new JsonObject();
        int cursorTick = panel.getCursor();
        preview3d.addProperty("cursor", cursorTick);

        JsonArray previewCamArr = new JsonArray();

        if (film != null)
        {
            for (Clip c : film.camera.get())
            {
                if (c == null || !c.enabled.get())
                {
                    continue;
                }

                JsonObject co = new JsonObject();
                co.addProperty("title", c.title.get());
                co.addProperty("tick", c.tick.get());
                co.addProperty("duration", c.duration.get());
                JsonArray path = new JsonArray();

                if (c instanceof PathClip)
                {
                    PathClip pc = (PathClip) c;

                    for (int i = 0; i < pc.size(); i++)
                    {
                        Position p = pc.getPoint(i);
                        JsonObject po = new JsonObject();
                        po.addProperty("x", p.point.x);
                        po.addProperty("y", p.point.y);
                        po.addProperty("z", p.point.z);
                        path.add(po);
                    }
                }

                co.add("path", path);
                previewCamArr.add(co);
            }
        }

        preview3d.add("cameras", previewCamArr);

        /* Camera pose at the playhead: interpolate the active PathClip, else a
         * default elevated look-at pose so the grid is visible. */
        JsonObject pose = new JsonObject();
        pose.addProperty("x", 0.0D);
        pose.addProperty("y", 60.0D);
        pose.addProperty("z", 0.0D);
        pose.addProperty("yaw", 0.0D);
        pose.addProperty("pitch", -35.0D);

        if (film != null)
        {
            for (Clip c : film.camera.get())
            {
                if (c == null || !c.enabled.get() || !(c instanceof PathClip))
                {
                    continue;
                }

                PathClip pc = (PathClip) c;
                int t0 = c.tick.get();
                int t1 = t0 + c.duration.get();

                if (cursorTick >= t0 && cursorTick < t1 && pc.size() > 0)
                {
                    CameraClipContext ctx = new CameraClipContext();
                    ctx.setup(cursorTick, 0);
                    Position pos = new Position();
                    ctx.apply(pc, pos);
                    pose.addProperty("x", (double) pos.point.x);
                    pose.addProperty("y", (double) pos.point.y);
                    pose.addProperty("z", (double) pos.point.z);
                    pose.addProperty("yaw", (double) pos.angle.yaw);
                    pose.addProperty("pitch", (double) pos.angle.pitch);
                    break;
                }
            }
        }

        preview3d.add("pose", pose);

        /* Actor ground positions at the playhead. */
        JsonArray actArr = new JsonArray();

        if (film != null)
        {
            for (Replay r : film.replays.getList())
            {
                if (!r.enabled.get())
                {
                    continue;
                }

                JsonObject ao = new JsonObject();
                ao.addProperty("x", r.keyframes.x.interpolate(cursorTick, 0.0D));
                ao.addProperty("y", r.keyframes.y.interpolate(cursorTick, 0.0D));
                ao.addProperty("z", r.keyframes.z.interpolate(cursorTick, 0.0D));
                ao.addProperty("masked", r.masked.get());
                actArr.add(ao);
            }
        }

        preview3d.add("actors", actArr);

        root.add("preview3d", preview3d);

        /* Scenes */
        JsonArray sceneArr = new JsonArray();

        if (scenes != null)
        {
            for (Scene scene : scenes.getScenes())
            {
                JsonObject obj = new JsonObject();
                obj.addProperty("id", scene.id);
                obj.addProperty("name", scene.name);
                obj.addProperty("background", scene.background == null ? "" : scene.background);
                sceneArr.add(obj);
            }
        }

        root.add("scenes", sceneArr);

        /* Available background worlds for scene creation: blank world first,
         * then every local singleplayer save Minecraft knows about.
         * Primary path uses Minecraft's own LevelStorageSource API, which is
         * launcher-agnostic (PCL2 version isolation, custom game directories,
         * symlinks) and reads the real save registry instead of guessing.
         * Only if that API is unavailable do we fall back to a manual
         * filesystem walk (findSavesDir). */
        JsonArray worldArr = new JsonArray();
        worldArr.add("");

        try
        {
            LevelStorageSource source = Minecraft.getInstance().getLevelSource();
            LevelStorageSource.LevelCandidates candidates = source.findLevelCandidates();

            for (LevelStorageSource.LevelDirectory dir : candidates)
            {
                Path p = dir.path();
                String name = p.getFileName() == null ? p.toString() : p.getFileName().toString();

                if (!name.isEmpty() && !BBS_PREVIEW_WORLD.equals(name))
                {
                    worldArr.add(name);
                }
            }
        }
        catch (Throwable t)
        {
            /* Fallback: manual scan of the saves directory. */
            File saves = findSavesDir(Minecraft.getInstance().gameDirectory);

            if (saves != null && saves.isDirectory())
            {
                File[] dirs = saves.listFiles(File::isDirectory);

                if (dirs != null)
                {
                    for (File dir : dirs)
                    {
                        if (new File(dir, "level.dat").exists() && !BBS_PREVIEW_WORLD.equals(dir.getName()))
                        {
                            worldArr.add(dir.getName());
                        }
                    }
                }
            }
        }

        root.add("worlds", worldArr);

        /* Registered entity types the user can pick as a character model in
         * the new-character dialog. Player is offered as a special skin model. */
        JsonArray modelArr = new JsonArray();
        JsonObject playerModel = new JsonObject();
        playerModel.addProperty("id", "minecraft:player");
        playerModel.addProperty("name", "玩家（皮肤）");
        modelArr.add(playerModel);

        for (EntityType<?> et : BuiltInRegistries.ENTITY_TYPE)
        {
            Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(et);

            if (id == null)
            {
                continue;
            }

            JsonObject o = new JsonObject();
            o.addProperty("id", id.toString());
            o.addProperty("name", id.getPath());
            modelArr.add(o);
        }

        root.add("models", modelArr);

        /* Sequences (containers that reference scenes, not characters) */
        JsonArray seqArr = new JsonArray();

        if (scenes != null)
        {
            for (Sequence seq : SequenceManager.get().getSequences())
            {
                JsonObject obj = new JsonObject();
                obj.addProperty("id", seq.id);
                obj.addProperty("name", seq.name);

                JsonArray refs = new JsonArray();

                for (Sequence.SequenceRef ref : seq.refs)
                {
                    JsonObject r = new JsonObject();
                    r.addProperty("type", ref.type);
                    r.addProperty("id", ref.id);
                    refs.add(r);
                }

                obj.add("refs", refs);
                seqArr.add(obj);
            }
        }

        root.add("sequences", seqArr);

        /* Backpack (global cross-work asset library) */
        JsonArray bpArr = new JsonArray();

        for (Backpack.Entry entry : Backpack.getEntries())
        {
            JsonObject o = new JsonObject();

            o.addProperty("name", entry.name);
            o.addProperty("type", entry.type);
            o.addProperty("label", entry.label);
            bpArr.add(o);
        }

        root.add("backpack", bpArr);

        /* Replays (characters) */
        JsonArray replayArr = new JsonArray();

        if (film != null)
        {
            int i = 0;

            for (Replay replay : film.replays.getList())
            {
                JsonObject obj = new JsonObject();
                obj.addProperty("index", i++);
                obj.addProperty("id", replay.getId());
                obj.addProperty("label", replay.getName());
                obj.addProperty("actor", replay.actor.get());
                obj.addProperty("fp", replay.fp.get());
                obj.addProperty("enabled", replay.enabled.get());
                obj.addProperty("masked", replay.masked.get());
                obj.addProperty("shadow", replay.shadow.get());
                obj.add("props", TrackPropStore.get(film.getId(), replay.getId()).toJson());
                replayArr.add(obj);
            }
        }

        root.add("replays", replayArr);

        /* Assets classified by form type for the asset box tree. Replays all
         * live in film.replays but map to character/entity/particle/item. */
        JsonArray charsArr = new JsonArray();
        JsonArray entsArr = new JsonArray();
        JsonArray partsArr = new JsonArray();
        JsonArray itemsArr = new JsonArray();

        if (film != null)
        {
            String filmId = film.getId();
            List<String> placed = TrackOrderStore.get(filmId);

            int libIdx = 0;

            for (Replay replay : film.replays.getList())
            {
                /* Library vs track: only UNPLACED replays appear in the asset
                 * box. Placed replays are rendered as timeline tracks instead,
                 * so creating a character no longer auto-grabs a timeline track
                 * (it lands in the library until dragged onto the timeline). */
                if (placed.contains(replay.getId()))
                {
                    libIdx++;
                    continue;
                }

                JsonObject obj = new JsonObject();
                /* F0: stable list index (into film.replays) is carried alongside
                 * the id so index-based subsystems (action editor, focus) can
                 * resolve without guessing; the id remains the source of truth. */
                obj.addProperty("index", libIdx);
                obj.addProperty("id", replay.getId());
                obj.addProperty("label", replay.label.get());
                obj.add("props", TrackPropStore.get(filmId, replay.getId()).toJson());

                Form f = replay.form.get();
                String cat = "entity";

                if (f instanceof MobForm || f instanceof ModelForm)
                {
                    cat = "character";
                }
                else if (f instanceof ParticleForm)
                {
                    cat = "particle";
                }
                else if (f instanceof ItemForm)
                {
                    cat = "item";
                }
                else if (f instanceof BlockForm)
                {
                    cat = "entity";
                }

                obj.addProperty("cat", cat);

                JsonArray bucket = entsArr;

                if ("character".equals(cat))
                {
                    bucket = charsArr;
                }
                else if ("particle".equals(cat))
                {
                    bucket = partsArr;
                }
                else if ("item".equals(cat))
                {
                    bucket = itemsArr;
                }

                bucket.add(obj);
                libIdx++;
            }
        }

        root.add("characters", charsArr);
        root.add("entities", entsArr);
        root.add("particles", partsArr);
        root.add("items", itemsArr);

        /* Action characters' actions surfaced into the asset box so the user
         * can drag them onto the path. */
        JsonArray actionAssets = new JsonArray();
        if (film != null)
        {
            int ci = 0;
            for (Replay r : film.replays.getList())
            {
                if ("action".equals(r.characterType.get()))
                {
                    int ai = 0;
                    for (Clip c : r.actions.get())
                    {
                        JsonObject a = new JsonObject();
                        a.addProperty("charIndex", ci);
                        a.addProperty("charLabel", r.getName());
                        a.addProperty("actionIndex", ai);
                        a.addProperty("title", c.title.get().isEmpty() ? "(未命名动作)" : c.title.get());
                        actionAssets.add(a);
                        ai++;
                    }
                }
                ci++;
            }
        }
        root.add("actionAssets", actionAssets);

        root.addProperty("activeSequence", activeSequenceId == null ? "" : activeSequenceId);

        /* Timeline clips: active sequence's resolved refs, or the camera timeline. */
        JsonArray clipArr = new JsonArray();

        if (film != null)
        {
            if (activeSequenceId != null && !activeSequenceId.isEmpty())
            {
                SequenceManager seqMgr = SequenceManager.get();
                Sequence activeSeq = seqMgr == null ? null : seqMgr.getById(activeSequenceId);
                clipArr = buildSequenceClips(activeSeq, film, scenes);
            }
            else
            {
                for (Clip clip : film.camera.get())
                {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("id", clip.id.get());
                    obj.addProperty("title", clip.title.get());
                    obj.addProperty("tick", clip.tick.get());
                    obj.addProperty("layer", clip.layer.get());
                    obj.addProperty("duration", clip.duration.get());
                    obj.addProperty("enabled", clip.enabled.get());
                    clipArr.add(obj);
                }
            }
        }

        root.add("clips", clipArr);

        /* Multi-track timeline: one row per track (camera track + one action
         * track per replay). Only built in camera mode (no active sequence);
         * sequence mode keeps showing resolved refs via clipArr. */
        JsonArray tracksArr = new JsonArray();

        if (film != null && (activeSequenceId == null || activeSequenceId.isEmpty()))
        {
            String filmId = film.getId();

            /* Legacy films have no explicit track order yet: seed it with every
             * replay in list order so they all appear on the timeline (matches
             * the old "every replay is a track" behaviour). Characters created
             * afterwards are NOT seeded, so they stay in the library until
             * dragged onto the timeline. */
            if (!TrackOrderStore.has(filmId))
            {
                List<String> all = new ArrayList<>();

                for (Replay r : film.replays.getList())
                {
                    all.add(r.getId());
                }

                TrackOrderStore.set(filmId, all);
            }

            /* Same legacy-seed for cameras: existing films show all their
             * cameras on the track until the user re-arranges them. New films
             * are seeded on first render; cameras created afterwards stay in
             * the library (film.camera) until dragged onto the track. */
            if (!CameraTrackStore.has(filmId))
            {
                List<String> allCam = new ArrayList<>();

                for (Clip c : film.camera.get())
                {
                    allCam.add(c.id.get());
                }

                CameraTrackStore.set(filmId, allCam);
            }

            Map<String, Replay> byId = new HashMap<>();

            for (Replay r : film.replays.getList())
            {
                byId.put(r.getId(), r);
            }

            List<String> order = TrackOrderStore.get(filmId);

            JsonObject camTrack = new JsonObject();
            camTrack.addProperty("kind", "camera");
            camTrack.addProperty("id", "camera");
            camTrack.addProperty("label", "相机");
            JsonArray camClips = new JsonArray();
            for (String cid : CameraTrackStore.get(filmId))
            {
                Clip clip = findCameraClip(film, cid);

                if (clip == null)
                {
                    continue;
                }

                JsonObject o = new JsonObject();
                o.addProperty("id", clip.id.get());
                o.addProperty("title", clip.title.get());
                o.addProperty("tick", clip.tick.get());
                o.addProperty("layer", clip.layer.get());
                o.addProperty("duration", clip.duration.get());
                o.addProperty("enabled", clip.enabled.get());
                camClips.add(o);
            }
            camTrack.add("clips", camClips);
            tracksArr.add(camTrack);

            int ri = 0;
            for (String replayId : order)
            {
                Replay replay = byId.get(replayId);

                if (replay == null)
                {
                    continue;
                }

                JsonObject rt = new JsonObject();
                rt.addProperty("kind", "replay");
                rt.addProperty("id", replay.getId());
                rt.addProperty("index", ri);
                rt.addProperty("label", replay.getName());
                JsonArray aClips = new JsonArray();
                for (Clip clip : replay.actions.get())
                {
                    JsonObject o = new JsonObject();
                    o.addProperty("id", clip.id.get());
                    o.addProperty("title", clip.title.get());
                    o.addProperty("tick", clip.tick.get());
                    o.addProperty("layer", clip.layer.get());
                    o.addProperty("duration", clip.duration.get());
                    o.addProperty("enabled", clip.enabled.get());
                    aClips.add(o);
                }
                rt.add("clips", aClips);
                rt.add("props", TrackPropStore.get(filmId, replay.getId()).toJson());
                tracksArr.add(rt);
                ri++;
            }
        }

        root.add("tracks", tracksArr);

        /* Camera list (for the asset-bin "相机" group). */
        JsonArray camArr = new JsonArray();

        if (film != null)
        {
            int ci = 0;

            for (Clip clip : film.camera.get())
            {
                JsonObject obj = new JsonObject();

                obj.addProperty("index", ci);
                obj.addProperty("id", clip.id.get());
                obj.addProperty("title", clip.title.get().isEmpty() ? clip.getClass().getSimpleName() : clip.title.get());
                obj.addProperty("base", isBaseCameraType(clip));
                obj.addProperty("type", clip.getClass().getSimpleName());
                obj.addProperty("recording", CameraPathRecorder.isActive() && CameraPathRecorder.isRecording(clip instanceof PathClip ? (PathClip) clip : null));
                camArr.add(obj);
                ci++;
            }
        }

        root.add("cameras", camArr);

        /* Camera groups (for the asset-bin "相机组" group). */
        JsonArray grpArr = new JsonArray();

        if (film != null)
        {
            int gi = 0;

            for (CameraGroup g : film.cameraGroups.getList())
            {
                JsonObject obj = new JsonObject();

                obj.addProperty("index", gi);
                obj.addProperty("id", g.id.get());
                obj.addProperty("name", g.name.get().isEmpty() ? ("相机组 " + (gi + 1)) : g.name.get());
                obj.addProperty("count", g.cameraIds().size());

                JsonArray rs = new JsonArray();

                for (String cid : g.cameraIds())
                {
                    rs.add(cid);
                }

                obj.add("refs", rs);
                grpArr.add(obj);
                gi++;
            }
        }

        root.add("cameraGroups", grpArr);

        long timelineDuration = film == null ? 0 : film.camera.calculateDuration();

        if (activeSequenceId != null && !activeSequenceId.isEmpty())
        {
            long seqEnd = 0;

            for (int ci = 0; ci < clipArr.size(); ci++)
            {
                JsonObject co = clipArr.get(ci).getAsJsonObject();
                long end = 0;

                if (co.has("tick") && co.has("duration"))
                {
                    end = co.get("tick").getAsLong() + co.get("duration").getAsLong();
                }
                else if (co.has("duration"))
                {
                    end = co.get("duration").getAsLong();
                }

                if (end > seqEnd)
                {
                    seqEnd = end;
                }
            }

            timelineDuration = seqEnd;
        }
        else if (film != null)
        {
            for (Replay r : film.replays.getList())
            {
                timelineDuration = Math.max(timelineDuration, r.actions.calculateDuration());
            }
        }

        root.addProperty("duration", timelineDuration);

        /* Camera period status: lost / overlap detection across the timeline. */
        if (film != null)
        {
            root.add("cameraStatus", computeCameraStatus(film, panel.getCursor()));
        }

        /* Surface an available update in the editor too (bottom-left notice). */
        UpdateChecker.UpdateInfo info = UpdateChecker.current;
        UpdateConfig cfg = UpdateConfig.get();

        if (info != null && (cfg.updatePush || cfg.autoUpdate))
        {
            JsonObject update = new JsonObject();

            update.addProperty("version", info.version);
            update.addProperty("releaseName", info.releaseName);
            update.addProperty("size", info.size);
            update.addProperty("staged", info.stagedJar != null && Files.exists(info.stagedJar));
            root.add("update", update);
        }

        /* Throttled background check for the sandboxed live-UI script. */
        LiveUi.ensureChecked();

        JsonObject actionEditor = buildActionEditorState(bridge);
        if (actionEditor != null)
        {
            root.add("actionEditor", actionEditor);
        }

        root.addProperty("actionEditorOpen", actionEditorReplay >= 0);

        JsonObject focused = buildFocusedCharacterSlice(bridge.panel);
        if (focused != null)
        {
            root.add("focusedCharacter", focused);
        }

        JsonObject equip = buildEquipState(bridge);
        if (equip != null)
        {
            root.add("equip", equip);
        }

        if (renameReq != null)
        {
            root.add("rename", renameReq);
        }

        return GSON.toJson(root);
    }

    /**
     * A1: Locate the real "saves" directory for the current Minecraft instance.
     *
     * <p>Standard (non-isolated) launchers keep saves at
     * {@code gameDirectory/saves}, but version-isolated launchers (e.g. PCL2)
     * set {@code gameDirectory} to something like {@code .../versions/luzhi},
     * while the actual {@code .minecraft/saves} lives higher up the tree. We
     * therefore probe, starting at {@code start}, each ancestor's "saves"
     * directory and return the first one that both exists and contains at
     * least one world folder (a sub-directory with a {@code level.dat}). If no
     * such directory is found we return the closest existing "saves" dir as a
     * fallback (or null).</p>
     */
    private static File findSavesDir(File start)
    {
        File dir = start;
        File fallback = null;

        while (dir != null)
        {
            File candidate = new File(dir, "saves");

            if (candidate.isDirectory())
            {
                if (fallback == null)
                {
                    fallback = candidate;
                }

                File[] subs = candidate.listFiles(File::isDirectory);

                if (subs != null)
                {
                    for (File sub : subs)
                    {
                        if (new File(sub, "level.dat").exists())
                        {
                            return candidate;
                        }
                    }
                }
            }

            dir = dir.getParentFile();
        }

        return fallback;
    }

    private static JsonArray buildSequenceClips(Sequence seq, Film film, SceneManager scenes)
    {
        JsonArray arr = new JsonArray();

        if (seq == null || film == null)
        {
            return arr;
        }

        if (seq.refs == null || seq.refs.isEmpty())
        {
            JsonObject empty = new JsonObject();
            empty.addProperty("id", "ref:" + seq.id + ":empty");
            empty.addProperty("title", "空序列");
            empty.addProperty("type", "empty");
            empty.addProperty("tick", 0);
            empty.addProperty("layer", 0);
            empty.addProperty("duration", 1);
            empty.addProperty("enabled", true);
            empty.addProperty("unresolved", false);
            arr.add(empty);
            return arr;
        }

        long cursor = 0;
        int i = 0;

        for (Sequence.SequenceRef ref : seq.refs)
        {
            if (ref == null)
            {
                i++;
                continue;
            }

            long in = ref.in;
            long out = ref.out;
            long tick;
            long duration;

            if (in >= 0 && out > in)
            {
                tick = in;
                duration = out - in;
            }
            else
            {
                tick = cursor;
                duration = defaultRefDuration(ref);
            }

            if (duration < 1)
            {
                duration = 1;
            }

            JsonObject blk = new JsonObject();
            blk.addProperty("id", "ref:" + seq.id + ":" + i);
            blk.addProperty("type", ref.type == null ? "" : ref.type);
            blk.addProperty("tick", tick);
            blk.addProperty("layer", 0);
            blk.addProperty("duration", duration);

            String name = resolveRefName(ref, film, scenes);
            boolean unresolved = name == null;

            if (unresolved)
            {
                name = (ref.type == null ? "" : ref.type) + ":" + (ref.id == null ? "" : ref.id);
            }

            blk.addProperty("title", name);
            blk.addProperty("enabled", true);
            blk.addProperty("unresolved", unresolved);

            arr.add(blk);
            cursor = tick + duration;
            i++;
        }

        return arr;
    }

    private static long defaultRefDuration(Sequence.SequenceRef ref)
    {
        if (Sequence.SequenceRef.SEQUENCE.equals(ref.type) && ref.id != null)
        {
            try
            {
                SequenceManager sm = SequenceManager.get();
                Sequence child = sm == null ? null : sm.getById(ref.id);

                if (child != null && child.refs != null && !child.refs.isEmpty())
                {
                    long end = 0;
                    long c = 0;

                    for (Sequence.SequenceRef cr : child.refs)
                    {
                        long d = (cr.in >= 0 && cr.out > cr.in) ? (cr.out - cr.in) : 1;
                        end = Math.max(end, c + d);
                        c += d;
                    }

                    return Math.max(1, end);
                }
            }
            catch (Exception e)
            {
            }
        }

        return 1;
    }

    private static String resolveRefName(Sequence.SequenceRef ref, Film film, SceneManager scenes)
    {
        if (ref == null || ref.type == null || film == null)
        {
            return null;
        }

        try
        {
            if (Sequence.SequenceRef.SCENE.equals(ref.type))
            {
                Scene sc = scenes == null ? null : scenes.getById(ref.id);
                return sc == null ? null : sc.name;
            }

            if (Sequence.SequenceRef.SEQUENCE.equals(ref.type))
            {
                SequenceManager sm = SequenceManager.get();
                Sequence s = sm == null ? null : sm.getById(ref.id);
                return s == null ? null : s.name;
            }

            if (Sequence.SequenceRef.MCPR.equals(ref.type))
            {
                Replay r = findReplay(film, ref.id);
                return r == null ? null : r.getName();
            }

            if (Sequence.SequenceRef.CAMERA.equals(ref.type))
            {
                Clip c = findClip(film, ref.id, false);
                return c == null ? null : c.title.get();
            }

            if (Sequence.SequenceRef.CAMERAGROUP.equals(ref.type))
            {
                if (ref.id != null)
                {
                    for (CameraGroup g : film.cameraGroups.getList())
                    {
                        if (ref.id.equals(g.id.get()))
                        {
                            return g.name.get();
                        }
                    }
                }

                return null;
            }

            if (Sequence.SequenceRef.AUDIO.equals(ref.type))
            {
                Clip c = findClip(film, ref.id, true);
                return c == null ? null : c.title.get();
            }
        }
        catch (Exception e)
        {
        }

        return null;
    }

    private static Replay findReplay(Film film, String id)
    {
        List<Replay> list = film.replays.getList();
        Integer idx = parseIntSafe(id);

        if (idx != null && idx >= 0 && idx < list.size())
        {
            return list.get(idx);
        }

        if (id != null)
        {
            for (Replay r : list)
            {
                if (id.equals(r.getName()))
                {
                    return r;
                }
            }
        }

        return null;
    }

    private static Clip findClip(Film film, String id, boolean requireAudio)
    {
        List<? extends Clip> list = film.camera.get();
        Integer idx = parseIntSafe(id);

        if (idx != null && idx >= 0 && idx < list.size())
        {
            Clip c = list.get(idx);

            if (!requireAudio || c instanceof AudioClip)
            {
                return c;
            }
        }

        if (id != null)
        {
            for (Clip c : list)
            {
                boolean ok = requireAudio ? (c instanceof AudioClip) : true;

                if (ok && id.equals(c.id.get()))
                {
                    return c;
                }
            }
        }

        return null;
    }

    private static Integer parseIntSafe(String s)
    {
        if (s == null)
        {
            return null;
        }

        try
        {
            return Integer.parseInt(s.trim());
        }
        catch (Exception e)
        {
            return null;
        }
    }

    /** A "base camera" sets an absolute camera position (overwrite package). */
    private static boolean isBaseCameraType(Clip clip)
    {
        return clip.getClass().getName().startsWith("mchorse.bbs_mod.camera.clips.overwrite.");
    }

    /**
     * Computes camera-period status across the timeline: which spans have no
     * base camera ("相机丢失") and which are covered by two or more base cameras
     * ("重叠"). Also reports the status at the current cursor tick.
     */
    private static JsonObject computeCameraStatus(Film film, int cursor)
    {
        JsonObject status = new JsonObject();
        int duration = film.camera.calculateDuration();

        status.addProperty("duration", duration);

        List<Clip> base = new ArrayList<>();

        for (Clip c : film.camera.get())
        {
            if (c.enabled.get() && isBaseCameraType(c))
            {
                base.add(c);
            }
        }

        status.addProperty("baseCount", base.size());

        boolean lost = false;
        boolean overlap = false;
        boolean currentLost = false;
        boolean currentOverlap = false;

        JsonArray lostRanges = new JsonArray();
        JsonArray overlapRanges = new JsonArray();

        if (duration <= 0)
        {
            lost = true;
            currentLost = true;
            addRange(lostRanges, 0, 0);
        }
        else
        {
            int runKind = 0; /* 0 none, 1 lost, 2 overlap */
            int lastStart = -1;

            for (int t = 0; t <= duration; t++)
            {
                int count = 0;

                for (Clip c : base)
                {
                    if (c.isInside(t))
                    {
                        count++;
                    }
                }

                int kind = count == 0 ? 1 : (count >= 2 ? 2 : 0);

                if (t == cursor)
                {
                    currentLost = kind == 1;
                    currentOverlap = kind == 2;
                }

                if (kind != runKind)
                {
                    if (runKind == 1)
                    {
                        lost = true;
                        addRange(lostRanges, lastStart, t - 1);
                    }
                    else if (runKind == 2)
                    {
                        overlap = true;
                        addRange(overlapRanges, lastStart, t - 1);
                    }

                    runKind = kind;
                    lastStart = t;
                }
            }

            if (runKind == 1)
            {
                lost = true;
                addRange(lostRanges, lastStart, duration);
            }
            else if (runKind == 2)
            {
                overlap = true;
                addRange(overlapRanges, lastStart, duration);
            }
        }

        status.addProperty("lost", lost);
        status.addProperty("overlap", overlap);
        status.addProperty("currentLost", currentLost);
        status.addProperty("currentOverlap", currentOverlap);
        status.add("lostRanges", lostRanges);
        status.add("overlapRanges", overlapRanges);

        return status;
    }

    private static void addRange(JsonArray arr, int s, int e)
    {
        JsonArray r = new JsonArray();

        r.add(s);
        r.add(e);
        arr.add(r);
    }

    /* -------- action dispatch (JSQuery) -------- */

    /**
     * Dispatch an action request from the page. Returns a JSON result string
     * ("ok" on success, or an error message).
     */
    public static String handle(String request, EditorBridge bridge)
    {
        JsonObject req;

        try
        {
            req = GSON.fromJson(request, JsonObject.class);
        }
        catch (Throwable t)
        {
            return "{\"ok\":false,\"error\":\"bad request\"}";
        }

        if (bridge == null)
        {
            return "{\"ok\":false,\"error\":\"no bridge\"}";
        }

        UIFilmPanel panel = bridge.panel;
        String action = req.has("a") ? req.get("a").getAsString() : "";

        switch (action)
        {
            case "openScene":
                openScene(panel, req.has("id") ? req.get("id").getAsString() : null);
                break;
            case "togglePlay":
                panel.togglePlayback();
                patchRunning(panel.isRunning());
                break;
            case "setCursor":
                panel.setCursor(req.has("tick") ? req.get("tick").getAsInt() : panel.getCursor());
                break;
            case "save":
                panel.save();
                break;
            case "undo":
                panel.undo();
                break;
            case "redo":
                panel.redo();
                break;
            case "closeEditor":
                closeEditor(panel);
                break;
            case "createScene":
                createScene(panel, req.has("name") ? req.get("name").getAsString() : "", req.has("world") ? req.get("world").getAsString() : "");
                break;
            case "createCamera":
                createCamera(panel, req.has("name") ? req.get("name").getAsString() : "");
                break;
            case "createSequence":
                createSequence(panel, req.has("name") ? req.get("name").getAsString() : null);
                break;
            case "createEntity":
                createEntity(panel, req.has("name") ? req.get("name").getAsString() : null);
                break;
            case "createParticle":
                createParticle(panel, req.has("name") ? req.get("name").getAsString() : null);
                break;
            case "createItem":
                createItem(panel, req.has("id") ? req.get("id").getAsString() : "");
                break;
            case "createCharacter":
                createCharacter(panel, req);
                break;
            case "openNativeCreate":
                openNativeCreate(panel, req.has("type") ? req.get("type").getAsString() : null);
                break;
            case "dropCamera":
                dropCamera(panel,
                    req.has("cameraId") ? req.get("cameraId").getAsString() : "",
                    (req.has("tick") && !req.get("tick").isJsonNull()) ? req.get("tick").getAsInt() : -1);
                break;
            case "viewportMetrics":
                MCEFUI.setViewportMetrics(
                    req.has("topFrac") ? req.get("topFrac").getAsFloat() : 0.08f,
                    req.has("leftFrac") ? req.get("leftFrac").getAsFloat() : 0.14f,
                    req.has("bottomFrac") ? req.get("bottomFrac").getAsFloat() : 0.22f);
                break;
            case "newScene":
                createScene(panel, "", "");
                break;
            case "newSequence":
                createSequence(panel, null);
                break;
            case "newCharacter":
                createCharacter(panel, req);
                break;
            case "newAction":
                newAction(panel, req.has("index") ? req.get("index").getAsInt() : -1);
                break;
            case "openActionEditor":
                openActionEditor(panel,
                    req.has("index") ? req.get("index").getAsInt() : -1,
                    req.has("action") ? req.get("action").getAsInt() : -1);
                break;
            case "aeClose":
                actionEditorReplay = -1;
                actionEditorReplayId = null;
                actionEditorSelected = -1;
                actionEditorSelectedChannel = null;
                MCEFUI.injectScript("window.bbsState.actionEditorOpen=false;renderActionEditor(window.bbsState);renderAssetDetail(window.bbsState);");
                break;
            case "closeActionEditor":
                actionEditorReplay = -1;
                actionEditorReplayId = null;
                MCEFUI.injectScript("window.bbsState.actionEditorOpen=false;renderActionEditor(window.bbsState);renderAssetDetail(window.bbsState);");
                break;
            case "focusCharacter":
            {
                String fcId = req.has("id") ? req.get("id").getAsString() : null;
                int fcIdx = req.has("index") ? req.get("index").getAsInt() : -1;
                Film f = panel.getData();

                if (fcId != null && !fcId.isEmpty())
                {
                    int found = replayIndexById(f, fcId);
                    focusedCharacterId = found >= 0 ? fcId : null;
                    focusedCharacterIndex = found;
                }
                else
                {
                    focusedCharacterId = null;
                    focusedCharacterIndex = fcIdx;
                }

                if (focusedCharacterIndex >= 0)
                {
                    actionEditorLeftMode = "action";
                    actionEditorSelected = -1;
                    actionEditorSelectedChannel = null;
                }
                pushFocusedCharacterSlice(bridge.panel);
                break;
            }
            case "aeSetCharName":
            {
                int ri = editorReplayIndex();
                Film film = panel.getData();
                if (film != null && ri >= 0 && ri < film.replays.getList().size())
                {
                    String name = req.has("value") ? req.get("value").getAsString() : "";
                    BaseValue.edit(film, f -> f.replays.getList().get(ri).label.set(name));
                    panel.save();
                    refreshHtml();
                }
                break;
            }
            case "aeToggleCharEnabled":
                toggleCharField(panel, r -> r.enabled.set(!r.enabled.get()));
                break;
            case "aeToggleCharFp":
                toggleCharField(panel, r -> r.fp.set(!r.fp.get()));
                break;
            case "aeToggleCharShadow":
                toggleCharField(panel, r -> r.shadow.set(!r.shadow.get()));
                break;
            case "aeSetLayer":
                aeSetClipField(panel, req, c -> c.layer.set(req.get("value").getAsInt()));
                break;
            case "aeSetEnabled":
                aeSetClipField(panel, req, c -> c.enabled.set(req.get("value").getAsBoolean()));
                break;
            case "aeSetBlendIn":
                aeSetClipField(panel, req, c -> c.envelope.fadeIn.set((float) req.get("value").getAsDouble()));
                break;
            case "aeSetBlendOut":
                aeSetClipField(panel, req, c -> c.envelope.fadeOut.set((float) req.get("value").getAsDouble()));
                break;
            case "screenshot":
                screenshot(panel);
                break;
            case "aeSelectChar":
                actionEditorReplay = req.has("index") ? req.get("index").getAsInt() : actionEditorReplay;
                actionEditorLeftMode = "action";
                actionEditorSelected = -1;
                actionEditorSelectedChannel = null;
                pushActionEditorSlice(bridge);
                break;
            case "aeSetCharType":
                aeSetCharType(panel, req.has("type") ? req.get("type").getAsString() : "keyframe");
                break;
            case "aeSetLeftMode":
                actionEditorLeftMode = req.has("mode") ? req.get("mode").getAsString() : "action";
                MCEFUI.injectScript("window.bbsState.actionEditor.leftMode=" + GSON.toJson(actionEditorLeftMode)
                    + ";renderActionEditor(window.bbsState);renderAssetDetail(window.bbsState);");
                break;
            case "aeSelectAction":
                actionEditorSelected = req.has("ai") ? req.get("ai").getAsInt() : -1;
                actionEditorSelectedChannel = null;
                MCEFUI.injectScript("window.bbsState.actionEditor.selectedIndex=" + actionEditorSelected
                    + ";window.bbsState.actionEditor.selectedChannel=null;renderActionEditor(window.bbsState);renderAssetDetail(window.bbsState);");
                break;
            case "aeSelectChannel":
                actionEditorSelectedChannel = req.has("channel") ? req.get("channel").getAsString() : null;
                actionEditorSelected = -1;
                MCEFUI.injectScript("window.bbsState.actionEditor.selectedChannel=" + GSON.toJson(actionEditorSelectedChannel)
                    + ";window.bbsState.actionEditor.selectedIndex=-1;renderActionEditor(window.bbsState);renderAssetDetail(window.bbsState);");
                break;
            case "composeActionGroup":
                composeActionGroup(panel, req);
                break;
            case "addActionGroup":
                addActionGroup(panel, req);
                break;
            case "aeCreateAction":
                aeCreateAction(panel, req.has("preset") ? req.get("preset").getAsInt() : -1);
                break;
            case "aeDeleteAction":
                aeDeleteAction(panel, req.has("ai") ? req.get("ai").getAsInt() : -1);
                break;
            case "aeAddMaterial":
                aeAddMaterial(panel, req.has("type") ? req.get("type").getAsString() : null);
                break;
            case "aeDeleteMaterial":
                aeDeleteMaterial(panel, req.has("mi") ? req.get("mi").getAsInt() : -1);
                break;
            case "aeSelectMaterial":
                aeSelectMaterial(panel, req.has("mi") ? req.get("mi").getAsInt() : -1);
                break;
            case "aeSetMaterialType":
                aeSetMaterialField(panel, req, c -> c.type.set(req.get("value").getAsString()));
                break;
            case "aeSetMaterialTarget":
                aeSetMaterialField(panel, req, c -> c.target.set(req.get("value").getAsString()));
                break;
            case "aeSetMaterialSlot":
                aeSetMaterialField(panel, req, c -> c.slot.set(req.get("value").getAsString()));
                break;
            case "aeSetMaterialItem":
                aeSetMaterialField(panel, req, c -> c.item.set(req.get("value").getAsString()));
                break;
            case "aeSetMaterialTick":
                aeSetMaterialField(panel, req, c -> c.tick.set(req.get("value").getAsInt()));
                break;
            case "aeSetMaterialDuration":
                aeSetMaterialField(panel, req, c -> c.duration.set(req.get("value").getAsInt()));
                break;
            case "aeToggleMaterialEnabled":
                aeSetMaterialField(panel, req, c -> c.enabled.set(!c.enabled.get()));
                break;
            case "aeSetTitle":
                aeSetActionField(panel, req, c -> c.title.set(req.get("value").getAsString()));
                break;
            case "aeSetDuration":
                aeSetActionField(panel, req, c -> c.duration.set(req.get("value").getAsInt()));
                break;
            case "aeSetFrequency":
                aeSetActionField(panel, req, c -> c.frequency.set(req.get("value").getAsInt()));
                break;
            case "aeSetTick":
                aeSetActionField(panel, req, c -> c.tick.set(req.get("value").getAsInt()));
                break;
            case "aeSetMode":
                aeSetActionField(panel, req, c ->
                {
                    if (c instanceof LocomotionActionClip loc) loc.mode.set(req.get("value").getAsString());
                });
                break;
            case "aeSetStep":
                aeSetActionField(panel, req, c ->
                {
                    if (c instanceof LocomotionActionClip loc) loc.step.set((float) req.get("value").getAsDouble());
                });
                break;
            case "aeSaveScript":
                aeSetActionField(panel, req, c ->
                {
                    if (c instanceof ScriptActionClip sac) sac.script.set(req.get("value").getAsString());
                });
                break;
            case "aeAddParam":
                aeMutateSelectedScript(panel, req, sac -> sac.params.add(new ValueFloat(String.valueOf(sac.params.getAllTyped().size()), 0F)));
                break;
            case "aeSetParam":
                aeMutateSelectedScript(panel, req, sac ->
                {
                    int pi = req.has("pi") ? req.get("pi").getAsInt() : -1;
                    if (pi >= 0 && pi < sac.params.getAllTyped().size()) sac.params.getAllTyped().get(pi).set((float) req.get("value").getAsDouble());
                });
                break;
            case "aeDelParam":
                aeMutateSelectedScript(panel, req, sac ->
                {
                    int pi = req.has("pi") ? req.get("pi").getAsInt() : -1;
                    if (pi >= 0 && pi < sac.params.getAllTyped().size()) sac.params.getAllTyped().remove(pi);
                });
                break;
            case "aeExportScript":
                aeMutateSelectedScript(panel, req, sac ->
                {
                    File out = sac.exportScript(Minecraft.getInstance().gameDirectory);
                    MCEFUI.injectScript(out != null ? "toast('脚本已导出到 bbs_scripts/')" : "toast('脚本导出失败', true)");
                });
                break;
            case "aeAddKeyframe":
                aeAddKeyframe(panel,
                    req.has("channel") ? req.get("channel").getAsString() : "",
                    req.has("tick") ? req.get("tick").getAsInt() : 0,
                    req.has("value") ? req.get("value").getAsDouble() : 0D);
                break;
            case "aeDelKeyframe":
                aeDelKeyframe(panel,
                    req.has("channel") ? req.get("channel").getAsString() : "",
                    req.has("index") ? req.get("index").getAsInt() : -1);
                break;
            case "aeSetKeyframeValue":
                aeSetKeyframeValue(panel,
                    req.has("channel") ? req.get("channel").getAsString() : "",
                    req.has("index") ? req.get("index").getAsInt() : -1,
                    req.has("value") ? req.get("value").getAsDouble() : 0D);
                break;
            case "setCharacterType":
                setCharacterType(panel,
                    req.has("index") ? req.get("index").getAsInt() : -1,
                    req.has("type") ? req.get("type").getAsString() : "keyframe");
                break;
            case "setCharacterMasked":
                setCharacterMasked(panel,
                    req.has("index") ? req.get("index").getAsInt() : -1,
                    req.has("masked") && req.get("masked").getAsBoolean());
                break;
            case "deleteReplay":
                /* F0: prefer stable replayId; fall back to legacy index only. */
                deleteReplay(panel, req.has("replayId")
                    ? req.get("replayId").getAsString()
                    : resolveReplayIdByIndex(panel, req.has("index") ? req.get("index").getAsInt() : -1));
                break;
            case "dropActor":
                dropActor(panel,
                    req.has("replayId") ? req.get("replayId").getAsString() : "",
                    req.has("targetReplayId") ? req.get("targetReplayId").getAsString() : "");
                break;
            case "reorderTrack":
                reorderTrack(panel,
                    req.has("replayId") ? req.get("replayId").getAsString() : "",
                    req.has("targetReplayId") ? req.get("targetReplayId").getAsString() : "");
                break;
            case "placeActor":
                placeActor(panel,
                    req.has("replayId") ? req.get("replayId").getAsString() : "",
                    req.has("targetReplayId") ? req.get("targetReplayId").getAsString() : "");
                break;
            case "unplaceActor":
                unplaceActor(panel, req.has("replayId") ? req.get("replayId").getAsString() : "");
                break;
            case "setTrackProp":
                setTrackProp(panel,
                    req.has("replayId") ? req.get("replayId").getAsString() : "",
                    req.has("prop") ? req.get("prop").getAsString() : "",
                    req.has("value") ? req.get("value").getAsString() : "");
                break;
            case "deleteScene":
                deleteScene(panel, req.has("id") ? req.get("id").getAsString() : "");
                break;
            case "deleteSequence":
                deleteSequence(panel, req.has("id") ? req.get("id").getAsString() : "");
                break;
            case "shiftAllKeyframes":
            {
                int t = req.has("tick") ? req.get("tick").getAsInt() : 0;
                shiftAllKeyframes(panel, t);
                break;
            }
            case "renameSequence":
            {
                String id = req.has("id") ? req.get("id").getAsString() : "";
                SequenceManager sm = SequenceManager.get();
                Sequence seq = sm == null ? null : sm.getById(id);
                if (seq != null)
                {
                    JsonObject r = new JsonObject();
                    r.addProperty("kind", "sequence");
                    r.addProperty("target", id);
                    r.addProperty("current", seq.name);
                    renameReq = r;
                    refreshHtml();
                }
                break;
            }
            case "toggleReplay":
                toggleReplay(panel, req.has("index") ? req.get("index").getAsInt() : -1);
                break;
            case "setTool":
                setTool(panel, req.has("tool") ? req.get("tool").getAsString() : "");
                break;
            case "setCameraMode":
                panel.getController().setPov(req.get("mode").getAsInt());
                break;
            case "toggleControl":
                panel.getController().toggleControl();
                break;
            case "toggleRecord":
                if (panel.getController().isRecording())
                {
                    panel.getController().stopRecording();
                }
                else
                {
                    panel.getController().startRecording(null);
                }
                break;
            case "recordCharacter":
                recordCharacter(panel, req.has("index") ? req.get("index").getAsInt() : -1);
                break;
            case "recordToAction":
                recordToAction(panel, req.has("index") ? req.get("index").getAsInt() : -1);
                break;
            case "toggleInstantKeys":
                panel.getController().toggleInstantKeyframes();
                break;
            case "newEntity":
                createEntity(panel, null);
                break;
            case "newParticle":
                createParticle(panel, null);
                break;
            case "newItem":
                createItem(panel, "");
                break;
            case "enterSceneWorld":
                enterSceneWorld(panel);
                break;
            case "enterPreviewWorld":
                enterPreviewWorld(panel);
                break;
            case "exitPreviewWorld":
                exitPreviewWorld();
                break;
            case "newCamera":
                createCamera(panel, "");
                break;
            case "recordCamera":
            {
                int ci = req.has("index") ? req.get("index").getAsInt() : -1;

                recordCamera(panel, ci);
                break;
            }
            case "deleteCamera":
            {
                String camId = req.has("cameraId") ? req.get("cameraId").getAsString() : "";

                Minecraft.getInstance().execute(() ->
                {
                    Film film = panel.getData();

                    if (film != null)
                    {
                        Clip c = findCameraClip(film, camId);

                        if (c != null)
                        {
                            int idx = film.camera.get().indexOf(c);

                            if (idx >= 0)
                            {
                                final int i = idx;

                                BaseValue.edit(film, f -> f.camera.get().remove(i));
                                /* Also drop it from the timeline track (if placed). */
                                CameraTrackStore.remove(film.getId(), camId);
                                /* Persist immediately — without this the deletion only
                                 * marks the film dirty and the stale camera reappears
                                 * on next load. */
                                panel.save();
                                refreshHtml();
                            }
                        }
                    }
                });

                break;
            }
            case "renameCamera":
            {
                String camId = req.has("cameraId") ? req.get("cameraId").getAsString() : "";

                renameCamera(panel, camId);

                break;
            }
            case "exportCameraFile":
            {
                int ci = req.has("index") ? req.get("index").getAsInt() : -1;

                return exportCameraFile(panel, ci);
            }
            case "exportCameraGroupFile":
                return exportCameraGroupFile(panel, req.has("id") ? req.get("id").getAsString() : "");
            case "createCameraGroup":
            {
                String name = req.has("name") ? req.get("name").getAsString() : "";
                List<String> ids = new ArrayList<>();

                if (req.has("ids") && req.get("ids").isJsonArray())
                {
                    for (com.google.gson.JsonElement e : req.getAsJsonArray("ids"))
                    {
                        ids.add(e.getAsString());
                    }
                }

                createCameraGroup(panel, name, ids);

                break;
            }
            case "renameCameraGroup":
                renameCameraGroup(panel, req.has("id") ? req.get("id").getAsString() : "");
                break;
            case "renameCommit":
            {
                if (renameReq != null)
                {
                    String kind = renameReq.has("kind") ? renameReq.get("kind").getAsString() : "";
                    String name = req.has("name") ? req.get("name").getAsString() : "";

                    if (name != null)
                    {
                        Film film = panel.getData();

                        if (film != null)
                        {
                            if ("camera".equals(kind))
                            {
                                String camId = renameReq.has("target") ? renameReq.get("target").getAsString() : "";
                                final String cid = camId;
                                final String n = name;

                                BaseValue.edit(film, f ->
                                {
                                    Clip c = findCameraClip(f, cid);

                                    if (c != null)
                                    {
                                        c.title.set(n);
                                    }
                                });
                            }
                            else if ("cameragroup".equals(kind))
                            {
                                String id = renameReq.has("target") ? renameReq.get("target").getAsString() : "";
                                final String aid = id;
                                final String n = name;

                                BaseValue.edit(film, f ->
                                {
                                    CameraGroup g = f.cameraGroups.getByAssetId(aid);

                                    if (g != null)
                                    {
                                        g.name.set(n);
                                    }
                                });
                            }
                            else if ("sequence".equals(kind))
                            {
                                String id = renameReq.has("target") ? renameReq.get("target").getAsString() : "";
                                final String aid = id;
                                final String n = name;
                                SequenceManager sm = SequenceManager.get();

                                if (sm != null)
                                {
                                    Sequence s = sm.getById(aid);

                                    if (s != null)
                                    {
                                        sm.rename(s, n);
                                    }
                                }
                            }
                        }
                    }

                    renameReq = null;
                    panel.save();
                    refreshHtml();
                }

                break;
            }
            case "renameCancel":
            {
                renameReq = null;
                refreshHtml();
                break;
            }
            case "deleteCameraGroup":
                deleteCameraGroup(panel, req.has("id") ? req.get("id").getAsString() : "");
                break;
            case "openEquip":
                openEquipDialog(panel, req.has("index") ? req.get("index").getAsInt() : -1);
                break;
            case "equipClose":
                equipReplay = -1;
                MCEFUI.injectScript("document.getElementById('equipModal').classList.remove('show');");
                break;
            case "equip":
                equip(panel,
                    req.has("index") ? req.get("index").getAsInt() : -1,
                    req.has("slot") ? req.get("slot").getAsString() : "",
                    req.has("item") ? req.get("item").getAsString() : "");
                break;
            case "clipOp":
                clipOp(panel,
                    req.has("op") ? req.get("op").getAsString() : "",
                    req.has("index") ? req.get("index").getAsInt() : -1,
                    req.has("id") ? req.get("id").getAsString() : "");
                break;
            case "toBackpack":
                return toBackpack(panel,
                    req.has("type") ? req.get("type").getAsString() : "",
                    req.has("id") ? req.get("id").getAsString() : "");
            case "fromBackpack":
                return fromBackpack(panel, req.has("name") ? req.get("name").getAsString() : "");
            case "deleteBackpackItem":
                BackpackService.remove(req.has("name") ? req.get("name").getAsString() : "");
                panel.assetBin.refresh();
                refreshHtml();
                break;
            case "enterSequence":
                /* Select a sequence as the drop target for click-to-add. */
                activeSequenceId = req.has("id") ? req.get("id").getAsString() : null;
                refreshHtml();
                break;
            case "addToCurrent":
                /* Click an asset -> it auto-enters the active (or first)
                 * sequence. Characters map to the native "mcpr" ref type. */
                return addToCurrent(bridge,
                    req.has("type") ? req.get("type").getAsString() : "",
                    req.has("id") ? req.get("id").getAsString() : "");
            case "updateNow":
                UpdateChecker.applyUpdate();
                break;
            case "dismissUpdate":
                UpdateChecker.dismiss();
                break;
            default:
                return "{\"ok\":false,\"error\":\"unknown action " + action + "\"}";
        }

        return "{\"ok\":true}";
    }

    /* -------- backpack (cross-work asset library) -------- */

    /**
     * Put an asset of the current work into the global backpack. Scenes go
     * in with their whole film payload, sequences drag the scenes they
     * reference along, so the item is usable in any other work. Characters
     * (角色单独入背包) export the whole replay (form + keyframes + actions).
     */
    private static String toBackpack(UIFilmPanel panel, String type, String id)
    {
        if ("character".equals(type))
        {
            return toBackpackCharacter(panel, id);
        }

        if (Backpack.TYPE_CAMERA.equals(type))
        {
            int index = -1;

            try
            {
                index = Integer.parseInt(id);
            }
            catch (NumberFormatException ignored)
            {
            }

            String r = toBackpackCamera(panel, index);

            panel.assetBin.refresh();

            return r;
        }

        if (Backpack.TYPE_CAMERAGROUP.equals(type))
        {
            String r = toBackpackCameraGroup(panel, id);

            panel.assetBin.refresh();

            return r;
        }

        java.util.List<String> errors = BackpackService.put(type, id);

        panel.assetBin.refresh();

        if (!errors.isEmpty())
        {
            return "{\"ok\":false,\"error\":" + GSON.toJson(errors.get(0)) + "}";
        }

        return "{\"ok\":true}";
    }

    /** Export one character replay (form + keyframes + actions) into the backpack. */
    private static String toBackpackCharacter(UIFilmPanel panel, String id)
    {
        Film film = panel.getData();
        int index = -1;

        try
        {
            index = Integer.parseInt(id);
        }
        catch (NumberFormatException ignored)
        {
        }

        if (film == null || index < 0 || index >= film.replays.getList().size())
        {
            return "{\"ok\":false,\"error\":\"角色不存在\"}";
        }

        Replay replay = film.replays.getList().get(index);
        BBSProject project = ProjectManager.get() == null ? null : ProjectManager.get().getCurrent();

        if (project == null)
        {
            return "{\"ok\":false,\"error\":\"没有打开的作品\"}";
        }

        BaseType data = replay.toData();
        MapType document = data instanceof MapType map ? map : new MapType();

        java.util.List<String> errors = Backpack.exportDocument(project, replay.getName(), Backpack.TYPE_REPLAY, document);

        panel.assetBin.refresh();

        return errors.isEmpty() ? "{\"ok\":true}" : "{\"ok\":false,\"error\":" + GSON.toJson(errors.get(0)) + "}";
    }

    /** Take one backpack item into the current work (never overwrites). */
    private static String fromBackpack(UIFilmPanel panel, String name)
    {
        String type = Backpack.typeOf(name);

        if (Backpack.TYPE_REPLAY.equals(type))
        {
            return importReplay(panel, name);
        }

        if (Backpack.TYPE_CAMERA.equals(type))
        {
            return importCamera(panel, Backpack.readDocument(name));
        }

        if (Backpack.TYPE_CAMERAGROUP.equals(type))
        {
            return importCameraGroup(panel, Backpack.readDocument(name));
        }

        java.util.List<String> errors = BackpackService.take(name);

        panel.assetBin.refresh();

        if (!errors.isEmpty())
        {
            return "{\"ok\":false,\"error\":" + GSON.toJson(errors.get(0)) + "}";
        }

        refreshHtml();

        return "{\"ok\":true}";
    }

    /** Import a backpack character replay into the current film. */
    private static String importReplay(UIFilmPanel panel, String name)
    {
        BBSProject project = ProjectManager.get() == null ? null : ProjectManager.get().getCurrent();

        if (project == null)
        {
            return "{\"ok\":false,\"error\":\"没有打开的作品\"}";
        }

        java.util.List<String> errors = new ArrayList<>(Backpack.restoreAssets(project, name));

        MapType document = Backpack.readDocument(name);

        if (document == null)
        {
            errors.add("背包项 '" + name + "' 没有文档");
        }

        if (!errors.isEmpty())
        {
            return "{\"ok\":false,\"error\":" + GSON.toJson(errors.get(0)) + "}";
        }

        Film film = panel.getData();

        if (film == null)
        {
            return "{\"ok\":false,\"error\":\"没有打开的作品\"}";
        }

        Replay replay = film.replays.addReplay();

        replay.fromData(document);
        panel.replayEditor.setReplay(replay);
        panel.assetBin.refresh();
        panel.fillData();
        refreshHtml();

        return "{\"ok\":true}";
    }

    /* -------- tool / clip actions -------- */

    /** Create a new particle replay (like the entity panel's PARTICLE type). */
    private static void newParticle(UIFilmPanel panel, String name)
    {
        Film film = panel.getData();

        if (film == null)
        {
            return;
        }

        Replay replay = film.replays.addReplay();
        replay.form.set(new mchorse.bbs_mod.forms.forms.ParticleForm());
        replay.label.set(name);
        panel.replayEditor.setReplay(replay);
        panel.showPanel(1);
        panel.fillData();
        refreshHtml();
    }

    /**
     * Create a new item replay (an ItemForm you can place in the scene).
     * This replaces the old "new armor" idea - armor is equipment you put ON a
     * character (see {@link #equip}), not a standalone asset.
     */
    private static void newItem(UIFilmPanel panel, String itemId)
    {
        Film film = panel.getData();

        if (film == null)
        {
            return;
        }

        Replay replay = film.replays.addReplay();
        ItemForm form = new ItemForm();

        if (itemId != null && !itemId.isEmpty())
        {
            try
            {
                form.stack.set(new ItemStack(BuiltInRegistries.ITEM.get(Identifier.parse(itemId)).orElseThrow().value()));
            }
            catch (Throwable t)
            {
                /* keep empty stack if the id is invalid */
            }
        }

        replay.form.set(form);
        replay.label.set("物品");
        replay.actor.set(false);
        panel.replayEditor.setReplay(replay);
        panel.showPanel(1);
        panel.fillData();
        refreshHtml();
    }

    /**
     * Equip an item onto a character (actor) replay at the current playhead.
     * BBS stores actor equipment as keyframe channels on
     * {@code replay.keyframes} (armorHead/armorChest/armorLegs/armorFeet/
     * mainHand/offHand) - the same tracks ActionPlayer.apply() reads when
     * spawning the actor. An empty item id clears the slot.
     */
    private static void equip(UIFilmPanel panel, int index, String slot, String itemId)
    {
        Film film = panel.getData();

        if (film == null || index < 0 || index >= film.replays.getList().size())
        {
            return;
        }

        Replay replay = film.replays.getList().get(index);

        KeyframeChannel<ItemStack> channel = switch (slot)
        {
            case "head" -> replay.keyframes.armorHead;
            case "chest" -> replay.keyframes.armorChest;
            case "legs" -> replay.keyframes.armorLegs;
            case "feet" -> replay.keyframes.armorFeet;
            case "mainhand" -> replay.keyframes.mainHand;
            case "offhand" -> replay.keyframes.offHand;
            default -> null;
        };

        if (channel == null)
        {
            return;
        }

        final KeyframeChannel<ItemStack> ch = channel;
        final boolean clear = itemId == null || itemId.isEmpty();
        final String id = clear ? "" : itemId;

        Minecraft.getInstance().execute(() ->
        {
            BaseValue.edit(film, f ->
            {
                if (clear)
                {
                    ch.removeAll();
                }
                else
                {
                    try
                    {
                        Item item = BuiltInRegistries.ITEM.get(Identifier.parse(id)).orElseThrow().value();

                        ch.insert(0, new ItemStack(item));
                    }
                    catch (Throwable t)
                    {
                    }
                }
            });

            panel.save();
            refreshHtml();
        });
    }

    /* -------- camera actions -------- */

    /** Create a new idle camera at the current playhead tick. The name comes
     *  from the HTML modal (the old Swing dialog was hidden by the MCEF
     *  overlay). */
    /** Open (or lazily create) the dedicated preview world so the editor can
     *  show real Minecraft rendering + playback without the player having to
     *  join their own worlds. First run generates a small flat world named
     *  bbs_preview; afterwards it is reused. The LoadingOverlay is suppressed
     *  while this runs (see LoadingOverlayMixin). */
    private static void enterPreviewWorld(UIFilmPanel panel)
    {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level != null)
        {
            return;
        }

        markEnteringWorld();

        try
        {
            Path savesDir = mc.gameDirectory.toPath().resolve("saves");
            Path dstDir = savesDir.resolve(BBS_PREVIEW_WORLD);
            Path levelDat = dstDir.resolve("level.dat");

            boolean dstOk = Files.isDirectory(dstDir) && Files.isRegularFile(levelDat);

            if (!dstOk)
            {
                SceneManager scenes = SceneManager.get();
                Scene current = scenes == null ? null : scenes.getCurrent();
                String srcName = (current == null || current.background == null || current.background.isEmpty()) ? null : current.background;

                Path srcDir = srcName == null ? null : savesDir.resolve(srcName);

                /* If the scene has no background world, clone any existing
                 * valid single-player world as a template. createFreshLevel()
                 * is asynchronous and does not write level.dat immediately,
                 * so cloning a real world is the only reliable way to get a
                 * loadable bbs_preview on first editor open. */
                if (srcDir == null || !Files.isDirectory(srcDir) || !Files.isRegularFile(srcDir.resolve("level.dat")))
                {
                    srcDir = findFirstValidWorld(savesDir, dstDir);
                }

                if (srcDir != null && Files.isDirectory(srcDir) && Files.isRegularFile(srcDir.resolve("level.dat")))
                {
                    if (Files.isDirectory(dstDir)) deleteRecursive(dstDir);
                    copyWorld(srcDir, dstDir);
                    renamePreviewWorld(dstDir);
                }
                else
                {
                    /* No template world available to clone bbs_preview from.
                     * We deliberately do NOT call createFreshLevel("bbs_preview")
                     * here: that name is reserved/internal and must not be
                     * created by this path (creation under it is redirected in
                     * WorldOpenFlowsMixin). The preview world therefore depends
                     * on at least one existing singleplayer save to clone from. */
                    BBSMod.LOGGER.error("[EditorBridge] enterPreviewWorld: no template world to clone bbs_preview from");
                }

            }

            if (Files.isDirectory(dstDir) && Files.isRegularFile(levelDat))
            {
                owningPreviewWorld = true;
                mc.createWorldOpenFlows().openWorld(BBS_PREVIEW_WORLD, () ->
                {
                    clearEnteringWorld();

                    /* Guard against a late callback that fires after the user
                     * already exited: exitPreviewWorld resets owningPreviewWorld
                     * to false and tears down mc.level, so re-running the world
                     * entry here would re-flag an exited state and risk an
                     * mc.level NPE in spawnReplayActors. */
                    if (owningPreviewWorld)
                    {
                        reopenEditorUi();
                        inWorld = true;
                        spawnReplayActors(panel);
                    }
                });
            }
            else
            {
                clearEnteringWorld();
                owningPreviewWorld = false;
                BBSMod.LOGGER.error("[EditorBridge] enterPreviewWorld: bbs_preview still unavailable");
            }
        }
        catch (Throwable t)
        {
            clearEnteringWorld();
            owningPreviewWorld = false;
            BBSMod.LOGGER.error("[EditorBridge] enterPreviewWorld failed", t);
        }
    }

    private static void deleteRecursive(Path dir) throws java.io.IOException
    {
        if (!Files.exists(dir)) return;

        try (java.util.stream.Stream<Path> stream = Files.walk(dir))
        {
            for (Path p : (Iterable<Path>) stream::iterator)
            {
                Files.deleteIfExists(p);
            }
        }
    }

    /** Find the first valid single-player world in saves/ that is not the
     *  destination preview world itself. Returns null if none exists. */
    private static Path findFirstValidWorld(Path savesDir, Path exclude)
    {
        try (java.util.stream.Stream<Path> stream = Files.list(savesDir))
        {
            for (Path p : (Iterable<Path>) stream::iterator)
            {
                if (!Files.isDirectory(p)) continue;
                if (p.equals(exclude)) continue;
                if (Files.isRegularFile(p.resolve("level.dat"))) return p;
            }
        }
        catch (Throwable t)
        {
            BBSMod.LOGGER.error("[EditorBridge] findFirstValidWorld failed", t);
        }
        return null;
    }

    /** Recursively copy a world folder (saves/<src> -> saves/<dst>). */
    private static void copyWorld(Path src, Path dst) throws java.io.IOException
    {
        try (java.util.stream.Stream<Path> stream = Files.walk(src))
        {
            for (Path p : (Iterable<Path>) stream::iterator)
            {
                Path rel = src.relativize(p);
                Path target = dst.resolve(rel);

                if (Files.isDirectory(p))
                {
                    Files.createDirectories(target);
                }
                else
                {
                    Files.copy(p, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    /** Rename the preview world shown in the single-player world list so it is
     *  clearly identifiable as the BBS preview world (the cloned template keeps
     *  its original LevelName otherwise). Operates on the raw level.dat bytes
     *  to avoid depending on the NBT API surface which varies across versions. */
    private static void renamePreviewWorld(Path worldDir)
    {
        Path levelDat = worldDir.resolve("level.dat");

        if (!Files.isRegularFile(levelDat))
        {
            return;
        }

        try
        {
            byte[] raw = Files.readAllBytes(levelDat);
            byte[] key = "LevelName".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            int idx = indexOf(raw, key);

            if (idx < 0)
            {
                return;
            }

            int vlenPos = idx + key.length;

            if (vlenPos + 2 > raw.length)
            {
                return;
            }

            int oldLen = ((raw[vlenPos] & 0xFF) << 8) | (raw[vlenPos + 1] & 0xFF);
            int valStart = vlenPos + 2;
            int valEnd = valStart + oldLen;

            if (valEnd > raw.length)
            {
                return;
            }

            byte[] newName = "bbs 预览世界".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            byte[] newLen = { (byte) (newName.length >> 8), (byte) (newName.length & 0xFF) };

            byte[] out = new byte[valStart + newLen.length + newName.length + (raw.length - valEnd)];
            System.arraycopy(raw, 0, out, 0, valStart);
            System.arraycopy(newLen, 0, out, valStart, newLen.length);
            System.arraycopy(newName, 0, out, valStart + newLen.length, newName.length);
            System.arraycopy(raw, valEnd, out, valStart + newLen.length + newName.length, raw.length - valEnd);

            Files.write(levelDat, out);
        }
        catch (Throwable t)
        {
            BBSMod.LOGGER.error("[EditorBridge] renamePreviewWorld failed", t);
        }
    }

    /** Find a byte subsequence in a byte array. */
    private static int indexOf(byte[] haystack, byte[] needle)
    {
        if (needle.length == 0) return 0;

        for (int i = 0; i + needle.length <= haystack.length; i++)
        {
            boolean match = true;

            for (int j = 0; j < needle.length; j++)
            {
                if (haystack[i + j] != needle[j])
                {
                    match = false;
                    break;
                }
            }

            if (match) return i;
        }

        return -1;
    }

    /** Auto-enter the current scene's background world so the preview shows
     *  real Minecraft rendering (scene worlds are singleplayer saves). While
     *  this is running the LoadingOverlay is suppressed (see LoadingOverlayMixin)
     *  so the editor page stays visible instead of a loading screen.
     *  Uses a timestamp + timeout instead of a bare boolean so a stuck flag can
     *  never leak into a normal world join and suppress its loading animation. */
    /** Reserved internal preview world folder name. Must never appear in any
     *  user-facing world list and must not be created by users; see
     *  LevelStorageSourceMixin (hide) and WorldOpenFlowsMixin (reserve name). */
    public static final String BBS_PREVIEW_WORLD = "bbs_preview";

    private static volatile long enteringWorldUntil = 0L;
    private static final long ENTERING_WORLD_TIMEOUT_MS = 15000L;

    private static void markEnteringWorld()
    {
        enteringWorldUntil = System.currentTimeMillis() + ENTERING_WORLD_TIMEOUT_MS;
    }

    private static void clearEnteringWorld()
    {
        enteringWorldUntil = 0L;
    }

    public static boolean isEnteringWorld()
    {
        if (System.currentTimeMillis() < enteringWorldUntil)
        {
            return true;
        }

        /* Timed out: drop the stale flag so it cannot suppress the loading
         * overlay of a subsequent, unrelated (normal) world join. */
        enteringWorldUntil = 0L;
        return false;
    }

    /**
     * Re-open the dashboard (and the currently-edited film panel) as the active
     * screen so the HTML editor overlay stays visible after {@code openWorld()}
     * swapped the screen away from the dashboard. The dashboard renders as a
     * transparent overlay above the live world, so the game keeps rendering in
     * the background while the editor UI is usable.
     */
    private static void reopenEditorUi()
    {
        Minecraft mc = Minecraft.getInstance();

        try
        {
            UIDashboard dashboard = BBSModClient.getDashboard();
            UIFilmPanel panel = dashboard.getPanel(UIFilmPanel.class);

            if (panel != null && panel.getData() != null)
            {
                dashboard.setPanel(panel);
            }

            /* Defer one tick: openWorld's completion callback fires while MC is
             * still settling its own in-game screen, so opening the dashboard
             * immediately can be overridden by MC's screen transition. Running
             * it on the next client tick guarantees the editor overlay wins. */
            mc.execute(() ->
            {
                UIScreen.open(dashboard);

                /* B3: the reopened editor reuses the existing MCEF instance
                 * (the page is NOT reloaded), so window.bbsState is never
                 * refreshed automatically. Without this, owningPreviewWorld /
                 * inWorld stay stale and the "退出预览世界" button never appears
                 * after entering / leaving a preview or scene world. */
                refreshHtml();
            });
        }
        catch (Throwable t)
        {
            BBSMod.LOGGER.error("[EditorBridge] reopenEditorUi failed", t);
        }
    }

    private static void enterSceneWorld(UIFilmPanel panel)
    {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level != null)
        {
            return;
        }

        SceneManager scenes = SceneManager.get();
        Scene current = scenes == null ? null : scenes.getCurrent();
        String world = (current == null || current.background == null || current.background.isEmpty()) ? null : current.background;

        if (world != null)
        {
            markEnteringWorld();
            owningPreviewWorld = true;
            mc.createWorldOpenFlows().openWorld(world, () ->
            {
                clearEnteringWorld();

                if (owningPreviewWorld)
                {
                    reopenEditorUi();
                    inWorld = true;
                    spawnReplayActors(panel);
                }
            });
        }
    }

    /**
     * Instantiate the current film's replays as client-side ActorEntity
     * instances in the freshly opened world, then publish the
     * replayId -> entityId mapping so BaseFilmController.updateEntities() can
     * drive them every frame via applyClientActions(). This is the client-only
     * preview path (no server ActionPlayer / ClientNetwork replay packet);
     * it mirrors ActionPlayer.updateReplayEntities() spawn logic but runs on
     * the client Level directly.
     */
    private static void spawnReplayActors(UIFilmPanel panel)
    {
        Minecraft mc = Minecraft.getInstance();
        Film film = panel.getData();

        if (mc.level == null || film == null)
        {
            return;
        }

        String filmId = film.getId();

        /* Clean up previously spawned actors for this film before adding new ones,
           so re-entering the world does not leak orphan entities in the level. */
        Map<String, Integer> oldMap = BBSModClient.getFilms().actors.get(filmId);

        if (oldMap != null && mc.level != null)
        {
            for (Integer eid : oldMap.values())
            {
                if (eid == null)
                {
                    continue;
                }

                Entity old = mc.level.getEntity(eid);

                if (old != null)
                {
                    mc.level.removeEntity(old.getId(), Entity.RemovalReason.DISCARDED);
                }
            }
        }

        Map<String, Integer> map = new HashMap<>();

        try
        {
            List<Replay> list = film.replays.getList();

            for (int i = 0; i < list.size(); i++)
            {
                Replay replay = list.get(i);
                boolean isActor = replay.actor.get() || replay.fp.get();

                if (!isActor || !replay.enabled.get())
                {
                    continue;
                }

                ActorEntity actor = new ActorEntity(BBSMod.ACTOR_ENTITY, mc.level);

                Form f = replay.form.get();

                if (f != null)
                {
                    actor.setForm(FormUtils.copy(f));
                }

                actor.setPos(replay.keyframes.x.sample(0), replay.keyframes.y.sample(0), replay.keyframes.z.sample(0));
                mc.level.addFreshEntity(actor);
                map.put(replay.getId(), actor.getId());
            }

            BBSModClient.getFilms().updateActors(filmId, map);
            panel.updateActors(filmId, map);

            if (panel.getController() != null)
            {
                panel.getController().createEntities();
            }
        }
        catch (Throwable t)
        {
            BBSMod.LOGGER.error("[EditorBridge] spawnReplayActors failed", t);
        }
    }

    /**
     * Leave the preview world (or any world the editor opened) and restore the
     * editor overlay without joining a separate world. Mirrors closeEditor's
     * clearClientLevel but keeps the editor open.
     */
    public static void exitPreviewWorld()
    {
        if (exitingPreviewWorld)
        {
            return;
        }

        exitingPreviewWorld = true;

        try
        {
            Minecraft mc = Minecraft.getInstance();

            if (mc.level != null)
            {
                /* Remove any editor-spawned replay actors before tearing down the level. */
                UIDashboard dashboard = BBSModClient.getDashboard();
                UIFilmPanel filmPanel = dashboard == null ? null : dashboard.getPanel(UIFilmPanel.class);
                Film film = filmPanel == null ? null : filmPanel.getData();

                if (film != null)
                {
                    Map<String, Integer> oldMap = BBSModClient.getFilms().actors.get(film.getId());

                    if (oldMap != null)
                    {
                        for (Integer eid : oldMap.values())
                        {
                            if (eid == null)
                            {
                                continue;
                            }

                            Entity old = mc.level.getEntity(eid);

                            if (old != null)
                            {
                                mc.level.removeEntity(old.getId(), Entity.RemovalReason.DISCARDED);
                            }
                        }
                    }
                }

                mc.clearClientLevel(new net.minecraft.client.gui.screens.TitleScreen());
            }

            clearEnteringWorld();
            owningPreviewWorld = false;
            inWorld = false;
            reopenEditorUi();
        }
        finally
        {
            exitingPreviewWorld = false;
        }
    }

    private static void createCamera(UIFilmPanel panel, String name)
    {
        Film film = panel.getData();

        if (film == null)
        {
            return;
        }

        IdleClip idle = new IdleClip();
        Camera camera = new Camera();
        Minecraft mc = Minecraft.getInstance();

        if (mc.player != null)
        {
            camera.set(mc.player, MathUtils.toRad(mc.options.fov().get()));
        }

        if (name != null && !name.isEmpty())
        {
            idle.title.set(name);
        }
        else
        {
            idle.title.set("相机 " + (film.camera.get().size() + 1));
        }

        idle.tick.set(panel.getCursor());
        idle.layer.set(0);
        idle.duration.set(BBSSettings.getDefaultDuration());
        idle.fromCamera(camera);

        BaseValue.edit(film, f -> f.camera.addClip(idle));
        refreshHtml();
    }

    /** Open the in-page HTML rename modal for a camera clip. Replaces the
     *  Swing text-input dialog, which the MCEF exclusive-fullscreen overlay
     *  buries and prevents from ever popping up. */
    private static void renameCamera(UIFilmPanel panel, String cameraId)
    {
        Film film = panel.getData();

        if (film == null || cameraId == null || cameraId.isEmpty())
        {
            return;
        }

        Clip c = findCameraClip(film, cameraId);

        if (c == null)
        {
            return;
        }

        renameReq = new JsonObject();
        renameReq.addProperty("kind", "camera");
        renameReq.addProperty("target", cameraId);
        renameReq.addProperty("current", c.title.get());
        refreshHtml();
    }

    /* ------------------------------------------------------------------ */
    /* Camera assetization: serialize / export / import                    */
    /* ------------------------------------------------------------------ */

    private static String okJson()
    {
        return "{\"ok\":true}";
    }

    private static String errJson(String m)
    {
        return "{\"ok\":false,\"error\":" + GSON.toJson(m == null ? "error" : m) + "}";
    }

    private static String newId8()
    {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    /** Find a camera clip by its stable asset id. */
    /** Find a camera clip on the timeline track by its stable clip id. */
    private static Clip findCameraClip(Film film, String id)
    {
        if (film == null || id == null)
        {
            return null;
        }

        for (Clip c : film.camera.get())
        {
            if (id.equals(c.id.get()))
            {
                return c;
            }
        }

        return null;
    }

    private static Clip findClipByStableId(Film film, String assetId)
    {
        return findCameraClip(film, assetId);
    }

    /** Wrap a camera clip into a self-contained export document. */
    private static MapType cameraToDoc(Clip clip)
    {
        MapType doc = new MapType();

        doc.putString("type", Backpack.TYPE_CAMERA);
        doc.putString("id", clip.id.get());
        doc.putString("title", clip.title.get());
        doc.put("clip", BBSMod.getFactoryCameraClips().toData(clip));

        return doc;
    }

    /** Rebuild a camera clip from its export document (fresh stable id). */
    private static Clip cameraFromDoc(MapType doc)
    {
        MapType clipMap = doc == null ? null : doc.getMap("clip");

        if (clipMap == null)
        {
            return null;
        }

        Clip clip = BBSMod.getFactoryCameraClips().fromData(clipMap);

        if (clip != null)
        {
            clip.id.set(newId8());
        }

        return clip;
    }

    /** Export one camera clip to <exportFolder>/<title>.cambbs */
    private static String exportCameraFile(UIFilmPanel panel, int index)
    {
        Film film = panel.getData();

        if (film == null || index < 0 || index >= film.camera.get().size())
        {
            return errJson("相机不存在");
        }

        Clip clip = film.camera.get().get(index);

        try
        {
            MapType doc = cameraToDoc(clip);
            File folder = BBSMod.getExportFolder();

            Files.createDirectories(folder.toPath());

            String name = clip.title.get().isEmpty() ? ("camera_" + clip.id.get()) : clip.title.get();
            File file = new File(folder, Backpack.sanitize(name) + ".cambbs");

            Files.write(file.toPath(), DataStorageUtils.writeToBytes(doc), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            return okJson();
        }
        catch (Exception e)
        {
            return errJson(e.getMessage());
        }
    }

    /** Wrap a camera group into a self-contained export document (bundles
     *  the camera clips it references so the file is usable on its own). */
    private static MapType cameraGroupToDoc(CameraGroup group, Film film)
    {
        MapType doc = new MapType();

        doc.putString("type", Backpack.TYPE_CAMERAGROUP);
        doc.putString("id", group.id.get());
        doc.putString("name", group.name.get());

        ListType refs = new ListType();
        MapType clips = new MapType();

        for (String cid : group.cameraIds())
        {
            Clip clip = findClipByStableId(film, cid);

            refs.add(new StringType(cid));

            if (clip != null)
            {
                clips.put(cid, BBSMod.getFactoryCameraClips().toData(clip));
            }
        }

        doc.put("refs", refs);
        doc.put("clips", clips);

        return doc;
    }

    /** Export a camera group to <exportFolder>/<name>.camgrp */
    private static String exportCameraGroupFile(UIFilmPanel panel, String assetId)
    {
        Film film = panel.getData();
        CameraGroup group = film == null ? null : film.cameraGroups.getByAssetId(assetId);

        if (group == null)
        {
            return errJson("相机组不存在");
        }

        try
        {
            MapType doc = cameraGroupToDoc(group, film);
            File folder = BBSMod.getExportFolder();

            Files.createDirectories(folder.toPath());

            String name = group.name.get().isEmpty() ? ("camera_group_" + group.id.get()) : group.name.get();
            File file = new File(folder, Backpack.sanitize(name) + ".camgrp");

            Files.write(file.toPath(), DataStorageUtils.writeToBytes(doc), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            return okJson();
        }
        catch (Exception e)
        {
            return errJson(e.getMessage());
        }
    }

    /** Import a camera clip document into the current film (fresh id). */
    private static String importCamera(UIFilmPanel panel, MapType doc)
    {
        Clip clip = cameraFromDoc(doc);

        if (clip == null)
        {
            return errJson("相机数据损坏");
        }

        Film film = panel.getData();

        if (film == null)
        {
            return errJson("没有打开的作品");
        }

        BaseValue.edit(film, f -> f.camera.addClip(clip));
        refreshHtml();

        return okJson();
    }

    /** Import a camera group document: bundle its camera clips into the film
     *  and recreate the group referencing the (new) clip ids. */
    private static String importCameraGroup(UIFilmPanel panel, MapType doc)
    {
        Film film = panel.getData();

        if (film == null)
        {
            return errJson("没有打开的作品");
        }

        if (doc == null)
        {
            return errJson("相机组数据损坏");
        }

        CameraGroup group = new CameraGroup("tmp");

        group.id.set(newId8());
        group.name.set(doc.getString("name", ""));

        MapType clips = doc.getMap("clips");
        BaseType refsRaw = doc.get("refs");

        if (refsRaw != null && refsRaw.isList())
        {
            for (BaseType ref : refsRaw.asList())
            {
                String cid = ref.isString() ? ref.asString() : (ref.isMap() ? ref.asMap().getString("id", "") : "");

                if (cid == null || cid.isEmpty())
                {
                    continue;
                }

                MapType clipMap = clips == null ? null : clips.getMap(cid);

                if (clipMap == null)
                {
                    /* Camera wasn't bundled in the export (or was missing at
                     * export time). Skip rather than add a dangling reference
                     * that can never resolve in this work. */
                    continue;
                }

                Clip clip = BBSMod.getFactoryCameraClips().fromData(clipMap);

                if (clip == null)
                {
                    /* Corrupt clip payload; skip to avoid a dead link. */
                    continue;
                }

                clip.id.set(newId8());

                final Clip c = clip;
                BaseValue.edit(film, f -> f.camera.addClip(c));

                group.addCamera(clip.id.get());
            }
        }

        final CameraGroup g = group;

        BaseValue.edit(film, f -> f.cameraGroups.add(g));
        refreshHtml();

        return okJson();
    }

    /** Put a camera clip into the global backpack. */
    private static String toBackpackCamera(UIFilmPanel panel, int index)
    {
        Film film = panel.getData();

        if (film == null || index < 0 || index >= film.camera.get().size())
        {
            return errJson("相机不存在");
        }

        Clip clip = film.camera.get().get(index);
        BBSProject project = ProjectManager.get() == null ? null : ProjectManager.get().getCurrent();

        if (project == null)
        {
            return errJson("没有打开的作品");
        }

        java.util.List<String> errors = Backpack.exportDocument(project, clip.title.get().isEmpty() ? ("camera_" + clip.id.get()) : clip.title.get(), Backpack.TYPE_CAMERA, cameraToDoc(clip));

        panel.assetBin.refresh();

        return errors.isEmpty() ? okJson() : errJson(errors.get(0));
    }

    /** Put a camera group into the global backpack (bundles its cameras). */
    private static String toBackpackCameraGroup(UIFilmPanel panel, String assetId)
    {
        Film film = panel.getData();
        CameraGroup group = film == null ? null : film.cameraGroups.getByAssetId(assetId);

        if (group == null)
        {
            return errJson("相机组不存在");
        }

        BBSProject project = ProjectManager.get() == null ? null : ProjectManager.get().getCurrent();

        if (project == null)
        {
            return errJson("没有打开的作品");
        }

        java.util.List<String> errors = Backpack.exportDocument(project, group.name.get().isEmpty() ? ("camera_group_" + group.id.get()) : group.name.get(), Backpack.TYPE_CAMERAGROUP, cameraGroupToDoc(group, film));

        panel.assetBin.refresh();

        return errors.isEmpty() ? okJson() : errJson(errors.get(0));
    }

    /** Build a camera group from a list of camera stable ids. */
    private static void createCameraGroup(UIFilmPanel panel, String name, List<String> cameraIds)
    {
        Film film = panel.getData();

        if (film == null || cameraIds == null || cameraIds.isEmpty())
        {
            return;
        }

        CameraGroup group = new CameraGroup("tmp");

        group.id.set(newId8());
        group.name.set(name == null || name.isEmpty() ? ("相机组 " + (film.cameraGroups.getList().size() + 1)) : name);

        for (String cid : cameraIds)
        {
            /* Only keep ids that actually resolve to a camera clip, so a
             * selection that mixes cameras with other clips can't create a
             * group full of dangling references. */
            if (findClipByStableId(film, cid) != null)
            {
                group.addCamera(cid);
            }
        }

        final CameraGroup g = group;

        BaseValue.edit(film, f -> f.cameraGroups.add(g));
        refreshHtml();
    }

    /** Open the in-page HTML rename modal for a camera group. Replaces the
     *  Swing text-input dialog, which the MCEF exclusive-fullscreen overlay
     *  buries and prevents from ever popping up. */
    private static void renameCameraGroup(UIFilmPanel panel, String assetId)
    {
        Film film = panel.getData();
        CameraGroup group = film == null ? null : film.cameraGroups.getByAssetId(assetId);

        if (group == null)
        {
            return;
        }

        renameReq = new JsonObject();
        renameReq.addProperty("kind", "cameragroup");
        renameReq.addProperty("target", assetId);
        renameReq.addProperty("current", group.name.get());
        refreshHtml();
    }

    /** Delete a camera group (cameras it referenced are NOT removed). */
    private static void deleteCameraGroup(UIFilmPanel panel, String assetId)
    {
        Film film = panel.getData();

        if (film == null)
        {
            return;
        }

        BaseValue.edit(film, f ->
        {
            CameraGroup g = f.cameraGroups.getByAssetId(assetId);

            if (g != null)
            {
                f.cameraGroups.getList().remove(g);
                f.cameraGroups.sync();
            }
        });

        /* Persist immediately, same reason as deleteCamera. */
        panel.save();
        refreshHtml();
    }

    /** Place a library camera onto the timeline track at the dropped tick. */
    private static void dropCamera(UIFilmPanel panel, String cameraId, int tick)
    {
        Film film = panel.getData();

        if (film == null || cameraId == null || cameraId.isEmpty())
        {
            return;
        }

        Clip c = findCameraClip(film, cameraId);

        if (c == null)
        {
            return;
        }

        int t = Math.max(0, tick);
        String filmId = film.getId();

        BaseValue.edit(film, f ->
        {
            Clip cc = findCameraClip(f, cameraId);

            if (cc != null)
            {
                cc.tick.set(t);
            }
        });

        /* Ensure the track order exists before inserting (mirrors the
         * getStateJson legacy-seed) so a drop never drops existing cameras. */
        if (!CameraTrackStore.has(filmId))
        {
            List<String> allCam = new ArrayList<>();

            for (Clip cl : film.camera.get())
            {
                allCam.add(cl.id.get());
            }

            CameraTrackStore.set(filmId, allCam);
        }

        int pos = CameraTrackStore.get(filmId).size();
        CameraTrackStore.insert(filmId, cameraId, pos);
        panel.save();
        refreshHtml();
    }

    /** Toggle in-world path recording for the camera at the given index. */
    private static void recordCamera(UIFilmPanel panel, int index)
    {
        Film film = panel.getData();

        if (film == null || index < 0 || index >= film.camera.get().size())
        {
            return;
        }

        Clip c = film.camera.get().get(index);

        if (!isBaseCameraType(c))
        {
            return;
        }

        PathClip path;

        if (c instanceof PathClip)
        {
            path = (PathClip) c;
        }
        else
        {
            path = new PathClip();
            path.tick.set(c.tick.get());
            path.layer.set(c.layer.get());
            path.duration.set(c.duration.get());
            path.title.set(c.title.get());
            path.enabled.set(c.enabled.get());

            final Clip old = c;

            BaseValue.edit(film, f ->
            {
                int i = f.camera.get().indexOf(old);

                if (i >= 0)
                {
                    f.camera.get().remove(i);
                    f.camera.get().add(i, path);
                }
            });
        }

        CameraPathRecorder.toggle(path, panel);
    }

    /** Phase C: 把该角色设为当前，进入世界录制走位（写回该角色 keyframes）。
     *  走完后按 右 Alt（record_replay 键位）结束并写回。 */
    private static void recordCharacter(UIFilmPanel panel, int index)
    {
        Minecraft.getInstance().execute(() ->
        {
            Film film = panel.getData();

            if (film == null || index < 0 || index >= film.replays.getList().size())
            {
                return;
            }

            Replay replay = film.replays.getList().get(index);

            panel.replayEditor.setReplay(replay);
            panel.getController().startRecording(List.of("outside"));
        });
    }

    /** Phase D: 把该角色已录制的走位轨迹打包成可复用动作（RecordedPathActionClip），
     *  加入该角色的动作列表，可拖入时间轴。 */
    private static void recordToAction(UIFilmPanel panel, int index)
    {
        Minecraft.getInstance().execute(() ->
        {
            Film film = panel.getData();

            if (film == null || index < 0 || index >= film.replays.getList().size())
            {
                return;
            }

            Replay replay = film.replays.getList().get(index);

            if (replay.keyframes.x.isEmpty())
            {
                return;
            }

            RecordedPathActionClip clip = new RecordedPathActionClip();

            clip.copyFrom(replay.keyframes);
            clip.tick.set(0);
            clip.duration.set(Math.max(1, clip.pathDuration()));

            BaseValue.edit(film, f -> replay.actions.addClip(clip));
        });
    }

    /* -------- native (OS window) dialogs -------- */

    /**
     * Pop a real OS window to collect the scene name + background world, then
     * create the scene. Replaces {@code UIFilmPanel.newScene()}'s in-game
     * overlay panel, which renders *under* the MCEF HTML texture and is
     * therefore buried/unclickable.
     */
    /** Push the latest editor state into the HTML page so the asset bin /
     *  timeline reflect freshly created (or otherwise changed) objects. The
     *  native assetBin.refresh() only updates the native layout -- when the
     *  HTML editor is the active UI we must also refresh the page.
     *
     *  B1: instead of rebuilding the whole page synchronously on every call,
     *  we only arm {@link #pendingRefresh}. The real {@code pushState()} is
     *  drained once per frame by the {@link ClientTickEvents#END_CLIENT_TICK}
     *  hook, so a burst of calls in a single frame collapses into one rebuild. */
    private static void refreshHtml()
    {
        pendingRefresh = true;
        ensureTickHook();
    }

    /** B1: explicit merged-refresh entry point (semantically identical to
     *  {@link #refreshHtml()}); used where the intent is "re-render soon, at
     *  most once per frame". */
    public static void scheduleRefresh()
    {
        pendingRefresh = true;
        ensureTickHook();
    }

    /** B1: register the per-tick consumer that drains {@link #pendingRefresh}
     *  at most once per client tick. Registered lazily and exactly once. Runs
     *  on the client thread, so calling {@link MCEFUI#pushState()} directly is
     *  safe (it only touches the CEF browser and reads panel state). This does
     *  NOT touch the render pipeline, so Vulkan / Iris+Sodium stay unaffected. */
    private static void ensureTickHook()
    {
        if (tickHookRegistered)
        {
            return;
        }

        tickHookRegistered = true;

        ClientTickEvents.END_CLIENT_TICK.register(client ->
        {
            if (instance != null && instance.panel != null && instance.panel.isRunning())
            {
                int c = instance.panel.getCursor();
                MCEFUI.injectScript("window.bbsState.cursor=" + c + ";if(window.bbsState&&typeof renderTimeline==='function')renderTimeline(window.bbsState);");
            }

            if (pendingRefresh)
            {
                pendingRefresh = false;
                MCEFUI.pushState();
            }
        });
    }

    /* -------- B2: lightweight view-state patches -------- */

    /**
     * B2: Build the {@code focusedCharacter} state slice (or null when no
     *  character is focused). Mirrors the inline slice previously emitted by
     *  {@link #getStateJson(EditorBridge)} so it can be pushed on its own
     *  without rebuilding the whole editor.
     */
    private static JsonObject buildFocusedCharacterSlice(UIFilmPanel panel)
    {
        Film film = panel.getData();

        if (focusedCharacterId == null || film == null)
        {
            return null;
        }

        int idx = replayIndexById(film, focusedCharacterId);

        if (idx < 0)
        {
            focusedCharacterId = null;
            focusedCharacterIndex = -1;

            return null;
        }

        Replay fcr = film.replays.getList().get(idx);
        JsonObject focused = new JsonObject();
        focused.addProperty("index", idx);
        focused.addProperty("id", fcr.getId());
        focused.addProperty("label", fcr.getName());
        focused.addProperty("characterType", fcr.characterType.get());
        focused.addProperty("enabled", fcr.enabled.get());
        focused.addProperty("masked", fcr.masked.get());
        focused.addProperty("fp", fcr.fp.get());
        focused.addProperty("shadow", fcr.shadow.get());
        focused.addProperty("nameTag", fcr.nameTag.get());
        focused.add("props", TrackPropStore.get(film.getId(), fcr.getId()).toJson());

        return focused;
    }

    /**
     * B2: Push only the action-editor slice and re-render it (plus the asset
     *  detail panel, which reuses the same slice). Used for pure in-editor
     *  selection/navigation changes that don't alter the underlying data model
     *  -- far cheaper than a full {@code pushState()}.
     */
    private static void pushActionEditorSlice(EditorBridge bridge)
    {
        JsonObject ae = buildActionEditorState(bridge);

        if (ae == null)
        {
            MCEFUI.injectScript("window.bbsState.actionEditorOpen=false;renderActionEditor(window.bbsState);renderAssetDetail(window.bbsState);");

            return;
        }

        MCEFUI.injectScript("window.bbsState.actionEditor=" + GSON.toJson(ae)
            + ";window.bbsState.actionEditorOpen=true;renderActionEditor(window.bbsState);renderAssetDetail(window.bbsState);");
    }

    /** B2: Push only the focused-character slice and re-render the asset-detail panel. */
    private static void pushFocusedCharacterSlice(UIFilmPanel panel)
    {
        JsonObject focused = buildFocusedCharacterSlice(panel);

        if (focused == null)
        {
            MCEFUI.injectScript("window.bbsState.focusedCharacter=null;renderAssetDetail(window.bbsState);");

            return;
        }

        MCEFUI.injectScript("window.bbsState.focusedCharacter=" + GSON.toJson(focused) + ";renderAssetDetail(window.bbsState);");
    }

    /** B2: Toggle only the play/pause highlight (the {@code running} flag). */
    private static void patchRunning(boolean running)
    {
        MCEFUI.injectScript("window.bbsState.running=" + (running ? "true" : "false") + ";renderViewportToolbar(window.bbsState);");
    }

    private static void createScene(UIFilmPanel panel, String name, String world)
    {
        Minecraft.getInstance().execute(() ->
        {
            SceneManager scenes = SceneManager.get();

            if (scenes == null)
            {
                return;
            }

            String sceneName = (name == null || name.isEmpty()) ? ("Scene " + (scenes.getScenes().size() + 1)) : name;
            Scene scene = scenes.create(sceneName, world == null ? "" : world);

            panel.assetBin.refresh();
            panel.openScene(scene);
            refreshHtml();
        });
    }

    /**
     * Pop a real OS window to collect character/entity options (name, form
     * type, actor/shadow/looping), then create the replay. Replaces
     * {@code UIFilmPanel.newCharacter()}'s buried overlay panel.
     */
    private static void newAction(UIFilmPanel panel, int index)
    {
        Film film = panel.getData();

        if (film == null || index < 0 || index >= film.replays.getList().size())
        {
            return;
        }

        /* Open the HTML in-page action editor for this character (the Swing
         * window is gone - it was buried by the fullscreen MCEF editor). */
        actionEditorReplay = index;
        actionEditorReplayId = replayIdByIndex(film, index);
        focusedCharacterIndex = index;
        focusedCharacterId = actionEditorReplayId;
        actionEditorLeftMode = "action";
        actionEditorSelected = -1;
        actionEditorSelectedChannel = null;
        refreshHtml();
    }

    /** Create a character/entity directly from the HTML modal fields. Replaces
     *  the Swing NativeDialog, which the MCEF overlay buried. */
    private static void createCharacter(UIFilmPanel panel, JsonObject req)
    {
        String name = req.has("name") ? req.get("name").getAsString() : "";
        String type = req.has("type") ? req.get("type").getAsString() : "MOB";
        String charType = req.has("charType") ? req.get("charType").getAsString() : "action";
        String model = req.has("model") ? req.get("model").getAsString() : "";
        boolean actor = !req.has("actor") || req.get("actor").getAsBoolean();
        boolean shadow = !req.has("shadow") || req.get("shadow").getAsBoolean();
        boolean looping = req.has("looping") && req.get("looping").getAsBoolean();

        createCharacter(panel, name, type, charType, model, actor, shadow, looping);
    }

    /** Create a character/entity from a {@link NativeDialog.CharResult}
     *  (the OS-native new-character window). */
    private static void createCharacter(UIFilmPanel panel, NativeDialog.CharResult r)
    {
        if (r == null)
        {
            return;
        }

        createCharacter(panel, r.name, r.type, r.charType, "", r.actor, r.shadow, r.looping);
    }

    private static void createCharacter(UIFilmPanel panel, String name, String type,
        String charType, String model, boolean actor, boolean shadow, boolean looping)
    {
        Minecraft.getInstance().execute(() ->
        {
            Film film = panel.getData();

            if (film == null)
            {
                panel.getContext().notifyError(UIKeys.ASSETS_NEED_SCENE);

                return;
            }

            Replay replay = film.replays.addReplay();

            Form form = switch (type)
            {
                case "MODEL" -> new ModelForm();
                case "PARTICLE" -> new ParticleForm();
                case "BLOCK" -> new BlockForm();
                default ->
                {
                    MobForm mf = new MobForm();

                    if (model != null && !model.isEmpty())
                    {
                        if (model.startsWith("group:"))
                        {
                            String g = model.substring("group:".length());

                            mf.mobGroup.set(g);

                            String[] parts = g.split("\\|");

                            if (parts.length > 0)
                            {
                                mf.mobID.set(parts[0].trim());
                            }
                        }
                        else
                        {
                            mf.mobID.set(model);
                        }
                    }

                    yield mf;
                }
            };

            replay.form.set(form);

            if (name != null && !name.isEmpty())
            {
                replay.label.set(name);
            }

            replay.actor.set(actor);
            replay.shadow.set(shadow);
            replay.looping.set(looping ? 1 : 0);
            replay.characterType.set("action".equals(charType) ? "action" : ("playback".equals(charType) ? "playback" : "keyframe"));

            panel.replayEditor.setReplay(replay);
            panel.showPanel(1);
            panel.fillData();
            refreshHtml();
        });
    }

    /** Create an item replay directly (id from the HTML modal). */
    private static void createItem(UIFilmPanel panel, String itemId)
    {
        Minecraft.getInstance().execute(() -> newItem(panel, itemId == null ? "" : itemId));
    }

    /**
     * Open the real OS-native "new X" window for the requested type, then run
     * the matching BBS creator once the user confirms. The browser blit is
     * suspended while the native window is open so it is never buried under
     * the full-screen HTML editor (the earlier 2.0.237 HTML-modal workaround
     * is hereby reverted in favour of a proper system-level dialog).
     */
    /**
     * Exclusive (real) fullscreen on Windows owns the display and hides other
     * top-level windows, so the OS-level Swing create dialogs would be
     * invisible. Drop to windowed mode so the native window can float above
     * the game. Done here (not only in openHtmlEditor) so it covers every
     * editor entry path - including dashboard -> editor navigation that
     * switches the CEF page without calling openHtmlEditor.
     */
    private static void dropFullscreenForNativeDialog()
    {
        var window = Minecraft.getInstance().getWindow();

        if (window.isFullscreen())
        {
            window.toggleFullScreen();
        }
    }

    private static void openNativeCreate(UIFilmPanel panel, String type)
    {
        if (type == null || type.isEmpty())
        {
            return;
        }

        dropFullscreenForNativeDialog();
        MCEFUI.setBrowserSuspended(true);

        Runnable resume = () -> MCEFUI.setBrowserSuspended(false);

        switch (type)
        {
            case "newScene":
                NativeDialog.sceneDialog((name, world) ->
                {
                    if (name != null)
                    {
                        Minecraft.getInstance().execute(() -> createScene(panel, name, world));
                    }

                    resume.run();
                });
                break;
            case "newCharacter":
                NativeDialog.characterDialog(r ->
                {
                    Minecraft.getInstance().execute(() -> createCharacter(panel, r));
                    resume.run();
                });
                break;
            case "newItem":
                NativeDialog.itemDialog(id ->
                {
                    Minecraft.getInstance().execute(() -> createItem(panel, id));
                    resume.run();
                });
                break;
            case "newCamera":
                NativeDialog.textInput("新建相机", "相机名称：", "", name ->
                {
                    if (name != null && !name.isEmpty())
                    {
                        Minecraft.getInstance().execute(() -> createCamera(panel, name));
                    }

                    resume.run();
                });
                break;
            case "newSequence":
                NativeDialog.textInput("新建序列", "序列名称：", "", name ->
                {
                    if (name != null && !name.isEmpty())
                    {
                        Minecraft.getInstance().execute(() -> createSequence(panel, name));
                    }

                    resume.run();
                });
                break;
            case "newEntity":
                NativeDialog.textInput("新建实体", "实体名称：", "实体", name ->
                {
                    if (name != null && !name.isEmpty())
                    {
                        Minecraft.getInstance().execute(() -> createEntity(panel, name));
                    }

                    resume.run();
                });
                break;
            case "newParticle":
                NativeDialog.textInput("新建粒子", "粒子名称：", "粒子", name ->
                {
                    if (name != null && !name.isEmpty())
                    {
                        Minecraft.getInstance().execute(() -> createParticle(panel, name));
                    }

                    resume.run();
                });
                break;
            default:
                resume.run();
        }
    }

    /** Pop a real OS equipment window for the selected actor replay. */
    private static void openEquipDialog(UIFilmPanel panel, int index)
    {
        Film film = panel.getData();

        if (film == null || index < 0 || index >= film.replays.getList().size())
        {
            return;
        }

        /* Push the equipment editor as an in-page HTML modal instead of a
         * Swing JDialog. The Swing window is buried by the OS under exclusive
         * fullscreen, so the button looked dead; the HTML modal stays visible
         * in both windowed and fullscreen modes. The NativeDialog.equipDialog
         * method is kept for any remaining vanilla (windowed) callers. */
        equipReplay = index;
        refreshHtml();
    }

    /**
     * Create a new Sequence (a higher-level container that can reference
     * scenes but NOT characters/entities), referencing the current scene.
     * Name comes from the HTML modal.
     */
    private static void createSequence(UIFilmPanel panel, String name)
    {
        if (name == null || name.isEmpty())
        {
            return;
        }

        SceneManager scenes = SceneManager.get();

        if (scenes == null || scenes.getCurrent() == null)
        {
            return;
        }

        Scene current = scenes.getCurrent();

        Minecraft.getInstance().execute(() ->
        {
            SequenceManager sequences = SequenceManager.get();
            Sequence sequence = sequences.create(name);

            panel.fillData();
            refreshHtml();
        });
    }

    /** Create a new entity replay directly (name from the HTML modal). */
    private static void createEntity(UIFilmPanel panel, String name)
    {
        if (name == null || name.isEmpty())
        {
            return;
        }

        Minecraft.getInstance().execute(() ->
        {
            Film film = panel.getData();

            if (film == null)
            {
                return;
            }

            panel.getController().createEntities();

            java.util.List<Replay> list = film.replays.getList();

            if (!list.isEmpty())
            {
                Replay replay = list.get(list.size() - 1);

                replay.label.set(name);
                panel.replayEditor.setReplay(replay);
                panel.showPanel(1);
                panel.fillData();
                refreshHtml();
            }
        });
    }

    /** Create a new particle replay directly (name from the HTML modal). */
    private static void createParticle(UIFilmPanel panel, String name)
    {
        if (name == null || name.isEmpty())
        {
            return;
        }

        Minecraft.getInstance().execute(() -> newParticle(panel, name));
    }

    /** Switch a character between keyframe-driven and action-driven mode. */
    private static void setCharacterType(UIFilmPanel panel, int index, String type)
    {
        Film film = panel.getData();

        if (film == null || index < 0 || index >= film.replays.getList().size())
        {
            return;
        }

        final String t = "action".equals(type) ? "action" : "keyframe";

        BaseValue.edit(film, f -> f.replays.getList().get(index).characterType.set(t));
    }

    /** Toggle the mask flag of a character: masked characters are not
     *  rendered (created) by BaseFilmController.createEntities. */
    private static void setCharacterMasked(UIFilmPanel panel, int index, boolean masked)
    {
        Film film = panel.getData();

        if (film == null || index < 0 || index >= film.replays.getList().size())
        {
            return;
        }

        BaseValue.edit(film, f -> f.replays.getList().get(index).masked.set(masked));
        refreshHtml();
    }

    /** Open the standalone action-editor window for a character (Replay). */
    private static void openActionEditor(UIFilmPanel panel, int index, int actionIndex)
    {
        Film film = panel.getData();

        if (film == null || film.replays.getList().isEmpty())
        {
            MCEFUI.injectScript("toast('请先创建至少一个角色', true)");
            return;
        }

        if (index < 0 || index >= film.replays.getList().size())
        {
            /* Prefer the first action character, else the first character. */
            index = 0;
            for (int i = 0; i < film.replays.getList().size(); i++)
            {
                if ("action".equals(film.replays.getList().get(i).characterType.get()))
                {
                    index = i;
                    break;
                }
            }
        }

        /* Render the action editor as an in-page HTML modal instead of a Swing
         * window - the Swing JDialog was buried by the OS in exclusive
         * fullscreen, so it never appeared. The modal lives inside editor_ui.html
         * and is driven entirely by window.bbsState.actionEditor. */
        actionEditorReplay = index;
        actionEditorReplayId = replayIdByIndex(film, index);
        focusedCharacterIndex = index;
        focusedCharacterId = actionEditorReplayId;
        actionEditorLeftMode = "action";
        actionEditorSelected = actionIndex >= 0 && actionIndex < film.replays.getList().get(index).actions.get().size() ? actionIndex : -1;
        actionEditorSelectedChannel = null;
        refreshHtml();
    }

    /* -------- action editor (HTML in-page modal) -------- */

    /** Build the action-editor state pushed to the page as
     *  window.bbsState.actionEditor. Returns null when the editor is closed. */
    private static JsonObject buildActionEditorState(EditorBridge bridge)
    {
        Film film = bridge.panel.getData();

        int idx = editorReplayIndex();

        if (film == null || idx < 0 || idx >= film.replays.getList().size())
        {
            return null;
        }

        Replay replay = film.replays.getList().get(idx);
        JsonObject o = new JsonObject();

        o.addProperty("replayIndex", idx);
        o.addProperty("replayLabel", replay.getName());
        String type = replay.characterType.get();
        o.addProperty("characterType", type);
        o.addProperty("leftMode", actionEditorLeftMode);
        o.addProperty("selectedIndex", actionEditorSelected);
        if (actionEditorSelectedChannel != null) o.addProperty("selectedChannel", actionEditorSelectedChannel);

        JsonArray chars = new JsonArray();
        int ci = 0;
        for (Replay r : film.replays.getList())
        {
            JsonObject c = new JsonObject();
            c.addProperty("index", ci);
            c.addProperty("label", r.getName());
            chars.add(c);
            ci++;
        }
        o.add("characters", chars);

        if ("action".equals(type))
        {
            if ("path".equals(actionEditorLeftMode))
            {
                JsonArray ch = new JsonArray();
                for (KeyframeChannel<Double> kc : java.util.Arrays.asList(replay.keyframes.x, replay.keyframes.y, replay.keyframes.z, replay.keyframes.yaw))
                {
                    ch.add(channelJson(kc));
                }
                o.add("channels", ch);
            }
            else
            {
                JsonArray acts = new JsonArray();
                int ai = 0;
                for (Clip clip : replay.actions.get())
                {
                    ActionClip ac = (ActionClip) clip;
                    JsonObject a = new JsonObject();
                    a.addProperty("index", ai);
                    a.addProperty("title", clip.title.get().isEmpty() ? "(未命名动作)" : clip.title.get());
                    a.addProperty("tick", clip.tick.get());
                    a.addProperty("duration", clip.duration.get());
                    a.addProperty("frequency", ac.frequency.get());
                    a.addProperty("kind", clip instanceof ScriptActionClip ? "script" : clip instanceof LocomotionActionClip ? "locomotion" : "action");
                    a.addProperty("layer", clip.layer.get());
                    a.addProperty("enabled", clip.enabled.get());
                    a.addProperty("blendIn", clip.envelope.fadeIn.get());
                    a.addProperty("blendOut", clip.envelope.fadeOut.get());
                    if (clip instanceof LocomotionActionClip loc)
                    {
                        a.addProperty("mode", loc.mode.get());
                        a.addProperty("step", loc.step.get());
                    }
                    if (clip instanceof ScriptActionClip sac)
                    {
                        a.addProperty("script", sac.script.get());
                        JsonArray params = new JsonArray();
                        int pi = 0;
                        for (ValueFloat v : sac.params.getList())
                        {
                            JsonObject p = new JsonObject();
                            p.addProperty("index", pi);
                            p.addProperty("value", v.get());
                            params.add(p);
                            pi++;
                        }
                        a.add("params", params);
                    }
                    acts.add(a);
                    ai++;
                }
                o.add("actions", acts);
            }
        }
        else
        {
            JsonArray ch = new JsonArray();
            for (KeyframeChannel<?> kc : replay.keyframes.getChannels())
            {
                if (kc instanceof KeyframeChannel && isDoubleChannel(kc))
                {
                    ch.add(channelJson((KeyframeChannel<Double>) kc));
                }
            }
            o.add("channels", ch);
        }

        int maxTick = film.camera.calculateDuration();
        o.addProperty("timelineMax", maxTick > 0 ? maxTick : 600);

        /* Material timeline: always present, independent of character type. */
        JsonArray mats = new JsonArray();
        int mi = 0;
        for (MaterialClip mc : replay.materials.getAllTyped())
        {
            JsonObject m = new JsonObject();
            m.addProperty("index", mi);
            m.addProperty("type", mc.type.get());
            m.addProperty("target", mc.target.get());
            m.addProperty("slot", mc.slot.get());
            m.addProperty("item", mc.item.get());
            m.addProperty("tick", mc.tick.get());
            m.addProperty("duration", mc.duration.get());
            m.addProperty("enabled", mc.enabled.get());
            mats.add(m);
            mi++;
        }
        o.add("materials", mats);
        o.addProperty("selectedMaterial", actionEditorSelectedMaterial);

        JsonArray groups = new JsonArray();
        for (Clip g : ActionGroupLibrary.get().get())
        {
            if (g instanceof ActionGroup group)
            {
                JsonObject go = new JsonObject();
                go.addProperty("id", group.id.get());
                go.addProperty("title", group.title.get().isEmpty() ? ("动作组" + (groups.size() + 1)) : group.title.get());
                go.addProperty("count", group.subActions.get().size());

                boolean allClient = !group.subActions.get().isEmpty();

                for (Clip sub : group.subActions.get())
                {
                    if (sub instanceof ActionClip subAction && !subAction.isClient())
                    {
                        allClient = false;
                        break;
                    }
                }

                go.addProperty("client", allClient);
                groups.add(go);
            }
        }
        o.add("actionGroups", groups);

        return o;
    }

    private static JsonObject buildEquipState(EditorBridge bridge)
    {
        UIFilmPanel panel = bridge.panel;
        Film film = panel.getData();

        if (film == null || equipReplay < 0 || equipReplay >= film.replays.getList().size())
        {
            return null;
        }

        Replay replay = film.replays.getList().get(equipReplay);
        JsonObject o = new JsonObject();

        o.addProperty("replayIndex", equipReplay);
        o.addProperty("label", replay.label.get());

        JsonArray slots = new JsonArray();
        slots.add(equipSlot("head", "头盔", replay.keyframes.armorHead));
        slots.add(equipSlot("chest", "胸甲", replay.keyframes.armorChest));
        slots.add(equipSlot("legs", "护腿", replay.keyframes.armorLegs));
        slots.add(equipSlot("feet", "靴子", replay.keyframes.armorFeet));
        slots.add(equipSlot("mainhand", "主手", replay.keyframes.mainHand));
        slots.add(equipSlot("offhand", "副手", replay.keyframes.offHand));
        o.add("slots", slots);

        return o;
    }

    private static JsonObject equipSlot(String key, String name, KeyframeChannel<ItemStack> channel)
    {
        JsonObject o = new JsonObject();

        o.addProperty("key", key);
        o.addProperty("name", name);

        ItemStack current = channel.interpolate(0, ItemStack.EMPTY);
        String id = "";

        if (current != null && !current.isEmpty())
        {
            Identifier rl = BuiltInRegistries.ITEM.getKey(current.getItem());

            if (rl != null)
            {
                id = rl.toString();
            }
        }

        o.addProperty("itemId", id);

        return o;
    }

    private static JsonObject channelJson(KeyframeChannel<Double> ch)
    {
        JsonObject o = new JsonObject();
        o.addProperty("id", ch.getId());
        JsonArray kfs = new JsonArray();
        int i = 0;
        for (Keyframe<Double> kf : ch.getKeyframes())
        {
            JsonObject k = new JsonObject();
            k.addProperty("index", i);
            k.addProperty("tick", (int) kf.getTick());
            k.addProperty("value", kf.getValue());
            kfs.add(k);
            i++;
        }
        o.add("keyframes", kfs);
        return o;
    }

    private static boolean isDoubleChannel(KeyframeChannel<?> ch)
    {
        if (ch.getKeyframes().isEmpty()) return true;
        Object v = ch.getKeyframes().get(0).getValue();
        return v instanceof Double;
    }

    private static KeyframeChannel<Double> findDoubleChannel(Replay replay, String id)
    {
        if (id == null) return null;
        for (KeyframeChannel<Double> kc : java.util.Arrays.asList(replay.keyframes.x, replay.keyframes.y, replay.keyframes.z, replay.keyframes.yaw))
        {
            if (id.equals(kc.getId())) return kc;
        }
        for (KeyframeChannel<?> kc : replay.keyframes.getChannels())
        {
            if (id.equals(kc.getId()) && kc instanceof KeyframeChannel && isDoubleChannel(kc))
            {
                return (KeyframeChannel<Double>) kc;
            }
        }
        return null;
    }

    private static void aeSetCharType(UIFilmPanel panel, String type)
    {
        Film film = panel.getData();
        if (film == null || actionEditorReplay < 0 || actionEditorReplay >= film.replays.getList().size()) return;
        String t = "action".equals(type) ? "action" : "keyframe";
        BaseValue.edit(film, f -> film.replays.getList().get(actionEditorReplay).characterType.set(t));
        actionEditorLeftMode = "action";
        actionEditorSelected = -1;
        actionEditorSelectedChannel = null;
        refreshHtml();
    }

    private static void aeCreateAction(UIFilmPanel panel, int presetIndex)
    {
        Film film = panel.getData();
        int ri = editorReplayIndex();
        if (film == null || ri < 0 || ri >= film.replays.getList().size()) return;
        Replay replay = film.replays.getList().get(ri);
        String[][] presets = {
            {"走路", "locomotion", "walk"},
            {"奔跑", "locomotion", "run"},
            {"空闲", "locomotion", "idle"},
            {"攻击", "attack", null},
            {"挥击", "swipe", null},
            {"破坏方块", "break_block", null},
            {"放置方块", "place_block", null},
            {"钓鱼", "use_item", "fishing_rod"},
            {"聊天", "chat", null},
            {"命令", "command", null},
            {"丢物品", "drop_item", null},
            {"脚本动作", "script", null}
        };
        if (presetIndex < 0 || presetIndex >= presets.length) return;
        if (!"action".equals(replay.characterType.get()))
        {
            MCEFUI.injectScript("toast('关键帧角色不支持动作，请先设为动作角色', true)");
            return;
        }
        String[] p = presets[presetIndex];
        BaseValue.edit(film, f ->
        {
            Clip c = BBSMod.getFactoryActionClips().create(Link.bbs(p[1]));
            if (c instanceof ActionClip ac)
            {
                ac.title.set(p[0]);
                ac.duration.set(20);
                if (c instanceof LocomotionActionClip loc && p[2] != null) loc.mode.set(p[2]);
                if (c instanceof ItemActionClip item && p[2] != null) item.itemStack.set(p[2].equals("fishing_rod") ? new ItemStack(Items.FISHING_ROD) : ItemStack.EMPTY);
                replay.actions.addClip(ac);
                actionEditorSelected = replay.actions.get().size() - 1;
            }
        });
        refreshHtml();
    }

    private static void aeDeleteAction(UIFilmPanel panel, int ai)
    {
        Film film = panel.getData();
        int ri = editorReplayIndex();
        if (film == null || ri < 0 || ri >= film.replays.getList().size()) return;
        Replay replay = film.replays.getList().get(ri);
        Clip c = replay.actions.get(ai);
        if (c == null) return;
        BaseValue.edit(film, f -> replay.actions.remove(c));
        int size = replay.actions.get().size();
        actionEditorSelected = size <= 0 ? -1 : Math.min(actionEditorSelected, size - 1);
        refreshHtml();
    }

    /** The replay index whose action-editor state is currently shown, either
     *  because the big modal is open ({@code actionEditorReplay}) or because a
     *  character is focused in the asset-detail panel ({@code focusedCharacterIndex}).
     *  The modal takes precedence; falls back to the focused character. */
    /** Compose the current replay's action-track sub-actions into one
     *  {@link ActionGroup}, replace the loose actions on the track with that
     *  single group, and store a reusable copy in the group library so it can
     *  be bound onto other characters from the dropdown. */
    private static void composeActionGroup(UIFilmPanel panel, JsonObject req)
    {
        Film film = panel.getData();
        int ri = editorReplayIndex();
        if (film == null || ri < 0 || ri >= film.replays.getList().size()) return;
        Replay replay = film.replays.getList().get(ri);
        String name = (req != null && req.has("name") && !req.get("name").isJsonNull()) ? req.get("name").getAsString() : "";
        ActionGroup group = new ActionGroup();
        group.title.set(name.isEmpty() ? ("动作组" + (ActionGroupLibrary.get().get().size() + 1)) : name);
        BaseValue.edit(film, f ->
        {
            int end = 0;
            List<Clip> current = new ArrayList<>(replay.actions.get());

            for (Clip c : current)
            {
                if (c instanceof ActionClip)
                {
                    Clip copy = c.copy();
                    group.subActions.addClip(copy);
                    end = Math.max(end, copy.tick.get() + copy.duration.get());
                    replay.actions.remove(c);
                }
            }

            group.tick.set(0);
            group.duration.set(Math.max(1, end));
            replay.actions.addClip(group);
        });
        ActionGroupLibrary.addGroup(group.copy());
        refreshHtml();
    }

    /** Bind a library action group onto the current replay's action timeline
     *  as a clip (optionally at the dropped tick, else appended at the end). */
    private static void addActionGroup(UIFilmPanel panel, JsonObject req)
    {
        if (req == null || !req.has("id") || req.get("id").isJsonNull())
        {
            MCEFUI.injectScript("toast('动作组参数缺失', true);");
            return;
        }
        String id = req.get("id").getAsString();
        if (id == null || id.isEmpty())
        {
            MCEFUI.injectScript("toast('动作组参数缺失', true);");
            return;
        }
        ActionGroup group = ActionGroupLibrary.find(id);
        if (group == null)
        {
            MCEFUI.injectScript("toast('未找到该动作组', true);");
            return;
        }
        Film film = panel.getData();
        int ri = -1;
        if (req != null && req.has("replayId") && !req.get("replayId").isJsonNull())
        {
            /* F0: resolve target replay by stable id. */
            String rid = req.get("replayId").getAsString();
            List<Replay> replays = film.replays.getList();

            for (int i = 0; i < replays.size(); i++)
            {
                if (rid.equals(replays.get(i).getId()))
                {
                    ri = i;
                    break;
                }
            }

            if (ri < 0)
            {
                MCEFUI.injectScript("toast('目标轨道不存在', true);");
                return;
            }
        }
        else if (req != null && req.has("index") && !req.get("index").isJsonNull())
        {
            int idx = req.get("index").getAsInt();
            if (idx >= 0 && idx < film.replays.getList().size())
            {
                ri = idx;
            }
            else
            {
                MCEFUI.injectScript("toast('目标轨道不存在', true);");
                return;
            }
        }
        else
        {
            ri = editorReplayIndex();
        }
        if (film == null || ri < 0 || ri >= film.replays.getList().size()) return;
        Replay replay = film.replays.getList().get(ri);
        int tick = (req.has("tick") && !req.get("tick").isJsonNull()) ? req.get("tick").getAsInt() : replay.actions.calculateDuration();
        BaseValue.edit(film, f ->
        {
            ActionGroup copy = (ActionGroup) group.copy();
            copy.tick.set(Math.max(0, tick));
            copy.layer.set(replay.actions.findFreeLayer(copy));
            replay.actions.addClip(copy);
        });
        refreshHtml();
    }

    private static int editorReplayIndex()
    {
        Film film = instance != null && instance.panel != null ? instance.panel.getData() : null;

        if (actionEditorReplayId != null)
        {
            int idx = replayIndexById(film, actionEditorReplayId);

            if (idx >= 0)
            {
                return idx;
            }
        }

        if (actionEditorReplay >= 0)
        {
            return actionEditorReplay;
        }

        return focusedCharacterIndex;
    }

    private static void aeSetActionField(UIFilmPanel panel, JsonObject req, java.util.function.Consumer<ActionClip> editor)
    {
        Film film = panel.getData();
        int ri = editorReplayIndex();
        if (film == null || ri < 0 || ri >= film.replays.getList().size()) return;
        Replay replay = film.replays.getList().get(ri);
        int ai = req.has("ai") ? req.get("ai").getAsInt() : -1;
        Clip clip = replay.actions.get(ai);
        if (!(clip instanceof ActionClip ac)) return;
        BaseValue.edit(film, f -> editor.accept(ac));
        refreshHtml();
    }

    /* ---- Material timeline commands (lingfeng.bbsnext) ---- */

    private static void aeSetMaterialField(UIFilmPanel panel, JsonObject req, java.util.function.Consumer<MaterialClip> editor)
    {
        Film film = panel.getData();
        int ri = editorReplayIndex();
        if (film == null || ri < 0 || ri >= film.replays.getList().size()) return;
        Replay replay = film.replays.getList().get(ri);
        int mi = req.has("mi") ? req.get("mi").getAsInt() : -1;
        if (mi < 0 || mi >= replay.materials.getAllTyped().size()) return;
        MaterialClip mc = replay.materials.getAllTyped().get(mi);
        BaseValue.edit(film, f -> editor.accept(mc));
        refreshHtml();
    }

    private static void aeAddMaterial(UIFilmPanel panel, String type)
    {
        Film film = panel.getData();
        int ri = editorReplayIndex();
        if (film == null || ri < 0 || ri >= film.replays.getList().size()) return;
        Replay replay = film.replays.getList().get(ri);
        BaseValue.edit(film, f ->
        {
            MaterialClip mc = new MaterialClip("material_" + replay.materials.getAllTyped().size());
            mc.type.set(type == null ? MaterialClip.TYPE_MODEL : type);
            replay.materials.add(mc);
            replay.materials.sync();
            actionEditorSelectedMaterial = replay.materials.getAllTyped().size() - 1;
        });
        refreshHtml();
    }

    private static void aeDeleteMaterial(UIFilmPanel panel, int mi)
    {
        Film film = panel.getData();
        int ri = editorReplayIndex();
        if (film == null || ri < 0 || ri >= film.replays.getList().size()) return;
        Replay replay = film.replays.getList().get(ri);
        if (mi < 0 || mi >= replay.materials.getAllTyped().size()) return;
        BaseValue.edit(film, f ->
        {
            replay.materials.getAllTyped().remove(mi);
            replay.materials.sync();
        });
        int size = replay.materials.getAllTyped().size();
        actionEditorSelectedMaterial = size <= 0 ? -1 : Math.min(actionEditorSelectedMaterial, size - 1);
        refreshHtml();
    }

    private static void aeSelectMaterial(UIFilmPanel panel, int mi)
    {
        actionEditorSelectedMaterial = mi;
        MCEFUI.injectScript("window.bbsState.actionEditor.selectedMaterial=" + mi
            + ";renderActionEditor(window.bbsState);renderAssetDetail(window.bbsState);");
    }

    /** Toggle a character-level field (enabled / fp / shadow) on the focused or
     *  open replay, then persist + re-render. */
    private static void toggleCharField(UIFilmPanel panel, java.util.function.Consumer<Replay> editor)
    {
        int ri = editorReplayIndex();
        Film film = panel.getData();
        if (film == null || ri < 0 || ri >= film.replays.getList().size()) return;
        BaseValue.edit(film, f -> editor.accept(f.replays.getList().get(ri)));
        panel.save();
        pushFocusedCharacterSlice(panel);
    }

    /** Edit a field of the selected ActionClip (layer / enabled / blendIn / blendOut)
     *  on the focused or open replay. */
    private static void aeSetClipField(UIFilmPanel panel, JsonObject req, java.util.function.Consumer<Clip> editor)
    {
        Film film = panel.getData();
        int ri = editorReplayIndex();
        if (film == null || ri < 0 || ri >= film.replays.getList().size()) return;
        Replay replay = film.replays.getList().get(ri);
        int ai = req.has("ai") ? req.get("ai").getAsInt() : -1;
        if (ai < 0 || ai >= replay.actions.get().size()) return;
        Clip clip = replay.actions.get(ai);
        BaseValue.edit(film, f -> editor.accept(clip));
        refreshHtml();
    }

    /** Capture the live 3D world (Minecraft's main render target) to a PNG in
     *  bbs_editor/screenshots and toast the path back to the page. */
    private static void screenshot(UIFilmPanel panel)
    {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null || mc.gameRenderer == null || mc.gameRenderer.mainRenderTarget() == null)
        {
            MCEFUI.injectScript("toast('没有可截图的画面（请先进入一个世界）', true)");
            return;
        }

        try
        {
            byte[] png = GlTextureBridge.captureMainRenderTarget(mc.gameRenderer.mainRenderTarget(), 1280);

            if (png == null)
            {
                MCEFUI.injectScript("toast('截图失败', true)");
                return;
            }

            File dir = new File(mc.gameDirectory, "bbs_editor/screenshots");
            dir.mkdirs();
            String name = "shot_" + System.currentTimeMillis() + ".png";
            Files.write(dir.toPath().resolve(name), png);
            MCEFUI.injectScript("toast('已保存截图：bbs_editor/screenshots/" + name + "')");
        }
        catch (Throwable t)
        {
            BBSMod.LOGGER.error("[MCEF] screenshot failed", t);
            MCEFUI.injectScript("toast('截图失败', true)");
        }
    }

    private static void aeMutateSelectedScript(UIFilmPanel panel, JsonObject req, java.util.function.Consumer<ScriptActionClip> editor)
    {
        Film film = panel.getData();
        int ri = editorReplayIndex();
        if (film == null || ri < 0 || ri >= film.replays.getList().size()) return;
        Replay replay = film.replays.getList().get(ri);
        int ai = req.has("ai") ? req.get("ai").getAsInt() : -1;
        Clip clip = replay.actions.get(ai);
        if (!(clip instanceof ScriptActionClip sac)) return;
        BaseValue.edit(film, f -> editor.accept(sac));
        refreshHtml();
    }

    private static void aeAddKeyframe(UIFilmPanel panel, String channel, int tick, double value)
    {
        Film film = panel.getData();
        int ri = editorReplayIndex();
        if (film == null || ri < 0 || ri >= film.replays.getList().size()) return;
        Replay replay = film.replays.getList().get(ri);
        KeyframeChannel<Double> ch = findDoubleChannel(replay, channel);
        if (ch == null) return;
        BaseValue.edit(film, f -> ch.insert(tick, value));
        actionEditorSelectedChannel = channel;
        refreshHtml();
    }

    private static void aeDelKeyframe(UIFilmPanel panel, String channel, int index)
    {
        Film film = panel.getData();
        int ri = editorReplayIndex();
        if (film == null || ri < 0 || ri >= film.replays.getList().size()) return;
        Replay replay = film.replays.getList().get(ri);
        KeyframeChannel<Double> ch = findDoubleChannel(replay, channel);
        if (ch == null) return;
        List<Keyframe<Double>> kfs = ch.getKeyframes();
        if (index < 0 || index >= kfs.size()) return;
        BaseValue.edit(film, f -> ch.getKeyframes().remove(index));
        actionEditorSelectedChannel = channel;
        refreshHtml();
    }

    private static void aeSetKeyframeValue(UIFilmPanel panel, String channel, int index, double value)
    {
        Film film = panel.getData();
        int ri = editorReplayIndex();
        if (film == null || ri < 0 || ri >= film.replays.getList().size()) return;
        Replay replay = film.replays.getList().get(ri);
        KeyframeChannel<Double> ch = findDoubleChannel(replay, channel);
        if (ch == null) return;
        List<Keyframe<Double>> kfs = ch.getKeyframes();
        if (index < 0 || index >= kfs.size()) return;
        BaseValue.edit(film, f -> kfs.get(index).setValue(value));
        actionEditorSelectedChannel = channel;
        refreshHtml();
    }

    /**
     * Drop an asset into the active (or first) sequence. Characters use the
     * native "mcpr" ref type; scenes/sequences map to their own types;
     * entity/particle/item use a custom type string (persisted, visualized by
     * the editor's ref list).
     */
    private static String addToCurrent(EditorBridge bridge, String type, String id)
    {
        SequenceManager sequences = SequenceManager.get();

        if (sequences == null)
        {
            return "{\"ok\":false,\"error\":\"no sequence manager\"}";
        }

        Sequence target = null;

        if (bridge.activeSequenceId != null)
        {
            target = sequences.getById(bridge.activeSequenceId);
        }

        if (target == null && !sequences.getSequences().isEmpty())
        {
            target = sequences.getSequences().get(0);
        }

        if (target == null)
        {
            return "{\"ok\":false,\"error\":\"no sequence to add into\"}";
        }

        String refType = type;

        if ("character".equals(type))
        {
            refType = Sequence.SequenceRef.MCPR;
        }
        else if ("scene".equals(type))
        {
            refType = Sequence.SequenceRef.SCENE;
        }
        else if ("sequence".equals(type))
        {
            refType = Sequence.SequenceRef.SEQUENCE;
        }
        else if (Backpack.TYPE_CAMERA.equals(type))
        {
            refType = Sequence.SequenceRef.CAMERA;
        }
        else if (Backpack.TYPE_CAMERAGROUP.equals(type))
        {
            refType = Sequence.SequenceRef.CAMERAGROUP;
        }

        sequences.addRef(target, refType, id);

        return "{\"ok\":true}";
    }

    /** Map the reference UI's left toolbar buttons to real BBS functions. */
    private static void setTool(UIFilmPanel panel, String tool)
    {
        if (tool == null || tool.isEmpty())
        {
            return;
        }

        switch (tool.charAt(0))
        {
            case 'Q': /* 选择/跟随相机 */ panel.getController().setPov(0); break;
            case 'W': /* 平移/自由相机 */ panel.getController().setPov(1); break;
            case 'E': /* 旋转/轨道相机 */ panel.getController().setPov(2); break;
            case 'R': /* 缩放/第三人称 */ panel.getController().setPov(4); break;
            case 'C': /* 切片/第一人称 */ panel.getController().setPov(3); break;
            case 'S': /* 吸附/瞬间关键帧 */ panel.getController().toggleInstantKeyframes(); break;
            case 'F': /* 录制 */ panel.getController().startRecording(null); break;
            case 'T': /* 骨骼/演员控制 */ panel.getController().toggleControl(); break;
            case 'Y': /* 动作模板/创建实体 */ panel.getController().createEntities(); break;
            default: break;
        }
    }

    private static void shiftAllKeyframes(UIFilmPanel panel, int tick)
    {
        if (tick == 0)
        {
            return;
        }

        Film film = panel.getData();

        if (film == null)
        {
            return;
        }

        BaseValue.edit(film, f ->
        {
            for (var r : f.replays.getList())
            {
                r.shift(tick);
            }
        });

        panel.save();
        refreshHtml();
    }

    private static void clipOp(UIFilmPanel panel, String op, int index, String id)
    {
        Film film = panel.getData();

        if (film == null)
        {
            return;
        }

        if ("select".equals(op))
        {
            /* Camera track clips are identified by stable clip id, so resolve
             * that to the library index the delete/split ops operate on. */
            if (id != null && !id.isEmpty())
            {
                Clip sel = findCameraClip(film, id);

                if (sel != null)
                {
                    int i = film.camera.get().indexOf(sel);

                    if (i >= 0)
                    {
                        selectedClipIndex = i;
                    }
                }

                return;
            }

            /* Remember the selected clip so index-less clipOp requests
             * (delete/split from the menu) can target it later. */
            if (index >= 0 && index < film.camera.get().size())
            {
                selectedClipIndex = index;
            }

            return;
        }

        /* Most clip operations arrive without an explicit index (the menu /
         * context items send index -1). Fall back to the last selection. */
        int target = index;

        if (target < 0 || target >= film.camera.get().size())
        {
            target = selectedClipIndex;
        }

        if (target < 0 || target >= film.camera.get().size())
        {
            return;
        }

        final int i = target;

        switch (op)
        {
            case "delete":
            {
                BaseValue.edit(film, f -> f.camera.get().remove(i));

                /* Keep the selection valid after removal. */
                if (selectedClipIndex == i)
                {
                    selectedClipIndex = -1;
                }

                break;
            }
            case "split":
            {
                /* Split the clip at the playhead. */
                final int offset = panel.getCursor() - film.camera.get().get(i).tick.get();

                if (offset > 0 && offset < film.camera.get().get(i).duration.get())
                {
                    BaseValue.edit(film, f ->
                    {
                        Clip clip = f.camera.get().get(i);
                        int splitOffset = Math.max(1, Math.min(offset, clip.duration.get() - 1));
                        clip.duration.set(splitOffset);

                        Clip rest = clip.copy();
                        rest.tick.set(clip.tick.get() + splitOffset);
                        rest.duration.set(Math.max(1, clip.duration.get() - splitOffset));
                        f.camera.get().add(i + 1, rest);
                    });
                }

                break;
            }
            default:
                break;
        }
    }

    private static void openScene(UIFilmPanel panel, String sceneId)
    {
        SceneManager scenes = SceneManager.get();

        if (scenes == null || sceneId == null)
        {
            return;
        }

        for (Scene scene : scenes.getScenes())
        {
            if (scene.id.equals(sceneId))
            {
                panel.save();
                panel.openScene(scene);

                /* Switching the current scene must not auto-enter the preview
                 * world (that is now an explicit "进入预览世界" button action).
                 * Reset the active sequence so renderAll does not show clips
                 * from the previously edited film against the new scene's film. */
                activeSequenceId = null;

                return;
            }
        }
    }

    private static void closeEditor(UIFilmPanel panel)
    {
        UIDashboard dashboard = panel.dashboard;

        panel.save();
        dashboard.setPanel(dashboard.getPanel(lingfeng.bbsnext.ui.dashboard.panels.UIProjectsPanel.class));

        /* Leave the dedicated preview world when the editor closes, so the
         * player is not left inside bbs_preview (goes back to the title). */
        Minecraft mc = Minecraft.getInstance();

        if (mc.level != null)
        {
            mc.clearClientLevel(new net.minecraft.client.gui.screens.TitleScreen());
        }

        clearEnteringWorld();
        owningPreviewWorld = false;
        inWorld = false;
    }

    /* F0: legacy fallback — a few older call sites still send a list index; map
     * it back to the stable replay id before anything reads it. */
    private static String resolveReplayIdByIndex(UIFilmPanel panel, int index)
    {
        Film film = panel.getData();

        if (film == null || index < 0 || index >= film.replays.getList().size())
        {
            return "";
        }

        return film.replays.getList().get(index).getId();
    }

    /* F0/S12: reverse lookup — stable replay id to its current list index. */
    private static int replayIndexById(Film film, String id)
    {
        if (film == null || id == null)
        {
            return -1;
        }

        List<Replay> list = film.replays.getList();

        for (int i = 0; i < list.size(); i++)
        {
            if (id.equals(list.get(i).getId()))
            {
                return i;
            }
        }

        return -1;
    }

    private static String replayIdByIndex(Film film, int index)
    {
        if (film == null || index < 0 || index >= film.replays.getList().size())
        {
            return null;
        }

        return film.replays.getList().get(index).getId();
    }

    /* F0/D2: delete a replay by its stable id and cascade the lingfeng
     * side-tables (track order, track props, matte sources) and any MCPR
     * sequence references into ONE undoable transaction. Snapshot-style: the
     * pre-delete state of every affected side-table/reference is captured and a
     * custom IUndo is pushed so a single Ctrl+Z restores the replay AND every
     * cascade. D1: focus is tracked by stable id, so the deletion cannot
     * mis-target the focused character. */
    private static void deleteReplay(UIFilmPanel panel, String replayId)
    {
        if (replayId == null || replayId.isEmpty())
        {
            return;
        }

        Minecraft.getInstance().execute(() ->
        {
            Film f = panel.getData();

            if (f == null)
            {
                return;
            }

            int removedIndex = replayIndexById(f, replayId);

            if (removedIndex < 0)
            {
                return;
            }

            String filmId = f.getId();

            BaseType replayBefore = f.replays.getList().get(removedIndex).toData();
            List<String> orderBefore = TrackOrderStore.get(filmId);
            JsonObject propBefore = snapshotTrackProp(filmId, replayId);
            List<JsonObject> matteBefore = snapshotMatteSources(f, filmId, replayId);
            List<JsonObject> seqRefsBefore = snapshotSeqRefs(filmId, replayId);
            MapType uiBefore = panel.getRoot() == null ? null : panel.getRoot().collectAllUndoData();

            DeleteReplayUndo undo = new DeleteReplayUndo(panel, filmId, replayId, removedIndex, replayBefore, orderBefore, propBefore, matteBefore, seqRefsBefore);

            applyDeleteReplay(panel, f, filmId, replayId);

            undo.uiAfter = panel.getRoot() == null ? null : panel.getRoot().collectAllUndoData();

            if (panel.getUndoHandler() != null)
            {
                panel.getUndoHandler().getUndoManager().pushUndo(undo);
            }

            refreshHtml();
        });
    }

    private static final String SNAP_REPLAY_ID = "__rid";

    private static JsonObject snapshotTrackProp(String filmId, String replayId)
    {
        TrackProp tp = TrackPropStore.getIfPresent(filmId, replayId);

        return tp == null ? null : tp.toJson();
    }

    private static List<JsonObject> snapshotMatteSources(Film film, String filmId, String replayId)
    {
        List<JsonObject> snaps = new ArrayList<>();

        for (Replay r : film.replays.getList())
        {
            if (replayId.equals(r.getId()))
            {
                continue;
            }

            TrackProp tp = TrackPropStore.getIfPresent(filmId, r.getId());

            if (tp != null && replayId.equals(tp.matteSource))
            {
                JsonObject snap = tp.toJson();
                snap.addProperty(SNAP_REPLAY_ID, r.getId());
                snaps.add(snap);
            }
        }

        return snaps;
    }

    private static List<JsonObject> snapshotSeqRefs(String filmId, String replayId)
    {
        List<JsonObject> snaps = new ArrayList<>();
        SequenceManager seqMgr = SequenceManager.get();

        if (seqMgr != null)
        {
            for (Sequence seq : seqMgr.getSequences())
            {
                for (Sequence.SequenceRef ref : seq.refs)
                {
                    if (Sequence.SequenceRef.MCPR.equals(ref.type) && replayId.equals(ref.id))
                    {
                        JsonObject snap = new JsonObject();
                        snap.addProperty("seqId", seq.id);
                        snap.addProperty("type", ref.type);
                        snap.addProperty("refId", ref.id);
                        snaps.add(snap);
                    }
                }
            }
        }

        return snaps;
    }

    /* D2: apply the deletion (replay + every cascade) without pushing an undo.
     * Shared by the initial delete and the custom undo's redo(). */
    private static void applyDeleteReplay(UIFilmPanel panel, Film f, String filmId, String replayId)
    {
        int removedIndex = replayIndexById(f, replayId);

        if (removedIndex < 0)
        {
            return;
        }

        Replay removed = f.replays.getList().get(removedIndex);
        List<Replay> list = f.replays.getAllTyped();

        list.remove(removed);

        for (int i = 0; i < list.size(); i++)
        {
            list.get(i).setId(String.valueOf(i));
            list.get(i).setParent(f.replays);
        }

        f.replays.postNotify();

        TrackPropStore.removeForReplay(filmId, replayId);
        TrackOrderStore.remove(filmId, replayId);
        TrackPropStore.clearMatteSource(filmId, replayId);

        SequenceManager seqMgr = SequenceManager.get();

        if (seqMgr != null)
        {
            for (Sequence seq : seqMgr.getSequences())
            {
                List<Sequence.SequenceRef> dangling = new ArrayList<>();

                for (Sequence.SequenceRef ref : seq.refs)
                {
                    if (Sequence.SequenceRef.MCPR.equals(ref.type) && replayId.equals(ref.id))
                    {
                        dangling.add(ref);
                    }
                }

                for (Sequence.SequenceRef ref : dangling)
                {
                    seqMgr.removeRef(seq, ref);
                }
            }
        }

        /* D1: maintain the index caches so neither the asset-detail panel nor the
         * action editor can mis-target a neighbour after the list shifts. */
        if (replayId.equals(focusedCharacterId))
        {
            focusedCharacterId = null;
            focusedCharacterIndex = -1;
        }
        else if (removedIndex >= 0 && removedIndex < focusedCharacterIndex)
        {
            focusedCharacterIndex--;
        }

        if (replayId.equals(actionEditorReplayId))
        {
            actionEditorReplayId = null;
            actionEditorReplay = -1;
        }
        else if (removedIndex >= 0 && removedIndex < actionEditorReplay)
        {
            actionEditorReplay--;
        }

        panel.save();
    }

    /* D2: reverse of applyDeleteReplay — rebuild the replay and every cascade
     * from the captured snapshots. */
    private static void applyRestoreReplay(UIFilmPanel panel, Film f, String filmId, String replayId, int removedIndex, BaseType replayBefore, List<String> orderBefore, JsonObject propBefore, List<JsonObject> matteBefore, List<JsonObject> seqRefsBefore)
    {
        Replay r = new Replay(String.valueOf(Math.max(0, removedIndex)));
        r.fromData(replayBefore);

        List<Replay> list = f.replays.getAllTyped();
        int insertAt = (removedIndex < 0 || removedIndex > list.size()) ? list.size() : removedIndex;

        list.add(insertAt, r);

        for (int i = 0; i < list.size(); i++)
        {
            list.get(i).setId(String.valueOf(i));
            list.get(i).setParent(f.replays);
        }

        f.replays.postNotify();

        TrackOrderStore.set(filmId, orderBefore);

        if (propBefore != null)
        {
            restoreTrackProp(filmId, replayId, propBefore);
        }

        for (JsonObject snap : matteBefore)
        {
            String otherId = snap.has(SNAP_REPLAY_ID) ? snap.get(SNAP_REPLAY_ID).getAsString() : null;
            snap.remove(SNAP_REPLAY_ID);

            if (otherId != null)
            {
                restoreTrackProp(filmId, otherId, snap);
            }
        }

        SequenceManager seqMgr = SequenceManager.get();

        if (seqMgr != null)
        {
            for (JsonObject snap : seqRefsBefore)
            {
                Sequence seq = seqMgr.getById(snap.get("seqId").getAsString());

                if (seq != null)
                {
                    seqMgr.addRef(seq, snap.get("type").getAsString(), snap.get("refId").getAsString());
                }
            }
        }

        /* D1: re-pin the index caches from the stable ids after re-insertion. */
        if (focusedCharacterId != null)
        {
            focusedCharacterIndex = replayIndexById(f, focusedCharacterId);
        }

        if (actionEditorReplayId != null)
        {
            actionEditorReplay = replayIndexById(f, actionEditorReplayId);
        }

        panel.save();
    }

    private static void restoreTrackProp(String filmId, String replayId, JsonObject json)
    {
        if (json == null)
        {
            return;
        }

        if (json.has("blendMode")) TrackPropStore.set(filmId, replayId, "blendMode", json.get("blendMode").getAsString());
        if (json.has("opacity")) TrackPropStore.set(filmId, replayId, "opacity", String.valueOf(json.get("opacity").getAsFloat()));
        if (json.has("matteSource")) TrackPropStore.set(filmId, replayId, "matteSource", json.get("matteSource").getAsString());
        if (json.has("matteMode")) TrackPropStore.set(filmId, replayId, "matteMode", json.get("matteMode").getAsString());
    }

    /* D2: single undo entry covering the replay deletion and all its cascades. */
    private static class DeleteReplayUndo implements IUndo<ValueGroup>
    {
        private final UIFilmPanel panel;
        private final String filmId;
        private final String replayId;
        private final int removedIndex;
        private final BaseType replayBefore;
        private final List<String> orderBefore;
        private final JsonObject propBefore;
        private final List<JsonObject> matteBefore;
        private final List<JsonObject> seqRefsBefore;
        private MapType uiBefore;
        private MapType uiAfter;

        DeleteReplayUndo(UIFilmPanel panel, String filmId, String replayId, int removedIndex, BaseType replayBefore, List<String> orderBefore, JsonObject propBefore, List<JsonObject> matteBefore, List<JsonObject> seqRefsBefore)
        {
            this.panel = panel;
            this.filmId = filmId;
            this.replayId = replayId;
            this.removedIndex = removedIndex;
            this.replayBefore = replayBefore;
            this.orderBefore = orderBefore;
            this.propBefore = propBefore;
            this.matteBefore = matteBefore;
            this.seqRefsBefore = seqRefsBefore;
        }

        @Override
        public IUndo<ValueGroup> noMerging()
        {
            return this;
        }

        @Override
        public boolean isMergeable(IUndo<ValueGroup> undo)
        {
            return false;
        }

        @Override
        public void merge(IUndo<ValueGroup> undo)
        {}

        @Override
        public void undo(ValueGroup context)
        {
            Film f = panel.getData();

            if (f == null)
            {
                return;
            }

            applyRestoreReplay(panel, f, filmId, replayId, removedIndex, replayBefore, orderBefore, propBefore, matteBefore, seqRefsBefore);
            applyUi(uiBefore);
            refreshHtml();
        }

        @Override
        public void redo(ValueGroup context)
        {
            Film f = panel.getData();

            if (f == null)
            {
                return;
            }

            applyDeleteReplay(panel, f, filmId, replayId);
            applyUi(uiAfter);
            refreshHtml();
        }

        private void applyUi(MapType data)
        {
            if (data != null && panel.getRoot() != null)
            {
                panel.getRoot().applyAllUndoData(data);
            }
        }
    }

    /* Drag a library character (replay) onto the timeline, or move a placed
     * track to a new position. Both are expressed purely in stable replay ids:
     * an id not yet on the timeline is placed (inserted before targetReplayId,
     * or appended when targetReplayId is empty); an already-placed id is
     * reordered. Track order lives in TrackOrderStore, so film.replays order is
     * never mutated for display purposes. */
    private static void dropActor(UIFilmPanel panel, String replayId, String targetReplayId)
    {
        placeOrReorderTrack(panel, replayId, targetReplayId);
    }

    private static void placeActor(UIFilmPanel panel, String replayId, String targetReplayId)
    {
        placeOrReorderTrack(panel, replayId, targetReplayId);
    }

    private static void reorderTrack(UIFilmPanel panel, String replayId, String targetReplayId)
    {
        placeOrReorderTrack(panel, replayId, targetReplayId);
    }

    private static void placeOrReorderTrack(UIFilmPanel panel, String replayId, String targetReplayId)
    {
        Film film = panel.getData();

        if (film == null || replayId == null || replayId.isEmpty())
        {
            return;
        }

        String filmId = film.getId();
        List<String> order = TrackOrderStore.get(filmId);

        if (order.contains(replayId))
        {
            order.remove(replayId);

            int idx = (targetReplayId != null && !targetReplayId.isEmpty()) ? order.indexOf(targetReplayId) : order.size();

            if (idx < 0)
            {
                idx = order.size();
            }

            order.add(idx, replayId);
            TrackOrderStore.set(filmId, order);
        }
        else
        {
            int tgt = order.size();

            if (targetReplayId != null && !targetReplayId.isEmpty())
            {
                int idx = order.indexOf(targetReplayId);

                if (idx >= 0)
                {
                    tgt = idx;
                }
            }

            TrackOrderStore.insert(filmId, replayId, tgt);
        }

        panel.save();
        refreshHtml();
    }

    /* F3.2/D2: destroy a track without deleting the replay. The replay returns to
     * the asset library; matte references on other tracks are cleared (S15). The
     * whole operation is a single undoable transaction so Ctrl+Z re-places the
     * track AND restores the matte references. */
    private static void unplaceActor(UIFilmPanel panel, String replayId)
    {
        Film film = panel.getData();

        if (film == null || replayId == null || replayId.isEmpty())
        {
            return;
        }

        String filmId = film.getId();
        List<String> orderBefore = TrackOrderStore.get(filmId);
        List<JsonObject> matteBefore = snapshotMatteSources(film, filmId, replayId);
        MapType uiBefore = panel.getRoot() == null ? null : panel.getRoot().collectAllUndoData();

        TrackOrderStore.remove(filmId, replayId);
        TrackPropStore.clearMatteSource(filmId, replayId);
        panel.save();

        MapType uiAfter = panel.getRoot() == null ? null : panel.getRoot().collectAllUndoData();

        UnplaceActorUndo undo = new UnplaceActorUndo(panel, filmId, replayId, orderBefore, matteBefore);
        undo.uiAfter = uiAfter;

        if (panel.getUndoHandler() != null)
        {
            panel.getUndoHandler().getUndoManager().pushUndo(undo);
        }

        refreshHtml();
    }

    /* D2: single undo entry for unplaceActor (track removal + matte cascade). */
    private static class UnplaceActorUndo implements IUndo<ValueGroup>
    {
        private final UIFilmPanel panel;
        private final String filmId;
        private final String replayId;
        private final List<String> orderBefore;
        private final List<JsonObject> matteBefore;
        private MapType uiBefore;
        private MapType uiAfter;

        UnplaceActorUndo(UIFilmPanel panel, String filmId, String replayId, List<String> orderBefore, List<JsonObject> matteBefore)
        {
            this.panel = panel;
            this.filmId = filmId;
            this.replayId = replayId;
            this.orderBefore = orderBefore;
            this.matteBefore = matteBefore;
        }

        @Override
        public IUndo<ValueGroup> noMerging()
        {
            return this;
        }

        @Override
        public boolean isMergeable(IUndo<ValueGroup> undo)
        {
            return false;
        }

        @Override
        public void merge(IUndo<ValueGroup> undo)
        {}

        @Override
        public void undo(ValueGroup context)
        {
            TrackOrderStore.set(filmId, orderBefore);

            for (JsonObject snap : matteBefore)
            {
                String otherId = snap.has(SNAP_REPLAY_ID) ? snap.get(SNAP_REPLAY_ID).getAsString() : null;
                snap.remove(SNAP_REPLAY_ID);

                if (otherId != null)
                {
                    restoreTrackProp(filmId, otherId, snap);
                }
            }

            panel.save();
            applyUi(uiBefore);
            refreshHtml();
        }

        @Override
        public void redo(ValueGroup context)
        {
            TrackOrderStore.remove(filmId, replayId);
            TrackPropStore.clearMatteSource(filmId, replayId);
            panel.save();
            applyUi(uiAfter);
            refreshHtml();
        }

        private void applyUi(MapType data)
        {
            if (data != null && panel.getRoot() != null)
            {
                panel.getRoot().applyAllUndoData(data);
            }
        }
    }

    /* F4: update a track's visual property in the lingfeng side-table, keyed by
     * stable replay id. Guards: blendMode whitelist (default normal), opacity is
     * clamped by TrackProp.apply, matteSource may not reference the track itself
     * (S14) and invalid sources degrade to no mask (S15). */
    private static void setTrackProp(UIFilmPanel panel, String replayId, String prop, String value)
    {
        Film film = panel.getData();

        if (film == null || replayId == null || replayId.isEmpty() || prop == null)
        {
            return;
        }

        /* F4.1/S14: a track cannot be its own matte source. */
        if ("matteSource".equals(prop) && replayId.equals(value))
        {
            return;
        }

        /* F4.3: reject unsupported blend modes; default (normal) is retained. */
        if ("blendMode".equals(prop) && !TrackProp.isSupportedBlendMode(value))
        {
            return;
        }

        TrackPropStore.set(film.getId(), replayId, prop, value);
        panel.save();
        refreshHtml();
    }

    private static void deleteScene(UIFilmPanel panel, String sceneId)
    {
        SceneManager scenes = SceneManager.get();

        if (scenes == null || sceneId == null || sceneId.isEmpty())
        {
            return;
        }

        Scene target = scenes.getById(sceneId);

        if (target == null)
        {
            return;
        }

        scenes.delete(target);
        panel.save();
        refreshHtml();
    }

    private static void deleteSequence(UIFilmPanel panel, String seqId)
    {
        SequenceManager sequences = SequenceManager.get();

        if (sequences == null || seqId == null || seqId.isEmpty())
        {
            return;
        }

        Sequence target = sequences.getById(seqId);

        if (target == null)
        {
            return;
        }

        sequences.delete(target);

        if (activeSequenceId != null && activeSequenceId.equals(seqId))
        {
            activeSequenceId = null;
        }

        panel.save();
        refreshHtml();
    }

    private static void toggleReplay(UIFilmPanel panel, int index)
    {
        Film film = panel.getData();

        if (film == null)
        {
            return;
        }

        int i = 0;

        for (Replay replay : film.replays.getList())
        {
            if (i++ == index)
            {
                BaseValue.edit(film, f ->
                {
                    Replay r = f.replays.getList().get(index);
                    r.enabled.set(!r.enabled.get());
                });

                return;
            }
        }
    }
}
