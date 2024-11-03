import android.content.Context
import android.content.SharedPreferences

object SharedPrefsHelper {
    private const val PREFS_NAME = "app_prefs"
    private const val KEY_LOGIN = "login"
    private const val KEY_PASSWORD = "password"

    private lateinit var sharedPrefs: SharedPreferences

    fun init(context: Context) {
        sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveLoginAndPassword(login: String, password: String) {
        sharedPrefs.edit()
            .putString(KEY_LOGIN, login)
            .putString(KEY_PASSWORD, password)
            .apply()
    }

    fun getLogin(): String ?{
        return sharedPrefs.getString(KEY_LOGIN, null)
    }

    fun getPassword(): String? {
        return sharedPrefs.getString(KEY_PASSWORD, null)
    }
}