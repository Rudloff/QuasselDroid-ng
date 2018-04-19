package de.kuschku.quasseldroid.ui.coresettings.aliaslist

import android.content.Context
import android.content.Intent
import de.kuschku.quasseldroid.util.ui.ServiceBoundSettingsActivity

class AliasListActivity : ServiceBoundSettingsActivity(AliasListFragment()) {
  companion object {
    fun launch(context: Context) = context.startActivity(intent(context))
    fun intent(context: Context) = Intent(context, AliasListActivity::class.java)
  }
}