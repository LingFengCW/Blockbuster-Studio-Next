package lingfeng.bbsnext.mcef;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import mchorse.bbs_mod.film.Film;
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
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

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
public class EditorBridge
{
    private final UIFilmPanel panel;
    /** ID of the sequence that click-to-add drops assets into. Set when the
     *  user selects a sequence in the asset tree (enterSequence). */
    private static String activeSequenceId = null;
    private static final Gson GSON = new Gson();

    public EditorBridge(UIFilmPanel panel)
    {
        this.panel = panel;
    }

    /* -------- state (JSON) -------- */

    /** Full editor state as a JSON string, pushed to the page as window.bbsState. */
    public static String getStateJson(EditorBridge bridge)
    {
        JsonObject root = new JsonObject();

        UIFilmPanel panel = bridge.panel;

        BBSProject project = ProjectManager.get() == null ? null : ProjectManager.get().getCurrent();
        SceneManager scenes = SceneManager.get();
        Film film = panel.getData();

        root.addProperty("project", project == null ? "" : project.name);
        root.addProperty("film", film == null ? "" : film.getId());

        Scene current = scenes == null ? null : scenes.getCurrent();
        root.addProperty("scene", current == null ? "" : current.name);

        root.addProperty("cursor", panel.getCursor());
        root.addProperty("running", panel.isRunning());
        root.addProperty("dirty", panel.isDirty());

        /* Scenes */
        JsonArray sceneArr = new JsonArray();

        if (scenes != null)
        {
            for (Scene scene : scenes.getScenes())
            {
                JsonObject obj = new JsonObject();
                obj.addProperty("id", scene.id);
                obj.addProperty("name", scene.name);
                sceneArr.add(obj);
            }
        }

        root.add("scenes", sceneArr);

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
                obj.addProperty("label", replay.getName());
                obj.addProperty("actor", replay.actor.get());
                obj.addProperty("fp", replay.fp.get());
                obj.addProperty("enabled", replay.enabled.get());
                obj.addProperty("shadow", replay.shadow.get());
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
            int i = 0;

            for (Replay replay : film.replays.getList())
            {
                JsonObject obj = new JsonObject();
                obj.addProperty("index", i);
                obj.addProperty("label", replay.label.get());

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

                i++;
            }
        }

        root.add("characters", charsArr);
        root.add("entities", entsArr);
        root.add("particles", partsArr);
        root.add("items", itemsArr);

        root.addProperty("activeSequence", activeSequenceId == null ? "" : activeSequenceId);

        /* Camera timeline clips */
        JsonArray clipArr = new JsonArray();

        if (film != null)
        {
            for (Clip clip : film.camera.get())
            {
                JsonObject obj = new JsonObject();
                obj.addProperty("title", clip.title.get());
                obj.addProperty("tick", clip.tick.get());
                obj.addProperty("layer", clip.layer.get());
                obj.addProperty("duration", clip.duration.get());
                obj.addProperty("enabled", clip.enabled.get());
                clipArr.add(obj);
            }
        }

        root.add("clips", clipArr);

        root.addProperty("duration", film == null ? 0 : film.camera.calculateDuration());

        return GSON.toJson(root);
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
                break;
            case "setCursor":
                panel.setCursor(req.get("tick").getAsInt());
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
            case "newScene":
                openSceneDialog(panel);
                break;
            case "newSequence":
                newSequence(panel);
                break;
            case "newCharacter":
                openCharacterDialog(panel);
                break;
            case "deleteReplay":
                deleteReplay(panel, req.get("index").getAsInt());
                break;
            case "toggleReplay":
                toggleReplay(panel, req.get("index").getAsInt());
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
                panel.getController().startRecording(null);
                break;
            case "toggleInstantKeys":
                panel.getController().toggleInstantKeyframes();
                break;
            case "newEntity":
                panel.getController().createEntities();
                break;
            case "newParticle":
                newParticle(panel);
                break;
            case "newItem":
                openItemDialog(panel);
                break;
            case "openEquip":
                openEquipDialog(panel, req.has("index") ? req.get("index").getAsInt() : -1);
                break;
            case "equip":
                equip(panel,
                    req.has("index") ? req.get("index").getAsInt() : -1,
                    req.has("slot") ? req.get("slot").getAsString() : "",
                    req.has("item") ? req.get("item").getAsString() : "");
                break;
            case "clipOp":
                clipOp(panel, req.has("op") ? req.get("op").getAsString() : "",
                    req.has("index") ? req.get("index").getAsInt() : -1);
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
                break;
            case "enterSequence":
                /* Select a sequence as the drop target for click-to-add. */
                activeSequenceId = req.has("id") ? req.get("id").getAsString() : null;
                break;
            case "addToCurrent":
                /* Click an asset -> it auto-enters the active (or first)
                 * sequence. Characters map to the native "mcpr" ref type. */
                return addToCurrent(bridge,
                    req.has("type") ? req.get("type").getAsString() : "",
                    req.has("id") ? req.get("id").getAsString() : "");
            default:
                return "{\"ok\":false,\"error\":\"unknown action " + action + "\"}";
        }

        return "{\"ok\":true}";
    }

    /* -------- backpack (cross-work asset library) -------- */

    /**
     * Put an asset of the current work into the global backpack. Scenes go
     * in with their whole film payload, sequences drag the scenes they
     * reference along, so the item is usable in any other work.
     */
    private static String toBackpack(UIFilmPanel panel, String type, String id)
    {
        java.util.List<String> errors = BackpackService.put(type, id);

        panel.assetBin.refresh();

        if (!errors.isEmpty())
        {
            return "{\"ok\":false,\"error\":" + GSON.toJson(errors.get(0)) + "}";
        }

        return "{\"ok\":true}";
    }

    /** Take one backpack item into the current work (never overwrites). */
    private static String fromBackpack(UIFilmPanel panel, String name)
    {
        java.util.List<String> errors = BackpackService.take(name);

        panel.assetBin.refresh();

        if (!errors.isEmpty())
        {
            return "{\"ok\":false,\"error\":" + GSON.toJson(errors.get(0)) + "}";
        }

        return "{\"ok\":true}";
    }

    /* -------- tool / clip actions -------- */

    /** Create a new particle replay (like the entity panel's PARTICLE type). */
    private static void newParticle(UIFilmPanel panel)
    {
        Film film = panel.getData();

        if (film == null)
        {
            return;
        }

        Replay replay = film.replays.addReplay();
        replay.form.set(new mchorse.bbs_mod.forms.forms.ParticleForm());
        replay.label.set("粒子");
        panel.replayEditor.setReplay(replay);
        panel.showPanel(1);
        panel.fillData();
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

        ItemStack stack = ItemStack.EMPTY;

        if (itemId != null && !itemId.isEmpty())
        {
            try
            {
                stack = new ItemStack(BuiltInRegistries.ITEM.get(Identifier.parse(itemId)).orElseThrow().value());
            }
            catch (Throwable t)
            {
                return;
            }
        }

        final KeyframeChannel<ItemStack> ch = channel;
        final ItemStack st = stack;
        final int tick = panel.getCursor();

        BaseValue.edit(film, f -> ch.insert(tick, st));
    }

    /* -------- native (OS window) dialogs -------- */

    /**
     * Pop a real OS window to collect the scene name + background world, then
     * create the scene. Replaces {@code UIFilmPanel.newScene()}'s in-game
     * overlay panel, which renders *under* the MCEF HTML texture and is
     * therefore buried/unclickable.
     */
    private static void openSceneDialog(UIFilmPanel panel)
    {
        NativeDialog.sceneDialog((name, bg) ->
        {
            if (name == null)
            {
                return;
            }

            Minecraft.getInstance().execute(() ->
            {
                SceneManager scenes = SceneManager.get();

                if (scenes == null)
                {
                    return;
                }

                String sceneName = name.isEmpty() ? ("Scene " + (scenes.getScenes().size() + 1)) : name;
                Scene scene = scenes.create(sceneName, bg == null ? "" : bg);

                panel.assetBin.refresh();
                panel.openScene(scene);
            });
        });
    }

    /**
     * Pop a real OS window to collect character/entity options (name, form
     * type, actor/shadow/looping), then create the replay. Replaces
     * {@code UIFilmPanel.newCharacter()}'s buried overlay panel.
     */
    private static void openCharacterDialog(UIFilmPanel panel)
    {
        NativeDialog.characterDialog(r ->
        {
            if (r == null)
            {
                return;
            }

            Minecraft.getInstance().execute(() ->
            {
                Film film = panel.getData();

                if (film == null)
                {
                    panel.getContext().notifyError(UIKeys.ASSETS_NEED_SCENE);

                    return;
                }

                Replay replay = film.replays.addReplay();

                Form form = switch (r.type)
                {
                    case "MODEL" -> new ModelForm();
                    case "PARTICLE" -> new ParticleForm();
                    case "BLOCK" -> new BlockForm();
                    default -> new MobForm();
                };

                replay.form.set(form);

                if (r.name != null && !r.name.isEmpty())
                {
                    replay.label.set(r.name);
                }

                replay.actor.set(r.actor);
                replay.shadow.set(r.shadow);
                replay.looping.set(r.looping ? 1 : 0);

                panel.replayEditor.setReplay(replay);
                panel.showPanel(1);
                panel.fillData();
            });
        });
    }

    /** Pop a real OS window to collect an item id, then create an item replay. */
    private static void openItemDialog(UIFilmPanel panel)
    {
        NativeDialog.itemDialog(id ->
            Minecraft.getInstance().execute(() -> newItem(panel, id == null ? "" : id)));
    }

    /** Pop a real OS equipment window for the selected actor replay. */
    private static void openEquipDialog(UIFilmPanel panel, int index)
    {
        Film film = panel.getData();

        if (film == null || index < 0 || index >= film.replays.getList().size())
        {
            return;
        }

        Replay replay = film.replays.getList().get(index);
        String label = replay.label.get();

        NativeDialog.equipDialog("为 " + (label == null ? "角色" : label) + " 装配装备", entry ->
            Minecraft.getInstance().execute(() ->
                equip(panel, index, entry.slot, entry.itemId == null ? "" : entry.itemId)));
    }

    /**
     * Create a new Sequence (a higher-level container that can reference
     * scenes but NOT characters/entities), referencing the current scene.
     * Mirrors UIAssetBin's "new sequence" action.
     */
    private static void newSequence(UIFilmPanel panel)
    {
        SceneManager scenes = SceneManager.get();

        if (scenes == null || scenes.getCurrent() == null)
        {
            return;
        }

        SequenceManager sequences = SequenceManager.get();
        Scene current = scenes.getCurrent();

        Sequence sequence = sequences.create(current.name + " seq");
        sequences.addRef(sequence, Sequence.SequenceRef.SCENE, current.id);
        activeSequenceId = sequence.id;

        panel.fillData();
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

    private static void clipOp(UIFilmPanel panel, String op, int index)
    {
        Film film = panel.getData();

        if (film == null || index < 0 || index >= film.camera.get().size())
        {
            return;
        }

        switch (op)
        {
            case "delete":
            {
                final int i = index;

                BaseValue.edit(film, f -> f.camera.get().remove(i));

                break;
            }
            case "split":
            {
                /* Split the clip at the playhead. */
                final int i = index;
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

                return;
            }
        }
    }

    private static void closeEditor(UIFilmPanel panel)
    {
        UIDashboard dashboard = panel.dashboard;

        panel.save();
        dashboard.setPanel(dashboard.getPanel(lingfeng.bbsnext.ui.dashboard.panels.UIProjectsPanel.class));
    }

    private static void deleteReplay(UIFilmPanel panel, int index)
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
                final Replay target = replay;

                BaseValue.edit(film, f -> f.replays.remove(target));

                return;
            }
        }
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
