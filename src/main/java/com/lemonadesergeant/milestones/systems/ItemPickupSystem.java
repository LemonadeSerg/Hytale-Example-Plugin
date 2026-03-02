package com.lemonadesergeant.milestones.systems;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.lemonadesergeant.milestones.data.GameEventType;
import com.lemonadesergeant.milestones.data.NormalizedGameEvent;
import com.lemonadesergeant.milestones.logging.LogSource;
import com.lemonadesergeant.milestones.logging.LogStage;
import com.lemonadesergeant.milestones.logging.PluginLog;
import com.lemonadesergeant.milestones.managers.EventForwardingManager;

public class ItemPickupSystem extends EntityTickingSystem<EntityStore> {

	private static final String PICKUP_TRACKED_META_KEY = "MilestonesPickupTracked";
	private static final int[] TRACKED_SECTION_IDS = {
		Inventory.HOTBAR_SECTION_ID,
		Inventory.STORAGE_SECTION_ID,
		Inventory.ARMOR_SECTION_ID,
		Inventory.UTILITY_SECTION_ID,
		Inventory.BACKPACK_SECTION_ID,
		Inventory.TOOLS_SECTION_ID
	};

	private final Map<String, Map<String, Integer>> previousQuantitiesByPlayer = new HashMap<>();

	public ItemPickupSystem() {
		PluginLog.info(LogSource.ITEM_PICKUP, LogStage.SETUP, "system=ItemPickupSystem action=init mode=inventory-diff");
	}

	@Override
	public Query<EntityStore> getQuery() {
		return Query.any();
	}

	@Override
	public void tick(
			float dt,
			int entityIndex,
			@Nonnull ArchetypeChunk<EntityStore> chunk,
			@Nonnull Store<EntityStore> store,
			@Nonnull CommandBuffer<EntityStore> commandBuffer
	) {
		SystemPlayerContextResolver.PlayerContext playerContext = SystemPlayerContextResolver.resolve(chunk, entityIndex);
		Player player = playerContext.player;
		Ref<EntityStore> playerRef = playerContext.playerRef;
		if (player == null || player.getInventory() == null) {
			return;
		}

		Inventory inventory = player.getInventory();
		String playerKey = resolvePlayerKey(player, entityIndex);

		Map<String, Integer> previousSnapshot = previousQuantitiesByPlayer.getOrDefault(playerKey, Map.of());
		Map<String, Integer> currentSnapshot = new HashMap<>();

		for (int sectionId : TRACKED_SECTION_IDS) {
			ItemContainer section = inventory.getSectionById(sectionId);
			if (section == null) {
				continue;
			}

			short capacity = section.getCapacity();
			for (short slot = 0; slot < capacity; slot++) {
				ItemStack itemStack = section.getItemStack(slot);
				if (ItemStack.isEmpty(itemStack)) {
					continue;
				}

				String itemId = itemStack.getItemId();
				String slotKey = toSlotKey(sectionId, slot, itemId);
				int currentQuantity = itemStack.getQuantity();
				currentSnapshot.put(slotKey, currentQuantity);

				int previousQuantity = previousSnapshot.getOrDefault(slotKey, 0);
				if (currentQuantity <= previousQuantity) {
					continue;
				}

				int gainedQuantity = currentQuantity - previousQuantity;
				if (isPickupAlreadyTracked(itemStack)) {
					continue;
				}

				publishPickup(store, playerRef, player, itemId, gainedQuantity);

				ItemStack taggedStack = itemStack.withMetadata(PICKUP_TRACKED_META_KEY, Codec.BOOLEAN, true);
				section.setItemStackForSlot(slot, taggedStack);
			}
		}

		previousQuantitiesByPlayer.put(playerKey, currentSnapshot);
	}

	@Override
	public boolean isParallel(int archetypeChunkSize, int taskCount) {
		return false;
	}

	private void publishPickup(Store<EntityStore> store, Ref<EntityStore> playerRef, Player player, String itemId, int gainedQuantity) {
	        if (itemId != null && itemId.startsWith("EditorTool_")) {
	            return;
	        }

	        String playerId = SystemEntityResolver.resolvePlayerUuid(player);

		PluginLog.info(
			LogSource.ITEM_PICKUP,
			LogStage.HANDLE,
			"playerId=%s itemId=%s amount=%s",
				playerId,
			itemId,
			gainedQuantity
		);

	        EventForwardingManager.forward(
				store,
				playerRef,
	            NormalizedGameEvent.of(GameEventType.ITEM_PICKUP)
	                .put("playerId", playerId)
	                .put("itemId", itemId)
	                .put("amount", gainedQuantity)
	        );
	}

	private boolean isPickupAlreadyTracked(ItemStack itemStack) {
		if (itemStack == null) {
			return false;
		}

		Boolean tracked = itemStack.getFromMetadataOrNull(PICKUP_TRACKED_META_KEY, Codec.BOOLEAN);
		return tracked != null && tracked;
	}

	private String resolvePlayerKey(Player player, int entityIndex) {
		String uuid = SystemEntityResolver.resolvePlayerUuid(player);
		return uuid == null || uuid.isBlank() ? "entity:" + entityIndex : uuid;
	}

	private String toSlotKey(int sectionId, short slot, String itemId) {
		return sectionId + ":" + slot + ":" + itemId;
	}
}
