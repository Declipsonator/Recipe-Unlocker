package me.declipsonator.recipeunlocker.mixin;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import me.declipsonator.recipeunlocker.RecipeUnlocker;
import me.declipsonator.recipeunlocker.util.MoveUtils;
import me.declipsonator.recipeunlocker.util.RecipeBookRecipes;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.*;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeGridAligner;
import net.minecraft.recipe.RecipeMatcher;
import net.minecraft.recipe.input.RecipeInput;
import net.minecraft.screen.AbstractRecipeScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

import static me.declipsonator.recipeunlocker.RecipeUnlocker.mc;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin<I extends RecipeInput, R extends Recipe<I>> implements RecipeGridAligner<Integer>  {
	@Shadow @Final private MinecraftClient client;
	@Unique
	PlayerInventory inventory;
	@Unique
	AbstractRecipeScreenHandler<I, R> handler;
	@Unique
	RecipeMatcher matcher = new RecipeMatcher();
	@Unique
	ClientPlayerInteractionManager im;

	@Inject(method = "clickRecipe", at = @At("HEAD"), cancellable = true)
	public void clickedRecipe(int syncId, RecipeEntry<?> recipe, boolean craftAll, CallbackInfo ci) {
		if(!(RecipeUnlocker.mc.currentScreen instanceof HandledScreen<?> handledScreen && RecipeBookRecipes.isCached(recipe.value()) && RecipeUnlocker.mc.player != null)) return;
		RecipeBookWidget widget = getRecipeBookWidget(handledScreen);
		if(widget == null) return;
		widget.reset();

		ClientPlayerEntity entity = RecipeUnlocker.mc.player;


		im = MinecraftClient.getInstance().interactionManager;
		handler = (AbstractRecipeScreenHandler<I, R>) RecipeUnlocker.mc.player.currentScreenHandler;
		inventory = entity.getInventory();

		if (!canReturnInputs() && !entity.isCreative()) {
			return;
		}
		matcher.clear();
		entity.getInventory().populateRecipeFinder(matcher);
		handler.populateRecipeFinder(matcher);
		if (matcher.match(recipe.value(), null)) {
			fillInputSlots((RecipeEntry<R>) recipe, craftAll);

		} else {
			returnInputs();
			widget.showGhostRecipe(recipe, RecipeUnlocker.mc.player.currentScreenHandler.slots);

		}
		entity.getInventory().markDirty();

		ci.cancel();
	}

	@Unique
	@Nullable
	private static RecipeBookWidget getRecipeBookWidget(HandledScreen<?> handledScreen) {
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
		return widget;
	}

	/*
		The code below was pretty much copied from the server side
		I couldn't interface with it by just referencing it or smth so I just pasted

		From InputSlotFiller
	 */

	@Unique
	protected void returnInputs() {
		for (int i = 0; i < handler.getCraftingSlotCount(); ++i) {
			if (!handler.canInsertIntoSlot(i)) continue;
//			ItemStack itemStack = handler.getSlot(i).getStack().copy();
//			inventory.offer(itemStack, false);
//			handler.getSlot(i).setStackNoCallbacks(itemStack);
			mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, handler.getSlot(i).id, 0, SlotActionType.QUICK_MOVE, mc.player);
			MoveUtils.pickupId(handler.getSlot(i).id, handler.getSlot(i).getStack().getCount());
			mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, handler.getSlot(i).id, 0, SlotActionType.THROW, mc.player);

		}
		handler.clearCraftingSlots();
	}

	@Unique
	protected void fillInputSlots(RecipeEntry<R> recipe, boolean craftAll) {
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
		j = getAmountToFill(craftAll, i, bl);
		IntArrayList intList = new IntArrayList();
		if (matcher.match(recipe.value(), intList, j)) {
			int k = j;
            for (Integer integer : intList) {
                int m;
                int l = integer;
                ItemStack itemStack2 = RecipeMatcher.getStackFromId(l);
                if (itemStack2.isEmpty() || (m = itemStack2.getMaxCount()) >= k) continue;
                k = m;
            }
			j = k;
			if (matcher.match(recipe.value(), intList, j)) {
				returnInputs();
				alignRecipeToGrid(handler.getCraftingWidth(), handler.getCraftingHeight(), handler.getCraftingResultSlotIndex(), recipe, intList.iterator(), j);
			}
		}
	}

	@Override
	public void acceptAlignedInput(Integer integer, int i, int j, int k, int l) {
		Slot slot = handler.getSlot(i);
		ItemStack itemStack = RecipeMatcher.getStackFromId(integer);
		if (itemStack.isEmpty()) {
			return;
		}
		int m = j;
		while (m > 0) {
			if ((m = fillInputSlot(slot, itemStack, m)) != -1) continue;
			return;
		}
	}

	@Unique
	protected int getAmountToFill(boolean craftAll, int limit, boolean recipeInCraftingSlots) {
		int i = 1;
		if (craftAll) {
			i = limit;
		} else if (recipeInCraftingSlots) {
			i = Integer.MAX_VALUE;
			for (int j = 0; j < handler.getCraftingWidth() * handler.getCraftingHeight() + 1; ++j) {
				ItemStack itemStack;
				if (j == handler.getCraftingResultSlotIndex() || (itemStack = handler.getSlot(j).getStack()).isEmpty() || i <= itemStack.getCount()) continue;
				i = itemStack.getCount();
			}
			if (i != Integer.MAX_VALUE) {
				++i;
			}
		}
		return i;
	}

	@Unique
	protected int fillInputSlot(Slot slot, ItemStack stack, int i) {

		int k;
		int max = -1;
		int itemStackSlot = 0;

		for (int p = 0; p < 36; p++) {
			if (inventory.getStack(p).isEmpty()) {
				continue;
			}
			if (inventory.getStack(p).getItem() == stack.getItem()) {
				if (inventory.getStack(p).getCount() > max) {
					max = inventory.getStack(p).getCount();
					itemStackSlot = p;
				}
			}
		}

		ItemStack itemStack = inventory.getStack(itemStackSlot);


		if (i <= itemStack.getCount()) {
//			inventory.removeStack(j, i);
			MoveUtils.pickup(itemStackSlot, i);

			k = i;
		} else {
//			inventory.removeStack(j);
			MoveUtils.pickupAll(itemStackSlot);
			k = client.player.currentScreenHandler.getCursorStack().getCount();

		}
		if (slot.getStack().isEmpty()) {
//			slot.setStackNoCallbacks(itemStack.copyWithCount(k));
			MoveUtils.putId(slot.id, k);
		} else {
//			slot.getStack().increment(k);
			MoveUtils.putAllId(slot.id);

		}
		return i - k;
	}

	@Unique
	private boolean canReturnInputs() {
		ArrayList<ItemStack> list = Lists.newArrayList();
		int i = getFreeInventorySlots();
		for (int j = 0; j < handler.getCraftingWidth() * handler.getCraftingHeight() + 1; ++j) {
			ItemStack itemStack;
			if (j == handler.getCraftingResultSlotIndex() || (itemStack = handler.getSlot(j).getStack().copy()).isEmpty()) continue;
			int k = inventory.getOccupiedSlotWithRoomForStack(itemStack);
			if (k == -1 && list.size() <= i) {
				for (ItemStack itemStack2 : list) {
					if (!ItemStack.areItemsEqual(itemStack2, itemStack) || itemStack2.getCount() == itemStack2.getMaxCount() || itemStack2.getCount() + itemStack.getCount() > itemStack2.getMaxCount()) continue;
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

	@Unique
	private int getFreeInventorySlots() {
		int i = 0;
		for (ItemStack itemStack : inventory.main) {
			if (!itemStack.isEmpty()) continue;
			++i;
		}
		return i;
	}
}