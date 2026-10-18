package com.cineverse.core.player

/**
 * Creates a [PlayerController]. Inject it with Hilt; each call returns a new player that the
 * caller owns and must [PlayerController.release].
 */
fun interface PlayerControllerFactory {
    fun create(): PlayerController
}
