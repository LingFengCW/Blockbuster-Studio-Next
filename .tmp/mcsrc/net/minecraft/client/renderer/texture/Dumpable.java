/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.resources.Identifier
 */
package net.minecraft.client.renderer.texture;

import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.resources.Identifier;

public interface Dumpable {
    public void dumpContents(Identifier var1, Path var2) throws IOException;
}

