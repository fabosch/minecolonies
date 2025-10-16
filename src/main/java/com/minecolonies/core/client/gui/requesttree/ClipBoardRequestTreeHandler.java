public class ClipBoardRequestTreeHandler extends DefaultRequestTreeHandler {
    /**
     * Hide or show not important requests.
     */
    private boolean hide = false;

    public ClipBoardRequestTreeHandler(final BlockPos building, final IColonyView colony, final AbstractWindowSkeleton attachedWindow, final boolean hideValue)
    {
        super(building, colony, attachedWindow);
        this.hide = hideValue;
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
    protected void cancel(@NotNull final IRequest<?> request)
    {
        Network.getNetwork().sendToServer(new UpdateRequestStateMessage(colony, request.getId(), RequestState.CANCELLED, null));
    }
}
