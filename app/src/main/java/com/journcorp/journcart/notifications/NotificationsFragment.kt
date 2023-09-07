package com.journcorp.journcart.notifications

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.journcorp.journcart.R
import com.journcorp.journcart.core.databases.MainRoomDatabase
import com.journcorp.journcart.core.fragments.BaseOtherFragment
import com.journcorp.journcart.core.utils.Resource
import com.journcorp.journcart.core.utils.addFragment
import com.journcorp.journcart.databinding.FragmentNotificationsBinding
import com.journcorp.journcart.events.EventsFragment
import com.journcorp.journcart.notifications.adapters.NotificationsAdapter
import com.journcorp.journcart.notifications.adapters.NotificationsClickListener
import com.journcorp.journcart.store.StoreFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


class NotificationsFragment : BaseOtherFragment() {
    private lateinit var viewModel: NotificationsViewModel
    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private lateinit var job: Job
    private lateinit var uiScope: CoroutineScope
    private lateinit var backgroundScope: CoroutineScope

    private lateinit var notificationsAdapter: NotificationsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)

        job = Job()
        uiScope = CoroutineScope(Dispatchers.Main + job)
        backgroundScope = CoroutineScope(Dispatchers.IO + job)

        val application = requireNotNull(activity).application
        val dataSource = MainRoomDatabase.getInstance(application)
        val viewModelFactory = NotificationsViewModelFactory(
            dataSource, application
        )
        viewModel =
            ViewModelProvider(
                this@NotificationsFragment,
                viewModelFactory
            )[NotificationsViewModel::class.java]

        //binding.lifecycleOwner = viewLifecycleOwner
        //binding.viewModel = viewModel

        notificationsAdapter = NotificationsAdapter(NotificationsClickListener {
            val link = if (it.link[0] == '/') {
                it.link.substring(1)
            } else {
                it.link
            }
            val linkArray = link.split("/")
            when (linkArray[0]) {
                "product" -> {
                    openProductDetails(linkArray[2])
                }
                else -> {
                    if (linkArray.size >= 2 && linkArray[1] === "event") {
                        addFragment(EventsFragment(linkArray[0]))
                    } else {
                        addFragment(StoreFragment(linkArray[0]))
                    }
                }
            }

        }, "Public notifications")

        binding.rvNotifications.apply {
            adapter = notificationsAdapter
            layoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.VERTICAL, false
            )
        }

        observers()
        return binding.root
    }

    private fun observers() {
        uiScope.launch {
            viewModel.notificationsDataStatus.collectLatest {
                when (it) {
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        it.data?.let { result ->
                            Log.i("notifications", result.toString())
                            notificationsAdapter.addHeaderAndSubmitList(result)
                        }
                    }
                    else -> {
                        Log.i("notifications", "error")
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val mainToolbar = requireActivity().findViewById(R.id.mainToolbar) as Toolbar
        mainToolbar.title = resources.getString(R.string.title_notifications)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if(!hidden){//showing fragment
            val mainToolbar = requireActivity().findViewById(R.id.mainToolbar) as Toolbar
            mainToolbar.title = resources.getString(R.string.title_notifications)
        }
        super.onHiddenChanged(hidden)
    }

    override fun onDestroyView() {
        binding.rvNotifications.adapter = null
        job.cancel()
        _binding = null

        super.onDestroyView()

    }
}