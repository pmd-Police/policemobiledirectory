package com.example.policemobiledirectory

sealed class Screen(val route: String) {
    object AdminLogin : Screen("admin_login")
    object PendingApprovals : Screen("pending_approvals")
    object EmployeeList : Screen("employee_list")
    object AddEmployee : Screen("add_employee")

    object EditEmployee : Screen("edit_employee/{employeeId}") {
        fun createRoute(employeeId: String) = "edit_employee/$employeeId"
    }

    object ForgotPin : Screen("forgot_pin_screen")
    object Login : Screen("login")
    object UserRegistration : Screen("user_registration")

    object ResetPassword : Screen("reset_password_screen/{kgid}") {
        fun createRoute(kgid: String) = "reset_password_screen/$kgid"
    }

    object AdminPanel : Screen("admin_panel_screen")
}
