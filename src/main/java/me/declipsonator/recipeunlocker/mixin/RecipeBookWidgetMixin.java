package me.declipsonator.recipeunlocker.mixin;

import me.declipsonator.recipeunlocker.util.GhostSlotClear;
import net.minecraft.client.gui.screen.recipebook.RecipeBookGhostSlots;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RecipeBookWidget.class)
public class RecipeBookWidgetMixin implements GhostSlotClear {


	@Shadow
	@Final
	protected RecipeBookGhostSlots ghostSlots;

	@Override
	public void clearGhostSlots() {
		this.ghostSlots.reset();
	}
}