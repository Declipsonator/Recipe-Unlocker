package me.declipsonator.recipeunlocker.util;

import net.minecraft.screen.*;
import net.minecraft.screen.slot.SlotActionType;

import static me.declipsonator.recipeunlocker.RecipeUnlocker.mc;

public class MoveUtils {
    public static final int HOTBAR_START = 0;
    public static final int HOTBAR_END = 8;

    public static final int MAIN_START = 9;
    public static final int MAIN_END = 35;

    public static final int ARMOR_START = 36;
    public static final int ARMOR_END = 39;

    public static int indexToId(int i) {
        if (mc.player == null) {
            return -1;
        }
        ScreenHandler handler = mc.player.currentScreenHandler;
        if (handler instanceof PlayerScreenHandler) return survivalInventory(i);
        if (handler instanceof CraftingScreenHandler) return craftingTable(i);
        if (handler instanceof FurnaceScreenHandler) return furnace(i);
        if (handler instanceof BlastFurnaceScreenHandler) return furnace(i);
        if (handler instanceof SmokerScreenHandler) return furnace(i);


        return -1;
    }

    private static int survivalInventory(int i) {
        if (isHotbar(i)) return 36 + i;
        if (isArmor(i)) return 5 + (i - 36);
        return i;
    }

    private static int craftingTable(int i) {
        if (isHotbar(i)) return 37 + i;
        if (isMain(i)) return i + 1;
        return -1;
    }

    private static int furnace(int i) {
        if (isHotbar(i)) return 30 + i;
        if (isMain(i)) return 3 + (i - 9);
        return -1;
    }

    public static boolean isHotbar(int i) {
        return i >= HOTBAR_START && i <= HOTBAR_END;
    }

    public static boolean isMain(int i) {
        return i >= MAIN_START && i <= MAIN_END;
    }

    public static boolean isArmor(int i) {
        return i >= ARMOR_START && i <= ARMOR_END;
    }


    public static void pickupId(int id, int amount) {
        click(id, 0);
        int amountHeld = mc.player.currentScreenHandler.getCursorStack().getCount();
        for (int i = 0; i < amountHeld - amount; i++) {
            click(id, 1);
        }
    }

    public static void pickup(int index, int amount) {
        int id = indexToId(index);
        pickupId(id, amount);
    }

    public static void putId(int id, int amount) {
        for(int i = 0; i < amount; i++) {
            click(id, 1);
        }
    }


    public static void putAllId(int id) {
        click(id, 0);
    }



    private static void click(int id, int button) {
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, id, button, SlotActionType.PICKUP, mc.player);
    }

}
