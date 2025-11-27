package portablemechest.items.content;

import java.util.Comparator;
import java.util.List;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;

/**
 * Aggregates multiple cell handlers, respecting priority and slot order.
 */
public class MultiCellInventoryHandler implements IMEInventoryHandler<IAEItemStack> {

    public static class HandlerEntry {

        final IMEInventoryHandler<IAEItemStack> handler;
        final int slot;
        final int priority;

        public HandlerEntry(IMEInventoryHandler<IAEItemStack> handler, int slot) {
            this.handler = handler;
            this.slot = slot;
            this.priority = handler.getPriority();
        }
    }

    private final List<HandlerEntry> handlers;

    public MultiCellInventoryHandler(List<HandlerEntry> handlers) {
        handlers.sort(
            Comparator.comparingInt((HandlerEntry e) -> e.priority)
                .reversed()
                .thenComparingInt(e -> e.slot));
        this.handlers = handlers;
    }

    @Override
    public StorageChannel getChannel() {
        return StorageChannel.ITEMS;
    }

    @Override
    public AccessRestriction getAccess() {
        return AccessRestriction.READ_WRITE;
    }

    @Override
    public IAEItemStack injectItems(IAEItemStack input, Actionable mode, BaseActionSource src) {
        IAEItemStack remaining = input;
        for (HandlerEntry entry : handlers) {
            if (remaining == null || remaining.getStackSize() == 0) {
                return null;
            }
            remaining = entry.handler.injectItems(remaining, mode, src);
        }
        return remaining;
    }

    @Override
    public IAEItemStack extractItems(IAEItemStack request, Actionable mode, BaseActionSource src) {
        for (HandlerEntry entry : handlers) {
            IAEItemStack out = entry.handler.extractItems(request, mode, src);
            if (out != null) {
                return out;
            }
        }
        return null;
    }

    @Override
    public IItemList<IAEItemStack> getAvailableItems(IItemList<IAEItemStack> out) {
        for (HandlerEntry entry : handlers) {
            entry.handler.getAvailableItems(out);
        }
        return out;
    }

    @Override
    public boolean canAccept(IAEItemStack input) {
        for (HandlerEntry entry : handlers) {
            if (entry.handler.canAccept(input)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getPriority() {
        return handlers.isEmpty() ? 0 : handlers.get(0).priority;
    }

    @Override
    public int getSlot() {
        return handlers.isEmpty() ? 0 : handlers.get(0).slot;
    }

    @Override
    public boolean validForPass(int i) {
        for (HandlerEntry entry : handlers) {
            if (entry.handler.validForPass(i)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isPrioritized(IAEItemStack input) {
        for (HandlerEntry entry : handlers) {
            if (entry.handler.isPrioritized(input)) {
                return true;
            }
        }
        return false;
    }
}
