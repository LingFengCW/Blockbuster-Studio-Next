package mchorse.bbs_mod.ui.animation;

import mchorse.bbs_mod.animation.AnimationManager;
import mchorse.bbs_mod.animation.AnimationPlayer;
import mchorse.bbs_mod.animation.BoneAnimation;
import mchorse.bbs_mod.animation.CharacterAnimation;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.UIScreen;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIConfirmOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIPromptOverlayPanel;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.HashMap;
import java.util.Map;

/**
 * Character animation editor: per-bone keyframes with bezier curves.
 * Pick a bone and an axis, then draw keyframes on the curve canvas
 * (click to add, drag points, drag the blue handles to shape the curve).
 * The timeline at the bottom scrubs through the animation; Play drives the
 * currently morphed model's bones in real time.
 */
public class UIAnimationMenu extends UIBaseMenu
{
    private UIScrollView animList;
    private UIScrollView boneList;
    private UICurveCanvas canvas;

    private UIButton playBtn;
    private UIButton delKeyBtn;
    private UIButton delBoneBtn;

    private final UIButton[] axisButtons = new UIButton[9];
    private int axis = BoneAnimation.ROT_Y;

    private CharacterAnimation animation;
    private String bone = "";

    private final Map<String, UIButton> boneButtons = new HashMap<>();
    private final Map<String, UIButton> animButtons = new HashMap<>();

    public UIAnimationMenu()
    {
        UIElement column = UI.column(6);
        column.relative(this.main).xy(0.5F, 0.5F).w(760).h(500).anchor(0.5F, 0.5F);

        column.add(UI.label(UIKeys.ANIMATION_TITLE, 24));

        /* Animation management row */
        UIButton newBtn = new UIButton(UIKeys.ANIMATION_NEW, (b) -> this.newAnimation());
        UIButton saveBtn = new UIButton(UIKeys.ANIMATION_SAVE, (b) -> this.saveAnimation());
        UIButton delAnimBtn = new UIButton(UIKeys.ANIMATION_DELETE, (b) -> this.deleteAnimation());
        /* closeThisMenu() nulls the screen, which is a black screen when
         * there is no world loaded - go back to the project menu instead. */
        UIButton backBtn = new UIButton(UIKeys.ANIMATION_BACK, (b) -> UIScreen.open(new mchorse.bbs_mod.ui.projects.UIProjectMenu()));

        UIElement animRow = UI.row(4);

        animRow.h(24);
        animRow.add(newBtn, saveBtn, delAnimBtn, backBtn);
        column.add(animRow);

        /* Three side by side panes - a column cannot place them next to each
         * other, so they live inside a row with fixed widths. Negative heights
         * make an element collapse entirely, hence explicit positive sizes. */
        UIElement body = UI.row(8);

        body.h(410);

        /* Left: animation list + bone list */
        UIElement left = UI.column(4);

        left.w(170);

        this.animList = UI.scrollView(3, 3);
        this.animList.h(90);
        left.add(this.animList);

        UIButton addBoneBtn = new UIButton(UIKeys.ANIMATION_ADD_BONE, (b) -> this.addBone());

        addBoneBtn.h(22);
        left.add(addBoneBtn);

        this.delBoneBtn = new UIButton(UIKeys.ANIMATION_DELETE_BONE, (b) -> this.removeBone());

        this.delBoneBtn.h(22);
        this.delBoneBtn.setEnabled(false);
        left.add(this.delBoneBtn);

        this.boneList = UI.scrollView(3, 3);
        this.boneList.h(258);
        left.add(this.boneList);

        /* Middle: curve canvas */
        this.canvas = new UICurveCanvas();

        /* Far right: axis buttons */
        UIElement axisColumn = UI.column(3);

        axisColumn.w(80);

        String[] labels = { "Px", "Py", "Pz", "Rx", "Ry", "Rz", "Sx", "Sy", "Sz" };

        for (int i = 0; i < 9; i++)
        {
            final int axisIndex = i;

            this.axisButtons[i] = new UIButton(IKey.raw(labels[i]), (b) -> this.selectAxis(axisIndex));
            this.axisButtons[i].h(20);
            axisColumn.add(this.axisButtons[i]);
        }

        body.add(left, this.canvas, axisColumn);
        column.add(body);

        /* Bottom: timeline */
        this.playBtn = new UIButton(UIKeys.ANIMATION_PLAY, (b) -> this.togglePlay());
        this.playBtn.w(80);

        UIButton prevBtn = new UIButton(IKey.raw("<"), (b) -> this.scrub(-1F));
        prevBtn.w(30);

        UIButton nextBtn = new UIButton(IKey.raw(">"), (b) -> this.scrub(1F));
        nextBtn.w(30);

        this.delKeyBtn = new UIButton(UIKeys.ANIMATION_DELETE_KEY, (b) -> this.deleteKeyframe());
        this.delKeyBtn.w(134);
        this.delKeyBtn.setEnabled(false);

        UIElement bottom = UI.row(6);

        bottom.h(24);
        bottom.add(this.playBtn, prevBtn, nextBtn, this.delKeyBtn);
        column.add(bottom);

        this.main.add(column);

        this.refreshAnimations();
    }

    private void refreshAnimations()
    {
        this.animButtons.clear();
        this.animList.removeAll();

        for (CharacterAnimation animation : AnimationManager.loadAll())
        {
            UIButton button = new UIButton(IKey.constant(animation.name), (b) -> this.selectAnimation(animation));

            button.h(22);
            this.animButtons.put(animation.id, button);
            this.animList.add(button);
        }
    }

    private void selectAnimation(CharacterAnimation animation)
    {
        this.animation = animation;
        AnimationPlayer.current = animation;
        AnimationPlayer.tick = 0F;
        AnimationPlayer.playing = false;

        for (Map.Entry<String, UIButton> entry : this.animButtons.entrySet())
        {
            entry.getValue().color(entry.getKey().equals(animation.id) ? Colors.A50 | Colors.ACTIVE : 0);
        }

        this.refreshBones();
    }

    private void refreshBones()
    {
        this.boneButtons.clear();
        this.boneList.removeAll();

        if (this.animation == null)
        {
            this.bone = "";
            this.updateCanvas();

            return;
        }

        /* Suggest bones from the currently morphed model. */
        java.util.List<String> bones = new java.util.ArrayList<>();

        if (FormUtilsClient.getCurrentForm() != null
            && FormUtilsClient.getRenderer(FormUtilsClient.getCurrentForm()) != null)
        {
            bones.addAll(FormUtilsClient.getRenderer(FormUtilsClient.getCurrentForm()).getBones());
        }

        for (BoneAnimation bone : this.animation.bones)
        {
            if (!bones.contains(bone.bone))
            {
                bones.add(bone.bone);
            }
        }

        if (bones.isEmpty())
        {
            UIElement label = UI.label(UIKeys.ANIMATION_NO_BONES, 18);

            label.h(18);
            this.boneList.add(label);
        }

        for (String name : bones)
        {
            UIButton button = new UIButton(IKey.constant(name), (b) -> this.selectBone(name));

            button.h(22);
            this.boneButtons.put(name, button);
            this.boneList.add(button);
        }

        this.delBoneBtn.setEnabled(this.animation != null && !this.bone.isEmpty());
    }

    private void selectBone(String bone)
    {
        this.bone = bone;

        for (Map.Entry<String, UIButton> entry : this.boneButtons.entrySet())
        {
            entry.getValue().color(entry.getKey().equals(bone) ? Colors.A50 | Colors.ACTIVE : 0);
        }

        this.delBoneBtn.setEnabled(true);
        this.updateCanvas();
    }

    private void selectAxis(int axis)
    {
        this.axis = axis;

        for (int i = 0; i < 9; i++)
        {
            this.axisButtons[i].color(i == axis ? Colors.A50 | Colors.ACTIVE : 0);
        }

        this.updateCanvas();
    }

    private void updateCanvas()
    {
        if (this.animation == null || this.bone.isEmpty())
        {
            this.canvas.setChannel(null);
            this.delKeyBtn.setEnabled(false);

            return;
        }

        BoneAnimation boneAnimation = this.animation.getOrCreateBone(this.bone);

        this.canvas.setChannel(boneAnimation.channels[this.axis]);
        this.delKeyBtn.setEnabled(true);
    }

    private void deleteKeyframe()
    {
        if (this.canvas.channel == null || this.canvas.selected == null)
        {
            return;
        }

        int index = this.canvas.channel.getKeyframes().indexOf(this.canvas.selected);

        if (index >= 0)
        {
            this.canvas.channel.remove(index);
        }

        this.canvas.selected = null;
    }

    private void newAnimation()
    {
        UIOverlay.addOverlay(this.context, new UIPromptOverlayPanel(
            UIKeys.ANIMATION_NEW,
            UIKeys.ANIMATION_NAME,
            (str) ->
            {
                if (str != null && !str.trim().isEmpty())
                {
                    this.animation = AnimationManager.create(str.trim());
                    AnimationPlayer.current = this.animation;
                    AnimationPlayer.tick = 0F;
                    this.refreshAnimations();
                    this.refreshBones();
                }
            }
        ));
    }

    private void saveAnimation()
    {
        if (this.animation != null)
        {
            AnimationManager.save(this.animation);
            this.refreshAnimations();
        }
    }

    private void deleteAnimation()
    {
        if (this.animation == null)
        {
            return;
        }

        CharacterAnimation animation = this.animation;

        UIOverlay.addOverlay(this.context, new UIConfirmOverlayPanel(
            UIKeys.ANIMATION_DELETE,
            IKey.constant(animation.name),
            (result) ->
            {
                if (result)
                {
                    AnimationManager.delete(animation);
                    this.animation = null;
                    AnimationPlayer.current = null;
                    this.refreshAnimations();
                    this.refreshBones();
                }
            }
        ));
    }

    private void addBone()
    {
        if (this.animation == null)
        {
            return;
        }

        UIOverlay.addOverlay(this.context, new UIPromptOverlayPanel(
            UIKeys.ANIMATION_ADD_BONE,
            UIKeys.ANIMATION_BONE_NAME,
            (str) ->
            {
                if (str != null && !str.trim().isEmpty())
                {
                    this.animation.getOrCreateBone(str.trim());
                    this.refreshBones();
                    this.selectBone(str.trim());
                }
            }
        ));
    }

    private void removeBone()
    {
        if (this.animation == null || this.bone.isEmpty())
        {
            return;
        }

        this.animation.removeBone(this.bone);
        this.bone = "";
        this.refreshBones();
        this.updateCanvas();
    }

    private void togglePlay()
    {
        AnimationPlayer.playing = !AnimationPlayer.playing;
        this.playBtn.label = AnimationPlayer.playing ? UIKeys.ANIMATION_STOP : UIKeys.ANIMATION_PLAY;
    }

    private void scrub(float direction)
    {
        AnimationPlayer.tick = Math.max(0F, AnimationPlayer.tick + direction);

        float length = this.animation == null ? 0F : this.animation.getLength();

        if (this.animation != null && length > 0F && AnimationPlayer.tick > length)
        {
            AnimationPlayer.tick = length;
        }
    }

    public static void open()
    {
        UIScreen.open(new UIAnimationMenu());
    }
}
