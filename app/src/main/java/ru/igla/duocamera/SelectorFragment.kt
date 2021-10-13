package ru.igla.duocamera

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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ru.igla.duocamera.dto.CameraInfo
import ru.igla.duocamera.ui.BaseFragment
import ru.igla.duocamera.ui.GenericListAdapter
import ru.igla.duocamera.utils.CameraReqType
import ru.igla.duocamera.utils.CameraUtils
import ru.igla.duocamera.utils.IntentUtils


/**
 * In this [Fragment] we let users pick a camera, size and FPS to use for high
 * speed video recording
 */
class SelectorFragment : BaseFragment() {

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

            val cameraManager =
                requireContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraList = enumerateVideoCameras(cameraManager)

            val layoutId = android.R.layout.simple_list_item_1
            adapter = GenericListAdapter(cameraList, itemLayoutId = layoutId) { view, item, _ ->
                view.findViewById<TextView>(android.R.id.text1).text = item.name
                view.setOnClickListener {
                    onClick(item)
                }
            }
        }
    }

    private fun onClick(item: CameraInfo) {
        val intent = Intent(context, DebugCameraActivity::class.java).apply {
            putExtra(DebugCameraActivity.CAMERA_INFO_OBJ, item)
        }
        IntentUtils.startActivitySafely(requireContext(), intent)
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
