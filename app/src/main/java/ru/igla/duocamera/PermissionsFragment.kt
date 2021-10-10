package ru.igla.duocamera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import ru.igla.duocamera.ui.BaseFragment


private const val PERMISSIONS_REQUEST_CODE = 10
private val PERMISSIONS_REQUIRED = arrayOf(
    Manifest.permission.CAMERA,
    Manifest.permission.RECORD_AUDIO,
    Manifest.permission.WRITE_EXTERNAL_STORAGE
)

/**
 * This [Fragment] requests permissions and, once granted, it will navigate to the next fragment
 */
class PermissionsFragment : BaseFragment() {

    private var mListener: OnBackFragmentGoListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (hasPermissions(requireContext())) {
            // If permissions have already been granted, proceed
            mListener?.onFinishFragment()
            activity?.supportFragmentManager?.popBackStack()
        } else {
            requestCameraPermission()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mListener = try {
            activity as OnBackFragmentGoListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$activity must implement OnArticleSelectedListener")
        }
    }

    private fun requestCameraPermission() {
        if (!shouldShowRequestPermissionRationale(
                Manifest.permission.CAMERA
            )
        ) {
            requestPermissions(PERMISSIONS_REQUIRED, PERMISSIONS_REQUEST_CODE)
            return
        }
        val listener = View.OnClickListener {
            requestPermissions(PERMISSIONS_REQUIRED, PERMISSIONS_REQUEST_CODE)
        }

        val rootView: View = requireActivity().window.decorView.findViewById(android.R.id.content)
        Snackbar.make(
            rootView,
            "Request camera and record permissions",
            Snackbar.LENGTH_INDEFINITE
        )
            .setAction("OK", listener)
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Takes the user to the success fragment when permission is granted
                mListener?.onFinishFragment()
                activity?.supportFragmentManager?.popBackStack()
            } else {
                Toast.makeText(context, "Permission request denied", Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {

        /** Convenience method used to check if all permissions required by this app are granted */
        fun hasPermissions(context: Context) = PERMISSIONS_REQUIRED.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}