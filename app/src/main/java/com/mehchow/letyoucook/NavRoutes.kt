package com.mehchow.letyoucook

object NavRoutes {
    const val SPLASH = "splash"
    const val AUTH = "auth"
    const val HOME = "home"
    const val HOME_WITH_USERNAME = "home/{username}"

    fun homeRoute(username: String): String = "$HOME/$username"
}