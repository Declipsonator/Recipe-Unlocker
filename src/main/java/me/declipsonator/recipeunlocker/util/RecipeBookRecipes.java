package me.declipsonator.recipeunlocker.util;

import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RecipeBookRecipes {
	private static final Set<Recipe<?>> RECIPES = new HashSet<>();

	public static boolean isCached(Recipe<?> recipe) {
		return RECIPES.contains(recipe);
	}

	public static void setRecipes(List<RecipeEntry<?>> recipeCache) {
		recipeCache.forEach(RecipeBookRecipes::addRecipe);
	}

	public static void addRecipe(RecipeEntry<?> recipe) {
		RECIPES.add(recipe.value());
	}

	public static void removeRecipeFromCache(RecipeEntry<?> recipe) {
		RECIPES.remove(recipe.value());
	}
}