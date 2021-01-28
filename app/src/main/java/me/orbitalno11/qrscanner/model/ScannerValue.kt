package me.orbitalno11.qrscanner.model

enum class ScannerResult(val value: String) {
    QR_SUCCESS_RESULT("qr_scanner_success_result"),
    QR_FAILURE_RESULT("qr_scanner_failure_result")
}

enum class ScannerResponse(val value: Int) {
    REQUEST_CODE(1112),
    QR_RESULT_CODE(1113)
}