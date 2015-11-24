/*
 * Copyright or © or Copr. ZLib contributors (2015)
 *
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
 */

package fr.zcraft.zlib.components.gui;

import fr.zcraft.zlib.core.ZLib;
import fr.zcraft.zlib.tools.PluginLogger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;


/**
 * Various utility methods for GUIs.
 */
abstract public class GuiUtils
{
	static private Method addItemFlagsMethod = null;
	static private Object[] itemFlagValues = null;

	/**
	 * Initializes the GUI utilities. This method must be called on plugin enabling.
	 */
	static public void init()
	{
		try
		{
			Class<?> itemFlagClass = Class.forName("org.bukkit.inventory.ItemFlag");
			Method valuesMethod = itemFlagClass.getDeclaredMethod("values");
			itemFlagValues = (Object[]) valuesMethod.invoke(null);
			addItemFlagsMethod = ItemMeta.class.getMethod("addItemFlags", itemFlagClass);
			addItemFlagsMethod.setAccessible(true);
		}
		catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException e)
		{
			// Not supported :c
		}
		catch (InvocationTargetException e)
		{
			PluginLogger.error("Exception occurred while looking for the ItemFlag API.", e);
		}
	}

	/**
	 * Hides all the item attributes of the given {@link ItemMeta}.
	 *
	 * @param meta The {@link ItemMeta} to hide attributes from.
	 */
	static public void hideItemAttributes(ItemMeta meta)
	{
		if (addItemFlagsMethod == null) return;
		try
		{
			addItemFlagsMethod.invoke(meta, itemFlagValues);
		}
		catch (IllegalAccessException | InvocationTargetException ex)
		{
			PluginLogger.error("Exception occurred while invoking the ItemMeta.addItemFlags method.", ex);
		}
	}


	/**
	 * Stores the ItemStack at the given index of a GUI's inventory. The inventory is only updated
	 * the next time the Bukkit Scheduler runs (i.e. next server tick).
	 *
	 * @param gui  The GUI to update
	 * @param slot The slot where to put the ItemStack
	 * @param item The ItemStack to set
	 */
	static public void setItemLater(InventoryGui gui, int slot, ItemStack item)
	{
		Bukkit.getScheduler().scheduleSyncDelayedTask(ZLib.getPlugin(),
				new CreateDisplayItemTask(gui.getInventory(), item, slot));
	}

	/**
	 * One-liner to construct an {@link ItemStack}.
	 *
	 * @param material The stack's material.
	 *
	 * @return The constructed {@link ItemStack}.
	 */
	static public ItemStack makeItem(Material material)
	{
		return makeItem(material, null, (List<String>) null);
	}

	/**
	 * One-liner to construct an {@link ItemStack}.
	 *
	 * @param material The stack's material.
	 * @param title The stack's title.
	 *
	 * @return The constructed {@link ItemStack}.
	 */
	static public ItemStack makeItem(Material material, String title)
	{
		return makeItem(material, title, (List<String>) null);
	}

	/**
	 * One-liner to construct an {@link ItemStack}.
	 *
	 * @param material The stack's material.
	 * @param title The stack's title.
	 * @param loreLines The stack's lore lines.
	 *
	 * @return The constructed {@link ItemStack}.
	 */
	static public ItemStack makeItem(Material material, String title, String... loreLines)
	{
		return makeItem(material, title, Arrays.asList(loreLines));
	}

	/**
	 * One-liner to construct an {@link ItemStack}.
	 *
	 * @param material The stack's material.
	 * @param title The stack's title.
	 * @param loreLines The stack's lore lines.
	 *
	 * @return The constructed {@link ItemStack}.
	 */
	static public ItemStack makeItem(Material material, String title, List<String> loreLines)
	{
		return makeItem(new ItemStack(material), title, loreLines);
	}

	/**
	 * One-liner to update an {@link ItemStack}'s {@link ItemMeta}.
	 *
	 * If the stack is a map, it's attributes will be hidden.
	 *
	 * @param itemStack The original {@link ItemStack}. This stack will be directly modified.
	 * @param title The stack's title.
	 * @param loreLines A list containing the stack's lines.
	 *
	 * @return The same {@link ItemStack}, but with an updated {@link ItemMeta}.
	 */
	static public ItemStack makeItem(ItemStack itemStack, String title, List<String> loreLines)
	{
		ItemMeta meta = itemStack.getItemMeta();
		meta.setDisplayName(title);
		meta.setLore(loreLines);

		if (itemStack.getType().equals(Material.MAP))
			hideItemAttributes(meta);

		itemStack.setItemMeta(meta);
		return itemStack;
	}

	/**
	 * Implements a bukkit runnable that updates an inventory slot later.
	 */
	static private class CreateDisplayItemTask implements Runnable
	{
		private final Inventory inventory;
		private final ItemStack item;
		private final int slot;

		public CreateDisplayItemTask(Inventory inventory, ItemStack item, int slot)
		{
			this.inventory = inventory;
			this.item = item;
			this.slot = slot;
		}

		@Override
		public void run()
		{
			inventory.setItem(slot, item);
			for (HumanEntity player : inventory.getViewers())
			{
				((Player) player).updateInventory();
			}
		}

	}
}
