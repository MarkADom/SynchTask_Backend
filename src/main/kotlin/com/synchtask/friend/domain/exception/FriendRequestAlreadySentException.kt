package com.synchtask.friend.domain.exception

class FriendRequestAlreadySentException(
    message: String = "Friend request already sent",
) : RuntimeException(message)
