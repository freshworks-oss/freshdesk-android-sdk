package com.freshdesk.southwest.ui.activity

// import com.freshworks.sdk.freshdesk.BuildConfig
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freshdesk.southwest.R
import com.freshdesk.southwest.SouthWestApp
import com.freshdesk.southwest.components.LogOut
import com.freshdesk.southwest.components.RenderImage
import com.freshdesk.southwest.components.TitleBar
import com.freshdesk.southwest.components.drawer.AddDivider
import com.freshdesk.southwest.components.drawer.ContactWithUs
import com.freshdesk.southwest.components.drawer.DrawerElement
import com.freshdesk.southwest.components.drawer.DrawerHeader
import com.freshdesk.southwest.data.DataStore
import com.freshdesk.southwest.extensions.applySDK35InsetsListener
import com.freshdesk.southwest.ui.theme.NavigationDrawerTheme
import com.freshdesk.southwest.ui.theme.SouthWestTheme
import com.freshworks.sdk.freshdesk.FreshdeskSDK
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val SOUTH_WEST = "SOUTH_WEST"
const val SHOW_SPLASH_SCREEN = "SHOW_SPLASH_SCREEN"
const val DELAY = 1000L

class HomeActivity : ComponentActivity() {

    private val viewModel: HomeViewModel by viewModels()

    // Notification permission launcher for Android 13+
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        android.util.Log.d(SOUTH_WEST, "Notification permission ${if (isGranted) "granted" else "denied"}")
    }

    companion object {
        fun getIntent(context: Context, showSplashScreen: Boolean): Intent {
            return Intent(context, HomeActivity::class.java).apply {
                putExtra(SHOW_SPLASH_SCREEN, showSplashScreen)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val showSplashScreen: Boolean = intent.extras?.getBoolean(SHOW_SPLASH_SCREEN) ?: true
        viewModel.isLoading.observe(this) {
            setContent {
                SetContent(isLoading = it, showSplashScreen)
            }
        }
        applySDK35InsetsListener()

        // Request notification permission for Android 13+ (API 33+)
        requestNotificationPermissionIfNeeded()
    }

    /**
     * Requests notification permission if running on Android 13+ and permission not yet granted.
     * This is required for the Freshdesk SDK to post notifications.
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                    android.util.Log.d(SOUTH_WEST, "Notification permission already granted")
                }
                else -> {
                    // Request permission
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    @Composable
    fun SetContent(isLoading: Boolean, showSplashScreen: Boolean) {
        SouthWestTheme {
            if (isLoading && showSplashScreen) {
                RenderImage(
                    imageId = R.drawable.southwest_banner,
                    contentScale = ContentScale.FillBounds,
                    contentDescription = stringResource(id = R.string.profile_icon)
                )
            } else {
                HomeScreen()
            }
        }
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun HomeScreen() {
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val unreadCount = rememberSaveable { mutableIntStateOf(0) }
        val userName = rememberSaveable { mutableStateOf("") }
        userName.value = DataStore.getUserName()
        rememberSaveable { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(modifier = Modifier.width(272.dp)) {
                    DrawerContent(
                        userName.value,
                        unreadCount
                    )
                }
            },
        ) {
            Scaffold(
                topBar = {
                    TitleBar(userName.value, unreadCount.intValue) {
                        scope.launch {
                            drawerState.apply {
                                if (isClosed) open() else close()
                            }
                        }
                    }
                },
                bottomBar = {
                    BottomAppBar()
                }
            ) { _ -> HomeScreenContent() }
        }
    }

    @Composable
    fun BottomAppBar() {
        BottomAppBar(
            modifier = Modifier
                .height(48.dp)
                .background(MaterialTheme.colorScheme.surface),
            tonalElevation = 0.dp,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 56.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_bottom_home),
                    contentDescription = stringResource(
                        id = R.string.home
                    ),
                )
                Icon(
                    painter = painterResource(id = R.drawable.ic_bottom_heart),
                    contentDescription = stringResource(
                        id = R.string.home
                    ),
                )
                Icon(
                    painter = painterResource(id = R.drawable.ic_bottom_cart),
                    contentDescription = stringResource(
                        id = R.string.home
                    ),
                )
            }
        }
    }

    @Composable
    fun HomeScreenContent() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            RenderImage(
                imageId = R.drawable.home_page,
                contentDescription = stringResource(id = R.string.home)
            ) {
                Intent(applicationContext, ProductDetailActivity::class.java).also {
                    startActivity(it)
                }
            }
        }
    }

    @Composable
    fun DrawerContent(userName: String, unreadCount: MutableIntState) {
        (applicationContext as SouthWestApp).unreadCount.observeAsState().value?.let {
            unreadCount.intValue = it
        }
        NavigationDrawerTheme {
            if (userName.isNotEmpty()) {
                DrawerHeader(userName = userName)
            } else {
                DrawerHeader {
                    Intent(applicationContext, LoginActivity::class.java).also {
                        startActivity(it)
                    }
                }
            }
            DrawerContentElements(userName = userName, unreadCount = unreadCount)
        }
    }

    @Composable
    fun DrawerContentElements(userName: String, unreadCount: MutableIntState) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            DrawerElement(
                title = stringResource(id = R.string.my_orders),
                subTitle = stringResource(id = R.string.already_have_2_order)
            ) {
                Intent(applicationContext, MyOrdersActivity::class.java).also {
                    startActivity(it)
                }
            }
            AddDivider()
            DrawerElement(
                title = stringResource(id = R.string.shipping_address),
                subTitle = stringResource(id = R.string.three_address)
            )
            AddDivider()
            DrawerElement(
                title = stringResource(id = R.string.payment_method),
                subTitle = stringResource(id = R.string.visa_34)
            )
            AddDivider()
            ContactWithUs(unreadCount = unreadCount.intValue) {
                FreshdeskSDK.openSupport(this@HomeActivity)
            }
            AddDivider()
            DrawerElement(
                title = stringResource(id = R.string.my_reviews),
                subTitle = stringResource(id = R.string.reviews_for_4_items)
            )
            AddDivider()
            DrawerElement(
                title = stringResource(id = R.string.developer_settings),
                subTitle = stringResource(id = R.string.developer_settings_desc)
            ) {
                Intent(applicationContext, SettingsActivity::class.java).also {
                    startActivity(it)
                }
            }
            AddDivider()
            AddSDKVersionDetails()
            if (userName.isNotEmpty()) {
                LogOut {
                    DataStore.setUserName("")
                    this@HomeActivity.recreate()
                }
            }
        }
    }

    @Composable
    private fun AddSDKVersionDetails() {
        val versionName = FreshdeskSDK.getSDKVersionName()
        val versionCode = FreshdeskSDK.getSDKVersionCode()
        val buildId = FreshdeskSDK.getSDKBuildID()
        Column(
            modifier = Modifier
                .width(272.dp)
                .wrapContentHeight()
        ) {
            Text(
                text = stringResource(
                    id = R.string.sdk_version, versionName, versionCode
                ),
                fontSize = 14.sp,
                fontWeight = FontWeight.W600,
                fontFamily = FontFamily.SansSerif,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp)
            )

            Text(
                text = stringResource(
                    id = R.string.sdk_build_id, buildId
                ),
                fontSize = 12.sp,
                fontWeight = FontWeight.W300,
                fontFamily = FontFamily.SansSerif,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 16.dp, end = 16.dp)
            )
        }
    }
}

class HomeViewModel : ViewModel() {
    private val _isLoading = MutableLiveData(true)
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        viewModelScope.launch {
            delay(DELAY)
            _isLoading.value = false
        }
    }
}
