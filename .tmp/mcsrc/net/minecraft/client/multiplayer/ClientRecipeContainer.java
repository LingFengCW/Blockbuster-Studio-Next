/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.world.item.crafting.RecipeAccess
 *  net.minecraft.world.item.crafting.RecipePropertySet
 *  net.minecraft.world.item.crafting.SelectableRecipe$SingleInputSet
 *  net.minecraft.world.item.crafting.StonecutterRecipe
 */
package net.minecraft.client.multiplayer;

import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.RecipeAccess;
import net.minecraft.world.item.crafting.RecipePropertySet;
import net.minecraft.world.item.crafting.SelectableRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;

public class ClientRecipeContainer
implements RecipeAccess {
    private final Map<ResourceKey<RecipePropertySet>, RecipePropertySet> itemSets;
    private final SelectableRecipe.SingleInputSet<StonecutterRecipe> stonecutterRecipes;

    public ClientRecipeContainer(Map<ResourceKey<RecipePropertySet>, RecipePropertySet> itemSets, SelectableRecipe.SingleInputSet<StonecutterRecipe> stonecutterRecipes) {
        this.itemSets = itemSets;
        this.stonecutterRecipes = stonecutterRecipes;
    }

    public RecipePropertySet propertySet(ResourceKey<RecipePropertySet> id) {
        return this.itemSets.getOrDefault(id, RecipePropertySet.EMPTY);
    }

    public SelectableRecipe.SingleInputSet<StonecutterRecipe> stonecutterRecipes() {
        return this.stonecutterRecipes;
    }
}

