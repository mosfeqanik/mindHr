package com.mindhr.mobile.data

import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import android.util.Log
import com.mindhr.mobile.BuildConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.IOException

/**
 * Endpoints the mobile app uses. List endpoints are already narrowed by the user's data scope on
 * the server; "my" screens still pass [Session.employeeId] so managers see only their own rows there.
 */
interface MindHrApi {
    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("api/account/change-password")
    suspend fun changePassword(@Body body: ChangePasswordRequest): Response<Unit>

    @GET("api/employees/me")
    suspend fun me(): Employee

    @GET("api/employees")
    suspend fun employees(): List<Employee>

    @GET("api/branches/{id}")
    suspend fun branch(@Path("id") id: Long): Branch

    @GET("api/departments/{id}")
    suspend fun department(@Path("id") id: Long): Department

    // Attendance
    @GET("api/attendance/today")
    suspend fun today(): Response<Attendance>

    @GET("api/attendance")
    suspend fun attendance(@Query("employeeId") employeeId: Long? = null): List<Attendance>

    @POST("api/attendance/check-in")
    suspend fun checkIn(@Body body: CheckInRequest): Attendance

    @POST("api/attendance/check-out")
    suspend fun checkOut(@Body body: CheckInRequest): Attendance

    // Leave
    @GET("api/leave-types")
    suspend fun leaveTypes(): List<LeaveType>

    @GET("api/leave-balances")
    suspend fun leaveBalances(@Query("employeeId") employeeId: Long? = null): List<LeaveBalance>

    @GET("api/leave-requests")
    suspend fun leaveRequests(@Query("employeeId") employeeId: Long? = null): List<LeaveRequest>

    @POST("api/leave-requests")
    suspend fun applyLeave(@Body body: LeaveApplyRequest): LeaveRequest

    @POST("api/leave-requests/{id}/cancel")
    suspend fun cancelLeave(@Path("id") id: Long): LeaveRequest

    @POST("api/leave-requests/{id}/approve")
    suspend fun approveLeave(@Path("id") id: Long, @Body body: DecisionRequest): LeaveRequest

    @POST("api/leave-requests/{id}/reject")
    suspend fun rejectLeave(@Path("id") id: Long, @Body body: DecisionRequest): LeaveRequest

    // Payslips
    @GET("api/payslips")
    suspend fun payslips(@Query("employeeId") employeeId: Long? = null): List<Payslip>

    // Work requests (advance, loan, expense, attendance correction, ...)
    @GET("api/requests")
    suspend fun requests(@Query("employeeId") employeeId: Long? = null): List<WorkRequest>

    @POST("api/requests")
    suspend fun createRequest(@Body body: WorkRequestCreate): WorkRequest

    @POST("api/requests/{id}/cancel")
    suspend fun cancelRequest(@Path("id") id: Long): WorkRequest

    @POST("api/requests/{id}/approve")
    suspend fun approveRequest(@Path("id") id: Long, @Body body: DecisionRequest): WorkRequest

    @POST("api/requests/{id}/reject")
    suspend fun rejectRequest(@Path("id") id: Long, @Body body: DecisionRequest): WorkRequest

    // Notifications (the server returns only the caller's own)
    @GET("api/notifications")
    suspend fun notifications(): List<AppNotification>

    @PATCH("api/notifications/{id}/read")
    suspend fun markRead(@Path("id") id: Long): AppNotification
}

/** Holds the JWT and app preferences (SharedPreferences) and the Retrofit client that attaches the token. */
object Session {
    private const val PREFS = "mindhr"
    private const val TOKEN = "token"
    private const val NAME = "name"
    private const val EMAIL = "email"
    private const val ONBOARDED = "onboarded"
    private const val CLOCK_24H = "clock24h"

    private lateinit var prefs: android.content.SharedPreferences
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    val token: String? get() = prefs.getString(TOKEN, null)
    val fullName: String get() = prefs.getString(NAME, "") ?: ""

    /** Last email used to sign in, kept when "Remember me" is on. */
    var rememberedEmail: String
        get() = prefs.getString(EMAIL, "") ?: ""
        set(value) = prefs.edit { putString(EMAIL, value) }

    var onboarded: Boolean
        get() = prefs.getBoolean(ONBOARDED, false)
        set(value) = prefs.edit { putBoolean(ONBOARDED, value) }

    var clock24h: Boolean
        get() = prefs.getBoolean(CLOCK_24H, false)
        set(value) = prefs.edit { putBoolean(CLOCK_24H, value) }

    /** Called on a 401 from a protected endpoint so the UI can return to login. */
    var onUnauthorized: (() -> Unit)? = null

    val api: MindHrApi by lazy {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                token?.let { req.header("Authorization", "Bearer $it") }
                val res = chain.proceed(req.build())
                if (res.code == 401 && !res.request.url.encodedPath.endsWith("/auth/login")) {
                    signOut()
                    onUnauthorized?.invoke()
                }
                res
            }
            .build()
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(MindHrApi::class.java)
    }

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    fun save(res: LoginResponse) {
        prefs.edit { putString(TOKEN, res.accessToken).putString(NAME, res.fullName) }
        cachedClaims = null
    }

    /** Clears the token but keeps device preferences (onboarding, clock format, remembered email). */
    fun signOut() {
        prefs.edit { remove(TOKEN).remove(NAME) }
        cachedClaims = null
    }

    // ---- JWT claims (set by the backend's JwtService; they change only at next login) ----

    private var cachedClaims: JsonObject? = null

    private val claims: JsonObject
        get() = cachedClaims ?: decodeClaims(token).also { cachedClaims = it }

    private fun decodeClaims(jwt: String?): JsonObject {
        val payload = jwt?.split(".")?.getOrNull(1) ?: return JsonObject(emptyMap())
        return runCatching {
            val bytes = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            json.parseToJsonElement(String(bytes)).jsonObject
        }.getOrDefault(JsonObject(emptyMap()))
    }

    private fun longClaim(name: String): Long? = (claims[name] as? JsonPrimitive)?.longOrNull

    /** The signed-in user's employee profile; null for accounts without one (e.g. platform admins). */
    val employeeId: Long? get() = longClaim("employeeId")
    val branchId: Long? get() = longClaim("branchId")
    val dataScope: String get() = (claims["dataScope"] as? JsonPrimitive)?.content ?: "SELF"

    val authorities: Set<String>
        get() = (claims["authorities"] as? JsonArray)
            ?.mapNotNull { (it as? JsonPrimitive)?.content }?.toSet() ?: emptySet()

    fun can(authority: String) = authority in authorities

    /** Approvers see the Approvals screen and the pending-approvals counter. */
    val isApprover: Boolean get() = can("LEAVE_APPROVE") || can("REQUEST_APPROVE")

    /** Best-effort message from the backend error body or the network error. */
    fun errorMessage(e: Throwable): String = errorText(e).also { Log.w("MindHR", "API call failed: $it", e) }

    private fun errorText(e: Throwable): String = when (e) {
        is HttpException -> {
            val raw = runCatching { e.response()?.errorBody()?.string().orEmpty() }.getOrDefault("")
            val body = runCatching { json.decodeFromString<ErrorBody>(raw) }.getOrNull()
            body?.message ?: body?.error ?: when (e.code()) {
                401 -> "Invalid credentials"
                403 -> "You do not have permission"
                404 -> "Not found"
                else -> "Request failed (${e.code()})"
            }
        }
        is IOException -> "Cannot reach the server. Check your connection."
        else -> e.message ?: "Unexpected error"
    }
}
