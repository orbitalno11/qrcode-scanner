package me.orbitalno11.qrscanner.listener

import java.io.Serializable
import java.lang.Exception

interface ScannerListener: Serializable {
    fun onSuccess(value: String?)
    fun onFailure(value: Exception)
}