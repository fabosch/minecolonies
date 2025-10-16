package com.minecolonies.core.client.gui.citizen;

import com.google.common.collect.ImmutableList;
import com.minecolonies.api.colony.ICitizenDataView;
import com.minecolonies.api.colony.buildings.views.IBuildingView;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.request.RequestState;
import com.minecolonies.api.colony.requestsystem.requestable.IDeliverable;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.MessageUtils;
import com.minecolonies.api.util.MessageUtils.MessagePriority;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.Network;
import com.minecolonies.core.network.messages.server.colony.UpdateRequestStateMessage;
import com.minecolonies.core.network.messages.server.colony.citizen.TransferItemsToCitizenRequestMessage;
import com.minecolonies.core.client.gui.requesttree.CitizenRequestRequestTreeHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import static com.minecolonies.api.util.constant.TranslationConstants.COM_MINECOLONIES_CANT_TAKE_EQUIPPED;
import static com.minecolonies.api.util.constant.WindowConstants.CITIZEN_REQ_RESOURCE_SUFFIX;

/**
 * BOWindow for the citizen.
 */
public class RequestWindowCitizen extends AbstractWindowCitizen
{
    /**
     * The citizenData.View object.
     */
    private final ICitizenDataView citizen;

    /**
     * Inventory of the player.
     */
    private final Inventory inventory = this.mc.player.getInventory();

    /**
     * Is the player in creative or not.
     */
    private final boolean isCreative = this.mc.player.isCreative();

    private CitizenRequestRequestTreeHandler requestTreeHandler;

    /**
     * Constructor to initiate the citizen windows.
     *
     * @param citizen citizen to bind the window to.
     */
    public RequestWindowCitizen(final ICitizenDataView citizen)
    {
        super(citizen, Constants.MOD_ID + CITIZEN_REQ_RESOURCE_SUFFIX);
        this.citizen = citizen;

        this.requestTreeHandler = new CitizenRequestRequestTreeHandler(
          citizen.getWorkBuilding(),
          IColonyManager.getInstance().getColonyView(citizen.getColonyId(), Minecraft.getInstance().level.dimension()),
          this);
    }

    public ICitizenDataView getCitizen()
    {
        return citizen;
    }

    // callled by WindowRequestDetail
    public boolean fulfillable(final IRequest<?> tRequest)
    {
        return this.requestTreeHandler.fulfillable(tRequest);
    }

    // callled by WindowRequestDetail
    public boolean cancellable(final IRequest<?> tRequest)
    {
        return this.requestTreeHandler.cancellable(tRequest);
    }

    // callled by WindowRequestDetail
    public void fulfill(@NotNull final IRequest<?> request)
    {
        this.requestTreeHandler.fulfill(request);
    }

    // callled by WindowRequestDetail
    public void cancel(@NotNull final IRequest<?> request)
    {
        this.requestTreeHandler.cancel(request);
    }
}
