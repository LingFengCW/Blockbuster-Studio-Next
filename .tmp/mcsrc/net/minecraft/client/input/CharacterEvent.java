/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.StringUtil
 */
package net.minecraft.client.input;

import net.minecraft.util.StringUtil;

public record CharacterEvent(int codepoint) {
    public String codepointAsString() {
        return Character.toString(this.codepoint);
    }

    public boolean isAllowedChatCharacter() {
        return StringUtil.isAllowedChatCharacter((int)this.codepoint);
    }
}

