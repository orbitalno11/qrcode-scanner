package me.orbitalno11.qrscanner.listener

import java.lang.Exception

interface ScannerListener {
    fun onSuccess(value: String?)
    fun onFailure(value: Exception)
}