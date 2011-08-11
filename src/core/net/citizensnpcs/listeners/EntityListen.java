package net.citizensnpcs.listeners;

import net.citizensnpcs.Citizens;
import net.citizensnpcs.CreatureTask;
import net.citizensnpcs.SettingsManager;
import net.citizensnpcs.events.NPCDisplayTextEvent;
import net.citizensnpcs.events.NPCRightClickEvent;
import net.citizensnpcs.events.NPCTargetEvent;
import net.citizensnpcs.npcs.NPCDataManager;
import net.citizensnpcs.npcs.NPCManager;
import net.citizensnpcs.resources.npclib.HumanNPC;
import net.citizensnpcs.utils.MessageUtils;
import net.citizensnpcs.utils.Messaging;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityListener;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.plugin.PluginManager;

public class EntityListen extends EntityListener implements Listener {

	@Override
	public void registerEvents() {
		PluginManager pm = Bukkit.getServer().getPluginManager();
		pm.registerEvent(Event.Type.ENTITY_DAMAGE, this, Event.Priority.Normal,
				Citizens.plugin);
		pm.registerEvent(Event.Type.ENTITY_TARGET, this, Event.Priority.Normal,
				Citizens.plugin);
		pm.registerEvent(Event.Type.ENTITY_DEATH, this, Event.Priority.Normal,
				Citizens.plugin);
	}

	@Override
	public void onEntityDamage(EntityDamageEvent event) {
		CreatureTask.onDamage(event.getEntity(), event);
		HumanNPC npc = NPCManager.get(event.getEntity());
		if (npc != null && !npc.callDamageEvent(event)) {
			event.setCancelled(true);
		}
		if (event instanceof EntityDamageByEntityEvent) {
			EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
			if (npc != null) {
				if (e.getDamager() instanceof Player) {
					Player player = (Player) e.getDamager();
					npc.callLeftClick(player, npc);
				}
			} else if (e.getDamager() instanceof Player) {
				CreatureTask.onDamage(e.getEntity(), event);
			}
		}
	}

	@Override
	public void onEntityTarget(EntityTargetEvent event) {
		if (!(event instanceof NPCTargetEvent)) {
			return;
		}
		if (CreatureTask.getCreature(event.getEntity()) != null) {
			CreatureTask.getCreature(event.getEntity()).onRightClick(
					(Player) event.getTarget());
		}
		if (NPCManager.isNPC(event.getTarget())) {
			if (!NPCManager.get(event.getTarget()).callTargetEvent(event)) {
				event.setCancelled(true);
			}
		}
		NPCTargetEvent e = (NPCTargetEvent) event;
		HumanNPC npc = NPCManager.get(e.getEntity());
		if (npc != null && event.getTarget() instanceof Player) {
			Player player = (Player) event.getTarget();
			if (Citizens.validateTool("items.basic.select-items", player
					.getItemInHand().getTypeId(), player.isSneaking())) {
				if (!NPCManager.validateSelected(player, npc.getUID())) {
					NPCDataManager.selectNPC(player, npc);
					Messaging.send(player, npc,
							SettingsManager.getString("SelectionMessage"));
					if (!SettingsManager.getBoolean("QuickSelect")) {
						return;
					}
				}
			}
			// Call text-display event
			if (Citizens.validateTool("items.basic.talk-items", player
					.getItemInHand().getTypeId(), player.isSneaking())) {
				Player target = (Player) e.getTarget();
				NPCDisplayTextEvent textEvent = new NPCDisplayTextEvent(npc,
						target, MessageUtils.getText(npc, target));
				Bukkit.getServer().getPluginManager().callEvent(textEvent);
				if (textEvent.isCancelled()) {
				} else if (!textEvent.getNPC().getNPCData().isLookClose()) {
					NPCManager.facePlayer(npc, target);
				} else if (!textEvent.getText().isEmpty()) {
					Messaging.send(target, npc, textEvent.getText());
				}
			}
			NPCRightClickEvent rightClickEvent = new NPCRightClickEvent(npc,
					player);
			Bukkit.getServer().getPluginManager().callEvent(rightClickEvent);
			if (!rightClickEvent.isCancelled()) {
				if (npc.getWaypoints().isStarted()
						&& npc.getWaypoints().current() != null) {
					npc.getWaypoints().scheduleDelay(npc,
							npc.getWaypoints().current().getLocation(),
							SettingsManager.getInt("RightClickPause"));
				}
				npc.callRightClick(player, rightClickEvent.getNPC());
			}
		}
	}

	@Override
	public void onEntityDeath(EntityDeathEvent event) {
		CreatureTask.onEntityDeath(event.getEntity());
		if (NPCManager.isNPC(event.getEntity())) {
			NPCManager.get(event.getEntity()).callDeathEvent(event);
		}
	}
}