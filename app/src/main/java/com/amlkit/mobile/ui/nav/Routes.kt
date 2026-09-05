package com.amlkit.mobile.ui.nav

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val SETUP = "setup"
    const val VERIFY_EMAIL = "verify-email/{email}?token={token}"
    const val HOME = "home"
    const val DASHBOARD = "dashboard"
    const val SCREENING = "screening"
    const val CUSTOMERS = "customers"
    const val CUSTOMER_NEW = "customers/new"
    const val CUSTOMER_DETAIL = "customers/{customerId}"
    const val ALERTS = "alerts"
    const val AUDIT = "audit"
    const val ADMIN = "admin"
    const val REPORTS = "reports"
    const val REPORT_NEW = "reports/new"
    const val REPORT_DETAIL = "reports/{reportId}"
    const val MORE = "more"
    const val ABOUT = "about"

    fun customerDetail(customerId: Int) = "customers/$customerId"
    fun reportDetail(reportId: Int) = "reports/$reportId"

    fun verifyEmail(email: String, devToken: String? = null): String {
        val encodedEmail = java.net.URLEncoder.encode(email, "UTF-8")
        val encodedToken = java.net.URLEncoder.encode(devToken ?: "", "UTF-8")
        return "verify-email/$encodedEmail?token=$encodedToken"
    }
}
