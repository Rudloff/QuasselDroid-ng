package de.kuschku.quasseldroid.ui.coresettings.networkconfig

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.v7.widget.SwitchCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import butterknife.BindView
import butterknife.ButterKnife
import de.kuschku.libquassel.quassel.syncables.NetworkConfig
import de.kuschku.libquassel.util.Optional
import de.kuschku.quasseldroid.R
import de.kuschku.quasseldroid.ui.coresettings.SettingsFragment
import de.kuschku.quasseldroid.util.helper.setDependent
import de.kuschku.quasseldroid.util.helper.toLiveData

class NetworkConfigFragment : SettingsFragment() {
  private var networkConfig: Pair<NetworkConfig, NetworkConfig>? = null

  @BindView(R.id.ping_timeout_enabled)
  lateinit var pingTimeoutEnabled: SwitchCompat

  @BindView(R.id.ping_timeout_group)
  lateinit var pingTimeoutGroup: ViewGroup

  @BindView(R.id.ping_interval)
  lateinit var pingInterval: EditText

  @BindView(R.id.max_ping_count)
  lateinit var maxPingCount: EditText

  @BindView(R.id.auto_who_enabled)
  lateinit var autoWhoEnabled: SwitchCompat

  @BindView(R.id.auto_who_group)
  lateinit var autoWhoGroup: ViewGroup

  @BindView(R.id.auto_who_interval)
  lateinit var autoWhoInterval: EditText

  @BindView(R.id.auto_who_nick_limit)
  lateinit var autoWhoNickLimit: EditText

  @BindView(R.id.auto_who_delay)
  lateinit var autoWhoDelay: EditText

  @BindView(R.id.standard_ctcp)
  lateinit var standardCtcp: SwitchCompat

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                            savedInstanceState: Bundle?): View? {
    val view = inflater.inflate(R.layout.settings_networkconfig, container, false)
    ButterKnife.bind(this, view)

    viewModel.networkConfig
      .filter(Optional<NetworkConfig?>::isPresent)
      .map(Optional<NetworkConfig?>::get)
      .firstElement()
      .toLiveData().observe(this, Observer {
        if (it != null) {
          this.networkConfig = Pair(it, it.copy())
          this.networkConfig?.let { (_, data) ->
            pingTimeoutEnabled.isChecked = data.pingTimeoutEnabled()
            pingInterval.setText(data.pingInterval().toString())
            maxPingCount.setText(data.maxPingCount().toString())

            autoWhoEnabled.isChecked = data.autoWhoEnabled()
            autoWhoInterval.setText(data.autoWhoInterval().toString())
            autoWhoNickLimit.setText(data.autoWhoNickLimit().toString())
            autoWhoDelay.setText(data.autoWhoDelay().toString())

            standardCtcp.isChecked = data.standardCtcp()
          }
        }
      })

    pingTimeoutEnabled.setDependent(pingTimeoutGroup)
    autoWhoEnabled.setDependent(autoWhoGroup)

    return view
  }


  override fun onSave() = networkConfig?.let { (it, data) ->
    data.setPingTimeoutEnabled(pingTimeoutEnabled.isChecked)
    pingInterval.text.toString().toIntOrNull()?.let(data::setPingInterval)
    maxPingCount.text.toString().toIntOrNull()?.let(data::setMaxPingCount)

    data.setAutoWhoEnabled(autoWhoEnabled.isChecked)
    autoWhoInterval.text.toString().toIntOrNull()?.let(data::setAutoWhoInterval)
    autoWhoNickLimit.text.toString().toIntOrNull()?.let(data::setAutoWhoNickLimit)
    autoWhoDelay.text.toString().toIntOrNull()?.let(data::setAutoWhoDelay)
    data.setStandardCtcp(standardCtcp.isChecked)

    it.requestUpdate(data.toVariantMap())
    true
  } ?: false
}