package org.blockstack.android.sdk.ui

import android.app.Activity.RESULT_OK
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.coroutineScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.fragment_connect_dialog.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.blockstack.android.sdk.BlockstackSignIn
import org.blockstack.android.sdk.R

class ConnectBottemSheetDialogFragment : BottomSheetDialogFragment() {
    private val REQUEST_HELP = 1
    private var blockstackSignIn: BlockstackSignIn? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is SignInProvider) {
            blockstackSignIn = context.provideBlockstackSignIn()
        } else {
            error("ConnectDialogFragment can only be added to activities that implement ${SignInProvider::class.java}")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setContentView(R.layout.fragment_connect_dialog)

        activity?.apply {
            dialog.connect_dialog_title.text = getString(R.string.connect_dialog_title, getString(applicationInfo.labelRes))
            dialog.connect_dialog_app_icon.setImageResource(applicationInfo.icon)
            dialog.connect_dialog_get_started.setOnClickListener {
                lifecycle.coroutineScope.launch(Dispatchers.IO) {
                    blockstackSignIn?.redirectUserToSignIn(this@apply)
                    this@ConnectBottemSheetDialogFragment.dismiss()
                }
            }
            dialog.connect_dialog_restore.setOnClickListener {
                lifecycle.coroutineScope.launch(Dispatchers.IO) {
                    blockstackSignIn?.redirectUserToSignIn(this@apply, sendToSignIn = true)
                    this@ConnectBottemSheetDialogFragment.dismiss()
                }
            }
            dialog.connect_dialog_help.setOnClickListener {
                this@ConnectBottemSheetDialogFragment.startActivityForResult(Intent(this, ConnectHelpActivity::class.java), REQUEST_HELP)
                this@ConnectBottemSheetDialogFragment.dismiss()
            }
        }

        return dialog
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_HELP) {
            if (resultCode == RESULT_OK) {
                lifecycle.coroutineScope.launch(Dispatchers.IO) {
                    blockstackSignIn?.redirectUserToSignIn(requireActivity())
                    this@ConnectBottemSheetDialogFragment.dismiss()
                }
            }
        }
    }

    companion object {

        fun newInstance(): ConnectBottemSheetDialogFragment =
                ConnectBottemSheetDialogFragment().apply {
                    arguments = Bundle()
                }
    }
}

interface SignInProvider {
    fun provideBlockstackSignIn(): BlockstackSignIn

}
