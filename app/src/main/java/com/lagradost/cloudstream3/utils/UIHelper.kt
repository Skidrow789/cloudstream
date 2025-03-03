package com.lagradost.cloudstream3.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AppOpsManager
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.*
import android.view.ViewGroup.MarginLayoutParams
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.ListAdapter
import android.widget.ListView
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StyleRes
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.drawable.toBitmapOrNull
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.marginBottom
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.marginTop
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.palette.graphics.Palette
import androidx.preference.PreferenceManager
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions.bitmapTransform
import com.bumptech.glide.request.target.Target
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipDrawable
import com.google.android.material.chip.ChipGroup
import com.lagradost.cloudstream3.CommonActivity.activity
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.ui.result.UiImage
import com.lagradost.cloudstream3.ui.settings.SettingsFragment.Companion.isEmulatorSettings
import com.lagradost.cloudstream3.ui.settings.SettingsFragment.Companion.isTvSettings
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlin.math.roundToInt


object UIHelper {
    val Int.toPx: Int get() = (this * Resources.getSystem().displayMetrics.density).toInt()
    val Float.toPx: Float get() = (this * Resources.getSystem().displayMetrics.density)
    val Int.toDp: Int get() = (this / Resources.getSystem().displayMetrics.density).toInt()
    val Float.toDp: Float get() = (this / Resources.getSystem().displayMetrics.density)

    fun Context.checkWrite(): Boolean {
        return (ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
                == PackageManager.PERMISSION_GRANTED
                // Since Android 13, we can't request external storage permission,
                // so don't check it.
                || Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
    }

    fun populateChips(view: ChipGroup?, tags: List<String>, @StyleRes style : Int = R.style.ChipFilled) {
        if (view == null) return
        view.removeAllViews()
        val context = view.context ?: return

        tags.forEach { tag ->
            val chip = Chip(context)
            val chipDrawable = ChipDrawable.createFromAttributes(
                context,
                null,
                0,
                style
            )
            chip.setChipDrawable(chipDrawable)
            chip.text = tag
            chip.isChecked = false
            chip.isCheckable = false
            chip.isFocusable = false
            chip.isClickable = false
            chip.setTextColor(context.colorFromAttribute(R.attr.white))
            view.addView(chip)
        }
    }

    fun Activity.requestRW() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.MANAGE_EXTERNAL_STORAGE
            ),
            1337
        )
    }


    /**
     * Sets ListView height dynamically based on the height of the items.
     *
     * @param listView to be resized
     * @return true if the listView is successfully resized, false otherwise
     */
    fun setListViewHeightBasedOnItems(listView: ListView?) {
        val listAdapter: ListAdapter = listView?.adapter ?: return
        val numberOfItems: Int = listAdapter.count

        // Get total height of all items.
        var totalItemsHeight = 0
        for (itemPos in 0 until numberOfItems) {
            val item: View = listAdapter.getView(itemPos, null, listView)
            item.measure(0, 0)
            totalItemsHeight += item.measuredHeight
        }

        // Get total height of all item dividers.
        val totalDividersHeight: Int = listView.dividerHeight *
                (numberOfItems - 1)

        // Set list height.
        val params: ViewGroup.LayoutParams = listView.layoutParams
        params.height = totalItemsHeight + totalDividersHeight
        listView.layoutParams = params
        listView.requestLayout()
    }

    fun Context?.getSpanCount(): Int? {
        val compactView = false
        val spanCountLandscape = if (compactView) 2 else 6
        val spanCountPortrait = if (compactView) 1 else 3
        val orientation = this?.resources?.configuration?.orientation ?: return null

        return if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            spanCountLandscape
        } else {
            spanCountPortrait
        }
    }

    fun Fragment.hideKeyboard() {
        activity?.window?.decorView?.clearFocus()
        view?.let {
            hideKeyboard(it)
        }
    }

    fun Activity.hideKeyboard() {
        window?.decorView?.clearFocus()
        this.findViewById<View>(android.R.id.content)?.rootView?.let {
            hideKeyboard(it)
        }
    }

    fun Activity?.navigate(@IdRes navigation: Int, arguments: Bundle? = null) {
        try {
            if (this is FragmentActivity) {
                val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment?
                navHostFragment?.navController?.let {
                    it.navigate(navigation, arguments)
                }
            }
        } catch (t: Throwable) {
            logError(t)
        }
    }

    @ColorInt
    fun Context.getResourceColor(@AttrRes resource: Int, alphaFactor: Float = 1f): Int {
        val typedArray = obtainStyledAttributes(intArrayOf(resource))
        val color = typedArray.getColor(0, 0)
        typedArray.recycle()

        if (alphaFactor < 1f) {
            val alpha = (color.alpha * alphaFactor).roundToInt()
            return Color.argb(alpha, color.red, color.green, color.blue)
        }

        return color
    }

    var createPaletteAsyncCache: HashMap<String, Palette> = hashMapOf()
    fun createPaletteAsync(url: String, bitmap: Bitmap, callback: (Palette) -> Unit) {
        createPaletteAsyncCache[url]?.let { palette ->
            callback.invoke(palette)
            return
        }
        Palette.from(bitmap).generate { paletteNull ->
            paletteNull?.let { palette ->
                createPaletteAsyncCache[url] = palette
                callback(palette)
            }
        }
    }

    /*inline fun <reified T : ViewBinding> bindViewBinding(
        inflater: LayoutInflater?,
        container: ViewGroup?,
        layout: Int
    ): Pair<T?, UiText?> {
        return try {
            val localInflater = inflater ?: container?.context?.let { LayoutInflater.from(it) }
            ?: return null to txt(
                R.string.unable_to_inflate,
                "Requires inflater OR container"
            )//throw IllegalArgumentException("Requires inflater OR container"))

            //println("methods: ${T::class.java.methods.map { it.name }}")
            val bind = T::class.java.methods.first { it.name == "bind" }
            //val inflate = T::class.java.methods.first { it.name == "inflate" }
            val root = localInflater.inflate(layout, container, false)
            bind.invoke(null, root) as T to null
        } catch (t: Throwable) {
            logError(t)
            val message = txt(R.string.unable_to_inflate, t.message ?: "Primary constructor")
            // if the desired layout is not found then we inflate the casted layout
            /*try {
                val localInflater = inflater ?: container?.context?.let { LayoutInflater.from(it) }
                ?: return null to txt(
                    R.string.unable_to_inflate,
                    "Requires inflater OR container"
                )//throw IllegalArgumentException("Requires inflater OR container"))

                // we don't know what method to use as there are 2, but first *should* always be true
                return try {
                    val inflate = T::class.java.methods.first { it.name == "inflate" }
                    inflate.invoke(null, localInflater, container, false) as T
                } catch (_: Throwable) {
                    val inflate = T::class.java.methods.last { it.name == "inflate" }
                    inflate.invoke(null, localInflater, container, false) as T
                } to message
            } catch (t: Throwable) {
                logError(t)
            }*/

            null to message
        }
    }*/

    fun ImageView?.setImage(
        url: String?,
        headers: Map<String, String>? = null,
        @DrawableRes
        errorImageDrawable: Int? = null,
        fadeIn: Boolean = true,
        radius: Int = 0,
        sample: Int = 3,
        colorCallback: ((Palette) -> Unit)? = null
    ): Boolean {
        if (url.isNullOrBlank()) return false
        this.setImage(
            UiImage.Image(url, headers, errorImageDrawable),
            errorImageDrawable,
            fadeIn,
            radius,
            sample,
            colorCallback
        )
        return true
    }

    fun ImageView?.setImage(
        uiImage: UiImage?,
        @DrawableRes
        errorImageDrawable: Int? = null,
        fadeIn: Boolean = true,
        radius: Int = 0,
        sample: Int = 3,
        colorCallback: ((Palette) -> Unit)? = null,
    ): Boolean {
        if (this == null || uiImage == null) return false

        val (glideImage, identifier) =
            (uiImage as? UiImage.Drawable)?.resId?.let {
                it to it.toString()
            } ?: (uiImage as? UiImage.Image)?.let { image ->
                GlideUrl(image.url) { image.headers ?: emptyMap() } to image.url
            } ?: return false

        return try {
            var builder = com.bumptech.glide.Glide.with(this)
                .load(glideImage)
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.ALL).let { req ->
                    if (fadeIn)
                        req.transition(DrawableTransitionOptions.withCrossFade())
                    else req
                }

            if (radius > 0) {
                builder = builder.apply(bitmapTransform(BlurTransformation(radius, sample)))
            }

            if (colorCallback != null) {
                builder = builder.listener(object : RequestListener<Drawable> {

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        resource.toBitmapOrNull()
                            ?.let { bitmap ->
                                createPaletteAsync(
                                    identifier,
                                    bitmap,
                                    colorCallback
                                )
                            }
                        return false
                    }

                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }
                })
            }

            val res = if (errorImageDrawable != null)
                builder.error(errorImageDrawable).into(this)
            else
                builder.into(this)
            res.clearOnDetach()

            true
        } catch (e: Exception) {
            logError(e)
            false
        }
    }

    fun ImageView?.setImageBlur(
        url: String?,
        radius: Int,
        sample: Int = 3,
        headers: Map<String, String>? = null
    ) {
        if (this == null || url.isNullOrBlank()) return
        try {
            val res = com.bumptech.glide.Glide.with(this)
                .load(GlideUrl(url) { headers ?: emptyMap() })
                .apply(bitmapTransform(BlurTransformation(radius, sample)))
                .transition(
                    DrawableTransitionOptions.withCrossFade()
                )
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(this)
            res.clearOnDetach()
        } catch (e: Exception) {
            logError(e)
        }
    }

    fun adjustAlpha(@ColorInt color: Int, factor: Float): Int {
        val alpha = (Color.alpha(color) * factor).roundToInt()
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        return Color.argb(alpha, red, green, blue)
    }

    fun Context.colorFromAttribute(attribute: Int): Int {
        val attributes = obtainStyledAttributes(intArrayOf(attribute))
        val color = attributes.getColor(0, 0)
        attributes.recycle()
        return color
    }

    fun Activity.hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            if (window.insetsController != null) {

                window!!.insetsController?.hide(WindowInsetsCompat.Type.systemBars())
                window!!.insetsController?.systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }

        else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            // Set the content to appear under the system bars so that the
                            // content doesn't resize when the system bars hide and show.
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            // Hide the nav bar and status bar
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                    )
        }
    }

    fun FragmentActivity.popCurrentPage() {
        this.onBackPressedDispatcher.onBackPressed()
    }

    fun Context.getStatusBarHeight(): Int {
        if (isTvSettings()) {
            return 0
        }

        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    fun fixPaddingStatusbar(v: View?) {
        if (v == null) return
        val ctx = v.context ?: return
        v.setPadding(
            v.paddingLeft,
            v.paddingTop + ctx.getStatusBarHeight(),
            v.paddingRight,
            v.paddingBottom
        )
    }

    fun fixPaddingStatusbarMargin(v: View?) {
        if (v == null) return
        val ctx = v.context ?: return

        v.layoutParams = v.layoutParams.apply {
            if (this is MarginLayoutParams) {
                setMargins(
                    v.marginLeft,
                    v.marginTop + ctx.getStatusBarHeight(),
                    v.marginRight,
                    v.marginBottom
                )
            }
        }
    }

    fun fixPaddingStatusbarView(v: View?) {
        if (v == null) return
        val ctx = v.context ?: return
        val params = v.layoutParams
        params.height = ctx.getStatusBarHeight()
        v.layoutParams = params
    }

    fun Context.getNavigationBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    fun Context?.IsBottomLayout(): Boolean {
        if (this == null) return true
        val settingsManager = PreferenceManager.getDefaultSharedPreferences(this)
        return settingsManager.getBoolean(getString(R.string.bottom_title_key), true)
    }

    fun Activity.changeStatusBarState(hide: Boolean): Int {
        @Suppress("DEPRECATION")
        return if (hide) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.hide(WindowInsets.Type.statusBars())

            } else {
                window.setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
                )
            }
            0
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.show(WindowInsets.Type.statusBars())

            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            }

            this.getStatusBarHeight()
        }
    }

    // Shows the system bars by removing all the flags
    // except for the ones that make the content appear under the system bars.
    fun Activity.showSystemUI() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

        if (window.insetsController != null) {
            window!!.insetsController?.show(WindowInsetsCompat.Type.systemBars())
        }

        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        }

        changeStatusBarState(isEmulatorSettings())
    }

    fun Context.shouldShowPIPMode(isInPlayer: Boolean): Boolean {
        return try {
            val settingsManager = PreferenceManager.getDefaultSharedPreferences(this)
            settingsManager?.getBoolean(
                getString(R.string.pip_enabled_key),
                true
            ) ?: true && isInPlayer
        } catch (e: Exception) {
            logError(e)
            false
        }
    }

    fun Context.hasPIPPermission(): Boolean {
        val appOps =
            getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
                android.os.Process.myUid(),
                packageName
            ) == AppOpsManager.MODE_ALLOWED
        } else {
            return true
        }
    }

    fun hideKeyboard(view: View?) {
        if (view == null) return

        val inputMethodManager =
            view.context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager?
        inputMethodManager?.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun showInputMethod(view: View?) {
        if (view == null) return
        val inputMethodManager =
            view.context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager?
        inputMethodManager?.showSoftInput(view, 0)
    }

    fun Dialog?.dismissSafe(activity: Activity?) {
        if (this?.isShowing == true && activity?.isFinishing == false) {
            this.dismiss()
        }
    }

    fun Dialog?.dismissSafe() {
        if (this?.isShowing == true && activity?.isFinishing != true) {
            this.dismiss()
        }
    }

    /**id, stringRes */
    @SuppressLint("RestrictedApi")
    fun View.popupMenuNoIcons(
        items: List<Pair<Int, Int>>,
        onMenuItemClick: MenuItem.() -> Unit,
    ): PopupMenu {
        val ctw = ContextThemeWrapper(context, R.style.PopupMenu)
        val popup = PopupMenu(ctw, this, Gravity.NO_GRAVITY, R.attr.actionOverflowMenuStyle, 0)

        items.forEach { (id, stringRes) ->
            popup.menu.add(0, id, 0, stringRes)
        }

        (popup.menu as? MenuBuilder)?.setOptionalIconsVisible(true)

        popup.setOnMenuItemClickListener {
            it.onMenuItemClick()
            true
        }

        popup.show()
        return popup
    }

    /**id, string */
    @SuppressLint("RestrictedApi")
    fun View.popupMenuNoIconsAndNoStringRes(
        items: List<Pair<Int, String>>,
        onMenuItemClick: MenuItem.() -> Unit,
    ): PopupMenu {
        val ctw = ContextThemeWrapper(context, R.style.PopupMenu)
        val popup = PopupMenu(ctw, this, Gravity.NO_GRAVITY, R.attr.actionOverflowMenuStyle, 0)

        items.forEach { (id, string) ->
            popup.menu.add(0, id, 0, string)
        }

        (popup.menu as? MenuBuilder)?.setOptionalIconsVisible(true)

        popup.setOnMenuItemClickListener {
            it.onMenuItemClick()
            true
        }

        popup.show()
        return popup
    }
}