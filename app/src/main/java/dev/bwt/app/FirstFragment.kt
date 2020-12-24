package dev.bwt.app

import android.content.Intent
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //view.findViewById<Button>(R.id.button_start).setOnClickListener {
        //    findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        //}

        observeLogs(view.findViewById(R.id.logview))
    }

    private fun observeLogs(logView: TextView) {
        val logCatViewModel by viewModels<LogCatViewModel>()
        logView.movementMethod = ScrollingMovementMethod() // auto scroll
        logCatViewModel.logCatOutput().observe(viewLifecycleOwner, Observer { logMessage ->
            logView.append("$logMessage\n")
        })
    }

    /*
    fun onClickSettings(view: View) {
        Log.d("bwt-main", "firstFragment onClickSettings")
        startActivity(Intent(context, SettingsActivity::class.java))
    }

    fun onClickStart(view: View) {
        Log.d("bwt-main", "firstFragment onClickStart")
        startActivity(Intent(context, MainActivity::class.java).apply {
            action = "dev.bwt.app.START_BWT"
        })
    }
     */
}