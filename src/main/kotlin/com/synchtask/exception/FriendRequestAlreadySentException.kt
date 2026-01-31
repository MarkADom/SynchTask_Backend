package com.synchtask.exception

class FriendRequestAlreadySentException(
    message: String = "Friend request already sent",
) : RuntimeException(message)
