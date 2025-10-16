package com.minecolonies.core.client.gui;

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

/**
 * ClipBoard window.
 */
public class WindowClipBoard extends AbstractWindowSkeleton
{
    /**
     * Resource suffix.
     */
    private static final String BUILD_TOOL_RESOURCE_SUFFIX = ":gui/windowclipboard.xml";



    /**
     * The colony id.
     */
    private final IColonyView colony;

    /**
     * The request tree handler containing the logic for the request tree for the clipboard.
     */
    private ClipBoardRequestTreeHandler requestTreeHandler;

    /**
     * Constructor of the clipboard GUI.
     *
     * @param colony the colony to check the requests for.
     */
    public WindowClipBoard(final IColonyView colony, boolean hidestate)
    {
        super(Constants.MOD_ID + BUILD_TOOL_RESOURCE_SUFFIX);
        this.colony = colony;

        this.requestTreeHandler = new ClipBoardRequestTreeHandler(null, colony, this, hidestate);
        
        registerButton(CLIPBOARD_TOGGLE, this::toggleImportant);
        paintButtonState();
    }
    
    @Override
    public void onUpdate()
    {
        super.onUpdate();

        this.requestTreeHandler.onWindowUpdate();
    }

    @Override
    public void onOpened()
    {
        super.onOpened();

        this.requestTreeHandler.onWindowOpened();
    }

    /**
     * Toggles the visibility of non-important requests and sends a message to
     * the server to save that setting on the clipboard item.
     *
     * @see ItemSettingMessage
     */
    private void toggleImportant()
    {
        this.requestTreeHandler.setHideValue(!this.requestTreeHandler.getHideValue());

        paintButtonState();

        ItemSettingMessage hideSetting = new ItemSettingMessage();
        hideSetting.setSetting(ItemClipboard.TAG_HIDEUNIMPORTANT, this.requestTreeHandler.getHideValue() ? 1 : 0);
        Network.getNetwork().sendToServer(hideSetting);
    }


    /**
     * Paints the button state of the important toggle.
     *
     * This function finds the important toggle button and sets its colors based on the state of hide.
     * If hide is true, the button is set to green. Otherwise, it is set to red.
     */
    private void paintButtonState()
    {
        final Button importantToggle = findPaneOfTypeByID("important", Button.class);

        if (this.requestTreeHandler.getHideValue())
        {
            importantToggle.setColors(Color.getByName("green", 0));
        }
        else
        {
            importantToggle.setColors(Color.getByName("red", 0));
        }
    }
}
