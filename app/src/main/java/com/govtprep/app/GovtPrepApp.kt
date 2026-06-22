package com.govtprep.app

import android.app.Application
import com.govtprep.app.data.remote.SupabaseModule
import com.govtprep.app.ui.theme.AvatarManager
import com.govtprep.app.ui.theme.ThemeManager
import com.govtprep.app.ui.theme.LanguageManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GovtPrepApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SupabaseModule.init(this)
        ThemeManager.init(this)
        LanguageManager.init(this)
        AvatarManager.init(this)
    }
}
