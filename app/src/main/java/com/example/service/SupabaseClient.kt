package com.example.service

import com.example.BuildConfig
import com.example.data.models.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

// @JvmSuppressWildcards evita que kotlin.collections.Map<K, out V> (covariante en V)
// emita wildcards "?" en la firma JVM de estos métodos. Retrofit valida cada parámetro
// @Body por reflexión Java y rechaza cualquier tipo con wildcard - sin esta anotación
// falla en runtime con "Parameter type must not include a type variable or wildcard"
// apenas se invoca el primer método con Map<String, Any> como body (login, signup, etc).
@JvmSuppressWildcards
interface SupabaseApi {
    // Auth (GoTrue)
    @POST("auth/v1/token?grant_type=password")
    suspend fun login(@Body body: Map<String, Any>): AuthResponse

    @POST("auth/v1/signup")
    suspend fun signup(@Body body: Map<String, Any>): AuthResponse

    @POST("auth/v1/token?grant_type=refresh_token")
    suspend fun refreshToken(@Body body: Map<String, String>): AuthResponse

    // Actualiza datos del usuario autenticado actual (ej. password). Requiere el
    // Authorization Bearer del propio usuario, que el authInterceptor ya agrega.
    @PUT("auth/v1/user")
    suspend fun updateAuthUser(@Body body: Map<String, String>): AuthUser

    // Profiles
    @GET("rest/v1/profiles")
    suspend fun getProfileById(@Query("id") id: String): List<Profile>

    @GET("rest/v1/profiles")
    suspend fun getProfileByPhone(@Query("phone") phone: String): List<Profile>

    @GET("rest/v1/profiles?order=full_name.asc")
    suspend fun getAllClientProfiles(@Query("role") role: String): List<Profile>

    @POST("rest/v1/profiles")
    suspend fun createProfile(@Body profile: Profile): List<Profile>

    @PATCH("rest/v1/profiles")
    suspend fun updateProfile(@Query("id") id: String, @Body updates: Map<String, Any?>): List<Profile>

    // Services
    @GET("rest/v1/services?order=id.asc")
    suspend fun getServices(): List<Service>

    @POST("rest/v1/services")
    suspend fun createService(@Body service: Service): List<Service>

    @PATCH("rest/v1/services")
    suspend fun updateService(@Query("id") id: String, @Body updates: Map<String, Any?>): List<Service>

    @DELETE("rest/v1/services")
    suspend fun deleteService(@Query("id") id: String)

    // Products
    @GET("rest/v1/products?order=id.desc")
    suspend fun getProducts(): List<Product>

    @POST("rest/v1/products")
    suspend fun createProduct(@Body product: Product): List<Product>

    @PATCH("rest/v1/products")
    suspend fun updateProduct(@Query("id") id: String, @Body updates: Map<String, Any?>): List<Product>

    @DELETE("rest/v1/products")
    suspend fun deleteProduct(@Query("id") id: String)

    // Appointments
    @GET("rest/v1/appointments?order=appointment_date.desc,appointment_time.asc")
    suspend fun getAllAppointments(): List<Appointment>

    @GET("rest/v1/appointments")
    suspend fun getAppointmentsByDate(
        @Query("appointment_date") date: String,
        @Query("order") order: String = "appointment_time.asc"
    ): List<Appointment>

    @GET("rest/v1/appointments")
    suspend fun getAppointmentsByClient(
        @Query("client_id") clientId: String,
        @Query("order") order: String = "appointment_date.desc"
    ): List<Appointment>

    @GET("rest/v1/appointments")
    suspend fun getAppointmentsByMainClient(
        @Query("main_client_id") clientId: String,
        @Query("order") order: String = "appointment_date.desc"
    ): List<Appointment>

    @POST("rest/v1/appointments")
    suspend fun createAppointment(@Body appointment: Appointment): List<Appointment>

    @PATCH("rest/v1/appointments")
    suspend fun updateAppointment(@Query("id") id: String, @Body updates: Map<String, Any?>): List<Appointment>

    @DELETE("rest/v1/appointments")
    suspend fun deleteAppointment(@Query("id") id: String)

    // Settings
    @GET("rest/v1/settings")
    suspend fun getSettings(): List<Settings>

    @POST("rest/v1/settings")
    suspend fun createSetting(@Body setting: Settings): List<Settings>

    @PATCH("rest/v1/settings")
    suspend fun updateSettingByKey(@Query("key") key: String, @Body updates: Map<String, Any?>): List<Settings>

    // Client notes (secciones 4/5/6). Body simple con Map, sin reutilizar la
    // entidad Room (así no arrastramos el id local ni el flag "synced" al POST).
    @POST("rest/v1/client_notes")
    suspend fun createClientNote(@Body body: Map<String, String>)

    // RPCs
    @POST("rest/v1/rpc/get_next_ticket")
    suspend fun getNextTicket(@Body body: Map<String, String>): Int?

    @POST("rest/v1/rpc/is_time_available")
    suspend fun isTimeAvailable(@Body body: Map<String, Any>): Boolean?

    @POST("rest/v1/rpc/create_appointment")
    suspend fun rpcCreateAppointment(@Body body: Map<String, Any?>): Long?

    @POST("rest/v1/rpc/get_day_appointments")
    suspend fun rpcGetDayAppointments(@Body body: Map<String, String>): List<Appointment>?

    @POST("rest/v1/rpc/cancel_appointment")
    suspend fun rpcCancelAppointment(@Body body: Map<String, Any?>): Boolean?

    @POST("rest/v1/rpc/mark_as_attended")
    suspend fun rpcMarkAsAttended(@Body body: Map<String, Any?>): Boolean?

    @POST("rest/v1/rpc/reschedule_next_month")
    suspend fun rpcRescheduleNextMonth(@Body body: Map<String, Any?>): Long?

    // Storage Upload
    @POST("storage/v1/object/Product_image/{filename}")
    suspend fun uploadProductImage(
        @Path("filename") filename: String,
        @Body fileBody: RequestBody
    ): Map<String, Any>?

    // Blocked Slots (botón ⊘ "Libre el resto del día", sección 8)
    @GET("rest/v1/blocked_slots")
    suspend fun getBlockedSlotsByDate(
        @Query("block_date") date: String
    ): List<BlockedSlot>

    @POST("rest/v1/blocked_slots")
    suspend fun createBlockedSlots(@Body slots: List<BlockedSlot>): List<BlockedSlot>

    @DELETE("rest/v1/blocked_slots")
    suspend fun deleteBlockedSlotsByDate(@Query("block_date") date: String)
}

data class AuthResponse(
    val access_token: String? = null,
    val refresh_token: String? = null,
    val user: AuthUser? = null,
    val error: String? = null,
    val error_description: String? = null
)

data class AuthUser(
    val id: String = "",
    val email: String? = null
)

object SupabaseClient {
    private val BASE_URL = BuildConfig.SUPABASE_URL
    private val ANON_KEY = BuildConfig.SUPABASE_ANON_KEY

    var currentAuthToken: String? = null
    var currentRefreshToken: String? = null

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val builder = original.newBuilder()
            .header("apikey", ANON_KEY)
            .header("Prefer", "return=representation")

        currentAuthToken?.let { token ->
            builder.header("Authorization", "Bearer $token")
        }

        chain.proceed(builder.build())
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        // Never log request/response bodies (tokens, passwords, phone numbers) in release builds.
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val api: SupabaseApi by lazy {
        Retrofit.Builder()
            .baseUrl("$BASE_URL/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SupabaseApi::class.java)
    }

    fun getPublicStorageUrl(filename: String): String {
        return "${BASE_URL}/storage/v1/object/public/Product_image/$filename"
    }
}