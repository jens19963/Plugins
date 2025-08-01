package com.example.EthanApiPlugin.Collections;

import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.Packets.MousePackets;
import com.example.Packets.WidgetPackets;
import net.runelite.api.Client;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.ItemComposition;
import net.runelite.api.VarPlayer;
import net.runelite.api.WorldType;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.RuneLite;
import net.runelite.client.util.Text;
import net.runelite.client.util.WildcardMatcher;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GrandExchange {

    private static final int F2P_SLOTS = 3;
    private static final int P2P_SLOTS = 8;
    private static final int PRICE_VARIBT = 4398;
    private static final int QUANTITY_VARBIT = 4396;

    private static final Client client = RuneLite.getInjector().getInstance(Client.class);

    public static boolean isFull() {
        boolean isMember = client.getWorldType().contains(WorldType.MEMBERS);
        return getOffers().size() > (isMember ? (P2P_SLOTS - 1) : (F2P_SLOTS - 1));
    }

    // must have a number set to the larger increase to use largeIncrease
    // it's the 2nd +% button
    public static void startBuyOffer(int itemId, int amount, int percentIncrease, boolean largeIncrease) {
        if (!isOpen() || isFull()) {
            return;
        }

        int slotNumber = freeSlot();
        if (slotNumber == -1) {
            return;
        }

        GrandExchangeSlot slot = GrandExchangeSlot.getBySlot(slotNumber);

        if (slot == null) {
            return;
        }

        MousePackets.queueClickPacket();
        WidgetPackets.queueWidgetActionPacket(1, slot.getId(), -1, slot.getBuyChild());
        setItem(itemId);
        setItemQuantity(amount);

        int ticker;
        if (percentIncrease < 0) {
            ticker = largeIncrease ? 56 : 10;
            percentIncrease = percentIncrease * -1;
        } else {
            ticker = largeIncrease ? 57 : 13;
        }
        int finalFive = percentIncrease;

        for (int i = 0; i < finalFive; i++) {
            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetActionPacket(1, InterfaceID.GeOffers.SETUP, -1, ticker);
        }

        Widgets.search().withAction("Confirm").first().ifPresent(widget -> {
            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetActionPacket(1, widget.getId(), -1, -1);
        });
    }

    public static void startBuyOffer(int itemId, int amount, int percentIncrease) {
        startBuyOffer(itemId, amount, percentIncrease, false);
    }

    public static boolean startBuyOfferPrice(int itemId, int amount, int price) {
        int slotNumber = freeSlot();
        if (slotNumber == -1) {
            return false;
        }

        GrandExchangeSlot slot = GrandExchangeSlot.getBySlot(slotNumber);
        if (slot == null) {
            return false;
        }

        MousePackets.queueClickPacket();
        WidgetPackets.queueWidgetActionPacket(1, slot.getId(), -1, slot.getBuyChild());
        setItem(itemId);
        setItemPrice(price);
        setItemQuantity(amount);
        Widgets.search().withAction("Confirm").first().ifPresent(w -> {
            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetActionPacket(1, w.getId(), -1, -1);
        });

        return true;
    }

    public static void startBuyOffer(String itemName, int amount, int fivePercentIncrease) {
        Map.Entry<Integer, ItemComposition> entry = EthanApiPlugin.itemDefs.asMap()
                .entrySet()
                .stream()
                .filter(e -> WildcardMatcher.matches(itemName.toLowerCase(), Text.removeTags(e.getValue().getName().toLowerCase())))
                .findFirst().orElse(null);

        if (entry == null) {
            return;
        }

        startBuyOffer(entry.getValue().getId(), amount, fivePercentIncrease, false);
    }

    // must have the large percent setup to use largeDecrease
    // it's the 2nd -% button
    public static void startSellOffer(Widget widget, int percentChange, boolean largeDecrease) {
        if (!isOpen() || isFull()) {
            return;
        }


        int slotNumber = freeSlot();
        if (slotNumber == -1) {
            return;
        }

        GrandExchangeSlot slot = GrandExchangeSlot.getBySlot(slotNumber);

        if (slot == null) {
            return;
        }


        int itemId = widget.getItemId();

        MousePackets.queueClickPacket();
        WidgetPackets.queueWidgetActionPacket(1, slot.getId(), -1, slot.getSellChild());
        MousePackets.queueClickPacket();
        WidgetPackets.queueWidgetActionPacket(1, InterfaceID.GeOffersSide.ITEMS, itemId, widget.getIndex());

        int ticker;
        if (percentChange < 0) {
            ticker = largeDecrease ? 56 : 10;
            percentChange = percentChange * -1;
        } else {
            ticker = largeDecrease ? 57 : 13;
        }
        int finalFive = percentChange;

        for (int i = 0; i < finalFive; i++) {
            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetActionPacket(1, InterfaceID.GeOffers.SETUP, -1, ticker);
        }

        Widgets.search().withAction("Confirm").first().ifPresent(w -> {
            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetActionPacket(1, w.getId(), -1, -1);
        });
    }

    public static void startSellOffer(Widget widget, int fivePercentDecrease) {
        startSellOffer(widget, fivePercentDecrease, false);
    }

    public static boolean isOpen() {
        return getPage() != Page.CLOSED && getPage() != Page.UNKNOWN;
    }

    public static boolean isOfferOpen() {
        return getPage() == Page.BUYING || getPage() == Page.SELLING;
    }

    public static List<GrandExchangeOffer> getOffers() {
        return Arrays.stream(client.getGrandExchangeOffers())
                .filter(offer -> offer.getItemId() > 0)
                .collect(Collectors.toList());
    }

    public static boolean isEmpty() {
        return getOffers().isEmpty();
    }

    public static void collectAll() {
        if (!readyToCollect()) {
            return;
        }

        MousePackets.queueClickPacket();
        WidgetPackets.queueWidgetActionPacket(1, InterfaceID.GeOffers.COLLECTALL, -1, 0);
    }

    public static boolean readyToCollect() {
        return !Widgets.search().hiddenState(false).withText("Collect").empty();
    }

    public static boolean hasItem(int itemId, int amount) {
        for (GrandExchangeOffer offer : getOffers()) {
            if (offer.getItemId() == itemId
                    && offer.getTotalQuantity() >= amount) {
                return true;
            }
        }

        return false;
    }

    public static boolean hasItem(int itemId) {
        return hasItem(itemId, 1);
    }

    public static boolean hasItem(String itemName, int amount) {
        Map.Entry<Integer, ItemComposition> entry = EthanApiPlugin.itemDefs.asMap()
                .entrySet()
                .stream()
                .filter(e -> WildcardMatcher.matches(itemName.toLowerCase(), Text.removeTags(e.getValue().getName().toLowerCase())))
                .filter(e -> e.getValue().getNote() != 799)
                .findFirst().orElse(null);

        if (entry == null || entry.getValue() == null) {
            return false;
        }

        return hasItem(entry.getValue().getId(), amount);
    }

    public static boolean hasItem(String itemName) {
        return hasItem(itemName, 1);
    }


    private static boolean isSellOpen() {
        return getPage() == Page.SELLING;
    }

    private static boolean isBuyOpen() {
        return getPage() == Page.BUYING;
    }

    private static int getItemId() {
        return client.getVarpValue(VarPlayer.CURRENT_GE_ITEM);
    }

    private static void setItem(int id) {
        MousePackets.queueClickPacket();
        client.runScript(754, id, 84);
    }

    private static int getItemPrice() {
        return client.getVarbitValue(PRICE_VARIBT);
    }

    private static int getItemQuantity() {
        return client.getVarbitValue(QUANTITY_VARBIT);
    }

    private static void setItemPrice(int price) {
        Widget offerWidget = client.getWidget(InterfaceID.GeOffers.SETUP);
        if (offerWidget != null && offerWidget.getChild(12) != null) {
            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetAction(offerWidget.getChild(12), "Enter price");
            WidgetPackets.queueResumeCount(price);
            return;
        }

        MousePackets.queueClickPacket();
        WidgetPackets.queueWidgetActionPacket(1, InterfaceID.GeOffers.SETUP, -1, 12);
    }

    private static void setItemQuantity(int quantity) {
        Widget offerWidget = client.getWidget(InterfaceID.GeOffers.SETUP);
        if (offerWidget != null && offerWidget.getChild(7) != null) {
            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetAction(offerWidget.getChild(7), "Enter quantity");
            WidgetPackets.queueResumeCount(quantity);
            return;
        }

        MousePackets.queueClickPacket();
        WidgetPackets.queueWidgetActionPacket(1, InterfaceID.GeOffers.SETUP, -1, 7);
    }

    private static Page getPage() {
        Widget offerContainer = client.getWidget(InterfaceID.GeOffers.SETUP);

        if (offerContainer != null && !offerContainer.isHidden()) {
            String text = offerContainer.getChild(20).getText();
            if (text == null || text.isEmpty()) {
                return Page.UNKNOWN;
            }

            if (text.equalsIgnoreCase("sell offer")) {
                return Page.SELLING;
            }

            if (text.equalsIgnoreCase("buy offer")) {
                return Page.BUYING;
            }

            return Page.UNKNOWN;
        }

        Widget homeContainer = client.getWidget(InterfaceID.GeOffers.CONTENTS);

        if (homeContainer != null && !homeContainer.isHidden()) {
            return Page.HOME;
        }

        return Page.CLOSED;
    }

    private static int freeSlot() {
        GrandExchangeOffer[] offers = client.getGrandExchangeOffers();
        for (int slot = 0; slot < 8; slot++) {
            if (offers[slot] == null || offers[slot].getState() == GrandExchangeOfferState.EMPTY) {
                return slot + 1;
            }
        }

        return -1;
    }

    private static boolean isNoted(int id) {
        return client.getItemDefinition(id).getNote() == 799;
    }

    private static int getNotedId(int id) {
        return client.getItemDefinition(id).getLinkedNoteId();
    }

    public enum Page {
        UNKNOWN,
        HOME,
        CLOSED,
        BUYING,
        SELLING
    }
}
