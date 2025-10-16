package com.minecolonies.core.client.gui.requesttree;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.ldtteam.blockui.Color;
import com.ldtteam.blockui.controls.Button;
import com.ldtteam.blockui.views.ZoomDragView;
import com.minecolonies.api.colony.ICitizenDataView;
import com.minecolonies.api.colony.IColonyView;
import com.minecolonies.api.colony.buildings.views.IBuildingView;
import com.minecolonies.api.colony.requestsystem.manager.IRequestManager;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.request.RequestState;
import com.minecolonies.api.colony.requestsystem.requestable.MinimumStack;
import com.minecolonies.api.colony.requestsystem.resolver.player.IPlayerRequestResolver;
import com.minecolonies.api.colony.requestsystem.resolver.retrying.IRetryingRequestResolver;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.api.util.Log;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.Network;
import com.minecolonies.core.client.gui.AbstractWindowSkeleton;
import com.minecolonies.core.items.ItemClipboard;
import com.minecolonies.core.network.messages.server.ItemSettingMessage;
import com.minecolonies.core.network.messages.server.colony.UpdateRequestStateMessage;
import com.minecolonies.core.client.gui.requesttree.ClipBoardRequestTreeHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.minecolonies.api.util.constant.WindowConstants.CLIPBOARD_TOGGLE;

public class ClipBoardRequestTreeHandler extends DefaultRequestTreeHandler {
    /**
     * Hide or show not important requests.
     */
    private boolean hide = false;

    /**
     * List of async request tokens.
     */
    private final List<IToken<?>> asyncRequest = new ArrayList<>();

    public ClipBoardRequestTreeHandler(final BlockPos building, final IColonyView colony, final AbstractWindowSkeleton attachedWindow, final boolean hideValue)
    {
        super(building, colony, attachedWindow);
        this.hide = hideValue;

        for (final ICitizenDataView view : this.colony.getCitizens().values())
        {
            if (view.getJobView() != null)
            {
                asyncRequest.addAll(view.getJobView().getAsyncRequests());
            }
        }
    }

    public boolean getHideValue()
    {
        return hide;
    }

    public void setHideValue(final boolean hide)
    {
        this.hide = hide;
    }
    
    @Override
    public ImmutableList<IRequest<?>> getOpenRequestsFromBuilding(final IBuildingView building)
    {
        final ArrayList<IRequest<?>> requests = Lists.newArrayList();

        if (colony == null)
        {
            return ImmutableList.of();
        }

        final IRequestManager requestManager = colony.getRequestManager();

        if (requestManager == null)
        {
            return ImmutableList.of();
        }

        try
        {
            final IPlayerRequestResolver resolver = requestManager.getPlayerResolver();
            final IRetryingRequestResolver retryingRequestResolver = requestManager.getRetryingRequestResolver();

            final Set<IToken<?>> requestTokens = new HashSet<>();
            requestTokens.addAll(resolver.getAllAssignedRequests());
            requestTokens.addAll(retryingRequestResolver.getAllAssignedRequests());

            for (final IToken<?> token : requestTokens)
            {
                IRequest<?> request = requestManager.getRequestForToken(token);

                if (this.getHideValue() && request.getType().equals(TypeToken.of(MinimumStack.class)))
                {
                    continue;
                }

                while (request != null && request.hasParent())
                {
                    request = requestManager.getRequestForToken(request.getParent());
                }

                if (request != null && !requests.contains(request))
                {
                    requests.add(request);
                }
            }

            if (getHideValue())
            {
                requests.removeIf(req -> asyncRequest.contains(req.getId()));
            }

            final BlockPos playerPos = Minecraft.getInstance().player.blockPosition();
            requests.sort(Comparator.comparing((IRequest<?> request) -> request.getRequester().getLocation().getInDimensionLocation()
                    .distSqr(new Vec3i(playerPos.getX(), playerPos.getY(), playerPos.getZ())))
                .thenComparingInt((IRequest<?> request) -> request.getId().hashCode()));
        }
        catch (Exception e)
        {
            Log.getLogger().warn("Exception trying to retreive requests:", e);
            requestManager.reset();
            return ImmutableList.of();
        }

        return ImmutableList.copyOf(requests);
    }

    @Override
    public boolean fulfillable(final IRequest<?> tRequest)
    {
        return false;
    }

    @Override
    public void cancel(@NotNull final IRequest<?> request)
    {
        Network.getNetwork().sendToServer(new UpdateRequestStateMessage(colony, request.getId(), RequestState.CANCELLED, null));
    }
}
