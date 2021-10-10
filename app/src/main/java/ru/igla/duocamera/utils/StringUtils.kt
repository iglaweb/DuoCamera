package ru.igla.duocamera.utils

/**
 * Created by lashkov on 06.09.21.
 * Copyright (c) 2021 igla LLC. All rights reserved.
 */
object StringUtils {
    /***
     * Check string is null or empty
     * @param cs string to check
     * @return true if null or empty, otherwise false
     */
    @JvmStatic
    fun isNullOrEmpty(cs: CharSequence?): Boolean {
        return cs == null || cs.toString().trim().isEmpty()
    }
}