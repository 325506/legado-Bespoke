package io.legado.app.ui.main.my

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import io.legado.app.R
import io.legado.app.base.BaseFragment
import io.legado.app.constant.EventBus
import io.legado.app.constant.PreferKey
import io.legado.app.constant.Theme
import io.legado.app.databinding.FragmentMyConfigBinding
import io.legado.app.service.WebService
import io.legado.app.ui.compose.screens.my.MyScreen
import io.legado.app.ui.compose.theme.LegadoTheme
import io.legado.app.ui.main.MainFragmentInterface
import io.legado.app.utils.observeEventSticky
import io.legado.app.utils.putPrefBoolean
import io.legado.app.utils.showHelp
import io.legado.app.utils.viewbindingdelegate.viewBinding

class MyFragment() : BaseFragment(R.layout.fragment_my_config), MainFragmentInterface {

    constructor(position: Int) : this() {
        val bundle = Bundle()
        bundle.putInt("position", position)
        arguments = bundle
    }

    override val position: Int? get() = arguments?.getInt("position")

    private val binding by viewBinding(FragmentMyConfigBinding::bind)

    private var webServiceRunning by mutableStateOf(WebService.isRun)
    private var webServiceAddress by mutableStateOf("")

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        initWebServiceState()
        observeWebServiceEvent()
        setupComposeContent()
    }

    private fun initWebServiceState() {
        webServiceRunning = WebService.isRun
        webServiceAddress = if (WebService.isRun) WebService.hostAddress else ""
    }

    private fun observeWebServiceEvent() {
        observeEventSticky<String>(EventBus.WEB_SERVICE) {
            webServiceRunning = WebService.isRun
            webServiceAddress = if (WebService.isRun) WebService.hostAddress else ""
        }
    }

    private fun setupComposeContent() {
        binding.preFragment.removeAllViews()
        val composeView = ComposeView(requireContext()).apply {
            setContent {
                LegadoTheme(theme = Theme.Auto) {
                    MyScreen(
                        onShowHelp = { showHelp("appHelp") },
                        webServiceState = Pair(webServiceRunning, webServiceAddress.ifEmpty { getString(R.string.web_service_desc) }),
                        onWebServiceToggle = { checked ->
                            webServiceRunning = checked
                            putPrefBoolean(PreferKey.webService, checked)
                            if (checked) {
                                WebService.start(requireContext())
                            } else {
                                WebService.stop(requireContext())
                            }
                        }
                    )
                }
            }
        }
        binding.preFragment.addView(composeView)
    }
}
