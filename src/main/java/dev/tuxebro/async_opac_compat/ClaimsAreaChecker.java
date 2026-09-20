package dev.tuxebro.async_opac_compat;

import it.unimi.dsi.fastutil.longs.Long2BooleanLinkedOpenHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import xaero.pac.common.claims.player.IPlayerChunkClaim;
import xaero.pac.common.claims.player.IPlayerClaimPosList;
import xaero.pac.common.claims.player.IPlayerDimensionClaims;
import xaero.pac.common.parties.party.IPartyPlayerInfo;
import xaero.pac.common.parties.party.ally.IPartyAlly;
import xaero.pac.common.parties.party.member.IPartyMember;
import xaero.pac.common.server.IOpenPACMinecraftServer;
import xaero.pac.common.server.IServerData;
import xaero.pac.common.server.claims.IServerClaimsManager;
import xaero.pac.common.server.claims.IServerDimensionClaimsManager;
import xaero.pac.common.server.claims.IServerRegionClaims;
import xaero.pac.common.server.claims.player.IServerPlayerClaimInfo;
import xaero.pac.common.server.parties.party.IServerParty;

import javax.annotation.Nullable;
import java.util.HashMap;

//im an insane premature optimizer omg
@EventBusSubscriber
public class ClaimsAreaChecker {
    private static final int TPS = 20;
    private static final int MAX_CLAIM_POS_CACHE_CAPACITY = 1_000;
    private static final int MAX_CLEAR_COUNTER_TICK = 30 * TPS;

    private static final HashMap<ResourceKey<Level>, Long2BooleanLinkedOpenHashMap> cache = new HashMap<>();
    private static int cacheClearingCounter = 0;

    public static boolean checkIfEntityIsIn(Level level, Entity entity) {
        if (entity == null) return false;
        if (level.isClientSide) return false;

        ResourceKey<Level> dimensionRes = level.dimension();
        MinecraftServer server = entity.getServer();
        if (server == null) return false;
        return checkIfEntityIsIn(server, dimensionRes, entity);
    }

    public static boolean checkIfEntityIsIn(MinecraftServer server, ResourceKey<Level> dimensionKey, Entity entity) {
        if (!cache.containsKey(dimensionKey))
            cache.put(dimensionKey, new Long2BooleanLinkedOpenHashMap(MAX_CLAIM_POS_CACHE_CAPACITY));
        Long2BooleanLinkedOpenHashMap claimPosCache = cache.get(dimensionKey);

        if (cacheClearingCounter > 20)
            cacheClearingCounter -= 20;

        ChunkPos chunkPos = entity.chunkPosition();
        ResourceLocation dimensionLoc = dimensionKey.location();
        long chunkPosPacked = chunkPos.toLong();
        boolean claimExists;

        boolean isClaimChecked = claimPosCache.containsKey(chunkPosPacked);
        if (isClaimChecked) {
            claimExists = claimPosCache.get(chunkPosPacked);
            return claimExists;
        }

        claimExists = checkClaim(server, dimensionLoc, chunkPos);
        claimPosCache.put(chunkPosPacked, claimExists);

        if (claimPosCache.size() - 10 >= MAX_CLAIM_POS_CACHE_CAPACITY)
            claimPosCache.removeFirstBoolean();

        return claimExists;
    }

    private synchronized static boolean checkClaim(MinecraftServer server, ResourceLocation dimensionLoc, ChunkPos chunkPos) {
        var claimsManager = getClaimsManager(server);
        if (claimsManager == null) return true;

        return claimsManager.get(dimensionLoc, chunkPos) != null;
    }

    // long ass data type holy mucho texto xaero
    @Nullable
    private synchronized static IServerClaimsManager<IPlayerChunkClaim, IServerPlayerClaimInfo<IPlayerDimensionClaims<IPlayerClaimPosList>>, IServerDimensionClaimsManager<IServerRegionClaims>> getClaimsManager(MinecraftServer server) {
        var serverOPAC = (IOpenPACMinecraftServer) server;

        var serverData = (IServerData<IServerClaimsManager<IPlayerChunkClaim,IServerPlayerClaimInfo<IPlayerDimensionClaims<IPlayerClaimPosList>>, IServerDimensionClaimsManager<IServerRegionClaims>>, IServerParty<IPartyMember, IPartyPlayerInfo, IPartyAlly>>)
                (serverOPAC).getXaero_OPAC_ServerData();
        if (serverData == null) return null;

        return serverData.getServerClaimsManager();
    }

    @SubscribeEvent
    private static void onTick(ServerTickEvent.Post event) {
        cacheClearingCounter++;
        if (cacheClearingCounter < MAX_CLEAR_COUNTER_TICK) return;

        cacheClearingCounter = 0;
        if (cache.isEmpty()) return;
        cache.forEach((dimension, claimPosCache) -> claimPosCache.clear());
    }
}
