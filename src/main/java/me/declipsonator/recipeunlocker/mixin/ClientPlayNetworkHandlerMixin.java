package me.declipsonator.recipeunlocker.mixin;

import me.declipsonator.recipeunlocker.RecipeUnlocker;
import me.declipsonator.recipeunlocker.util.RecipeBookRecipes;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.recipebook.ClientRecipeBook;
import net.minecraft.network.packet.s2c.play.SynchronizeRecipesS2CPacket;
import net.minecraft.network.packet.s2c.play.UnlockRecipesS2CPacket;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {

	@Shadow
	@Final
	private RecipeManager recipeManager;

	@Inject(method = "onSynchronizeRecipes", at = @At("HEAD"))
	private void synchronizingRecipes(SynchronizeRecipesS2CPacket packet, CallbackInfo cir) {
		if(RecipeUnlocker.mc.player == null) return;
		ClientRecipeBook recipeBook = RecipeUnlocker.mc.player.getRecipeBook();
		packet.getRecipes().forEach(recipeBook::add);
		RecipeBookRecipes.setRecipes(packet.getRecipes());
	}

	@Inject(method = "onUnlockRecipes", at = @At("HEAD"))
	private void unlockRecipes(UnlockRecipesS2CPacket packet, CallbackInfo cir) {
		switch (packet.getAction()) {
			case ADD, INIT -> {
				for (Identifier identifier : packet.getRecipeIdsToChange()) {
					this.recipeManager.get(identifier).ifPresent(RecipeBookRecipes::removeRecipeFromCache);
				}
			}
			case REMOVE -> {
				for (Identifier identifier : packet.getRecipeIdsToChange()) {
					this.recipeManager.get(identifier).ifPresent(RecipeBookRecipes::addRecipe);
				}
			}
		}
	}
}