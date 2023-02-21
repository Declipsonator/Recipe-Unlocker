package me.declipsonator.recipeunlocker.mixin;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import me.declipsonator.recipeunlocker.RecipeUnlocker;
import me.declipsonator.recipeunlocker.util.RecipeBookRecipes;
import net.minecraft.client.gui.screen.ingame.*;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeGridAligner;
import net.minecraft.recipe.RecipeMatcher;
import net.minecraft.screen.AbstractRecipeScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Iterator;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin<C extends Inventory> implements RecipeGridAligner<Integer> {
	PlayerInventory inventory;
	AbstractRecipeScreenHandler<C> handler;
	RecipeMatcher matcher = new RecipeMatcher();
	
	@Inject(method = "clickRecipe", at = @At("HEAD"), cancellable = true)
	public void clickedRecipe(int syncId, Recipe<?> recipe, boolean craftAll, CallbackInfo cir) {
		if(!(RecipeUnlocker.mc.currentScreen instanceof HandledScreen<?> handledScreen && RecipeBookRecipes.isCached(recipe) && RecipeUnlocker.mc.player != null)) return;
		RecipeBookWidget widget = null;
		if (handledScreen instanceof InventoryScreen playerScreen) {
			widget = playerScreen.getRecipeBookWidget();
		} else if(handledScreen instanceof CraftingScreen craftingScreen) {
			widget = craftingScreen.getRecipeBookWidget();
		} else if(handledScreen instanceof FurnaceScreen furnaceScreen) {
			widget = furnaceScreen.getRecipeBookWidget();
		} else if(handledScreen instanceof SmokerScreen smokerScreen) {
			widget = smokerScreen.getRecipeBookWidget();
		} else if(handledScreen instanceof BlastFurnaceScreen blastFurnaceScreen) {
			widget = blastFurnaceScreen.getRecipeBookWidget();
		}
		if(widget == null) return;
		widget.reset();

		ClientPlayerEntity entity = RecipeUnlocker.mc.player;

		handler = (AbstractRecipeScreenHandler<C>) RecipeUnlocker.mc.player.currentScreenHandler;
		inventory = entity.getInventory();
		handler.clearCraftingSlots();

		if (!canReturnInputs() && !entity.isCreative()) {
			return;
		}
		matcher.clear();
		entity.getInventory().populateRecipeFinder(matcher);
		handler.populateRecipeFinder(matcher);
		if (matcher.match(recipe, null)) {
			fillInputSlots((Recipe<C>) recipe, craftAll);

		} else {
			returnInputs();
			widget.showGhostRecipe(recipe, RecipeUnlocker.mc.player.currentScreenHandler.slots);

		}
		entity.getInventory().markDirty();

		cir.cancel();
	}

	/*
		The code below was pretty much copied from the server side
		I couldn't interface with it by just referencing it or smth so I just pasted
	 */
	

	protected void returnInputs() {
		for (int i = 0; i < handler.getCraftingSlotCount(); ++i) {
			if (!handler.canInsertIntoSlot(i)) continue;
			ItemStack itemStack = handler.getSlot(i).getStack().copy();
			inventory.offer(itemStack, false);
			handler.getSlot(i).setStack(itemStack);
		}
		handler.clearCraftingSlots();
	}

	protected void fillInputSlots(Recipe<C> recipe, boolean craftAll) {
		IntArrayList intList;
		int j;
		boolean bl = handler.matches(recipe);
		int i = matcher.countCrafts(recipe, null);
		if (bl) {
			for (j = 0; j < handler.getCraftingHeight() * handler.getCraftingWidth() + 1; ++j) {
				ItemStack itemStack;
				if (j == handler.getCraftingResultSlotIndex() || (itemStack = handler.getSlot(j).getStack()).isEmpty() || Math.min(i, itemStack.getMaxCount()) >= itemStack.getCount() + 1) continue;
				return;
			}
		}
		if (matcher.match(recipe, intList = new IntArrayList(), j = getAmountToFill(craftAll, i, bl))) {
			int k = j;
			for (int l : intList) {
				int m = RecipeMatcher.getStackFromId(l).getMaxCount();
				if (m >= k) continue;
				k = m;
			}
			j = k;
			if (matcher.match(recipe, intList, j)) {
				returnInputs();
				alignRecipeToGrid(handler.getCraftingWidth(), handler.getCraftingHeight(), handler.getCraftingResultSlotIndex(), recipe, intList.iterator(), j);
			}
		}
	}

	@Override
	public void acceptAlignedInput(Iterator<Integer> inputs, int slot, int amount, int gridX, int gridY) {
		Slot slot2 = handler.getSlot(slot);
		ItemStack itemStack = RecipeMatcher.getStackFromId(inputs.next());
		if (!itemStack.isEmpty()) {
			for (int i = 0; i < amount; ++i) {
				fillInputSlot(slot2, itemStack);
			}
		}
	}

	protected int getAmountToFill(boolean craftAll, int limit, boolean recipeInCraftingSlots) {
		int i = 1;
		if (craftAll) {
			i = limit;
		} else if (recipeInCraftingSlots) {
			i = 64;
			for (int j = 0; j < handler.getCraftingWidth() * handler.getCraftingHeight() + 1; ++j) {
				ItemStack itemStack;
				if (j == handler.getCraftingResultSlotIndex() || (itemStack = handler.getSlot(j).getStack()).isEmpty() || i <= itemStack.getCount()) continue;
				i = itemStack.getCount();
			}
			if (i < 64) {
				++i;
			}
		}
		return i;
	}

	protected void fillInputSlot(Slot slot, ItemStack stack) {
		int i = inventory.indexOf(stack);
		if (i == -1) {
			return;
		}
		ItemStack itemStack = inventory.getStack(i).copy();
		if (itemStack.isEmpty()) {
			return;
		}
		if (itemStack.getCount() > 1) {
			inventory.removeStack(i, 1);
		} else {
			inventory.removeStack(i);
		}
		itemStack.setCount(1);
		if (slot.getStack().isEmpty()) {
			slot.setStack(itemStack);
		} else {
			slot.getStack().increment(1);
		}
	}

	private boolean canReturnInputs() {
		ArrayList<ItemStack> list = Lists.newArrayList();
		int i = getFreeInventorySlots();
		for (int j = 0; j < handler.getCraftingWidth() * handler.getCraftingHeight() + 1; ++j) {
			ItemStack itemStack;
			if (j == handler.getCraftingResultSlotIndex() || (itemStack = handler.getSlot(j).getStack().copy()).isEmpty()) continue;
			int k = inventory.getOccupiedSlotWithRoomForStack(itemStack);
			if (k == -1 && list.size() <= i) {
				for (ItemStack itemStack2 : list) {
					if (!itemStack2.isItemEqual(itemStack) || itemStack2.getCount() == itemStack2.getMaxCount() || itemStack2.getCount() + itemStack.getCount() > itemStack2.getMaxCount()) continue;
					itemStack2.increment(itemStack.getCount());
					itemStack.setCount(0);
					break;
				}
				if (itemStack.isEmpty()) continue;
				if (list.size() < i) {
					list.add(itemStack);
					continue;
				}
				return false;
			}
			if (k != -1) continue;
			return false;
		}
		return true;
	}

	private int getFreeInventorySlots() {
		int i = 0;
		for (ItemStack itemStack : inventory.main) {
			if (!itemStack.isEmpty()) continue;
			++i;
		}
		return i;
	}
}