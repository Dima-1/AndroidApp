package com.strikelines.app.ui

import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager


@Suppress("DEPRECATION")
class ProgressDialogFragment : DialogFragment() {

	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		val args = arguments!!
		val titleId = args.getInt(TITLE_ID)
		val messageId = args.getInt(MESSAGE_ID)
		val style = args.getInt(STYLE)

		return createProgressDialog(context!!, getString(titleId), getString(messageId), style)
	}

	private fun createProgressDialog(ctx: Context, title: String, message: String, style: Int): ProgressDialog {
		return ProgressDialog(ctx).apply {
			setTitle(title)
			setMessage(message)
			setProgressStyle(style)
			isIndeterminate = style == ProgressDialog.STYLE_HORIZONTAL
		}
	}


	companion object {
		const val TAG = "progress"
		private const val TITLE_ID = "title_id"
		private const val MESSAGE_ID = "message_id"
		private const val STYLE = "style"

		fun showInstance(
			fm: FragmentManager,
			titleId: Int,
			messageId: Int,
			style: Int = ProgressDialog.STYLE_SPINNER
		): Boolean {
			return try {
				ProgressDialogFragment().apply {
					arguments = Bundle().apply {
						putInt(TITLE_ID, titleId)
						putInt(MESSAGE_ID, messageId)
						putInt(STYLE, style)
					}
					show(fm, TAG)
				}
				true
			} catch (e: RuntimeException) {
				false
			}
		}
	}
}