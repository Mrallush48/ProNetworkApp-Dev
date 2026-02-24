package com.pronetwork.app.network

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Helper object to send approval requests for restricted actions (User role).
 * Admin executes directly, User sends a request for approval.
 */
object ApprovalHelper {

    /**
     * Check role and either execute directly (Admin) or send approval request (User).
     *
     * @param context Android context for Toast messages
     * @param authManager AuthManager instance
     * @param scope CoroutineScope for network call
     * @param requestType Type: DELETE_CLIENT, DELETE_BUILDING, EXPORT_REPORT, DISCONNECT_CLIENT
     * @param targetId Optional ID of the target entity
     * @param targetName Display name of the target (for admin review)
     * @param reason Optional reason for the request
     * @param onAdminDirect Lambda to execute directly if user is Admin
     * @param onRequestSent Lambda called after request is successfully sent (for UI feedback)
     * @param onError Lambda called on error
     */
    fun executeOrRequest(
        context: Context,
        authManager: AuthManager,
        scope: CoroutineScope,
        requestType: String,
        targetId: Int? = null,
        targetName: String? = null,
        reason: String? = null,
        onAdminDirect: () -> Unit,
        onRequestSent: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (authManager.isAdmin()) {
            // Admin → execute directly
            onAdminDirect()
        } else {
            // User → send approval request
            scope.launch {
                try {
                    val response = ApiClient.safeCall { api ->
                        api.createRequest(
                            token = authManager.getBearerToken(),
                            request = ApprovalRequestCreate(
                                request_type = requestType,
                                target_id = targetId,
                                target_name = targetName,
                                reason = reason
                            )
                        )
                    }
                    if (response.isSuccessful) {
                        onRequestSent()
                    } else {
                        val errorBody = response.errorBody()?.string() ?: "Unknown error"
                        onError(errorBody)
                    }
                } catch (e: Exception) {
                    onError(e.message ?: "Network error")
                }
            }
        }
    }
}
