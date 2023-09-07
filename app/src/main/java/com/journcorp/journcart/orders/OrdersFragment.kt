package com.journcorp.journcart.orders

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.Toolbar
import androidx.core.net.toUri
import androidx.databinding.Observable
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.journcorp.journcart.R
import com.journcorp.journcart.core.activities.CameraActivity
import com.journcorp.journcart.core.adapters.ProductLoadStateAdapter
import com.journcorp.journcart.core.databases.MainRoomDatabase
import com.journcorp.journcart.core.fragments.BaseOtherFragment
import com.journcorp.journcart.core.utils.Constants
import com.journcorp.journcart.core.utils.Constants.tmpFileUriString
import com.journcorp.journcart.core.utils.GlideLoader
import com.journcorp.journcart.core.utils.Resource
import com.journcorp.journcart.databinding.FragmentOrdersBinding
import com.journcorp.journcart.orders.adapters.ConfirmClickListener
import com.journcorp.journcart.orders.adapters.DisputeClickListener
import com.journcorp.journcart.orders.adapters.OrdersAdapter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class OrdersFragment : BaseOtherFragment() {

    private var _binding: FragmentOrdersBinding? = null
    private val binding get() = _binding!!
    private lateinit var dialogView: View

    private lateinit var observableCallback: Observable.OnPropertyChangedCallback
    private lateinit var job: Job
    private lateinit var uiScope: CoroutineScope
    private lateinit var backgroundScope: CoroutineScope

    private lateinit var viewModel: OrdersViewModel
    private lateinit var ordersAdapter: OrdersAdapter


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrdersBinding.inflate(inflater, container, false)

        job = Job()
        uiScope = CoroutineScope(Dispatchers.Main + job)
        backgroundScope = CoroutineScope(Dispatchers.IO + job)

        val application = requireNotNull(activity).application
        val dataSource = MainRoomDatabase.getInstance(application)
        val viewModelFactory = OrdersViewModelFactory(
            dataSource, application
        )
        viewModel =
            ViewModelProvider(this@OrdersFragment, viewModelFactory)[OrdersViewModel::class.java]

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel


        ordersAdapter = OrdersAdapter(ConfirmClickListener { orderId ->
            //Toast.makeText(context, "confirm $orderId", Toast.LENGTH_SHORT).show()
            showConfirmOrderDialog(orderId)
        }, DisputeClickListener { orderId ->
            //Toast.makeText(context, "dispute $orderId", Toast.LENGTH_SHORT).show()
        })

        binding.rvOrders.apply {
            adapter = ordersAdapter.withLoadStateHeaderAndFooter(
                header = ProductLoadStateAdapter {
                    ordersAdapter.retry()
                },
                footer = ProductLoadStateAdapter {
                    ordersAdapter.retry()
                }
            )
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        }

        val spinner = binding.sFilterOrders

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.orderFilterBy,
            R.layout.design_orders_spinner
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            spinner.adapter = adapter
        }

        observers()
        return binding.root
    }

    private fun observers() {
        uiScope.launch {
            viewModel.pagingDataFlow.collectLatest {
                ordersAdapter.submitData(viewLifecycleOwner.lifecycle, it)
                //Log.i("onCreateViewobs",ordersAdapter.snapshot().items.toString() )
            }
        }
    }

    @SuppressLint("SetTextI18n", "InflateParams")
    private fun showConfirmOrderDialog(orderId: String) {
        val dialog = BottomSheetDialog(requireContext())
        dialogView = layoutInflater.inflate(R.layout.dialog_confirm_order, null)

        val ivUploadImage = dialogView.findViewById<ImageView>(R.id.ivUploadImage)
        val ratingBar = dialogView.findViewById<RatingBar>(R.id.ratingBar)
        val etFeedback = dialogView.findViewById<TextView>(R.id.etFeedback)
        val dialogConfirm = dialogView.findViewById<Button>(R.id.dialogConfirm)
        val pbConfirmOrders = dialogView.findViewById<ProgressBar>(R.id.pbConfirmOrders)
        tmpFileUriString.set(null)//reset the file value everytime the confirm dialog is opened


        dialogConfirm.setOnClickListener {
            if (tmpFileUriString.get() === null) {
                submitFeedback(orderId, ratingBar.rating, etFeedback.text.toString())
            } else {
                submitFeedback(
                    orderId,
                    tmpFileUriString.get()!!,
                    ratingBar.rating,
                    etFeedback.text.toString()
                )
            }

            uiScope.launch {
                viewModel.confirmOrderStatus.collectLatest {
                    when (it) {
                        is Resource.Loading -> {
                            pbConfirmOrders.visibility = View.VISIBLE
                        }
                        is Resource.Success -> {
                            pbConfirmOrders.visibility = View.GONE

                            if (it.data?.error != true) {
                                showErrorSnackBar(resources.getString(R.string.success), false)
                                dialog.hide()
                                viewModel.onRefresh()
                            } else {
                                showErrorSnackBar(
                                    resources.getString(R.string.err_unexpected_error),
                                    true
                                )
                            }
                        }
                        is Resource.Error -> {
                            pbConfirmOrders.visibility = View.GONE
                            showErrorSnackBar(
                                resources.getString(R.string.err_unexpected_error),
                                true
                            )
                        }
                    }
                }
            }
        }


        ivUploadImage.setOnClickListener {
            val intent = Intent(requireActivity(), CameraActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            intent.putExtra("title", resources.getString(R.string.feedback))
            startActivity(intent)

            observableCallback = object : Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                    if (sender == tmpFileUriString) {
                        val newValue = tmpFileUriString.get()
                        // Do something with the new value
                        newValue?.let {
                            val file = File(requireActivity().externalMediaDirs[0], newValue)

                            uiScope.launch {
                                GlideLoader(requireContext()).loadUserPicture(
                                    file.toUri(),
                                    ivUploadImage,
                                    false
                                )
                            }
                        }
                    }
                }
            }

            tmpFileUriString.addOnPropertyChangedCallback(observableCallback)
        }


        dialog.setCancelable(true)
        dialog.setContentView(dialogView)
        dialog.show()
    }

    private fun submitFeedback(
        orderId: String,
        feedBackImage: String,
        feedbackRate: Float,
        feedbackText: String
    ) {

        val file = File(requireActivity().externalMediaDirs[0], feedBackImage)
        val requestFile: RequestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
        val imagePart: MultipartBody.Part =
            MultipartBody.Part.createFormData("image", file.name, requestFile)

        Log.i("testing 0101", file.toUri().toString())

        viewModel.confirmOrder(
            Constants.deviceKey,
            orderId,
            imagePart,
            feedbackRate,
            feedbackText
        )
    }

    private fun submitFeedback(orderId: String, feedbackRate: Float, feedbackText: String) {
        viewModel.confirmOrder(Constants.deviceKey, orderId, feedbackRate, feedbackText)
    }


    override fun onResume() {
        super.onResume()
        val mainToolbar = requireActivity().findViewById(R.id.mainToolbar) as Toolbar
        mainToolbar.title = resources.getString(R.string.title_orders)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (!hidden) {//showing fragment
            val mainToolbar = requireActivity().findViewById(R.id.mainToolbar) as Toolbar
            mainToolbar.title = resources.getString(R.string.title_orders)
        }

        super.onHiddenChanged(hidden)
    }

    override fun onDestroyView() {
        binding.rvOrders.adapter = null

        _binding = null
        super.onDestroyView()
    }
}