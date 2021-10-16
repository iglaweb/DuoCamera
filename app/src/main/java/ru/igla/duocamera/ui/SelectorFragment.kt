package ru.igla.duocamera.ui

import android.R
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.igla.duocamera.dto.CameraInfo
import ru.igla.duocamera.ui.toastcompat.Toaster
import ru.igla.duocamera.ui.widgets.GenericListAdapter
import ru.igla.duocamera.utils.*


/**
 * In this [Fragment] we let users pick a camera, size and FPS to use for high
 * speed video recording
 */
class SelectorFragment : BaseFragment() {

    private val toaster: Toaster by lazy { Toaster(requireContext().applicationContext) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = RecyclerView(requireContext())

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view as RecyclerView
        view.apply {
            layoutManager = LinearLayoutManager(requireContext())

            lifecycleScope.launch(Dispatchers.Default) {
                val cameraManager =
                    requireContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager
                val cameraList = enumerateVideoCameras(cameraManager)
                withContext(Dispatchers.Main) {
                    if (cameraList.isEmpty()) {
                        toaster.showToast("No camera preview sizes available")
                    }
                    val layoutId = android.R.layout.simple_list_item_1
                    adapter =
                        GenericListAdapter(cameraList, itemLayoutId = layoutId) { view, item, _ ->
                            view.findViewById<TextView>(R.id.text1).text = item.name
                            view.setOnClickListener {
                                onClick(item)
                            }
                        }
                }
            }
        }
    }

    private fun onClick(item: CameraInfo) {
        Intent(context, DebugCameraActivity::class.java).apply {
            putExtra(DebugCameraActivity.CAMERA_INFO_OBJ, item)
            IntentUtils.startActivitySafely(requireContext(), this)
        }
    }

    companion object {

        /** Lists all video-capable cameras and supported resolution and FPS combinations */
        @SuppressLint("InlinedApi")
        private fun enumerateVideoCameras(cameraManager: CameraManager): List<CameraInfo> {
            val availableCameras: MutableList<CameraInfo> = mutableListOf()
            availableCameras.apply {
                add(
                    CameraInfo(
                        CameraReqType.REQ_MIN_SIZE,
                        "Min available size, 30 fps (${SIZE_VGA.long}x${SIZE_VGA.short})",
                        "0",
                        Size(SIZE_VGA.long, SIZE_VGA.short),
                        30
                    )
                )
                add(
                    CameraInfo(
                        CameraReqType.REQ_MAX_SIZE,
                        "Max available size, 30 fps (${SIZE_1080P.long}x${SIZE_1080P.short})",
                        "0",
                        Size(SIZE_1080P.long, SIZE_1080P.short),
                        30
                    )
                )
                addAll(CameraUtils.enumerateVideoCameras(cameraManager))
            }
            return availableCameras
        }
    }
}
