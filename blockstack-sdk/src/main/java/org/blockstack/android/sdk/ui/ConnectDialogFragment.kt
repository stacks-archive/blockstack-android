package org.blockstack.android.sdk.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.coroutineScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.fragment_connect_dialog.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.blockstack.android.sdk.BlockstackSignIn
import org.blockstack.android.sdk.R
import org.blockstack.android.sdk.Scope
import org.blockstack.android.sdk.model.BlockstackConfig
import java.net.URI

// TODO: Customize parameter argument names
const val ARG_ITEM_COUNT = "item_count"

class ConnectDialogFragment : BottomSheetDialogFragment() {
    private var signInProvider: SignInProvider? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_connect_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        activity?.apply {
            view.findViewById<TextView>(R.id.connect_dialog_title).text = getString(R.string.connect_dialog_title, getString(applicationInfo.labelRes))
            view.findViewById<ImageView>(R.id.connect_dialog_app_icon).setImageResource(applicationInfo.icon)
            connect_dialog_get_started.setOnClickListener {
                lifecycle.coroutineScope.launch(Dispatchers.IO) {
                    signInProvider?.redirectUserToSignIn(true)
                }
            }
            connect_dialog_restore.setOnClickListener {
                lifecycle.coroutineScope.launch(Dispatchers.IO) {
                    signInProvider?.redirectUserToSignIn(sendToSignIn = true)
                }
            }
        }
    }

    companion object {

        fun newInstance(): ConnectDialogFragment =
                ConnectDialogFragment().apply {
                    arguments = Bundle()
                    }
                }

    }
}

interface SignInProvider {
    fun redirectUserToSignIn(sendToSignIn: Boolean = false)

}
