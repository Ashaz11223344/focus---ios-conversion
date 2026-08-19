package com.focus.sync

/**
 * Strategy for resolving conflicts when syncing offline changes with remote state.
 */
object ConflictResolver {
    enum class ResolutionStrategy {
        LAST_WRITE_WINS,
        REMOTE_WINS,
        LOCAL_WINS
    }

    /**
     * Resolves a conflict between a local entity and remote entity based on timestamps and strategy.
     * Returns true if local entity should overwrite remote, false if remote wins.
     */
    fun shouldLocalOverwriteRemote(
        localTimestamp: Long,
        remoteTimestamp: Long,
        strategy: ResolutionStrategy = ResolutionStrategy.LAST_WRITE_WINS
    ): Boolean {
        return when (strategy) {
            ResolutionStrategy.LOCAL_WINS -> true
            ResolutionStrategy.REMOTE_WINS -> false
            ResolutionStrategy.LAST_WRITE_WINS -> localTimestamp >= remoteTimestamp
        }
    }
}
