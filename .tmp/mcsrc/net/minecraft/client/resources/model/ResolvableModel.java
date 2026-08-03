/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.resources.Identifier
 */
package net.minecraft.client.resources.model;

import net.minecraft.resources.Identifier;

public interface ResolvableModel {
    public void resolveDependencies(Resolver var1);

    public static interface Resolver {
        public void markDependency(Identifier var1);
    }
}

