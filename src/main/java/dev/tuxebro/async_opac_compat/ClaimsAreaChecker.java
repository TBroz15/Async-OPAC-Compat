package dev.tuxebro.async_opac_compat;

import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
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
public class ClaimsAreaChecker {
    private static final int MAX_CLAIM_POS_CACHE_CAPACITY = 5_000;
    private static final HashMap<ResourceKey<Level>, LongLinkedOpenHashSet> cache = new HashMap<>();

    public static boolean checkIfEntityNearClaim(Level level, Entity entity) {
        if (entity == null) return false;
        if (level.isClientSide) return false;

        ResourceKey<Level> dimensionRes = level.dimension();
        MinecraftServer server = entity.getServer();
        if (server == null) return false;
        return checkIfEntityNearClaim(server, dimensionRes, entity);
    }

    public static boolean checkIfEntityNearClaim(MinecraftServer server, ResourceKey<Level> dimensionKey, Entity entity) {
        if (!cache.containsKey(dimensionKey))
            cache.put(dimensionKey, new LongLinkedOpenHashSet(MAX_CLAIM_POS_CACHE_CAPACITY));
        LongLinkedOpenHashSet claimPosCache = cache.get(dimensionKey);

        ChunkPos chunkPos = entity.chunkPosition();
        ResourceLocation dimensionLoc = dimensionKey.location();

        boolean isInClaim = ChunkPosChecker.checkNear3x3Area(chunkPos, (x, z) -> {
            long chunkLong = ChunkPos.asLong(x, z);
            return claimPosCache.contains(chunkLong);
        });
        if (isInClaim) return true;

        isInClaim = checkClaim(server, dimensionLoc, chunkPos);
        if (!isInClaim) return false;

        claimPosCache.add(chunkPos.toLong());
        if (claimPosCache.size() - 10 >= MAX_CLAIM_POS_CACHE_CAPACITY)
            claimPosCache.removeFirstLong();

        return true;
    }

    private synchronized static boolean checkClaim(MinecraftServer server, ResourceLocation dimensionLoc, ChunkPos chunkPos) {
        var claimsManager = getClaimsManager(server);
        if (claimsManager == null) return true;

        return ChunkPosChecker.checkNear3x3Area(chunkPos,
                (x, z) -> claimsManager.get(dimensionLoc, new ChunkPos(x, z)) != null);
    }

    private static class ChunkPosChecker {
        @FunctionalInterface
        public interface Predicate {
            boolean matches(int x, int z);
        }

        private static boolean checkNear3x3Area(ChunkPos chunkPos, ChunkPosChecker.Predicate predicate) {
            // fast check instead of starting to traverse immediately
            if (predicate.matches(chunkPos.x, chunkPos.z)) return true;

            final int minX = chunkPos.x - 1;
            final int minZ = chunkPos.z - 1;

            final int maxX = chunkPos.x + 1;
            final int maxZ = chunkPos.z + 1;

            int x = minX;
            int z = minZ;

            while (true) {
                if (z > maxZ) return false;
                if (predicate.matches(x, z)) return true;

                if (x >= maxX) {
                    x = minX;
                    z++;
                } else x++;
            }
        }
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
}
