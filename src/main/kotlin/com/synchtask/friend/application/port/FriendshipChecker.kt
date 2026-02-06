package com.synchtask.friend.application.port

interface FriendshipChecker {
    fun areFriends(userId: Long, otherUserId: Long): Boolean
}
