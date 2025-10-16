public class CitizenRequestRequestTreeHandler extends DefaultRequestTreeHandler {

    public CitizenRequestRequestTreeHandler(final BlockPos building, final IColonyView colony, final RequestWindowCitizen attachedWindow)
    {
        super(building, colony, attachedWindow);
    }

    @Override
    public boolean canFulFill()
    {
        return true;
    }

    @Override
    public ImmutableList<IRequest<?>> getOpenRequestsFromBuilding(final IBuildingView building)
    {
        Citizen citizen = ((RequestWindowCitizen) this.attachedWindow).getCitizen();
        
        if (building == null)
        {
            return ImmutableList.of();
        }

        final List<IRequest<?>> requests = new ArrayList<>();
        for (final IToken<?> req : building.getOpenRequestsByCitizen().getOrDefault(citizen.getId(), Collections.emptyList()))
        {
            if (req != null)
            {
                final IRequest<?> request = colony.getRequestManager().getRequestForToken(req);
                if (request != null)
                {
                    requests.add(request);
                }
            }
        }

        for (final IToken<?> req : building.getOpenRequestsByCitizen().getOrDefault(-1, Collections.emptyList()))
        {
            if (req != null)
            {
                final IRequest<?> request = colony.getRequestManager().getRequestForToken(req);
                if (request != null)
                {
                    requests.add(request);
                }
            }
        }

        return ImmutableList.copyOf(requests);
    }

    @Override
    public void fulfill(@NotNull final IRequest<?> tRequest)
    {
        Citizen citizen = ((RequestWindowCitizen) this.attachedWindow).getCitizen();

        if (!(tRequest.getRequest() instanceof IDeliverable))
        {
            return;
        }

        @NotNull final IRequest<? extends IDeliverable> request = (IRequest<? extends IDeliverable>) tRequest;

        final Predicate<ItemStack> requestPredicate = stack -> request.getRequest().matches(stack);
        final int amount = request.getRequest().getCount();

        final int count = InventoryUtils.getItemCountInItemHandler(new InvWrapper(inventory), requestPredicate);

        if (!isCreative && count <= 0)
        {
            return;
        }

        // The itemStack size should not be greater than itemStack.getMaxStackSize, We send 1 instead
        // and use quantity for the size
        @NotNull final ItemStack itemStack;
        if (isCreative)
        {
            itemStack = request.getDisplayStacks().stream().findFirst().orElse(ItemStack.EMPTY);
        }
        else
        {
            final List<Integer> slots = InventoryUtils.findAllSlotsInItemHandlerWith(new InvWrapper(inventory), requestPredicate);
            final int invSize = inventory.getContainerSize() - 5; // 4 armour slots + 1 shield slot
            int slot = -1;
            for (final Integer possibleSlot : slots)
            {
                if (possibleSlot < invSize)
                {
                    slot = possibleSlot;
                    break;
                }
            }

            if (slot == -1)
            {
                MessageUtils.format("<%s> ")
                  .append(COM_MINECOLONIES_CANT_TAKE_EQUIPPED, citizen.getName())
                  .withPriority(MessagePriority.IMPORTANT)
                  .sendTo(Minecraft.getInstance().player);

                return; // We don't have one that isn't in our armour slot
            }
            itemStack = inventory.getItem(slot);
        }


        if (citizen.getWorkBuilding() != null)
        {
            colony.getBuilding(citizen.getWorkBuilding()).onRequestedRequestComplete(colony.getRequestManager(), tRequest);
        }
        Network.getNetwork().sendToServer(
          new TransferItemsToCitizenRequestMessage(colony, citizen, itemStack, isCreative ? amount : Math.min(amount, count)));

        final ItemStack copy = itemStack.copy();
        copy.setCount(isCreative ? amount : Math.min(amount, count));
        Network.getNetwork().sendToServer(new UpdateRequestStateMessage(colony, request.getId(), RequestState.OVERRULED, copy));
    }
}
