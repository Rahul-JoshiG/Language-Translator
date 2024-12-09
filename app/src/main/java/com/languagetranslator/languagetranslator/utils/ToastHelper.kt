import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast

@SuppressLint("StaticFieldLeak")
object ToastHelper {
    private lateinit var mContext: Context

    /**
     * Initializes the ToastHelper with the application context.
     * This method should only be called once in the Application class.
     */
    fun initToast(context: Context) {
        if (!::mContext.isInitialized) {
            mContext = context.applicationContext
        }
    }

    /**
     * Displays a toast message.
     *
     * @param message The message to display in the toast.
     */
    fun toast(message: String) {
        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show()
    }
}
