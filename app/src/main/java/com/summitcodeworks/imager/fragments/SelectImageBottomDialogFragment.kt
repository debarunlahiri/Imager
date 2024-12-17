package com.summitcodeworks.imager.fragments

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.RectF
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.summitcodeworks.imager.R
import com.summitcodeworks.imager.activities.CameraActivity
import com.summitcodeworks.imager.databinding.FragmentSelectImageBottomDialogBinding

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [SelectImageBottomDialogFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SelectImageBottomDialogFragment : BottomSheetDialogFragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var binding: FragmentSelectImageBottomDialogBinding

    private lateinit var mContext: Context

    private lateinit var onSelectImageListener: OnSelectImageListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSelectImageBottomDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mContext = requireContext()
        onSelectImageListener = mContext as OnSelectImageListener

        view.setBackgroundResource(R.drawable.rounded_bottom_sheet)

        binding.bCapture.setOnClickListener {
            onSelectImageListener.onCameraClick()
        }

        binding.bChooseFIle.setOnClickListener {
            onSelectImageListener.onGalleryClick()
        }

        binding.ibDialogSelectImageCancel.setOnClickListener {
            onSelectImageListener.onDialogDismiss()
            dismiss()
        }
    }

    interface OnSelectImageListener {
        fun onCameraClick()
        fun onGalleryClick()
        fun onDialogDismiss()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment SelectImageBottomDialogFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            SelectImageBottomDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}