package com.journcorp.journcart.search

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView.OnEditorActionListener
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.journcorp.journcart.R
import com.journcorp.journcart.core.application.App
import com.journcorp.journcart.core.entities.Search
import com.journcorp.journcart.core.fragments.BaseOtherFragment
import com.journcorp.journcart.core.utils.addFragment
import com.journcorp.journcart.core.utils.replaceFragment
import com.journcorp.journcart.databinding.FragmentSearchBinding
import com.journcorp.journcart.search.adapters.DeleteClickListener
import com.journcorp.journcart.search.adapters.SearchAdapter
import com.journcorp.journcart.search.adapters.SearchClickListener
import com.journcorp.journcart.searchResults.SearchResultsFragment


class SearchFragment : BaseOtherFragment() {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SearchViewModel by viewModels {
        SearchViewModelFactory((requireActivity().application as App).repository)
    }

    private lateinit var searchAdapter: SearchAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_search, container, false
        )

        val mainSearch = binding.mainSearch
        val searchRV = binding.searchRV
        val clearHistory = binding.clearHistory
        val closeSearch = binding.closeSearch

        closeSearch.setOnClickListener {
            activity?.onBackPressed()
        }

        binding.lifecycleOwner = viewLifecycleOwner

        searchAdapter =
            SearchAdapter(SearchClickListener {
                addFragment(
                    SearchResultsFragment(
                        it,
                        "all"
                    )
                )
            }, DeleteClickListener {
                viewModel.delete(it)
            }, "Search history")

        searchRV.layoutManager = LinearLayoutManager(requireContext())
        searchRV.adapter = searchAdapter

        // To display all items in recycler view
        viewModel.allSearchList.observe(viewLifecycleOwner) {
            searchAdapter.addHeaderAndSubmitList(it)
            //searchAdapter.notifyDataSetChanged()
        }

        clearHistory.setOnClickListener {
            viewModel.deleteAll()
        }

        mainSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val newText = s.toString()
                val searchQuery = "%$newText%"

                viewModel.searchRows(searchQuery).observe(viewLifecycleOwner) {
                    searchAdapter.addHeaderAndSubmitList(it)
                    //searchAdapter.notifyDataSetChanged()
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })

        mainSearch.setOnEditorActionListener(OnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val query = mainSearch.text.toString()
                val id = query.replace("\\s".toRegex(), "").lowercase()
                viewModel.insert(Search(id, query, System.currentTimeMillis()))

                replaceFragment(
                    SearchResultsFragment(
                        query,
                        "all"
                    )
                )
                return@OnEditorActionListener true
            }
            false
        })

        return binding.root
    }

    override fun onResume() {
        super.onResume()

        val mainSearch = binding.mainSearch
        mainSearch.requestFocus()
        val imm: InputMethodManager =
            requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(mainSearch, InputMethodManager.SHOW_IMPLICIT)

        val mainToolbar = requireActivity().findViewById(R.id.mainToolbar) as Toolbar
        mainToolbar.visibility = View.GONE
    }

    override fun onHiddenChanged(hidden: Boolean) {
        val mainToolbar = requireActivity().findViewById(R.id.mainToolbar) as Toolbar
        if(!hidden){//showing fragment
            mainToolbar.visibility = View.GONE
        }else{//hidden fragment
            mainToolbar.visibility = View.VISIBLE
        }
        super.onHiddenChanged(hidden)
    }

    override fun onDestroyView() {
        _binding = null
        val mainToolbar = requireActivity().findViewById(R.id.mainToolbar) as Toolbar
        mainToolbar.visibility = View.VISIBLE

        super.onDestroyView()
    }

}
